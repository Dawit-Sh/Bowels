export const SCHEMA_VERSION = 1;

export const SCHEMA_SQL = `
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS sessions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  start_time TEXT NOT NULL,
  end_time TEXT,
  duration_seconds INTEGER,
  type TEXT CHECK (type IN ('urine', 'bowel')),
  date_key TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
  updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE INDEX IF NOT EXISTS idx_sessions_date_key ON sessions(date_key);
CREATE INDEX IF NOT EXISTS idx_sessions_start_time ON sessions(start_time);

CREATE TABLE IF NOT EXISTS session_answers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id INTEGER NOT NULL,
  question_key TEXT NOT NULL,
  value TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
  UNIQUE (session_id, question_key)
);

CREATE INDEX IF NOT EXISTS idx_session_answers_session_id ON session_answers(session_id);

CREATE TABLE IF NOT EXISTS daily_health (
  date_key TEXT PRIMARY KEY,
  water TEXT,
  fiber TEXT,
  meals TEXT,
  stress TEXT,
  sleep TEXT,
  exercise TEXT,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
  updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS insights_cache (
  key TEXT PRIMARY KEY,
  payload TEXT NOT NULL,
  updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);
`;

