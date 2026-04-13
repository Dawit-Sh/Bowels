# Update Configuration

## Configure GitHub Repository for Updates

Before releasing your app, update the GitHub repository information in `src/config.ts`:

```typescript
export const APP_VERSION = "v2.0.2";
export const GITHUB_REPO = "your-username/your-repo-name";
```

### Example:
If your GitHub repo is at `https://github.com/johndoe/bowels-app`, set:
```typescript
export const GITHUB_REPO = "johndoe/bowels-app";
```

## How the Update System Works

1. User taps "Check for Updates" in Settings
2. App fetches latest release from GitHub API
3. Compares latest version with current version
4. If update available, shows download dialog
5. User taps "Download" to open browser and download APK
6. User installs the downloaded APK

## Version Management

When releasing a new version:

1. Update version in `package.json`
2. Update version in `app.json`
3. Update `APP_VERSION` in `src/config.ts`
4. Commit and push changes
5. Create git tag: `git tag v2.0.3`
6. Push tag: `git push origin v2.0.3`
7. GitHub Actions will automatically build and create release

## Notification Timer

The notification now shows a synchronized timer that updates every 10 seconds:
- Shows elapsed time in MM:SS format
- Has a "Finish Session" button
- Tapping "Finish Session" opens the app to the questions screen
- Timer automatically stops when session ends
