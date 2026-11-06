/**
 * Service worker. It is short-lived by design: Chrome kills it after ~30 s of
 * idle time and wakes it for alarms, notification clicks and storage changes.
 * Everything it needs is recomputed from storage on each wake.
 */
import {
  PRAYER_LABELS,
  formatTime,
  nextPrayer,
  upcomingEvents,
  type PrayerName,
  type Settings,
} from '@salat/core';
import { load, onChange, type Stored } from './storage.js';

const PREFIX = 'salat:';
const RESCHEDULE = `${PREFIX}reschedule`;
const SNOOZE = `${PREFIX}snooze:`;

// ---- scheduling -----------------------------------------------------------

async function reschedule(): Promise<void> {
  const stored = await load();
  const existing = await chrome.alarms.getAll();
  await Promise.all(
    existing
      .filter((a) => a.name.startsWith(PREFIX) && !a.name.startsWith(SNOOZE))
      .map((a) => chrome.alarms.clear(a.name)),
  );

  const now = new Date();
  if (stored.settings.notifications.enabled) {
    // 30 h horizon: covers today + tomorrow's Fajr with margin.
    for (const ev of upcomingEvents(stored.settings, now, 30 * 3600_000)) {
      await chrome.alarms.create(`${PREFIX}${ev.kind}:${ev.name}:${ev.at.getTime()}`, { when: ev.at.getTime() });
    }
  }
  // Safety net: rebuild the schedule daily even if nothing else wakes us.
  await chrome.alarms.create(RESCHEDULE, { when: now.getTime() + 24 * 3600_000 });
  await updateBadge(stored);
}

async function updateBadge(stored: Stored): Promise<void> {
  if (!stored.extras.badge) {
    await chrome.action.setBadgeText({ text: '' });
    return;
  }
  const next = nextPrayer(stored.settings, new Date());
  await chrome.action.setBadgeBackgroundColor({ color: '#2f6f5e' });
  await chrome.action.setBadgeTextColor?.({ color: '#ffffff' });
  await chrome.action.setBadgeText({ text: PRAYER_LABELS[next.name].slice(0, 4) });
  await chrome.action.setTitle({
    title: `Salat – ${PRAYER_LABELS[next.name]} at ${formatTime(next.time, stored.settings.location.timezone, stored.settings.hour12)}`,
  });
}

// ---- notifications --------------------------------------------------------

async function notify(kind: 'prayer' | 'pre' | 'snooze', name: PrayerName, settings: Settings): Promise<void> {
  const label = PRAYER_LABELS[name];
  const title = kind === 'pre' ? `${label} in ${settings.notifications.preReminderMinutes} min` : `${label} time`;
  const message =
    kind === 'pre'
      ? `${label} prayer is at ${formatTime(nextPrayerTime(settings, name), settings.location.timezone, settings.hour12)}.`
      : `It is time for ${label} prayer.`;

  const id = `${PREFIX}notif:${name}:${Date.now()}`;
  await chrome.notifications.create(id, {
    type: 'basic',
    iconUrl: 'icons/icon128.png',
    title,
    message,
    priority: 1,
    silent: true, // we handle sound ourselves (or not at all)
    buttons: kind === 'prayer' ? [{ title: `Snooze ${settings.notifications.snoozeMinutes} min` }] : [],
  });

  if (kind === 'prayer' && settings.notifications.sound) await playChime();
}

function nextPrayerTime(settings: Settings, name: PrayerName): Date {
  const now = new Date();
  for (const ev of upcomingEvents(settings, now, 30 * 3600_000)) {
    if (ev.kind === 'prayer' && ev.name === name) return ev.at;
  }
  return now;
}

async function playChime(): Promise<void> {
  try {
    const has = await chrome.offscreen.hasDocument?.();
    if (!has) {
      await chrome.offscreen.createDocument({
        url: 'offscreen.html',
        reasons: [chrome.offscreen.Reason.AUDIO_PLAYBACK],
        justification: 'Play a short chime at prayer time when the user enabled sound.',
      });
    }
    await chrome.runtime.sendMessage({ type: 'play-chime' });
  } catch (e) {
    console.warn('chime failed', e);
  }
}

// ---- events ---------------------------------------------------------------

chrome.runtime.onInstalled.addListener(() => void reschedule());
chrome.runtime.onStartup.addListener(() => void reschedule());
onChange(() => void reschedule());

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (!alarm.name.startsWith(PREFIX)) return;
  const stored = await load();

  if (alarm.name === RESCHEDULE) {
    await reschedule();
    return;
  }
  if (alarm.name.startsWith(SNOOZE)) {
    const name = alarm.name.slice(SNOOZE.length).split(':')[0] as PrayerName;
    await notify('snooze', name, stored.settings);
    return;
  }
  const [, kind, name] = alarm.name.split(':'); // salat:<kind>:<name>:<ts>
  if (kind === 'prayer' || kind === 'pre') {
    await notify(kind, name as PrayerName, stored.settings);
    if (kind === 'prayer') await updateBadge(stored);
    // After tomorrow's Fajr has been scheduled we may be running low; top up.
    const remaining = (await chrome.alarms.getAll()).filter((a) => a.name.startsWith(`${PREFIX}prayer:`));
    if (remaining.length < 3) await reschedule();
  }
});

chrome.notifications.onButtonClicked.addListener(async (id, index) => {
  if (!id.startsWith(`${PREFIX}notif:`) || index !== 0) return;
  const name = id.split(':')[2] as PrayerName;
  const { settings } = await load();
  await chrome.alarms.create(`${SNOOZE}${name}:${Date.now()}`, {
    when: Date.now() + settings.notifications.snoozeMinutes * 60_000,
  });
  chrome.notifications.clear(id);
});

chrome.notifications.onClicked.addListener((id) => {
  if (id.startsWith(PREFIX)) chrome.notifications.clear(id);
});

chrome.runtime.onMessage.addListener((msg, _sender, respond) => {
  if (msg?.type === 'test-notification') {
    load().then(({ settings }) => notify('prayer', 'dhuhr', settings)).then(() => respond(true));
    return true;
  }
  if (msg?.type === 'reschedule') {
    reschedule().then(() => respond(true));
    return true;
  }
  return false;
});
