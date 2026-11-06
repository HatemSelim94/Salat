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
