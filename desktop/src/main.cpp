#include <QApplication>
#include <QCommandLineParser>
#include <QGuiApplication>
#include <QIcon>
#include <QLockFile>
#include <QPalette>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>
#include <QQuickWindow>
#include <QStandardPaths>
#include <QTimer>
#include <QDir>

#include "Engine.h"
#include "Settings.h"
#include "Tray.h"
#include "Tr.h"
#include "About.h"

int main(int argc, char* argv[]) {
    QApplication app(argc, argv);  // QApplication (not QGuiApplication) for QSystemTrayIcon + QMenu
    app.setApplicationName(QStringLiteral("salat"));
    app.setApplicationDisplayName(QStringLiteral("Salat"));
    app.setOrganizationName(QStringLiteral("salat"));
    app.setApplicationVersion(QStringLiteral(SALAT_VERSION));
    app.setWindowIcon(QIcon(QStringLiteral(":/Salat/resources/icons/icon256.png")));
    app.setQuitOnLastWindowClosed(false);  // closing the window keeps the tray running

    QCommandLineParser parser;
    parser.setApplicationDescription(QStringLiteral("Salat – prayer-time reminders"));
    parser.addHelpOption();
    parser.addVersionOption();
    const QCommandLineOption hidden(QStringLiteral("hidden"), QStringLiteral("Start in the tray without showing the window."));
    const QCommandLineOption screenshot(QStringLiteral("screenshot"), QStringLiteral("Render the window to PNG and exit (for docs/tests)."), QStringLiteral("file"));
    const QCommandLineOption dark(QStringLiteral("dark"), QStringLiteral("Force the dark theme."));
    const QCommandLineOption light(QStringLiteral("light"), QStringLiteral("Force the light theme."));
    const QCommandLineOption page(QStringLiteral("page"), QStringLiteral("Open a page at start: home or settings."), QStringLiteral("name"), QStringLiteral("home"));
    const QCommandLineOption size(QStringLiteral("size"), QStringLiteral("Initial window size WxH (for screenshots)."), QStringLiteral("WxH"));
    parser.addOptions({hidden, screenshot, dark, light, page, size});
    parser.process(app);

    // Single instance: a second launch just asks the first to show its window (via the lock file's presence).
    QDir().mkpath(QStandardPaths::writableLocation(QStandardPaths::AppDataLocation));
    QLockFile lock(QStandardPaths::writableLocation(QStandardPaths::AppDataLocation) + "/salat.lock");
    if (!lock.tryLock(100) && !parser.isSet(screenshot)) return 0;

    QQuickStyle::setStyle(QStringLiteral("Material"));

    Settings settings;
    Tr::instance().setPreference(settings.language());
    QObject::connect(&settings, &Settings::changed, &app, [&settings] { Tr::instance().setPreference(settings.language()); });
    Engine engine(&settings);

    const bool systemDark = QGuiApplication::palette().color(QPalette::Window).lightness() < 128;
    const bool useDark = parser.isSet(dark) ? true : parser.isSet(light) ? false : systemDark;

    QQmlApplicationEngine qml;
    qml.rootContext()->setContextProperty(QStringLiteral("engine"), &engine);
    qml.rootContext()->setContextProperty(QStringLiteral("settingsObj"), &settings);
    qml.rootContext()->setContextProperty(QStringLiteral("Tr"), &Tr::instance());
    About about;
    qml.rootContext()->setContextProperty(QStringLiteral("About"), &about);
    qml.rootContext()->setContextProperty(QStringLiteral("useDarkTheme"), useDark);
    qml.rootContext()->setContextProperty(QStringLiteral("initialPage"), parser.value(page));
    qml.addImportPath(QStringLiteral("qrc:/"));
    qml.load(QUrl(QStringLiteral("qrc:/Salat/qml/Main.qml")));
    if (qml.rootObjects().isEmpty()) return 1;

    auto* window = qobject_cast<QQuickWindow*>(qml.rootObjects().first());
    if (parser.isSet(size)) {
        const auto parts = parser.value(size).split('x');
        if (parts.size() == 2) window->resize(parts[0].toInt(), parts[1].toInt());
    }
    Tray tray(&engine, window);

    const bool startHidden = parser.isSet(hidden) || (settings.startHidden() && tray.available() && !parser.isSet(screenshot));
    if (!startHidden) tray.showWindow();

    if (parser.isSet(screenshot)) {
        const QString file = parser.value(screenshot);
        QTimer::singleShot(600, &app, [window, file] {
            window->grabWindow().save(file);
            QCoreApplication::quit();
        });
    }
    return app.exec();
}
