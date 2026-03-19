import * as Haptics from 'expo-haptics';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React from 'react';
import { Alert, AppState, StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { createSession, deleteSession, finishSession, getSession } from '@/db/queries';
import { useTheme } from '@/theme/ThemeProvider';
import { formatMmSs, isoNow } from '@/utils/datetime';
import { dismissSessionNotification, maybeShowSessionNotification } from '@/utils/sessionNotifications';

export function ActiveSessionScreen() {
  const theme = useTheme();
  const router = useRouter();
  const params = useLocalSearchParams<{ sessionId?: string }>();
  const resumeId = params.sessionId ? Number(params.sessionId) : null;

  const [sessionId, setSessionId] = React.useState<number | null>(null);
  const [startIso, setStartIso] = React.useState<string | null>(null);
  const [elapsed, setElapsed] = React.useState(0);

  const startRef = React.useRef<number | null>(null);
  const timerRef = React.useRef<ReturnType<typeof setInterval> | null>(null);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      if (resumeId) {
        const existing = await getSession(resumeId);
        if (!existing || existing.end_time) {
          // Fall back to a new session if the referenced one is missing/finished.
        } else {
          if (cancelled) return;
          setSessionId(existing.id);
          setStartIso(existing.start_time);
          startRef.current = new Date(existing.start_time).getTime();
          setElapsed(Math.floor((Date.now() - startRef.current) / 1000));
        }
      }

      if (!resumeId || !startRef.current) {
        const start = isoNow();
        const id = await createSession(start);
        if (cancelled) return;
        setSessionId(id);
        setStartIso(start);
        startRef.current = Date.now();
      }

      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => {});

      timerRef.current = setInterval(() => {
        if (!startRef.current) return;
        setElapsed(Math.floor((Date.now() - startRef.current) / 1000));
      }, 250);
    })();

    return () => {
      cancelled = true;
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [resumeId]);

  React.useEffect(() => {
    if (!sessionId || !startIso) return;
    let mounted = true;
    const sub = AppState.addEventListener('change', async (next) => {
      if (!mounted) return;
      if (next === 'background') {
        await maybeShowSessionNotification({ sessionId, startTimeIso: startIso });
      }
      if (next === 'active') {
        await dismissSessionNotification();
      }
    });
    return () => {
      mounted = false;
      sub.remove();
    };
  }, [sessionId, startIso]);

  const onCancel = () => {
    if (!sessionId) return router.back();
    Alert.alert('Cancel session?', 'This session will not be saved.', [
      { text: 'Keep', style: 'cancel' },
      {
        text: 'Cancel',
        style: 'destructive',
        onPress: async () => {
          await deleteSession(sessionId);
          await dismissSessionNotification();
          router.back();
        },
      },
    ]);
  };

  const onFinish = async () => {
    if (!sessionId || !startIso) return;
    const end = isoNow();
    const durationSeconds = Math.max(0, elapsed);
    await finishSession(sessionId, end, durationSeconds);
    await dismissSessionNotification();
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => {});
    router.replace({ pathname: '/session/questions', params: { sessionId: String(sessionId) } });
  };

  return (
    <Screen scroll={false} contentStyle={{ justifyContent: 'space-between', paddingVertical: 24, flex: 1 }}>
      <View style={{ gap: 6, alignItems: 'center' }}>
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Session timer</Text>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>Live tracking</Text>
      </View>

      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <Card style={[styles.heroCard, { backgroundColor: theme.colors.surface, shadowColor: theme.colors.textPrimary }]}>
          <Text style={[styles.heroTimer, { color: theme.colors.textPrimary }]}>
            {formatMmSs(elapsed)}
          </Text>
          <Text style={{ color: theme.colors.textSecondary, fontWeight: '700', fontSize: 16, marginTop: 8, textTransform: 'uppercase', letterSpacing: 2 }}>
            Elapsed
          </Text>
        </Card>
      </View>

      <View style={{ flexDirection: 'row', gap: 10, paddingHorizontal: 16 }}>
        <Button label="Cancel" variant="secondary" onPress={onCancel} style={{ flex: 1 }} />
        <Button label="Finish" onPress={onFinish} style={{ flex: 1 }} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900', textAlign: 'center' },
  heroCard: {
    padding: 40,
    borderRadius: 30,
    alignItems: 'center',
    justifyContent: 'center',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.1,
    shadowRadius: 20,
    elevation: 8,
    width: '100%',
    minWidth: 280,
    maxWidth: 340,
  },
  heroTimer: {
    fontSize: 76,
    fontWeight: '900',
    fontVariant: ['tabular-nums'],
    letterSpacing: -2,
  },
});
