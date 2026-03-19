import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { useTheme } from '@/theme/ThemeProvider';

export function Card({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  const theme = useTheme();
  return (
    <View
      style={[
        styles.base,
        {
          backgroundColor: theme.colors.surface,
          borderColor: theme.colors.border,
          borderRadius: theme.radius.lg,
          shadowColor: '#000',
          shadowOpacity: theme.mode === 'dark' ? 0.22 : 0.10,
          shadowRadius: 16,
          shadowOffset: { width: 0, height: 10 },
          elevation: 4,
        },
        style,
      ]}>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    padding: 16,
    borderWidth: 1,
  },
});
