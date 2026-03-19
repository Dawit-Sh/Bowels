import React from 'react';
import { StyleSheet, Text, View, type ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';
import { withAlpha } from '@/utils/color';

export function MetricTile({
  label,
  value,
  unit,
  hint,
  style,
}: {
  label: string;
  value: string;
  unit?: string;
  hint?: string;
  style?: ViewStyle;
}) {
  const theme = useTheme();
  return (
    <View
      style={[
        styles.base,
        {
          borderRadius: theme.radius.xl,
          borderColor: theme.colors.border,
          backgroundColor: withAlpha(theme.colors.surface, theme.mode === 'dark' ? 0.7 : 0.85),
        },
        style,
      ]}>
      <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{label}</Text>
      <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 6, marginTop: 10 }}>
        <Text style={{ color: theme.colors.textPrimary, fontSize: 44, fontWeight: '900', letterSpacing: -1.2 }}>
          {value}
        </Text>
        {unit ? <Text style={{ color: theme.colors.textSecondary, fontSize: 16, fontWeight: '800' }}>{unit}</Text> : null}
      </View>
      {hint ? (
        <Text style={{ marginTop: 8, color: theme.colors.textSecondary, fontWeight: '700' }}>{hint}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    borderWidth: 1,
    padding: 16,
  },
});

