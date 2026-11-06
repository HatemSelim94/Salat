# Salat – Prayer Times for VS Code

A quiet prayer-time countdown in the status bar, and one notification at each prayer. Calculated offline from your coordinates; no network, no tracking, no sidebar, no webview.

```
 $(bell) Asr 14:30 · 1h 05m
```

Hover the item for today's full timetable; click it for a quick pick. Notifications are ordinary VS Code toasts with a **Snooze** button, so they are hidden automatically when you turn on VS Code's *Do Not Disturb* mode.

## Setup

1. Run **Salat: Set City** from the command palette (`Ctrl/Cmd+Shift+P`) and pick your city. Or set `salat.location.*` by hand with exact coordinates. Until you do, the location is guessed from your system time zone.
2. Optionally pick your calculation method (`salat.method`), Asr madhab, and per-prayer minute adjustments to match your local mosque.

## Settings

| Setting | Default | |
|---|---|---|
| `salat.location.latitude` / `longitude` / `timezone` / `label` | guessed | Your location |
| `salat.method` | `MuslimWorldLeague` | 12 methods incl. Egyptian, Umm al-Qura, Diyanet, ISNA, Karachi |
| `salat.madhab` | `shafi` | `hanafi` for a later Asr |
| `salat.highLatitudeRule` | `recommended` | Summer Fajr/Isha rule north of ~48° |
| `salat.adjustments` | `{}` | e.g. `{ "fajr": 3, "isha": -2 }` minutes |
| `salat.notifications.enabled` | `true` | |
| `salat.notifications.preReminderMinutes` | `0` | Heads-up before each prayer; 0 = off |
| `salat.notifications.snoozeMinutes` | `10` | |
| `salat.hour12` | `false` | |
| `salat.statusBar.enabled` | `true` | |
| `salat.statusBar.compact` | `false` | Hide the clock time, keep name + countdown |

## Behaviour

- Activates after VS Code has finished starting (`onStartupFinished`), so it adds nothing to startup time.
- The status bar refreshes once a minute, on the minute. One `setTimeout` is armed for the next prayer; a once-a-minute check catches timer drift after sleep. Events missed by more than 15 minutes (laptop lid closed) are skipped rather than fired late.
- Works only while a VS Code window is open. For reminders regardless of what is running, use the Salat desktop app.

Part of [Salat](https://github.com/HatemSelim94/salat). © Hatem Selim, all rights reserved; third-party notices in the repository.
