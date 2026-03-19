import fs from 'node:fs';

const file = process.argv[2];
if (!file) {
  console.error('Usage: node scripts/extract-apk-url.mjs <eas-build.json>');
  process.exit(1);
}

const raw = fs.readFileSync(file, 'utf8');
const json = JSON.parse(raw);

const urls = [];
walk(json, (v) => {
  if (typeof v === 'string' && /^https?:\/\//.test(v)) urls.push(v);
});

const apkUrl =
  urls.find((u) => /\.apk(\?|$)/i.test(u)) ||
  urls.find((u) => /applicationArchiveUrl/i.test(u)) ||
  urls.find((u) => /artifacts/i.test(u) && /https?:\/\//.test(u)) ||
  urls[0];

if (!apkUrl) {
  console.error('No URL found in EAS build JSON.');
  process.exit(2);
}

process.stdout.write(apkUrl);

function walk(obj, fn) {
  if (Array.isArray(obj)) {
    obj.forEach((x) => walk(x, fn));
    return;
  }
  if (obj && typeof obj === 'object') {
    for (const k of Object.keys(obj)) walk(obj[k], fn);
    return;
  }
  fn(obj);
}

