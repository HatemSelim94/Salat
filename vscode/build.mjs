// Bundles src/extension.ts (+ @salat/core) into dist/extension.js (CommonJS, as VS Code requires).
import { build, context } from 'esbuild';

const watch = process.argv.includes('--watch');
const options = {
  entryPoints: ['src/extension.ts'],
  bundle: true,
  format: 'cjs',
  platform: 'node',
  target: 'node20',
  outfile: 'dist/extension.js',
  external: ['vscode'],
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
  console.log('built vscode/dist/extension.js');
}
