# Quick Release Commands - v2.0.3

## 🚀 Copy & Paste These Commands

```bash
# Step 1: Check status
git status

# Step 2: Add all changes
git add .

# Step 3: Commit
git commit -m "Release v2.0.3: Improved prediction, notification timer, updates checker, and Weekly Wrapped"

# Step 4: Push to main
git push origin main

# Step 5: Create tag
git tag v2.0.3

# Step 6: Push tag (triggers automatic build)
git push origin v2.0.3
```

## ⏱️ What Happens Next

1. **Immediately**: Tag pushed to GitHub
2. **~30 seconds**: GitHub Actions workflow starts
3. **~10-15 minutes**: EAS builds Android APK
4. **Automatic**: GitHub Release created with APK attached

## 🔍 Monitor Progress

Visit: `https://github.com/YOUR_USERNAME/YOUR_REPO/actions`

## ✅ Verify Release

Visit: `https://github.com/YOUR_USERNAME/YOUR_REPO/releases/tag/v2.0.3`

## 📝 Before You Start

Make sure you've updated in `src/config.ts`:
```typescript
export const GITHUB_REPO = "your-username/your-repo-name";
```

## 🎉 That's It!

The build will complete automatically and create the release with the APK attached.
