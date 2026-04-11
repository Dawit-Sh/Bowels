import type { DailyHealthRecord, SessionAnswerRecord, SessionRecord } from "./types";

const now = new Date();

function isoDaysAgo(daysAgo: number, hour: number, minute: number) {
  const date = new Date(now);
  date.setDate(now.getDate() - daysAgo);
  date.setHours(hour, minute, 0, 0);
  return date.toISOString();
}

function endFrom(start: string, minutes: number) {
  return new Date(new Date(start).getTime() + minutes * 60 * 1000).toISOString();
}

const demoSessionSeeds = [
  // Day 0 (today)
  { id: 1, daysAgo: 0, hour: 7, minute: 42, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "3", blood: false },
  // Day 1
  { id: 2, daysAgo: 1, hour: 8, minute: 5, duration: 5, kind: "bowel" as const, stoolType: "3", pain: "none", urgency: "2", blood: false },
  { id: 3, daysAgo: 1, hour: 14, minute: 30, duration: 3, kind: "bowel" as const, stoolType: "4", pain: "mild", urgency: "4", blood: false },
  // Day 2
  { id: 4, daysAgo: 2, hour: 7, minute: 55, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "3", blood: false },
  // Day 3
  { id: 5, daysAgo: 3, hour: 8, minute: 18, duration: 3, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "2", blood: false },
  { id: 6, daysAgo: 3, hour: 16, minute: 45, duration: 6, kind: "bowel" as const, stoolType: "5", pain: "mild", urgency: "5", blood: false },
  // Day 4
  { id: 7, daysAgo: 4, hour: 7, minute: 51, duration: 4, kind: "bowel" as const, stoolType: "3", pain: "none", urgency: "3", blood: false },
  // Day 5
  { id: 8, daysAgo: 5, hour: 8, minute: 11, duration: 5, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "2", blood: false },
  // Day 6
  { id: 9, daysAgo: 6, hour: 7, minute: 48, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "3", blood: false },
  { id: 10, daysAgo: 6, hour: 19, minute: 20, duration: 3, kind: "bowel" as const, stoolType: "5", pain: "none", urgency: "4", blood: false },
  // Day 7
  { id: 11, daysAgo: 7, hour: 8, minute: 8, duration: 3, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "2", blood: false },
  // Day 8
  { id: 12, daysAgo: 8, hour: 9, minute: 15, duration: 7, kind: "bowel" as const, stoolType: "2", pain: "moderate", urgency: "5", blood: false },
  // Day 9
  { id: 13, daysAgo: 9, hour: 7, minute: 35, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "3", blood: false },
  // Day 10
  { id: 14, daysAgo: 10, hour: 8, minute: 22, duration: 5, kind: "bowel" as const, stoolType: "3", pain: "none", urgency: "2", blood: false },
  { id: 15, daysAgo: 10, hour: 15, minute: 10, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "mild", urgency: "4", blood: false },
  // Day 11
  { id: 16, daysAgo: 11, hour: 7, minute: 58, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "3", blood: false },
  // Day 12
  { id: 17, daysAgo: 12, hour: 8, minute: 40, duration: 6, kind: "bowel" as const, stoolType: "5", pain: "mild", urgency: "5", blood: false },
  // Day 13
  { id: 18, daysAgo: 13, hour: 7, minute: 25, duration: 3, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "2", blood: false },
  // Day 14
  { id: 19, daysAgo: 14, hour: 8, minute: 12, duration: 5, kind: "bowel" as const, stoolType: "3", pain: "none", urgency: "3", blood: false },
  { id: 20, daysAgo: 14, hour: 20, minute: 5, duration: 4, kind: "bowel" as const, stoolType: "4", pain: "none", urgency: "4", blood: false },
];

export const demoSessions: SessionRecord[] = demoSessionSeeds.map((seed) => {
  const startTime = isoDaysAgo(seed.daysAgo, seed.hour, seed.minute);
  return {
    id: seed.id,
    kind: seed.kind,
    startTime,
    endTime: endFrom(startTime, seed.duration),
    durationSeconds: seed.duration * 60,
    createdAt: startTime,
  };
});

export const demoSessionAnswers: SessionAnswerRecord[] = demoSessionSeeds.flatMap((seed, index) => {
  if (!seed.stoolType) {
    return [];
  }

  return [
    {
      id: 200 + index,
      sessionId: seed.id,
      key: "stool_type",
      value: seed.stoolType,
      createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
    },
    {
      id: 300 + index,
      sessionId: seed.id,
      key: "pain",
      value: seed.pain,
      createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
    },
    {
      id: 400 + index,
      sessionId: seed.id,
      key: "urgency",
      value: seed.urgency,
      createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
    },
    {
      id: 500 + index,
      sessionId: seed.id,
      key: "blood",
      value: seed.blood ? "true" : "false",
      createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
    },
  ];
});

export const demoDailyHealth: DailyHealthRecord[] = new Array(15).fill(0).map((_, index) => {
  const day = isoDaysAgo(index, 9, 0).slice(0, 10);
  
  // Create varied but realistic patterns
  const sleepQuality = index % 5 === 0 ? "Poor" : index % 3 === 0 ? "Fair" : "Great";
  const fiberIntake = index % 4 === 0 ? "Low" : index % 2 === 0 ? "High" : "Medium";
  const waterIntake = index % 6 === 0 ? "Low" : index % 3 === 0 ? "Okay" : "Great";
  const stressLevel = index % 7 === 0 ? "High" : index % 4 === 0 ? "Medium" : "Low";
  const caffeineLevel = index % 5 === 0 ? "High" : index % 3 === 0 ? "Medium" : index % 2 === 0 ? "Low" : "None";
  const alcoholLevel = index % 8 === 0 ? "Moderate" : index % 10 === 0 ? "Light" : "None";
  const exerciseLevel = index % 3 === 0 ? "None" : index % 2 === 0 ? "Light" : "Active";
  const moodState = index % 7 === 0 ? "Stressed" : index % 5 === 0 ? "Anxious" : index % 4 === 0 ? "Neutral" : index % 3 === 0 ? "Sad" : "Happy";
  
  return {
    id: index + 1,
    day,
    water: waterIntake,
    fiber: fiberIntake,
    meals: "Balanced",
    stress: stressLevel,
    sleep: sleepQuality,
    exercise: exerciseLevel,
    caffeine: caffeineLevel,
    alcohol: alcoholLevel,
    medication: index % 9 === 0 ? "OTC" : "None",
    mood: moodState,
    createdAt: `${day}T09:00:00.000Z`,
    updatedAt: `${day}T09:00:00.000Z`,
  };
});
