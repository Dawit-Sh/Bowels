import React from 'react';
import { SafeAreaView, ScrollView, StyleSheet, View, ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';
import { withAlpha } from '@/utils/color';

type Props = {
  children: React.ReactNode;
  scroll?: boolean;
  contentStyle?: ViewStyle;
};

export function Screen({ children, scroll = true, contentStyle }: Props) {
  const theme = useTheme();

  if (!scroll) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]}>
        <View
          style={[
            styles.backdrop,
            {
              backgroundColor:
                theme.mode === 'dark'
                  ? withAlpha(theme.colors.accent, 0.08)
                  : withAlpha(theme.colors.accent, 0.06),
            },
          ]}
        />
        <View style={[styles.content, contentStyle]}>{children}</View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]}>
      <View
        style={[
          styles.backdrop,
          {
            backgroundColor:
              theme.mode === 'dark' ? withAlpha(theme.colors.accent, 0.08) : withAlpha(theme.colors.accent, 0.06),
          },
        ]}
      />
      <ScrollView contentContainerStyle={[styles.content, contentStyle]}>{children}</ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
  },
  backdrop: {
    position: 'absolute',
    top: -220,
    left: -220,
    width: 520,
    height: 520,
    borderRadius: 999,
  },
  content: {
    padding: 16,
    gap: 12,
  },
});
