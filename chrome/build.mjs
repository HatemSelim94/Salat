// Bundles the extension into dist/ (load that folder unpacked, or zip it).
import { build, context } from 'esbuild';
import { cpSync, mkdirSync, rmSync } from 'node:fs';

const watch = process.argv.includes('--watch');
rmSync('dist', { recursive: true, force: true });
mkdirSync('dist', { recursive: true });
cpSync('static', 'dist', { recursive: true });

const options = {
  entryPoints: {
    background: 'src/background.ts',
    popup: 'src/popup.ts',
    options: 'src/options.ts',
    offscreen: 'src/offscreen.ts',
    month: 'src/month.ts',
  },
  bundle: true,
  format: 'esm',
  target: 'chrome120',
  outdir: 'dist',
  minify: !watch,
  sourcemap: watch ? 'inline' : false,
  legalComments: 'none',
};

if (watch) {
  const ctx = await context(options);
  await ctx.watch();
  console.log('watching…');
} else {
  await build(options);
  console.log('built chrome/dist');
}
