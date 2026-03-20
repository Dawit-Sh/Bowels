export type SessionKind = "urine" | "bowel";
export type PainLevel = "none" | "mild" | "moderate" | "severe";
export type CompletionLevel = "incomplete" | "normal" | "urgent";
export type UrgencyLevel = 1 | 2 | 3 | 4 | 5;
export type UrineUrgency = "low" | "medium" | "high";
export type UrineColor = "light" | "normal" | "dark";
export type HealthField = "water" | "fiber" | "meals" | "stress" | "sleep" | "exercise";
export type AdaptiveTag = "bloating" | "straining" | "gas";
export type StoolType = 1 | 2 | 3 | 4 | 5 | 6 | 7;
export type ThemeMode = "light" | "dark" | "system";
export type AccentKey = "olive" | "coral" | "rose" | "slate";
export type AppScreen = "home" | "active" | "questions" | "history" | "analytics" | "weekly" | "health" | "settings";

export type SessionRecord = {
  id: number;
  kind: SessionKind;
  startTime: string;
  endTime: string;
  durationSeconds: number;
  createdAt: string;
};

export type SessionAnswerRecord = {
  id: number;
  sessionId: number;
  key: string;
  value: string;
  createdAt: string;
};

export type DailyHealthRecord = {
  id: number;
  day: string;
  water: string;
  fiber: string;
  meals: string;
  stress: string;
  sleep: string;
  exercise: string;
  createdAt: string;
  updatedAt: string;
};

export type SettingRecord = {
  key: string;
  value: string;
  updatedAt: string;
};

export type SessionDraft = {
  kind: SessionKind;
  startTime: string | null;
  endTime: string | null;
  durationSeconds: number;
  urineUrgency: UrineUrgency | null;
  urineColor: UrineColor | null;
  stoolType: StoolType | null;
  pain: PainLevel | null;
  blood: boolean | null;
  urgency: UrgencyLevel | null;
  completion: CompletionLevel | null;
  adaptiveTags: AdaptiveTag[];
};

export type DailyHealthInput = Record<HealthField, string>;

export type AppSettings = {
  themeMode: ThemeMode;
  accent: AccentKey;
  reminderHour: number;
  notificationsEnabled: boolean;
};

export type AnalyticsSummary = {
  visitsPerDay: Array<{ day: string; count: number }>;
  totalTimePerDay: Array<{ day: string; totalSeconds: number }>;
  averageDurationSeconds: number;
  stoolDistribution: Array<{ stoolType: StoolType; count: number }>;
  fiberCorrelationLabel: string;
  totalVisits: number;
  totalDurationSeconds: number;
  predictedNextTimeLabel: string;
};

export type InsightItem = {
  id: string;
  title: string;
  body: string;
  severity: "info" | "warning" | "high";
};
