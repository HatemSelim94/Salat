import {
  CalculationMethod,
  CalculationParameters,
  Coordinates,
  HighLatitudeRule,
  Madhab as AdhanMadhab,
  PrayerTimes,
} from 'adhan';
import type { HighLatitudeRuleId, MethodId, PrayerName, Settings } from './settings.js';
import { PRAYERS } from './settings.js';
import { applyTimetable, inQuietHours, isFriday } from './features.js';

export interface DayTimes {
  /** Civil date (in the location's time zone) these times belong to, as YYYY-MM-DD. */
  date: string;
  fajr: Date;
  sunrise: Date;
  dhuhr: Date;
  asr: Date;
  maghrib: Date;
  isha: Date;
}

export interface PrayerEvent {
  name: PrayerName;
  time: Date;
}

export interface ScheduledEvent {
  /** 'prayer' = the prayer time itself, 'pre' = heads-up before it, 'jumuah' = Friday reminder before Dhuhr. */
  kind: 'prayer' | 'pre' | 'jumuah';
  name: PrayerName;
  at: Date;
  /** True when the event falls inside quiet hours: notify without sound. */
  quiet: boolean;
}

const METHOD_FACTORIES: Record<MethodId, () => CalculationParameters> = {
  MuslimWorldLeague: CalculationMethod.MuslimWorldLeague,
  Egyptian: CalculationMethod.Egyptian,
  Karachi: CalculationMethod.Karachi,
  UmmAlQura: CalculationMethod.UmmAlQura,
  Dubai: CalculationMethod.Dubai,
  MoonsightingCommittee: CalculationMethod.MoonsightingCommittee,
  NorthAmerica: CalculationMethod.NorthAmerica,
  Kuwait: CalculationMethod.Kuwait,
  Qatar: CalculationMethod.Qatar,
  Singapore: CalculationMethod.Singapore,
  Tehran: CalculationMethod.Tehran,
  Turkey: CalculationMethod.Turkey,
};

type HighLatitudeRuleValue = CalculationParameters['highLatitudeRule'];

function highLatitudeRule(id: HighLatitudeRuleId, coords: Coordinates): HighLatitudeRuleValue {
  switch (id) {
    case 'middleOfTheNight':
      return HighLatitudeRule.MiddleOfTheNight;
    case 'seventhOfTheNight':
      return HighLatitudeRule.SeventhOfTheNight;
    case 'twilightAngle':
      return HighLatitudeRule.TwilightAngle;
    default:
      return HighLatitudeRule.recommended(coords);
  }
}

function buildParams(settings: Settings, coords: Coordinates): CalculationParameters {
  const params = METHOD_FACTORIES[settings.method]();
  params.madhab = settings.madhab === 'hanafi' ? AdhanMadhab.Hanafi : AdhanMadhab.Shafi;
  params.highLatitudeRule = highLatitudeRule(settings.highLatitudeRule, coords);
  const a = settings.adjustments;
  params.adjustments = {
    fajr: a.fajr ?? 0,
    sunrise: a.sunrise ?? 0,
    dhuhr: a.dhuhr ?? 0,
    asr: a.asr ?? 0,
    maghrib: a.maghrib ?? 0,
    isha: a.isha ?? 0,
  };
  return params;
}

/** Civil date parts of `instant` in the given IANA time zone. */
export function civilDate(instant: Date, timezone: string): { year: number; month: number; day: number } {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(instant);
  const get = (type: string) => Number(parts.find((p) => p.type === type)?.value);
  return { year: get('year'), month: get('month'), day: get('day') };
}

function isoDate(d: { year: number; month: number; day: number }): string {
  const mm = String(d.month).padStart(2, '0');
  const dd = String(d.day).padStart(2, '0');
  return `${d.year}-${mm}-${dd}`;
}

/**
 * Prayer times for the civil day that contains `instant` in the location's
 * time zone, offset by `dayOffset` days (0 = today, 1 = tomorrow, …).
 *
 * adhan-js reads year/month/day from the Date it is given using the runtime's
 * local getters, so we construct a local-noon Date carrying the location's
 * civil date; this keeps results correct even when the runtime's zone differs
 * from the location's.
 */
export function computeDay(settings: Settings, instant: Date, dayOffset = 0): DayTimes {
  const { latitude, longitude, timezone } = settings.location;
  const coords = new Coordinates(latitude, longitude);
  const base = civilDate(instant, timezone);
  const dateForAdhan = new Date(base.year, base.month - 1, base.day + dayOffset, 12, 0, 0, 0);
  const pt = new PrayerTimes(coords, dateForAdhan, buildParams(settings, coords));
  const day: DayTimes = {
    date: isoDate(civilDate(pt.dhuhr, timezone)),
    fajr: pt.fajr,
    sunrise: pt.sunrise,
    dhuhr: pt.dhuhr,
    asr: pt.asr,
    maghrib: pt.maghrib,
    isha: pt.isha,
  };
  return settings.timetable[day.date] ? applyTimetable(settings, day) : day;
}

/** The five obligatory prayers of a day as an ordered list. */
export function prayerList(day: DayTimes): PrayerEvent[] {
  return PRAYERS.map((name) => ({ name, time: day[name] }));
}

/** Next obligatory prayer strictly after `now` (wraps to tomorrow's Fajr). */
export function nextPrayer(settings: Settings, now: Date): PrayerEvent {
  for (const offset of [0, 1, 2]) {
    const day = computeDay(settings, now, offset);
    const hit = prayerList(day).find((p) => p.time.getTime() > now.getTime());
    if (hit) return hit;
  }
  // Unreachable in practice; satisfy the type checker.
  const day = computeDay(settings, now, 1);
  return { name: 'fajr', time: day.fajr };
}

/** Most recent obligatory prayer at or before `now` (wraps to yesterday's Isha). */
export function currentPrayer(settings: Settings, now: Date): PrayerEvent {
  for (const offset of [0, -1]) {
    const day = computeDay(settings, now, offset);
    const list = prayerList(day).filter((p) => p.time.getTime() <= now.getTime());
    if (list.length) return list[list.length - 1];
  }
  const day = computeDay(settings, now, -1);
  return { name: 'isha', time: day.isha };
}

/**
 * Every notification that should fire in (now, now + horizonMs]. Front-ends
 * turn these into chrome.alarms / timers / AlarmManager entries. A pre-reminder
 * is only emitted when enabled and when it is still in the future.
 */
export function upcomingEvents(settings: Settings, now: Date, horizonMs = 36 * 3600_000): ScheduledEvent[] {
  const out: ScheduledEvent[] = [];
  const end = now.getTime() + horizonMs;
  const pre = settings.notifications.preReminderMinutes;
  const jm = settings.jumuah.enabled ? settings.jumuah.reminderMinutes : 0;
  for (const offset of [0, 1, 2]) {
    const day = computeDay(settings, now, offset);
    const friday = isFriday(day.dhuhr, settings.location.timezone);
    for (const p of prayerList(day)) {
      const t = p.time.getTime();
      if (t <= now.getTime() || t > end) continue;
      if (pre > 0) {
        const preAt = t - pre * 60_000;
        if (preAt > now.getTime()) out.push({ kind: 'pre', name: p.name, at: new Date(preAt), quiet: inQuietHours(settings, new Date(preAt)) });
      }
      if (friday && p.name === 'dhuhr' && jm > 0) {
        const jAt = t - jm * 60_000;
        if (jAt > now.getTime()) out.push({ kind: 'jumuah', name: 'dhuhr', at: new Date(jAt), quiet: inQuietHours(settings, new Date(jAt)) });
      }
      out.push({ kind: 'prayer', name: p.name, at: p.time, quiet: inQuietHours(settings, p.time) });
    }
  }
  out.sort((a, b) => a.at.getTime() - b.at.getTime());
  return out;
}

/** "13:42" or "1:42 PM" in the location's zone. */
export function formatTime(d: Date, timezone: string, hour12: boolean): string {
  return new Intl.DateTimeFormat(undefined, {
    timeZone: timezone,
    hour: hour12 ? 'numeric' : '2-digit',
    minute: '2-digit',
    hour12,
  }).format(d);
}

/** "2h 05m" / "14m" / "<1m" until `target`. */
export function formatCountdown(now: Date, target: Date): string {
  const totalMin = Math.max(0, Math.round((target.getTime() - now.getTime()) / 60_000));
  if (totalMin < 1) return '<1m';
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  if (h === 0) return `${m}m`;
  return `${h}h ${String(m).padStart(2, '0')}m`;
}
