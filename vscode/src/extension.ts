import * as vscode from 'vscode';
import {
  METHODS,
  cityToLocation,
  prayerLabel,
  resolveLanguage,
  t as tr,
  type Language,
  civilDate,
  computeDay,
  currentPrayer,
  defaultLocationForTimezone,
  defaultSettings,
  formatCountdown,
  formatTime,
  nextPrayer,
  searchCities,
  systemTimezone,
  upcomingEvents,
  type PrayerName,
  type ScheduledEvent,
  type Settings,
} from '@salat/core';

const TICK_MS = 60_000;
/** Events older than this at tick time are considered missed (sleep/wake) and skipped silently. */
const STALE_MS = 15 * 60_000;
const MAX_TIMEOUT = 2 ** 31 - 1;

let statusItem: vscode.StatusBarItem | undefined;
let tickTimer: NodeJS.Timeout | undefined;
let eventTimer: NodeJS.Timeout | undefined;
let settings: Settings;
let queue: ScheduledEvent[] = [];
let locationGuessed = false;
let lang: Language = 'en';
const t = (key: string, params?: Record<string, string | number>) => tr(lang, key, params);
const label = (p: PrayerName) => prayerLabel(lang, p);

// ---- settings ---------------------------------------------------------------

function readSettings(): Settings {
  const cfg = vscode.workspace.getConfiguration('salat');
  const lat = cfg.get<number | null>('location.latitude');
  const lng = cfg.get<number | null>('location.longitude');
  const tzCfg = (cfg.get<string>('location.timezone') ?? '').trim();
  const label = (cfg.get<string>('location.label') ?? '').trim();

  let location;
  if (typeof lat === 'number' && typeof lng === 'number') {
    locationGuessed = false;
    location = { latitude: lat, longitude: lng, timezone: tzCfg || systemTimezone(), label: label || `${lat.toFixed(2)}, ${lng.toFixed(2)}` };
  } else {
    locationGuessed = true;
    location =
      defaultLocationForTimezone(tzCfg || systemTimezone()) ??
      { latitude: 21.39, longitude: 39.86, timezone: 'Asia/Riyadh', label: 'Makkah, SA' };
  }

  const s = defaultSettings(location);
  s.language = cfg.get('language', 'auto');
  lang = resolveLanguage(s.language, vscode.env.language);
  s.method = cfg.get('method', s.method);
  s.madhab = cfg.get('madhab', s.madhab);
  s.highLatitudeRule = cfg.get('highLatitudeRule', s.highLatitudeRule);
  s.adjustments = cfg.get('adjustments', {});
  s.notifications.enabled = cfg.get('notifications.enabled', true);
  s.notifications.preReminderMinutes = cfg.get('notifications.preReminderMinutes', 0);
  s.notifications.snoozeMinutes = cfg.get('notifications.snoozeMinutes', 10);
  s.hour12 = cfg.get('hour12', false);
  return s;
}

// ---- status bar -------------------------------------------------------------

function updateStatus(): void {
  if (!statusItem) return;
  const cfg = vscode.workspace.getConfiguration('salat');
  if (!cfg.get('statusBar.enabled', true)) {
    statusItem.hide();
    return;
  }
  const now = new Date();
  const tz = settings.location.timezone;
  const next = nextPrayer(settings, now);
  const compact = cfg.get('statusBar.compact', false);
  const clock = formatTime(next.time, tz, settings.hour12);
  statusItem.text = compact
    ? `$(bell) ${label(next.name)} ${formatCountdown(now, next.time)}`
    : `$(bell) ${label(next.name)} ${clock} · ${formatCountdown(now, next.time)}`;

  const day = computeDay(settings, now);
  const current = currentPrayer(settings, now);
  const md = new vscode.MarkdownString();
  md.appendMarkdown(`**${t('app.name')}** — ${settings.location.label}  \n`);
  for (const p of ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as PrayerName[]) {
    const mark = p === current.name ? '●' : '○';
    md.appendMarkdown(`${mark} ${label(p)} — ${formatTime(day[p], tz, settings.hour12)}  \n`);
  }
  md.appendMarkdown(`\n_${METHODS.find((m) => m.id === settings.method)?.label ?? settings.method}_`);
  if (locationGuessed) md.appendMarkdown(`  \n_${t('vscode.guessed')}_`);
  statusItem.tooltip = md;
  statusItem.show();
}

// ---- scheduling -------------------------------------------------------------

function rebuildQueue(): void {
  const now = new Date();
  queue = settings.notifications.enabled ? upcomingEvents(settings, now, 36 * 3600_000) : [];
  armNextEvent();
}

function armNextEvent(): void {
  if (eventTimer) clearTimeout(eventTimer);
  eventTimer = undefined;
  const next = queue[0];
  if (!next) return;
  const delay = Math.min(MAX_TIMEOUT, Math.max(0, next.at.getTime() - Date.now()));
  eventTimer = setTimeout(onEventTimer, delay);
}

function onEventTimer(): void {
  drainDue(Date.now());
  if (queue.length < 2) rebuildQueue();
  else armNextEvent();
}

/** Fire every due event; skip ones that are too old (we were asleep). */
function drainDue(nowMs: number): void {
  while (queue.length && queue[0].at.getTime() <= nowMs) {
    const ev = queue.shift()!;
    if (nowMs - ev.at.getTime() <= STALE_MS) void notify(ev);
  }
}

function tick(): void {
  // Catches timer drift after sleep/wake and keeps the countdown honest.
  drainDue(Date.now());
  if (!queue.length) rebuildQueue();
  else armNextEvent();
  updateStatus();
}

// ---- notifications ----------------------------------------------------------

async function notify(ev: ScheduledEvent, snoozed = false): Promise<void> {
  const name = label(ev.name);
  const tz = settings.location.timezone;
  const pre = settings.notifications.preReminderMinutes;
  const prayerAt = new Date(ev.at.getTime() + (ev.kind === 'pre' ? pre * 60_000 : 0));
  const message =
    ev.kind === 'pre'
      ? `${t('notif.preTitle', { prayer: name, n: pre })} (${formatTime(prayerAt, tz, settings.hour12)})`
      : snoozed
        ? t('notif.snoozedTitle', { prayer: name })
        : t('notif.prayerBody', { prayer: name, time: formatTime(ev.at, tz, settings.hour12) });
  const snooze = t('notif.snooze', { n: settings.notifications.snoozeMinutes });
  const dismiss = t('notif.dismiss');
  const choice =
    ev.kind === 'prayer'
      ? await vscode.window.showInformationMessage(message, snooze, dismiss)
      : await vscode.window.showInformationMessage(message, dismiss);
  if (choice === snooze) {
    setTimeout(() => void notify({ ...ev, at: new Date() }, true), settings.notifications.snoozeMinutes * 60_000);
  }
}

// ---- commands ---------------------------------------------------------------

async function showTimes(): Promise<void> {
  const now = new Date();
  const day = computeDay(settings, now);
  const current = currentPrayer(settings, now);
  const tz = settings.location.timezone;
  const items: vscode.QuickPickItem[] = (['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as PrayerName[]).map((p) => ({
    label: `${p === current.name ? '$(circle-filled)' : '$(circle-outline)'} ${label(p)}`,
    description: formatTime(day[p], tz, settings.hour12),
    detail: p === current.name ? t('vscode.current') : undefined,
  }));
  const next = nextPrayer(settings, now);
  const tomorrow = civilDate(next.time, tz).day !== civilDate(now, tz).day ? ` (${t('home.tomorrow')})` : '';
  await vscode.window.showQuickPick(items, {
    title: `${t('app.name')} — ${settings.location.label} · ${day.date}`,
    placeHolder: `${t('vscode.next')}: ${label(next.name)} ${formatTime(next.time, tz, settings.hour12)}${tomorrow} · ${t('home.in')} ${formatCountdown(now, next.time)}`,
  });
}

async function setCity(): Promise<void> {
  const qp = vscode.window.createQuickPick<vscode.QuickPickItem & { idx: number }>();
  qp.title = t('vscode.setCity');
  qp.placeholder = t('vscode.typeCity');
  qp.matchOnDescription = true;
  let results = searchCities('', 0);
  qp.onDidChangeValue((v) => {
    results = searchCities(v, 10);
    qp.items = results.map((c, idx) => ({ label: `${c.name}, ${c.country}`, description: c.tz, idx }));
  });
  qp.onDidAccept(async () => {
    const pick = qp.selectedItems[0];
    qp.hide();
    if (!pick) return;
    const loc = cityToLocation(results[pick.idx]);
    const cfg = vscode.workspace.getConfiguration('salat');
    const target = vscode.ConfigurationTarget.Global;
    await cfg.update('location.label', loc.label, target);
    await cfg.update('location.latitude', loc.latitude, target);
    await cfg.update('location.longitude', loc.longitude, target);
    await cfg.update('location.timezone', loc.timezone, target);
    vscode.window.setStatusBarMessage(t('vscode.locationSet', { label: loc.label }), 3000);
  });
  qp.onDidHide(() => qp.dispose());
  qp.show();
}

// ---- lifecycle --------------------------------------------------------------

export function activate(context: vscode.ExtensionContext): void {
  settings = readSettings();

  statusItem = vscode.window.createStatusBarItem('salat.next', vscode.StatusBarAlignment.Right, 100);
  statusItem.name = `${t('app.name')}: ${t('home.nextPrayer')}`;
  statusItem.command = 'salat.showTimes';
  context.subscriptions.push(statusItem);

  context.subscriptions.push(
    vscode.commands.registerCommand('salat.showTimes', showTimes),
    vscode.commands.registerCommand('salat.setCity', setCity),
    vscode.commands.registerCommand('salat.openSettings', () =>
      vscode.commands.executeCommand('workbench.action.openSettings', '@ext:HatemSelim94.salat'),
    ),
    vscode.workspace.onDidChangeConfiguration((e) => {
      if (!e.affectsConfiguration('salat')) return;
      settings = readSettings();
      rebuildQueue();
      updateStatus();
    }),
  );

  rebuildQueue();
  updateStatus();
  // Align the minute tick to the wall clock so the countdown changes on the minute.
  const msToNextMinute = TICK_MS - (Date.now() % TICK_MS);
  tickTimer = setTimeout(() => {
    tick();
    tickTimer = setInterval(tick, TICK_MS);
  }, msToNextMinute);
}

export function deactivate(): void {
  if (tickTimer) clearTimeout(tickTimer), clearInterval(tickTimer);
  if (eventTimer) clearTimeout(eventTimer);
}
