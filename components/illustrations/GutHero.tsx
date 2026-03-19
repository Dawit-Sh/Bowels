import React from 'react';
import { View, type ViewStyle } from 'react-native';
import Svg, { Defs, LinearGradient, Path, Stop } from 'react-native-svg';

import { useTheme } from '@/theme/ThemeProvider';

export function GutHero({ style }: { style?: ViewStyle }) {
  const theme = useTheme();

  const base = theme.mode === 'dark' ? '#FF7A2F' : '#FF5A1F';
  const deep = theme.mode === 'dark' ? '#C93510' : '#D93A12';
  const highlight = theme.mode === 'dark' ? '#FFD2B9' : '#FFE2D3';

  return (
    <View style={style} pointerEvents="none">
      <Svg width="100%" height="100%" viewBox="0 0 420 520">
        <Defs>
          <LinearGradient id="g1" x1="0" y1="0" x2="1" y2="1">
            <Stop offset="0" stopColor={highlight} stopOpacity="1" />
            <Stop offset="0.3" stopColor={base} stopOpacity="1" />
            <Stop offset="1" stopColor={deep} stopOpacity="1" />
          </LinearGradient>
          <LinearGradient id="g2" x1="0" y1="1" x2="1" y2="0">
            <Stop offset="0" stopColor="#FFFFFF" stopOpacity="0.55" />
            <Stop offset="1" stopColor="#FFFFFF" stopOpacity="0" />
          </LinearGradient>
        </Defs>

        {/* A stylized, original “3D-ish” gut coil (not a medical asset) */}
        <Path
          d="M112 98c40-36 98-44 146-20 18 9 33 22 44 38 20 28 22 64 2 92-14 19-36 30-58 34 28 16 49 44 49 77 0 48-42 86-94 86-23 0-46-7-63-19-12 32-44 55-82 55-49 0-90-38-90-86 0-36 22-67 54-80-28-15-47-44-47-77 0-49 42-86 94-86 15 0 30 3 45 10z"
          fill="url(#g1)"
          opacity={0.98}
        />

        {/* Inner folds */}
        <Path
          d="M140 142c28-18 64-20 92-5 10 6 18 14 23 23 9 15 8 33-3 47-12 17-33 24-53 20-14-3-26-10-39-17-19-10-39-16-61-6-16 7-27 22-27 40 0 21 14 40 35 48 12 5 26 6 39 5 15-2 26 10 25 24-1 12-10 21-22 22-26 3-52-2-74-14-33-18-54-52-54-90 0-46 32-85 76-95 14-3 28-4 43-2z"
          fill={deep}
          opacity={0.22}
        />

        {/* Specular highlight */}
        <Path
          d="M170 115c-26 4-50 16-64 34-6 7-15 9-23 4-8-6-10-17-4-25 21-26 55-44 92-50 10-1 19 6 20 16 1 10-6 19-16 21z"
          fill="url(#g2)"
          opacity={0.9}
        />

        {/* Top “stem” detail */}
        <Path
          d="M250 64c10-10 28-9 37 2 8 9 7 23-2 32l-26 24c-9 8-24 8-32-1-9-10-8-25 2-33l23-24z"
          fill="url(#g1)"
          opacity={0.98}
        />
      </Svg>
    </View>
  );
}

