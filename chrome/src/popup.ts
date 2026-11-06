import {
  METHODS,
  civilDate,
  intlLocale,
  prayerLabel,
  t,
  computeDay,
  currentPrayer,
  formatCountdown,
  formatTime,
  nextPrayer,
  type PrayerName,
} from '@salat/core';
import { load } from './storage.js';
import { localiseDocument, pageLanguage } from './ui-i18n.js';

const ORDER: PrayerName[] = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;

async function render(): Promise<void> {
  const { settings, extras } = await load();
  const lang = pageLanguage(settings);
  localiseDocument(lang);
  const tz = settings.location.timezone;
  const now = new Date();
  const day = computeDay(settings, now);
  const next = nextPrayer(settings, now);
  const current = currentPrayer(settings, now);

  $('location').textContent = settings.location.label;
  $('gregorian').textContent = new Intl.DateTimeFormat(intlLocale(lang), {
    timeZone: tz, weekday: 'short', day: 'numeric', month: 'short',
  }).format(now);
  try {
    $('hijri').textContent = new Intl.DateTimeFormat(`${intlLocale(lang)}-u-ca-islamic-umalqura`, {
      timeZone: tz, day: 'numeric', month: 'long', year: 'numeric',
    }).format(now).replace(/ AH$/, '').replace(/ هـ$/, '');
  } catch { $('hijri').textContent = ''; }

  $('next-name').textContent = prayerLabel(lang, next.name);
  const tomorrow = civilDate(next.time, tz).day !== civilDate(now, tz).day ? ` ${t(lang, 'home.tomorrow')}` : '';
  $('next-in').innerHTML = `${formatTime(next.time, tz, settings.hour12)}${tomorrow} · ${t(lang, 'home.in')} <b>${formatCountdown(now, next.time)}</b>`;

  const list = $('times');
  list.innerHTML = '';
  for (const name of ORDER) {
    const li = document.createElement('li');
    const time = day[name];
    li.className = name === 'sunrise' ? 'sunrise' : '';
    if (name !== 'sunrise') {
      if (name === current.name && time.getTime() <= now.getTime()) li.classList.add('current');
      else if (time.getTime() <= now.getTime()) li.classList.add('passed');
    } else if (time.getTime() <= now.getTime()) li.classList.add('passed');
    li.innerHTML = `<span>${prayerLabel(lang, name)}</span><span class="time">${formatTime(time, tz, settings.hour12)}</span>`;
    list.appendChild(li);
  }

  $('method').textContent = METHODS.find((m) => m.id === settings.method)?.label.split(',')[0] ?? '';

  const notice = $('notice');
  if (!extras.configured) {
    notice.hidden = false;
    notice.innerHTML = `${t(lang, 'home.locationGuessed')} <a href="#" id="fix-location">${t(lang, 'home.setCity')}</a> ${t(lang, 'home.forExactTimes')}`;
    notice.querySelector('#fix-location')?.addEventListener('click', openOptions);
  } else if (!settings.notifications.enabled) {
    notice.hidden = false;
    notice.textContent = t(lang, 'home.remindersOff');
  } else {
    notice.hidden = true;
  }
}

function openOptions(e: Event): void {
  e.preventDefault();
  chrome.runtime.openOptionsPage();
}

$('open-options').addEventListener('click', openOptions);
void render();
// The popup only lives while open; a 30 s tick keeps the countdown honest.
setInterval(() => void render(), 30_000);
