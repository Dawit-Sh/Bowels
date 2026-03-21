import { Image, ScrollView, StyleSheet, Text, View } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { MaterialIcons } from "@expo/vector-icons";

const logo = require("../assets/icon.png");

const milestones = [
  { days: 1, title: "First Rhythm", body: "The beginning of a conscious path to harmony.", tone: "primary" },
  { days: 7, title: "Steady Flow", body: "Finding balance through consistency and care.", tone: "secondary" },
  { days: 30, title: "Deep Connection", body: "Listening to the internal whispers of the body.", tone: "locked" },
  { days: 100, title: "Master of Self", body: "Complete alignment of mind, habit, and health.", tone: "locked" },
] as const;

function getNextMilestone(progressDays: number) {
  return milestones.find((item) => progressDays < item.days) ?? { days: 365, title: "Eternal Sanctuary" };
}

function badgeBackground(palette: any, tone: string): [string, string] {
  if (tone === "primary") {
    return [palette.primaryFixed, palette.primaryContainer];
  }
  if (tone === "secondary") {
    return [palette.secondaryContainer, palette.secondary];
  }
  return [palette.surfaceContainerHighest, palette.surfaceContainerHigh];
}

export function BadgesScreen({ palette, progressDays }: { palette: any; progressDays: number }) {
  const nextMilestone = getNextMilestone(progressDays);
  const progressPercent = Math.max(0, Math.min(1, progressDays / nextMilestone.days));

  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <View style={styles.decorWrap}>
        <Image source={logo} style={styles.decorLogo} resizeMode="contain" />
      </View>

      <View style={styles.headerCard}>
        <View style={styles.row}>
          <View>
            <Text style={[styles.kicker, { color: `${palette.onSurfaceVariant}CC` }]}>Current Rhythm</Text>
            <View style={styles.dayRow}>
              <Text style={[styles.dayCount, { color: palette.primary }]}>{progressDays}</Text>
              <Text style={[styles.dayUnit, { color: `${palette.primary}99` }]}>days</Text>
            </View>
          </View>
          <View style={styles.nextWrap}>
            <Text style={[styles.nextCaption, { color: palette.onSurfaceVariant }]}>Next: {nextMilestone.title}</Text>
            <Text style={[styles.nextValue, { color: palette.primary }]}>{Math.max(0, nextMilestone.days - progressDays)} days to go</Text>
          </View>
        </View>

        <View style={styles.progressBlock}>
          <View style={[styles.progressTrack, { backgroundColor: palette.surfaceContainerHighest }]}>
            <View style={[styles.progressInner, { backgroundColor: palette.primary, width: `${progressPercent * 100}%` }]} />
          </View>
          <View style={styles.progressLabels}>
            <Text style={[styles.progressLabel, { color: `${palette.onSurfaceVariant}99` }]}>Steady Flow</Text>
            <Text style={[styles.progressLabel, { color: `${palette.onSurfaceVariant}99` }]}>{nextMilestone.title}</Text>
          </View>
        </View>
      </View>

      <View style={styles.grid}>
        {milestones.map((milestone, index) => {
          const earned = progressDays >= milestone.days;
          const locked = !earned;
          const accent = milestone.tone === "secondary" ? palette.secondary : palette.primary;
          return (
            <View
              key={milestone.days}
              style={[
                styles.milestoneCard,
                {
                  backgroundColor: locked ? `${palette.surfaceContainerLow}` : palette.surfaceContainerLowest,
                  opacity: locked ? (index === 2 ? 0.72 : 0.52) : 1,
                },
              ]}
            >
              {locked ? <MaterialIcons name="lock" size={18} color={`${palette.onSurfaceVariant}66`} style={styles.lockIcon} /> : null}

              <LinearGradient colors={badgeBackground(palette, milestone.tone)} style={styles.badgeShell}>
                <Image source={logo} style={[styles.badgeArt, locked && styles.lockedArt]} resizeMode="contain" />
                <View style={styles.badgeRing} />
              </LinearGradient>

              <View style={styles.textBlock}>
                <Text style={[styles.cardTitle, { color: locked ? `${palette.onSurfaceVariant}` : palette.onSurface }]}>{milestone.title}</Text>
                <Text style={[styles.cardBody, { color: locked ? `${palette.onSurfaceVariant}BB` : palette.onSurfaceVariant }]}>{milestone.body}</Text>
              </View>

              <View style={[styles.pill, { backgroundColor: locked ? palette.surfaceContainerHighest : `${accent}14` }]}>
                <Text style={[styles.pillText, { color: locked ? `${palette.onSurfaceVariant}88` : accent }]}>
                  {earned ? `${milestone.days} Day${milestone.days === 1 ? "" : "s"} Achieved` : index === 2 ? `Locked (${progressDays}/${milestone.days})` : `${milestone.days} Days Goal`}
                </Text>
              </View>
            </View>
          );
        })}
      </View>

      <View style={[styles.heroCard, { backgroundColor: `${palette.surfaceContainerHigh}AA` }]}>
        <LinearGradient colors={[`${palette.tertiaryContainer}55`, `${palette.tertiary ?? palette.secondary}22`]} style={styles.heroBadge}>
          <Image source={logo} style={[styles.heroArt, styles.lockedArt]} resizeMode="contain" />
        </LinearGradient>
        <Text style={[styles.heroTitle, { color: palette.onSurfaceVariant }]}>Eternal Sanctuary</Text>
        <Text style={[styles.heroBody, { color: `${palette.onSurfaceVariant}CC` }]}>
          The ultimate milestone. A year of dedicated wellness and listening to your inner rhythm.
        </Text>
        <View style={[styles.pill, { backgroundColor: `${palette.tertiary ?? palette.secondary}12` }]}>
          <Text style={[styles.pillText, { color: `${palette.tertiary ?? palette.secondary}99` }]}>The Final Frontier</Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, paddingTop: 8, gap: 24 },
  decorWrap: { position: "absolute", right: -60, top: -30, opacity: 0.05, transform: [{ rotate: "12deg" }] },
  decorLogo: { width: 220, height: 220 },
  headerCard: { gap: 18 },
  row: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-end", gap: 14 },
  kicker: { fontFamily: "Manrope_500Medium", fontSize: 13, textTransform: "uppercase", letterSpacing: 1.2 },
  dayRow: { flexDirection: "row", alignItems: "baseline", gap: 8 },
  dayCount: { fontFamily: "Manrope_800ExtraBold", fontSize: 56, letterSpacing: -2 },
  dayUnit: { fontFamily: "Manrope_700Bold", fontSize: 24 },
  nextWrap: { alignItems: "flex-end", gap: 4 },
  nextCaption: { fontFamily: "Manrope_500Medium", fontSize: 13 },
  nextValue: { fontFamily: "Manrope_700Bold", fontSize: 14 },
  progressBlock: { gap: 8 },
  progressTrack: { height: 16, borderRadius: 999, overflow: "hidden", padding: 4 },
  progressInner: { height: "100%", borderRadius: 999 },
  progressLabels: { flexDirection: "row", justifyContent: "space-between" },
  progressLabel: { fontFamily: "Manrope_700Bold", fontSize: 10, textTransform: "uppercase", letterSpacing: 1.2 },
  grid: { gap: 18 },
  milestoneCard: {
    borderRadius: 40,
    padding: 28,
    alignItems: "center",
    gap: 20,
    shadowColor: "#1b1d0e",
    shadowOpacity: 0.06,
    shadowRadius: 18,
    shadowOffset: { width: 8, height: 8 },
    elevation: 3,
  },
  lockIcon: { position: "absolute", left: 22, top: 22 },
  badgeShell: {
    width: 128,
    height: 128,
    borderRadius: 999,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
  },
  badgeArt: { width: 92, height: 92 },
  badgeRing: { position: "absolute", top: 0, right: 0, bottom: 0, left: 0, borderRadius: 999, borderWidth: 4, borderColor: "rgba(255,255,255,0.18)" },
  lockedArt: { opacity: 0.55 },
  textBlock: { gap: 4, alignItems: "center" },
  cardTitle: { fontFamily: "Manrope_700Bold", fontSize: 22, textAlign: "center" },
  cardBody: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20, textAlign: "center" },
  pill: { borderRadius: 999, paddingHorizontal: 16, paddingVertical: 8 },
  pillText: { fontFamily: "Manrope_700Bold", fontSize: 11, textTransform: "uppercase", letterSpacing: 1.1 },
  heroCard: { borderRadius: 48, padding: 36, alignItems: "center", gap: 20 },
  heroBadge: { width: 192, height: 192, borderRadius: 999, alignItems: "center", justifyContent: "center" },
  heroArt: { width: 132, height: 132 },
  heroTitle: { fontFamily: "Manrope_800ExtraBold", fontSize: 34, textAlign: "center" },
  heroBody: { fontFamily: "Manrope_400Regular", fontSize: 16, lineHeight: 24, textAlign: "center" },
});
