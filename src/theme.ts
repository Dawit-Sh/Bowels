import type { AccentKey, ThemeMode } from "./types";

const base = {
  surfaceTint: "#56642b",
  surfaceContainerHighest: "#e4e4cc",
  onBackground: "#1b1d0e",
  surfaceContainerLow: "#f5f5dc",
  background: "#fbfbe2",
  onSurfaceVariant: "#46483c",
  surfaceContainerHigh: "#eaead1",
  onPrimary: "#ffffff",
  surfaceContainer: "#efefd7",
  onSurface: "#1b1d0e",
  surfaceContainerLowest: "#ffffff",
  outline: "#76786b",
  primaryContainer: "#8a9a5b",
  surfaceVariant: "#e4e4cc",
  surfaceDim: "#dbdcc3",
  inverseSurface: "#303221",
} as const;

const accents: Record<AccentKey, { primary: string; secondary: string; tertiary: string; primaryFixedDim: string; secondaryFixed: string }> = {
  olive: { primary: "#56642b", secondary: "#9f402d", tertiary: "#8c4b55", primaryFixedDim: "#bdce89", secondaryFixed: "#ffdad3" },
  coral: { primary: "#8a5a2b", secondary: "#bf5b3f", tertiary: "#9f402d", primaryFixedDim: "#e2c089", secondaryFixed: "#ffd8d0" },
  rose: { primary: "#7a4d5b", secondary: "#b65a6d", tertiary: "#8c4b55", primaryFixedDim: "#dbb2bc", secondaryFixed: "#ffd9dd" },
  slate: { primary: "#41525e", secondary: "#7d5c4c", tertiary: "#58657a", primaryFixedDim: "#a9bcc8", secondaryFixed: "#e7dad4" },
};

export const radius = {
  pill: 999,
  xl: 20,
  xxl: 28,
  jumbo: 40,
  giant: 48,
};

export function buildPalette(mode: ThemeMode, accent: AccentKey) {
  const accentColors = accents[accent];
  const dark = mode === "dark";
  return {
    ...base,
    ...accentColors,
    background: dark ? "#1b1d0e" : base.background,
    surfaceContainerLowest: dark ? "#2a2d18" : base.surfaceContainerLowest,
    surfaceContainerLow: dark ? "#252815" : base.surfaceContainerLow,
    surfaceContainer: dark ? "#2d311c" : base.surfaceContainer,
    surfaceContainerHigh: dark ? "#343923" : base.surfaceContainerHigh,
    surfaceContainerHighest: dark ? "#3d422b" : base.surfaceContainerHighest,
    onBackground: dark ? "#f2f2d9" : base.onBackground,
    onSurface: dark ? "#f2f2d9" : base.onSurface,
    onSurfaceVariant: dark ? "#c7ccb1" : base.onSurfaceVariant,
    outline: dark ? "#969b84" : base.outline,
  };
}
