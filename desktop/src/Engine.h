#pragma once

#include <QDateTime>
#include <QObject>
#include <QString>
#include <QTimer>
#include <QVariantList>
#include <qqml.h>

#include <vector>

class Settings;

/// Computes prayer times from Settings, exposes them to QML, and schedules
/// reminders. Owns the only timers in the app: one single-shot for the next
/// event and one 60 s tick for the countdown / sleep-wake detection.
class Engine : public QObject {
    Q_OBJECT

    /// [{ key, label, time, epoch, state: "passed"|"current"|"upcoming" }] for today.
    Q_PROPERTY(QVariantList today READ today NOTIFY updated)
    Q_PROPERTY(QString nextKey READ nextKey NOTIFY updated)
    Q_PROPERTY(QString nextLabel READ nextLabel NOTIFY updated)
    Q_PROPERTY(QString nextTime READ nextTime NOTIFY updated)
    Q_PROPERTY(bool nextIsTomorrow READ nextIsTomorrow NOTIFY updated)
    Q_PROPERTY(QString countdown READ countdown NOTIFY updated)
    Q_PROPERTY(QString gregorianDate READ gregorianDate NOTIFY updated)
    Q_PROPERTY(QString hijriDate READ hijriDate NOTIFY updated)
    Q_PROPERTY(QString methodLabel READ methodLabel NOTIFY updated)
    Q_PROPERTY(QString trayText READ trayText NOTIFY updated)

public:
    enum class Prayer { Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha };

    struct Event {
        bool pre = false;   // heads-up before the prayer
        Prayer prayer = Prayer::Fajr;
        qint64 at = 0;      // UTC epoch seconds
    };
    struct DayTimes { qint64 fajr, sunrise, dhuhr, asr, maghrib, isha; QString date; };

    explicit Engine(Settings* settings, QObject* parent = nullptr);

    QVariantList today() const { return m_today; }
    QString nextKey() const;
    QString nextLabel() const;
    QString nextTime() const;
    bool nextIsTomorrow() const { return m_nextIsTomorrow; }
    QString countdown() const;
    QString gregorianDate() const;
    QString hijriDate() const;
    QString methodLabel() const;
    QString trayText() const;

    /// Formats an epoch in the configured time zone and clock style.
    Q_INVOKABLE QString formatTime(qint64 epoch) const;
    /// Preview of today's times for the settings page (label → time), using current settings.
    Q_INVOKABLE QVariantList previewToday() const;
    /// Fire a sample notification now.
    Q_INVOKABLE void testNotification();
    /// Snooze the most recent prayer notification.
    Q_INVOKABLE void snooze();

    static QString labelFor(Prayer p);
    static QString keyFor(Prayer p);

signals:
    void updated();
    /// Ask the UI/tray layer to show a notification.
    void notify(const QString& title, const QString& message, bool offerSnooze);

public slots:
    void recompute();

private:
    DayTimes computeDay(qint64 instant, int dayOffset) const;
    void rebuildToday();
    void rebuildQueue();
    void armNext();
    void onEventTimer();
    void onTick();
    void drainDue(qint64 nowEpoch);
    void fire(const Event& e, bool snoozed = false);
    qint64 nowEpoch() const { return QDateTime::currentSecsSinceEpoch(); }

    Settings* m_settings;
    QVariantList m_today;
    QString m_nextKey;
    qint64 m_nextAt = 0;
    bool m_nextIsTomorrow = false;
    std::vector<Event> m_queue;
    QTimer m_eventTimer;
    QTimer m_tick;
    qint64 m_lastTick = 0;
    QByteArray m_lastSystemTz;
    Event m_lastFired;
    bool m_hasLastFired = false;
};
