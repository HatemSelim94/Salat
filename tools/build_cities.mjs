// Builds core/src/cities.json from the `city-timezones` npm dataset (MIT, MaxMind-derived),
// merged with the hand-curated list in tools/cities-curated.json (preferred spellings, aliases).
// Run occasionally; the output is committed. Needs network for `npm pack`.
//
//   node tools/build_cities.mjs [minPopulation=200000]
import { execSync } from 'node:child_process';
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const minPop = Number(process.argv[2] ?? 200_000);
const curated = JSON.parse(readFileSync('tools/cities-curated.json', 'utf8'));

const dir = mkdtempSync(join(tmpdir(), 'ctz-'));
const tgz = execSync('npm pack city-timezones --silent', { cwd: dir }).toString().trim();
execSync(`tar xzf ${tgz}`, { cwd: dir });
const raw = JSON.parse(readFileSync(join(dir, 'package/data/cityMap.json'), 'utf8'));

// Dedupe (same name + country keeps the largest), drop rows without a time zone.
const byKey = new Map();
for (const c of raw) {
  if (!c.timezone || typeof c.iso2 !== 'string' || c.iso2.length !== 2 || !c.city_ascii) continue; // a few rows carry iso2 = -99
  const k = `${c.city_ascii}|${c.iso2}`;
  if (!byKey.has(k) || byKey.get(k).pop < c.pop) byKey.set(k, c);
}
const big = [...byKey.values()].filter((c) => c.pop >= minPop);

// Disambiguate same-named cities inside one country with their province.
const nameCount = new Map();
for (const c of big) { const k = `${c.city_ascii}|${c.iso2}`; nameCount.set(k, (nameCount.get(k) ?? 0) + 1); }
const dupName = new Map();
for (const c of raw) if (c.pop >= minPop) { const k = `${c.city_ascii}|${c.iso2}`; dupName.set(k, (dupName.get(k) ?? 0) + 1); }

const r2 = (n) => Math.round(n * 100) / 100;
const norm = (s) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
const latin = (s) => /^[\p{Script=Latin}\p{P}\s\d]+$/u.test(s);
// Curated ↔ dataset match: same country and (same name/alias, or within ~10 km).
const sameName = (k, c) => [k.name, ...(k.alt ?? [])].map(norm).some((n) => n === norm(c.city_ascii) || n === norm(c.city));

// Pair curated entries with dataset rows: by name first, then by proximity for what is left.
const pair = new Map(); // dataset row → curated index
const usedCurated = new Set();
for (const pass of [sameName, (k, c) => Math.abs(k.lat - c.lat) < 0.1 && Math.abs(k.lng - c.lng) < 0.1]) {
  for (const c of big) {
    if (pair.has(c)) continue;
    const i = curated.findIndex((k, idx) => !usedCurated.has(idx) && k.country === c.iso2 && pass(k, c));
    if (i >= 0) { pair.set(c, i); usedCurated.add(i); }
  }
}

const out = [];
for (const c of big) {
  const cur = pair.has(c) ? curated[pair.get(c)] : null;
  const alts = new Set();
  let name = latin(c.city) ? c.city : c.city_ascii; // native spelling when it is Latin script (Nürnberg, Zürich)
  if (cur) {
    name = cur.name;
    for (const a of cur.alt ?? []) alts.add(a);
    if (c.city_ascii !== name) alts.add(c.city_ascii);
  }
  if (c.city && c.city !== name) alts.add(c.city);
  if (c.city_ascii !== name) alts.add(c.city_ascii);
  if (dupName.get(`${c.city_ascii}|${c.iso2}`) > 1 && c.province) name = `${name} (${c.province})`;
  const row = { name, country: c.iso2, lat: r2(c.lat), lng: r2(c.lng), tz: c.timezone, pop: Math.round(c.pop) };
  if (alts.size) row.alt = [...alts].filter((a) => a !== name).join('|');
  out.push(row);
}
// Curated cities the dataset lacks (or that are below the threshold) are kept.
curated.forEach((k, i) => {
  if (usedCurated.has(i)) return;
  const row = { name: k.name, country: k.country, lat: k.lat, lng: k.lng, tz: k.tz, pop: k.pop ?? 0 };
  if (k.alt?.length) row.alt = k.alt.join('|');
  out.push(row);
});
out.sort((a, b) => b.pop - a.pop || a.name.localeCompare(b.name));
for (const row of out) delete row.pop; // population only orders the list

writeFileSync('core/src/cities.json', JSON.stringify(out) + '\n');
console.log(`core/src/cities.json: ${out.length} cities (${curated.length} curated), min population ${minPop}`);
