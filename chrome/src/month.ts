import {
  civilDate,
  formatTime,
  intlLocale,
  monthCsv,
  monthTable,
  prayerDisplayLabel,
  t,
  type PrayerName,
} from '@salat/core';
import { load } from './storage.js';
import { localiseDocument, pageLanguage } from './ui-i18n.js';

const ORDER: PrayerName[] = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];
const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;

let year = 0;
let month = 0;

async function render(): Promise<void> {
  const { settings } = await load();
  const lang = pageLanguage(settings);
  localiseDocument(lang);
  const tz = settings.location.timezone;
  const now = new Date();
  const today = civilDate(now, tz);
  if (!year) { year = today.year; month = today.month; }

  $('subtitle').textContent = settings.location.label;
  $('month-name').textContent = new Intl.DateTimeFormat(intlLocale(lang), { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(year, month - 1, 15)));

  const rows = monthTable(settings, year, month);
  const probe = new Date(Date.UTC(year, month - 1, 15, 12));
  $('head').innerHTML = `<th></th>` + ORDER.map((p) => `<th>${prayerDisplayLabel(lang, settings, p, probe).replace("Jumu'ah", t(lang, 'prayer.dhuhr'))}</th>`).join('');
  const body = $('rows');
  body.innerHTML = '';
  for (const r of rows) {
    const tr = document.createElement('tr');
    const isToday = r.date === `${today.year}-${String(today.month).padStart(2, '0')}-${String(today.day).padStart(2, '0')}`;
    tr.className = `${r.friday ? 'friday ' : ''}${isToday ? 'today' : ''}`;
    const dayNum = Number(r.date.slice(8));
    const wd = new Intl.DateTimeFormat(intlLocale(lang), { weekday: 'short', timeZone: 'UTC' }).format(new Date(Date.UTC(year, month - 1, dayNum)));
    const hijri = r.hijri ? `<span class="hijri">${r.hijri.day}/${r.hijri.month}</span>` : '';
    tr.innerHTML = `<td>${dayNum} ${wd}${hijri}</td>` + ORDER.map((p) => `<td class="${p === 'sunrise' ? 'sunrise' : ''}">${formatTime(r.times[p], tz, settings.hour12)}</td>`).join('');
    body.appendChild(tr);
  }

  $('export').onclick = () => {
    const csv = monthCsv(settings, rows);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `salat-${settings.location.label.replace(/[^A-Za-z0-9]+/g, '-').replace(/^-|-$/g, '')}-${year}-${String(month).padStart(2, '0')}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  };
}

function shift(delta: number): void {
  month += delta;
  if (month > 12) { month = 1; year++; }
  if (month < 1) { month = 12; year--; }
  void render();
}

$('prev').addEventListener('click', () => shift(-1));
$('next').addEventListener('click', () => shift(1));
$('today').addEventListener('click', () => { year = 0; void render(); });
void render();
