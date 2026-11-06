#include "Tray.h"

#include <QApplication>
#include <QIcon>
#include <QQuickWindow>
#include <QTimer>

#include "Engine.h"
#include "Tr.h"

Tray::Tray(Engine* engine, QQuickWindow* window, QObject* parent)
    : QObject(parent), m_engine(engine), m_window(window), m_icon(QIcon(QStringLiteral(":/Salat/resources/icons/icon256.png"))) {
    m_nextAction = m_menu.addAction(engine->trayText());
    m_nextAction->setEnabled(false);
    m_menu.addSeparator();
    m_openAction = m_menu.addAction(Tr::instance().t("tray.open"), this, &Tray::showWindow);
    m_snoozeAction = m_menu.addAction(Tr::instance().t("tray.snoozeLast"), engine, &Engine::snooze);
    m_snoozeAction->setVisible(false);
    m_menu.addSeparator();
    m_quitAction = m_menu.addAction(Tr::instance().t("tray.quit"), qApp, &QApplication::quit);
    connect(&Tr::instance(), &Tr::changed, this, [this] {
        m_openAction->setText(Tr::instance().t("tray.open"));
        m_snoozeAction->setText(Tr::instance().t("tray.snoozeLast"));
        m_quitAction->setText(Tr::instance().t("tray.quit"));
    });
    m_icon.setContextMenu(&m_menu);
    m_icon.setToolTip(engine->trayText());

    connect(&m_icon, &QSystemTrayIcon::activated, this, [this](QSystemTrayIcon::ActivationReason r) {
        if (r == QSystemTrayIcon::Trigger || r == QSystemTrayIcon::DoubleClick) toggleWindow();
    });
    connect(&m_icon, &QSystemTrayIcon::messageClicked, this, &Tray::showWindow);
    connect(engine, &Engine::updated, this, [this] {
        m_icon.setToolTip(m_engine->trayText());
        m_nextAction->setText(m_engine->trayText());
    });
    connect(engine, &Engine::notify, this, &Tray::showNotification);

    if (available()) m_icon.show();
}

void Tray::showNotification(const QString& title, const QString& message, bool offerSnooze) {
    if (available()) {
        m_icon.showMessage(title, message, QSystemTrayIcon::NoIcon, 10000);
    } else if (m_window) {
        // No tray (unusual desktop): bring the window up instead so the reminder is not lost.
        showWindow();
    }
    m_snoozeAction->setVisible(offerSnooze);
    if (offerSnooze) QTimer::singleShot(30 * 60 * 1000, this, [this] { m_snoozeAction->setVisible(false); });
}

void Tray::showWindow() {
    if (!m_window) return;
    m_window->show();
    m_window->raise();
    m_window->requestActivate();
}

void Tray::toggleWindow() {
    if (!m_window) return;
    if (m_window->isVisible() && m_window->isActive()) m_window->hide();
    else showWindow();
}
