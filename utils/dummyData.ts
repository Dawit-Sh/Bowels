import { getDb } from '@/db/client';
import { isoNow, toDateKeyLocal } from '@/utils/datetime';

const BRISTOL_TYPES = ['1', '2', '3', '4', '5', '6', '7'];

export async function generateDummyData() {
  const db = await getDb();
  await db.withTransactionAsync(async () => {
    // Generate data for the past 90 days
    const nowMs = Date.now();
    const dayMs = 24 * 60 * 60 * 1000;
    
    for (let i = 0; i <= 90; i++) {
        const dateMs = nowMs - i * dayMs;
        const d = new Date(dateMs);
        const dateKey = toDateKeyLocal(d);
        
        // Random 0 to 3 sessions per day
        const numSessions = Math.floor(Math.random() * 4);
        
        for (let j = 0; j < numSessions; j++) {
            // Random hour between 6am and 10pm
            const hour = 6 + Math.floor(Math.random() * 16);
            const min = Math.floor(Math.random() * 60);
            
            d.setHours(hour, min, 0, 0);
            const startIso = d.toISOString();
            
            const durationSec = 30 + Math.floor(Math.random() * 300);
            const endd = new Date(d.getTime() + durationSec * 1000);
            const endIso = endd.toISOString();
            
            const typeVar = Math.random() > 0.4 ? 'bowel' : 'urine';
            
            const res = await db.runAsync(
              'INSERT INTO sessions(start_time, end_time, duration_seconds, type, date_key, created_at, updated_at) VALUES(?,?,?,?,?,?,?)',
              [startIso, endIso, durationSec, typeVar, dateKey, isoNow(), isoNow()]
            );
            
            const sessionId = res.lastInsertRowId;
            
            if (typeVar === 'bowel') {
                const bType = BRISTOL_TYPES[Math.floor(Math.random() * BRISTOL_TYPES.length)];
                await db.runAsync(
                    'INSERT INTO session_answers(session_id, question_key, value, created_at) VALUES(?,?,?,?)',
                    [sessionId, 'bowel_stool_type', bType, isoNow()]
                );
            }
        }
        
        // Populate dummy daily health for 60% of days
        if (Math.random() > 0.4) {
            await db.runAsync(
                'INSERT OR IGNORE INTO daily_health(date_key, water, fiber, meals, stress, sleep, exercise, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?)',
                [dateKey, 'medium', 'medium', 'good', 'medium', '7-9', 'moderate', isoNow(), isoNow()]
            );
        }
    }
  });
}
