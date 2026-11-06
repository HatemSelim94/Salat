// Inline SVG glyphs (same geometry as the Qt and Android apps). All use currentColor.

const f = (n: number): string => n.toFixed(2);

function crescent(cx: number, cy: number, r: number): string {
  const tipX = cx + r * 0.55, dy = r * 0.75;
  const ir = r * 0.757;
  return `<path d="M${f(tipX)} ${f(cy - dy)} A${f(r)} ${f(r)} 0 1 0 ${f(tipX)} ${f(cy + dy)} A${f(ir)} ${f(ir)} 0 1 1 ${f(tipX)} ${f(cy - dy)} Z"/>`;
}

function rays(cx: number, cy: number, r1: number, r2: number, onlyTop: boolean): string {
  const angles = onlyTop ? [200, 240, 270, 300, 340] : [0, 45, 90, 135, 180, 225, 270, 315];
  return angles.map((a) => {
    const dx = Math.cos((a * Math.PI) / 180), dy = Math.sin((a * Math.PI) / 180);
    return `<line x1="${f(cx + dx * r1)}" y1="${f(cy + dy * r1)}" x2="${f(cx + dx * r2)}" y2="${f(cy + dy * r2)}"/>`;
  }).join('');
}

const line = (x1: number, y1: number, x2: number, y2: number, extra = ''): string =>
  `<line x1="${f(x1)}" y1="${f(y1)}" x2="${f(x2)}" y2="${f(y2)}"${extra}/>`;

/** 20×20 glyph for a prayer; `size` sets the rendered box. */
export function prayerGlyph(name: string, size = 18): string {
  const w = 20, h = 20, cx = 10, cy = 10;
  let body = '';
  switch (name) {
    case 'fajr':
      body = crescent(cx - w * 0.04, cy - h * 0.04, w * 0.3) + line(w * 0.15, h * 0.88, w * 0.85, h * 0.88, ' opacity=".6"');
      break;
    case 'sunrise':
      body = line(w * 0.1, h * 0.68, w * 0.9, h * 0.68)
        + `<path d="M${f(cx - w * 0.24)} ${f(h * 0.68)} A${f(w * 0.24)} ${f(w * 0.24)} 0 0 1 ${f(cx + w * 0.24)} ${f(h * 0.68)} Z" stroke="none"/>`
        + rays(cx, h * 0.68, w * 0.34, w * 0.44, true);
      break;
    case 'dhuhr':
      body = `<circle cx="${cx}" cy="${cy}" r="${f(w * 0.2)}" stroke="none"/>` + rays(cx, cy, w * 0.3, w * 0.42, false);
      break;
    case 'asr': {
      const x = cx + w * 0.05, y = cy + h * 0.08;
      body = `<circle cx="${f(x)}" cy="${f(y)}" r="${f(w * 0.17)}" stroke="none"/>` + rays(x, y, w * 0.25, w * 0.34, false);
      break;
    }
    case 'maghrib':
      body = line(w * 0.1, h * 0.62, w * 0.9, h * 0.62)
        + `<path d="M${f(cx - w * 0.19)} ${f(h * 0.62)} A${f(w * 0.2)} ${f(w * 0.2)} 0 0 1 ${f(cx + w * 0.19)} ${f(h * 0.62)} Z" stroke="none"/>`
        + line(cx - w * 0.14, h * 0.8, cx + w * 0.14, h * 0.8);
      break;
    default:
      body = crescent(cx - w * 0.08, cy + h * 0.06, w * 0.3) + `<circle cx="${f(cx + w * 0.3)}" cy="${f(cy - h * 0.3)}" r="1" stroke="none"/>`;
  }
  return `<svg class="glyph" width="${size}" height="${size}" viewBox="0 0 20 20" fill="currentColor" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true">${body}</svg>`;
}

export function navGlyph(icon: 'calendar' | 'compass' | 'gear', size = 16): string {
  let body = '';
  if (icon === 'calendar') {
    body = `<rect x="2.4" y="3.6" width="15.2" height="14.4" rx="2"/>${line(2.4, 7.6, 17.6, 7.6)}${line(6.4, 1.6, 6.4, 5.2)}${line(13.6, 1.6, 13.6, 5.2)}`
      + [0, 1, 2].flatMap((i) => [0, 1].map((j) => `<circle cx="${f(6.4 + i * 3.6)}" cy="${f(11 + j * 3.6)}" r=".9" fill="currentColor" stroke="none"/>`)).join('');
  } else if (icon === 'compass') {
    body = `<circle cx="10" cy="10" r="8"/><path d="M12.4 7.6 8.8 8.8 7.6 12.4 11.2 11.2Z" fill="currentColor" stroke="none"/>`;
  } else {
    body = `<circle cx="10" cy="10" r="2.8"/><circle cx="10" cy="10" r="5.2"/>`
      + [0, 1, 2, 3, 4, 5, 6, 7].map((i) => { const a = (i * Math.PI) / 4, dx = Math.cos(a), dy = Math.sin(a); return line(10 + dx * 5.2, 10 + dy * 5.2, 10 + dx * 8, 10 + dy * 8); }).join('');
  }
  return `<svg width="${size}" height="${size}" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" aria-hidden="true">${body}</svg>`;
}

export const pinGlyph = `<svg class="pin" width="12" height="14" viewBox="0 0 12 14" aria-hidden="true"><circle cx="6" cy="5.5" r="5.5" fill="currentColor"/><path d="M1.6 8 6 14 10.4 8Z" fill="currentColor"/><circle cx="6" cy="5.5" r="2" fill="var(--bg)"/></svg>`;

/** Period of the day for the next-prayer card colours. */
export function periodFor(now: number, fajr: number, sunrise: number, dhuhr: number, asr: number, maghrib: number, isha: number): string {
  if (now < fajr) return 'night';
  if (now < sunrise) return 'dawn';
  if (now < dhuhr) return 'morning';
  if (now < asr) return 'day';
  if (now < maghrib) return 'afternoon';
  if (now < isha) return 'dusk';
  return 'night';
}

/** Fajr → next-Fajr progress line as SVG; `dots` are fractions 0..1 with a passed flag. */
export function dayProgress(progress: number, dots: Array<{ frac: number; passed: boolean }>, width: number): string {
  const pad = 6, x = (fr: number) => pad + Math.min(1, Math.max(0, fr)) * (width - 2 * pad);
  const px = x(progress);
  return `<svg class="progress" width="${width}" height="14" viewBox="0 0 ${width} 14" aria-hidden="true">`
    + `<line x1="${pad}" y1="7" x2="${width - pad}" y2="7" class="track"/>`
    + (progress > 0 ? `<line x1="${pad}" y1="7" x2="${f(px)}" y2="7" class="fill"/>` : '')
    + dots.map((d) => `<circle cx="${f(x(d.frac))}" cy="7" r="4" class="${d.passed ? 'fill' : 'track'}"/>`).join('')
    + `<circle cx="${f(px)}" cy="7" r="6" class="fill"/><circle cx="${f(px)}" cy="7" r="2.5" fill="#fff"/></svg>`;
}
