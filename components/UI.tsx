import type { ReactNode } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { MaterialIcons } from "@expo/vector-icons";

export function Card({ palette, children, style }: { palette: any; children: ReactNode; style?: any }) {
  return <View style={[styles.card, { backgroundColor: palette.surfaceContainerLowest }, style]}>{children}</View>;
}

export function GradientButton({ palette, label, icon, onPress }: { palette: any; label: string; icon?: keyof typeof MaterialIcons.glyphMap; onPress: () => void }) {
  return (
    <Pressable onPress={onPress}>
      <LinearGradient colors={[palette.primary, palette.primaryContainer]} style={styles.button}>
        <Text style={styles.buttonLabel}>{label}</Text>
        {icon ? <MaterialIcons name={icon} size={18} color={palette.onPrimary} /> : null}
      </LinearGradient>
    </Pressable>
  );
}

export function OptionChip({ palette, label, active, onPress }: { palette: any; label: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={[styles.chip, { backgroundColor: active ? palette.primary : palette.surfaceContainerHigh }]}>
      <Text style={[styles.chipLabel, { color: active ? palette.onPrimary : palette.onSurface }]}>{label}</Text>
    </Pressable>
  );
}

export function SectionTitle({ palette, title, subtitle }: { palette: any; title: string; subtitle?: string }) {
  return (
    <View style={{ gap: 4 }}>
      <Text style={[styles.title, { color: palette.onSurface }]}>{title}</Text>
      {subtitle ? <Text style={[styles.subtitle, { color: palette.onSurfaceVariant }]}>{subtitle}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 32,
    padding: 24,
    gap: 16,
    shadowColor: "#9fa28f",
    shadowOpacity: 0.18,
    shadowRadius: 18,
    shadowOffset: { width: 6, height: 10 },
    elevation: 4,
  },
  button: {
    borderRadius: 24,
    paddingVertical: 16,
    paddingHorizontal: 20,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
  },
  buttonLabel: {
    fontFamily: "Manrope_700Bold",
    fontSize: 16,
    color: "#ffffff",
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: 999,
  },
  chipLabel: {
    fontFamily: "Manrope_700Bold",
    fontSize: 13,
  },
  title: {
    fontFamily: "Manrope_800ExtraBold",
    fontSize: 28,
  },
  subtitle: {
    fontFamily: "Manrope_400Regular",
    fontSize: 14,
    lineHeight: 20,
  },
});
