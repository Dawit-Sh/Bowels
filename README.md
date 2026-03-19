# Bowels

Privacy-first gut health tracker (Expo + React Native, TypeScript). Local-first storage (SQLite) and tap-only inputs.

## Users

- Home → **Start Session** → timer runs → **Finish**
- Answer quick tap-only questions (max 5)
- View History, Analytics, Insights, Weekly Wrapped
- Settings: theme, notifications, export

## Developers

### Requirements

- Node.js (LTS recommended)
- Expo CLI via `npx expo`

### Setup

```bash
npm install
npx expo start
```

Notes:
- Notification action buttons (Open/Finish/Cancel) work best in a **development build** (Expo Go on Android has limitations).

### Scripts

```bash
npm run lint
npm run typecheck
npm run build:apk   # EAS preview universal APK
npm run build:aab   # EAS production Android App Bundle
```

### Structure

- `app/` Expo Router routes
- `screens/` screen implementations
- `components/` UI + illustrations
- `db/` SQLite schema + queries
- `store/` Zustand state
- `utils/` notifications, exports, insights, wrapped

## Contributing

See `CONTRIBUTING.md`.

## GitHub Releases (auto-attach APK)

This repo includes a workflow that can build an APK on tag push and attach it to a GitHub Release.

1) Create an Expo access token and add it to GitHub repo secrets:
- `EXPO_TOKEN`

2) Tag and push:

```bash
git tag -a vX.Y.Z -m "vX.Y.Z"
git push origin vX.Y.Z
```

