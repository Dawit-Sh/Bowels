import { ThemeProvider as NavThemeProvider } from '@react-navigation/native';
import * as SplashScreen from 'expo-splash-screen';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import React from 'react';
import 'react-native-reanimated';

import { AppBootstrap } from '@/screens/AppBootstrap';
import { ThemeProvider, useTheme } from '@/theme/ThemeProvider';

SplashScreen.preventAutoHideAsync().catch(() => {
  // no-op
});

function NavigationThemeBridge({ children }: { children: React.ReactNode }) {
  const theme = useTheme();
  return (
    <NavThemeProvider
      value={{
        dark: theme.mode === 'dark',
        colors: {
          primary: theme.colors.accent,
          background: theme.colors.background,
          card: theme.colors.surface,
          text: theme.colors.textPrimary,
          border: theme.colors.border,
          notification: theme.colors.accent,
        },
        fonts: {
          regular: { fontFamily: 'System', fontWeight: '400' },
          medium: { fontFamily: 'System', fontWeight: '500' },
          bold: { fontFamily: 'System', fontWeight: '700' },
          heavy: { fontFamily: 'System', fontWeight: '800' },
        },
      }}>
      {children}
    </NavThemeProvider>
  );
}

export default function RootLayout() {
  return (
    <ThemeProvider>
      <NavigationThemeBridge>
        <AppBootstrap>
          <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="onboarding" options={{ presentation: 'fullScreenModal' }} />
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="session/active" options={{ presentation: 'modal' }} />
            <Stack.Screen name="session/questions" options={{ presentation: 'modal' }} />
            <Stack.Screen name="session/summary" options={{ presentation: 'modal' }} />
            <Stack.Screen name="wrapped" options={{ presentation: 'modal' }} />
            <Stack.Screen name="health-info" options={{ presentation: 'modal' }} />
          </Stack>
        </AppBootstrap>
        <StatusBar style="auto" />
      </NavigationThemeBridge>
    </ThemeProvider>
  );
}
