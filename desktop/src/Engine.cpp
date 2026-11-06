#include "Engine.h"

#include <QCalendar>
#include <QRegularExpression>
#include <QLocale>
#include <QTimeZone>
#include <QVariantMap>

#include <algorithm>
#include <cmath>
#include <cstdlib>

#include "Platform.h"
#include "Settings.h"
#include "Tr.h"
#include "Features.h"
#include <QFileDialog>
#include <QFile>
#include <QStandardPaths>
#include <QTextStream>
#include "adhan/adhan.h"

namespace {
constexpr int kTickMs = 60 * 1000;
constexpr qint64 kStaleSeconds = 15 * 60;    // missed by more than this → skip silently
constexpr qint64 kHorizonSeconds = 36 * 3600;
constexpr int kMaxTimerMs = 24 * 3600 * 1000;

const Engine::Prayer kObligatory[] = {Engine::Prayer::Fajr, Engine::Prayer::Dhuhr, Engine::Prayer::Asr, Engine::Prayer::Maghrib, Engine::Prayer::Isha};
const Engine::Prayer kAll[] = {Engine::Prayer::Fajr, Engine::Prayer::Sunrise, Engine::Prayer::Dhuhr, Engine::Prayer::Asr, Engine::Prayer::Maghrib, Engine::Prayer::Isha};
}  // namespace

Engine::Engine(Settings* settings, QObject* parent) : QObject(parent), m_settings(settings) {
    m_eventTimer.setSingleShot(true);
    m_eventTimer.setTimerType(Qt::VeryCoarseTimer);
    connect(&m_eventTimer, &QTimer::timeout, this, &Engine::onEventTimer);
    m_tick.setTimerType(Qt::VeryCoarseTimer);
    connect(&m_tick, &QTimer::timeout, this, &Engine::onTick);
    connect(m_settings, &Settings::changed, this, &Engine::recompute);
    connect(&Tr::instance(), &Tr::changed, this, &Engine::recompute);
    m_lastSystemTz = QTimeZone::systemTimeZoneId();
    recompute();
    // Align the tick to the wall-clock minute so the countdown flips on time.
    const int msToMinute = kTickMs - static_cast<int>(QDateTime::currentMSecsSinceEpoch() % kTickMs);
    QTimer::singleShot(msToMinute, this, [this] { onTick(); m_tick.start(kTickMs); });
    m_lastTick = nowEpoch();
}

QString Engine::keyFor(Prayer p) {
    switch (p) {
        case Prayer::Fajr: return QStringLiteral("fajr");
        case Prayer::Sunrise: return QStringLiteral("sunrise");
        case Prayer::Dhuhr: return QStringLiteral("dhuhr");
        case Prayer::Asr: return QStringLiteral("asr");
        case Prayer::Maghrib: return QStringLiteral("maghrib");
        case Prayer::Isha: return QStringLiteral("isha");
    }
    return {};
}

QString Engine::labelFor(Prayer p) { return Tr::instance().prayer(keyFor(p)); }

// ---- computation ------------------------------------------------------------

Engine::DayTimes Engine::computeDay(qint64 instant, int dayOffset) const {
    const QTimeZone tz(m_settings->timezone().toUtf8());
    return computeDate(QDateTime::fromSecsSinceEpoch(instant, tz).date().addDays(dayOffset));
}

Engine::DayTimes Engine::computeDate(const QDate& local) const {
    const QTimeZone tz(m_settings->timezone().toUtf8());

    adhan::Coordinates coords{m_settings->latitude(), m_settings->longitude()};
    auto params = adhan::parametersFor(adhan::methodFromId(m_settings->method().toStdString()).value_or(adhan::Method::MuslimWorldLeague));
    if (m_settings->method() == "Custom") {
        params.fajrAngle = m_settings->customFajrAngle();
        params.ishaAngle = m_settings->customIshaAngle();
        params.ishaInterval = m_settings->customIshaInterval();
    }
    params.madhab = m_settings->madhab() == "hanafi" ? adhan::Madhab::Hanafi : adhan::Madhab::Shafi;
    const QString rule = m_settings->highLatitudeRule();
    params.highLatitudeRule = rule == "middleOfTheNight"  ? adhan::HighLatitudeRule::MiddleOfTheNight
                            : rule == "seventhOfTheNight" ? adhan::HighLatitudeRule::SeventhOfTheNight
                            : rule == "twilightAngle"     ? adhan::HighLatitudeRule::TwilightAngle
                                                          : adhan::recommendedHighLatitudeRule(coords);
    params.adjustments.fajr = m_settings->adjustment("fajr");
    params.adjustments.sunrise = m_settings->adjustment("sunrise");
    params.adjustments.dhuhr = m_settings->adjustment("dhuhr");
    params.adjustments.asr = m_settings->adjustment("asr");
    params.adjustments.maghrib = m_settings->adjustment("maghrib");
    params.adjustments.isha = m_settings->adjustment("isha");

    const auto t = adhan::computePrayerTimes(coords, adhan::CivilDate{local.year(), local.month(), local.day()}, params);
    DayTimes d{t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha, local.toString(Qt::ISODate)};
    // Imported mosque timetable overrides the calculation for listed dates.
    const auto row = m_settings->timetable().constFind(d.date);
    if (row != m_settings->timetable().constEnd()) {
        auto apply = [&](const QString& key, qint64& out) {
            const auto m = Features::minutesOfDay(row->value(key));
            if (m) out = QDateTime(local, QTime(*m / 60, *m % 60), tz).toSecsSinceEpoch();
        };
        apply("fajr", d.fajr); apply("sunrise", d.sunrise); apply("dhuhr", d.dhuhr);
        apply("asr", d.asr); apply("maghrib", d.maghrib); apply("isha", d.isha);
    }
    return d;
}

QString Engine::labelFor(Prayer p, const QDate& date) const {
    if (p == Prayer::Dhuhr && m_settings->jumuahEnabled() && Features::isFriday(date)) return Tr::instance().t("prayer.jumuah");
    return labelFor(p);
}

static qint64 timeOf(const Engine::DayTimes& d, Engine::Prayer p) {
    switch (p) {
        case Engine::Prayer::Fajr: return d.fajr;
        case Engine::Prayer::Sunrise: return d.sunrise;
        case Engine::Prayer::Dhuhr: return d.dhuhr;
        case Engine::Prayer::Asr: return d.asr;
        case Engine::Prayer::Maghrib: return d.maghrib;
        case Engine::Prayer::Isha: return d.isha;
    }
    return 0;
}

void Engine::recompute() {
    rebuildToday();
    rebuildQueue();
    emit updated();
}

void Engine::rebuildToday() {
    const qint64 now = nowEpoch();
    const DayTimes today = computeDay(now, 0);

    // Current obligatory prayer: last one at or before now (yesterday's Isha before Fajr).
    Prayer current = Prayer::Isha;
    for (Prayer p : kObligatory) if (timeOf(today, p) <= now) current = p;
    const bool beforeFajr = today.fajr > now;

    m_today.clear();
    const QDate todayDate = QDate::fromString(today.date, Qt::ISODate);
    for (Prayer p : kAll) {
        const qint64 t = timeOf(today, p);
        QVariantMap row;
        row["key"] = keyFor(p);
        row["label"] = labelFor(p, todayDate);
        row["time"] = formatTime(t);
        const int iq = p == Prayer::Sunrise ? 0 : m_settings->iqamah(keyFor(p));
        row["iqamah"] = iq > 0 ? formatTime(t + iq * 60) : QString();
        row["epoch"] = t;
        row["sunrise"] = p == Prayer::Sunrise;
        row["state"] = (!beforeFajr && p == current && p != Prayer::Sunrise) ? "current" : (t <= now ? "passed" : "upcoming");
        m_today.push_back(row);
    }

    // Period of the day and progress along it (Fajr → next Fajr).
    m_period = now < today.fajr ? "night" : now < today.sunrise ? "dawn" : now < today.dhuhr ? "morning"
             : now < today.asr ? "day" : now < today.maghrib ? "afternoon" : now < today.isha ? "dusk" : "night";
    const qint64 nextFajr = computeDay(now, 1).fajr;
    const double span = std::max<qint64>(1, nextFajr - today.fajr);
    m_dayProgress = std::clamp((now - today.fajr) / span, 0.0, 1.0);
    m_dayDots.clear();
    for (Prayer p : kObligatory) {
        const qint64 t = timeOf(today, p);
        QVariantMap dot; dot["frac"] = std::clamp((t - today.fajr) / span, 0.0, 1.0); dot["passed"] = t <= now;
        m_dayDots.push_back(dot);
    }

    // Next obligatory prayer strictly after now (wraps to tomorrow's Fajr).
    m_nextKey.clear();
    for (int off = 0; off < 3 && m_nextKey.isEmpty(); ++off) {
        const DayTimes d = off == 0 ? today : computeDay(now, off);
        for (Prayer p : kObligatory) {
            if (timeOf(d, p) > now) { m_nextKey = keyFor(p); m_nextAt = timeOf(d, p); m_nextIsTomorrow = off > 0; break; }
        }
    }
}

void Engine::rebuildQueue() {
    m_queue.clear();
    if (m_settings->notificationsEnabled()) {
        const qint64 now = nowEpoch();
        const int pre = m_settings->preReminderMinutes();
        const int jm = m_settings->jumuahEnabled() ? m_settings->jumuahReminderMinutes() : 0;
        for (int off = 0; off < 3; ++off) {
            const DayTimes d = computeDay(now, off);
            const bool friday = Features::isFriday(QDate::fromString(d.date, Qt::ISODate));
            for (Prayer p : kObligatory) {
                const qint64 t = timeOf(d, p);
                if (t <= now || t > now + kHorizonSeconds) continue;
                if (pre > 0 && t - pre * 60 > now) m_queue.push_back({Event::Kind::Pre, p, t - pre * 60, Features::inQuietHours(*m_settings, t - pre * 60)});
                if (friday && p == Prayer::Dhuhr && jm > 0 && t - jm * 60 > now) m_queue.push_back({Event::Kind::Jumuah, p, t - jm * 60, Features::inQuietHours(*m_settings, t - jm * 60)});
                m_queue.push_back({Event::Kind::Prayer, p, t, Features::inQuietHours(*m_settings, t)});
            }
        }
        std::sort(m_queue.begin(), m_queue.end(), [](const Event& a, const Event& b) { return a.at < b.at; });
    }
    armNext();
}

void Engine::armNext() {
    m_eventTimer.stop();
    if (m_queue.empty()) return;
    const qint64 delayMs = std::max<qint64>(0, (m_queue.front().at - nowEpoch()) * 1000);
    m_eventTimer.start(static_cast<int>(std::min<qint64>(delayMs, kMaxTimerMs)));
}

void Engine::onEventTimer() {
    drainDue(nowEpoch());
    if (m_queue.size() < 2) rebuildQueue(); else armNext();
    rebuildToday();
    emit updated();
}

void Engine::onTick() {
    const qint64 now = nowEpoch();
    // The tick is 60 s. A gap far from that in either direction means suspend/resume or a clock change.
    const bool slept = std::llabs((now - m_lastTick) - 60) > 90;
    m_lastTick = now;
    const QByteArray tz = QTimeZone::systemTimeZoneId();
    if (slept || tz != m_lastSystemTz) {
        m_lastSystemTz = tz;
        drainDue(now);
        recompute();
        return;
    }
    drainDue(now);
    if (m_queue.empty()) rebuildQueue(); else armNext();
    rebuildToday();
    emit updated();
}

void Engine::drainDue(qint64 now) {
    while (!m_queue.empty() && m_queue.front().at <= now) {
        const Event e = m_queue.front();
        m_queue.erase(m_queue.begin());
        if (now - e.at <= kStaleSeconds) fire(e);
    }
}

void Engine::fire(const Event& e, bool snoozed) {
    const QDate date = QDateTime::fromSecsSinceEpoch(e.at, QTimeZone(m_settings->timezone().toUtf8())).date();
    const QString label = labelFor(e.prayer, date);
    const Tr& tr = Tr::instance();
    QString title, message;
    if (e.kind == Event::Kind::Jumuah) {
        const int n = m_settings->jumuahReminderMinutes();
        title = tr.f("notif.jumuahTitle", {{"n", n}});
        message = tr.f("notif.jumuahBody", {{"time", formatTime(e.at + n * 60)}});
    } else if (e.pre()) {
        const int n = m_settings->preReminderMinutes();
        title = tr.f("notif.preTitle", {{"prayer", label}, {"n", n}});
        message = tr.f("notif.preBody", {{"prayer", label}, {"time", formatTime(e.at + n * 60)}});
    } else if (snoozed) {
        title = tr.f("notif.snoozedTitle", {{"prayer", label}});
        message = tr.t("notif.snoozedBody");
    } else {
        title = tr.f("notif.prayerTitle", {{"prayer", label}});
        message = tr.f("notif.prayerBody", {{"prayer", label}, {"time", formatTime(e.at)}});
    }
    const bool isPrayer = e.kind == Event::Kind::Prayer;
    if (isPrayer) { m_lastFired = e; m_hasLastFired = true; }
    emit notify(title, message, isPrayer);
    if (isPrayer && m_settings->sound() && !e.quiet) Platform::playSound(Platform::chimePath());
}

void Engine::snooze() {
    if (!m_hasLastFired) return;
    const Event e = m_lastFired;
    QTimer::singleShot(m_settings->snoozeMinutes() * 60 * 1000, this, [this, e] { fire(e, true); });
}

void Engine::testNotification() {
    Event e;
    e.prayer = Prayer::Dhuhr;
    e.at = nowEpoch();
    fire(e);
}

// ---- features ---------------------------------------------------------------

QString Engine::ramadanNext(qint64& at) const {
    const qint64 now = nowEpoch();
    const DayTimes d = computeDay(now, 0);
    const Tr& tr = Tr::instance();
    if (now < d.fajr) { at = d.fajr; return tr.t("prayer.suhoor"); }
    if (now < d.maghrib) { at = d.maghrib; return tr.t("prayer.iftar"); }
    at = computeDay(now, 1).fajr;
    return tr.t("prayer.suhoor");
}

bool Engine::ramadan() const {
    const QDate local = QDateTime::currentDateTime().toTimeZone(QTimeZone(m_settings->timezone().toUtf8())).date();
    return Features::isRamadan(*m_settings, local);
}
QString Engine::ramadanLabel() const { qint64 at; return ramadanNext(at); }
QString Engine::ramadanTime() const { qint64 at; ramadanNext(at); return formatTime(at); }
QString Engine::ramadanCountdown() const {
    qint64 at; ramadanNext(at);
    const qint64 total = std::max<qint64>(0, std::llround((at - nowEpoch()) / 60.0));
    const qint64 h = total / 60, m = total % 60;
    return h == 0 ? QStringLiteral("%1m").arg(m) : QStringLiteral("%1h %2m").arg(h).arg(m, 2, 10, QLatin1Char('0'));
}
double Engine::qiblaBearing() const { return Features::qiblaBearing(m_settings->latitude(), m_settings->longitude()); }
double Engine::qiblaDistanceKm() const { return Features::distanceToKaabaKm(m_settings->latitude(), m_settings->longitude()); }

QVariantList Engine::monthTable(int year, int month) const {
    QVariantList out;
    const QDate today = QDateTime::currentDateTime().toTimeZone(QTimeZone(m_settings->timezone().toUtf8())).date();
    const QLocale loc = Tr::instance().locale();
    for (int day = 1; day <= QDate(year, month, 1).daysInMonth(); ++day) {
        const QDate date(year, month, day);
        const DayTimes d = computeDate(date);
        QVariantMap row;
        row["day"] = day;
        row["weekday"] = loc.dayName(date.dayOfWeek(), QLocale::ShortFormat);
        const auto h = Features::hijri(date);
        row["hijri"] = h ? QStringLiteral("%1/%2").arg(h->day).arg(h->month) : QString();
        row["friday"] = Features::isFriday(date);
        row["today"] = date == today;
        row["imported"] = m_settings->timetable().contains(d.date);
        for (Prayer p : kAll) row[keyFor(p)] = formatTime(timeOf(d, p));
        out.push_back(row);
    }
    return out;
}

QString Engine::monthName(int year, int month) const {
    return Tr::instance().locale().toString(QDate(year, month, 1), QStringLiteral("MMMM yyyy"));
}

QString Engine::exportMonthCsv(int year, int month) {
    const QString suggested = QStandardPaths::writableLocation(QStandardPaths::DocumentsLocation)
        + QStringLiteral("/salat-%1-%2-%3.csv").arg(m_settings->label().toLower().replace(QRegularExpression("[^a-z0-9]+"), "-")).arg(year).arg(month, 2, 10, QLatin1Char('0'));
    const QString path = QFileDialog::getSaveFileName(nullptr, Tr::instance().t("month.export"), suggested, QStringLiteral("CSV (*.csv)"));
    if (path.isEmpty()) return {};
    QFile f(path);
    if (!f.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text)) return {};
    QTextStream out(&f);
    out << "date,fajr,sunrise,dhuhr,asr,maghrib,isha\n";
    const QTimeZone tz(m_settings->timezone().toUtf8());
    for (int day = 1; day <= QDate(year, month, 1).daysInMonth(); ++day) {
        const QDate date(year, month, day);
        const DayTimes d = computeDate(date);
        out << d.date;
        for (Prayer p : kAll) out << ',' << QDateTime::fromSecsSinceEpoch(timeOf(d, p), tz).toString("HH:mm");
        out << '\n';
    }
    return path;
}

QString Engine::importTimetable() {
    const QString path = QFileDialog::getOpenFileName(nullptr, Tr::instance().t("settings.timetableImport"), QString(), QStringLiteral("CSV (*.csv *.txt);;All files (*)"));
    if (path.isEmpty()) return {};
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly | QIODevice::Text)) return {};
    const auto result = Features::parseTimetableCsv(QString::fromUtf8(f.readAll()));
    if (result.days() > 0) {
        auto merged = m_settings->timetable();
        for (auto it = result.entries.begin(); it != result.entries.end(); ++it) merged[it.key()] = it.value();
        m_settings->setTimetable(merged);
    }
    QString msg = Tr::instance().f("settings.timetableStatus", {{"days", result.days()}});
    if (!result.errors.isEmpty()) msg += " · " + Tr::instance().f("settings.timetableErrors", {{"n", result.errors.size()}});
    return msg;
}

void Engine::clearTimetable() { m_settings->setTimetable({}); }

// ---- presentation -----------------------------------------------------------

QString Engine::formatTime(qint64 epoch) const {
    const QDateTime dt = QDateTime::fromSecsSinceEpoch(epoch, QTimeZone(m_settings->timezone().toUtf8()));
    return m_settings->hour12() ? QLocale(QLocale::English).toString(dt, QStringLiteral("h:mm AP")) : dt.toString(QStringLiteral("HH:mm"));  // Latin digits in every language
}

QString Engine::nextKey() const { return m_nextKey; }
QString Engine::nextLabel() const {
    for (Prayer p : kObligatory) if (keyFor(p) == m_nextKey) return labelFor(p);
    return {};
}
QString Engine::nextTime() const { return m_nextKey.isEmpty() ? QString() : formatTime(m_nextAt); }

QString Engine::countdown() const {
    const qint64 total = std::max<qint64>(0, std::llround((m_nextAt - nowEpoch()) / 60.0));
    if (total < 1) return QStringLiteral("<1m");
    const qint64 h = total / 60, m = total % 60;
    return h == 0 ? QStringLiteral("%1m").arg(m) : QStringLiteral("%1h %2m").arg(h).arg(m, 2, 10, QLatin1Char('0'));
}

QString Engine::gregorianDate() const {
    const QDateTime now = QDateTime::currentDateTime().toTimeZone(QTimeZone(m_settings->timezone().toUtf8()));
    return Tr::instance().locale().toString(now.date(), QStringLiteral("ddd, d MMM"));
}

QString Engine::hijriDate() const {
    // Tabular (civil) Islamic calendar: can differ by ±1 day from Umm al-Qura / sighting.
    const QDate today = QDateTime::currentDateTime().toTimeZone(QTimeZone(m_settings->timezone().toUtf8())).date();
    const QCalendar cal(QCalendar::System::IslamicCivil);
    if (!cal.partsFromDate(today).isValid()) return {};
    return cal.dateTimeToString(QStringLiteral("d MMMM yyyy"), QDateTime(), today, QTime(), Tr::instance().locale());
}

QString Engine::methodLabel() const {
    static const QHash<QString, QString> labels = {
        {"MuslimWorldLeague", "Muslim World League"}, {"Egyptian", "Egyptian General Authority"}, {"Karachi", "Karachi"},
        {"UmmAlQura", "Umm al-Qura"}, {"Dubai", "Dubai"}, {"MoonsightingCommittee", "Moonsighting Committee"},
        {"NorthAmerica", "ISNA"}, {"Kuwait", "Kuwait"}, {"Qatar", "Qatar"}, {"Singapore", "Singapore"}, {"Tehran", "Tehran"},
        {"Turkey", "Diyanet"}, {"Custom", "Custom angles"}};
    return labels.value(m_settings->method(), m_settings->method());
}

QString Engine::trayText() const {
    if (m_nextKey.isEmpty()) return Tr::instance().t("app.name");
    return QStringLiteral("%1 %2 · %3 %4").arg(nextLabel(), nextTime(), Tr::instance().t("home.in"), countdown());
}

QVariantList Engine::previewToday() const {
    const DayTimes d = computeDay(nowEpoch(), 0);
    QVariantList out;
    for (Prayer p : kAll) {
        QVariantMap row;
        row["label"] = labelFor(p);
        row["time"] = formatTime(timeOf(d, p));
        out.push_back(row);
    }
    return out;
}
