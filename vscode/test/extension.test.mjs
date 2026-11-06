// Runs the bundled extension against a minimal stub of the `vscode` API.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import Module from 'node:module';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

function makeVscode(config = {}, uiLanguage = 'en') {
  const messages = [];
  const items = [];
  let cfgListener;
  const api = {
    StatusBarAlignment: { Left: 1, Right: 2 },
    ConfigurationTarget: { Global: 1 },
    MarkdownString: class { constructor() { this.value = ''; } appendMarkdown(s) { this.value += s; return this; } },
    window: {
      createStatusBarItem: () => { const it = { shown: false, show() { it.shown = true; }, hide() { it.shown = false; }, dispose() {} }; items.push(it); return it; },
      showInformationMessage: async (msg, ...actions) => { messages.push({ msg, actions }); return undefined; },
      showQuickPick: async () => undefined,
      createQuickPick: () => ({ onDidChangeValue() {}, onDidAccept() {}, onDidHide() {}, show() {}, hide() {}, dispose() {} }),
      setStatusBarMessage() {},
    },
    env: { language: uiLanguage },
    commands: { registerCommand: () => ({ dispose() {} }), executeCommand: async () => {} },
    workspace: {
      getConfiguration: () => ({
        get: (k, d) => (k in config ? config[k] : d),
        update: async (k, v) => { config[k] = v; cfgListener?.({ affectsConfiguration: () => true }); },
      }),
      onDidChangeConfiguration: (cb) => { cfgListener = cb; return { dispose() {} }; },
    },
    _messages: messages,
    _items: items,
    _fireConfig: () => cfgListener?.({ affectsConfiguration: () => true }),
  };
  return api;
}

function loadExtension(vscodeStub) {
  const originalLoad = Module._load;
  Module._load = function (request, ...rest) {
    if (request === 'vscode') return vscodeStub;
    return originalLoad.call(this, request, ...rest);
  };
  const path = require.resolve('../dist/extension.js');
  delete require.cache[path];
  const ext = require(path);
  Module._load = originalLoad;
  return ext;
}

test('activates, shows a status bar item with next prayer and countdown', () => {
  const vs = makeVscode({ 'location.latitude': 48.78, 'location.longitude': 9.18, 'location.timezone': 'Europe/Berlin', 'location.label': 'Stuttgart' });
  const ext = loadExtension(vs);
  const subs = [];
  ext.activate({ subscriptions: subs });
  const item = vs._items[0];
  assert.ok(item.shown);
  assert.match(item.text, /^\$\(bell\) (Fajr|Dhuhr|Asr|Maghrib|Isha) \d{2}:\d{2} · (<1m|\d+m|\d+h \d{2}m)$/);
  assert.match(item.tooltip.value, /Stuttgart/);
  assert.match(item.tooltip.value, /Fajr/);
  assert.equal(item.command, 'salat.showTimes');
  ext.deactivate();
});

test('guesses location from time zone when none is configured', () => {
  const vs = makeVscode({});
  const ext = loadExtension(vs);
  ext.activate({ subscriptions: [] });
  assert.match(vs._items[0].tooltip.value, /guessed from time zone/);
  ext.deactivate();
});

test('status bar hides when disabled and reacts to configuration changes', () => {
  const vs = makeVscode({ 'statusBar.enabled': true });
  const ext = loadExtension(vs);
  ext.activate({ subscriptions: [] });
  assert.ok(vs._items[0].shown);
  vs.workspace.getConfiguration().update('statusBar.enabled', false);
  assert.equal(vs._items[0].shown, false);
  ext.deactivate();
});

test('compact mode drops the clock time', () => {
  const vs = makeVscode({ 'statusBar.compact': true });
  const ext = loadExtension(vs);
  ext.activate({ subscriptions: [] });
  assert.match(vs._items[0].text, /^\$\(bell\) \w+ (<1m|\d+m|\d+h \d{2}m)$/);
  ext.deactivate();
});

test('follows the editor language and the salat.language override', () => {
  const de = makeVscode({}, 'de');
  const extDe = loadExtension(de);
  extDe.activate({ subscriptions: [] });
  assert.match(de._items[0].tooltip.value, /Sonnenaufgang/);
  extDe.deactivate();

  const ar = makeVscode({ language: 'ar' }, 'en-US');
  const extAr = loadExtension(ar);
  extAr.activate({ subscriptions: [] });
  assert.match(ar._items[0].text, /\$\(bell\) (الفجر|الظهر|العصر|المغرب|العشاء) /);
  extAr.deactivate();
});
