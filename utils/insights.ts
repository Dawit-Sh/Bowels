import { getDailyAggregates, getLastSessionOfType, getStoolTypeDistribution, insightsCacheGet, insightsCacheSet } from '@/db/queries';
import { addDaysDateKey, toDateKeyLocal } from '@/utils/datetime';

export type Insight = {
  id: string;
  severity: 'low' | 'medium' | 'high';
  title: string;
  body: string;
};

function dateKeyToDate(dateKey: string): Date {
  const [y, m, d] = dateKey.split('-').map((x) => Number(x));
  return new Date(y, m - 1, d);
}

function daysBetween(aKey: string, bKey: string): number {
  const a = dateKeyToDate(aKey).getTime();
  const b = dateKeyToDate(bKey).getTime();
  return Math.round((b - a) / (1000 * 60 * 60 * 24));
}

export async function computeInsights(): Promise<Insight[]> {
  const today = toDateKeyLocal(new Date());
  const cacheKey = `insights:${today}`;
  const cached = await insightsCacheGet<Insight[]>(cacheKey);
  if (cached) return cached;

  const from = addDaysDateKey(today, -13);
  const insights: Insight[] = [];

  const lastBowel = await getLastSessionOfType('bowel');
  if (lastBowel) {
    const days = daysBetween(lastBowel.date_key, today);
    if (days >= 3) {
      insights.push({
        id: 'constipation_alert',
        severity: days >= 5 ? 'high' : 'medium',
        title: 'Possible constipation pattern',
        body: `No bowel movement logged for ${days} days. If this is unusual for you, consider hydration/fiber and seek care if you have pain or red flags.`,
      });
    }
  }

  const stoolDist = await getStoolTypeDistribution(from, today);
  const totalStool = stoolDist.reduce((a, b) => a + b.count, 0);
  const count12 = stoolDist
    .filter((s) => s.stool_type === '1' || s.stool_type === '2')
    .reduce((a, b) => a + b.count, 0);
  const count67 = stoolDist
    .filter((s) => s.stool_type === '6' || s.stool_type === '7')
    .reduce((a, b) => a + b.count, 0);

  if (totalStool >= 3 && count12 / totalStool >= 0.34) {
    insights.push({
      id: 'low_fiber',
      severity: 'medium',
      title: 'More Type 1–2 this period',
      body: 'Stool types 1–2 can correlate with low fiber and dehydration. Consider gradual fiber increases and fluids.',
    });
  }

  if (totalStool >= 3 && count67 / totalStool >= 0.34) {
    insights.push({
      id: 'hydration_warning',
      severity: 'medium',
      title: 'More Type 6–7 this period',
      body: 'Stool types 6–7 can occur with low fiber, illness, or diet changes. Hydrate and monitor for persistence.',
    });
  }

  const daily = await getDailyAggregates(from, today);
  const longDays = daily.filter((d) => d.avg_seconds >= 8 * 60 && d.visits >= 2);
  if (longDays.length) {
    insights.push({
      id: 'long_sessions',
      severity: 'low',
      title: 'Longer sessions detected',
      body: 'Some days show longer average toilet time. If you’re straining or spending a long time often, consider discussing with a clinician.',
    });
  }

  const spikes = daily.filter((d) => d.visits >= 8);
  if (spikes.length) {
    insights.push({
      id: 'frequency_spike',
      severity: 'low',
      title: 'High-frequency days',
      body: `You had ${spikes.length} day(s) with unusually high visit counts. Look for triggers like stress, diet, or hydration changes.`,
    });
  }

  await insightsCacheSet(cacheKey, insights);
  return insights;
}

