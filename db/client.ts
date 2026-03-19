import * as SQLite from 'expo-sqlite';

import { SCHEMA_SQL, SCHEMA_VERSION } from '@/db/schema';

let _db: SQLite.SQLiteDatabase | null = null;
let _initPromise: Promise<void> | null = null;

export async function getDb(): Promise<SQLite.SQLiteDatabase> {
  if (_db) return _db;
  _db = await SQLite.openDatabaseAsync('bowels.db');
  return _db;
}

async function getUserVersion(db: SQLite.SQLiteDatabase): Promise<number> {
  const row = await db.getFirstAsync<{ user_version: number }>('PRAGMA user_version;');
  return row?.user_version ?? 0;
}

async function setUserVersion(db: SQLite.SQLiteDatabase, version: number): Promise<void> {
  await db.execAsync(`PRAGMA user_version = ${version};`);
}

async function migrate(db: SQLite.SQLiteDatabase): Promise<void> {
  const userVersion = await getUserVersion(db);
  if (userVersion >= SCHEMA_VERSION) return;

  // v1: initial schema
  await db.execAsync(SCHEMA_SQL);
  await setUserVersion(db, SCHEMA_VERSION);
}

export async function initDb(): Promise<void> {
  if (_initPromise) return _initPromise;
  _initPromise = (async () => {
    const db = await getDb();
    await db.execAsync('PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;');
    await migrate(db);
  })();
  return _initPromise;
}
