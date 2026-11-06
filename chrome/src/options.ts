import {
  HIGH_LATITUDE_RULES,
  LANGUAGES,
  METHODS,
  cityToLocation,
  prayerLabel,
  t,
  type Language,
  type LanguagePref,
  computeDay,
  formatTime,
  searchCities,
  type HighLatitudeRuleId,
  type Madhab,
  type MethodId,
  type PrayerName,
} from '@salat/core';
import { load, save, type Stored } from './storage.js';
import { localiseDocument, pageLanguage } from './ui-i18n.js';

const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;
const input = (id: string) => $<HTMLInputElement>(id);
const select = (id: string) => $<HTMLSelectElement>(id);

let stored: Stored;
let lang: Language = 'en';
let saveTimer: number | undefined;

function fillSelect(el: HTMLSelectElement, items: readonly { id: string; label: string }[]): void {
  el.innerHTML = '';
  for (const it of items) {
    const o = document.createElement('option');
    o.value = it.id;
    o.textContent = it.label;
    el.appendChild(o);
  }
}

function renderForm(): void {
  const s = stored.settings;
  input('city').value = s.location.label;
  input('lat').value = String(s.location.latitude);
  input('lng').value = String(s.location.longitude);
  input('tz').value = s.location.timezone;
  select('method').value = s.method;
  select('madhab').value = s.madhab;
  select('hlr').value = s.highLatitudeRule;
  select('language').value = s.language;
  for (const p of ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as PrayerName[]) {
    input(`adj-${p}`).value = String(s.adjustments[p] ?? 0);
  }
  input('enabled').checked = s.notifications.enabled;
  input('pre').value = String(s.notifications.preReminderMinutes);
  input('sound').checked = s.notifications.sound;
  input('snooze').value = String(s.notifications.snoozeMinutes);
  input('hour12').checked = s.hour12;
  input('badge').checked = stored.extras.badge;
  renderPreview();
}

function renderPreview(): void {
  const s = stored.settings;
  const el = $('preview');
  try {
    const day = computeDay(s, new Date());
    el.innerHTML = (['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as PrayerName[])
      .map((p) => `<div><span class="muted">${prayerLabel(lang, p)}</span><b>${formatTime(day[p], s.location.timezone, s.hour12)}</b></div>`)
      .join('');
    $('location-status').textContent = '';
  } catch (e) {
    el.innerHTML = '';
    $('location-status').textContent = `Cannot compute times: ${(e as Error).message}`;
  }
}

function readForm(): void {
  const s = stored.settings;
  const lat = Number(input('lat').value);
  const lng = Number(input('lng').value);
  const tz = input('tz').value.trim();
  if (Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
    s.location.latitude = lat;
    s.location.longitude = lng;
  }
  if (tz && isValidTimezone(tz)) s.location.timezone = tz;
  s.location.label = input('city').value.trim() || `${lat.toFixed(2)}, ${lng.toFixed(2)}`;
  s.method = select('method').value as MethodId;
  s.madhab = select('madhab').value as Madhab;
  s.highLatitudeRule = select('hlr').value as HighLatitudeRuleId;
  s.adjustments = {};
  for (const p of ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as PrayerName[]) {
    const v = Number(input(`adj-${p}`).value);
    if (v) s.adjustments[p] = Math.max(-60, Math.min(60, Math.round(v)));
  }
  s.notifications.enabled = input('enabled').checked;
  s.notifications.preReminderMinutes = clamp(Number(input('pre').value), 0, 60);
  s.notifications.sound = input('sound').checked;
  s.notifications.snoozeMinutes = clamp(Number(input('snooze').value) || 10, 1, 60);
  s.hour12 = input('hour12').checked;
  const newLang = select('language').value as LanguagePref;
  if (newLang !== s.language) {
    s.language = newLang;
    applyLanguage();
  }
  stored.extras.badge = input('badge').checked;
  stored.extras.configured = true;
}

function clamp(v: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, Number.isFinite(v) ? v : lo));
}

function isValidTimezone(tz: string): boolean {
  try {
    new Intl.DateTimeFormat(undefined, { timeZone: tz });
    return true;
  } catch {
    return false;
  }
}

function applyLanguage(): void {
  lang = pageLanguage(stored.settings);
  localiseDocument(lang);
  fillSelect(select('method'), METHODS);
  fillSelect(select('hlr'), HIGH_LATITUDE_RULES.map((r) => ({ id: r.id, label: t(lang, `rule.${r.id}`) })));
  fillSelect(select('language'), LANGUAGES);
  document.querySelectorAll<HTMLLabelElement>('#adjust label span').forEach((el) => {
    el.textContent = prayerLabel(lang, el.dataset.prayer as PrayerName);
  });
  select('method').value = stored.settings.method;
  select('hlr').value = stored.settings.highLatitudeRule;
  select('language').value = stored.settings.language;
}

function scheduleSave(): void {
  readForm();
  renderPreview();
  window.clearTimeout(saveTimer);
  saveTimer = window.setTimeout(async () => {
    await save(stored);
    const st = $('save-status');
    st.textContent = t(lang, 'settings.saved');
    setTimeout(() => (st.textContent = ''), 1500);
  }, 300);
}

// ---- city search ----------------------------------------------------------

function setupCitySearch(): void {
  const box = input('city');
  const list = $('city-results');
  let active = -1;

  const close = () => { list.innerHTML = ''; active = -1; };
  const pick = (idx: number) => {
    const li = list.children[idx] as HTMLLIElement | undefined;
    if (!li) return;
    const c = searchCities(box.value, 8)[idx];
    if (!c) return;
    stored.settings.location = cityToLocation(c);
    close();
    renderForm();
    scheduleSave();
  };

  box.addEventListener('input', () => {
    const results = searchCities(box.value, 8);
    list.innerHTML = '';
    active = -1;
    for (const c of results) {
      const li = document.createElement('li');
      li.setAttribute('role', 'option');
      li.innerHTML = `<span>${c.name}, ${c.country}</span><span class="tz">${c.tz}</span>`;
      li.addEventListener('mousedown', (e) => { e.preventDefault(); pick([...list.children].indexOf(li)); });
      list.appendChild(li);
    }
    // Typing a free-form label still saves (coords stay as they are).
    scheduleSave();
  });
  box.addEventListener('keydown', (e) => {
    const n = list.children.length;
    if (!n) return;
    if (e.key === 'ArrowDown') { active = (active + 1) % n; e.preventDefault(); }
    else if (e.key === 'ArrowUp') { active = (active - 1 + n) % n; e.preventDefault(); }
    else if (e.key === 'Enter' && active >= 0) { pick(active); e.preventDefault(); return; }
    else if (e.key === 'Escape') { close(); return; }
    [...list.children].forEach((li, i) => li.setAttribute('aria-selected', String(i === active)));
  });
  box.addEventListener('blur', () => setTimeout(close, 120));
}

// ---- init -----------------------------------------------------------------

async function init(): Promise<void> {
  stored = await load();
  const adj = $('adjust');
  for (const p of ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as PrayerName[]) {
    const label = document.createElement('label');
    label.innerHTML = `<span data-prayer="${p}"></span><input id="adj-${p}" type="number" min="-60" max="60" step="1">`;
    adj.appendChild(label);
  }
  applyLanguage();
  renderForm();
  setupCitySearch();

  document.querySelectorAll<HTMLElement>('input:not(#city), select').forEach((el) => {
    el.addEventListener('change', scheduleSave);
    if (el instanceof HTMLInputElement && (el.type === 'number' || el.type === 'text')) el.addEventListener('input', scheduleSave);
  });

  $('test').addEventListener('click', async () => {
    await save(stored);
    await chrome.runtime.sendMessage({ type: 'test-notification' });
  });
}

void init();
