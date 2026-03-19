import React from 'react';
import { Pressable, StyleSheet, Text, ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';

type Props = {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  disabled?: boolean;
  style?: ViewStyle;
  testID?: string;
};

export function Button({ label, onPress, variant = 'primary', disabled, style, testID }: Props) {
  const theme = useTheme();
  const colors = theme.colors;

  const bg =
    variant === 'primary'
      ? colors.accent
      : variant === 'secondary'
        ? colors.surface2
        : variant === 'danger'
          ? colors.danger
          : 'transparent';

  const textColor =
    variant === 'primary' || variant === 'danger' ? '#FFFFFF' : theme.colors.textPrimary;

  const borderColor = variant === 'ghost' ? colors.border : 'transparent';

  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        {
          backgroundColor: bg,
          borderColor,
          opacity: disabled ? 0.5 : pressed ? 0.9 : 1,
        },
        style,
      ]}>
      <Text style={[styles.label, { color: textColor }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 52,
    paddingHorizontal: 16,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
  },
  label: {
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
});

