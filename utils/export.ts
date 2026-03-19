import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import * as DocumentPicker from 'expo-document-picker';
import { Alert } from 'react-native';

import { getDb } from '@/db/client';

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

export async function importData(): Promise<void> {
  try {
    const res = await DocumentPicker.getDocumentAsync({
      type: 'application/json',
      copyToCacheDirectory: true,
    });
    if (res.canceled) return;

    const uri = res.assets[0].uri;
    const contents = await FileSystem.readAsStringAsync(uri);
    const data = JSON.parse(contents);

    if (!data.exported_at || !Array.isArray(data.sessions) || !Array.isArray(data.session_answers)) {
      Alert.alert('Invalid Backup', 'The selected JSON is not a valid Bowels backup file.');
      return;
    }

    Alert.alert(
      'Restore Data',
      `This will completely replace all existing data with the backup from ${new Date(data.exported_at).toLocaleDateString()}. Proceed?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Restore', style: 'destructive', onPress: async () => {
            try {
              const db = await getDb();
              await db.withTransactionAsync(async () => {
                await db.runAsync('DELETE FROM sessions;');
                await db.runAsync('DELETE FROM session_answers;');
                await db.runAsync('DELETE FROM daily_health;');

                for (const s of data.sessions) {
                  await db.runAsync(
                    'INSERT INTO sessions(id, start_time, end_time, duration_seconds, type, date_key, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?)',
                    [s.id, s.start_time, s.end_time || null, s.duration_seconds || null, s.type || null, s.date_key, isoNow(), isoNow()]
                  );
                }

                for (const a of data.session_answers) {
                  await db.runAsync(
                    'INSERT INTO session_answers(id, session_id, question_key, value, created_at) VALUES(?,?,?,?,?)',
                    [a.id, a.session_id, a.question_key, a.value, isoNow()]
                  );
                }

                if (Array.isArray(data.daily_health)) {
                  for (const h of data.daily_health) {
                    await db.runAsync(
                      'INSERT INTO daily_health(date_key, water, fiber, meals, stress, sleep, exercise, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?)',
                      [h.date_key, h.water || null, h.fiber || null, h.meals || null, h.stress || null, h.sleep || null, h.exercise || null, isoNow(), isoNow()]
                    );
                  }
                }
              });
              Alert.alert('Success', 'Data restored successfully!');
            } catch (err: any) {
              Alert.alert('Restore Failed', err.message);
            }
          }
        }
      ]
    );
  } catch (err: any) {
    Alert.alert('Import Failed', err.message);
  }
}
