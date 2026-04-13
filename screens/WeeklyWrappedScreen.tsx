import { useRef } from "react";
import { Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { MaterialIcons } from "@expo/vector-icons";
import * as Sharing from "expo-sharing";
import * as MediaLibrary from "expo-media-library";
import { captureRef } from "react-native-view-shot";

import { Card } from "../components/UI";
import { stoolTypeMeta } from "../src/sessionMeta";
import type { AnalyticsSummary } from "../src/types";

function getQuirkyInsight(analytics: AnalyticsSummary) {
  const quirks = [
    `You spent ${Math.round(analytics.totalDurationSeconds / 60)} minutes this week in mindful reflection. That's ${Math.round(analytics.totalDurationSeconds / 60 / 7)} minutes per day of self-care.`,
    `Your most productive day had ${analytics.visitsPerDay.reduce((max, day) => Math.max(max, day.count), 0)} visits. Consistency is key!`,
    `You're in the top ${Math.floor(Math.random() * 15) + 5}% of users who track their wellness journey.`,
    `Your rhythm is ${analytics.totalVisits >= 14 ? "incredibly steady" : analytics.totalVisits >= 7 ? "building momentum" : "just getting started"}. Keep it up!`,
    `If your sessions were songs, you'd have a ${Math.round(analytics.totalDurationSeconds / 60)}-minute wellness playlist.`,
  ];
  return quirks[Math.floor(Math.random() * quirks.length)];
}

function getPersonalityType(analytics: AnalyticsSummary) {
  const avgDuration = analytics.averageDurationSeconds / 60;
  if (avgDuration < 3) return { type: "The Efficient One", desc: "Quick and focused" };
  if (avgDuration < 5) return { type: "The Balanced Soul", desc: "Perfect harmony" };
  if (avgDuration < 8) return { type: "The Mindful Meditator", desc: "Deep reflection" };
  return { type: "The Zen Master", desc: "Ultimate patience" };
}

export function WeeklyWrappedScreen({ palette, analytics }: { palette: any; analytics: AnalyticsSummary }) {
  const viewRef = useRef<View>(null);
  const best = analytics.visitsPerDay.reduce((bestDay, item) => (item.count > bestDay.count ? item : bestDay), analytics.visitsPerDay[0] ?? { day: "", count: 0 });
  const worst = analytics.visitsPerDay.reduce((worstDay, item) => (item.count < worstDay.count ? item : worstDay), analytics.visitsPerDay[0] ?? { day: "", count: 0 });
  const common = analytics.stoolDistribution.reduce((bestType, item) => (item.count > bestType.count ? item : bestType), analytics.stoolDistribution[0] ?? { stoolType: 4, count: 0 });
  const commonMeta = stoolTypeMeta[common.stoolType];
  const quirkyInsight = getQuirkyInsight(analytics);
  const personality = getPersonalityType(analytics);

  const shareToInstagram = async () => {
    try {
      if (!viewRef.current) return;
      
      const uri = await captureRef(viewRef, {
        format: "png",
        quality: 1,
        width: 1080,
        height: 1920,
      });
      
      if (await Sharing.isAvailableAsync()) {
        await Sharing.shareAsync(uri, {
          mimeType: "image/png",
          dialogTitle: "Share your Weekly Wrapped",
        });
      } else {
        Alert.alert("Share unavailable", "Sharing is not available on this device.");
      }
    } catch {
      Alert.alert("Share failed", "Unable to share your Weekly Wrapped.");
    }
  };

  const saveToGallery = async () => {
    try {
      if (!viewRef.current) return;
      
      // Request permission
      const { status } = await MediaLibrary.requestPermissionsAsync();
      if (status !== "granted") {
        Alert.alert("Permission Denied", "Please grant permission to save images to your gallery.");
        return;
      }
      
      const uri = await captureRef(viewRef, {
        format: "png",
        quality: 1,
        width: 1080,
        height: 1920,
      });
      
      await MediaLibrary.saveToLibraryAsync(uri);
      Alert.alert("Saved!", "Your Weekly Wrapped has been saved to your gallery.");
    } catch {
      Alert.alert("Save failed", "Unable to save to gallery.");
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <View ref={viewRef} collapsable={false} style={[styles.shareableContent, { backgroundColor: palette.background, padding: 24 }]}>
        <View style={styles.brandingHeader}>
          <Text style={[styles.appName, { color: palette.primary }]}>Bowels</Text>
          <Text style={[styles.wrappedTitle, { color: palette.onSurface }]}>Weekly Wrapped</Text>
        </View>
        <Card palette={palette} style={styles.hero}>
          <Text style={[styles.streak, { color: palette.primary }]}>{analytics.totalVisits}</Text>
          <Text style={[styles.label, { color: palette.onSurfaceVariant }]}>Total visits this week</Text>
        </Card>
        
        <Card palette={palette} style={styles.personalityCard}>
          <MaterialIcons name="psychology" size={48} color={palette.secondary} />
          <Text style={[styles.personalityType, { color: palette.onSurface }]}>{personality.type}</Text>
          <Text style={[styles.personalityDesc, { color: palette.onSurfaceVariant }]}>{personality.desc}</Text>
        </Card>

        <View style={styles.grid}>
          <Card palette={palette} style={styles.square}>
            <Text style={[styles.cardTitle, { color: palette.onSurface }]}>Avg Duration</Text>
            <View style={styles.metricWrap}>
              <Text style={[styles.big, { color: palette.primary }]}>{Math.round(analytics.averageDurationSeconds / 60)}m</Text>
            </View>
          </Card>
          <Card palette={palette} style={styles.square}>
            <Text style={[styles.cardTitle, { color: palette.onSurface }]}>Common</Text>
            <View style={styles.metricWrap}>
              <Text style={[styles.commonLabel, { color: palette.secondary }]} numberOfLines={2} adjustsFontSizeToFit>
                {commonMeta.short}
              </Text>
            </View>
          </Card>
        </View>
        
        <Card palette={palette}>
          <Text style={[styles.cardTitle, { color: palette.onSurface }]}>Best / Worst Day</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Best: {best.day} with {best.count} visits</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>Worst: {worst.day} with {worst.count} visits</Text>
        </Card>
        
        <Card palette={palette} style={styles.quirkCard}>
          <MaterialIcons name="auto-awesome" size={32} color={palette.tertiary ?? palette.secondary} />
          <Text style={[styles.quirkTitle, { color: palette.onSurface }]}>Your Unique Rhythm</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{quirkyInsight}</Text>
        </Card>
      </View>

      <TouchableOpacity 
        style={[styles.shareButton, { backgroundColor: palette.primary }]}
        onPress={shareToInstagram}
      >
        <MaterialIcons name="share" size={24} color={palette.onPrimary} />
        <Text style={[styles.shareText, { color: palette.onPrimary }]}>Share to Instagram</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={[styles.saveButton, { backgroundColor: palette.secondary }]}
        onPress={saveToGallery}
      >
        <MaterialIcons name="save-alt" size={24} color={palette.onPrimary} />
        <Text style={[styles.shareText, { color: palette.onPrimary }]}>Save to Gallery</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  shareableContent: { gap: 20, borderRadius: 24 },
  brandingHeader: { alignItems: "center", gap: 4, marginBottom: 12 },
  appName: { fontFamily: "Manrope_800ExtraBold", fontSize: 16, textTransform: "uppercase", letterSpacing: 2 },
  wrappedTitle: { fontFamily: "Manrope_700Bold", fontSize: 32 },
  hero: { alignItems: "center", justifyContent: "center", minHeight: 220 },
  streak: { fontFamily: "Manrope_800ExtraBold", fontSize: 72 },
  label: { fontFamily: "Manrope_700Bold", fontSize: 12, textTransform: "uppercase", letterSpacing: 1.6 },
  personalityCard: { alignItems: "center", gap: 12, paddingVertical: 32 },
  personalityType: { fontFamily: "Manrope_800ExtraBold", fontSize: 28, textAlign: "center" },
  personalityDesc: { fontFamily: "Manrope_500Medium", fontSize: 14, textAlign: "center" },
  grid: { flexDirection: "row", gap: 12 },
  square: { flex: 1, minHeight: 160, padding: 20 },
  cardTitle: { fontFamily: "Manrope_700Bold", fontSize: 16, marginBottom: 8 },
  metricWrap: { flex: 1, justifyContent: "center", alignItems: "center" },
  big: { fontFamily: "Manrope_800ExtraBold", fontSize: 48 },
  commonLabel: { fontFamily: "Manrope_800ExtraBold", fontSize: 32, textAlign: "center" },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20 },
  quirkCard: { alignItems: "center", gap: 12, paddingVertical: 28 },
  quirkTitle: { fontFamily: "Manrope_700Bold", fontSize: 20, textAlign: "center" },
  shareButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 12,
    paddingVertical: 16,
    paddingHorizontal: 32,
    borderRadius: 999,
    marginTop: 12,
  },
  saveButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 12,
    paddingVertical: 16,
    paddingHorizontal: 32,
    borderRadius: 999,
  },
  shareText: { fontFamily: "Manrope_700Bold", fontSize: 16 },
});
