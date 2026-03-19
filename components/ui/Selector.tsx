import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';

export type SelectorOption = {
  key: string;
  label: string;
  hint?: string;
};

export function Selector({
  options,
  value,
  onChange,
  columns = 2,
}: {
  options: SelectorOption[];
  value: string | null;
  onChange: (key: string) => void;
  columns?: 1 | 2 | 3;
}) {
  const theme = useTheme();
  return (
    <View style={[styles.grid, { gap: 10 }]}>
      {options.map((opt) => {
        const selected = opt.key === value;
        return (
          <Pressable
            key={opt.key}
            accessibilityRole="button"
            onPress={() => onChange(opt.key)}
            style={[
              styles.option,
              {
                width: `${Math.floor(100 / columns)}%`,
                backgroundColor: selected ? theme.colors.accent : theme.colors.surface,
                borderColor: selected ? theme.colors.accent : theme.colors.border,
              },
            ]}>
            <Text style={[styles.optionLabel, { color: selected ? '#fff' : theme.colors.textPrimary }]}>
              {opt.label}
            </Text>
            {opt.hint ? (
              <Text style={[styles.optionHint, { color: selected ? 'rgba(255,255,255,0.82)' : theme.colors.textSecondary }]}>
                {opt.hint}
              </Text>
            ) : null}
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  option: {
    minHeight: 56,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 16,
    borderWidth: 1,
    justifyContent: 'center',
  },
  optionLabel: {
    fontSize: 15,
    fontWeight: '700',
  },
  optionHint: {
    marginTop: 2,
    fontSize: 12,
    fontWeight: '600',
  },
});

