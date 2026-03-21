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
  { id: 1, daysAgo: 0, hour: 7, minute: 42, duration: 4, kind: "bowel" as const, stoolType: "4" },
  { id: 2, daysAgo: 1, hour: 8, minute: 5, duration: 5, kind: "bowel" as const, stoolType: "3" },
  { id: 3, daysAgo: 2, hour: 7, minute: 55, duration: 4, kind: "bowel" as const, stoolType: "4" },
  { id: 4, daysAgo: 3, hour: 8, minute: 18, duration: 3, kind: "bowel" as const, stoolType: "4" },
  { id: 5, daysAgo: 4, hour: 7, minute: 51, duration: 4, kind: "bowel" as const, stoolType: "3" },
  { id: 6, daysAgo: 5, hour: 8, minute: 11, duration: 5, kind: "bowel" as const, stoolType: "4" },
  { id: 7, daysAgo: 6, hour: 7, minute: 48, duration: 4, kind: "bowel" as const, stoolType: "4" },
  { id: 8, daysAgo: 7, hour: 8, minute: 8, duration: 3, kind: "urine" as const, stoolType: null },
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
    return [
      {
        id: 100 + index,
        sessionId: seed.id,
        key: "urine_urgency",
        value: "medium",
        createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
      },
    ];
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
      value: "none",
      createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
    },
    {
      id: 400 + index,
      sessionId: seed.id,
      key: "urgency",
      value: "3",
      createdAt: isoDaysAgo(seed.daysAgo, seed.hour, seed.minute),
    },
  ];
});

export const demoDailyHealth: DailyHealthRecord[] = new Array(5).fill(0).map((_, index) => {
  const day = isoDaysAgo(index, 9, 0).slice(0, 10);
  return {
    id: index + 1,
    day,
    water: index < 2 ? "Great" : "Okay",
    fiber: index < 3 ? "High" : "Medium",
    meals: "Balanced",
    stress: index === 4 ? "Medium" : "Low",
    sleep: index === 1 ? "Fair" : "Great",
    exercise: index % 2 === 0 ? "Light" : "Active",
    createdAt: `${day}T09:00:00.000Z`,
    updatedAt: `${day}T09:00:00.000Z`,
  };
});
