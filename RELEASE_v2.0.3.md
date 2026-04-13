# Release v2.0.3 - Complete Guide

## 🎯 What's New in v2.0.3

### Major Features
1. **Improved Prediction Algorithm**
   - Multi-factor analysis (21 sessions)
   - Day-of-week pattern detection
   - Time-of-day preferences
   - Confidence indicators (🎯📊🔮)
   - 85% weighted recent sessions

2. **Synchronized Notification Timer**
   - Updates every 10 seconds
   - Shows live elapsed time
   - "Finish Session" button in notification
   - Auto-dismisses when session ends

3. **Check for Updates Feature**
   - Fetches from GitHub releases
   - Compares versions automatically
   - Downloads APK directly
   - Replaces "Reset to Demo Data"

4. **Dynamic Status Messages**
   - Real-time activity tracking
   - Consistency scoring
   - Personalized messages
   - Data-driven insights

5. **Weekly Wrapped Improvements**
   - Fixed text cutoff issues
   - Save to Gallery feature
   - Better card sizing
   - 1080x1920 Instagram Story format

6. **15 Days Demo Data**
   - Comprehensive sample data
   - Realistic patterns
   - All health metrics included

### Bug Fixes
- Removed floating timer (replaced with notification)
- Removed daily health popup
- Fixed package version conflicts
- Fixed Weekly Wrapped text wrapping
- Improved card layouts

---

## 📋 Pre-Release Checklist

Before pushing to GitHub, ensure:

- [x] Version updated in package.json (2.0.3)
- [x] Version updated in app.json (2.0.3)
- [x] Version updated in src/config.ts (v2.0.3)
- [ ] GitHub repo configured in src/config.ts
- [ ] All TypeScript errors resolved
- [ ] All tests passing (if any)

---

## 🚀 Git Commands

### 1. Check Status
```bash
git status
```

### 2. Add All Changes
```bash
git add .
```

### 3. Commit Changes
```bash
git commit -m "Release v2.0.3: Improved prediction, notification timer, updates checker, and Weekly Wrapped"
```

### 4. Push to Main Branch
```bash
git push origin main
```

### 5. Create Version Tag
```bash
git tag v2.0.3
```

### 6. Push Tag (Triggers Release Build)
```bash
git push origin v2.0.3
```

---

## 🏗️ Build Commands

### Option 1: Automatic Build (Recommended)
After pushing the tag, GitHub Actions will automatically:
1. Run TypeScript checks
2. Run linting
3. Build Android APK using EAS
4. Create GitHub Release
5. Attach APK to release

**Monitor progress:**
- Go to: https://github.com/YOUR_USERNAME/YOUR_REPO/actions
- Click on "Release" workflow
- Wait 10-15 minutes for completion

### Option 2: Manual Local Build
If you want to build locally:

```bash
# Build APK locally (requires Android SDK)
eas build --platform android --profile preview --local

# Or build on EAS servers
eas build --platform android --profile preview
```

### Option 3: Development Build
For testing with development client:

```bash
# Build development client
eas build --platform android --profile development

# Start dev server
npx expo start --dev-client
```

---

## 📦 After Release

### 1. Verify Release Created
- Go to: https://github.com/YOUR_USERNAME/YOUR_REPO/releases
- Check that v2.0.3 release exists
- Verify APK is attached

### 2. Test Update Feature
- Install the APK on a device
- Go to Settings → Check for Updates
- Should show "Up to Date"

### 3. Update Documentation
- Update README with new features
- Update changelog
- Update screenshots if needed

---

## 🔧 Configuration Required

### Before First Release

1. **Update GitHub Repo in src/config.ts:**
```typescript
export const GITHUB_REPO = "your-username/your-repo-name";
```

2. **Ensure EAS is configured:**
```bash
eas login
eas build:configure
```

3. **Add EXPO_TOKEN to GitHub Secrets:**
- Go to: Settings → Secrets and variables → Actions
- Add new secret: `EXPO_TOKEN`
- Get token from: `eas whoami` or Expo dashboard

---

## 📝 Release Notes Template

Use this for GitHub release description:

```markdown
## 🎯 What's New

### Features
- 🎯 Improved prediction algorithm with confidence indicators
- ⏱️ Synchronized notification timer with finish button
- 🔄 Check for updates feature in Settings
- 📊 Dynamic status messages based on real data
- 📸 Save Weekly Wrapped to gallery
- 📦 15 days of comprehensive demo data

### Improvements
- Better prediction accuracy (21 sessions, day-of-week patterns)
- Fixed Weekly Wrapped text cutoff
- Removed floating timer (notification-based now)
- Improved card layouts and sizing
- Better insights with data correlations

### Bug Fixes
- Fixed package version conflicts
- Removed daily health popup
- Fixed notification timer synchronization
- Improved text wrapping in cards

## 📥 Installation

1. Download the APK below
2. Enable "Install from Unknown Sources" on your Android device
3. Install the APK
4. Open the app and start tracking!

## 🔄 Updating from Previous Version

If you have v2.0.2 or earlier:
1. Download the new APK
2. Install over the existing app
3. Your data will be preserved
4. New features will be available immediately

## 📱 Requirements

- Android 5.0 or higher
- 50 MB free space
- Internet connection (for updates check)
```

---

## 🎉 Complete Command Sequence

Copy and paste these commands in order:

```bash
# 1. Check what's changed
git status

# 2. Add all changes
git add .

# 3. Commit with version message
git commit -m "Release v2.0.3: Improved prediction, notification timer, updates checker, and Weekly Wrapped"

# 4. Push to main
git push origin main

# 5. Create and push tag (triggers automatic build)
git tag v2.0.3
git push origin v2.0.3

# 6. Monitor build progress
# Go to: https://github.com/YOUR_USERNAME/YOUR_REPO/actions
```

---

## ⏱️ Timeline

- **Git push**: Instant
- **Tag push**: Instant
- **GitHub Actions start**: ~30 seconds
- **Build completion**: 10-15 minutes
- **Release created**: Automatic after build

---

## 🆘 Troubleshooting

### Build Fails
- Check GitHub Actions logs
- Verify EXPO_TOKEN is set
- Ensure eas.json is correct

### Tag Already Exists
```bash
# Delete local tag
git tag -d v2.0.3

# Delete remote tag
git push origin :refs/tags/v2.0.3

# Create new tag
git tag v2.0.3
git push origin v2.0.3
```

### Need to Update After Tag
```bash
# Make changes
git add .
git commit -m "Fix: description"
git push origin main

# Delete and recreate tag
git tag -d v2.0.3
git push origin :refs/tags/v2.0.3
git tag v2.0.3
git push origin v2.0.3
```

---

## ✅ Success Indicators

You'll know the release is successful when:
- ✅ GitHub Actions workflow completes (green checkmark)
- ✅ Release appears at /releases/tag/v2.0.3
- ✅ APK file is attached to release
- ✅ Release notes are auto-generated
- ✅ APK downloads and installs correctly
- ✅ App shows v2.0.3 in Settings → Check for Updates
