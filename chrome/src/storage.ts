import {
  defaultLocationForTimezone,
  defaultSettings,
  mergeSettings,
  systemTimezone,
  type Settings,
} from '@salat/core';

/** Chrome-only extras kept next to the shared settings. */
export interface ChromeExtras {
  /** Show the next prayer's name on the toolbar icon (static text, no animation). */
  badge: boolean;
  /** True once the user has saved settings at least once. */
  configured: boolean;
}

export interface Stored {
  settings: Settings;
  extras: ChromeExtras;
}

const KEY = 'salat';

const FALLBACK_LOCATION = {
  latitude: 21.39,
  longitude: 39.86,
  timezone: 'Asia/Riyadh',
  label: 'Makkah, SA',
};

export function fallbackSettings(): Settings {
  const loc = defaultLocationForTimezone(systemTimezone()) ?? FALLBACK_LOCATION;
  return defaultSettings(loc);
}

export async function load(): Promise<Stored> {
  const raw = (await chrome.storage.local.get(KEY))[KEY] as Partial<Stored> | undefined;
  const settings = mergeSettings(raw?.settings, fallbackSettings());
  const extras: ChromeExtras = { badge: true, configured: false, ...(raw?.extras ?? {}) };
  return { settings, extras };
}

export async function save(stored: Stored): Promise<void> {
  await chrome.storage.local.set({ [KEY]: stored });
}

export function onChange(cb: (stored: Stored) => void): void {
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'local' && changes[KEY]) load().then(cb);
  });
}
