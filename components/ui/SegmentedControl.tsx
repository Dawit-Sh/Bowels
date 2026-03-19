import React from 'react';
import { Pressable, StyleSheet, Text, View, type ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';
import { withAlpha } from '@/utils/color';

export type SegOption = { key: string; label: string };

export function SegmentedControl({
  options,
  value,
  onChange,
  size = 'md',
  style,
}: {
  options: SegOption[];
  value: string | null;
  onChange: (key: string) => void;
  size?: 'sm' | 'md';
  style?: ViewStyle;
}) {
  const theme = useTheme();
  const pad = size === 'sm' ? 4 : 6;
  const height = size === 'sm' ? 32 : 38;

  return (
    <View
      style={[
        styles.wrap,
        {
          padding: pad,
          borderRadius: 999,
          backgroundColor: withAlpha(theme.colors.surface2, theme.mode === 'dark' ? 0.75 : 0.95),
          borderColor: theme.colors.border,
          minHeight: height,
        },
        style,
      ]}>
      {options.map((o) => {
        const selected = o.key === value;
        return (
          <Pressable
            key={o.key}
            accessibilityRole="button"
            onPress={() => onChange(o.key)}
            style={({ pressed }) => [
              styles.pill,
              {
                borderRadius: 999,
                backgroundColor: selected ? theme.colors.accent : 'transparent',
                opacity: pressed ? 0.9 : 1,
                paddingVertical: size === 'sm' ? 6 : 8,
                paddingHorizontal: size === 'sm' ? 10 : 12,
              },
            ]}>
            <Text
              style={{
                color: selected ? '#fff' : theme.colors.textSecondary,
                fontWeight: selected ? '900' : '800',
                fontSize: size === 'sm' ? 12 : 13,
              }}>
              {o.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: 'row',
    gap: 6,
    borderWidth: 1,
    alignSelf: 'flex-start',
  },
  pill: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});

