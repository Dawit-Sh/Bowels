import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Image } from 'expo-image';

import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';

import { Button } from '@/components/ui/Button';
import { Screen } from '@/components/ui/Screen';
import { useSettingsStore } from '@/store/settings';
import { useTheme } from '@/theme/ThemeProvider';

const SLIDES = [
  {
    title: 'Track sessions, privately',
    body: 'Start a timer when you sit down, then answer quick tap-only questions. Everything is stored locally.',
  },
  {
    title: 'Find patterns',
    body: 'See trends in frequency, time spent, stool types, and daily habits like fiber, stress, and sleep.',
  },
  {
    title: 'Weekly wrapped',
    body: 'A clean weekly summary with simple insights — computed on-device.',
  },
];

export function OnboardingScreen() {
  const theme = useTheme();
  const router = useRouter();
  const setHasOnboarded = useSettingsStore((s) => s.setHasOnboarded);
  const [i, setI] = React.useState(0);

  const slide = SLIDES[i];
  const isLast = i === SLIDES.length - 1;

  const insets = useSafeAreaInsets();

  return (
    <Screen scroll={false} contentStyle={{ flex: 1, paddingBottom: insets.bottom || 24, paddingHorizontal: 20 }}>
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', gap: 32 }}>
        <View style={{ alignItems: 'center', gap: 12 }}>
          <Image source={require('@/assets/images/logo.png')} style={{ width: 96, height: 96 }} />
          <Text style={[styles.brand, { color: theme.colors.textPrimary }]}>Bowels</Text>
        </View>

        <View style={[styles.block, { width: '100%' }]}>
          <BlurView intensity={theme.mode === 'dark' ? 30 : 60} tint={theme.mode === 'dark' ? 'dark' : 'light'} style={{ borderRadius: theme.radius.xl, overflow: 'hidden' }}>
            <View style={{ padding: 24, backgroundColor: theme.mode === 'dark' ? 'rgba(255,255,255,0.05)' : 'rgba(255,255,255,0.6)' }}>
              <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 24, textAlign: 'center' }}>{slide.title}</Text>
              <Text style={{ marginTop: 12, color: theme.colors.textSecondary, fontWeight: '700', textAlign: 'center', fontSize: 16 }}>{slide.body}</Text>
            </View>
          </BlurView>
        </View>
      </View>

      <View style={{ width: '100%', maxWidth: 420, alignSelf: 'center', gap: 16, marginBottom: 16 }}>
        <View style={{ flexDirection: 'row', gap: 12, width: '100%' }}>
          {i > 0 && (
            <Button label="Back" variant="secondary" onPress={() => setI((v) => Math.max(0, v - 1))} style={{ flex: 1, height: 50 }} />
          )}
          <Button
            label={isLast ? 'Get started' : 'Next'}
            onPress={async () => {
              if (!isLast) {
                setI((v) => Math.min(SLIDES.length - 1, v + 1));
                return;
              }
              await setHasOnboarded(true);
              router.replace('/(tabs)');
            }}
            style={{ flex: 1, height: 50 }}
          />
        </View>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '700', textAlign: 'center', fontSize: 12 }}>
          Privacy-first: no accounts, no cloud by default.
        </Text>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  brand: { fontSize: 36, fontWeight: '900', textAlign: 'center', letterSpacing: -0.5 },
  block: { maxWidth: 420 },
});
