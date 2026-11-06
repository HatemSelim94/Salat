#pragma once

#include <QString>

/// Small per-OS helpers: autostart registration and sound playback.
/// Deliberately no QtMultimedia — the chime is played with whatever the OS has.
namespace Platform {

bool isAutostartEnabled();
void setAutostartEnabled(bool enabled);

/// Play a WAV file asynchronously. Best effort; silently does nothing if no player is available.
void playSound(const QString& wavPath);

/// Where the chime lives on disk (extracted from resources on first use).
QString chimePath();

}  // namespace Platform
