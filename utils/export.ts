import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';

import { listAllDailyHealth, listAllSessionAnswers, listAllSettings, listSessions } from '@/db/queries';
import { isoNow } from '@/utils/datetime';

function csvEscape(v: unknown): string {
  const s = String(v ?? '');
  if (/[",\n]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

function toCsv(rows: Record<string, unknown>[]): string {
  if (rows.length === 0) return '';
  const headers = Object.keys(rows[0]);
  const lines = [headers.map(csvEscape).join(',')];
  rows.forEach((r) => {
    lines.push(headers.map((h) => csvEscape(r[h])).join(','));
  });
  return lines.join('\n');
}

export async function exportData(kind: 'json' | 'csv'): Promise<void> {
  const sessions = await listSessions(10000, 0);
  const answers = await listAllSessionAnswers();
  const dailyHealth = await listAllDailyHealth();
  const settings = await listAllSettings();

  const exportAt = isoNow();

  if (kind === 'json') {
    const payload = { exported_at: exportAt, sessions, session_answers: answers, daily_health: dailyHealth, settings };
    const path = `${FileSystem.cacheDirectory ?? ''}bowels-export-${Date.now()}.json`;
    await FileSystem.writeAsStringAsync(path, JSON.stringify(payload, null, 2));
    if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(path);
    return;
  }

  // CSV: one flat sessions file (answers as JSON per session).
  const bySession = new Map<number, Record<string, string>>();
  answers.forEach((a) => {
    if (!bySession.has(a.session_id)) bySession.set(a.session_id, {});
    bySession.get(a.session_id)![a.question_key] = a.value;
  });

  const rows = sessions.map((s) => ({
    id: s.id,
    start_time: s.start_time,
    end_time: s.end_time ?? '',
    duration_seconds: s.duration_seconds ?? '',
    type: s.type ?? '',
    date_key: s.date_key,
    answers_json: JSON.stringify(bySession.get(s.id) ?? {}),
  }));

  const csv = toCsv(rows);
  const path = `${FileSystem.cacheDirectory ?? ''}bowels-export-${Date.now()}.csv`;
  await FileSystem.writeAsStringAsync(path, csv);
  if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(path);
}
