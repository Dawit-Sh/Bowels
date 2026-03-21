import { getDb } from "./client";
import type {
  AppSettings,
  DailyHealthInput,
  DailyHealthRecord,
  SessionDraft,
  SessionAnswerRecord,
  SessionRecord,
  SettingRecord,
} from "../src/types";

const nowIso = () => new Date().toISOString();

export async function saveSession(draft: SessionDraft) {
  if (!draft.startTime || !draft.endTime) {
    throw new Error("Session draft is incomplete.");
  }

  const db = await getDb();
  const result = await db.runAsync(
    "INSERT INTO sessions (kind, start_time, end_time, duration_seconds, created_at) VALUES (?, ?, ?, ?, ?)",
    [draft.kind, draft.startTime, draft.endTime, draft.durationSeconds, nowIso()]
  );

  const answers = (
    draft.kind === "urine"
      ? [
          ["urine_urgency", draft.urineUrgency],
          ["urine_color", draft.urineColor],
        ]
      : [
          ["stool_type", draft.stoolType ? String(draft.stoolType) : null],
          ["pain", draft.pain],
          ["blood", draft.blood === null ? null : String(draft.blood)],
          ["urgency", draft.urgency ? String(draft.urgency) : null],
          ["completion", draft.completion],
          ["adaptive_tags", draft.adaptiveTags.join(",")],
        ]
  ).filter(([, value]) => value !== null && value !== "");

  for (const [key, value] of answers) {
    await db.runAsync(
      "INSERT INTO session_answers (session_id, answer_key, answer_value, created_at) VALUES (?, ?, ?, ?)",
      [result.lastInsertRowId, key, String(value), nowIso()]
    );
  }
}

export async function upsertDailyHealth(day: string, input: DailyHealthInput) {
  const db = await getDb();
  const existing = await db.getFirstAsync<{ id: number }>("SELECT id FROM daily_health WHERE day = ?", [day]);
  const values = [input.water, input.fiber, input.meals, input.stress, input.sleep, input.exercise];

  if (existing) {
    await db.runAsync(
      "UPDATE daily_health SET water = ?, fiber = ?, meals = ?, stress = ?, sleep = ?, exercise = ?, updated_at = ? WHERE day = ?",
      [...values, nowIso(), day]
    );
    return;
  }

  await db.runAsync(
    "INSERT INTO daily_health (day, water, fiber, meals, stress, sleep, exercise, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
    [day, ...values, nowIso(), nowIso()]
  );
}

export async function saveSettings(settings: AppSettings) {
  const db = await getDb();
  const entries = Object.entries(settings);
  for (const [key, value] of entries) {
    await db.runAsync(
      "INSERT OR REPLACE INTO settings (setting_key, setting_value, updated_at) VALUES (?, ?, ?)",
      [key, String(value), nowIso()]
    );
  }
}

export async function readSessions() {
  const db = await getDb();
  return db.getAllAsync<SessionRecord>(
    "SELECT id, kind, start_time AS startTime, end_time AS endTime, duration_seconds AS durationSeconds, created_at AS createdAt FROM sessions ORDER BY start_time DESC"
  );
}

export async function readSessionAnswers() {
  const db = await getDb();
  return db.getAllAsync<{ sessionId: number; key: string; value: string }>(
    "SELECT session_id AS sessionId, answer_key AS key, answer_value AS value FROM session_answers"
  );
}

export async function readDailyHealth() {
  const db = await getDb();
  return db.getAllAsync<DailyHealthRecord>(
    "SELECT id, day, water, fiber, meals, stress, sleep, exercise, created_at AS createdAt, updated_at AS updatedAt FROM daily_health ORDER BY day DESC"
  );
}

export async function readSettings() {
  const db = await getDb();
  const rows = await db.getAllAsync<SettingRecord>(
    "SELECT setting_key AS `key`, setting_value AS `value`, updated_at AS updatedAt FROM settings"
  );
  return rows;
}

export async function exportArchiveData() {
  const [sessions, answers, dailyHealth, settings] = await Promise.all([
    readSessions(),
    readSessionAnswers(),
    readDailyHealth(),
    readSettings(),
  ]);

  return { sessions, answers, dailyHealth, settings };
}

type NormalizedArchive = {
  sessions: Array<SessionRecord>;
  answers: Array<{ sessionId: number; key: string; value: string }>;
  dailyHealth: Array<DailyHealthRecord>;
  settings: Array<SettingRecord>;
};

const legacyAnswerKeyMap: Record<string, string | null> = {
  session_type: null,
  bowel_stool_type: "stool_type",
  bowel_pain: "pain",
  bowel_blood: "blood",
  bowel_urgency: "urgency",
  bowel_completion: "completion",
  bowel_adaptive_tags: "adaptive_tags",
  urine_urgency: "urine_urgency",
  urine_color: "urine_color",
};

function toIsoDate(value: unknown) {
  return typeof value === "string" && value ? value : nowIso();
}

function toBooleanString(value: unknown) {
  if (typeof value === "boolean") {
    return String(value);
  }
  if (typeof value === "string") {
    const lowered = value.trim().toLowerCase();
    if (["yes", "true", "1"].includes(lowered)) return "true";
    if (["no", "false", "0"].includes(lowered)) return "false";
    return value;
  }
  return String(value ?? "");
}

function normalizeArchive(raw: any): NormalizedArchive {
  if (Array.isArray(raw?.answers) && Array.isArray(raw?.sessions)) {
    const settings = Array.isArray(raw.settings) ? [...raw.settings] : [];
    if (!settings.some((item) => item.key === "hasRealData")) {
      settings.push({ key: "hasRealData", value: "true", updatedAt: nowIso() });
    }
    if (!settings.some((item) => item.key === "hasOnboarded")) {
      settings.push({ key: "hasOnboarded", value: "true", updatedAt: nowIso() });
    }
    return { ...raw, settings } as NormalizedArchive;
  }

  if (!Array.isArray(raw?.sessions) || !Array.isArray(raw?.session_answers)) {
    throw new Error("Unsupported archive format.");
  }

  const sessions: SessionRecord[] = raw.sessions.map((session: any, index: number) => ({
    id: Number(session.id ?? index + 1),
    kind: session.type === "urine" ? "urine" : "bowel",
    startTime: toIsoDate(session.start_time),
    endTime: toIsoDate(session.end_time ?? session.start_time),
    durationSeconds: Number(session.duration_seconds ?? 0),
    createdAt: toIsoDate(session.created_at ?? session.updated_at ?? session.start_time),
  }));

  const answers = raw.session_answers
    .map((answer: any) => {
      const key = legacyAnswerKeyMap[String(answer.question_key ?? "")];
      if (!key) {
        return null;
      }
      const rawValue = key === "blood" ? toBooleanString(answer.value) : String(answer.value ?? "");
      return {
        sessionId: Number(answer.session_id),
        key,
        value: rawValue,
      };
    })
    .filter(Boolean) as Array<{ sessionId: number; key: string; value: string }>;

  const dailyHealth: DailyHealthRecord[] = Array.isArray(raw.daily_health)
    ? raw.daily_health.map((health: any, index: number) => ({
        id: Number(health.id ?? index + 1),
        day: String(health.day ?? health.date_key ?? toIsoDate(health.created_at).slice(0, 10)),
        water: String(health.water ?? ""),
        fiber: String(health.fiber ?? ""),
        meals: String(health.meals ?? ""),
        stress: String(health.stress ?? ""),
        sleep: String(health.sleep ?? ""),
        exercise: String(health.exercise ?? ""),
        createdAt: toIsoDate(health.created_at),
        updatedAt: toIsoDate(health.updated_at ?? health.created_at),
      }))
    : [];

  const settings: SettingRecord[] = [];
  let sawOnboardingState = false;

  if (Array.isArray(raw.settings)) {
    for (const setting of raw.settings) {
      const key = String(setting.key ?? "");
      const value = setting.value;
      if (key === "app_settings_v1") {
        try {
          const parsed = JSON.parse(String(value ?? "{}"));
          if (parsed.mode) settings.push({ key: "themeMode", value: String(parsed.mode), updatedAt: toIsoDate(setting.updated_at) });
          if (parsed.accent) settings.push({ key: "accent", value: String(parsed.accent).toLowerCase() === "orange" ? "coral" : String(parsed.accent), updatedAt: toIsoDate(setting.updated_at) });
          if (parsed.dailyReminderTime) settings.push({ key: "reminderHour", value: String(Number(String(parsed.dailyReminderTime).split(":")[0]) || 20), updatedAt: toIsoDate(setting.updated_at) });
          if (parsed.notificationsEnabled !== undefined) settings.push({ key: "notificationsEnabled", value: String(Boolean(parsed.notificationsEnabled)), updatedAt: toIsoDate(setting.updated_at) });
          if (parsed.hasOnboarded !== undefined) {
            sawOnboardingState = true;
            settings.push({ key: "hasOnboarded", value: String(Boolean(parsed.hasOnboarded)), updatedAt: toIsoDate(setting.updated_at) });
          }
        } catch {
          // Ignore malformed legacy settings payloads.
        }
        continue;
      }
      if (["themeMode", "accent", "reminderHour", "notificationsEnabled", "hasOnboarded"].includes(key)) {
        if (key === "hasOnboarded") {
          sawOnboardingState = true;
        }
        settings.push({ key, value: String(value ?? ""), updatedAt: toIsoDate(setting.updated_at) });
      }
    }
  }

  if (!sawOnboardingState) {
    settings.push({ key: "hasOnboarded", value: "true", updatedAt: nowIso() });
  }
  settings.push({ key: "hasRealData", value: "true", updatedAt: nowIso() });

  return { sessions, answers, dailyHealth, settings };
}

export async function importArchiveData(archive: any) {
  const db = await getDb();
  const normalized = normalizeArchive(archive);
  const sessionIdMap = new Map<number, number>();

  for (const session of normalized.sessions) {
    const inserted = await db.runAsync(
      "INSERT INTO sessions (kind, start_time, end_time, duration_seconds, created_at) VALUES (?, ?, ?, ?, ?)",
      [session.kind, session.startTime, session.endTime, session.durationSeconds, session.createdAt]
    );
    sessionIdMap.set(session.id, Number(inserted.lastInsertRowId));
  }

  for (const answer of normalized.answers) {
    const mappedSessionId = sessionIdMap.get(answer.sessionId);
    if (!mappedSessionId) {
      continue;
    }
    await db.runAsync(
      "INSERT INTO session_answers (session_id, answer_key, answer_value, created_at) VALUES (?, ?, ?, ?)",
      [mappedSessionId, answer.key, answer.value, nowIso()]
    );
  }

  for (const health of normalized.dailyHealth) {
    await db.runAsync(
      "INSERT OR REPLACE INTO daily_health (day, water, fiber, meals, stress, sleep, exercise, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
      [health.day, health.water, health.fiber, health.meals, health.stress, health.sleep, health.exercise, health.createdAt, health.updatedAt]
    );
  }

  for (const setting of normalized.settings) {
    await db.runAsync(
      "INSERT OR REPLACE INTO settings (setting_key, setting_value, updated_at) VALUES (?, ?, ?)",
      [setting.key, setting.value, setting.updatedAt]
    );
  }
}

export async function clearAllData() {
  const db = await getDb();
  await db.execAsync(`
    DELETE FROM session_answers;
    DELETE FROM sessions;
    DELETE FROM daily_health;
  `);
}

export async function readSessionAnswerRecords() {
  const db = await getDb();
  return db.getAllAsync<SessionAnswerRecord>(
    "SELECT id, session_id AS sessionId, answer_key AS `key`, answer_value AS `value`, created_at AS createdAt FROM session_answers ORDER BY id DESC"
  );
}
