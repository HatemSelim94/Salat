import strings from './strings.json' with { type: 'json' };
import type { PrayerName } from './settings.js';

export type Language = 'en' | 'ar' | 'de';
export type LanguagePref = 'auto' | Language;
export const LANGUAGES: readonly { id: LanguagePref; label: string }[] = [
  { id: 'auto', label: 'System' },
  { id: 'en', label: 'English' },
  { id: 'ar', label: 'العربية' },
  { id: 'de', label: 'Deutsch' },
];

type Table = Record<string, Record<Language, string>>;
const TABLE = strings as unknown as Table;

/** Resolve a preference against a BCP-47 system tag ("de-DE", "ar", "en-US"). */
export function resolveLanguage(pref: LanguagePref | undefined, systemTag: string | undefined): Language {
  if (pref && pref !== 'auto') return pref;
  const base = (systemTag ?? 'en').toLowerCase().split(/[-_]/)[0];
  return base === 'ar' || base === 'de' ? base : 'en';
}

export function isRtl(lang: Language): boolean {
  return lang === 'ar';
}

/** Translate `key`, substituting `{name}` placeholders. Falls back to English, then to the key. */
export function t(lang: Language, key: string, params: Record<string, string | number> = {}): string {
  const entry = TABLE[key];
  let s = entry?.[lang] ?? entry?.en ?? key;
  for (const [k, v] of Object.entries(params)) s = s.replaceAll(`{${k}}`, String(v));
  return s;
}

export function prayerLabel(lang: Language, name: PrayerName): string {
  return t(lang, `prayer.${name}`);
}

/** Locale tag for Intl date formatting in this language. */
export function intlLocale(lang: Language): string {
  return lang === 'ar' ? 'ar' : lang === 'de' ? 'de-DE' : 'en-GB';
}
