# Salat

Lightweight, offline, non-annoying prayer-time reminders — for your desktop, your browser, your editor and your phone.

| Target | Folder | Stack | Status |
|---|---|---|---|
| Shared engine | [`core/`](core) | TypeScript, [adhan-js](https://github.com/batoulapps/adhan-js) | ✅ tests passing |
| Chrome extension | [`chrome/`](chrome) | Manifest V3, vanilla TS | ✅ working, unpublished |
| VS Code extension | [`vscode/`](vscode) | VS Code API | 🚧 |
| Desktop app | [`desktop/`](desktop) | C++17, Qt 6 / QML | 🚧 |
| Android app | [`android/`](android) | Kotlin, Jetpack Compose | 🚧 |

## Principles

- **Light.** No Electron, no frameworks where a few hundred lines will do. Idle CPU is zero; memory is measured in single-digit megabytes.
- **Offline and private.** Prayer times are calculated on your device from coordinates. No network requests, no accounts, no telemetry, no ads.
- **Not annoying.** One notification per prayer, auto-dismissing. Pre-prayer warnings and sound are off until you turn them on. One-time location setup, then nothing is asked again. No new-tab hijacking, no badge animations, no "rate us".
- **Works in the background.** Tray icon / service worker / status bar / exact alarms — whatever each platform offers, used the way it was designed, with correct behaviour across sleep, wake and time-zone changes.

## Calculation

Times come from the astronomical algorithm implemented by the [adhan](https://github.com/batoulapps/adhan) family of libraries: 12 calculation methods (MWL, Egyptian, Umm al-Qura, Diyanet, ISNA, Karachi, …), Shafi/Hanafi Asr, high-latitude rules (automatic by default — relevant for northern Europe in summer), and per-prayer minute offsets so you can match your local mosque.

## Unsigned builds

The desktop builds are **not code-signed** (signing costs ~€100/year per platform for a free app). Expect a one-time warning:

- **Windows:** SmartScreen → "More info" → "Run anyway".
- **macOS:** the first launch is blocked; open *System Settings → Privacy & Security* and click **Open Anyway** next to Salat, then launch again.
- **Linux:** none.

All code is open source under the [MIT license](LICENSE); read it or build it yourself if you prefer.

## Development

```sh
npm install          # installs core, chrome and vscode workspaces
npm test             # core unit tests
npm run build        # builds core, chrome and vscode
```

The Qt and Android projects have their own build instructions in their folders.
