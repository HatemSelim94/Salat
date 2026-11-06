// Generates desktop/tests/vectors.csv from @salat/core (adhan-js) for the C++ port to match.
import { writeFileSync } from 'node:fs';
import { computeDay, defaultSettings, METHODS } from '../../core/dist/index.js';

const cities = [
  ['Stuttgart', 48.78, 9.18, 'Europe/Berlin'], ['Cairo', 30.04, 31.24, 'Africa/Cairo'], ['Makkah', 21.39, 39.86, 'Asia/Riyadh'],
  ['Erbil', 36.19, 44.01, 'Asia/Baghdad'], ['Stockholm', 59.33, 18.07, 'Europe/Stockholm'], ['Oslo', 59.91, 10.75, 'Europe/Oslo'],
  ['Jakarta', -6.21, 106.85, 'Asia/Jakarta'], ['Sydney', -33.87, 151.21, 'Australia/Sydney'], ['New York', 40.71, -74.01, 'America/New_York'],
  ['Tokyo', 35.68, 139.69, 'Asia/Tokyo'], ['Auckland', -36.85, 174.76, 'Pacific/Auckland'], ['Reykjavik', 64.15, -21.94, 'Atlantic/Reykjavik'],
];
const rows = ['city,lat,lng,date,method,madhab,rule,fajr,sunrise,dhuhr,asr,maghrib,isha'];
for (const [name, lat, lng, tz] of cities) {
  for (let doy = 0; doy < 365; doy += 37) {
    const d = new Date(Date.UTC(2026, 0, 1 + doy, 12));
    const date = d.toISOString().slice(0, 10);
    for (const m of METHODS) {
      for (const madhab of ['shafi', 'hanafi']) {
        for (const rule of ['recommended', 'middleOfTheNight', 'seventhOfTheNight', 'twilightAngle']) {
          if (madhab === 'hanafi' && rule !== 'recommended') continue; // madhab only affects asr
          const s = defaultSettings({ latitude: lat, longitude: lng, timezone: tz, label: name });
          s.method = m.id; s.madhab = madhab; s.highLatitudeRule = rule;
          s.adjustments = { fajr: 2, isha: -3 };
          // Use noon UTC of that civil date; computeDay derives the civil date in tz. To keep the
          // civil date unambiguous for the C++ side we pass the tz-local civil date explicitly.
          const t = computeDay(s, new Date(`${date}T12:00:00Z`));
          const e = (x) => Math.floor(x.getTime() / 1000);
          rows.push([name, lat, lng, t.date, m.id, madhab, rule, e(t.fajr), e(t.sunrise), e(t.dhuhr), e(t.asr), e(t.maghrib), e(t.isha)].join(','));
        }
      }
    }
  }
}
writeFileSync(new URL('./vectors.csv', import.meta.url), rows.join('\n') + '\n');
console.log(rows.length - 1, 'vectors');
