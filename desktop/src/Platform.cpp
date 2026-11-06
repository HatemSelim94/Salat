#include "Platform.h"

#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QProcess>
#include <QStandardPaths>

#if defined(Q_OS_WIN)
#include <QSettings>
#include <windows.h>
#include <mmsystem.h>
#endif

namespace Platform {

namespace {
const QString kAppId = QStringLiteral("io.github.hatemselim94.salat");

QString exePath() { return QDir::toNativeSeparators(QCoreApplication::applicationFilePath()); }

#if defined(Q_OS_LINUX) || defined(Q_OS_FREEBSD)
QString autostartFile() {
    return QStandardPaths::writableLocation(QStandardPaths::ConfigLocation) + "/autostart/salat.desktop";
}
#elif defined(Q_OS_MACOS)
QString launchAgentFile() { return QDir::homePath() + "/Library/LaunchAgents/" + kAppId + ".plist"; }
#endif
}  // namespace

bool isAutostartEnabled() {
#if defined(Q_OS_WIN)
    QSettings run(QStringLiteral("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"), QSettings::NativeFormat);
    return run.contains(QStringLiteral("Salat"));
#elif defined(Q_OS_MACOS)
    return QFile::exists(launchAgentFile());
#else
    return QFile::exists(autostartFile());
#endif
}

void setAutostartEnabled(bool enabled) {
#if defined(Q_OS_WIN)
    QSettings run(QStringLiteral("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"), QSettings::NativeFormat);
    if (enabled) run.setValue(QStringLiteral("Salat"), '"' + exePath() + "\" --hidden");
    else run.remove(QStringLiteral("Salat"));
#elif defined(Q_OS_MACOS)
    const QString path = launchAgentFile();
    if (!enabled) { QFile::remove(path); return; }
    QDir().mkpath(QFileInfo(path).absolutePath());
    // The bundle executable is inside Salat.app/Contents/MacOS/.
    const QString plist = QStringLiteral(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
        "<plist version=\"1.0\"><dict>\n"
        "  <key>Label</key><string>%1</string>\n"
        "  <key>ProgramArguments</key><array><string>%2</string><string>--hidden</string></array>\n"
        "  <key>RunAtLoad</key><true/>\n"
        "</dict></plist>\n").arg(kAppId, exePath());
    QFile f(path);
    if (f.open(QIODevice::WriteOnly | QIODevice::Truncate)) f.write(plist.toUtf8());
#else
    const QString path = autostartFile();
    if (!enabled) { QFile::remove(path); return; }
    QDir().mkpath(QFileInfo(path).absolutePath());
    const QString desktop = QStringLiteral(
        "[Desktop Entry]\nType=Application\nName=Salat\nComment=Prayer-time reminders\n"
        "Exec=\"%1\" --hidden\nIcon=salat\nTerminal=false\nX-GNOME-Autostart-enabled=true\n").arg(exePath());
    QFile f(path);
    if (f.open(QIODevice::WriteOnly | QIODevice::Truncate)) f.write(desktop.toUtf8());
#endif
}

QString chimePath() {
    const QString dir = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(dir);
    const QString path = dir + "/chime.wav";
    if (!QFile::exists(path)) QFile::copy(QStringLiteral(":/Salat/resources/chime.wav"), path);
    return path;
}

void playSound(const QString& wavPath) {
#if defined(Q_OS_WIN)
    static std::wstring keepAlive;  // PlaySound with SND_ASYNC needs the buffer to outlive the call
    keepAlive = wavPath.toStdWString();
    PlaySoundW(keepAlive.c_str(), nullptr, SND_FILENAME | SND_ASYNC | SND_NODEFAULT);
#elif defined(Q_OS_MACOS)
    QProcess::startDetached(QStringLiteral("afplay"), {wavPath});
#else
    // Try PipeWire, then PulseAudio, then ALSA. All exit after the clip.
    for (const auto& player : {QStringLiteral("pw-play"), QStringLiteral("paplay"), QStringLiteral("aplay")}) {
        if (!QStandardPaths::findExecutable(player).isEmpty()) {
            QStringList args = {wavPath};
            if (player == "aplay") args.prepend(QStringLiteral("-q"));
            QProcess::startDetached(player, args);
            return;
        }
    }
#endif
}

}  // namespace Platform
