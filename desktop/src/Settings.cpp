#include "Settings.h"

#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QTimeZone>

#include "Platform.h"
#include "Features.h"
#include <QDir>
#include <QStandardPaths>

namespace {
const QString kLat = QStringLiteral("location/latitude");
const QString kLng = QStringLiteral("location/longitude");
const QString kTz = QStringLiteral("location/timezone");
const QString kLabel = QStringLiteral("location/label");
const QString kConfigured = QStringLiteral("location/configured");

/// Zero-prompt default: first bundled city in the system time zone, else Makkah.
struct DefaultLocation { double lat, lng; QString tz, label; };
DefaultLocation defaultLocation() {
    const QString tz = QString::fromUtf8(QTimeZone::systemTimeZoneId());
    QFile f(QStringLiteral(":/Salat/resources/cities.json"));
    if (f.open(QIODevice::ReadOnly)) {
        const auto arr = QJsonDocument::fromJson(f.readAll()).array();
        for (const auto& v : arr) {
            const auto o = v.toObject();
            if (o.value("tz").toString() == tz) {
                return {o.value("lat").toDouble(), o.value("lng").toDouble(), tz,
                        o.value("name").toString() + ", " + o.value("country").toString()};
            }
        }
    }
    return {21.39, 39.86, QStringLiteral("Asia/Riyadh"), QStringLiteral("Makkah, SA")};
}
}  // namespace

QString Settings::timetablePath() const {
    return QStandardPaths::writableLocation(QStandardPaths::AppDataLocation) + "/timetable.json";
}

Settings::Settings(QObject* parent) : QObject(parent), m_s(QStringLiteral("salat"), QStringLiteral("salat")) {
    QFile tf(timetablePath());
    if (tf.open(QIODevice::ReadOnly)) m_timetable = Features::timetableFromJson(QJsonDocument::fromJson(tf.readAll()).object());
    if (!m_s.contains(kLat)) {
        const auto d = defaultLocation();
        m_s.setValue(kLat, d.lat);
        m_s.setValue(kLng, d.lng);
        m_s.setValue(kTz, d.tz);
        m_s.setValue(kLabel, d.label);
        m_s.setValue(kConfigured, false);
    }
}

template <typename T> void Settings::put(const QString& key, const T& value) {
    if (m_s.value(key) == QVariant::fromValue(value)) return;
    m_s.setValue(key, value);
    m_s.sync();
    emit changed();
}

double Settings::latitude() const { return m_s.value(kLat).toDouble(); }
double Settings::longitude() const { return m_s.value(kLng).toDouble(); }
QString Settings::timezone() const { return m_s.value(kTz).toString(); }
QString Settings::label() const { return m_s.value(kLabel).toString(); }
bool Settings::locationConfigured() const { return m_s.value(kConfigured, false).toBool(); }
QString Settings::method() const { return m_s.value("calc/method", QStringLiteral("MuslimWorldLeague")).toString(); }
QString Settings::madhab() const { return m_s.value("calc/madhab", QStringLiteral("shafi")).toString(); }
QString Settings::highLatitudeRule() const { return m_s.value("calc/highLatitudeRule", QStringLiteral("recommended")).toString(); }
bool Settings::notificationsEnabled() const { return m_s.value("notify/enabled", true).toBool(); }
int Settings::preReminderMinutes() const { return m_s.value("notify/preReminderMinutes", 0).toInt(); }
bool Settings::sound() const { return m_s.value("notify/sound", true).toBool(); }
int Settings::snoozeMinutes() const { return m_s.value("notify/snoozeMinutes", 10).toInt(); }
bool Settings::hour12() const { return m_s.value("ui/hour12", false).toBool(); }
bool Settings::autostart() const { return Platform::isAutostartEnabled(); }
bool Settings::startHidden() const { return m_s.value("ui/startHidden", true).toBool(); }
QString Settings::language() const { return m_s.value("ui/language", QStringLiteral("auto")).toString(); }
bool Settings::jumuahEnabled() const { return m_s.value("jumuah/enabled", true).toBool(); }
int Settings::jumuahReminderMinutes() const { return m_s.value("jumuah/reminder", 0).toInt(); }
QString Settings::ramadan() const { return m_s.value("ramadan", QStringLiteral("auto")).toString(); }
bool Settings::quietHoursEnabled() const { return m_s.value("quiet/enabled", false).toBool(); }
QString Settings::quietFrom() const { return m_s.value("quiet/from", QStringLiteral("22:00")).toString(); }
QString Settings::quietTo() const { return m_s.value("quiet/to", QStringLiteral("06:00")).toString(); }
int Settings::iqamah(const QString& prayer) const { return m_s.value("iqamah/" + prayer, 0).toInt(); }
void Settings::setIqamah(const QString& prayer, int minutes) { put("iqamah/" + prayer, qBound(0, minutes, 120)); }
void Settings::setJumuahEnabled(bool v) { put(QStringLiteral("jumuah/enabled"), v); }
void Settings::setJumuahReminderMinutes(int v) { put(QStringLiteral("jumuah/reminder"), qBound(0, v, 180)); }
void Settings::setRamadan(const QString& v) { put(QStringLiteral("ramadan"), v); }
void Settings::setQuietHoursEnabled(bool v) { put(QStringLiteral("quiet/enabled"), v); }
void Settings::setQuietFrom(const QString& v) { if (Features::minutesOfDay(v)) put(QStringLiteral("quiet/from"), v); }
void Settings::setQuietTo(const QString& v) { if (Features::minutesOfDay(v)) put(QStringLiteral("quiet/to"), v); }

void Settings::setTimetable(const QMap<QString, QMap<QString, QString>>& t) {
    m_timetable = t;
    QDir().mkpath(QFileInfo(timetablePath()).absolutePath());
    if (t.isEmpty()) QFile::remove(timetablePath());
    else {
        QFile f(timetablePath());
        if (f.open(QIODevice::WriteOnly | QIODevice::Truncate)) f.write(QJsonDocument(Features::timetableToJson(t)).toJson(QJsonDocument::Compact));
    }
    emit changed();
}

void Settings::setLatitude(double v) { put(kLat, v); }
void Settings::setLongitude(double v) { put(kLng, v); }
void Settings::setTimezone(const QString& v) {
    if (QTimeZone(v.toUtf8()).isValid()) put(kTz, v);
}
void Settings::setLabel(const QString& v) { put(kLabel, v); }
void Settings::setMethod(const QString& v) { put(QStringLiteral("calc/method"), v); }
void Settings::setMadhab(const QString& v) { put(QStringLiteral("calc/madhab"), v); }
void Settings::setHighLatitudeRule(const QString& v) { put(QStringLiteral("calc/highLatitudeRule"), v); }
void Settings::setNotificationsEnabled(bool v) { put(QStringLiteral("notify/enabled"), v); }
void Settings::setPreReminderMinutes(int v) { put(QStringLiteral("notify/preReminderMinutes"), qBound(0, v, 60)); }
void Settings::setSound(bool v) { put(QStringLiteral("notify/sound"), v); }
void Settings::setSnoozeMinutes(int v) { put(QStringLiteral("notify/snoozeMinutes"), qBound(1, v, 60)); }
void Settings::setHour12(bool v) { put(QStringLiteral("ui/hour12"), v); }
void Settings::setStartHidden(bool v) { put(QStringLiteral("ui/startHidden"), v); }
void Settings::setLanguage(const QString& v) { put(QStringLiteral("ui/language"), v); }
void Settings::setAutostart(bool v) {
    if (Platform::isAutostartEnabled() == v) return;
    Platform::setAutostartEnabled(v);
    emit changed();
}

int Settings::adjustment(const QString& prayer) const { return m_s.value("adjust/" + prayer, 0).toInt(); }
void Settings::setAdjustment(const QString& prayer, int minutes) { put("adjust/" + prayer, qBound(-60, minutes, 60)); }

void Settings::setLocation(double lat, double lng, const QString& tz, const QString& label) {
    m_s.setValue(kLat, lat);
    m_s.setValue(kLng, lng);
    if (QTimeZone(tz.toUtf8()).isValid()) m_s.setValue(kTz, tz);
    m_s.setValue(kLabel, label);
    m_s.setValue(kConfigured, true);
    m_s.sync();
    emit changed();
}
