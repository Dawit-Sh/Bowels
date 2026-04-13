import { Alert, Linking, ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, GradientButton, OptionChip, SectionTitle } from "../components/UI";
import type { AccentKey, AppSettings, ThemeMode } from "../src/types";
import { APP_VERSION, GITHUB_REPO } from "../src/config";

export function SettingsScreen({
  palette,
  settings,
  setThemeMode,
  setAccent,
  setReminderHour,
}: {
  palette: any;
  settings: AppSettings;
  setThemeMode: (mode: ThemeMode) => Promise<void>;
  setAccent: (accent: AccentKey) => Promise<void>;
  setReminderHour: (hour: number) => Promise<void>;
}) {
  const handleCheckUpdates = async () => {
    try {
      const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`);
      
      if (!response.ok) {
        Alert.alert("Check Failed", "Unable to check for updates. Please try again later.");
        return;
      }
      
      const data = await response.json();
      const latestVersion = data.tag_name; // e.g., "v2.0.2"
      
      if (latestVersion === APP_VERSION) {
        Alert.alert("Up to Date", "You're running the latest version!");
        return;
      }
      
      // Find the APK asset
      const apkAsset = data.assets.find((asset: any) => asset.name.endsWith(".apk"));
      
      if (!apkAsset) {
        Alert.alert("No APK Found", "No APK file found in the latest release.");
        return;
      }
      
      Alert.alert(
        "Update Available",
        `Version ${latestVersion} is available. Current version: ${APP_VERSION}\n\nWould you like to download it?`,
        [
          { text: "Cancel", style: "cancel" },
          {
            text: "Download",
            onPress: () => {
              Linking.openURL(apkAsset.browser_download_url).catch(() => {
                Alert.alert("Error", "Unable to open download link.");
              });
            },
          },
        ]
      );
    } catch {
      Alert.alert("Error", "Failed to check for updates. Please check your internet connection.");
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title="Settings" subtitle="Theme, accent colors, and reminders are persisted locally." />
      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.onSurface }]}>Theme</Text>
        <View style={styles.row}>{(["light", "dark", "system"] as ThemeMode[]).map((value) => <OptionChip key={value} palette={palette} label={value} active={settings.themeMode === value} onPress={() => void setThemeMode(value)} />)}</View>
      </Card>
      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.onSurface }]}>Accent</Text>
        <View style={styles.row}>{(["olive", "coral", "rose", "slate"] as AccentKey[]).map((value) => <OptionChip key={value} palette={palette} label={value} active={settings.accent === value} onPress={() => void setAccent(value)} />)}</View>
      </Card>
      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.onSurface }]}>Daily reminder hour</Text>
        <View style={styles.row}>{[8, 12, 18, 20, 22].map((value) => <OptionChip key={value} palette={palette} label={`${value}:00`} active={settings.reminderHour === value} onPress={() => void setReminderHour(value)} />)}</View>
      </Card>
      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.onSurface }]}>App Updates</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>
          Check for the latest version on GitHub and download updates.
        </Text>
        <GradientButton
          palette={palette}
          label="Check for Updates"
          icon="system-update"
          onPress={handleCheckUpdates}
        />
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  title: { fontFamily: "Manrope_700Bold", fontSize: 20 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20, marginBottom: 12 },
  row: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
});
