import { Alert, Linking, Platform } from "react-native";
import * as FileSystem from "expo-file-system/legacy";
import AsyncStorage from "@react-native-async-storage/async-storage";

const GITHUB_REPO = "YOUR_USERNAME/YOUR_REPO"; // Update this with your GitHub repo
const UPDATE_CHECK_KEY = "last-update-check";
const CHECK_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
const ENABLE_AUTO_UPDATE = false; // Set to true when you configure your GitHub repo

type GitHubRelease = {
  tag_name: string;
  name: string;
  body: string;
  assets: Array<{
    name: string;
    browser_download_url: string;
    size: number;
  }>;
};

function compareVersions(current: string, latest: string): boolean {
  const cleanCurrent = current.replace(/^v/, "");
  const cleanLatest = latest.replace(/^v/, "");
  
  const currentParts = cleanCurrent.split(".").map(Number);
  const latestParts = cleanLatest.split(".").map(Number);
  
  for (let i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
    const curr = currentParts[i] || 0;
    const lat = latestParts[i] || 0;
    
    if (lat > curr) return true;
    if (lat < curr) return false;
  }
  
  return false;
}

async function getLatestRelease(): Promise<GitHubRelease | null> {
  try {
    const response = await fetch(
      `https://api.github.com/repos/${GITHUB_REPO}/releases/latest`,
      {
        headers: {
          Accept: "application/vnd.github.v3+json",
        },
      }
    );
    
    if (!response.ok) {
      console.error("Failed to fetch latest release:", response.status);
      return null;
    }
    
    return await response.json();
  } catch (error) {
    console.error("Error fetching latest release:", error);
    return null;
  }
}

async function downloadAndInstallAPK(url: string, version: string) {
  try {
    const filename = `bowels-${version}.apk`;
    const downloadPath = `${FileSystem.documentDirectory}${filename}`;
    
    Alert.alert(
      "Downloading Update",
      `Downloading version ${version}...`,
      [{ text: "OK" }]
    );
    
    const downloadResumable = FileSystem.createDownloadResumable(
      url,
      downloadPath,
      {},
      (downloadProgress) => {
        const progress = downloadProgress.totalBytesWritten / downloadProgress.totalBytesExpectedToWrite;
        console.log(`Download progress: ${(progress * 100).toFixed(0)}%`);
      }
    );
    
    const result = await downloadResumable.downloadAsync();
    
    if (result && result.uri) {
      Alert.alert(
        "Download Complete",
        "The update has been downloaded. Opening installer...",
        [
          {
            text: "Install Now",
            onPress: () => {
              // Open the APK file for installation
              Linking.openURL(`file://${result.uri}`).catch((err) => {
                console.error("Failed to open APK:", err);
                Alert.alert(
                  "Installation Failed",
                  "Please install the APK manually from your downloads folder."
                );
              });
            },
          },
          {
            text: "Later",
            style: "cancel",
          },
        ]
      );
    }
  } catch (error) {
    console.error("Error downloading update:", error);
    Alert.alert(
      "Download Failed",
      "Failed to download the update. Please try again later."
    );
  }
}

export async function checkForUpdates(currentVersion: string, silent: boolean = true) {
  // Only check on Android and if enabled
  if (Platform.OS !== "android" || !ENABLE_AUTO_UPDATE) {
    return;
  }
  
  // Check if repo is configured
  if (GITHUB_REPO === "YOUR_USERNAME/YOUR_REPO") {
    console.log("Auto-update not configured. Set GITHUB_REPO in src/autoUpdate.ts");
    return;
  }
  
  try {
    // Check if we should check for updates (not more than once per day)
    const lastCheck = await AsyncStorage.getItem(UPDATE_CHECK_KEY);
    const now = Date.now();
    
    if (lastCheck && now - parseInt(lastCheck) < CHECK_INTERVAL && silent) {
      return;
    }
    
    await AsyncStorage.setItem(UPDATE_CHECK_KEY, now.toString());
    
    const release = await getLatestRelease();
    
    if (!release) {
      if (!silent) {
        Alert.alert("Update Check", "Unable to check for updates. Please check your internet connection.");
      }
      return;
    }
    
    const hasUpdate = compareVersions(currentVersion, release.tag_name);
    
    if (!hasUpdate) {
      if (!silent) {
        Alert.alert("No Updates", "You're running the latest version!");
      }
      return;
    }
    
    // Find APK asset
    const apkAsset = release.assets.find((asset) =>
      asset.name.toLowerCase().endsWith(".apk")
    );
    
    if (!apkAsset) {
      console.error("No APK found in release");
      return;
    }
    
    Alert.alert(
      "Update Available",
      `Version ${release.tag_name} is available!\n\n${release.body || "New features and improvements."}`,
      [
        {
          text: "Download",
          onPress: () => downloadAndInstallAPK(apkAsset.browser_download_url, release.tag_name),
        },
        {
          text: "Later",
          style: "cancel",
        },
      ]
    );
  } catch (error) {
    console.error("Error checking for updates:", error);
    if (!silent) {
      Alert.alert("Update Check Failed", "Unable to check for updates.");
    }
  }
}

export async function manualUpdateCheck(currentVersion: string) {
  await checkForUpdates(currentVersion, false);
}
