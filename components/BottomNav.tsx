import { Pressable, StyleSheet, Text } from "react-native";
import { BlurView } from "expo-blur";
import { MaterialIcons } from "@expo/vector-icons";

import type { AppScreen } from "../src/types";

const items: Array<{ key: AppScreen; label: string; icon: keyof typeof MaterialIcons.glyphMap }> = [
  { key: "home", label: "Home", icon: "menu-book" },
  { key: "active", label: "Rhythm", icon: "bakery-dining" },
  { key: "analytics", label: "Insights", icon: "insights" },
  { key: "history", label: "History", icon: "history" },
];

export function BottomNav({ palette, screen, setScreen }: { palette: any; screen: AppScreen; setScreen: (screen: AppScreen) => void }) {
  const dark = palette.background !== "#fbfbe2";
  return (
    <BlurView intensity={36} tint={dark ? "dark" : "light"} style={[styles.wrap, { backgroundColor: dark ? "rgba(37,40,21,0.92)" : `${palette.surfaceContainerLow}CC`, borderTopColor: `${palette.outline}22` }]}>
      {items.map((item) => {
        const active = screen === item.key;
        return (
          <Pressable key={item.key} style={[styles.item, active && { backgroundColor: dark ? palette.surfaceContainerHigh : palette.surfaceContainerLowest }]} onPress={() => setScreen(item.key)}>
            <MaterialIcons name={item.icon} size={22} color={active ? palette.primary : palette.onSurfaceVariant} />
            <Text style={[styles.label, { color: active ? palette.primary : palette.onSurfaceVariant }]}>{item.label}</Text>
          </Pressable>
        );
      })}
    </BlurView>
  );
}

const styles = StyleSheet.create({
  wrap: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: "row",
    justifyContent: "space-around",
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 24,
    borderTopLeftRadius: 34,
    borderTopRightRadius: 34,
    overflow: "hidden",
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  item: {
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 999,
    gap: 4,
  },
  label: {
    fontFamily: "Manrope_700Bold",
    fontSize: 10,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
});
