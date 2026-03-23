import type { AnalyticsSummary, DailyHealthRecord, InsightItem, SessionRecord, StoolType } from "./types";

type AnswerMap = Record<number, Record<string, string>>;

const lastDays = (days: number) => {
  return new Array(days).fill(0).map((_, idx) => {
    const date = new Date();
    date.setDate(date.getDate() - (days - 1 - idx));
    return date.toISOString().slice(0, 10);
  });
};

export function buildAnalytics(sessions: SessionRecord[], answers: AnswerMap, dailyHealth: DailyHealthRecord[]): AnalyticsSummary {
  const days = lastDays(14);
  const visitsPerDay = days.map((day) => ({
    day,
    count: sessions.filter((session) => session.startTime.slice(0, 10) === day).length,
  }));

  const totalTimePerDay = days.map((day) => ({
    day,
    totalSeconds: sessions.filter((session) => session.startTime.slice(0, 10) === day).reduce((sum, item) => sum + item.durationSeconds, 0),
  }));

  const averageDurationSeconds = sessions.length
    ? Math.round(sessions.reduce((sum, item) => sum + item.durationSeconds, 0) / sessions.length)
    : 0;

  const stoolDistribution = ([1, 2, 3, 4, 5, 6, 7] as StoolType[]).map((stoolType) => ({
    stoolType,
    count: sessions.filter((session) => answers[session.id]?.stool_type === String(stoolType)).length,
  }));

  const totalVisits = sessions.length;
  const totalDurationSeconds = sessions.reduce((sum, item) => sum + item.durationSeconds, 0);

  const fiberHeavyDays = dailyHealth.filter((day) => ["high", "great"].includes(day.fiber.toLowerCase())).length;
  const smoothDays = sessions.filter((session) => ["3", "4"].includes(answers[session.id]?.stool_type ?? "")).length;
  const fiberCorrelationLabel = fiberHeavyDays && smoothDays
    ? "Higher-fiber days trend toward Type 3-4 results."
    : "More logs are needed for a reliable fiber correlation.";

  const bowelTimes = sessions
    .filter((session) => session.kind === "bowel")
    .map((session) => new Date(session.startTime))
    .sort((a, b) => b.getTime() - a.getTime());
  
  const predictedNextTimeLabel = bowelTimes.length >= 3
    ? (() => {
        // Use weighted average: recent sessions have more weight
        const recentSessions = bowelTimes.slice(0, Math.min(10, bowelTimes.length));
        const weights = recentSessions.map((_, idx) => Math.pow(0.85, idx)); // Exponential decay
        const totalWeight = weights.reduce((sum, w) => sum + w, 0);
        
        const weightedMinutes = recentSessions.reduce((sum, date, idx) => {
          const minutes = date.getHours() * 60 + date.getMinutes();
          return sum + minutes * weights[idx];
        }, 0) / totalWeight;
        
        // Calculate time intervals between sessions to estimate next occurrence
        const intervals: number[] = [];
        for (let i = 0; i < recentSessions.length - 1; i++) {
          intervals.push((recentSessions[i].getTime() - recentSessions[i + 1].getTime()) / (1000 * 60 * 60));
        }
        const avgInterval = intervals.length > 0 ? intervals.reduce((sum, val) => sum + val, 0) / intervals.length : 24;
        
        const hours = Math.floor(weightedMinutes / 60);
        const minutes = Math.round(weightedMinutes % 60);
        const next = new Date(bowelTimes[0]);
        next.setTime(next.getTime() + avgInterval * 60 * 60 * 1000);
        next.setHours(hours, minutes, 0, 0);
        
        // If predicted time is in the past, move to next day
        if (next.getTime() < Date.now()) {
          next.setDate(next.getDate() + 1);
        }
        
        return next.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
      })()
    : bowelTimes.length >= 2 
      ? "Building pattern..."
      : "Need more data";

  return {
    visitsPerDay,
    totalTimePerDay,
    averageDurationSeconds,
    stoolDistribution,
    fiberCorrelationLabel,
    totalVisits,
    totalDurationSeconds,
    predictedNextTimeLabel,
    milestoneProgressDays: days.filter((day) => sessions.some((session) => session.startTime.slice(0, 10) === day)).length,
  };
}

export function buildInsights(sessions: SessionRecord[], answers: AnswerMap): InsightItem[] {
  const items: InsightItem[] = [];
  const bowelSessions = sessions.filter((session) => session.kind === "bowel");
  const today = new Date();
  const latestBowel = bowelSessions[0];

  if (!latestBowel || (today.getTime() - new Date(latestBowel.endTime).getTime()) / (1000 * 60 * 60 * 24) >= 3) {
    items.push({
      id: "no-bowel",
      title: "No bowel log in 3 days",
      body: "Your recent history suggests a gap in bowel activity. Consider hydration, movement, and fiber intake.",
      severity: "high",
    });
  }

  const lastStool = latestBowel ? Number(answers[latestBowel.id]?.stool_type ?? 0) : 0;
  if ([1, 2].includes(lastStool)) {
    items.push({
      id: "low-fiber",
      title: "Pattern suggests low fiber",
      body: "Recent bowel sessions trend toward Type 1-2, which may align with constipation or low fiber intake.",
      severity: "warning",
    });
  }
  if ([6, 7].includes(lastStool)) {
    items.push({
      id: "hydration-issue",
      title: "Hydration issue flagged",
      body: "Type 6-7 patterns can be associated with hydration imbalance or irritation.",
      severity: "warning",
    });
  }

  const longSessions = sessions.filter((session) => session.durationSeconds >= 12 * 60);
  if (longSessions.length >= 2) {
    items.push({
      id: "long-session",
      title: "Long sessions recurring",
      body: "Multiple sessions exceeded 12 minutes. Consider whether straining, urgency, or discomfort is increasing.",
      severity: "info",
    });
  }

  if (!items.length) {
    items.push({
      id: "steady",
      title: "Rhythm looks steady",
      body: "Recent logs do not show a strong warning pattern. Keep your routine consistent.",
      severity: "info",
    });
  }

  return items;
}
