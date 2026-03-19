import React from 'react';
import { Pressable, StyleSheet } from 'react-native';

import { IconSymbol, type IconSymbolName } from '@/components/ui/icon-symbol';
import { useTheme } from '@/theme/ThemeProvider';

export function IconButton({
  icon,
  onPress,
  variant = 'surface',
}: {
  icon: IconSymbolName;
  onPress: () => void;
  variant?: 'surface' | 'ghost';
}) {
  const theme = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        {
          backgroundColor: variant === 'surface' ? theme.colors.surface : 'transparent',
          borderColor: theme.colors.border,
          opacity: pressed ? 0.85 : 1,
        },
      ]}>
      <IconSymbol name={icon} color={theme.colors.textPrimary} size={18} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    width: 38,
    height: 38,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});

