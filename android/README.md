# Salat – Android app (Kotlin / Jetpack Compose)

> **Status: partially verified.** The environment this was authored in could not reach Google's Maven repository, so the Android/Compose parts have never been built. What *has* been compiled and run: `core/Adhan.kt` + `AdhanTest` (all 43,200 reference times match), and `core/Engine.kt` / `core/Settings.kt` type-check against stubs. The Compose UI, receivers and manifest were reviewed by hand only — expect a few nits on first open in Android Studio.

## What it does

- Computes prayer times on-device with a Kotlin port of the adhan algorithm (`core/Adhan.kt`), a line-by-line translation of the C++ port in `desktop/`, which in turn is verified against the TypeScript core. `AdhanTest` checks the Kotlin port against the same 43,200 reference times.
- Schedules **one** exact alarm for the next event (`AlarmManager.setExactAndAllowWhileIdle`). When it fires, `AlarmReceiver` posts the notification and schedules the next one. `BootReceiver` re-arms after reboot, app update, time or time-zone change. Reminders missed by more than 15 minutes (phone off) are skipped, not fired late.
- Notification channels: *Prayer times* (silent), *Prayer times (with chime)*, *Heads-up before prayer*. The sound setting picks between the first two because a channel's sound cannot be changed after creation. Notifications carry a **Snooze** action and expire after an hour.
- Permissions: `USE_EXACT_ALARM` (13+; reminders are the app's core function), `SCHEDULE_EXACT_ALARM` (12 only), `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`. **No** `INTERNET`, no location permission.
- Home screen nudges the user once to pick a city, and — because OEMs kill background apps — to exclude Salat from battery optimisation (it opens the system list; the app does not request the restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission).

## Build

1. Open `android/` in Android Studio (Ladybug or newer). It will offer to generate the Gradle wrapper JAR if missing; or run `gradle wrapper` once.
2. `./gradlew :app:testDebugUnitTest` — runs `AdhanTest` against `src/test/resources/vectors.csv`.
3. `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

Dependencies are deliberately minimal: Compose BOM + Material 3, activity-compose, core-ktx, lifecycle-runtime-compose. No DataStore, no WorkManager, no navigation library. A release build with R8 should be well under 5 MB.

## Play Store notes (verify current policy before submitting)

- One-time registration fee; personal accounts have had to run a closed test with a minimum number of testers before production access — check the current rule.
- `USE_EXACT_ALARM` is reviewed: the listing should state that the app's core function is prayer-time reminders.
- Package name is `io.github.hatemselim94.salat`; change `applicationId` in `app/build.gradle.kts` before the first upload if you want something else, since it is permanent afterwards.

## Layout

```
core/Adhan.kt          algorithm (pure Kotlin)
core/Settings.kt       SharedPreferences-backed settings + bundled city list (assets/cities.json)
core/Engine.kt         day computation, next/current prayer, event list, formatting, Hijri date (Umm al-Qura via java.time)
notify/Scheduler.kt    single exact alarm, snooze, exact-alarm permission check
notify/AlarmReceiver.kt  fire → notify → reschedule; BootReceiver
notify/Notifications.kt  channels and the notification itself
ui/                    Compose: Theme (shared palette), SalatApp (home), SettingsScreen
```
