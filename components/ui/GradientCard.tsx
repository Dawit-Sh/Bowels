import { LinearGradient } from 'expo-linear-gradient';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';
import { withAlpha } from '@/utils/color';

type Props = {
  children: React.ReactNode;
  style?: ViewStyle;
  tone?: 'neutral' | 'accent' | 'pastel';
};

export function GradientCard({ children, style, tone = 'neutral' }: Props) {
  const theme = useTheme();

  const colors: [string, string] =
    tone === 'accent'
      ? [withAlpha(theme.colors.accent, 0.20), withAlpha(theme.colors.accent, 0.06)]
      : tone === 'pastel'
        ? theme.mode === 'dark'
          ? [withAlpha('#B8C6FF', 0.10), withAlpha('#FFB8E1', 0.06)]
          : [withAlpha('#B8C6FF', 0.30), withAlpha('#FFB8E1', 0.16)]
        : theme.mode === 'dark'
          ? [withAlpha('#FFFFFF', 0.06), withAlpha('#FFFFFF', 0.02)]
          : [withAlpha('#FFFFFF', 0.90), withAlpha('#FFFFFF', 0.72)];

  return (
    <View
      style={[
        styles.shell,
        {
          borderRadius: theme.radius.xl,
          borderColor: theme.colors.border,
          shadowColor: '#000',
        },
        style,
      ]}>
      <LinearGradient
        colors={colors}
        start={{ x: 0.1, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={[
          styles.inner,
          {
            borderRadius: theme.radius.xl,
          },
        ]}>
        {children}
      </LinearGradient>
    </View>
  );
}

const styles = StyleSheet.create({
  shell: {
    borderWidth: 1,
    overflow: 'hidden',
    shadowOpacity: 0.12,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 10 },
    elevation: 6,
  },
  inner: {
    padding: 18,
  },
});
