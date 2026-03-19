import React from 'react';

import { useSettingsStore } from '@/store/settings';
import { createTheme } from '@/theme/createTheme';
import type { AppTheme } from '@/theme/types';

const ThemeContext = React.createContext<AppTheme | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const mode = useSettingsStore((s) => s.mode);
  const accent = useSettingsStore((s) => s.accent);

  const theme = React.useMemo(() => createTheme(mode, accent), [mode, accent]);
  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}

export function useTheme(): AppTheme {
  const ctx = React.useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return ctx;
}

