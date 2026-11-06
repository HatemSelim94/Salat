#include "Features.h"

#include <QCalendar>
#include <QDateTime>
#include <QRegularExpression>
#include <QTimeZone>

#include <cmath>

#include "Settings.h"

namespace Features {
namespace {
constexpr double kKaabaLat = 21.4225;
constexpr double kKaabaLng = 39.8262;
constexpr double kPi = 3.14159265358979323846;
double rad(double d) { return d * kPi / 180.0; }
double deg(double r) { return r * 180.0 / kPi; }
}  // namespace

double qiblaBearing(double latitude, double longitude) {
    const double p1 = rad(latitude), p2 = rad(kKaabaLat), dl = rad(kKaabaLng - longitude);
    const double y = std::sin(dl) * std::cos(p2);
    const double x = std::cos(p1) * std::sin(p2) - std::sin(p1) * std::cos(p2) * std::cos(dl);
    return std::fmod(deg(std::atan2(y, x)) + 360.0, 360.0);
}

double distanceToKaabaKm(double latitude, double longitude) {
    const double p1 = rad(latitude), p2 = rad(kKaabaLat);
    const double dp = p2 - p1, dl = rad(kKaabaLng - longitude);
    const double a = std::sin(dp / 2) * std::sin(dp / 2) + std::cos(p1) * std::cos(p2) * std::sin(dl / 2) * std::sin(dl / 2);
    return 6371.0 * 2 * std::atan2(std::sqrt(a), std::sqrt(1 - a));
}

std::optional<Hijri> hijri(const QDate& local) {
    const QCalendar cal(QCalendar::System::IslamicCivil);
    const auto parts = cal.partsFromDate(local);
    if (!parts.isValid()) return std::nullopt;
    return Hijri{parts.year, parts.month, parts.day};
}

bool isRamadan(const Settings& s, const QDate& local) {
    if (s.ramadan() == "on") return true;
    if (s.ramadan() == "off") return false;
    const auto h = hijri(local);
    return h && h->month == 9;
}

std::optional<int> minutesOfDay(const QString& hhmm) {
    static const QRegularExpression re(QStringLiteral("^(\\d{1,2}):(\\d{2})$"));
    const auto m = re.match(hhmm.trimmed());
    if (!m.hasMatch()) return std::nullopt;
    const int h = m.captured(1).toInt(), min = m.captured(2).toInt();
    if (h > 23 || min > 59) return std::nullopt;
    return h * 60 + min;
}

bool inQuietHours(const Settings& s, qint64 epoch) {
    if (!s.quietHoursEnabled()) return false;
    const auto from = minutesOfDay(s.quietFrom()), to = minutesOfDay(s.quietTo());
    if (!from || !to) return false;
    const QTime t = QDateTime::fromSecsSinceEpoch(epoch, QTimeZone(s.timezone().toUtf8())).time();
    const int now = t.hour() * 60 + t.minute();
    return *from <= *to ? (now >= *from && now < *to) : (now >= *from || now < *to);
}

// ---- timetable import -------------------------------------------------------

namespace {
const QHash<QString, QString>& aliases() {
    static const QHash<QString, QString> a = {
        {"date", "date"}, {"datum", "date"}, {"day", "date"}, {QString::fromUtf8("تاريخ"), "date"}, {QString::fromUtf8("التاريخ"), "date"},
        {"fajr", "fajr"}, {"fajer", "fajr"}, {"fadschr", "fajr"}, {QString::fromUtf8("الفجر"), "fajr"}, {QString::fromUtf8("فجر"), "fajr"},
        {"sunrise", "sunrise"}, {"shuruq", "sunrise"}, {"shurooq", "sunrise"}, {"sonnenaufgang", "sunrise"}, {QString::fromUtf8("الشروق"), "sunrise"}, {QString::fromUtf8("شروق"), "sunrise"},
        {"dhuhr", "dhuhr"}, {"zuhr", "dhuhr"}, {"duhr", "dhuhr"}, {"dhuhur", "dhuhr"}, {QString::fromUtf8("الظهر"), "dhuhr"}, {QString::fromUtf8("ظهر"), "dhuhr"},
        {"asr", "asr"}, {QString::fromUtf8("العصر"), "asr"}, {QString::fromUtf8("عصر"), "asr"},
        {"maghrib", "maghrib"}, {"magrib", "maghrib"}, {QString::fromUtf8("المغرب"), "maghrib"}, {QString::fromUtf8("مغرب"), "maghrib"},
        {"isha", "isha"}, {"ishaa", "isha"}, {QString::fromUtf8("العشاء"), "isha"}, {QString::fromUtf8("عشاء"), "isha"},
    };
    return a;
}

QString normaliseDate(const QString& s) {
    static const QRegularExpression iso(QStringLiteral("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$"));
    static const QRegularExpression eu(QStringLiteral("^(\\d{1,2})[./](\\d{1,2})[./](\\d{4})$"));
    auto m = iso.match(s);
    if (m.hasMatch()) return QStringLiteral("%1-%2-%3").arg(m.captured(1), m.captured(2).rightJustified(2, '0'), m.captured(3).rightJustified(2, '0'));
    m = eu.match(s);
    if (m.hasMatch()) return QStringLiteral("%1-%2-%3").arg(m.captured(3), m.captured(2).rightJustified(2, '0'), m.captured(1).rightJustified(2, '0'));
    return {};
}

QString normaliseTime(const QString& s) {
    static const QRegularExpression re(QStringLiteral("^(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*(am|pm)?$"), QRegularExpression::CaseInsensitiveOption);
    const auto m = re.match(s);
    if (!m.hasMatch()) return {};
    int h = m.captured(1).toInt();
    const int min = m.captured(2).toInt();
    const QString ampm = m.captured(3).toLower();
    if (!ampm.isEmpty()) { if (ampm == "pm" && h < 12) h += 12; if (ampm == "am" && h == 12) h = 0; }
    if (h > 23 || min > 59) return {};
    return QStringLiteral("%1:%2").arg(h, 2, 10, QLatin1Char('0')).arg(min, 2, 10, QLatin1Char('0'));
}
}  // namespace

ImportResult parseTimetableCsv(const QString& text) {
    ImportResult r;
    QStringList lines;
    for (const QString& l : text.split(QRegularExpression(QStringLiteral("\\r?\\n")))) if (!l.trimmed().isEmpty()) lines << l.trimmed();
    if (lines.isEmpty()) { r.errors << "empty file"; return r; }
    const QChar sep = lines[0].contains(';') ? ';' : lines[0].contains('\t') ? '\t' : ',';
    QStringList first;
    for (const QString& c : lines[0].split(sep)) first << c.trimmed().toLower().remove('"').remove('\'');
    QStringList columns;
    int start = 0;
    bool header = false;
    for (const QString& c : first) if (aliases().value(c) == "date") header = true;
    if (header) { for (const QString& c : first) columns << aliases().value(c); start = 1; }
    else columns = {"date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    const int dateIdx = columns.indexOf("date");
    for (int i = start; i < lines.size(); ++i) {
        QStringList cells;
        for (const QString& c : lines[i].split(sep)) cells << c.trimmed().remove('"').remove('\'');
        const QString iso = normaliseDate(cells.value(dateIdx));
        if (iso.isEmpty()) { r.errors << QStringLiteral("line %1: unreadable date").arg(i + 1); continue; }
        QMap<QString, QString> row;
        for (int idx = 0; idx < columns.size(); ++idx) {
            const QString col = columns[idx];
            if (col.isEmpty() || col == "date") continue;
            const QString raw = cells.value(idx);
            const QString tm = normaliseTime(raw);
            if (!tm.isEmpty()) row[col] = tm;
            else if (!raw.isEmpty()) r.errors << QStringLiteral("line %1: unreadable time \"%2\"").arg(i + 1).arg(raw);
        }
        if (!row.isEmpty()) r.entries[iso] = row;
    }
    return r;
}

QJsonObject timetableToJson(const Timetable& t) {
    QJsonObject root;
    for (auto it = t.begin(); it != t.end(); ++it) {
        QJsonObject row;
        for (auto p = it->begin(); p != it->end(); ++p) row[p.key()] = p.value();
        root[it.key()] = row;
    }
    return root;
}

Timetable timetableFromJson(const QJsonObject& o) {
    Timetable t;
    for (auto it = o.begin(); it != o.end(); ++it) {
        const QJsonObject row = it->toObject();
        QMap<QString, QString> m;
        for (auto p = row.begin(); p != row.end(); ++p) m[p.key()] = p->toString();
        t[it.key()] = m;
    }
    return t;
}

}  // namespace Features
