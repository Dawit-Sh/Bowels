import type * as SQLite from 'expo-sqlite';

import { getDb } from '@/db/client';
import { isoNow, toDateKeyLocal } from '@/utils/datetime';

export type SessionType = 'urine' | 'bowel';

export type SessionRow = {
  id: number;
  start_time: string;
  end_time: string | null;
  duration_seconds: number | null;
  type: SessionType | null;
  date_key: string;
};

export type SessionAnswerRow = {
  id: number;
  session_id: number;
  question_key: string;
  value: string;
};

async function run(db: SQLite.SQLiteDatabase, sql: string, params: SQLite.SQLiteBindParams = []): Promise<void> {
  await db.runAsync(sql, params);
}

export async function settingsGet<T>(key: string): Promise<T | null> {
  const db = await getDb();
  const row = await db.getFirstAsync<{ value: string }>('SELECT value FROM settings WHERE key = ?;', [key]);
  if (!row) return null;
  try {
    return JSON.parse(row.value) as T;
  } catch {
    return null;
  }
}

export async function settingsSet<T>(key: string, value: T): Promise<void> {
  const db = await getDb();
  await run(
    db,
    `
INSERT INTO settings(key, value, updated_at)
VALUES(?, ?, ?)
ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at;
`,
    [key, JSON.stringify(value), isoNow()]
  );
}

export async function createSession(startTimeIso: string): Promise<number> {
  const db = await getDb();
  const dateKey = toDateKeyLocal(startTimeIso);
  const res = await db.runAsync(
    'INSERT INTO sessions(start_time, date_key, created_at, updated_at) VALUES (?, ?, ?, ?);',
    [startTimeIso, dateKey, isoNow(), isoNow()]
  );
  return res.lastInsertRowId;
}

export async function finishSession(sessionId: number, endTimeIso: string, durationSeconds: number): Promise<void> {
  const db = await getDb();
  await run(
    db,
    'UPDATE sessions SET end_time = ?, duration_seconds = ?, updated_at = ? WHERE id = ?;',
    [endTimeIso, durationSeconds, isoNow(), sessionId]
  );
}

export async function deleteSession(sessionId: number): Promise<void> {
  const db = await getDb();
  await run(db, 'DELETE FROM sessions WHERE id = ?;', [sessionId]);
}

export async function setSessionType(sessionId: number, type: SessionType): Promise<void> {
  const db = await getDb();
  await run(db, 'UPDATE sessions SET type = ?, updated_at = ? WHERE id = ?;', [type, isoNow(), sessionId]);
}

export async function upsertSessionAnswer(sessionId: number, questionKey: string, value: string): Promise<void> {
  const db = await getDb();
  await run(
    db,
    `
INSERT INTO session_answers(session_id, question_key, value, created_at)
VALUES(?, ?, ?, ?)
ON CONFLICT(session_id, question_key) DO UPDATE SET value = excluded.value;
`,
    [sessionId, questionKey, value, isoNow()]
  );
}

export async function getSession(sessionId: number): Promise<SessionRow | null> {
  const db = await getDb();
  return await db.getFirstAsync<SessionRow>('SELECT * FROM sessions WHERE id = ?;', [sessionId]);
}

export async function getSessionAnswers(sessionId: number): Promise<SessionAnswerRow[]> {
  const db = await getDb();
  return await db.getAllAsync<SessionAnswerRow>('SELECT * FROM session_answers WHERE session_id = ?;', [sessionId]);
}

export async function listAllSessionAnswers(): Promise<SessionAnswerRow[]> {
  const db = await getDb();
  return await db.getAllAsync<SessionAnswerRow>('SELECT * FROM session_answers ORDER BY session_id ASC, id ASC;');
}

export async function listSessions(limit = 50, offset = 0): Promise<SessionRow[]> {
  const db = await getDb();
  return await db.getAllAsync<SessionRow>(
    'SELECT * FROM sessions ORDER BY start_time DESC LIMIT ? OFFSET ?;',
    [limit, offset]
  );
}

export async function listSessionsByDateKey(fromDateKey: string, toDateKey: string): Promise<SessionRow[]> {
  const db = await getDb();
  return await db.getAllAsync<SessionRow>(
    'SELECT * FROM sessions WHERE date_key >= ? AND date_key <= ? ORDER BY start_time ASC;',
    [fromDateKey, toDateKey]
  );
}

export async function getLastSessionOfType(type: SessionType): Promise<SessionRow | null> {
  const db = await getDb();
  return await db.getFirstAsync<SessionRow>(
    'SELECT * FROM sessions WHERE type = ? AND end_time IS NOT NULL ORDER BY start_time DESC LIMIT 1;',
    [type]
  );
}

export async function getActiveSession(): Promise<SessionRow | null> {
  const db = await getDb();
  return await db.getFirstAsync<SessionRow>(
    'SELECT * FROM sessions WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1;'
  );
}

export type DailyHealthRow = {
  date_key: string;
  water: string | null;
  fiber: string | null;
  meals: string | null;
  stress: string | null;
  sleep: string | null;
  exercise: string | null;
};

export async function upsertDailyHealth(row: DailyHealthRow): Promise<void> {
  const db = await getDb();
  await run(
    db,
    `
INSERT INTO daily_health(date_key, water, fiber, meals, stress, sleep, exercise, created_at, updated_at)
VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
ON CONFLICT(date_key) DO UPDATE SET
  water = excluded.water,
  fiber = excluded.fiber,
  meals = excluded.meals,
  stress = excluded.stress,
  sleep = excluded.sleep,
  exercise = excluded.exercise,
  updated_at = excluded.updated_at;
`,
    [
      row.date_key,
      row.water,
      row.fiber,
      row.meals,
      row.stress,
      row.sleep,
      row.exercise,
      isoNow(),
      isoNow(),
    ]
  );
}

export async function getDailyHealth(dateKey: string): Promise<DailyHealthRow | null> {
  const db = await getDb();
  return await db.getFirstAsync<DailyHealthRow>('SELECT * FROM daily_health WHERE date_key = ?;', [dateKey]);
}

export async function listDailyHealth(fromDateKey: string, toDateKey: string): Promise<DailyHealthRow[]> {
  const db = await getDb();
  return await db.getAllAsync<DailyHealthRow>(
    'SELECT * FROM daily_health WHERE date_key >= ? AND date_key <= ? ORDER BY date_key ASC;',
    [fromDateKey, toDateKey]
  );
}

export async function listAllDailyHealth(): Promise<DailyHealthRow[]> {
  const db = await getDb();
  return await db.getAllAsync<DailyHealthRow>('SELECT * FROM daily_health ORDER BY date_key ASC;');
}

export async function listAllSettings(): Promise<{ key: string; value: string; updated_at: string }[]> {
  const db = await getDb();
  return await db.getAllAsync<{ key: string; value: string; updated_at: string }>(
    'SELECT key, value, updated_at FROM settings ORDER BY key ASC;'
  );
}

export type DailySessionAggregate = {
  date_key: string;
  visits: number;
  total_seconds: number;
  avg_seconds: number;
};

export async function getDailyAggregates(fromDateKey: string, toDateKey: string): Promise<DailySessionAggregate[]> {
  const db = await getDb();
  return await db.getAllAsync<DailySessionAggregate>(
    `
SELECT
  date_key,
  COUNT(*) as visits,
  COALESCE(SUM(duration_seconds), 0) as total_seconds,
  COALESCE(AVG(duration_seconds), 0) as avg_seconds
FROM sessions
WHERE date_key >= ? AND date_key <= ? AND duration_seconds IS NOT NULL
GROUP BY date_key
ORDER BY date_key ASC;
`,
    [fromDateKey, toDateKey]
  );
}

export async function getStoolTypeDistribution(fromDateKey: string, toDateKey: string): Promise<
  { stool_type: string; count: number }[]
> {
  const db = await getDb();
  return await db.getAllAsync<{ stool_type: string; count: number }>(
    `
SELECT sa.value as stool_type, COUNT(*) as count
FROM session_answers sa
JOIN sessions s ON s.id = sa.session_id
WHERE sa.question_key = 'bowel_stool_type'
  AND s.date_key >= ? AND s.date_key <= ?
GROUP BY sa.value
ORDER BY count DESC;
`,
    [fromDateKey, toDateKey]
  );
}

export async function getDailyAvgStoolType(fromDateKey: string, toDateKey: string): Promise<
  { date_key: string; avg_stool_type: number; count: number }[]
> {
  const db = await getDb();
  return await db.getAllAsync<{ date_key: string; avg_stool_type: number; count: number }>(
    `
SELECT
  s.date_key as date_key,
  AVG(CAST(sa.value AS INTEGER)) as avg_stool_type,
  COUNT(*) as count
FROM session_answers sa
JOIN sessions s ON s.id = sa.session_id
WHERE sa.question_key = 'bowel_stool_type'
  AND s.date_key >= ? AND s.date_key <= ?
GROUP BY s.date_key
ORDER BY s.date_key ASC;
`,
    [fromDateKey, toDateKey]
  );
}

export async function insightsCacheGet<T>(key: string): Promise<T | null> {
  const db = await getDb();
  const row = await db.getFirstAsync<{ payload: string }>('SELECT payload FROM insights_cache WHERE key = ?;', [key]);
  if (!row) return null;
  try {
    return JSON.parse(row.payload) as T;
  } catch {
    return null;
  }
}

export async function insightsCacheSet<T>(key: string, payload: T): Promise<void> {
  const db = await getDb();
  await run(
    db,
    `
INSERT INTO insights_cache(key, payload, updated_at)
VALUES(?, ?, ?)
ON CONFLICT(key) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at;
`,
    [key, JSON.stringify(payload), isoNow()]
  );
}
