/**
 * Settings shared by every Salat front-end (Chrome, VS Code; mirrored by the
 * Qt and Android apps). Keep this flat and JSON-serialisable: it is what gets
 * written to chrome.storage / VS Code configuration / QSettings / DataStore.
 */

export type MethodId =
  | 'MuslimWorldLeague'
  | 'Egyptian'
  | 'Karachi'
  | 'UmmAlQura'
  | 'Dubai'
  | 'MoonsightingCommittee'
  | 'NorthAmerica'
  | 'Kuwait'
  | 'Qatar'
  | 'Singapore'
  | 'Tehran'
  | 'Turkey';

export type Madhab = 'shafi' | 'hanafi';

export type HighLatitudeRuleId =
  | 'recommended'
  | 'middleOfTheNight'
  | 'seventhOfTheNight'
  | 'twilightAngle';

export type PrayerName = 'fajr' | 'sunrise' | 'dhuhr' | 'asr' | 'maghrib' | 'isha';

/** The five obligatory prayers, in order. Sunrise is informational only. */
export const PRAYERS: readonly PrayerName[] = ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'];

export const PRAYER_LABELS: Record<PrayerName, string> = {
  fajr: 'Fajr',
  sunrise: 'Sunrise',
  dhuhr: 'Dhuhr',
  asr: 'Asr',
  maghrib: 'Maghrib',
  isha: 'Isha',
};

export const PRAYER_LABELS_AR: Record<PrayerName, string> = {
  fajr: 'الفجر',
  sunrise: 'الشروق',
  dhuhr: 'الظهر',
  asr: 'العصر',
  maghrib: 'المغرب',
  isha: 'العشاء',
};

export interface Location {
  latitude: number;
  longitude: number;
  /** IANA zone, e.g. "Europe/Berlin". */
  timezone: string;
  /** Free-text label shown in the UI, e.g. "Stuttgart". */
  label: string;
}

export interface Settings {
  location: Location;
  method: MethodId;
  madhab: Madhab;
  highLatitudeRule: HighLatitudeRuleId;
  /** Per-prayer manual offsets in minutes (e.g. to match a local mosque). */
  adjustments: Partial<Record<PrayerName, number>>;
  notifications: {
    /** Master switch for prayer-time notifications. */
    enabled: boolean;
    /** Minutes before the prayer for an optional heads-up. 0 = off. */
    preReminderMinutes: number;
    /** Play the chime with the notification. On by default — a reminder you cannot hear is not a reminder. */
    sound: boolean;
    /** Snooze length offered on the notification. */
    snoozeMinutes: number;
  };
  /** 12- or 24-hour clock in the UI. */
  hour12: boolean;
  /** UI language; 'auto' follows the system / host application. */
  language: 'auto' | 'en' | 'ar' | 'de';
}

export const METHODS: readonly { id: MethodId; label: string }[] = [
  { id: 'MuslimWorldLeague', label: 'Muslim World League' },
  { id: 'Egyptian', label: 'Egyptian General Authority of Survey' },
  { id: 'Karachi', label: 'University of Islamic Sciences, Karachi' },
  { id: 'UmmAlQura', label: 'Umm al-Qura University, Makkah' },
  { id: 'Dubai', label: 'Dubai' },
  { id: 'MoonsightingCommittee', label: 'Moonsighting Committee Worldwide' },
  { id: 'NorthAmerica', label: 'ISNA (North America)' },
  { id: 'Kuwait', label: 'Kuwait' },
  { id: 'Qatar', label: 'Qatar' },
  { id: 'Singapore', label: 'Singapore' },
  { id: 'Tehran', label: 'Institute of Geophysics, Tehran' },
  { id: 'Turkey', label: 'Diyanet (Turkey)' },
];

export const HIGH_LATITUDE_RULES: readonly { id: HighLatitudeRuleId; label: string }[] = [
  { id: 'recommended', label: 'Automatic (recommended for latitude)' },
  { id: 'middleOfTheNight', label: 'Middle of the night' },
  { id: 'seventhOfTheNight', label: 'One-seventh of the night' },
  { id: 'twilightAngle', label: 'Twilight angle' },
];

export function defaultSettings(location: Location): Settings {
  return {
    location,
    method: 'MuslimWorldLeague',
    madhab: 'shafi',
    highLatitudeRule: 'recommended',
    adjustments: {},
    notifications: {
      enabled: true,
      preReminderMinutes: 0,
      sound: true,
      snoozeMinutes: 10,
    },
    hour12: false,
    language: 'auto',
  };
}

/** Fill in any missing fields from a partially-stored settings object. */
export function mergeSettings(stored: unknown, fallback: Settings): Settings {
  if (!stored || typeof stored !== 'object') return fallback;
  const s = stored as Partial<Settings>;
  return {
    location: { ...fallback.location, ...(s.location ?? {}) },
    method: s.method ?? fallback.method,
    madhab: s.madhab ?? fallback.madhab,
    highLatitudeRule: s.highLatitudeRule ?? fallback.highLatitudeRule,
    adjustments: { ...fallback.adjustments, ...(s.adjustments ?? {}) },
    notifications: { ...fallback.notifications, ...(s.notifications ?? {}) },
    hour12: s.hour12 ?? fallback.hour12,
    language: s.language ?? fallback.language,
  };
}
