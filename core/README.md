# @salat/core

The shared prayer-time engine used by the Chrome and VS Code extensions. The Qt (C++) and Android (Kotlin) apps mirror this API and its settings schema with the adhan ports for their languages.

```ts
import { defaultSettings, computeDay, nextPrayer, upcomingEvents, formatTime } from '@salat/core';

const settings = defaultSettings({ latitude: 48.78, longitude: 9.18, timezone: 'Europe/Berlin', label: 'Stuttgart' });
const today = computeDay(settings, new Date());        // { fajr, sunrise, dhuhr, asr, maghrib, isha, date }
const next = nextPrayer(settings, new Date());         // { name: 'asr', time: Date }
const events = upcomingEvents(settings, new Date());   // what to schedule in the next 36 h
formatTime(today.fajr, settings.location.timezone, false); // "05:30"
```

- `settings.ts` — the settings schema (`Settings`), method/rule catalogues, `defaultSettings`, `mergeSettings`.
- `engine.ts` — `computeDay`, `nextPrayer`, `currentPrayer`, `upcomingEvents`, formatting helpers. Time-zone-correct regardless of the runtime's own zone.
- `cities.ts` — bundled city list for the picker and the zero-prompt default (`defaultLocationForTimezone(systemTimezone())`).

```sh
npm test -w core
```
