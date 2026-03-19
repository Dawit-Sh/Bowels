import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/ui/Card';
import { GradientCard } from '@/components/ui/GradientCard';
import { Screen } from '@/components/ui/Screen';
import {
  getDailyAggregates,
  getDailyAvgStoolType,
  getStoolTypeDistribution,
  listDailyHealth,
  listSessionsByDateKey,
} from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { addDaysDateKey, formatMmSs, toDateKeyLocal } from '@/utils/datetime';
import { SimpleBarChart } from '@/screens/charts/SimpleBarChart';

export function AnalyticsScreen() {
  const theme = useTheme();
  const to = toDateKeyLocal(new Date());
  const from = addDaysDateKey(to, -13);

  const [daily, setDaily] = React.useState<Awaited<ReturnType<typeof getDailyAggregates>>>([]);
  const [stool, setStool] = React.useState<Awaited<ReturnType<typeof getStoolTypeDistribution>>>([]);
  const [dailyStool, setDailyStool] = React.useState<Awaited<ReturnType<typeof getDailyAvgStoolType>>>([]);
  const [health, setHealth] = React.useState<Awaited<ReturnType<typeof listDailyHealth>>>([]);
  const [sessionsInRange, setSessionsInRange] = React.useState<Awaited<ReturnType<typeof listSessionsByDateKey>>>([]);

  React.useEffect(() => {
    (async () => {
      setDaily(await getDailyAggregates(from, to));
      setStool(await getStoolTypeDistribution(from, to));
      setDailyStool(await getDailyAvgStoolType(from, to));
      setHealth(await listDailyHealth(from, to));
      setSessionsInRange(await listSessionsByDateKey(from, to));
    })();
  }, [from, to]);

  const totalVisits = daily.reduce((a, b) => a + b.visits, 0);
  const totalSeconds = daily.reduce((a, b) => a + b.total_seconds, 0);
  const avg = totalVisits ? Math.round(totalSeconds / totalVisits) : 0;
  const longest = React.useMemo(() => {
    const s = sessionsInRange
      .filter((x) => typeof x.duration_seconds === 'number')
      .sort((a, b) => (b.duration_seconds ?? 0) - (a.duration_seconds ?? 0))[0];
    return s?.duration_seconds ?? 0;
  }, [sessionsInRange]);

  const longCount = React.useMemo(
    () => sessionsInRange.filter((x) => (x.duration_seconds ?? 0) >= 10 * 60).length,
    [sessionsInRange]
  );

  const spikeDays = React.useMemo(() => daily.filter((d) => d.visits >= 8).map((d) => d.date_key), [daily]);

  const healthByKey = React.useMemo(() => new Map(health.map((h) => [h.date_key, h])), [health]);

  const fiberBuckets = React.useMemo(() => {
    const buckets: Record<string, { sum: number; n: number }> = { low: { sum: 0, n: 0 }, medium: { sum: 0, n: 0 }, high: { sum: 0, n: 0 } };
    dailyStool.forEach((d) => {
      const fiber = healthByKey.get(d.date_key)?.fiber ?? null;
      if (!fiber || !(fiber in buckets)) return;
      buckets[fiber].sum += d.avg_stool_type;
      buckets[fiber].n += 1;
    });
    return buckets;
  }, [dailyStool, healthByKey]);

  const stressBuckets = React.useMemo(() => {
    const buckets: Record<string, { sum: number; n: number }> = { low: { sum: 0, n: 0 }, medium: { sum: 0, n: 0 }, high: { sum: 0, n: 0 } };
    daily.forEach((d) => {
      const stress = healthByKey.get(d.date_key)?.stress ?? null;
      if (!stress || !(stress in buckets)) return;
      buckets[stress].sum += d.visits;
      buckets[stress].n += 1;
    });
    return buckets;
  }, [daily, healthByKey]);

  return (
    <Screen>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Analytics</Text>
      <Text style={[styles.subtitle, { color: theme.colors.textSecondary }]}>
        Last 14 days ({from} → {to})
      </Text>

      <Card>
        <View style={styles.row}>
          <View style={styles.stat}>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>Visits</Text>
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>{totalVisits}</Text>
          </View>
          <View style={styles.stat}>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>Total time</Text>
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>{formatMmSs(totalSeconds)}</Text>
          </View>
          <View style={styles.stat}>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>Avg</Text>
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>{formatMmSs(avg)}</Text>
          </View>
        </View>
      </Card>

      <GradientCard tone="neutral" style={{ padding: 0 }}>
        <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Visits per day</Text>
        <SimpleBarChart
          data={daily.map((d) => ({ x: d.date_key.slice(5), y: d.visits }))}
          accent={theme.colors.accent}
          labelColor={theme.colors.textSecondary}
        />
      </GradientCard>

      <Card>
        <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Highlights</Text>
        <View style={{ gap: 8, marginTop: 10 }}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>Longest session</Text>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{formatMmSs(longest)}</Text>
          </View>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>10+ min sessions</Text>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{longCount}</Text>
          </View>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 10 }}>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>Spike days (8+ visits)</Text>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>
              {spikeDays.length ? spikeDays.length : '0'}
            </Text>
          </View>
        </View>
      </Card>

      <Card>
        <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Stool type distribution</Text>
        {stool.length ? (
          <View style={{ gap: 8, marginTop: 10 }}>
            {stool.slice(0, 7).map((s) => (
              <View key={s.stool_type} style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                <Text style={{ color: theme.colors.textPrimary, fontWeight: '800' }}>Type {s.stool_type}</Text>
                <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{s.count}</Text>
              </View>
            ))}
          </View>
        ) : (
          <Text style={{ marginTop: 10, color: theme.colors.textSecondary, fontWeight: '700' }}>
            Log bowel sessions to populate this chart.
          </Text>
        )}
      </Card>

      <Card>
        <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Correlations (simple)</Text>
        <View style={{ gap: 10, marginTop: 10 }}>
          <View style={{ gap: 6 }}>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>Fiber vs stool</Text>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
              Avg Bristol type by daily fiber rating (days with both logs):
            </Text>
            {(['low', 'medium', 'high'] as const).map((k) => {
              const b = fiberBuckets[k];
              const v = b.n ? (b.sum / b.n).toFixed(1) : '—';
              return (
                <View key={k} style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                  <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{k}</Text>
                  <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{v}</Text>
                </View>
              );
            })}
          </View>

          <View style={{ gap: 6 }}>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>Stress vs frequency</Text>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
              Avg visits/day by daily stress rating (days with both logs):
            </Text>
            {(['low', 'medium', 'high'] as const).map((k) => {
              const b = stressBuckets[k];
              const v = b.n ? (b.sum / b.n).toFixed(1) : '—';
              return (
                <View key={k} style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                  <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{k}</Text>
                  <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{v}</Text>
                </View>
              );
            })}
          </View>

          <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
            Correlation ≠ causation. These are lightweight on-device summaries.
          </Text>
        </View>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
  subtitle: { fontSize: 12, fontWeight: '700' },
  row: { flexDirection: 'row', gap: 12 },
  stat: { flex: 1, gap: 4 },
  statLabel: { fontSize: 12, fontWeight: '800', textTransform: 'uppercase' },
  statValue: { fontSize: 18, fontWeight: '900' },
  cardTitle: { fontSize: 16, fontWeight: '900' },
});
