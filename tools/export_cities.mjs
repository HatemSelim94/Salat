// Exports core's bundled city list as JSON for the Qt and Android apps,
// so there is exactly one source of truth (core/src/cities.ts).
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { CITIES } from '../core/dist/index.js';

const targets = ['desktop/resources/cities.json', 'android/app/src/main/assets/cities.json'];
const json = JSON.stringify(CITIES);
for (const t of targets) {
  mkdirSync(dirname(t), { recursive: true });
  writeFileSync(t, json + '\n');
  console.log(`${t}: ${CITIES.length} cities`);
}
