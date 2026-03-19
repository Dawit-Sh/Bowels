import { create } from 'zustand';

import { settingsGet, settingsSet } from '@/db/queries';
import type { AccentColor, ColorMode } from '@/theme/types';

type PersistedSettings = {
  mode: ColorMode;
  accent: AccentColor;
  hasOnboarded: boolean;
  hasRealData: boolean;
  notificationsEnabled: boolean;
  dailyReminderTime: string; // "HH:MM"
  weeklyReportEnabled: boolean;
};

const DEFAULTS: PersistedSettings = {
  mode: 'light',
  accent: 'orange',
  hasOnboarded: false,
  hasRealData: false,
  notificationsEnabled: true,
  dailyReminderTime: '20:00',
  weeklyReportEnabled: true,
};

type SettingsState = PersistedSettings & {
  hydrated: boolean;
  hydrate: () => Promise<void>;
  setMode: (mode: ColorMode) => Promise<void>;
  setAccent: (accent: AccentColor) => Promise<void>;
  setHasOnboarded: (hasOnboarded: boolean) => Promise<void>;
  setHasRealData: (hasRealData: boolean) => Promise<void>;
  setNotificationsEnabled: (enabled: boolean) => Promise<void>;
  setDailyReminderTime: (hhmm: string) => Promise<void>;
  setWeeklyReportEnabled: (enabled: boolean) => Promise<void>;
};

const SETTINGS_KEY = 'app_settings_v1';

export const useSettingsStore = create<SettingsState>((set, get) => ({
  ...DEFAULTS,
  hydrated: false,
  hydrate: async () => {
    const loaded = (await settingsGet<PersistedSettings>(SETTINGS_KEY)) ?? DEFAULTS;
    set({ ...DEFAULTS, ...loaded, hydrated: true });
  },
  setMode: async (mode) => {
    set({ mode });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), mode });
  },
  setAccent: async (accent) => {
    set({ accent });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), accent });
  },
  setHasOnboarded: async (hasOnboarded) => {
    set({ hasOnboarded });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), hasOnboarded });
  },
  setHasRealData: async (hasRealData) => {
    set({ hasRealData });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), hasRealData });
  },
  setNotificationsEnabled: async (notificationsEnabled) => {
    set({ notificationsEnabled });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), notificationsEnabled });
  },
  setDailyReminderTime: async (dailyReminderTime) => {
    set({ dailyReminderTime });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), dailyReminderTime });
  },
  setWeeklyReportEnabled: async (weeklyReportEnabled) => {
    set({ weeklyReportEnabled });
    await settingsSet(SETTINGS_KEY, { ...getPersisted(get()), weeklyReportEnabled });
  },
}));

function getPersisted(state: SettingsState): PersistedSettings {
  return {
    mode: state.mode,
    accent: state.accent,
    hasOnboarded: state.hasOnboarded,
    hasRealData: state.hasRealData,
    notificationsEnabled: state.notificationsEnabled,
    dailyReminderTime: state.dailyReminderTime,
    weeklyReportEnabled: state.weeklyReportEnabled,
  };
}
