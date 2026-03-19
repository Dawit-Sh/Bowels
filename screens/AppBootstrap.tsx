import * as SplashScreen from 'expo-splash-screen';
import { useRouter, useSegments } from 'expo-router';
import React from 'react';
import { ActivityIndicator, AppState, Text, View } from 'react-native';

import { initDb } from '@/db/client';
import { getActiveSession } from '@/db/queries';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { useSettingsStore } from '@/store/settings';
import { ensureNotificationSetup } from '@/utils/notifications';
import { dismissSessionNotification, maybeShowSessionNotification, registerSessionNotificationRouter } from '@/utils/sessionNotifications';
import { useTheme } from '@/theme/ThemeProvider';

export function AppBootstrap({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const segments = useSegments();

  const hydrate = useSettingsStore((s) => s.hydrate);
  const hydrated = useSettingsStore((s) => s.hydrated);
  const hasOnboarded = useSettingsStore((s) => s.hasOnboarded);
  const notificationsEnabled = useSettingsStore((s) => s.notificationsEnabled);
  const dailyReminderTime = useSettingsStore((s) => s.dailyReminderTime);
  const weeklyReportEnabled = useSettingsStore((s) => s.weeklyReportEnabled);

  const [ready, setReady] = React.useState(false);
  const [bootError, setBootError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        // Keep boot fast: only the essentials should block the first render.
        await withTimeout(initDb(), 8000);
        await withTimeout(hydrate(), 8000);
      } catch (e) {
        setBootError(e instanceof Error ? e.message : 'Boot failed');
      } finally {
        if (!cancelled) {
          setReady(true);
          SplashScreen.hideAsync().catch(() => {});
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [hydrate]);

  React.useEffect(() => {
    if (!ready || !hydrated) return;
    // Defer notifications so permission prompts / native setup never block launch.
    ensureNotificationSetup({
      enabled: notificationsEnabled,
      dailyReminderTime,
      weeklyReportEnabled,
    }).catch(() => {});

    registerSessionNotificationRouter(router);
  }, [dailyReminderTime, hydrated, notificationsEnabled, ready, router, weeklyReportEnabled]);

  React.useEffect(() => {
    if (!ready || !hydrated) return;

    const top = segments[0] ?? '';
    if (!hasOnboarded && top !== 'onboarding') {
      router.replace('/onboarding');
      return;
    }
  }, [hasOnboarded, hydrated, ready, router, segments]);

  React.useEffect(() => {
    if (!ready) return;
    let mounted = true;
    const sub = AppState.addEventListener('change', async (next) => {
      if (!mounted) return;
      if (next === 'background') {
        const active = await getActiveSession();
        if (active?.id && active.start_time) {
          await maybeShowSessionNotification({ sessionId: active.id, startTimeIso: active.start_time });
        }
      }
      if (next === 'active') {
        await dismissSessionNotification();
      }
    });
    return () => {
      mounted = false;
      sub.remove();
    };
  }, [ready]);

  if (!ready) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator />
      </View>
    );
  }

  if (bootError) {
    return <BootErrorScreen message={bootError} />;
  }

  return <>{children}</>;
}

function BootErrorScreen({ message }: { message: string }) {
  const theme = useTheme();
  return (
    <Screen scroll={false} contentStyle={{ justifyContent: 'center' }}>
      <Card style={{ borderRadius: 20 }}>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>Startup issue</Text>
        <Text style={{ marginTop: 8, color: theme.colors.textSecondary, fontWeight: '700' }}>
          The app couldn’t finish booting. This is usually caused by permissions or a database init error.
        </Text>
        <Text style={{ marginTop: 10, color: theme.colors.textSecondary, fontWeight: '700' }}>{message}</Text>
        <View style={{ marginTop: 14 }}>
          <Button label="Try again" onPress={() => {}} />
        </View>
      </Card>
    </Screen>
  );
}

async function withTimeout<T>(p: Promise<T>, ms: number): Promise<T> {
  let t: ReturnType<typeof setTimeout> | null = null;
  const timeout = new Promise<never>((_, rej) => {
    t = setTimeout(() => rej(new Error('Startup timed out')), ms);
  });
  try {
    return await Promise.race([p, timeout]);
  } finally {
    if (t) clearTimeout(t);
  }
}
