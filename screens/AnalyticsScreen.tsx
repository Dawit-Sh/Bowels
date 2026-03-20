import { ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, SectionTitle } from "../components/UI";
import type { AnalyticsSummary, InsightItem } from "../src/types";

export function AnalyticsScreen({ palette, analytics, insights }: { palette: any; analytics: AnalyticsSummary; insights: InsightItem[] }) {
  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title="14-Day Insights" subtitle="Directly adapted from the supplied analytics and weekly report layouts." />
      <View style={styles.stats}>
        <Card palette={palette} style={styles.statCard}>
          <Text style={[styles.big, { color: palette.onSurface }]}>{analytics.totalVisits}</Text>
          <Text style={[styles.label, { color: palette.onSurfaceVariant }]}>Total Visits</Text>
        </Card>
        <Card palette={palette} style={[styles.statCard, { backgroundColor: palette.primaryContainer }]}>
          <Text style={[styles.big, { color: palette.onPrimary }]}>{Math.round(analytics.averageDurationSeconds / 60)}m</Text>
          <Text style={[styles.label, { color: `${palette.onPrimary}CC` }]}>Avg Duration</Text>
        </Card>
      </View>

      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.onSurface }]}>Bristol Distribution</Text>
        <View style={styles.chart}>
          {analytics.stoolDistribution.map((item) => (
            <View key={item.stoolType} style={styles.chartCol}>
              <Text style={[styles.label, { color: palette.primary }]}>{item.count}</Text>
              <View style={[styles.chartBar, { height: 22 + item.count * 12, backgroundColor: item.stoolType === 3 || item.stoolType === 4 ? palette.primary : palette.surfaceContainerHighest }]} />
              <Text style={[styles.label, { color: palette.onSurfaceVariant }]}>{`T${item.stoolType}`}</Text>
            </View>
          ))}
        </View>
      </Card>

      <Card palette={palette}>
        <Text style={[styles.title, { color: palette.onSurface }]}>Fiber Effect</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{analytics.fiberCorrelationLabel}</Text>
        <View style={styles.correlation}>
          {[32, 40, 54, 76, 92, 70, 58].map((value, index) => (
            <View key={`${index}-${value}`} style={[styles.correlationBar, { height: value, backgroundColor: index >= 2 ? palette.primary : palette.surfaceContainerHighest }]} />
          ))}
        </View>
      </Card>

      {insights.slice(0, 2).map((item) => (
        <Card key={item.id} palette={palette}>
          <Text style={[styles.title, { color: palette.secondary }]}>{item.title}</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{item.body}</Text>
        </Card>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  stats: { flexDirection: "row", gap: 12 },
  statCard: { flex: 1, minHeight: 150, justifyContent: "space-between" },
  big: { fontFamily: "Manrope_800ExtraBold", fontSize: 36 },
  label: { fontFamily: "Manrope_700Bold", fontSize: 11, textTransform: "uppercase", letterSpacing: 1 },
  title: { fontFamily: "Manrope_700Bold", fontSize: 22 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20 },
  chart: { flexDirection: "row", alignItems: "flex-end", gap: 6, minHeight: 180 },
  chartCol: { flex: 1, alignItems: "center", justifyContent: "flex-end", gap: 8 },
  chartBar: { width: "100%", borderRadius: 999 },
  correlation: { flexDirection: "row", alignItems: "flex-end", gap: 6, height: 120 },
  correlationBar: { flex: 1, borderTopLeftRadius: 12, borderTopRightRadius: 12 },
});
