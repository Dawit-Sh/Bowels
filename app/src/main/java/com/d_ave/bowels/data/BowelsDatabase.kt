package com.d_ave.bowels.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/**
 * The same SQLite file and schema the Expo app used (expo-sqlite keeps it at
 * files/SQLite/bowels.db), so an in-place update from the React Native build keeps every
 * session, answer and health check-in.
 */
class BowelsDatabase(context: Context) : SQLiteOpenHelper(
    context,
    File(context.filesDir, "SQLite").apply { mkdirs() }.resolve("bowels.db").absolutePath,
    null,
    VERSION,
) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    // The Expo database has user_version 0, so this also runs on top of it. Every statement is
    // IF NOT EXISTS, so existing tables and rows are left alone.
    override fun onCreate(db: SQLiteDatabase) {
        SCHEMA.forEach(db::execSQL)
        addMissingHealthColumns(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    private fun addMissingHealthColumns(db: SQLiteDatabase) {
        val columns = db.rawQuery("PRAGMA table_info(daily_health)", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }
        listOf("caffeine" to "None", "alcohol" to "None", "medication" to "None", "mood" to "Neutral")
            .filter { (name, _) -> name !in columns }
            .forEach { (name, default) -> db.execSQL("ALTER TABLE daily_health ADD COLUMN $name TEXT DEFAULT '$default'") }
    }

    companion object {
        private const val VERSION = 1

        private val SCHEMA = listOf(
            """
            CREATE TABLE IF NOT EXISTS sessions (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              kind TEXT NOT NULL,
              start_time TEXT NOT NULL,
              end_time TEXT NOT NULL,
              duration_seconds INTEGER NOT NULL,
              created_at TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS session_answers (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              session_id INTEGER NOT NULL,
              answer_key TEXT NOT NULL,
              answer_value TEXT NOT NULL,
              created_at TEXT NOT NULL,
              FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS daily_health (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              day TEXT NOT NULL UNIQUE,
              water TEXT NOT NULL,
              fiber TEXT NOT NULL,
              meals TEXT NOT NULL,
              stress TEXT NOT NULL,
              sleep TEXT NOT NULL,
              exercise TEXT NOT NULL,
              caffeine TEXT DEFAULT 'None',
              alcohol TEXT DEFAULT 'None',
              medication TEXT DEFAULT 'None',
              mood TEXT DEFAULT 'Neutral',
              created_at TEXT NOT NULL,
              updated_at TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS settings (
              setting_key TEXT PRIMARY KEY NOT NULL,
              setting_value TEXT NOT NULL,
              updated_at TEXT NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_answers_session ON session_answers(session_id)",
        )
    }
}
