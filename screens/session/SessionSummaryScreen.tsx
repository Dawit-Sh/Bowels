import { useLocalSearchParams, useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { getSession, getSessionAnswers } from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { formatMmSs } from '@/utils/datetime';

export function SessionSummaryScreen() {
  const theme = useTheme();
  const router = useRouter();
  const params = useLocalSearchParams<{ sessionId?: string }>();
  const sessionId = Number(params.sessionId ?? '0');

  const [session, setSession] = React.useState<Awaited<ReturnType<typeof getSession>>>(null);
  const [answers, setAnswers] = React.useState<Awaited<ReturnType<typeof getSessionAnswers>>>([]);

  React.useEffect(() => {
    (async () => {
      setSession(await getSession(sessionId));
      setAnswers(await getSessionAnswers(sessionId));
    })();
  }, [sessionId]);

  if (!session) {
    return (
      <Screen>
        <Card>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>Session not found.</Text>
        </Card>
      </Screen>
    );
  }

  const when = new Date(session.start_time).toLocaleString();

  return (
    <Screen>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Session summary</Text>

      <Card>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 12 }}>
          <View style={{ flex: 1, gap: 6 }}>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>
              {session.type ? (session.type === 'bowel' ? 'Bowel movement' : 'Urine') : 'Session'}
            </Text>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>{when}</Text>
          </View>
          <View style={{ alignItems: 'flex-end', gap: 6 }}>
            <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>
              {formatMmSs(session.duration_seconds ?? 0)}
            </Text>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>{session.date_key}</Text>
          </View>
        </View>
      </Card>

      <Card>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>Answers</Text>
        <View style={{ gap: 8, marginTop: 10 }}>
          {answers.map((a) => (
            <View key={a.id} style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 10 }}>
              <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{a.question_key}</Text>
              <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>{a.value}</Text>
            </View>
          ))}
          {answers.length === 0 ? (
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>No answers recorded.</Text>
          ) : null}
        </View>
      </Card>

      <Button label="Done" onPress={() => router.replace('/(tabs)')} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
});

