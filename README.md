# Bowels

<div align="center">

### A calm, local-first digestive health tracker built with Expo and React Native

Track sessions, log daily health signals, surface patterns, and build a steadier rhythm over time.

![Expo](https://img.shields.io/badge/Expo-55-1f2937?style=for-the-badge)
![React Native](https://img.shields.io/badge/React_Native-0.83-0f766e?style=for-the-badge)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-1d4ed8?style=for-the-badge)
![Local First](https://img.shields.io/badge/Data-Local_First-7c3aed?style=for-the-badge)

</div>

---

## Why This App Exists

`Bowels` is designed to make digestive health tracking feel approachable instead of clinical or tedious. The app focuses on fast logging, gentle guidance, and insights grounded in the data the user actually records.

It currently includes:

- Fast bowel logging with one-tap stool presets
- Timed session tracking with follow-up questions
- Daily health check-ins for water, fiber, meals, stress, sleep, exercise, caffeine, alcohol, medication, and mood
- Insight cards based on recent patterns
- Weekly Wrapped summaries
- Milestones and reminder notifications
- Import/export for backups and device migration
- Demo-mode onboarding before real data is added

---

## User Guide

### What You Can Do

| Feature | What it helps with |
|---|---|
| Quick Log | Save a bowel entry in one tap |
| Start Session | Track a live session with elapsed time |
| Questions | Add stool type, pain, blood, urgency, completion, and tags |
| Daily Health Check | Log the context around digestion |
| Insights | Surface recent trends and possible correlations |
| Weekly Wrapped | Review your week in a more visual summary |
| History | Export or import your archive |
| Settings | Pick theme accent and reminder time |

### First-Time Setup

1. Open the app and complete onboarding.
2. If you have a previous export, import it during onboarding or later from `Data & Privacy`.
3. If you are new, start with either `Quick Log` or `Start Session`.
4. Add a few days of Daily Health Check data to improve insight quality.

### Daily Workflow

### Quick log

Use `Quick Log` when you want speed. Pick the stool type preset and the app saves a short bowel entry immediately.

### Full session

Use `Start Session` when you want richer tracking:

1. Start the timer.
2. Finish the session when done.
3. Answer the follow-up questions.
4. Save and let the app fold that into your analytics.

### Daily health check

Log the surrounding context once per day:

- Water
- Fiber
- Meals
- Stress
- Sleep
- Exercise
- Caffeine
- Alcohol
- Medication
- Mood

These inputs help the app generate more meaningful pattern detection.

### Insights, Weekly Wrapped, and Milestones

- `Insights` highlights recent signals like gaps in logging, recurring pain, blood, hydration patterns, stress-linked urgency, and similar trends.
- `Weekly Wrapped` turns the week into a more celebratory summary.
- `Milestones` celebrate consistency as your logged-day streak grows.

### Data, Privacy, and Backups

This app is built to be local-first.

- Session data is stored on-device in SQLite
- Active draft state and milestone unlock state are stored in AsyncStorage
- Export creates a JSON archive you can share or keep as a backup
- Import supports the current archive format and legacy archive migration

If you care about keeping your history safe, export occasionally from `Data & Privacy`.

---

## Developer Guide

### Stack

- Expo 55
- React Native 0.83
- React 19
- TypeScript
- Expo SQLite
- AsyncStorage
- Expo Notifications

### Local Setup

### Prerequisites

- Node.js 20+ recommended
- npm
- Expo-compatible Android/iOS simulator or a physical device
- EAS CLI if you plan to produce APKs

### Install

```bash
npm install
```

### Run the app

```bash
npm start
```

Useful variants:

```bash
npm run android
npm run ios
npm run web
```

### Quality Checks

```bash
npm run lint
npm run typecheck
npm run doctor
npm run verify
```

### Build Commands

```bash
npm run build:apk
npm run build:apk:local
```

### Important Development Notes

### Notifications in Expo Go

The notification helpers intentionally no-op in Expo Go (`Constants.executionEnvironment === "storeClient"`). That means reminder and live session notification behavior should be tested in a development build or production-style build, not just Expo Go.

### Local-first behavior

The app does not depend on a backend for its core experience. Most development work happens inside:

- local persistence
- derived analytics
- UI states and flows

### Demo mode

New users begin without real data. Until `hasRealData` is set, the app renders seeded demo sessions and health entries so the product feels alive during onboarding.

### Project Structure

```text
.
|-- App.tsx                # App shell and screen routing
|-- components/            # Shared presentational building blocks
|-- screens/               # Main app screens
|-- store/                 # AppProvider and state orchestration
|-- db/                    # SQLite client, schema, repository helpers
|-- src/                   # Analytics, notifications, theme, config, types
|-- assets/                # Icons and static assets
|-- scripts/               # Utility scripts
|-- .github/               # Release templates and workflows
```

### How Data Flows

1. UI events trigger actions from `AppProvider`.
2. Repository helpers in `db/repository.ts` persist and read data from SQLite.
3. `AppProvider` derives analytics, insights, and Weekly Wrapped summaries from stored records.
4. Screens render those derived values and allow the user to keep logging.

### Core persisted tables

- `sessions`
- `session_answers`
- `daily_health`
- `settings`

### Key Source Files

- `App.tsx`: top-level app shell, screen selection, import/export, and notification wiring
- `store/AppProvider.tsx`: central app state, settings persistence, demo-mode switching, and derived summaries
- `db/repository.ts`: reads, writes, archive export/import, and legacy migration
- `db/schema.ts`: SQLite schema
- `src/notifications.ts`: reminder scheduling, live session notification updates, and milestone notifications
- `screens/HomeScreen.tsx`: quick log, status messaging, and daily check-in entry point

### Release Docs In This Repo

If you are preparing a release, these files already exist and are worth reading:

- `QUICK_RELEASE.md`
- `CUSTOMIZE_RELEASE.md`
- `FINAL_RELEASE_COMMANDS.md`
- `RELEASE_GUIDE_COMPLETE.md`
- `READY_TO_RELEASE.md`

### Suggested Contributor Workflow

1. Install dependencies with `npm install`.
2. Run `npm start` and open the app on a device or emulator.
3. Make focused changes.
4. Run `npm run lint` and `npm run verify`.
5. Test the affected flow manually.
6. If notifications are involved, verify in a dev build rather than Expo Go.

---

## Product Principles

- Fast enough to use in the moment
- Calm instead of alarming
- Insightful without pretending certainty
- Private by default
- Useful even before a user has a long history

---

## Version

Current app version in this repo: `2.0.3`
