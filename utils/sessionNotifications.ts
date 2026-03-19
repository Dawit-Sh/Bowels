import type { Router } from 'expo-router';
import Constants from 'expo-constants';

import { deleteSession, finishSession, settingsGet, settingsSet } from '@/db/queries';
import { formatMmSs, isoNow } from '@/utils/datetime';

import * as TaskManager from 'expo-task-manager';

type ExpoNotificationsModule = typeof import('expo-notifications');

const IDS_KEY = 'session_notif_ids_v1';

type Stored = { activeId?: string };

function isExpoGo(): boolean {
  return Constants.appOwnership === 'expo';
}

let _notif: Promise<ExpoNotificationsModule> | null = null;
async function getNotifications(): Promise<ExpoNotificationsModule> {
  if (_notif) return _notif;
  _notif = import('expo-notifications').then((Notifications) => Notifications);
  return _notif;
}

async function ensureCategory(): Promise<void> {
  const Notifications = await getNotifications();
  await Notifications.setNotificationCategoryAsync('SESSION_ACTIVE', [
    { identifier: 'OPEN_SESSION', buttonTitle: 'Open', options: { opensAppToForeground: true } },
    { identifier: 'FINISH_SESSION', buttonTitle: 'Finish', options: { opensAppToForeground: true } },
    { identifier: 'CANCEL_SESSION', buttonTitle: 'Cancel', options: { opensAppToForeground: true } },
  ]);
}

export async function maybeShowSessionNotification({
  sessionId,
  startTimeIso,
}: {
  sessionId: number;
  startTimeIso: string;
}): Promise<void> {
  if (isExpoGo()) return;
  const Notifications = await getNotifications();
  await ensureCategory();

  const stored = (await settingsGet<Stored>(IDS_KEY)) ?? {};
  if (stored.activeId) await safeDismiss(Notifications, stored.activeId);

  const elapsed = Math.max(0, Math.floor((Date.now() - new Date(startTimeIso).getTime()) / 1000));
  const id = 'ACTIVE_SESSION_NOTIF';
  await Notifications.scheduleNotificationAsync({
    identifier: id,
    content: {
      title: 'Bowels — session running',
      body: `Elapsed ${formatMmSs(elapsed)} • Tap Open to return`,
      categoryIdentifier: 'SESSION_ACTIVE',
      data: { kind: 'session', sessionId, startTimeIso },
      priority: Notifications.AndroidNotificationPriority.MAX,
    },
    trigger: null,
  });

  await settingsSet(IDS_KEY, { activeId: id });
}

export async function dismissSessionNotification(): Promise<void> {
  if (isExpoGo()) return;
  const Notifications = await getNotifications();
  const stored = (await settingsGet<Stored>(IDS_KEY)) ?? {};
  if (!stored.activeId) return;
  await safeDismiss(Notifications, stored.activeId);
  await settingsSet(IDS_KEY, {});
}

let _router: Router | null = null;
let _listener: { remove: () => void } | null = null;

export function registerSessionNotificationRouter(router: Router): void {
  if (isExpoGo()) return;
  _router = router;
  if (_listener) return;
  void ensureCategory();
  void (async () => {
    const Notifications = await getNotifications();
    _listener = Notifications.addNotificationResponseReceivedListener(async (resp) => {
      const action = resp.actionIdentifier;
      const data = (resp.notification.request.content.data ?? {}) as any;
      if (data.kind !== 'session') return;
      const sessionId = Number(data.sessionId ?? 0);
      const startTimeIso = String(data.startTimeIso ?? '');

      if (!_router || !sessionId) return;

      if (action === 'OPEN_SESSION' || action === Notifications.DEFAULT_ACTION_IDENTIFIER) {
        _router.push({ pathname: '/session/active', params: { sessionId: String(sessionId) } });
        return;
      }

      if (action === 'FINISH_SESSION') {
        // Finish on open: compute from timestamps.
        const end = isoNow();
        const elapsed = Math.max(0, Math.floor((Date.now() - new Date(startTimeIso).getTime()) / 1000));
        await finishSession(sessionId, end, elapsed);
        await dismissSessionNotification();
        _router.replace({ pathname: '/session/questions', params: { sessionId: String(sessionId) } });
        return;
      }

      if (action === 'CANCEL_SESSION') {
        await deleteSession(sessionId);
        await dismissSessionNotification();
        _router.replace('/(tabs)');
      }
    });
  })();
}

async function safeDismiss(Notifications: ExpoNotificationsModule, id: string): Promise<void> {
  try {
    await Notifications.dismissNotificationAsync(id);
    return;
  } catch {
    // ignore
  }
  try {
    await Notifications.cancelScheduledNotificationAsync(id);
  } catch {
    // ignore
  }
}

const BG_NOTIF_TASK = 'BG_NOTIF_TASK';

TaskManager.defineTask(BG_NOTIF_TASK, async ({ data, error }) => {
  if (error) return;
  const { actionIdentifier, notification } = data as any;
  const d = notification?.request?.content?.data ?? {};
  if (d.kind !== 'session') return;

  const sessionId = Number(d.sessionId ?? 0);
  const startTimeIso = String(d.startTimeIso ?? '');
  if (!sessionId) return;

  if (actionIdentifier === 'FINISH_SESSION') {
    const end = isoNow();
    const elapsed = Math.max(0, Math.floor((Date.now() - new Date(startTimeIso).getTime()) / 1000));
    await finishSession(sessionId, end, elapsed);
    await dismissSessionNotification();
  }

  if (actionIdentifier === 'CANCEL_SESSION') {
    await deleteSession(sessionId);
    await dismissSessionNotification();
  }
});

export function registerBackgroundNotificationTask(): void {
  if (isExpoGo()) return;
  getNotifications().then((n) => {
    n.registerTaskAsync(BG_NOTIF_TASK).catch(() => {});
  });
}
