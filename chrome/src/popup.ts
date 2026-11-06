import {
  METHODS,
  PRAYER_LABELS,
  civilDate,
  computeDay,
  currentPrayer,
  formatCountdown,
  formatTime,
  nextPrayer,
  type PrayerName,
} from '@salat/core';
import { load } from './storage.js';

const ORDER: PrayerName[] = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;

async function render(): Promise<void> {
  const { settings, extras } = await load();
  const tz = settings.location.timezone;
  const now = new Date();
  const day = computeDay(settings, now);
  const next = nextPrayer(settings, now);
  const current = currentPrayer(settings, now);

  $('location').textContent = settings.location.label;
  $('gregorian').textContent = new Intl.DateTimeFormat(undefined, {
    timeZone: tz, weekday: 'short', day: 'numeric', month: 'short',
  }).format(now);
  try {
    $('hijri').textContent = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura', {
      timeZone: tz, day: 'numeric', month: 'long', year: 'numeric',
    }).format(now).replace(/ AH$/, '');
  } catch { $('hijri').textContent = ''; }

  $('next-name').textContent = PRAYER_LABELS[next.name];
  const tomorrow = civilDate(next.time, tz).day !== civilDate(now, tz).day ? ' tomorrow' : '';
  $('next-in').innerHTML = `${formatTime(next.time, tz, settings.hour12)}${tomorrow} · in <b>${formatCountdown(now, next.time)}</b>`;

  const list = $('times');
  list.innerHTML = '';
  for (const name of ORDER) {
    const li = document.createElement('li');
    const t = day[name];
    li.className = name === 'sunrise' ? 'sunrise' : '';
    if (name !== 'sunrise') {
      if (name === current.name && t.getTime() <= now.getTime()) li.classList.add('current');
      else if (t.getTime() <= now.getTime()) li.classList.add('passed');
    } else if (t.getTime() <= now.getTime()) li.classList.add('passed');
    li.innerHTML = `<span>${PRAYER_LABELS[name]}</span><span class="time">${formatTime(t, tz, settings.hour12)}</span>`;
    list.appendChild(li);
  }

  $('method').textContent = METHODS.find((m) => m.id === settings.method)?.label.split(',')[0] ?? '';

  const notice = $('notice');
  if (!extras.configured) {
    notice.hidden = false;
    notice.innerHTML = `Location guessed from your time zone. <a href="#" id="fix-location">Set your city</a> for exact times.`;
    notice.querySelector('#fix-location')?.addEventListener('click', openOptions);
  } else if (!settings.notifications.enabled) {
    notice.hidden = false;
    notice.textContent = 'Reminders are turned off.';
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
