import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { IconButton } from '@/components/ui/IconButton';
import { MetricTile } from '@/components/ui/MetricTile';
import { Screen } from '@/components/ui/Screen';
import { SegmentedControl } from '@/components/ui/SegmentedControl';
import { SwipeSlider } from '@/components/ui/SwipeSlider';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { GutHero } from '@/components/illustrations/GutHero';
import { getActiveSession, getDailyAggregates, getDailyHealth, upsertDailyHealth } from '@/db/queries';
import { getDb } from '@/db/client';
import { useSettingsStore } from '@/store/settings';
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
    <Screen contentStyle={{ paddingBottom: 120 }}>
      <View style={[styles.headerRow, { justifyContent: 'space-between', alignItems: 'center', paddingBottom: 8 }]}>
        <IconButton icon="bell" onPress={() => router.push('/insights')} />
        <Text style={[styles.headline, { color: theme.colors.textPrimary, textAlign: 'center' }]}>Bowels</Text>
        <IconButton icon="person.circle" onPress={() => router.push('/settings')} />
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
        <Card style={{ padding: 24, borderRadius: 32, backgroundColor: '#4C7CFF', overflow: 'hidden' }}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', zIndex: 10 }}>
            <View style={{ flex: 1 }}>
              <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13, fontWeight: '900', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                Quick Access
              </Text>
              <Text style={{ color: '#fff', fontSize: 26, fontWeight: '900', marginTop: 4, letterSpacing: -0.5 }}>
                {activeSessionId ? 'Resume Tracking' : 'Start Session'}
              </Text>
              
              <View style={{ flexDirection: 'row', marginTop: 16, gap: 16 }}>
                 <View>
                   <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13, fontWeight: '700' }}>Today</Text>
                   <Text style={{ color: '#fff', fontSize: 18, fontWeight: '900' }}>{todayVisits} logs</Text>
                 </View>
                 <View>
                   <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13, fontWeight: '700' }}>Week Avg</Text>
                   <Text style={{ color: '#fff', fontSize: 18, fontWeight: '900' }}>{formatMmSs(weekAvg)}</Text>
                 </View>
              </View>
            </View>
            
            <View style={{ width: 44, height: 44, borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.2)', justifyContent: 'center', alignItems: 'center' }}>
              <IconSymbol name="bolt.fill" size={24} color="#fff" />
            </View>
          </View>

          <View style={{ marginTop: 24, zIndex: 10 }}>
            <SwipeSlider
              isActive={!!activeSessionId}
              onSwipeSuccess={async () => {
                if (activeSessionId) {
                  router.push({ pathname: '/session/active', params: { sessionId: String(activeSessionId) } });
                } else {
                  const settings = useSettingsStore.getState();
                  if (!settings.hasRealData) {
                    const db = await getDb();
                    await db.runAsync('DELETE FROM sessions;');
                    await db.runAsync('DELETE FROM daily_health;');
                    await settings.setHasRealData(true);
                  }
                  router.push('/session/active');
                }
              }}
            />
          </View>
          
          <GutHero style={{ position: 'absolute', opacity: 0.15, right: -50, top: -20, width: 250, height: 250 }} />
        </Card>
      </View>

      <Card style={{ borderRadius: 32 }}>
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

      <View style={{ flexDirection: 'row', gap: 12, paddingBottom: 100 }}>
        <Card style={{ flex: 1, borderRadius: 32 }}>
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
        <Card style={{ flex: 1, borderRadius: 32, justifyContent: 'space-between' }}>
          <View>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '900', textTransform: 'uppercase' }}>
              Trends
            </Text>
            <Text style={{ marginTop: 6, color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>
              Analytics
            </Text>
          </View>
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
