import fs from "node:fs";

const inputPath = process.argv[2];

if (!inputPath) {
  console.error("Usage: node scripts/extract-apk-url.mjs <build-json-path>");
  process.exit(1);
}

const raw = fs.readFileSync(inputPath, "utf8");
const parsed = JSON.parse(raw);
const builds = Array.isArray(parsed) ? parsed : [parsed];

const latestBuild = builds.find((build) => build?.platform === "ANDROID" || build?.platform === "android") ?? builds[0];
const artifactUrl =
  latestBuild?.artifacts?.buildUrl ??
  latestBuild?.artifacts?.applicationArchiveUrl ??
  latestBuild?.artifacts?.url ??
  latestBuild?.buildArtifactUrl ??
  latestBuild?.applicationArchiveUrl;

if (!artifactUrl) {
  console.error("Could not find APK artifact URL in build output.");
  process.exit(1);
}

process.stdout.write(String(artifactUrl));
