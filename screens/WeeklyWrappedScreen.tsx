import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { IconButton } from '@/components/ui/IconButton';
import { GradientCard } from '@/components/ui/GradientCard';
import { Screen } from '@/components/ui/Screen';
import { useTheme } from '@/theme/ThemeProvider';
import { buildWeeklyWrapped } from '@/utils/wrapped';

export function WeeklyWrappedScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [wrapped, setWrapped] = React.useState<Awaited<ReturnType<typeof buildWeeklyWrapped>> | null>(null);

  React.useEffect(() => {
    (async () => setWrapped(await buildWeeklyWrapped(new Date())))();
  }, []);

  return (
    <Screen>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
        <IconButton icon="chevron.left" onPress={() => router.back()} />
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Weekly Wrapped</Text>
        <View style={{ width: 38 }} />
      </View>
      <Text style={[styles.subtitle, { color: theme.colors.textSecondary }]}>Here’s your weekly summary</Text>

      {wrapped ? (
        <View style={{ gap: 12 }}>
          {wrapped.cards.map((c) => (
            <GradientCard key={c.id} tone={c.id === 'total_visits' || c.id === 'total_time' ? 'accent' : 'neutral'} style={{ padding: 0 }}>
              <Text style={{ color: theme.colors.textSecondary, fontWeight: '900', textTransform: 'uppercase' }}>
                {c.kicker}
              </Text>
              <Text style={{ marginTop: 8, color: theme.colors.textPrimary, fontWeight: '900', fontSize: 22 }}>
                {c.title}
              </Text>
              <Text style={{ marginTop: 8, color: theme.colors.textSecondary, fontWeight: '700' }}>{c.body}</Text>
            </GradientCard>
          ))}
          <Card style={{ borderRadius: theme.radius.xl }}>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 12 }}>
              <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', flex: 1 }}>
                Insight
              </Text>
              <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>Tip</Text>
            </View>
            <View style={{ marginTop: 10, padding: 14, borderRadius: 18, backgroundColor: theme.colors.accent }}>
              <Text style={{ color: '#fff', fontWeight: '900' }}>
                Your weekly report is ready — keep logging to improve accuracy.
              </Text>
            </View>
          </Card>
        </View>
      ) : (
        <Card>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>Generating…</Text>
        </Card>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
  subtitle: { fontSize: 12, fontWeight: '700' },
});
