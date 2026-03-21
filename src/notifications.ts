import AsyncStorage from "@react-native-async-storage/async-storage";
import Constants from "expo-constants";

const SESSION_NOTIFICATION_KEY = "bowels-active-session-notification-id";
const SESSION_CATEGORY_ID = "sessionControls";
const FINISH_ACTION_ID = "finishSession";
const isExpoGo = Constants.executionEnvironment === "storeClient";
let configured = false;

async function loadNotifications() {
  if (isExpoGo) {
    return null;
  }
  const module = await import("expo-notifications");
  return module;
}

function formatElapsed(startTime: string) {
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(startTime).getTime()) / 1000));
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
}

export async function configureNotifications() {
  if (configured) {
    return;
  }

  const Notifications = await loadNotifications();
  if (!Notifications || typeof Notifications.setNotificationHandler !== "function") {
    return;
  }

  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldPlaySound: false,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });

  await Notifications.setNotificationCategoryAsync(SESSION_CATEGORY_ID, [
    {
      identifier: FINISH_ACTION_ID,
      buttonTitle: "Finish Session",
      options: {
        opensAppToForeground: true,
      },
    },
  ]);

  await Notifications.setNotificationChannelAsync("bowels-session", {
    name: "Bowels Session",
    importance: Notifications.AndroidImportance.HIGH,
  });

  configured = true;
}

export async function ensureNotificationPermission() {
  const Notifications = await loadNotifications();
  if (!Notifications || typeof Notifications.getPermissionsAsync !== "function") {
    return false;
  }
  await configureNotifications();

  const settings = await Notifications.getPermissionsAsync();
  if (settings.granted) {
    return true;
  }

  const requested = await Notifications.requestPermissionsAsync();
  return requested.granted;
}

export async function scheduleDailyReminder(hour: number) {
  const Notifications = await loadNotifications();
  if (!Notifications || typeof Notifications.scheduleNotificationAsync !== "function") {
    return false;
  }
  const granted = await ensureNotificationPermission();
  if (!granted) {
    return false;
  }

  await Notifications.cancelAllScheduledNotificationsAsync();
  await Notifications.scheduleNotificationAsync({
    content: {
      title: "Bowels check-in",
      body: "Log today's rhythm and health check in under a minute.",
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.DAILY,
      hour,
      minute: 0,
    },
  });

  await Notifications.scheduleNotificationAsync({
    content: {
      title: "Weekly report ready",
      body: "Your Weekly Wrapped is available.",
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.WEEKLY,
      weekday: 7,
      hour: 18,
      minute: 0,
    },
  });

  return true;
}

export async function showSessionNotification(startTime: string) {
  const Notifications = await loadNotifications();
  if (!Notifications || typeof Notifications.scheduleNotificationAsync !== "function") {
    return false;
  }
  const granted = await ensureNotificationPermission();
  if (!granted) {
    return false;
  }

  await dismissSessionNotification();

  const id = await Notifications.scheduleNotificationAsync({
    content: {
      title: "Session running",
      body: `Elapsed ${formatElapsed(startTime)}. Tap Finish Session to complete and answer questions.`,
      categoryIdentifier: SESSION_CATEGORY_ID,
      data: { type: "session", startTime },
      sticky: true,
      autoDismiss: false,
    },
    trigger: null,
  });

  await AsyncStorage.setItem(SESSION_NOTIFICATION_KEY, id);
  return true;
}

export async function dismissSessionNotification() {
  const Notifications = await loadNotifications();
  if (!Notifications) {
    return;
  }
  const id = await AsyncStorage.getItem(SESSION_NOTIFICATION_KEY);
  if (id) {
    await Notifications.dismissNotificationAsync(id).catch(() => undefined);
    await Notifications.cancelScheduledNotificationAsync(id).catch(() => undefined);
  }
  await AsyncStorage.removeItem(SESSION_NOTIFICATION_KEY);
}

export async function addSessionResponseListener(listener: (actionId: string, data: Record<string, unknown>) => void) {
  const Notifications = await loadNotifications();
  if (!Notifications || typeof Notifications.addNotificationResponseReceivedListener !== "function") {
    return { remove: () => undefined };
  }
  return Notifications.addNotificationResponseReceivedListener((response) => {
    const data = (response.notification.request.content.data ?? {}) as Record<string, unknown>;
    if (data.type !== "session") {
      return;
    }
    listener(response.actionIdentifier, data);
  });
}

export async function getInitialSessionResponse() {
  const Notifications = await loadNotifications();
  if (!Notifications || typeof Notifications.getLastNotificationResponseAsync !== "function") {
    return null;
  }
  const response = await Notifications.getLastNotificationResponseAsync();
  if (!response) {
    return null;
  }

  const data = (response.notification.request.content.data ?? {}) as Record<string, unknown>;
  if (data.type !== "session") {
    return null;
  }

  await Notifications.clearLastNotificationResponseAsync?.();
  return {
    actionId: response.actionIdentifier,
    data,
  };
}

export function isFinishSessionAction(actionId: string) {
  return actionId === FINISH_ACTION_ID;
}
