import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { getDb } from '@/db/client';
import { useTheme } from '@/theme/ThemeProvider';

export default function PredictionsScreen() {
  const theme = useTheme();
  const [loading, setLoading] = React.useState(true);
  const [prediction, setPrediction] = React.useState<string | null>(null);

  React.useEffect(() => {
    (async () => {
      const db = await getDb();
      // Calculate average hour from past bowel sessions
      const rows = await db.getAllAsync<{ h: number, m: number }>(`
        SELECT 
          CAST(strftime('%H', start_time) AS INTEGER) as h,
          CAST(strftime('%M', start_time) AS INTEGER) as m
        FROM sessions
        WHERE type = 'bowel'
      `);
      
      if (rows.length < 5) {
        setPrediction('We need at least 5 bowel recorded sessions to form an accurate baseline. Keep tracking!');
      } else {
        const totalMins = rows.reduce((sum, r) => sum + r.h * 60 + r.m, 0);
        const avgMins = Math.round(totalMins / rows.length);
        const h = Math.floor(avgMins / 60);
        const m = avgMins % 60;
        
        const d = new Date();
        d.setHours(h, m, 0, 0);
        const timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        setPrediction(`Your circadian gut rhythm typically activates around ${timeStr}. Keep an eye out for natural signs!`);
      }
      setLoading(false);
    })();
  }, []);

  return (
    <Screen>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Predictions</Text>
      
      <Card style={{ marginTop: 24, borderRadius: theme.radius.xl, padding: 24, alignItems: 'center' }}>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 20 }}>The Oracle Says...</Text>
        <Text style={{ marginTop: 12, color: theme.colors.textSecondary, fontWeight: '700', fontSize: 16, lineHeight: 24, textAlign: 'center' }}>
          {loading ? 'Consulting the stars...' : prediction}
        </Text>
      </Card>
      
      <Card style={{ marginTop: 16, borderRadius: theme.radius.xl }}>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>Algorithm Context</Text>
        <Text style={{ marginTop: 8, color: theme.colors.textSecondary, fontWeight: '700' }}>
          This prediction identifies your typical time window by normalizing and averaging previous session timestamps globally. With more data points, the expected time frame narrows in accuracy.
        </Text>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 28, fontWeight: '900' },
});
