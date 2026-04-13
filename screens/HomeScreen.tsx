import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { MaterialIcons } from "@expo/vector-icons";
import { memo } from "react";

import { Card, GradientButton, OptionChip, SectionTitle } from "../components/UI";
import { quickLogPresets, stoolTypeMeta } from "../src/sessionMeta";
import type { AnalyticsSummary, AppScreen, DailyHealthInput, InsightItem, SessionRecord } from "../src/types";

const healthOptions = {
  water: ["Low", "Okay", "Great"],
  fiber: ["Low", "Medium", "High"],
  meals: ["Light", "Balanced", "Heavy"],
  stress: ["Low", "Medium", "High"],
  sleep: ["Poor", "Fair", "Great"],
  exercise: ["None", "Light", "Active"],
  caffeine: ["None", "Low", "Medium", "High"],
  alcohol: ["None", "Light", "Moderate", "Heavy"],
  medication: ["None", "Prescribed", "OTC", "Both"],
  mood: ["Happy", "Neutral", "Sad", "Anxious", "Stressed"],
} as const;

export function HomeScreen({
  palette,
  sessions,
  insights,
  analytics,
  dailyHealth,
  setScreen,
  startSession,
  saveDailyHealth,
  quickLogBowel,
}: {
  palette: any;
  sessions: SessionRecord[];
  insights: InsightItem[];
  analytics: AnalyticsSummary;
  dailyHealth: DailyHealthInput;
  setScreen: (screen: AppScreen) => void;
  startSession: () => void;
  saveDailyHealth: (input: DailyHealthInput) => Promise<void>;
  quickLogBowel: (stoolType: 1 | 2 | 3 | 4 | 5 | 6 | 7) => Promise<void>;
}) {
  const dailyBars = new Array(7).fill(0).map((_, idx) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - idx));
    const key = date.toISOString().slice(0, 10);
    return {
      key,
      label: date.toLocaleDateString(undefined, { weekday: "short" }).slice(0, 3).toUpperCase(),
      count: sessions.filter((session) => session.startTime.slice(0, 10) === key).length,
      active: idx === 6,
    };
  });

  // Generate dynamic status message based on actual data
  const todayCount = dailyBars[6].count;
  const yesterdayCount = dailyBars[5].count;
  const last7DaysCount = dailyBars.reduce((sum, day) => sum + day.count, 0);
  const avgPerDay = last7DaysCount / 7;
  
  const statusMessage = (() => {
    if (sessions.length === 0) {
      return "Start tracking your bowel health";
    }
    
    const lastSession = sessions[0];
    const hoursSinceLastSession = (Date.now() - new Date(lastSession.startTime).getTime()) / (1000 * 60 * 60);
    
    if (hoursSinceLastSession > 48) {
      return "No activity in 2+ days";
    }
    
    if (todayCount === 0 && hoursSinceLastSession < 24) {
      return "No logs today yet";
    }
    
    if (todayCount > 0) {
      if (todayCount === Math.round(avgPerDay)) {
        return "Your rhythm is steady today";
      } else if (todayCount > avgPerDay) {
        return "More active than usual today";
      } else {
        return "Less active than usual today";
      }
    }
    
    if (Math.abs(last7DaysCount - 7) <= 1) {
      return "Consistent daily rhythm";
    }
    
    return "Tracking your bowel health";
  })();
  
  const statusSubtitle = (() => {
    if (sessions.length === 0) {
      return "Begin your journey to better digestive health with quick logging and insights.";
    }
    
    const consistency = last7DaysCount > 0 ? Math.min(100, Math.round((last7DaysCount / 7) * 100)) : 0;
    
    if (consistency >= 90) {
      return `${last7DaysCount} sessions this week. Excellent tracking consistency!`;
    } else if (consistency >= 70) {
      return `${last7DaysCount} sessions this week. Good tracking habits.`;
    } else if (consistency >= 50) {
      return `${last7DaysCount} sessions this week. Keep building the habit.`;
    } else if (last7DaysCount > 0) {
      return `${last7DaysCount} session${last7DaysCount === 1 ? '' : 's'} this week. Track daily for better insights.`;
    }
    
    return "Fast, local-first tracking with quick log, daily health, and predictive insights.";
  })();

  const latestInsight = insights[0];

  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title={statusMessage} subtitle={statusSubtitle} />

      {latestInsight ? (
        <Card palette={palette} style={{ borderLeftWidth: 4, borderLeftColor: latestInsight.severity === "high" ? "#ba1a1a" : latestInsight.severity === "warning" ? palette.secondary : palette.primary }}>
          <Text style={[styles.cardTitle, { color: palette.onSurface }]}>{latestInsight.title}</Text>
          <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{latestInsight.body}</Text>
        </Card>
      ) : null}

      <View style={styles.badges}>
        <MemoizedBadge palette={palette} label={`Next likely bowel time: ${analytics.predictedNextTimeLabel}`} tone="primary" />
        <MemoizedBadge palette={palette} label={`Avg duration ${Math.round(analytics.averageDurationSeconds / 60) || 0}m`} tone="secondary" />
      </View>

      <Card palette={palette}>
        <View style={styles.row}>
          <Text style={[styles.cardTitle, { color: palette.primary }]}>Daily Rhythm</Text>
          <Text style={[styles.small, { color: palette.onSurfaceVariant }]}>Last 7 days</Text>
        </View>
        <View style={styles.bars}>
          {dailyBars.map((item) => (
            <View key={item.key} style={styles.barCol}>
              <View
                style={[
                  styles.bar,
                  {
                    height: 34 + item.count * 22,
                    backgroundColor: item.active ? palette.primary : item.count > 0 ? `${palette.primaryFixedDim}CC` : palette.surfaceContainerHigh,
                    width: item.active ? 34 : 28,
                  },
                ]}
              />
              <Text style={[styles.barLabel, { color: item.active ? palette.primary : palette.outline }]}>{item.label}</Text>
            </View>
          ))}
        </View>
      </Card>

      <GradientButton palette={palette} label="Start Session" icon="play-arrow" onPress={startSession} />

      <Card palette={palette}>
        <View style={styles.row}>
          <Text style={[styles.cardTitle, { color: palette.primary }]}>Quick Log</Text>
          <Text style={[styles.small, { color: palette.onSurfaceVariant }]}>One tap</Text>
        </View>
        <View style={styles.quickGrid}>
          {quickLogPresets.map((preset) => (
            <Pressable key={preset.stoolType} style={[styles.quickItem, { backgroundColor: palette.surfaceContainerHigh }]} onPress={() => void quickLogBowel(preset.stoolType)}>
              <Text style={styles.emoji}>{stoolTypeMeta[preset.stoolType].emoji}</Text>
              <Text style={[styles.quickTitle, { color: palette.onSurface }]}>{preset.title}</Text>
              <Text style={[styles.quickBody, { color: palette.onSurfaceVariant }]}>{stoolTypeMeta[preset.stoolType].description}</Text>
            </Pressable>
          ))}
        </View>
      </Card>

      <Card palette={palette}>
        <Text style={[styles.cardTitle, { color: palette.primary }]}>Daily Health Check</Text>
        {Object.entries(healthOptions).map(([key, options]) => (
          <View key={key} style={styles.fieldBlock}>
            <Text style={[styles.fieldLabel, { color: palette.onSurface }]}>{key}</Text>
            <View style={styles.chips}>
              {options.map((option) => (
                <OptionChip
                  key={option}
                  palette={palette}
                  label={option}
                  active={dailyHealth[key as keyof DailyHealthInput] === option}
                  onPress={() => void saveDailyHealth({ ...dailyHealth, [key]: option })}
                />
              ))}
            </View>
          </View>
        ))}
      </Card>

      <View style={styles.quickLinks}>
        <MemoizedQuickLink palette={palette} icon="auto-awesome" onPress={() => setScreen("weekly")} />
        <MemoizedQuickLink palette={palette} icon="emoji-events" onPress={() => setScreen("badges")} />
        <MemoizedQuickLink palette={palette} icon="health-and-safety" onPress={() => setScreen("health")} />
      </View>
    </ScrollView>
  );
}

function QuickLink({ palette, icon, onPress }: { palette: any; icon: keyof typeof MaterialIcons.glyphMap; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={[styles.quickCard, { backgroundColor: palette.surfaceContainerLowest }]}>
      <MaterialIcons name={icon} size={32} color={palette.primary} />
    </Pressable>
  );
}

const MemoizedQuickLink = memo(QuickLink);

function Badge({ palette, label, tone }: { palette: any; label: string; tone: "primary" | "secondary" }) {
  const bg = tone === "primary" ? `${palette.primary}14` : `${palette.secondary}14`;
  const fg = tone === "primary" ? palette.primary : palette.secondary;
  return (
    <View style={[styles.badge, { backgroundColor: bg }]}>
      <Text style={[styles.badgeText, { color: fg }]}>{label}</Text>
    </View>
  );
}

const MemoizedBadge = memo(Badge);

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  row: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 12 },
  badges: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  badge: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 999 },
  badgeText: { fontFamily: "Manrope_700Bold", fontSize: 12 },
  cardTitle: { fontFamily: "Manrope_700Bold", fontSize: 20 },
  small: { fontFamily: "Manrope_500Medium", fontSize: 13 },
  bars: { flexDirection: "row", alignItems: "flex-end", justifyContent: "space-between", gap: 8, minHeight: 130 },
  barCol: { alignItems: "center", gap: 8, flex: 1 },
  bar: { borderRadius: 999 },
  barLabel: { fontFamily: "Manrope_700Bold", fontSize: 10 },
  quickGrid: { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  quickItem: { width: "47%", borderRadius: 24, padding: 16, gap: 6 },
  emoji: { fontSize: 28 },
  quickTitle: { fontFamily: "Manrope_700Bold", fontSize: 16 },
  quickBody: { fontFamily: "Manrope_400Regular", fontSize: 12, lineHeight: 17 },
  fieldBlock: { gap: 10 },
  fieldLabel: { fontFamily: "Manrope_700Bold", fontSize: 14, textTransform: "capitalize" },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  quickLinks: { flexDirection: "row", flexWrap: "wrap", gap: 12, justifyContent: "center" },
  quickCard: { width: 80, height: 80, borderRadius: 20, alignItems: "center", justifyContent: "center", shadowColor: "#000", shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 2 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20 },
});
