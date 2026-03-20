import Constants from "expo-constants";

const isExpoGo = Constants.executionEnvironment === "storeClient";
let configured = false;

async function loadNotifications() {
  if (isExpoGo) {
    return null;
  }

  const module = await import("expo-notifications");
  return module;
}

export async function configureNotifications() {
  if (configured || isExpoGo) {
    return;
  }

  const Notifications = await loadNotifications();
  if (!Notifications) {
    return;
  }

  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldPlaySound: true,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });

  configured = true;
}

export async function ensureNotificationPermission() {
  if (isExpoGo) {
    return false;
  }

  const Notifications = await loadNotifications();
  if (!Notifications) {
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
  if (isExpoGo) {
    return false;
  }

  const Notifications = await loadNotifications();
  if (!Notifications) {
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
