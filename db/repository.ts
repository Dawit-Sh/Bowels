import { getDb } from "./client";
import type {
  AppSettings,
  DailyHealthInput,
  DailyHealthRecord,
  SessionDraft,
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

export async function importArchiveData(archive: {
  sessions: Array<SessionRecord>;
  answers: Array<{ sessionId: number; key: string; value: string }>;
  dailyHealth: Array<DailyHealthRecord>;
  settings: Array<SettingRecord>;
}) {
  const db = await getDb();

  for (const session of archive.sessions) {
    const inserted = await db.runAsync(
      "INSERT INTO sessions (kind, start_time, end_time, duration_seconds, created_at) VALUES (?, ?, ?, ?, ?)",
      [session.kind, session.startTime, session.endTime, session.durationSeconds, session.createdAt]
    );
    for (const answer of archive.answers.filter((item) => item.sessionId === session.id)) {
      await db.runAsync(
        "INSERT INTO session_answers (session_id, answer_key, answer_value, created_at) VALUES (?, ?, ?, ?)",
        [inserted.lastInsertRowId, answer.key, answer.value, nowIso()]
      );
    }
  }

  for (const health of archive.dailyHealth) {
    await db.runAsync(
      "INSERT OR REPLACE INTO daily_health (day, water, fiber, meals, stress, sleep, exercise, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
      [health.day, health.water, health.fiber, health.meals, health.stress, health.sleep, health.exercise, health.createdAt, health.updatedAt]
    );
  }

  for (const setting of archive.settings) {
    await db.runAsync(
      "INSERT OR REPLACE INTO settings (setting_key, setting_value, updated_at) VALUES (?, ?, ?)",
      [setting.key, setting.value, setting.updatedAt]
    );
  }
}
