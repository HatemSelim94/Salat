# Salat – desktop app (Qt 6 / C++17)

A tray application: one small window with today's times and a countdown, a native notification at each prayer, optional chime, autostart. No Electron, no web view, no network.

| | |
|---|---|
| Binary | ~0.5 MB (plus the Qt libraries the installer ships) |
| Idle | 0 % CPU; ~55 MB RSS on Linux with the QML engine loaded |
| Timers | one single-shot for the next event, one 60 s tick |
| Sleep/wake | a tick gap > 150 s or a system time-zone change triggers a full recompute; reminders missed by > 15 min are skipped, not fired late |

## Build

Requires Qt 6.4+ (Core, Gui, Qml, Quick, QuickControls2, Widgets) and CMake 3.21+.

```sh
cd desktop
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build -j
ctest --test-dir build          # verifies the C++ adhan port against core's vectors
./build/salat                   # Linux; Salat.app on macOS; salat.exe on Windows
```

Useful flags: `--hidden` (start in the tray), `--light` / `--dark` (force theme), `--screenshot out.png`, `--page settings`, `--size 360x600`.

Deploy with `windeployqt` / `macdeployqt` or your distro's Qt packages. The builds are unsigned — see the root README for the one-time warnings on Windows and macOS.

## Layout

```
src/adhan/     dependency-free C++ port of adhan-js (algorithm only)
src/Engine     computation, scheduling, countdown, notifications
src/Settings   QSettings-backed settings mirroring core's schema
src/Tray       QSystemTrayIcon + menu + native notifications
src/Platform   autostart (registry / LaunchAgent / XDG autostart) and chime playback
qml/           Material-style UI: HomePage, SettingsPage, Theme singleton
resources/     icons, chime.wav, cities.json (exported from core by tools/export_cities.mjs)
tests/         adhan_test.cpp + vectors.csv generated from @salat/core
```

## Verification

`tests/vectors.csv` holds 7,200 prayer-time sets (43,200 individual times) computed by the TypeScript core across 12 cities from Jakarta to Reykjavik, 10 dates through the year, all 12 methods, both madhabs and all high-latitude rules. The C++ port reproduces every one of them to the second.

## Notes

- Sound is played with the OS: `PlaySound` on Windows, `afplay` on macOS, `pw-play`/`paplay`/`aplay` on Linux. No QtMultimedia dependency.
- The Hijri date uses Qt's tabular Islamic calendar, which can differ by a day from Umm al-Qura or local sighting.
- macOS: the bundle is a menu-bar app (`LSUIElement`), so there is no Dock icon; open the window from the menu-bar icon.
- Autostart writes `HKCU\…\Run` on Windows, `~/Library/LaunchAgents/io.github.hatemselim94.salat.plist` on macOS, `~/.config/autostart/salat.desktop` on Linux.
