import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { getDailyAggregates } from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { formatMmSs, toDateKeyLocal } from '@/utils/datetime';

export default function YearlyWrappedScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [loading, setLoading] = React.useState(true);
  const [stats, setStats] = React.useState({ totalDays: 0, totalVisits: 0, totalSeconds: 0 });

  React.useEffect(() => {
    (async () => {
      const now = new Date();
      const startOfYear = new Date(now.getFullYear(), 0, 1);
      const agg = await getDailyAggregates(toDateKeyLocal(startOfYear), toDateKeyLocal(now));
      
      const totalDays = agg.filter(d => d.visits > 0).length;
      const totalVisits = agg.reduce((s, a) => s + a.visits, 0);
      const totalSeconds = agg.reduce((s, a) => s + a.total_seconds, 0);
      
      setStats({ totalDays, totalVisits, totalSeconds });
      setLoading(false);
    })();
  }, []);

  if (loading) return <Screen><Text>Loading...</Text></Screen>;

  return (
    <Screen contentStyle={{ justifyContent: 'space-between', paddingVertical: 24 }}>
      <View style={{ alignItems: 'center', gap: 8, marginTop: 40 }}>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '900', textTransform: 'uppercase', letterSpacing: 2 }}>
          {new Date().getFullYear()}
        </Text>
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Yearly Wrapped</Text>
      </View>

      <View style={{ gap: 16 }}>
        <Card style={styles.card}>
          <Text style={styles.bigNum}>{stats.totalVisits}</Text>
          <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Total Visits</Text>
        </Card>
        
        <View style={{ flexDirection: 'row', gap: 16 }}>
          <Card style={[styles.card, { flex: 1 }]}>
            <Text style={styles.midNum}>{stats.totalDays}</Text>
            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Active Days</Text>
          </Card>
          <Card style={[styles.card, { flex: 1 }]}>
            <Text style={styles.midNum}>{formatMmSs(stats.totalSeconds)}</Text>
            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Time Spent</Text>
          </Card>
        </View>
      </View>

      <View style={{ gap: 12 }}>
          <Button label="Share Your Wrapped" variant="secondary" onPress={() => {}} />
          <Button label="Close" onPress={() => router.back()} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 36, fontWeight: '900', textAlign: 'center' },
  card: { alignItems: 'center', padding: 24 },
  bigNum: { fontSize: 64, fontWeight: '900', color: '#ff6b00', letterSpacing: -2 },
  midNum: { fontSize: 32, fontWeight: '900', color: '#ff6b00' },
  label: { fontSize: 13, fontWeight: '800', marginTop: 4, textTransform: 'uppercase' },
});
