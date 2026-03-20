import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Appearance } from "react-native";

import { buildAnalytics, buildInsights } from "../src/analytics";
import { scheduleDailyReminder } from "../src/notifications";
import type {
  AccentKey,
  AnalyticsSummary,
  AppScreen,
  AppSettings,
  DailyHealthInput,
  DailyHealthRecord,
  InsightItem,
  SessionDraft,
  SessionRecord,
  ThemeMode,
} from "../src/types";
import {
  readDailyHealth,
  readSessionAnswers,
  readSessions,
  readSettings,
  saveSession,
  saveSettings,
  upsertDailyHealth,
} from "../db/repository";

type AppContextValue = {
  loading: boolean;
  screen: AppScreen;
  setScreen: (screen: AppScreen) => void;
  sessions: SessionRecord[];
  dailyHealth: DailyHealthRecord[];
  settings: AppSettings;
  analytics: AnalyticsSummary;
  insights: InsightItem[];
  refresh: () => Promise<void>;
  activeDraft: SessionDraft;
  quickLogBowel: (stoolType: 1 | 2 | 3 | 4 | 5 | 6 | 7) => Promise<void>;
  startSession: () => void;
  finishSession: () => void;
  cancelSession: () => void;
  updateDraft: (patch: Partial<SessionDraft>) => void;
  saveQuestions: () => Promise<void>;
  saveDailyHealth: (input: DailyHealthInput) => Promise<void>;
  setThemeMode: (themeMode: ThemeMode) => Promise<void>;
  setAccent: (accent: AccentKey) => Promise<void>;
  setReminderHour: (hour: number) => Promise<void>;
};

const defaultSettings: AppSettings = {
  themeMode: "system",
  accent: "olive",
  reminderHour: 20,
  notificationsEnabled: true,
};

const emptyDraft: SessionDraft = {
  kind: "bowel",
  startTime: null,
  endTime: null,
  durationSeconds: 0,
  urineUrgency: null,
  urineColor: null,
  stoolType: 4,
  pain: "none",
  blood: false,
  urgency: 3,
  completion: "normal",
  adaptiveTags: [],
};

const AppContext = createContext<AppContextValue | null>(null);

async function seedIfEmpty() {
  const existing = await readSessions();
  if (existing.length) {
    return;
  }

  const now = new Date();
  for (let index = 0; index < 4; index += 1) {
    const start = new Date(now);
    start.setDate(now.getDate() - index);
    start.setHours(8, 10 + index * 4, 0, 0);
    const end = new Date(start.getTime() + (4 + index) * 60 * 1000);
    await saveSession({
      ...emptyDraft,
      kind: "bowel",
      startTime: start.toISOString(),
      endTime: end.toISOString(),
      durationSeconds: Math.floor((end.getTime() - start.getTime()) / 1000),
      stoolType: (index % 2 === 0 ? 4 : 3) as 3 | 4,
    });
  }

  await upsertDailyHealth(new Date().toISOString().slice(0, 10), {
    water: "Great",
    fiber: "High",
    meals: "Balanced",
    stress: "Low",
    sleep: "Great",
    exercise: "Light",
  });
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true);
  const [screen, setScreen] = useState<AppScreen>("home");
  const [sessions, setSessions] = useState<SessionRecord[]>([]);
  const [answers, setAnswers] = useState<Record<number, Record<string, string>>>({});
  const [dailyHealth, setDailyHealth] = useState<DailyHealthRecord[]>([]);
  const [settings, setSettings] = useState<AppSettings>(defaultSettings);
  const [activeDraft, setActiveDraft] = useState<SessionDraft>(emptyDraft);

  const reload = async () => {
    const [sessionRows, answerRows, healthRows, settingRows] = await Promise.all([
      readSessions(),
      readSessionAnswers(),
      readDailyHealth(),
      readSettings(),
    ]);

    setSessions(sessionRows);
    setDailyHealth(healthRows);
    const answerMap = answerRows.reduce<Record<number, Record<string, string>>>((acc, item) => {
      acc[item.sessionId] = acc[item.sessionId] ?? {};
      acc[item.sessionId][item.key] = item.value;
      return acc;
    }, {});
    setAnswers(answerMap);

    if (settingRows.length) {
      const nextSettings = { ...defaultSettings };
      settingRows.forEach((row) => {
        if (row.key === "themeMode") nextSettings.themeMode = row.value as ThemeMode;
        if (row.key === "accent") nextSettings.accent = row.value as AccentKey;
        if (row.key === "reminderHour") nextSettings.reminderHour = Number(row.value);
        if (row.key === "notificationsEnabled") nextSettings.notificationsEnabled = row.value === "true";
      });
      setSettings(nextSettings);
    } else {
      await saveSettings(defaultSettings);
    }
  };

  useEffect(() => {
    seedIfEmpty()
      .then(reload)
      .finally(() => setLoading(false));
  }, []);

  const persistSettings = async (next: AppSettings) => {
    setSettings(next);
    await saveSettings(next);
    if (next.notificationsEnabled) {
      await scheduleDailyReminder(next.reminderHour);
    }
  };

  const analytics = useMemo(() => buildAnalytics(sessions, answers, dailyHealth), [sessions, answers, dailyHealth]);
  const insights = useMemo(() => buildInsights(sessions, answers), [sessions, answers]);

  const value: AppContextValue = {
    loading,
    screen,
    setScreen,
    sessions,
    dailyHealth,
    settings,
    analytics,
    insights,
    refresh: reload,
    activeDraft,
    quickLogBowel: async (stoolType) => {
      const started = new Date();
      const ended = new Date(started.getTime() + 60 * 1000);
      await saveSession({
        ...emptyDraft,
        kind: "bowel",
        stoolType,
        startTime: started.toISOString(),
        endTime: ended.toISOString(),
        durationSeconds: 60,
      });
      await reload();
    },
    startSession: () => {
      setActiveDraft({
        ...emptyDraft,
        startTime: new Date().toISOString(),
      });
      setScreen("active");
    },
    finishSession: () => {
      setActiveDraft((current) => ({
        ...current,
        endTime: new Date().toISOString(),
        durationSeconds: current.startTime ? Math.max(1, Math.floor((Date.now() - new Date(current.startTime).getTime()) / 1000)) : 0,
      }));
      setScreen("questions");
    },
    cancelSession: () => {
      setActiveDraft(emptyDraft);
      setScreen("home");
    },
    updateDraft: (patch) => {
      setActiveDraft((current) => {
        const adaptiveTags = patch.adaptiveTags ? patch.adaptiveTags.slice(0, 5) : current.adaptiveTags;
        return { ...current, ...patch, adaptiveTags };
      });
    },
    saveQuestions: async () => {
      await saveSession(activeDraft);
      await reload();
      setActiveDraft(emptyDraft);
      setScreen("history");
    },
    saveDailyHealth: async (input) => {
      await upsertDailyHealth(new Date().toISOString().slice(0, 10), input);
      await reload();
    },
    setThemeMode: async (themeMode) => {
      await persistSettings({ ...settings, themeMode });
    },
    setAccent: async (accent) => {
      await persistSettings({ ...settings, accent });
    },
    setReminderHour: async (reminderHour) => {
      await persistSettings({ ...settings, reminderHour });
    },
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error("useApp must be used within AppProvider.");
  }
  return context;
}

export function useResolvedThemeMode(themeMode: ThemeMode) {
  if (themeMode === "system") {
    return Appearance.getColorScheme() === "dark" ? "dark" : "light";
  }
  return themeMode;
}
