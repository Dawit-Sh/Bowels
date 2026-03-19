import type { AccentColor, AppTheme, ColorMode } from './types';

const accentMap: Record<AccentColor, string> = {
  blue: '#2F6BFF',
  green: '#2DB36B',
  purple: '#8C52FF',
  orange: '#FF8A2D',
};

export function createTheme(mode: ColorMode, accent: AccentColor): AppTheme {
  const isDark = mode === 'dark';
  const accentHex = accentMap[accent];

  return {
    mode,
    accent,
    colors: {
      background: isDark ? '#0B0D10' : '#F6F7FB',
      surface: isDark ? '#121621' : '#FFFFFF',
      surface2: isDark ? '#161B2B' : '#F0F2F7',
      textPrimary: isDark ? '#F3F6FF' : '#0B0D10',
      textSecondary: isDark ? 'rgba(243,246,255,0.72)' : 'rgba(11,13,16,0.64)',
      accent: accentHex,
      border: isDark ? 'rgba(255,255,255,0.10)' : 'rgba(11,13,16,0.10)',
      danger: '#FF4D4F',
      warning: '#F5A524',
      success: '#2DB36B',
      muted: isDark ? 'rgba(243,246,255,0.12)' : 'rgba(11,13,16,0.08)',
    },
    radius: { sm: 12, md: 16, lg: 20, xl: 28 },
    spacing: { xs: 6, sm: 10, md: 14, lg: 18, xl: 24 },
  };
}

export const ACCENTS: AccentColor[] = ['blue', 'green', 'purple', 'orange'];

