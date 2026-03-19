import { useLocalSearchParams, useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { Selector } from '@/components/ui/Selector';
import { getSessionAnswers, setSessionType, upsertSessionAnswer } from '@/db/queries';
import type { SessionType } from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { buildQuestionFlow } from '@/utils/questions';

export function PostSessionQuestionsScreen() {
  const theme = useTheme();
  const router = useRouter();
  const params = useLocalSearchParams<{ sessionId?: string }>();
  const sessionId = Number(params.sessionId ?? '0');

  const [answers, setAnswers] = React.useState<Record<string, string>>({});
  const [sessionType, setType] = React.useState<SessionType | null>(null);
  const [index, setIndex] = React.useState(0);

  React.useEffect(() => {
    (async () => {
      const rows = await getSessionAnswers(sessionId);
      const map: Record<string, string> = {};
      rows.forEach((r) => (map[r.question_key] = r.value));
      setAnswers(map);
      const t = map.session_type as SessionType | undefined;
      if (t === 'urine' || t === 'bowel') setType(t);
    })();
  }, [sessionId]);

  const flow = React.useMemo(() => buildQuestionFlow(sessionType, sessionId || 1), [sessionId, sessionType]);
  const q = flow[index];
  const progress = `${Math.min(index + 1, flow.length)}/${flow.length}`;

  const onSelect = async (key: string) => {
    if (!q) return;
    const nextAnswers = { ...answers, [q.key]: key };
    setAnswers(nextAnswers);
    await upsertSessionAnswer(sessionId, q.key, key);

    if (q.key === 'session_type') {
      const t = key as SessionType;
      setType(t);
      await setSessionType(sessionId, t);
      setIndex(1);
      return;
    }

    const nextIndex = index + 1;
    if (nextIndex >= flow.length) {
      router.replace({ pathname: '/session/summary', params: { sessionId: String(sessionId) } });
      return;
    }
    setIndex(nextIndex);
  };

  if (!q) return null;

  return (
    <Screen>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Quick questions</Text>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '800' }}>{progress}</Text>
      </View>

      <Card>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>{q.title}</Text>
        {q.subtitle ? (
          <Text style={{ marginTop: 6, color: theme.colors.textSecondary, fontWeight: '700' }}>{q.subtitle}</Text>
        ) : null}
        <View style={{ marginTop: 12 }}>
          <Selector options={q.options} value={answers[q.key] ?? null} onChange={onSelect} columns={q.columns ?? 2} />
        </View>
      </Card>

      <Card>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
          Tap-only inputs. Max 5 questions per session.
        </Text>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
});

