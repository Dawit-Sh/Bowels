import { ScrollView, StyleSheet, Text } from "react-native";

import { Card, SectionTitle } from "../components/UI";
import { healthInfo } from "../src/healthInfo";

export function HealthInfoScreen({ palette }: { palette: any }) {
  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title="Gut Health Best Practices" subtitle="Static, lightweight guidance stored locally for fast access." />
      {healthInfo.map((item) => (
        <Card key={item.id} palette={palette}>
          <Text style={[styles.title, { color: palette.onSurface }]}>{item.title}</Text>
          <Text style={[styles.summary, { color: palette.primary }]}>{item.summary}</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{item.body}</Text>
        </Card>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  title: { fontFamily: "Manrope_700Bold", fontSize: 20 },
  summary: { fontFamily: "Manrope_700Bold", fontSize: 14, lineHeight: 20 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 21 },
});
