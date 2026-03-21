import { Image, ScrollView, StyleSheet, Text, View } from "react-native";
import { MaterialIcons } from "@expo/vector-icons";

import { Card, GradientButton } from "../components/UI";

const logo = require("../assets/icon.png");

const features = [
  {
    icon: "visibility-off",
    title: "Discrete Tracking",
    body: "Elegant, quiet entries that blend into your daily flow.",
    tone: "primary",
  },
  {
    icon: "favorite",
    title: "Local-First Privacy",
    body: "Your data never leaves your device. Total ownership.",
    tone: "secondary",
  },
  {
    icon: "spa",
    title: "Gentle Insights",
    body: "Meaningful patterns shared without judgment.",
    tone: "neutral",
  },
] as const;

export function OnboardingScreen({ palette, completeOnboarding }: { palette: any; completeOnboarding: () => Promise<void> }) {
  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <View style={styles.brand}>
        <Image source={logo} style={styles.logo} resizeMode="contain" />
        <Text style={[styles.brandText, { color: palette.primary }]}>Bowels</Text>
      </View>

      <View style={styles.hero}>
        <Text style={[styles.title, { color: palette.onSurface }]}>Welcome to Bowels.</Text>
        <Text style={[styles.subtitle, { color: palette.onSurfaceVariant }]}>
          A private, serene space to understand your body&apos;s natural rhythm.
        </Text>
      </View>

      <View style={styles.grid}>
        {features.map((feature, index) => {
          const circleColor =
            feature.tone === "primary" ? palette.primaryFixed : feature.tone === "secondary" ? palette.secondaryFixed : palette.surfaceContainerHighest;
          const iconColor =
            feature.tone === "primary" ? palette.primary : feature.tone === "secondary" ? palette.secondary : palette.onSurfaceVariant;
          return (
            <Card key={feature.title} palette={palette} style={[styles.card, index === 1 ? styles.offsetCard : null]}>
              <View style={[styles.iconWrap, { backgroundColor: circleColor }]}>
                <MaterialIcons name={feature.icon} size={24} color={iconColor} />
              </View>
              <Text style={[styles.cardTitle, { color: palette.onSurface }]}>{feature.title}</Text>
              <Text style={[styles.cardBody, { color: palette.onSurfaceVariant }]}>{feature.body}</Text>
            </Card>
          );
        })}
      </View>

      <View style={styles.actions}>
        <GradientButton palette={palette} label="Begin Your Journey" onPress={() => void completeOnboarding()} />
        <View style={[styles.privacyBadge, { backgroundColor: palette.surfaceContainerHigh }]}>
          <MaterialIcons name="lock" size={16} color={palette.primary} />
          <Text style={[styles.privacyText, { color: palette.onSurfaceVariant }]}>No accounts. No cloud. Just you and your data.</Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: {
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 48,
    gap: 24,
    minHeight: "100%",
  },
  brand: { alignItems: "center", gap: 10, marginTop: 8 },
  logo: { width: 72, height: 72 },
  brandText: { fontFamily: "Manrope_700Bold", fontSize: 22 },
  hero: { alignItems: "center", gap: 12, marginTop: 12 },
  title: { fontFamily: "Manrope_800ExtraBold", fontSize: 38, textAlign: "center" },
  subtitle: { fontFamily: "Manrope_400Regular", fontSize: 18, lineHeight: 28, textAlign: "center" },
  grid: { gap: 16, marginTop: 8 },
  card: { alignItems: "center", textAlign: "center" },
  offsetCard: { marginHorizontal: 12 },
  iconWrap: { width: 48, height: 48, borderRadius: 999, alignItems: "center", justifyContent: "center", marginBottom: 4 },
  cardTitle: { fontFamily: "Manrope_700Bold", fontSize: 18, textAlign: "center" },
  cardBody: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20, textAlign: "center" },
  actions: { gap: 20, marginTop: 8 },
  privacyBadge: {
    borderRadius: 999,
    paddingHorizontal: 18,
    paddingVertical: 14,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
  },
  privacyText: { fontFamily: "Manrope_500Medium", fontSize: 12, textAlign: "center" },
});
