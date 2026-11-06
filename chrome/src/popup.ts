import {
  METHODS,
  civilDate,
  distanceToKaabaKm,
  intlLocale,
  iqamahTime,
  isRamadan,
  prayerDisplayLabel,
  qiblaBearing,
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
import { dayProgress, navGlyph, periodFor, pinGlyph, prayerGlyph } from './glyphs.js';

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

  $('next-name').textContent = prayerDisplayLabel(lang, settings, next.name, next.time);
  $('next-glyph').innerHTML = prayerGlyph(next.name, 36);
  const ms = (d: Date) => d.getTime();
  $('next').className = `next ${periodFor(ms(now), ms(day.fajr), ms(day.sunrise), ms(day.dhuhr), ms(day.asr), ms(day.maghrib), ms(day.isha))}`;
  const nextFajr = ms(computeDay(settings, now, 1).fajr);
  const span = Math.max(1, nextFajr - ms(day.fajr));
  const frac = (d: Date) => (ms(d) - ms(day.fajr)) / span;
  $('progress').innerHTML = dayProgress(
    (ms(now) - ms(day.fajr)) / span,
    (['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'] as const).map((n) => ({ frac: frac(day[n]), passed: ms(day[n]) <= ms(now) })),
    268,
  );
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
    const iq = name !== 'sunrise' ? iqamahTime(settings, name, time) : null;
    const iqHtml = iq ? `<span class="iqamah">${t(lang, 'home.iqamah').slice(0, 3)} ${formatTime(iq, tz, settings.hour12)}</span>` : '';
    li.innerHTML = `<span class="bar"></span>${prayerGlyph(name, name === 'sunrise' ? 16 : 18)}<span class="label">${prayerDisplayLabel(lang, settings, name, time)}</span><span>${iqHtml}<span class="time">${formatTime(time, tz, settings.hour12)}</span></span>`;
    list.appendChild(li);
  }

  $('method').textContent = METHODS.find((m) => m.id === settings.method)?.label.split(',')[0] ?? '';

  // Ramadan: Suhoor ends at Fajr, Iftar at Maghrib — whichever is next.
  const ram = $('ramadan');
  if (isRamadan(settings, now)) {
    const suhoor = now < day.fajr;
    const iftar = !suhoor && now < day.maghrib;
    const at = suhoor ? day.fajr : iftar ? day.maghrib : computeDay(settings, now, 1).fajr;
    ram.hidden = false;
    $('ramadan-name').textContent = t(lang, iftar ? 'prayer.iftar' : 'prayer.suhoor');
    $('ramadan-time').textContent = `${formatTime(at, tz, settings.hour12)} · ${formatCountdown(now, at)}`;
  } else ram.hidden = true;

  const { latitude, longitude } = settings.location;
  $('qibla').textContent = `${t(lang, 'home.qibla')} ${t(lang, 'home.qiblaBearing', { deg: Math.round(qiblaBearing(latitude, longitude)) })} · ${t(lang, 'home.qiblaDistance', { km: Math.round(distanceToKaabaKm(latitude, longitude)).toLocaleString(intlLocale(lang)) })}`;

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

$('pin').innerHTML = pinGlyph;
$('ico-month').innerHTML = navGlyph('calendar');
$('ico-settings').innerHTML = navGlyph('gear');
$('open-options').addEventListener('click', openOptions);
$('location-link').addEventListener('click', openOptions);
$('open-month').addEventListener('click', (e) => {
  e.preventDefault();
  chrome.tabs.create({ url: chrome.runtime.getURL('month.html') });
});
void render();
// The popup only lives while open; a 30 s tick keeps the countdown honest.
setInterval(() => void render(), 30_000);
