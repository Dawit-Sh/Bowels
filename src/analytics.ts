import type { AnalyticsSummary, DailyHealthRecord, InsightItem, SessionRecord, StoolType, WeeklyWrappedSummary } from "./types";

type AnswerMap = Record<number, Record<string, string>>;

const lastDays = (days: number) => {
  return new Array(days).fill(0).map((_, idx) => {
    const date = new Date();
    date.setDate(date.getDate() - (days - 1 - idx));
    return date.toISOString().slice(0, 10);
  });
};

const createDayWindow = (days: number) =>
  new Array(days).fill(0).map((_, idx) => {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() - (days - 1 - idx));
    return date.toISOString().slice(0, 10);
  });

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
        // Advanced prediction using multiple factors
        const recentSessions = bowelTimes.slice(0, Math.min(21, bowelTimes.length));
        
        // 1. Calculate time intervals between consecutive sessions
        const intervals: number[] = [];
        for (let i = 0; i < recentSessions.length - 1; i++) {
          const hoursDiff = (recentSessions[i].getTime() - recentSessions[i + 1].getTime()) / (1000 * 60 * 60);
          intervals.push(hoursDiff);
        }
        
        // 2. Detect patterns: daily, every-other-day, or custom frequency
        const avgInterval = intervals.length > 0 
          ? intervals.reduce((sum, val) => sum + val, 0) / intervals.length 
          : 24;
        
        // Weight recent intervals more heavily
        const weightedInterval = intervals.length > 0
          ? intervals.reduce((sum, val, idx) => sum + val * Math.pow(0.85, idx), 0) / 
            intervals.reduce((sum, _, idx) => sum + Math.pow(0.85, idx), 0)
          : 24;
        
        // 3. Analyze day-of-week patterns
        const dayOfWeekData: Record<number, { times: number[], count: number }> = {};
        recentSessions.forEach((date) => {
          const dayOfWeek = date.getDay();
          const minutesSinceMidnight = date.getHours() * 60 + date.getMinutes();
          if (!dayOfWeekData[dayOfWeek]) {
            dayOfWeekData[dayOfWeek] = { times: [], count: 0 };
          }
          dayOfWeekData[dayOfWeek].times.push(minutesSinceMidnight);
          dayOfWeekData[dayOfWeek].count++;
        });
        
        // 4. Analyze time-of-day patterns (morning, afternoon, evening)
        const timeOfDayBuckets = { morning: 0, afternoon: 0, evening: 0, night: 0 };
        recentSessions.forEach((date) => {
          const hour = date.getHours();
          if (hour >= 5 && hour < 12) timeOfDayBuckets.morning++;
          else if (hour >= 12 && hour < 17) timeOfDayBuckets.afternoon++;
          else if (hour >= 17 && hour < 22) timeOfDayBuckets.evening++;
          else timeOfDayBuckets.night++;
        });
        
        // 5. Calculate consistency score (lower variance = more predictable)
        const variance = intervals.length > 1
          ? intervals.reduce((sum, val) => sum + Math.pow(val - avgInterval, 2), 0) / intervals.length
          : 0;
        const consistencyScore = Math.max(0, 1 - (Math.sqrt(variance) / avgInterval));
        
        // 6. Predict next occurrence time
        const lastSession = bowelTimes[0];
        const predictedNext = new Date(lastSession);
        
        // Use weighted interval for better accuracy
        predictedNext.setTime(predictedNext.getTime() + weightedInterval * 60 * 60 * 1000);
        
        // 7. Adjust based on day-of-week pattern if strong pattern exists
        const predictedDayOfWeek = predictedNext.getDay();
        const dayData = dayOfWeekData[predictedDayOfWeek];
        
        if (dayData && dayData.count >= 2) {
          // Strong day-of-week pattern exists
          const dayAvgMinutes = dayData.times.reduce((sum, m) => sum + m, 0) / dayData.times.length;
          
          // Calculate standard deviation for this day
          const dayVariance = dayData.times.reduce((sum, m) => sum + Math.pow(m - dayAvgMinutes, 2), 0) / dayData.times.length;
          const dayConsistency = Math.max(0, 1 - (Math.sqrt(dayVariance) / 60)); // Normalize by 1 hour
          
          // If this day has consistent timing, use it
          if (dayConsistency > 0.7) {
            const hours = Math.floor(dayAvgMinutes / 60);
            const minutes = Math.round(dayAvgMinutes % 60);
            predictedNext.setHours(hours, minutes, 0, 0);
          }
        } else {
          // No strong day pattern, use overall time-of-day preference
          const preferredTimeOfDay = Object.entries(timeOfDayBuckets).reduce((max, [key, val]) => 
            val > max.val ? { key, val } : max, { key: 'morning', val: 0 }
          ).key;
          
          // Calculate average time for preferred time-of-day
          const preferredTimes = recentSessions
            .filter((date) => {
              const hour = date.getHours();
              if (preferredTimeOfDay === 'morning') return hour >= 5 && hour < 12;
              if (preferredTimeOfDay === 'afternoon') return hour >= 12 && hour < 17;
              if (preferredTimeOfDay === 'evening') return hour >= 17 && hour < 22;
              return hour >= 22 || hour < 5;
            })
            .map((date) => date.getHours() * 60 + date.getMinutes());
          
          if (preferredTimes.length > 0) {
            const avgPreferredMinutes = preferredTimes.reduce((sum, m) => sum + m, 0) / preferredTimes.length;
            const hours = Math.floor(avgPreferredMinutes / 60);
            const minutes = Math.round(avgPreferredMinutes % 60);
            predictedNext.setHours(hours, minutes, 0, 0);
          }
        }
        
        // 8. If predicted time is in the past, move forward by interval
        while (predictedNext.getTime() < Date.now()) {
          predictedNext.setTime(predictedNext.getTime() + weightedInterval * 60 * 60 * 1000);
        }
        
        // 9. Format output with confidence indicator
        const dayName = predictedNext.toLocaleDateString(undefined, { weekday: 'short' });
        const timeStr = predictedNext.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
        
        // Add confidence indicator based on consistency
        const confidenceEmoji = consistencyScore > 0.8 ? "🎯" : consistencyScore > 0.6 ? "📊" : "🔮";
        
        return `${confidenceEmoji} ${dayName} ${timeStr}`;
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
    milestoneProgressDays: getAllUniqueDaysWithSessions(sessions),
  };
}

export function buildWeeklyWrapped(sessions: SessionRecord[], answers: AnswerMap): WeeklyWrappedSummary {
  const weekDays = createDayWindow(7);
  const weekSet = new Set(weekDays);
  const weeklyBowelSessions = sessions.filter((session) => session.kind === "bowel" && weekSet.has(session.startTime.slice(0, 10)));

  const visitsPerDay = weekDays.map((day) => ({
    day,
    count: weeklyBowelSessions.filter((session) => session.startTime.slice(0, 10) === day).length,
  }));

  const stoolDistribution = ([1, 2, 3, 4, 5, 6, 7] as StoolType[]).map((stoolType) => ({
    stoolType,
    count: weeklyBowelSessions.filter((session) => answers[session.id]?.stool_type === String(stoolType)).length,
  }));

  const totalVisits = weeklyBowelSessions.length;
  const totalDurationSeconds = weeklyBowelSessions.reduce((sum, item) => sum + item.durationSeconds, 0);
  const averageDurationSeconds = totalVisits ? Math.round(totalDurationSeconds / totalVisits) : 0;
  const mostCommon = stoolDistribution.reduce<{ stoolType: StoolType | null; count: number }>(
    (bestType, item) => (item.count > bestType.count ? item : bestType),
    { stoolType: null, count: 0 }
  );
  const bestDay = visitsPerDay.reduce((best, item) => (item.count > best.count ? item : best), visitsPerDay[0] ?? { day: "", count: 0 });
  const worstDay = visitsPerDay.reduce((worst, item) => (item.count < worst.count ? item : worst), visitsPerDay[0] ?? { day: "", count: 0 });

  return {
    totalVisits,
    averageDurationSeconds,
    totalDurationSeconds,
    visitsPerDay,
    stoolDistribution,
    mostCommonStoolType: mostCommon.count > 0 ? mostCommon.stoolType : null,
    bestDay,
    worstDay,
  };
}

function getAllUniqueDaysWithSessions(sessions: SessionRecord[]): number {
  const uniqueDays = new Set<string>();
  sessions.forEach((session) => {
    uniqueDays.add(session.startTime.slice(0, 10));
  });
  return uniqueDays.size;
}

export function buildInsights(sessions: SessionRecord[], answers: AnswerMap, dailyHealth?: DailyHealthRecord[]): InsightItem[] {
  const items: InsightItem[] = [];
  const bowelSessions = sessions.filter((session) => session.kind === "bowel");
  const today = new Date();
  const latestBowel = bowelSessions[0];

  // Check for gaps in bowel activity
  if (!latestBowel || (today.getTime() - new Date(latestBowel.endTime).getTime()) / (1000 * 60 * 60 * 24) >= 3) {
    items.push({
      id: "no-bowel",
      title: "No bowel log in 3 days",
      body: "Your recent history suggests a gap in bowel activity. Consider hydration, movement, and fiber intake.",
      severity: "high",
    });
  }

  // Analyze stool type patterns
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

  // Check for long sessions
  const longSessions = sessions.filter((session) => session.durationSeconds >= 12 * 60);
  if (longSessions.length >= 2) {
    items.push({
      id: "long-session",
      title: "Long sessions recurring",
      body: "Multiple sessions exceeded 12 minutes. Consider whether straining, urgency, or discomfort is increasing.",
      severity: "info",
    });
  }

  // Analyze pain patterns
  const recentPainSessions = bowelSessions.slice(0, 5).filter((session) => 
    ["moderate", "severe"].includes(answers[session.id]?.pain ?? "")
  );
  if (recentPainSessions.length >= 2) {
    items.push({
      id: "pain-pattern",
      title: "Pain pattern detected",
      body: "Multiple recent sessions with moderate to severe pain. Consider consulting a healthcare provider if this persists.",
      severity: "high",
    });
  }

  // Analyze blood presence
  const recentBloodSessions = bowelSessions.slice(0, 7).filter((session) => 
    answers[session.id]?.blood === "true"
  );
  if (recentBloodSessions.length >= 1) {
    items.push({
      id: "blood-detected",
      title: "Blood detected in recent sessions",
      body: "Blood in stool should be evaluated by a healthcare provider, especially if recurring.",
      severity: "high",
    });
  }

  // Analyze urgency patterns
  const highUrgencySessions = bowelSessions.slice(0, 7).filter((session) => 
    Number(answers[session.id]?.urgency ?? 0) >= 4
  );
  if (highUrgencySessions.length >= 3) {
    items.push({
      id: "high-urgency",
      title: "Frequent high urgency",
      body: "Multiple sessions with high urgency (4-5). This may indicate digestive sensitivity or stress.",
      severity: "warning",
    });
  }

  // Analyze adaptive tags (bloating, straining, gas)
  const recentTaggedSessions = bowelSessions.slice(0, 7);
  const tagCounts = { bloating: 0, straining: 0, gas: 0 };
  recentTaggedSessions.forEach((session) => {
    const tags = (answers[session.id]?.adaptive_tags ?? "").split(",").filter(Boolean);
    tags.forEach((tag) => {
      if (tag in tagCounts) tagCounts[tag as keyof typeof tagCounts]++;
    });
  });

  if (tagCounts.straining >= 3) {
    items.push({
      id: "straining-pattern",
      title: "Frequent straining detected",
      body: "Straining during bowel movements may indicate low fiber, dehydration, or other digestive issues.",
      severity: "warning",
    });
  }

  if (tagCounts.bloating >= 3) {
    items.push({
      id: "bloating-pattern",
      title: "Recurring bloating",
      body: "Frequent bloating may be related to diet, food sensitivities, or eating habits. Consider tracking trigger foods.",
      severity: "info",
    });
  }

  // Analyze daily health correlations if available
  if (dailyHealth && dailyHealth.length > 0) {
    const recentHealth = dailyHealth.slice(0, 7);
    
    // Check caffeine correlation
    const highCaffeineDays = recentHealth.filter(h => 
      ["High", "Very High", "Excessive"].includes(h.caffeine)
    ).length;
    const looseStools = bowelSessions.slice(0, 7).filter(s => 
      [6, 7].includes(Number(answers[s.id]?.stool_type ?? 0))
    ).length;
    
    if (highCaffeineDays >= 3 && looseStools >= 2) {
      items.push({
        id: "caffeine-correlation",
        title: "Caffeine may be affecting digestion",
        body: "High caffeine intake correlates with looser stools. Consider reducing intake to see if symptoms improve.",
        severity: "info",
      });
    }

    // Check stress correlation
    const highStressDays = recentHealth.filter(h => 
      ["High", "Very High", "Extreme"].includes(h.stress)
    ).length;
    const urgentSessions = bowelSessions.slice(0, 7).filter(s => 
      Number(answers[s.id]?.urgency ?? 0) >= 4
    ).length;
    
    if (highStressDays >= 3 && urgentSessions >= 2) {
      items.push({
        id: "stress-correlation",
        title: "Stress may be impacting digestion",
        body: "High stress levels correlate with increased urgency. Stress management techniques may help.",
        severity: "info",
      });
    }

    // Check sleep correlation
    const poorSleepDays = recentHealth.filter(h => 
      ["Poor", "Very Poor", "None"].includes(h.sleep)
    ).length;
    
    if (poorSleepDays >= 4) {
      items.push({
        id: "sleep-pattern",
        title: "Poor sleep pattern detected",
        body: "Consistent poor sleep can affect digestive health. Aim for 7-9 hours of quality sleep.",
        severity: "warning",
      });
    }

    // Check alcohol correlation
    const alcoholDays = recentHealth.filter(h => 
      !["None", ""].includes(h.alcohol)
    ).length;
    const irregularStools = bowelSessions.slice(0, 7).filter(s => {
      const type = Number(answers[s.id]?.stool_type ?? 0);
      return type <= 2 || type >= 6;
    }).length;
    
    if (alcoholDays >= 2 && irregularStools >= 2) {
      items.push({
        id: "alcohol-correlation",
        title: "Alcohol may be affecting regularity",
        body: "Alcohol consumption correlates with irregular stool patterns. Consider moderating intake.",
        severity: "info",
      });
    }

    // Check mood patterns
    const negativeMoodDays = recentHealth.filter(h => 
      ["Sad", "Anxious", "Stressed", "Angry"].includes(h.mood)
    ).length;
    
    if (negativeMoodDays >= 4) {
      items.push({
        id: "mood-pattern",
        title: "Mood affecting wellness",
        body: "Negative mood patterns can impact digestive health. Consider stress management or speaking with a professional.",
        severity: "info",
      });
    }
  }

  // Default positive insight if no issues
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
