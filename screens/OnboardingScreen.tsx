import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Image } from 'expo-image';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
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

  return (
    <Screen scroll={false} contentStyle={{ justifyContent: 'center', gap: 14, alignItems: 'center' }}>
      <View style={{ alignItems: 'center', gap: 10 }}>
        <Image source={require('@/assets/images/logo.png')} style={{ width: 84, height: 84 }} />
        <Text style={[styles.brand, { color: theme.colors.textPrimary }]}>Bowels</Text>
      </View>

      <View style={[styles.block, { width: '100%' }]}>
        <Card style={{ padding: 18, borderRadius: theme.radius.xl }}>
          <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 22 }}>{slide.title}</Text>
          <Text style={{ marginTop: 10, color: theme.colors.textSecondary, fontWeight: '700' }}>{slide.body}</Text>
        </Card>
      </View>

      <View style={[styles.block, { width: '100%', flexDirection: 'row', gap: 10 }]}>
        {i > 0 ? (
          <Button label="Back" variant="secondary" onPress={() => setI((v) => Math.max(0, v - 1))} style={{ flex: 1 }} />
        ) : null}
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
          style={{ flex: 1 }}
        />
      </View>
      <Text style={{ color: theme.colors.textSecondary, fontWeight: '700', textAlign: 'center' }}>
        Privacy-first: no accounts, no cloud by default.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  brand: { fontSize: 34, fontWeight: '900', textAlign: 'center', letterSpacing: -0.4 },
  block: { maxWidth: 420 },
});
