import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { useTheme } from '@/theme/ThemeProvider';
import { computeInsights } from '@/utils/insights';

export function InsightsScreen() {
  const theme = useTheme();
  const [insights, setInsights] = React.useState<Awaited<ReturnType<typeof computeInsights>>>([]);

  React.useEffect(() => {
    (async () => setInsights(await computeInsights()))();
  }, []);

  return (
    <Screen>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Insights</Text>
      <Text style={[styles.subtitle, { color: theme.colors.textSecondary }]}>
        Rule-based highlights, computed on-device.
      </Text>

      {insights.map((i) => (
        <Card key={i.id}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 12 }}>
            <View style={{ flex: 1, gap: 6 }}>
              <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{i.title}</Text>
              <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>{i.body}</Text>
            </View>
            <View
              style={{
                alignSelf: 'flex-start',
                paddingHorizontal: 10,
                paddingVertical: 6,
                borderRadius: 999,
                backgroundColor:
                  i.severity === 'high'
                    ? theme.colors.danger
                    : i.severity === 'medium'
                      ? theme.colors.warning
                      : theme.colors.muted,
              }}>
              <Text style={{ color: i.severity === 'low' ? theme.colors.textPrimary : '#fff', fontWeight: '900' }}>
                {i.severity.toUpperCase()}
              </Text>
            </View>
          </View>
        </Card>
      ))}

      {insights.length === 0 ? (
        <Card>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
            No insights yet — log a few days to unlock patterns.
          </Text>
        </Card>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
  subtitle: { fontSize: 12, fontWeight: '700' },
});

