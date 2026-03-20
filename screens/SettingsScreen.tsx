import { ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, OptionChip, SectionTitle } from "../components/UI";
import type { AccentKey, AppSettings, ThemeMode } from "../src/types";

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
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  title: { fontFamily: "Manrope_700Bold", fontSize: 20 },
  row: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
});
