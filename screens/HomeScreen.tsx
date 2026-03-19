import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { IconButton } from '@/components/ui/IconButton';
import { MetricTile } from '@/components/ui/MetricTile';
import { Screen } from '@/components/ui/Screen';
import { SegmentedControl } from '@/components/ui/SegmentedControl';
import { GutHero } from '@/components/illustrations/GutHero';
import { getActiveSession, getDailyAggregates, getDailyHealth, upsertDailyHealth } from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { fetchDailyTip } from '@/utils/tipsApi';
import { formatMmSs, toDateKeyLocal, weekRange } from '@/utils/datetime';

const tri = [
  { key: 'low', label: 'Low' },
  { key: 'medium', label: 'Medium' },
  { key: 'high', label: 'High' },
];

export function HomeScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [scope, setScope] = React.useState<'daily' | 'weekly'>('weekly');
  const [tip, setTip] = React.useState<string | null>(null);

  const todayKey = React.useMemo(() => toDateKeyLocal(new Date()), []);
  const { from, to } = React.useMemo(() => weekRange(new Date()), []);

  const [todayVisits, setTodayVisits] = React.useState(0);
  const [weekTotal, setWeekTotal] = React.useState(0);
  const [weekAvg, setWeekAvg] = React.useState(0);
  const [activeSessionId, setActiveSessionId] = React.useState<number | null>(null);

  const [daily, setDaily] = React.useState<{
    water: string | null;
    fiber: string | null;
    meals: string | null;
    stress: string | null;
    sleep: string | null;
    exercise: string | null;
  }>({ water: null, fiber: null, meals: null, stress: null, sleep: null, exercise: null });

  const refresh = React.useCallback(async () => {
    const [todayAgg] = await getDailyAggregates(todayKey, todayKey);
    const weekAgg = await getDailyAggregates(from, to);
    setTodayVisits(todayAgg?.visits ?? 0);
    setWeekTotal(weekAgg.reduce((acc, d) => acc + d.total_seconds, 0));
    const weightedAvg =
      weekAgg.reduce((acc, d) => acc + (d.visits ? d.avg_seconds * d.visits : 0), 0) /
      Math.max(1, weekAgg.reduce((acc, d) => acc + d.visits, 0));
    setWeekAvg(Math.round(weightedAvg));

    const health = await getDailyHealth(todayKey);
    setDaily({
      water: health?.water ?? null,
      fiber: health?.fiber ?? null,
      meals: health?.meals ?? null,
      stress: health?.stress ?? null,
      sleep: health?.sleep ?? null,
      exercise: health?.exercise ?? null,
    });

    const active = await getActiveSession();
    setActiveSessionId(active?.id ?? null);
  }, [from, to, todayKey]);

  React.useEffect(() => {
    refresh();
    fetchDailyTip().then(setTip);
  }, [refresh]);

  const updateDaily = async (patch: Partial<typeof daily>) => {
    const next = { ...daily, ...patch };
    setDaily(next);
    await upsertDailyHealth({
      date_key: todayKey,
      water: next.water,
      fiber: next.fiber,
      meals: next.meals,
      stress: next.stress,
      sleep: next.sleep,
      exercise: next.exercise,
    });
  };

  return (
    <Screen>
      <View style={styles.headerRow}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.hi, { color: theme.colors.textSecondary }]}>Hello</Text>
          <Text style={[styles.headline, { color: theme.colors.textPrimary }]}>Bowels</Text>
        </View>
        <View style={{ flexDirection: 'row', gap: 10 }}>
          <IconButton icon="person.circle" onPress={() => router.push('/(tabs)/settings')} />
          <IconButton icon="bell" onPress={() => router.push('/(tabs)/insights')} />
        </View>
      </View>

      <SegmentedControl
        options={[
          { key: 'daily', label: 'Daily' },
          { key: 'weekly', label: 'Weekly' },
        ]}
        value={scope}
        onChange={(k) => setScope(k as any)}
      />

      {tip && (
        <Card style={{ padding: 16, borderRadius: theme.radius.lg, backgroundColor: theme.colors.surface, marginTop: 12 }}>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '800', fontSize: 12, textTransform: 'uppercase', marginBottom: 4 }}>Tip of the day</Text>
          <Text style={{ color: theme.colors.textPrimary, fontWeight: '700', fontSize: 14 }}>{tip}</Text>
        </Card>
      )}

      <View style={styles.heroCard}>
        <Card style={{ padding: 18, borderRadius: theme.radius.xl }}>
          <View style={{ minHeight: 240 }}>
            <GutHero style={styles.gut} />

            <View style={{ gap: 10 }}>
              <Text style={[styles.kicker, { color: theme.colors.textSecondary }]}>
                {scope === 'weekly' ? 'Statistics' : 'Today'}
              </Text>
              <Text style={[styles.sub, { color: theme.colors.textSecondary }]}>
                {scope === 'weekly' ? 'Weekly overview' : 'Your day so far'}
              </Text>
            </View>

            <View style={{ marginTop: 14, flexDirection: 'row', gap: 12 }}>
              <MetricTile
                label={scope === 'weekly' ? 'Today visits' : 'Visits'}
                value={`${todayVisits}`}
                unit=""
                style={{ flex: 1 }}
              />
              <MetricTile
                label={scope === 'weekly' ? 'Total time' : 'Week total'}
                value={scope === 'weekly' ? formatMmSs(weekTotal) : formatMmSs(weekTotal)}
                hint={scope === 'weekly' ? `Avg ${formatMmSs(weekAvg)}` : `Avg ${formatMmSs(weekAvg)}`}
                style={{ flex: 1 }}
              />
            </View>

            <View style={{ marginTop: 14 }}>
              {activeSessionId ? (
                <View style={{ gap: 10 }}>
                  <Button
                    label="Resume Session"
                    onPress={() =>
                      router.push({ pathname: '/session/active', params: { sessionId: String(activeSessionId) } })
                    }
                  />
                  <Button label="Start New Session" variant="secondary" onPress={() => router.push('/session/active')} />
                </View>
              ) : (
                <Button label="Start Session" onPress={() => router.push('/session/active')} />
              )}
            </View>
          </View>
        </Card>
      </View>

      <Card style={{ borderRadius: theme.radius.xl }}>
        <View style={{ gap: 12 }}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Daily check-in</Text>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{todayKey}</Text>
          </View>

          <RowPick label="Water" value={daily.water} options={tri} onChange={(v) => updateDaily({ water: v })} />
          <RowPick label="Fiber" value={daily.fiber} options={tri} onChange={(v) => updateDaily({ fiber: v })} />
          <RowPick
            label="Meals"
            value={daily.meals}
            options={[
              { key: 'poor', label: 'Poor' },
              { key: 'average', label: 'Average' },
              { key: 'good', label: 'Good' },
            ]}
            onChange={(v) => updateDaily({ meals: v })}
          />
          <RowPick label="Stress" value={daily.stress} options={tri} onChange={(v) => updateDaily({ stress: v })} />
          <RowPick
            label="Sleep"
            value={daily.sleep}
            options={[
              { key: '<5', label: '<5h' },
              { key: '5-7', label: '5–7h' },
              { key: '7-9', label: '7–9h' },
              { key: '9+', label: '9h+' },
            ]}
            onChange={(v) => updateDaily({ sleep: v })}
          />
          <RowPick
            label="Exercise"
            value={daily.exercise}
            options={[
              { key: 'none', label: 'None' },
              { key: 'light', label: 'Light' },
              { key: 'moderate', label: 'Moderate' },
              { key: 'intense', label: 'Intense' },
            ]}
            onChange={(v) => updateDaily({ exercise: v })}
          />
        </View>
      </Card>

      <View style={{ flexDirection: 'row', gap: 12 }}>
        <Card style={{ flex: 1, borderRadius: theme.radius.xl }}>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '900', textTransform: 'uppercase' }}>
            Wrapped
          </Text>
          <Text style={{ marginTop: 6, color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>
            Summaries
          </Text>
          <View style={{ marginTop: 12, gap: 8 }}>
            <Button label="Weekly" onPress={() => router.push('/wrapped')} />
            <Button label="Yearly" variant="secondary" onPress={() => router.push('/wrapped-yearly')} />
          </View>
        </Card>
        <Card style={{ flex: 1, borderRadius: theme.radius.xl }}>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '900', textTransform: 'uppercase' }}>
            Trends
          </Text>
          <Text style={{ marginTop: 6, color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>
            Analytics
          </Text>
          <View style={{ marginTop: 12 }}>
            <Button label="Open" variant="secondary" onPress={() => router.push('/(tabs)/analytics')} />
          </View>
        </Card>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 12 },
  hi: { fontSize: 14, fontWeight: '800' },
  headline: { fontSize: 30, fontWeight: '900', letterSpacing: -0.9 },
  heroCard: { marginTop: 4 },
  gut: { position: 'absolute', right: -36, top: -10, width: 230, height: 230, opacity: 0.98 },
  kicker: { fontSize: 12, fontWeight: '900', textTransform: 'uppercase', letterSpacing: 0.7 },
  sub: { fontSize: 13, fontWeight: '700' },
  cardTitle: { fontSize: 16, fontWeight: '900' },
  questionLabel: { fontSize: 12, fontWeight: '800', textTransform: 'uppercase' },
});

function RowPick({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string | null;
  options: { key: string; label: string }[];
  onChange: (key: string) => void;
}) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
      <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{label}</Text>
      <SegmentedControl options={options} value={value} onChange={onChange} size="sm" />
    </View>
  );
}
