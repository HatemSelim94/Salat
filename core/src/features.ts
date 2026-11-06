/**
 * Small, computation-only features on top of the engine: Qibla, Hijri date /
 * Ramadan, Jumu'ah, iqamah, quiet hours, month tables, CSV import/export.
 * No network, no sensors, no background work.
 */
import { civilDate, computeDay, type DayTimes } from './engine.js';
import { t, type Language } from './i18n.js';
import { PRAYERS, type PrayerName, type Settings } from './settings.js';

// ---- Qibla ----------------------------------------------------------------

const KAABA = { latitude: 21.4225, longitude: 39.8262 };

/** Initial great-circle bearing from the location to the Kaaba, degrees clockwise from true north [0, 360). */
export function qiblaBearing(latitude: number, longitude: number): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const toDeg = (r: number) => (r * 180) / Math.PI;
  const φ1 = toRad(latitude);
  const φ2 = toRad(KAABA.latitude);
  const Δλ = toRad(KAABA.longitude - longitude);
  const y = Math.sin(Δλ) * Math.cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);
  return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

/** Great-circle distance to the Kaaba in km (for display only). */
export function distanceToKaabaKm(latitude: number, longitude: number): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const φ1 = toRad(latitude), φ2 = toRad(KAABA.latitude);
  const Δφ = φ2 - φ1, Δλ = toRad(KAABA.longitude - longitude);
  const a = Math.sin(Δφ / 2) ** 2 + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ---- Hijri / Ramadan --------------------------------------------------------

export interface HijriDate { year: number; month: number; day: number }

/** Umm al-Qura Hijri date of `instant` in `timezone` (via Intl). Null if the runtime lacks the calendar. */
export function hijriDate(instant: Date, timezone: string): HijriDate | null {
  try {
    const parts = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura-nu-latn', {
      timeZone: timezone, year: 'numeric', month: 'numeric', day: 'numeric',
    }).formatToParts(instant);
    const get = (type: string) => Number(parts.find((p) => p.type === type)?.value);
    const d = { year: get('year'), month: get('month'), day: get('day') };
    return Number.isFinite(d.year) && Number.isFinite(d.month) ? d : null;
  } catch {
    return null;
  }
}

export function isRamadan(settings: Settings, instant: Date): boolean {
  if (settings.ramadan === 'on') return true;
  if (settings.ramadan === 'off') return false;
  return hijriDate(instant, settings.location.timezone)?.month === 9;
}

// ---- Jumu'ah ----------------------------------------------------------------

export function isFriday(instant: Date, timezone: string): boolean {
  const wd = new Intl.DateTimeFormat('en-US', { timeZone: timezone, weekday: 'short' }).format(instant);
  return wd === 'Fri';
}

/** Display label for a prayer on a given day: Dhuhr becomes Jumu'ah on Fridays when enabled. */
export function prayerDisplayLabel(lang: Language, settings: Settings, name: PrayerName, dayInstant: Date): string {
  if (name === 'dhuhr' && settings.jumuah.enabled && isFriday(dayInstant, settings.location.timezone)) return t(lang, 'prayer.jumuah');
  return t(lang, `prayer.${name}`);
}

// ---- Iqamah -------------------------------------------------------------------

/** Iqamah time for a prayer, or null when not configured. */
export function iqamahTime(settings: Settings, name: PrayerName, adhan: Date): Date | null {
  const m = settings.iqamah[name];
  return m && m > 0 ? new Date(adhan.getTime() + m * 60_000) : null;
}

// ---- Quiet hours -------------------------------------------------------------

function minutesOfDay(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

/** True when `instant` falls inside the configured quiet window (may wrap past midnight). */
export function inQuietHours(settings: Settings, instant: Date): boolean {
  const q = settings.quietHours;
  if (!q.enabled) return false;
  const parts = new Intl.DateTimeFormat('en-GB', { timeZone: settings.location.timezone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(instant);
  const get = (type: string) => Number(parts.find((p) => p.type === type)?.value);
  const now = get('hour') * 60 + get('minute');
  const from = minutesOfDay(q.from), to = minutesOfDay(q.to);
  return from <= to ? now >= from && now < to : now >= from || now < to;
}

// ---- Month table + CSV ---------------------------------------------------------

export interface MonthRow { date: string; hijri: HijriDate | null; times: DayTimes; friday: boolean }

/** Every day of the civil month (year, month 1–12) in the location's zone. */
export function monthTable(settings: Settings, year: number, month: number): MonthRow[] {
  const rows: MonthRow[] = [];
  const tz = settings.location.timezone;
  for (let day = 1; day <= 31; day++) {
    // Noon UTC of the civil date is safely inside that date for every zone (UTC−12…+14 are both < 12 h).
    const probe = new Date(Date.UTC(year, month - 1, day, 12));
    if (probe.getUTCMonth() !== month - 1) break;
    // computeDay derives the civil date from the instant in tz; noon UTC maps to the same civil date for |offset| < 12 h.
    const times = computeDay(settings, probe);
    if (civilDate(probe, tz).day !== day) continue;
    rows.push({ date: times.date, hijri: hijriDate(probe, tz), times, friday: isFriday(probe, tz) });
  }
  return rows;
}

/** CSV with a header: date,fajr,sunrise,dhuhr,asr,maghrib,isha (HH:MM in the location's zone). */
export function monthCsv(settings: Settings, rows: MonthRow[], hour12 = false): string {
  const fmt = (d: Date) => formatHHMM(d, settings.location.timezone, hour12);
  const lines = ['date,fajr,sunrise,dhuhr,asr,maghrib,isha'];
  for (const r of rows) {
    lines.push([r.date, fmt(r.times.fajr), fmt(r.times.sunrise), fmt(r.times.dhuhr), fmt(r.times.asr), fmt(r.times.maghrib), fmt(r.times.isha)].join(','));
  }
  return lines.join('\n') + '\n';
}

function formatHHMM(d: Date, timezone: string, hour12: boolean): string {
  return new Intl.DateTimeFormat('en-GB', { timeZone: timezone, hour: '2-digit', minute: '2-digit', hour12, hourCycle: hour12 ? undefined : 'h23' }).format(d);
}

// ---- Timetable import -------------------------------------------------------------

export interface ImportResult { entries: Record<string, Partial<Record<PrayerName, string>>>; days: number; errors: string[] }

/**
 * Parse a mosque timetable CSV. Accepts a header row naming the columns (any order,
 * case-insensitive: date, fajr, sunrise/shuruq, dhuhr/zuhr, asr, maghrib, isha) or the
 * fixed order date,fajr,sunrise,dhuhr,asr,maghrib,isha. Dates: YYYY-MM-DD or DD.MM.YYYY
 * or DD/MM/YYYY. Times: H:MM or HH:MM (24 h). Rows that fail are reported, not fatal.
 */
export function parseTimetableCsv(text: string): ImportResult {
  const entries: ImportResult['entries'] = {};
  const errors: string[] = [];
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  if (!lines.length) return { entries, days: 0, errors: ['empty file'] };
  const sep = lines[0].includes(';') ? ';' : lines[0].includes('\t') ? '\t' : ',';
  const aliases: Record<string, PrayerName | 'date'> = {
    date: 'date', datum: 'date', day: 'date', تاريخ: 'date', التاريخ: 'date',
    fajr: 'fajr', fajer: 'fajr', fadschr: 'fajr', الفجر: 'fajr', فجر: 'fajr',
    sunrise: 'sunrise', shuruq: 'sunrise', shurooq: 'sunrise', sonnenaufgang: 'sunrise', الشروق: 'sunrise', شروق: 'sunrise',
    dhuhr: 'dhuhr', zuhr: 'dhuhr', duhr: 'dhuhr', dhuhur: 'dhuhr', الظهر: 'dhuhr', ظهر: 'dhuhr',
    asr: 'asr', العصر: 'asr', عصر: 'asr',
    maghrib: 'maghrib', magrib: 'maghrib', المغرب: 'maghrib', مغرب: 'maghrib',
    isha: 'isha', ishaa: 'isha', العشاء: 'isha', عشاء: 'isha',
  };
  const first = lines[0].split(sep).map((c) => c.trim().toLowerCase().replace(/["']/g, ''));
  let columns: (PrayerName | 'date' | null)[];
  let start = 0;
  if (first.some((c) => aliases[c] === 'date')) {
    columns = first.map((c) => aliases[c] ?? null);
    start = 1;
  } else {
    columns = ['date', 'fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];
  }
  for (let i = start; i < lines.length; i++) {
    const cells = lines[i].split(sep).map((c) => c.trim().replace(/["']/g, ''));
    const dateIdx = columns.indexOf('date');
    const iso = normaliseDate(cells[dateIdx] ?? '');
    if (!iso) { errors.push(`line ${i + 1}: unreadable date "${cells[dateIdx] ?? ''}"`); continue; }
    const row: Partial<Record<PrayerName, string>> = {};
    columns.forEach((col, idx) => {
      if (!col || col === 'date') return;
      const tm = normaliseTime(cells[idx] ?? '');
      if (tm) row[col] = tm;
      else if (cells[idx]) errors.push(`line ${i + 1}: unreadable time "${cells[idx]}" for ${col}`);
    });
    if (Object.keys(row).length) entries[iso] = row;
  }
  return { entries, days: Object.keys(entries).length, errors };
}

function normaliseDate(s: string): string | null {
  let m = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
  if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`;
  m = s.match(/^(\d{1,2})[./](\d{1,2})[./](\d{4})$/);
  if (m) return `${m[3]}-${m[2].padStart(2, '0')}-${m[1].padStart(2, '0')}`;
  return null;
}

function normaliseTime(s: string): string | null {
  const m = s.match(/^(\d{1,2}):(\d{2})(?::\d{2})?\s*(am|pm)?$/i);
  if (!m) return null;
  let h = Number(m[1]);
  const min = Number(m[2]);
  if (m[3]) { const pm = m[3].toLowerCase() === 'pm'; if (pm && h < 12) h += 12; if (!pm && h === 12) h = 0; }
  if (h > 23 || min > 59) return null;
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
}

/**
 * Apply imported timetable entries to a computed day: any prayer present in the
 * imported row for that date replaces the calculated time (interpreted in the location's zone).
 */
export function applyTimetable(settings: Settings, day: DayTimes): DayTimes {
  const row = settings.timetable[day.date];
  if (!row) return day;
  const out = { ...day };
  for (const name of [...PRAYERS, 'sunrise' as PrayerName]) {
    const hhmm = row[name];
    if (!hhmm) continue;
    const at = instantFor(day.date, hhmm, settings.location.timezone);
    if (at) out[name] = at;
  }
  return out;
}

/** The instant of civil `date` at `hhmm` in `timezone` (handles DST via Intl offset lookup). */
export function instantFor(date: string, hhmm: string, timezone: string): Date | null {
  const [y, mo, d] = date.split('-').map(Number);
  const [h, mi] = hhmm.split(':').map(Number);
  if (![y, mo, d, h, mi].every(Number.isFinite)) return null;
  // Start from the UTC guess, then correct by the zone offset at that moment (twice, for DST edges).
  let guess = Date.UTC(y, mo - 1, d, h, mi);
  for (let i = 0; i < 2; i++) {
    const offsetMin = zoneOffsetMinutes(new Date(guess), timezone);
    guess = Date.UTC(y, mo - 1, d, h, mi) - offsetMin * 60_000;
  }
  return new Date(guess);
}

function zoneOffsetMinutes(instant: Date, timezone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone, hourCycle: 'h23', year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric',
  }).formatToParts(instant);
  const get = (type: string) => Number(parts.find((p) => p.type === type)?.value);
  const asUtc = Date.UTC(get('year'), get('month') - 1, get('day'), get('hour') % 24, get('minute'), get('second'));
  return Math.round((asUtc - instant.getTime()) / 60_000);
}
