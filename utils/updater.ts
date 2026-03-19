import * as FileSystem from 'expo-file-system/legacy';
import * as IntentLauncher from 'expo-intent-launcher';
import * as WebBrowser from 'expo-web-browser';
import { Platform, Alert } from 'react-native';

const REPO_URL = 'https://api.github.com/repos/Dawit-Sh/Bowels/releases/latest';

export async function checkForUpdates() {
  try {
    const res = await fetch(REPO_URL);
    if (!res.ok) throw new Error('Failed to fetch latest release from GitHub.');
    const release = await res.json();
    
    // Check if Android, we can download and intent launch APK
    if (Platform.OS === 'android') {
      const apkAsset = release.assets.find((a: any) => a.name.endsWith('.apk'));
      if (apkAsset) {
        Alert.alert(
          'Update Available',
          `Version ${release.tag_name} is available. Do you want to download and install it?`,
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Download', onPress: () => downloadAndInstall(apkAsset.browser_download_url) }
          ]
        );
        return;
      }
    }
    
    // iOS or fallback: open release notes in browser
    Alert.alert(
      'Update Available',
      `Version ${release.tag_name} is available.`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'View Release', onPress: () => WebBrowser.openBrowserAsync(release.html_url) }
      ]
    );
  } catch (error: any) {
    Alert.alert('You are on the latest version', 'No valid updates found on GitHub.');
  }
}

async function downloadAndInstall(url: string) {
  try {
    const dest = `${FileSystem.documentDirectory}update.apk`;
    const { uri } = await FileSystem.downloadAsync(url, dest);
    
    // For local APK install
    const contentUri = await FileSystem.getContentUriAsync(uri);
    await IntentLauncher.startActivityAsync('android.intent.action.VIEW', {
      data: contentUri,
      flags: 1,
      type: 'application/vnd.android.package-archive',
    });
  } catch (err: any) {
    Alert.alert('Installation Error', err.message);
  }
}
