# Complete Release Guide - Detailed Release Page

## 🎯 Goal: Create a Professional Release Page

Like the example you showed with:
- ✅ Detailed "What's Changed" section
- ✅ Contributor information
- ✅ Full changelog link
- ✅ Professional formatting
- ✅ APK download

---

## 📋 Step-by-Step Instructions

### Step 1: Customize Release Template

Edit `.github/RELEASE_TEMPLATE.md` and replace:

**Find this line:**
```markdown
**Full Changelog**: https://github.com/YOUR_USERNAME/YOUR_REPO/compare/v2.0.2...v2.0.3
```

**Replace with your info:**
```markdown
**Full Changelog**: https://github.com/johndoe/bowels-app/compare/v2.0.2...v2.0.3
```

**Also replace in Support section:**
```markdown
1. Check the [Issues](https://github.com/johndoe/bowels-app/issues) page
2. Create a new issue with details
```

### Step 2: Commit the Template

```bash
git add .github/RELEASE_TEMPLATE.md
git commit -m "docs: add detailed release template"
git push origin main
```

### Step 3: Create Release with Tag

```bash
# Add all your code changes
git add .
git commit -m "Release v2.0.3: Improved prediction, notification timer, updates checker, and Weekly Wrapped"
git push origin main

# Create and push tag
git tag v2.0.3
git push origin v2.0.3
```

### Step 4: Wait for Build

- Go to: `https://github.com/YOUR_USERNAME/YOUR_REPO/actions`
- Watch the "Release" workflow
- Wait 10-15 minutes for completion

### Step 5: Check Your Release Page

- Go to: `https://github.com/YOUR_USERNAME/YOUR_REPO/releases/tag/v2.0.3`
- You'll see:
  - Your detailed description from template
  - APK file in Assets
  - Auto-generated commit list
  - Contributors section

---

## 🎨 What Your Release Page Will Look Like

```
┌─────────────────────────────────────────────────┐
│ v2.0.3                              [Latest]    │
│ @yourusername released this last week           │
├─────────────────────────────────────────────────┤
│                                                 │
│ 🎯 What's New in v2.0.3                        │
│                                                 │
│ ✨ Major Features                               │
│                                                 │
│ 🎯 Improved Prediction Algorithm                │
│ - Multi-factor analysis using up to 21 sessions│
│ - Day-of-week pattern detection                │
│ - Confidence indicators: 🎯📊🔮                 │
│                                                 │
│ ⏱️ Synchronized Notification Timer              │
│ - Live timer updates every 10 seconds          │
│ - "Finish Session" button in notification      │
│                                                 │
│ [... rest of your template ...]                │
│                                                 │
├─────────────────────────────────────────────────┤
│ Assets                                          │
│ 📦 bowels-v2.0.3.apk (45.2 MB)                 │
├─────────────────────────────────────────────────┤
│ What's Changed                                  │
│ • Release v2.0.3: Improved prediction...       │
│   by @yourusername in #15                      │
│ • Fix: text cutoff in Weekly Wrapped           │
│   by @yourusername in #14                      │
│                                                 │
│ Full Changelog: v2.0.2...v2.0.3                │
├─────────────────────────────────────────────────┤
│ Contributors                                    │
│ 👤 @yourusername                                │
└─────────────────────────────────────────────────┘
```

---

## 🔧 Customization Options

### Option 1: Add Screenshots

After release is created:
1. Go to release page
2. Click "Edit"
3. Drag and drop screenshots
4. They'll appear in description

### Option 2: Add Badges

Add to top of `.github/RELEASE_TEMPLATE.md`:

```markdown
![Version](https://img.shields.io/badge/version-2.0.3-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![License](https://img.shields.io/badge/license-MIT-orange)
```

### Option 3: Add Comparison Table

```markdown
## 📊 Version Comparison

| Feature | v2.0.2 | v2.0.3 |
|---------|--------|--------|
| Prediction Sessions | 14 | 21 |
| Confidence Indicators | ❌ | ✅ |
| Notification Timer | ❌ | ✅ |
| Save to Gallery | ❌ | ✅ |
| Demo Data Days | 8 | 15 |
```

### Option 4: Add Known Issues

```markdown
## ⚠️ Known Issues

- None at this time

If you find any issues, please report them [here](https://github.com/YOUR_USERNAME/YOUR_REPO/issues).
```

---

## 📝 Using Conventional Commits

For better "What's Changed" section, use these commit formats:

```bash
# Features
git commit -m "feat: add synchronized notification timer"
git commit -m "feat(prediction): improve algorithm accuracy"

# Bug Fixes
git commit -m "fix: resolve text cutoff in Weekly Wrapped"
git commit -m "fix(notifications): sync timer with app state"

# Documentation
git commit -m "docs: update README with new features"

# Performance
git commit -m "perf: optimize prediction calculations"

# Refactor
git commit -m "refactor: simplify analytics logic"

# Style
git commit -m "style: improve card layouts"

# Tests
git commit -m "test: add prediction algorithm tests"
```

GitHub will automatically group these by type!

---

## 🎯 Complete Command Sequence

Here's everything in order:

```bash
# 1. Customize template (do this once)
# Edit .github/RELEASE_TEMPLATE.md with your GitHub username/repo

# 2. Commit template
git add .github/RELEASE_TEMPLATE.md
git commit -m "docs: customize release template"
git push origin main

# 3. Commit your code changes
git add .
git commit -m "Release v2.0.3: Improved prediction, notification timer, updates checker, and Weekly Wrapped"
git push origin main

# 4. Create and push tag (triggers build)
git tag v2.0.3
git push origin v2.0.3

# 5. Monitor build
# Visit: https://github.com/YOUR_USERNAME/YOUR_REPO/actions

# 6. View release
# Visit: https://github.com/YOUR_USERNAME/YOUR_REPO/releases/tag/v2.0.3
```

---

## ✅ Verification Checklist

After pushing the tag, verify:

- [ ] GitHub Actions workflow started
- [ ] Build completed successfully (green checkmark)
- [ ] Release page created at `/releases/tag/v2.0.3`
- [ ] APK file appears in Assets
- [ ] Release description shows your template
- [ ] "What's Changed" shows commits
- [ ] Contributors section shows your username
- [ ] Full Changelog link works

---

## 🆘 Troubleshooting

### Release Description is Empty
- Check that `.github/RELEASE_TEMPLATE.md` exists
- Verify it's committed and pushed
- Check workflow file uses `body_path`

### APK Not Attached
- Check GitHub Actions logs
- Verify EXPO_TOKEN secret is set
- Check EAS build completed

### "What's Changed" is Empty
- Need at least one commit since last tag
- Commits must be pushed before tag

### Want to Update Release
1. Go to release page
2. Click "Edit"
3. Update description
4. Click "Update release"

---

## 🎉 You're Ready!

Your release page will look professional with:
- ✅ Detailed feature descriptions
- ✅ Installation instructions
- ✅ Requirements and permissions
- ✅ Support information
- ✅ Downloadable APK
- ✅ Auto-generated changelog
- ✅ Contributor attribution

Just run the commands and watch the magic happen! 🚀
