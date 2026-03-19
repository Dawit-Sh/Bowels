import React from 'react';
import { Pressable, StyleSheet, Text, View, ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';
import { IconSymbol, IconSymbolName } from '@/components/ui/icon-symbol';

type Props = {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  disabled?: boolean;
  style?: ViewStyle;
  testID?: string;
  iconRight?: IconSymbolName;
};

export function Button({ label, onPress, variant = 'primary', disabled, style, testID, iconRight }: Props) {
  const theme = useTheme();
  const colors = theme.colors;

  const bg =
    variant === 'primary'
      ? '#1C1C1E' // The Matrix black background
      : variant === 'secondary'
        ? colors.surface2
        : variant === 'danger'
          ? colors.danger
          : 'transparent';

  const textColor =
    variant === 'primary' || variant === 'danger' ? '#FFFFFF' : theme.colors.textPrimary;

  const borderColor = variant === 'ghost' ? colors.border : 'transparent';
  
  // The 'primary' variant gets rounded heavy pill style
  const isPrimary = variant === 'primary';

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
          borderRadius: isPrimary ? 32 : 16,
          paddingHorizontal: isPrimary && iconRight ? 8 : 16,
          flexDirection: 'row',
          opacity: disabled ? 0.5 : pressed ? 0.9 : 1,
        },
        style,
      ]}>
      <View style={{ flex: iconRight ? 1 : 0, alignItems: 'center' }}>
        <Text style={[styles.label, { color: textColor }]}>{label}</Text>
      </View>
      
      {iconRight && (
        <View style={{ width: 44, height: 44, borderRadius: 22, backgroundColor: '#B8FF2D', justifyContent: 'center', alignItems: 'center' }}>
          <IconSymbol name={iconRight} size={24} color="#000000" />
        </View>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 58,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
  },
  label: {
    fontSize: 17,
    fontWeight: '800',
    letterSpacing: 0.2,
  },
});

