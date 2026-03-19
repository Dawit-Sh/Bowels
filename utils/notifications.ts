import Constants from 'expo-constants';

import { settingsGet, settingsSet } from '@/db/queries';

type NotifIds = {
  dailyId?: string;
  weeklyId?: string;
};

const NOTIF_IDS_KEY = 'notif_ids_v1';

type ExpoNotificationsModule = typeof import('expo-notifications');

function isExpoGo(): boolean {
  // appOwnership === 'expo' means Expo Go.
  return Constants.appOwnership === 'expo';
}

let _notif: Promise<ExpoNotificationsModule> | null = null;
async function getNotifications(): Promise<ExpoNotificationsModule> {
  if (_notif) return _notif;
  _notif = import('expo-notifications').then((Notifications) => {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldShowAlert: true,
        shouldShowBanner: true,
        shouldShowList: true,
        shouldPlaySound: false,
        shouldSetBadge: false,
      }),
    });
    return Notifications;
  });
  return _notif;
}

function parseHhMm(hhmm: string): { hour: number; minute: number } {
  const [h, m] = hhmm.split(':').map((x) => Number(x));
  return { hour: Number.isFinite(h) ? h : 20, minute: Number.isFinite(m) ? m : 0 };
}

export async function ensureNotificationSetup({
  enabled,
  dailyReminderTime,
  weeklyReportEnabled,
}: {
  enabled: boolean;
  dailyReminderTime: string;
  weeklyReportEnabled: boolean;
}): Promise<void> {
  // NOTE: On Android, Expo Go (SDK 53+) throws at module init due to removed push token support.
  // We skip notifications entirely in Expo Go so the app can run without a dev build.
  if (isExpoGo()) return;

  const Notifications = await getNotifications();
  const existing = (await settingsGet<NotifIds>(NOTIF_IDS_KEY)) ?? {};

  const cancelIf = async (id?: string) => {
    if (!id) return;
    try {
      await Notifications.cancelScheduledNotificationAsync(id);
    } catch {
      // ignore
    }
  };

  if (!enabled) {
    await cancelIf(existing.dailyId);
    await cancelIf(existing.weeklyId);
    await settingsSet(NOTIF_IDS_KEY, {});
    return;
  }

  const perm = await Notifications.getPermissionsAsync();
  if (perm.status !== 'granted') {
    const req = await Notifications.requestPermissionsAsync();
    if (req.status !== 'granted') return;
  }

  // Daily reminder
  await cancelIf(existing.dailyId);
  const { hour, minute } = parseHhMm(dailyReminderTime);
  const dailyId = await Notifications.scheduleNotificationAsync({
    content: { title: 'Bowels', body: 'Quick check-in: log your health and sessions.' },
    trigger: { type: Notifications.SchedulableTriggerInputTypes.CALENDAR, hour, minute, repeats: true } as any,
  });

  // Weekly report (Monday 09:00 by default)
  let weeklyId: string | undefined;
  await cancelIf(existing.weeklyId);
  if (weeklyReportEnabled) {
    weeklyId = await Notifications.scheduleNotificationAsync({
      content: { title: 'Weekly Wrapped', body: 'Your weekly report is ready.' },
      trigger: {
        type: Notifications.SchedulableTriggerInputTypes.CALENDAR,
        weekday: 2,
        hour: 9,
        minute: 0,
        repeats: true,
      } as any,
    });
  }

  await settingsSet(NOTIF_IDS_KEY, { dailyId, weeklyId });
}
