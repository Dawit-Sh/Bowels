import * as SQLite from "expo-sqlite";

import { schemaSql } from "./schema";

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

const expectedTables: Record<string, string[]> = {
  sessions: ["id", "kind", "start_time", "end_time", "duration_seconds", "created_at"],
  session_answers: ["id", "session_id", "answer_key", "answer_value", "created_at"],
  daily_health: ["id", "day", "water", "fiber", "meals", "stress", "sleep", "exercise", "caffeine", "alcohol", "medication", "mood", "created_at", "updated_at"],
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

async function runMigrations(db: SQLite.SQLiteDatabase) {
  // Check if new columns exist, if not add them
  try {
    // Try to select new columns - if they don't exist, this will fail
    await db.getFirstAsync("SELECT caffeine, alcohol, medication, mood FROM daily_health LIMIT 1");
  } catch {
    // Columns don't exist, add them
    await db.execAsync(`
      ALTER TABLE daily_health ADD COLUMN caffeine TEXT DEFAULT 'None';
      ALTER TABLE daily_health ADD COLUMN alcohol TEXT DEFAULT 'None';
      ALTER TABLE daily_health ADD COLUMN medication TEXT DEFAULT 'None';
      ALTER TABLE daily_health ADD COLUMN mood TEXT DEFAULT 'Neutral';
    `);
  }
}

export async function getDb() {
  if (!dbPromise) {
    dbPromise = SQLite.openDatabaseAsync("bowels.db")
      .then(async (db) => {
        try {
          await ensureSchema(db);
          await runMigrations(db);
          return db;
        } catch (error) {
          console.error("Database initialization error:", error);
          throw error;
        }
      })
      .catch((error) => {
        console.error("Failed to open database:", error);
        dbPromise = null; // Reset so it can be retried
        throw error;
      });
  }

  return dbPromise;
}
