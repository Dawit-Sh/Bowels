export type ColorMode = 'light' | 'dark';
export type AccentColor = 'blue' | 'green' | 'purple' | 'orange';

export type ThemeColors = {
  background: string;
  surface: string;
  surface2: string;
  textPrimary: string;
  textSecondary: string;
  accent: string;
  border: string;
  danger: string;
  warning: string;
  success: string;
  muted: string;
};

export type AppTheme = {
  mode: ColorMode;
  accent: AccentColor;
  colors: ThemeColors;
  radius: {
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
  spacing: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
};

