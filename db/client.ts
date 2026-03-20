import * as SQLite from "expo-sqlite";

import { schemaSql } from "./schema";

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

const expectedTables: Record<string, string[]> = {
  sessions: ["id", "kind", "start_time", "end_time", "duration_seconds", "created_at"],
  session_answers: ["id", "session_id", "answer_key", "answer_value", "created_at"],
  daily_health: ["id", "day", "water", "fiber", "meals", "stress", "sleep", "exercise", "created_at", "updated_at"],
  settings: ["setting_key", "setting_value", "updated_at"],
};

async function getColumns(db: SQLite.SQLiteDatabase, tableName: string) {
  const rows = await db.getAllAsync<{ name: string }>(`PRAGMA table_info(${tableName})`);
  return rows.map((row) => row.name);
}

async function resetSchema(db: SQLite.SQLiteDatabase) {
  await db.execAsync(`
    DROP TABLE IF EXISTS session_answers;
    DROP TABLE IF EXISTS daily_health;
    DROP TABLE IF EXISTS settings;
    DROP TABLE IF EXISTS sessions;
  `);
  await db.execAsync(schemaSql);
}

async function ensureSchema(db: SQLite.SQLiteDatabase) {
  await db.execAsync(schemaSql);

  for (const [tableName, expectedColumns] of Object.entries(expectedTables)) {
    const columns = await getColumns(db, tableName);
    const compatible = expectedColumns.every((column) => columns.includes(column));
    if (!compatible) {
      await resetSchema(db);
      return;
    }
  }
}

export async function getDb() {
  if (!dbPromise) {
    dbPromise = SQLite.openDatabaseAsync("bowels.db").then(async (db) => {
      await ensureSchema(db);
      return db;
    });
  }

  return dbPromise;
}
