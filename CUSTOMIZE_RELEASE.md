# How to Customize Your Release Page

## 📝 Before Pushing the Tag

### Step 1: Update Release Template

Edit `.github/RELEASE_TEMPLATE.md` and replace:

```markdown
**Full Changelog**: https://github.com/YOUR_USERNAME/YOUR_REPO/compare/v2.0.2...v2.0.3
```

With your actual GitHub username and repo name:

```markdown
**Full Changelog**: https://github.com/johndoe/bowels-app/compare/v2.0.2...v2.0.3
```

### Step 2: Update Support Links

Replace these placeholders in the template:
- `YOUR_USERNAME` → Your GitHub username
- `YOUR_REPO` → Your repository name

### Step 3: Customize Content (Optional)

You can edit `.github/RELEASE_TEMPLATE.md` to:
- Add more features
- Remove features you didn't implement
- Change descriptions
- Add screenshots
- Add GIFs or videos

---

## 🎨 What Gets Generated Automatically

When you push a tag, GitHub will:

1. ✅ Use your template from `.github/RELEASE_TEMPLATE.md`
2. ✅ Add the APK file to Assets
3. ✅ Show commit history in "What's Changed"
4. ✅ List contributors automatically
5. ✅ Create download links

---

## 📸 Adding Screenshots (Optional)

To add screenshots to your release:

### Option 1: Upload to Release
1. Create the release (tag push)
2. Go to the release page
3. Click "Edit"
4. Drag and drop images into the description
5. Save

### Option 2: Use GitHub Issues
1. Create a new issue
2. Upload images there
3. Copy the image URLs
4. Add to `.github/RELEASE_TEMPLATE.md`:
```markdown
![Screenshot](https://user-images.githubusercontent.com/...)
```

---

## 🎬 Adding Demo Video (Optional)

### Option 1: YouTube
```markdown
## 📹 Demo Video

[![Watch Demo](https://img.youtube.com/vi/VIDEO_ID/0.jpg)](https://www.youtube.com/watch?v=VIDEO_ID)
```

### Option 2: Direct Upload
1. Upload video to GitHub release
2. Link it in description

---

## 📋 Release Checklist Template

Add this to your release description:

```markdown
## ✅ Release Checklist

- [x] TypeScript compilation passes
- [x] Linting passes
- [x] APK builds successfully
- [x] APK installs on Android device
- [x] All new features tested
- [x] No critical bugs
- [x] Documentation updated
- [x] Version numbers updated
```

---

## 🏷️ Using Labels

GitHub can auto-categorize commits if you use conventional commits:

```bash
# Features
git commit -m "feat: add notification timer"

# Bug fixes
git commit -m "fix: resolve text cutoff issue"

# Documentation
git commit -m "docs: update README"

# Performance
git commit -m "perf: optimize prediction algorithm"
```

These will be grouped in "What's Changed" section.

---

## 🎯 Example: Perfect Release Page

Here's what your release page will look like:

```
v2.0.3 [Latest]

🎯 What's New in v2.0.3

✨ Major Features
- Improved prediction algorithm
- Synchronized notification timer
- Check for updates feature
[... rest of template ...]

📥 Installation
[Instructions]

Assets
📦 bowels-v2.0.3.apk (45.2 MB)

What's Changed
- Release v2.0.3: Improved prediction... by @yourusername
- Fix: text cutoff in Weekly Wrapped by @yourusername
[Auto-generated from commits]

Contributors
@yourusername
```

---

## 🔄 Updating After Release

If you need to update the release description:

1. Go to: `https://github.com/YOUR_USERNAME/YOUR_REPO/releases`
2. Find your release (v2.0.3)
3. Click "Edit"
4. Update the description
5. Click "Update release"

---

## 💡 Pro Tips

### 1. Use Emojis
Makes the release notes more engaging:
- ✨ Features
- 🐛 Bug fixes
- 📝 Documentation
- ⚡ Performance
- 🎨 UI/UX

### 2. Add Comparison
```markdown
### Before vs After
- ❌ Before: Text cutoff in cards
- ✅ After: Perfect text sizing
```

### 3. Add Metrics
```markdown
### Performance Improvements
- 🚀 Prediction accuracy: +25%
- ⚡ App startup: -30% faster
- 💾 APK size: 45.2 MB
```

### 4. Add Breaking Changes
```markdown
## ⚠️ Breaking Changes
- Removed floating timer (use notification instead)
- Minimum Android version: 5.0
```

### 5. Add Migration Guide
```markdown
## 🔄 Migration from v2.0.2
1. Install new version
2. Data migrates automatically
3. Check Settings for new features
```

---

## 🎉 Ready to Release?

Once you've customized `.github/RELEASE_TEMPLATE.md`, run:

```bash
git add .github/RELEASE_TEMPLATE.md
git commit -m "docs: customize release template"
git push origin main
git tag v2.0.3
git push origin v2.0.3
```

Your beautiful release page will be created automatically! 🚀
