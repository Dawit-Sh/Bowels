# Releasing Bowels

## Versioning

- Update `version` in `package.json`
- Update `expo.version` in `app.json`
- Commit those changes

## Git tags

Create an annotated tag and push it:

```bash
git tag -a vX.Y.Z -m "vX.Y.Z"
git push origin vX.Y.Z
```

## Universal Android APK (internal/testing)

This repo includes `eas.json` with a `preview` profile that builds an APK:

```bash
npx eas build -p android --profile preview
```

## Android App Bundle (Play Store)

For production releases use the `production` profile:

```bash
npx eas build -p android --profile production
```

Note: before store release, set a stable Android package name in `app.json` (`expo.android.package`) and keep it forever.

