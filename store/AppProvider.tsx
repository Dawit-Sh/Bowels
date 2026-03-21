import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Appearance } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";

import { buildAnalytics, buildInsights } from "../src/analytics";
import { demoDailyHealth, demoSessionAnswers, demoSessions } from "../src/demoData";
import { scheduleDailyReminder } from "../src/notifications";
import type {
  AccentKey,
  AnalyticsSummary,
  AppScreen,
  AppSettings,
  DailyHealthInput,
  DailyHealthRecord,
  InsightItem,
  SessionAnswerRecord,
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
  completeOnboarding: () => Promise<void>;
  markHasRealData: () => Promise<void>;
};

const defaultSettings: AppSettings = {
  themeMode: "system",
  accent: "olive",
  reminderHour: 20,
  notificationsEnabled: true,
  hasOnboarded: false,
  hasRealData: false,
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
const ACTIVE_DRAFT_KEY = "bowels-active-draft";

function buildAnswerMap(answerRows: Array<{ sessionId: number; key: string; value: string }>) {
  return answerRows.reduce<Record<number, Record<string, string>>>((acc, item) => {
    acc[item.sessionId] = acc[item.sessionId] ?? {};
    acc[item.sessionId][item.key] = item.value;
    return acc;
  }, {});
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true);
  const [screen, setScreen] = useState<AppScreen>("home");
  const [sessions, setSessions] = useState<SessionRecord[]>([]);
  const [answers, setAnswers] = useState<Record<number, Record<string, string>>>({});
  const [dailyHealth, setDailyHealth] = useState<DailyHealthRecord[]>([]);
  const [settings, setSettings] = useState<AppSettings>(defaultSettings);
  const [activeDraft, setActiveDraft] = useState<SessionDraft>(emptyDraft);

  const demoAnswerMap = useMemo(
    () =>
      buildAnswerMap(
        demoSessionAnswers.map((item: SessionAnswerRecord) => ({
          sessionId: item.sessionId,
          key: item.key,
          value: item.value,
        }))
      ),
    []
  );

  const persistSettings = async (next: AppSettings) => {
    setSettings(next);
    await saveSettings(next);
    if (next.notificationsEnabled) {
      await scheduleDailyReminder(next.reminderHour);
    }
  };

  const markHasRealData = async () => {
    if (settings.hasRealData) {
      return;
    }
    await persistSettings({ ...settings, hasRealData: true });
  };

  const reload = async () => {
    const [sessionRows, answerRows, healthRows, settingRows] = await Promise.all([
      readSessions(),
      readSessionAnswers(),
      readDailyHealth(),
      readSettings(),
    ]);

    setSessions(sessionRows);
    setDailyHealth(healthRows);
    setAnswers(buildAnswerMap(answerRows));

    if (settingRows.length) {
      const nextSettings = { ...defaultSettings };
      settingRows.forEach((row) => {
        if (row.key === "themeMode") nextSettings.themeMode = row.value as ThemeMode;
        if (row.key === "accent") nextSettings.accent = row.value as AccentKey;
        if (row.key === "reminderHour") nextSettings.reminderHour = Number(row.value);
        if (row.key === "notificationsEnabled") nextSettings.notificationsEnabled = row.value === "true";
        if (row.key === "hasOnboarded") nextSettings.hasOnboarded = row.value === "true";
        if (row.key === "hasRealData") nextSettings.hasRealData = row.value === "true";
      });
      setSettings(nextSettings);
      if (!nextSettings.hasOnboarded) {
        setScreen("onboarding");
      }
    } else {
      await saveSettings(defaultSettings);
      setSettings(defaultSettings);
      setScreen("onboarding");
    }
  };

  useEffect(() => {
    reload().finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const persistDraft = async () => {
      if (activeDraft.startTime) {
        await AsyncStorage.setItem(ACTIVE_DRAFT_KEY, JSON.stringify(activeDraft));
        return;
      }
      await AsyncStorage.removeItem(ACTIVE_DRAFT_KEY);
    };

    void persistDraft();
  }, [activeDraft]);

  useEffect(() => {
    const restoreDraft = async () => {
      const raw = await AsyncStorage.getItem(ACTIVE_DRAFT_KEY);
      if (!raw) {
        return;
      }
      try {
        const restored = JSON.parse(raw) as SessionDraft;
        if (restored?.startTime) {
          setActiveDraft(restored);
        }
      } catch {
        await AsyncStorage.removeItem(ACTIVE_DRAFT_KEY);
      }
    };

    void restoreDraft();
  }, []);

  const usingDemo = !settings.hasRealData;
  const visibleSessions = usingDemo ? demoSessions : sessions;
  const visibleAnswers = usingDemo ? demoAnswerMap : answers;
  const visibleDailyHealth = usingDemo ? demoDailyHealth : dailyHealth;

  const analytics = useMemo(() => buildAnalytics(visibleSessions, visibleAnswers, visibleDailyHealth), [visibleAnswers, visibleDailyHealth, visibleSessions]);
  const insights = useMemo(() => buildInsights(visibleSessions, visibleAnswers), [visibleAnswers, visibleSessions]);

  const value: AppContextValue = {
    loading,
    screen,
    setScreen,
    sessions: visibleSessions,
    dailyHealth: visibleDailyHealth,
    settings,
    analytics,
    insights,
    refresh: reload,
    activeDraft,
    quickLogBowel: async (stoolType) => {
      await markHasRealData();
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
      await markHasRealData();
      await saveSession(activeDraft);
      await AsyncStorage.removeItem(ACTIVE_DRAFT_KEY);
      await reload();
      setActiveDraft(emptyDraft);
      setScreen("history");
    },
    saveDailyHealth: async (input) => {
      await markHasRealData();
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
    completeOnboarding: async () => {
      await persistSettings({ ...settings, hasOnboarded: true });
      setScreen("home");
    },
    markHasRealData,
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
