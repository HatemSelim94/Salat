#pragma once

#include <QAction>
#include <QMenu>
#include <QObject>
#include <QSystemTrayIcon>

class Engine;
class QQuickWindow;

/// System tray icon, its menu, and native notifications via the tray.
class Tray : public QObject {
    Q_OBJECT
public:
    Tray(Engine* engine, QQuickWindow* window, QObject* parent = nullptr);
    bool available() const { return QSystemTrayIcon::isSystemTrayAvailable(); }

public slots:
    void showWindow();
    void toggleWindow();

private:
    void showNotification(const QString& title, const QString& message, bool offerSnooze);

    Engine* m_engine;
    QQuickWindow* m_window;
    QSystemTrayIcon m_icon;
    QMenu m_menu;
    QAction* m_nextAction = nullptr;
    QAction* m_snoozeAction = nullptr;
};
