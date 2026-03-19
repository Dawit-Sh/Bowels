import React from 'react';
import { useRouter } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import Constants from 'expo-constants';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { SegmentedControl } from '@/components/ui/SegmentedControl';
import { useSettingsStore } from '@/store/settings';
import { ACCENTS } from '@/theme/createTheme';
import { useTheme } from '@/theme/ThemeProvider';
import { exportData, importData } from '@/utils/export';
import { ensureNotificationSetup } from '@/utils/notifications';
import { generateDummyData } from '@/utils/dummyData';
import { checkForUpdates } from '@/utils/updater';
import { Alert } from 'react-native';

export function SettingsScreen() {
  const theme = useTheme();
  const router = useRouter();
  const isExpoGo = Constants.appOwnership === 'expo';

  const mode = useSettingsStore((s) => s.mode);
  const accent = useSettingsStore((s) => s.accent);
  const notificationsEnabled = useSettingsStore((s) => s.notificationsEnabled);
  const dailyReminderTime = useSettingsStore((s) => s.dailyReminderTime);
  const weeklyReportEnabled = useSettingsStore((s) => s.weeklyReportEnabled);

  const setMode = useSettingsStore((s) => s.setMode);
  const setAccent = useSettingsStore((s) => s.setAccent);
  const setNotificationsEnabled = useSettingsStore((s) => s.setNotificationsEnabled);
  const setDailyReminderTime = useSettingsStore((s) => s.setDailyReminderTime);
  const setWeeklyReportEnabled = useSettingsStore((s) => s.setWeeklyReportEnabled);

  const rescheduleNotifications = async (patch: Partial<{ enabled: boolean; time: string; weekly: boolean }>) => {
    const enabled = patch.enabled ?? notificationsEnabled;
    const time = patch.time ?? dailyReminderTime;
    const weekly = patch.weekly ?? weeklyReportEnabled;
    await ensureNotificationSetup({ enabled, dailyReminderTime: time, weeklyReportEnabled: weekly });
  };

  return (
    <Screen contentStyle={{ alignItems: 'center' }}>
      <View style={styles.container}>
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Settings</Text>

        <Card style={{ borderRadius: theme.radius.xl }}>
          <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Theme</Text>
          <View style={{ gap: 12, marginTop: 12 }}>
            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Mode</Text>
            <SegmentedControl
              options={[
                { key: 'light', label: 'Light' },
                { key: 'dark', label: 'Dark' },
              ]}
              value={mode}
              onChange={(k) => setMode(k as any)}
              style={{ alignSelf: 'stretch' }}
            />

            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Accent</Text>
            <View style={{ gap: 10 }}>
              {ACCENTS.map((a) => (
                <SegmentedControl
                  key={a}
                  options={[{ key: a, label: a[0].toUpperCase() + a.slice(1) }]}
                  value={accent === a ? a : null}
                  onChange={() => setAccent(a as any)}
                  style={{ alignSelf: 'stretch' }}
                />
              ))}
            </View>
          </View>
        </Card>

        <Card style={{ borderRadius: theme.radius.xl }}>
          <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Notifications</Text>
          <View style={{ gap: 12, marginTop: 12 }}>
            {isExpoGo ? (
              <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
                Notifications require a development build (Expo Go on Android disables parts of `expo-notifications`).
              </Text>
            ) : null}

            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Enabled</Text>
            <SegmentedControl
              options={[
                { key: 'on', label: 'On' },
                { key: 'off', label: 'Off' },
              ]}
              value={notificationsEnabled ? 'on' : 'off'}
              onChange={async (k) => {
                const enabled = k === 'on';
                await setNotificationsEnabled(enabled);
                await rescheduleNotifications({ enabled });
              }}
              style={{ alignSelf: 'stretch' }}
            />

            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Daily reminder</Text>
            <View style={{ gap: 10 }}>
              {['08:00', '12:00', '18:00', '20:00'].map((t) => (
                <SegmentedControl
                  key={t}
                  options={[{ key: t, label: t }]}
                  value={dailyReminderTime === t ? t : null}
                  onChange={async () => {
                    await setDailyReminderTime(t);
                    await rescheduleNotifications({ time: t });
                  }}
                  style={{ alignSelf: 'stretch' }}
                />
              ))}
            </View>

            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>Weekly report</Text>
            <SegmentedControl
              options={[
                { key: 'on', label: 'On' },
                { key: 'off', label: 'Off' },
              ]}
              value={weeklyReportEnabled ? 'on' : 'off'}
              onChange={async (k) => {
                const weekly = k === 'on';
                await setWeeklyReportEnabled(weekly);
                await rescheduleNotifications({ weekly });
              }}
              style={{ alignSelf: 'stretch' }}
            />
          </View>
        </Card>

        <Card style={{ borderRadius: theme.radius.xl }}>
          <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Privacy</Text>
          <View style={{ gap: 10, marginTop: 12 }}>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>
              Data stays on-device unless you export it.
            </Text>
            <Button label="Export JSON" onPress={() => exportData('json')} variant="secondary" />
            <Button label="Export CSV" onPress={() => exportData('csv')} variant="secondary" />
            <Button label="Restore JSON" onPress={() => importData()} variant="secondary" />
          </View>
        </Card>

        <Card style={{ borderRadius: theme.radius.xl }}>
          <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>Developer Tools</Text>
          <View style={{ gap: 10, marginTop: 12 }}>
            <Button label="Generate 90 Days Dummy Data" onPress={async () => {
              try {
                await generateDummyData();
                Alert.alert('Success', '90 Days of dummy data randomly generated.');
              } catch (e: any) {
                Alert.alert('Error', e.message);
              }
            }} variant="secondary" />
          </View>
        </Card>

        <Card style={{ borderRadius: theme.radius.xl }}>
          <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>About</Text>
          <View style={{ gap: 10, marginTop: 12 }}>
            <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>Version 1.0.0</Text>
            <Button label="Check for Updates" onPress={() => checkForUpdates()} variant="secondary" />
          </View>
        </Card>

        <Card style={{ borderRadius: theme.radius.xl }}>
          <Button label="Health Info" onPress={() => router.push('/health-info')} variant="secondary" />
        </Card>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
  cardTitle: { fontSize: 16, fontWeight: '900' },
  label: { fontSize: 12, fontWeight: '800', textTransform: 'uppercase' },
  container: { width: '100%', maxWidth: 420, gap: 12 },
});
