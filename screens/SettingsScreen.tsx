import { Alert, ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, GradientButton, OptionChip, SectionTitle } from "../components/UI";
import type { AccentKey, AppSettings, ThemeMode } from "../src/types";

export function SettingsScreen({
  palette,
  settings,
  setThemeMode,
  setAccent,
  setReminderHour,
  resetToDemo,
}: {
  palette: any;
  settings: AppSettings;
  setThemeMode: (mode: ThemeMode) => Promise<void>;
  setAccent: (accent: AccentKey) => Promise<void>;
  setReminderHour: (hour: number) => Promise<void>;
  resetToDemo: () => Promise<void>;
}) {
  const handleReset = () => {
    Alert.alert(
      "Reset to Demo Data?",
      "This will clear all your real data and show 15 days of demo data. This action cannot be undone.",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Reset",
          style: "destructive",
          onPress: () => void resetToDemo(),
        },
      ]
    );
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
        <Text style={[styles.title, { color: palette.onSurface }]}>Data</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>
          {settings.hasRealData ? "You're viewing your real data." : "You're viewing demo data."}
        </Text>
        <GradientButton
          palette={palette}
          label="Reset to Demo Data"
          icon="refresh"
          onPress={handleReset}
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
