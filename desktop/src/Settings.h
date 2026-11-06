#pragma once

#include <QObject>
#include <QSettings>
#include <QString>
#include <QVariantMap>
#include <qqml.h>

/// Persistent settings, mirroring @salat/core's schema. Backed by QSettings
/// (registry on Windows, plist on macOS, ~/.config/salat/salat.conf on Linux).
class Settings : public QObject {
    Q_OBJECT

    Q_PROPERTY(double latitude READ latitude WRITE setLatitude NOTIFY changed)
    Q_PROPERTY(double longitude READ longitude WRITE setLongitude NOTIFY changed)
    Q_PROPERTY(QString timezone READ timezone WRITE setTimezone NOTIFY changed)
    Q_PROPERTY(QString label READ label WRITE setLabel NOTIFY changed)
    Q_PROPERTY(bool locationConfigured READ locationConfigured NOTIFY changed)
    Q_PROPERTY(QString method READ method WRITE setMethod NOTIFY changed)
    Q_PROPERTY(QString madhab READ madhab WRITE setMadhab NOTIFY changed)
    Q_PROPERTY(QString highLatitudeRule READ highLatitudeRule WRITE setHighLatitudeRule NOTIFY changed)
    Q_PROPERTY(bool notificationsEnabled READ notificationsEnabled WRITE setNotificationsEnabled NOTIFY changed)
    Q_PROPERTY(int preReminderMinutes READ preReminderMinutes WRITE setPreReminderMinutes NOTIFY changed)
    Q_PROPERTY(bool sound READ sound WRITE setSound NOTIFY changed)
    Q_PROPERTY(int snoozeMinutes READ snoozeMinutes WRITE setSnoozeMinutes NOTIFY changed)
    Q_PROPERTY(bool hour12 READ hour12 WRITE setHour12 NOTIFY changed)
    Q_PROPERTY(bool autostart READ autostart WRITE setAutostart NOTIFY changed)
    Q_PROPERTY(bool startHidden READ startHidden WRITE setStartHidden NOTIFY changed)
    Q_PROPERTY(QString language READ language WRITE setLanguage NOTIFY changed)

public:
    explicit Settings(QObject* parent = nullptr);

    double latitude() const;
    double longitude() const;
    QString timezone() const;
    QString label() const;
    bool locationConfigured() const;
    QString method() const;
    QString madhab() const;
    QString highLatitudeRule() const;
    bool notificationsEnabled() const;
    int preReminderMinutes() const;
    bool sound() const;
    int snoozeMinutes() const;
    bool hour12() const;
    bool autostart() const;
    bool startHidden() const;
    QString language() const;

    void setLatitude(double v);
    void setLongitude(double v);
    void setTimezone(const QString& v);
    void setLabel(const QString& v);
    void setMethod(const QString& v);
    void setMadhab(const QString& v);
    void setHighLatitudeRule(const QString& v);
    void setNotificationsEnabled(bool v);
    void setPreReminderMinutes(int v);
    void setSound(bool v);
    void setSnoozeMinutes(int v);
    void setHour12(bool v);
    void setAutostart(bool v);
    void setStartHidden(bool v);
    void setLanguage(const QString& v);

    /// Per-prayer minute offsets: keys fajr, sunrise, dhuhr, asr, maghrib, isha.
    Q_INVOKABLE int adjustment(const QString& prayer) const;
    Q_INVOKABLE void setAdjustment(const QString& prayer, int minutes);

    /// Set all location fields at once (from the city picker) and mark as configured.
    Q_INVOKABLE void setLocation(double lat, double lng, const QString& tz, const QString& label);

signals:
    void changed();

private:
    template <typename T> void put(const QString& key, const T& value);
    QSettings m_s;
};
