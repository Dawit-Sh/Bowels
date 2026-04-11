# Release v2.0.2

## Changes in this release:
- Performance optimizations (Hermes engine, React.memo, useMemo, useCallback)
- 15 days of comprehensive demo data
- Reset to demo data functionality in Settings
- Improved animations with InteractionManager
- Enhanced daily health modal with smooth spring animations
- Better milestone tracking and notifications
- Floating draggable timer
- Weekly Wrapped with Instagram sharing
- Expanded health insights and correlations

## Git Commands to Push and Release:

```bash
# 1. Check current status
git status

# 2. Add all changes
git add .

# 3. Commit with version message
git commit -m "Release v2.0.2: Performance optimizations and 15-day demo data"

# 4. Push to main branch
git push origin main

# 5. Create and push version tag (this triggers the release build)
git tag v2.0.2
git push origin v2.0.2
```

## What happens after pushing the tag:
1. GitHub Actions workflow automatically starts
2. Runs TypeScript checks and linting
3. Builds Android APK using EAS Build
4. Creates a GitHub Release with the APK attached
5. Release notes are auto-generated from commits

## To check build progress:
- Go to: https://github.com/YOUR_USERNAME/YOUR_REPO/actions
- Click on the "Release" workflow run
- Wait for the build to complete (usually 10-15 minutes)

## After release is created:
- Users with auto-update enabled will be notified
- APK will be available at: https://github.com/YOUR_USERNAME/YOUR_REPO/releases/tag/v2.0.2
