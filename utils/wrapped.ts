import { getDailyAggregates, getStoolTypeDistribution, listDailyHealth } from '@/db/queries';
import { addDaysDateKey, formatMmSs, toDateKeyLocal, weekRange } from '@/utils/datetime';

export type WrappedCard = {
  id: string;
  kicker: string;
  title: string;
  body: string;
};

export type WeeklyWrapped = {
  from: string;
  to: string;
  cards: WrappedCard[];
};

function dateKeyToDow(dateKey: string): string {
  const [y, m, d] = dateKey.split('-').map((x) => Number(x));
  const dt = new Date(y, m - 1, d);
  return dt.toLocaleDateString(undefined, { weekday: 'long' });
}

function scoreDay(agg: { visits: number; avg_seconds: number }, health: any | null): number {
  let score = 0;
  if (agg.visits > 0) score += 1;
  if (agg.avg_seconds > 0 && agg.avg_seconds < 6 * 60) score += 1;
  if (health?.fiber === 'high') score += 1;
  if (health?.water === 'high') score += 1;
  if (health?.stress === 'low') score += 1;
  if (health?.sleep === '7-9' || health?.sleep === '9+') score += 1;
  return score;
}

export async function buildWeeklyWrapped(date: Date): Promise<WeeklyWrapped> {
  const { from, to } = weekRange(date);
  const daily = await getDailyAggregates(from, to);
  const health = await listDailyHealth(from, to);
  const healthByKey = new Map(health.map((h) => [h.date_key, h]));

  const totalVisits = daily.reduce((a, b) => a + b.visits, 0);
  const totalSeconds = daily.reduce((a, b) => a + b.total_seconds, 0);
  const avgSeconds = totalVisits ? Math.round(totalSeconds / totalVisits) : 0;

  const stoolDist = await getStoolTypeDistribution(from, to);
  const common = stoolDist[0]?.stool_type ?? null;

  const scored = daily.map((d) => ({
    date_key: d.date_key,
    score: scoreDay(d, healthByKey.get(d.date_key) ?? null),
    visits: d.visits,
  }));

  const best = scored.length ? scored.reduce((a, b) => (b.score > a.score ? b : a), scored[0]) : null;
  const worst = scored.length ? scored.reduce((a, b) => (b.score < a.score ? b : a), scored[0]) : null;

  const mostVisitsDay = daily.length ? daily.reduce((a, b) => (b.visits > a.visits ? b : a), daily[0]) : null;

  // Consistency: longest streak this week where either a session exists or daily health exists.
  const weekKeys = Array.from({ length: 7 }).map((_, idx) => addDaysDateKey(from, idx));
  const hasLog = (k: string) => daily.some((d) => d.date_key === k && d.visits > 0) || healthByKey.has(k);
  let bestStreak = 0;
  let streak = 0;
  weekKeys.forEach((k) => {
    if (hasLog(k)) {
      streak += 1;
      bestStreak = Math.max(bestStreak, streak);
    } else {
      streak = 0;
    }
  });

  // Previous week comparison
  const prevFrom = addDaysDateKey(from, -7);
  const prevTo = addDaysDateKey(from, -1);
  const prevDaily = await getDailyAggregates(prevFrom, prevTo);
  const prevVisits = prevDaily.reduce((a, b) => a + b.visits, 0);
  const prevSeconds = prevDaily.reduce((a, b) => a + b.total_seconds, 0);
  const prevAvg = prevVisits ? prevSeconds / prevVisits : 0;

  const delta = avgSeconds - prevAvg;
  const deltaText =
    prevVisits === 0
      ? 'This is your first week with enough data.'
      : delta > 15
        ? 'Your average session time increased.'
        : delta < -15
          ? 'Your average session time decreased.'
          : 'Your average session time stayed steady.';

  const cards: WrappedCard[] = [
    {
      id: 'total_visits',
      kicker: 'This week',
      title: `${totalVisits} total visits`,
      body: `Logged from ${from} to ${to}.`,
    },
    {
      id: 'total_time',
      kicker: 'Time spent',
      title: `${formatMmSs(totalSeconds)} total`,
      body: `Average session: ${formatMmSs(avgSeconds)}.`,
    },
    {
      id: 'common_stool',
      kicker: 'Most common',
      title: common ? `Bristol Type ${common}` : 'No stool data yet',
      body: common ? 'Based on logged bowel sessions this week.' : 'Log bowel sessions to unlock this slide.',
    },
    {
      id: 'best_day',
      kicker: 'Best day',
      title: best ? dateKeyToDow(best.date_key) : '—',
      body: best ? 'Most “healthy pattern” signals in your logs.' : 'Log a few days to unlock this slide.',
    },
    {
      id: 'consistent_day',
      kicker: 'Consistency',
      title: mostVisitsDay ? `Most consistent on ${dateKeyToDow(mostVisitsDay.date_key)}` : '—',
      body: mostVisitsDay ? 'Based on your highest-activity day this week.' : 'Log a few days to unlock this slide.',
    },
    {
      id: 'worst_day',
      kicker: 'Worst day',
      title: worst ? dateKeyToDow(worst.date_key) : '—',
      body: worst ? 'Most irregular signals in your logs.' : 'Log a few days to unlock this slide.',
    },
    {
      id: 'streak',
      kicker: 'Consistency',
      title: `${bestStreak}-day streak`,
      body: 'Days with any log (session or daily check-in).',
    },
    {
      id: 'trend',
      kicker: 'Trend',
      title: 'Week-over-week',
      body: deltaText,
    },
  ];

  return { from, to, cards };
}
