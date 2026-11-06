import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  computeDay,
  nextPrayer,
  currentPrayer,
  upcomingEvents,
  defaultSettings,
  defaultLocationForTimezone,
  searchCities,
  formatCountdown,
  formatTime,
  civilDate,
  mergeSettings,
} from '../dist/index.js';

const STUTTGART = { latitude: 48.78, longitude: 9.18, timezone: 'Europe/Berlin', label: 'Stuttgart' };
const CAIRO = { latitude: 30.04, longitude: 31.24, timezone: 'Africa/Cairo', label: 'Cairo' };

const hhmm = (d, tz) => formatTime(d, tz, false);
const minutesInTz = (d, tz) => {
  const [h, m] = hhmm(d, tz).split(':').map(Number);
  return h * 60 + m;
};

test('times are strictly ordered and on the requested civil date', () => {
  const s = defaultSettings(STUTTGART);
  const day = computeDay(s, new Date('2026-06-21T10:00:00Z'));
  assert.equal(day.date, '2026-06-21');
  const seq = [day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha];
  for (let i = 1; i < seq.length; i++) assert.ok(seq[i] > seq[i - 1], `order broken at index ${i}`);
  for (const t of seq) assert.equal(civilDate(t, STUTTGART.timezone).day, 21);
});

test('Dhuhr lands near solar noon (sanity check against astronomy)', () => {
  // Solar noon ≈ 12:00 UTC − longitude×4 min + equation of time (|EoT| ≤ ~17 min).
  const s = defaultSettings(STUTTGART);
  const day = computeDay(s, new Date('2026-11-06T10:00:00Z'));
  const dhuhrUtcMin = day.dhuhr.getUTCHours() * 60 + day.dhuhr.getUTCMinutes();
  const expected = 12 * 60 - STUTTGART.longitude * 4; // ≈ 11:23 UTC before EoT
  assert.ok(Math.abs(dhuhrUtcMin - expected) < 25, `dhuhr ${dhuhrUtcMin} vs ${expected}`);
});

test('Cairo Dhuhr falls in the local early afternoon', () => {
  const s = defaultSettings(CAIRO);
  s.method = 'Egyptian';
  const day = computeDay(s, new Date('2026-03-15T08:00:00Z'));
  const m = minutesInTz(day.dhuhr, CAIRO.timezone);
  assert.ok(m > 11 * 60 + 40 && m < 12 * 60 + 20, hhmm(day.dhuhr, CAIRO.timezone));
});

test('high-latitude rule keeps Stuttgart midsummer Isha before midnight', () => {
  const s = defaultSettings(STUTTGART);
  const day = computeDay(s, new Date('2026-06-21T10:00:00Z'));
  const isha = minutesInTz(day.isha, STUTTGART.timezone);
  assert.ok(isha < 24 * 60 && isha > 21 * 60, hhmm(day.isha, STUTTGART.timezone));
  assert.ok(day.isha > day.maghrib);
});

test('nextPrayer wraps to tomorrow Fajr after Isha', () => {
  const s = defaultSettings(STUTTGART);
  const day = computeDay(s, new Date('2026-11-06T10:00:00Z'));
  const afterIsha = new Date(day.isha.getTime() + 60_000);
  const n = nextPrayer(s, afterIsha);
  assert.equal(n.name, 'fajr');
  assert.equal(civilDate(n.time, STUTTGART.timezone).day, 7);
  const c = currentPrayer(s, afterIsha);
  assert.equal(c.name, 'isha');
});

test('currentPrayer before Fajr is yesterday Isha', () => {
  const s = defaultSettings(STUTTGART);
  const day = computeDay(s, new Date('2026-11-06T10:00:00Z'));
  const beforeFajr = new Date(day.fajr.getTime() - 60_000);
  const c = currentPrayer(s, beforeFajr);
  assert.equal(c.name, 'isha');
  assert.equal(civilDate(c.time, STUTTGART.timezone).day, 5);
});

test('upcomingEvents emits 5 prayers per 24h, plus pre-reminders when enabled', () => {
  const s = defaultSettings(STUTTGART);
  const now = new Date('2026-11-06T00:30:00Z');
  const plain = upcomingEvents(s, now, 24 * 3600_000);
  assert.equal(plain.filter((e) => e.kind === 'prayer').length, 5);
  assert.equal(plain.filter((e) => e.kind === 'pre').length, 0);
  for (let i = 1; i < plain.length; i++) assert.ok(plain[i].at >= plain[i - 1].at);

  s.notifications.preReminderMinutes = 15;
  const withPre = upcomingEvents(s, now, 24 * 3600_000);
  assert.equal(withPre.filter((e) => e.kind === 'pre').length, 5);
  const fajrPre = withPre.find((e) => e.kind === 'pre' && e.name === 'fajr');
  const fajr = withPre.find((e) => e.kind === 'prayer' && e.name === 'fajr');
  assert.equal(fajr.at.getTime() - fajrPre.at.getTime(), 15 * 60_000);
});

test('per-prayer adjustments shift only that prayer', () => {
  const s = defaultSettings(STUTTGART);
  const base = computeDay(s, new Date('2026-11-06T10:00:00Z'));
  s.adjustments = { fajr: 5 };
  const adj = computeDay(s, new Date('2026-11-06T10:00:00Z'));
  assert.equal(adj.fajr.getTime() - base.fajr.getTime(), 5 * 60_000);
  assert.equal(adj.dhuhr.getTime(), base.dhuhr.getTime());
});

test('default location from time zone and city search', () => {
  const loc = defaultLocationForTimezone('Europe/Berlin');
  assert.ok(loc && loc.timezone === 'Europe/Berlin');
  assert.equal(defaultLocationForTimezone('Antarctica/Troll'), null);
  assert.equal(searchCities('stutt')[0].name, 'Stuttgart');
  assert.equal(searchCities('MALMO')[0].name, 'Malmö');
  assert.ok(searchCities('eg').some((c) => c.country === 'EG'));
});

test('formatting helpers', () => {
  const now = new Date('2026-11-06T10:00:00Z');
  assert.equal(formatCountdown(now, new Date(now.getTime() + 125 * 60_000)), '2h 05m');
  assert.equal(formatCountdown(now, new Date(now.getTime() + 14 * 60_000)), '14m');
  assert.equal(formatCountdown(now, new Date(now.getTime() + 10_000)), '<1m');
  assert.equal(formatTime(now, 'Europe/Berlin', false), '11:00');
});

test('mergeSettings fills gaps and keeps stored values', () => {
  const fb = defaultSettings(STUTTGART);
  const merged = mergeSettings({ method: 'Turkey', notifications: { sound: true } }, fb);
  assert.equal(merged.method, 'Turkey');
  assert.equal(merged.notifications.sound, true);
  assert.equal(merged.notifications.snoozeMinutes, 10);
  assert.equal(merged.location.label, 'Stuttgart');
});

test('i18n: resolve, translate, placeholders, fallback', async () => {
  const { t, resolveLanguage, prayerLabel, isRtl } = await import('../dist/index.js');
  assert.equal(resolveLanguage('auto', 'de-DE'), 'de');
  assert.equal(resolveLanguage('auto', 'ar-EG'), 'ar');
  assert.equal(resolveLanguage('auto', 'fr-FR'), 'en');
  assert.equal(resolveLanguage('ar', 'en-US'), 'ar');
  assert.equal(t('en', 'notif.preTitle', { prayer: 'Asr', n: 10 }), 'Asr in 10 min');
  assert.equal(t('de', 'notif.preTitle', { prayer: 'Asr', n: 10 }), 'Asr in 10 Min.');
  assert.equal(prayerLabel('ar', 'maghrib'), 'المغرب');
  assert.equal(t('de', 'does.not.exist'), 'does.not.exist');
  assert.ok(isRtl('ar') && !isRtl('de'));
});

test('DST transition days and a leap day compute cleanly', () => {
  const s = defaultSettings(STUTTGART);
  for (const iso of ['2026-03-29', '2026-10-25', '2028-02-29', '2026-12-31', '2027-01-01']) {
    const day = computeDay(s, new Date(`${iso}T12:00:00Z`));
    assert.equal(day.date, iso);
    const seq = [day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha];
    for (let i = 1; i < seq.length; i++) assert.ok(seq[i] > seq[i - 1], `${iso}: order broken at ${i}`);
    // Dhuhr stays near local noon on both sides of a DST switch (12:0x–12:3x CET/CEST).
    const dh = Number(formatTime(day.dhuhr, STUTTGART.timezone, false).slice(0, 2));
    assert.ok(dh === 12 || dh === 13, `${iso}: dhuhr hour ${dh}`);
  }
});

test('features: qibla, hijri/ramadan, jumuah, iqamah, quiet hours', async () => {
  const f = await import('../dist/index.js');
  const s = defaultSettings(STUTTGART);
  // Stuttgart → Kaaba: roughly south-east, ~130°; Makkah itself is undefined-ish but finite.
  const q = f.qiblaBearing(48.78, 9.18);
  assert.ok(q > 125 && q < 140, `qibla ${q}`);
  assert.ok(Math.abs(f.distanceToKaabaKm(48.78, 9.18) - 4150) < 150);
  // 6 Nov 2026 is a Friday; 7 Nov is not.
  assert.equal(f.isFriday(new Date('2026-11-06T12:00:00Z'), 'Europe/Berlin'), true);
  assert.equal(f.isFriday(new Date('2026-11-07T12:00:00Z'), 'Europe/Berlin'), false);
  assert.equal(f.prayerDisplayLabel('en', s, 'dhuhr', new Date('2026-11-06T12:00:00Z')), "Jumu'ah");
  assert.equal(f.prayerDisplayLabel('en', s, 'dhuhr', new Date('2026-11-07T12:00:00Z')), 'Dhuhr');
  // Hijri: 6 Nov 2026 ≈ 26 Jumada I 1448; Ramadan 1448 starts around 7 Feb 2027.
  const h = f.hijriDate(new Date('2026-11-06T12:00:00Z'), 'Europe/Berlin');
  assert.ok(h && h.year === 1448 && h.month === 5, JSON.stringify(h));
  assert.equal(f.isRamadan(s, new Date('2026-11-06T12:00:00Z')), false);
  assert.equal(f.isRamadan(s, new Date('2027-02-20T12:00:00Z')), true);
  s.ramadan = 'on';
  assert.equal(f.isRamadan(s, new Date('2026-11-06T12:00:00Z')), true);
  // Iqamah
  s.iqamah = { asr: 15 };
  const day = computeDay(s, new Date('2026-11-06T12:00:00Z'));
  assert.equal(f.iqamahTime(s, 'asr', day.asr).getTime() - day.asr.getTime(), 15 * 60_000);
  assert.equal(f.iqamahTime(s, 'fajr', day.fajr), null);
  // Quiet hours wrapping midnight (22:00–06:00 Berlin): 23:30 local is quiet, 12:00 is not.
  s.quietHours = { enabled: true, from: '22:00', to: '06:00' };
  assert.equal(f.inQuietHours(s, new Date('2026-11-06T22:30:00Z')), true);   // 23:30 CET
  assert.equal(f.inQuietHours(s, new Date('2026-11-06T11:00:00Z')), false);  // 12:00 CET
  assert.equal(f.inQuietHours(s, new Date('2026-11-06T04:30:00Z')), true);   // 05:30 CET
  const evs = upcomingEvents(s, new Date('2026-11-06T00:30:00Z'), 24 * 3600_000);
  assert.equal(evs.find((e) => e.name === 'fajr' && e.kind === 'prayer').quiet, true);
  assert.equal(evs.find((e) => e.name === 'dhuhr' && e.kind === 'prayer').quiet, false);
  // Jumu'ah reminder appears only on Friday
  s.jumuah = { enabled: true, reminderMinutes: 45 };
  const fri = upcomingEvents(s, new Date('2026-11-06T00:30:00Z'), 24 * 3600_000);
  const j = fri.find((e) => e.kind === 'jumuah');
  assert.ok(j && day.dhuhr.getTime() - j.at.getTime() === 45 * 60_000);
  const sat = upcomingEvents(s, new Date('2026-11-07T00:30:00Z'), 24 * 3600_000);
  assert.equal(sat.find((e) => e.kind === 'jumuah'), undefined);
});

test('features: month table, CSV export, timetable import + override', async () => {
  const f = await import('../dist/index.js');
  const s = defaultSettings(STUTTGART);
  const rows = f.monthTable(s, 2026, 11);
  assert.equal(rows.length, 30);
  assert.equal(rows[0].date, '2026-11-01');
  assert.equal(rows[5].friday, true);
  const csv = f.monthCsv(s, rows);
  assert.match(csv.split('\n')[0], /^date,fajr,sunrise,dhuhr,asr,maghrib,isha$/);
  assert.match(csv.split("\n")[6], /^2026-11-06,05:30,07:18,12:08,14:30,16:55,18:37$/); // matches the Stuttgart sanity output
  // Import: header in German, semicolon separated, DD.MM.YYYY dates
  const imp = f.parseTimetableCsv('Datum;Fajr;Sonnenaufgang;Dhuhr;Asr;Maghrib;Isha\n06.11.2026;05:40;07:20;12:10;14:35;17:00;18:45\nbad;x;x;x;x;x;x\n');
  assert.equal(imp.days, 1);
  assert.equal(imp.errors.length, 1);
  assert.deepEqual(imp.entries['2026-11-06'], { fajr: '05:40', sunrise: '07:20', dhuhr: '12:10', asr: '14:35', maghrib: '17:00', isha: '18:45' });
  s.timetable = imp.entries;
  const day = computeDay(s, new Date('2026-11-06T12:00:00Z'));
  assert.equal(formatTime(day.fajr, 'Europe/Berlin', false), '05:40');
  assert.equal(formatTime(day.isha, 'Europe/Berlin', false), '18:45');
  // Unlisted date still calculated
  const other = computeDay(s, new Date('2026-11-07T12:00:00Z'));
  assert.notEqual(formatTime(other.fajr, 'Europe/Berlin', false), '05:40');
  // DST-correct instant construction
  assert.equal(formatTime(f.instantFor('2026-07-01', '05:00', 'Europe/Berlin'), 'Europe/Berlin', false), '05:00');
  assert.equal(f.instantFor('2026-07-01', '05:00', 'Europe/Berlin').toISOString(), '2026-07-01T03:00:00.000Z');
});
