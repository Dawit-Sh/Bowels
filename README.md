# Bowels

<div align="center">

### A calm, local-first digestive health tracker for Android and Wear OS

Track sessions, log daily health signals, see heart rate and blood pressure from around each session, and build a steadier rhythm over time.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7f52ff?style=for-the-badge)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-0f766e?style=for-the-badge)
![Wear OS](https://img.shields.io/badge/Wear_OS-companion-1d4ed8?style=for-the-badge)
![Local First](https://img.shields.io/badge/Data-Local_First-7c3aed?style=for-the-badge)

</div>

---

## What's in 3.0

Bowels 3.0 is a native Kotlin rewrite of the Expo / React Native app (which stays, unchanged, on `main` up to 2.x).

- **Native Android app** in Jetpack Compose, same look, same features.
- **Wear OS companion.** Start and finish a session from your wrist when your phone is in another room. The watch measures your heart rate during the session and syncs to the phone on its own, even if the phone was out of range or switched off at the time.
- **Heart rate and blood pressure.** For every session, Bowels reads what your watch or blood pressure cuff recorded around that time through Health Connect (Samsung Health, Fitbit, Pixel Watch, Omron and others write there). You can also type a reading in. Read-only: Bowels never writes or shares health data.
- **Fixed: the weekly report notification** now opens Weekly Wrapped. Before, tapping it did nothing, because only session notifications were routed.
- **Fixed: shared images.** Weekly Wrapped and milestone images are now drawn at a true 1080x1920 (Instagram story size). Before, the on-screen card was screenshotted and forced into 1080x1920, which stretched it and made it blurry. The Weekly Wrapped screen previews the exact image you'll share.
- **New: share a milestone** straight from the Milestones screen.

### Updating from 2.x keeps your data

The new app opens the same SQLite file the Expo app used (`files/SQLite/bowels.db`) and carries over the running session and announced milestones from AsyncStorage. For an in-place update the APK must be signed with the same key as the Expo builds (see Releases below). Backups exported from 2.x import into 3.0 too.

---

## No setup for users

Everything is asked in context, once, through the system's own permission sheets:

| When | What happens |
|---|---|
| Tapping **Begin** in onboarding | Notifications permission, then Health Connect's read permissions for heart rate and blood pressure (if Health Connect is on the phone) |
| Health Connect missing (Android 13 and older) | A Home card with one button that opens its install page |
| A Wear OS watch is paired without Bowels | A Home card with **Install on watch**, which opens Bowels' page in the watch's Play Store |
| First session started on the watch | The heart-rate permission, in the same tap |
| Ending a session | Heart rate and blood pressure fill in on the questions screen by themselves |
| Opening the app | Recent sessions without vitals are checked against Health Connect again (watches and cuffs often sync late) |

Nothing needs to be turned on in system settings.

---

## Project layout

```text
.
|-- app/        Phone app (Compose)
|   |-- data/       SQLite, repository, backups, local prefs
|   |-- domain/     Analytics, insights, content, demo data
|   |-- health/     Health Connect reads
|   |-- notify/     Reminders, weekly report, session and milestone notifications
|   |-- share/      1080x1920 share images
|   |-- wear/       Watch sync service and install prompt
|   |-- ui/         ViewModel, theme, components, screens
|-- wear/       Wear OS app (Compose for Wear, Health Services, Data Layer)
|-- shared/     The phone/watch sync contract
|-- docs/       GitHub Pages site
```

### How the watch syncs

1. The watch saves each finished session as a Wearable Data Layer item (`/bowels/session/<id>`).
2. Google Play services keeps it on the watch and delivers it whenever the phone is reachable.
3. The phone's `WatchSyncService` saves it (de-duplicated by id), looks up blood pressure in Health Connect, then deletes the item.
4. The deletion syncs back, and the watch shows "All synced".

Vitals are stored as ordinary session answers (`hr_avg`, `hr_min`, `hr_max`, `bp_systolic`, `bp_diastolic`, `bp_time`, and their sources), so backups carry them with no format change.

---

## Developer guide

Requirements: JDK 17 and the Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

The tests cover analytics and insights, backup import (current and legacy formats), watch session sync and the weekly notification's routing. They also render the share images to `app/build/share-previews/` so you can look at them.

To try the watch with the phone, install both debug APKs (they share the debug key) on a phone and a paired Wear OS watch, or a paired emulator pair.

### Releases

Put the signing key in `keystore.properties` at the repo root (never committed):

```properties
storeFile=bowels-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

For existing 2.x installs to update in place, use the key EAS signed the Expo builds with (`eas credentials`, Android, download the keystore). Phone and watch must be signed with the same key, or the Data Layer won't connect them. The release workflow reads the same values from repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

The watch app reaches users through the Play Store only (Wear OS doesn't install downloaded APKs without developer mode). Until Bowels is on Play, the watch APK installs with `adb` over Wi-Fi.

```bash
./gradlew assembleRelease
```

---

## Product principles

- Fast enough to use in the moment
- Calm instead of alarming
- Insightful without pretending certainty
- Private by default
- Useful even before a user has a long history
- No setup: anything that can be connected is offered in one tap, in context
