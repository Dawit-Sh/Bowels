import { ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, GradientButton, SectionTitle } from "../components/UI";
import type { SessionRecord } from "../src/types";

export function HistoryScreen({
  palette,
  sessions,
  onExport,
  onImport,
}: {
  palette: any;
  sessions: SessionRecord[];
  onExport: () => void;
  onImport: () => void;
}) {
  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title="Data & Privacy" subtitle="Manage archives, keep exports local, and review your recent history." />
      <View style={styles.row}>
        <Card palette={palette} style={styles.actionCard}>
          <Text style={[styles.title, { color: palette.onSurface }]}>Export Data</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Save sessions, answers, and health check-ins as a local archive.</Text>
          <GradientButton palette={palette} label="Generate Archive" icon="download" onPress={onExport} />
        </Card>
        <Card palette={palette} style={styles.actionCard}>
          <Text style={[styles.title, { color: palette.onSurface }]}>Import Data</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Merge a previous Bowels backup into this device.</Text>
          <GradientButton palette={palette} label="Select Backup File" icon="upload-file" onPress={onImport} />
        </Card>
      </View>

      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.primary }]}>Recent Sessions</Text>
        {sessions.slice(0, 12).map((item) => (
          <View key={item.id} style={[styles.historyRow, { borderBottomColor: `${palette.outline}22` }]}>
            <View>
              <Text style={[styles.rowTitle, { color: palette.onSurface }]}>{item.kind === "bowel" ? "Bowel session" : "Urine session"}</Text>
              <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{new Date(item.startTime).toLocaleString()}</Text>
            </View>
            <Text style={[styles.rowValue, { color: palette.primary }]}>{Math.round(item.durationSeconds / 60)}m</Text>
          </View>
        ))}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  row: { flexDirection: "row", gap: 12 },
  actionCard: { flex: 1, minHeight: 240, justifyContent: "space-between" },
  title: { fontFamily: "Manrope_700Bold", fontSize: 20 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20 },
  historyRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 10, borderBottomWidth: StyleSheet.hairlineWidth },
  rowTitle: { fontFamily: "Manrope_700Bold", fontSize: 15 },
  rowValue: { fontFamily: "Manrope_800ExtraBold", fontSize: 18 },
});
