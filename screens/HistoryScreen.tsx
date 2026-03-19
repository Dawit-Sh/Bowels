import { useRouter } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { listSessions } from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { formatMmSs } from '@/utils/datetime';

export function HistoryScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [rows, setRows] = React.useState<Awaited<ReturnType<typeof listSessions>>>([]);

  React.useEffect(() => {
    (async () => setRows(await listSessions(200, 0)))();
  }, []);

  return (
    <Screen>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>History</Text>

      {rows.map((s) => {
        const when = new Date(s.start_time);
        return (
          <Pressable
            key={s.id}
            onPress={() => router.push({ pathname: '/session/summary', params: { sessionId: String(s.id) } })}
            style={({ pressed }) => [{ opacity: pressed ? 0.85 : 1 }]}>
            <Card style={{ marginBottom: 10 }}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 12 }}>
                <View style={{ flex: 1, gap: 4 }}>
                  <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 16 }}>
                    {s.type ? (s.type === 'bowel' ? 'Bowel movement' : 'Urine') : 'Session'}
                  </Text>
                  <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
                    {when.toLocaleString()}
                  </Text>
                </View>
                <View style={{ alignItems: 'flex-end', gap: 4 }}>
                  <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>
                    {formatMmSs(s.duration_seconds ?? 0)}
                  </Text>
                  <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>{s.date_key}</Text>
                </View>
              </View>
            </Card>
          </Pressable>
        );
      })}

      {rows.length === 0 ? (
        <Card>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
            No sessions yet. Start one from Home.
          </Text>
        </Card>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
});

