import { ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, SectionTitle } from "../components/UI";
import { stoolTypeMeta } from "../src/sessionMeta";
import type { AnalyticsSummary } from "../src/types";

export function WeeklyWrappedScreen({ palette, analytics }: { palette: any; analytics: AnalyticsSummary }) {
  const best = analytics.visitsPerDay.reduce((bestDay, item) => (item.count > bestDay.count ? item : bestDay), analytics.visitsPerDay[0] ?? { day: "", count: 0 });
  const worst = analytics.visitsPerDay.reduce((worstDay, item) => (item.count < worstDay.count ? item : worstDay), analytics.visitsPerDay[0] ?? { day: "", count: 0 });
  const common = analytics.stoolDistribution.reduce((bestType, item) => (item.count > bestType.count ? item : bestType), analytics.stoolDistribution[0] ?? { stoolType: 4, count: 0 });
  const commonMeta = stoolTypeMeta[common.stoolType];

  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title="Your Weekly Rhythm" subtitle="A mobile-friendly version of the Weekly Wrapped design." />
      <Card palette={palette} style={styles.hero}>
        <Text style={[styles.streak, { color: palette.primary }]}>{analytics.totalVisits}</Text>
        <Text style={[styles.label, { color: palette.onSurfaceVariant }]}>Total visits this week</Text>
      </Card>
      <View style={styles.grid}>
        <Card palette={palette} style={styles.square}><Text style={[styles.cardTitle, { color: palette.onSurface }]}>Avg Duration</Text><Text style={[styles.big, { color: palette.primary }]}>{Math.round(analytics.averageDurationSeconds / 60)}m</Text></Card>
        <Card palette={palette} style={styles.square}>
          <Text style={[styles.cardTitle, { color: palette.onSurface }]}>Common</Text>
          <View style={styles.commonWrap}>
            <Text style={[styles.commonLabel, { color: palette.secondary }]}>{commonMeta.short}</Text>
          </View>
        </Card>
      </View>
      <Card palette={palette}>
        <Text style={[styles.cardTitle, { color: palette.onSurface }]}>Best / Worst Day</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Best: {best.day} with {best.count} visits</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Worst: {worst.day} with {worst.count} visits</Text>
      </Card>
      <Card palette={palette}>
        <Text style={[styles.cardTitle, { color: palette.onSurface }]}>Simple Insight</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Stable routines tend to map to better weekly scores. Keep hydration and fiber consistent to reduce variability.</Text>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  hero: { alignItems: "center", justifyContent: "center", minHeight: 220 },
  streak: { fontFamily: "Manrope_800ExtraBold", fontSize: 72 },
  label: { fontFamily: "Manrope_700Bold", fontSize: 12, textTransform: "uppercase", letterSpacing: 1.6 },
  grid: { flexDirection: "row", gap: 12 },
  square: { flex: 1, aspectRatio: 1, justifyContent: "space-between" },
  cardTitle: { fontFamily: "Manrope_700Bold", fontSize: 22 },
  big: { fontFamily: "Manrope_800ExtraBold", fontSize: 36 },
  commonWrap: { flex: 1, justifyContent: "flex-end" },
  commonLabel: { fontFamily: "Manrope_800ExtraBold", fontSize: 30 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20 },
});
