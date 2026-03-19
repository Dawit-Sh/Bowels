import type { SessionType } from '@/db/queries';

export type Question = {
  key: string;
  title: string;
  subtitle?: string;
  options: { key: string; label: string; hint?: string }[];
  columns?: 1 | 2 | 3;
};

export type Answers = Record<string, string | undefined>;

export function buildQuestionFlow(sessionType: SessionType | null, seed: number): Question[] {
  const first: Question = {
    key: 'session_type',
    title: 'What was this session?',
    options: [
      { key: 'urine', label: 'Urine' },
      { key: 'bowel', label: 'Bowel movement' },
    ],
    columns: 2 as const,
  };

  if (!sessionType) return [first];

  if (sessionType === 'urine') {
    const base: Question[] = [
      first,
      {
        key: 'urine_urgency',
        title: 'Urgency',
        options: [
          { key: 'low', label: 'Low' },
          { key: 'medium', label: 'Medium' },
          { key: 'high', label: 'High' },
        ],
        columns: 3 as const,
      },
      {
        key: 'urine_color',
        title: 'Color',
        options: [
          { key: 'light', label: 'Light' },
          { key: 'normal', label: 'Normal' },
          { key: 'dark', label: 'Dark' },
        ],
        columns: 3 as const,
      },
    ];

    const adaptivePool: Question[] = [
      {
        key: 'bloating',
        title: 'Bloating',
        options: [
          { key: 'none', label: 'None' },
          { key: 'mild', label: 'Mild' },
          { key: 'severe', label: 'Severe' },
        ],
        columns: 3 as const,
      },
      {
        key: 'straining',
        title: 'Straining',
        options: [
          { key: 'no', label: 'No' },
          { key: 'yes', label: 'Yes' },
        ],
        columns: 2 as const,
      },
      {
        key: 'gas',
        title: 'Gas',
        options: [
          { key: 'low', label: 'Low' },
          { key: 'medium', label: 'Medium' },
          { key: 'high', label: 'High' },
        ],
        columns: 3 as const,
      },
    ];

    // Rotate adaptives deterministically; keep max 5 questions total.
    const pick1 = adaptivePool[seed % adaptivePool.length];
    const pick2 = adaptivePool[(seed + 1) % adaptivePool.length];
    return [...base, pick1, pick2].slice(0, 5);
  }

  // Bowel: fill up to 5 with high-signal questions.
  return [
    first,
    {
      key: 'bowel_stool_type',
      title: 'Stool type (Bristol)',
      subtitle: 'Tap 1–7',
      options: [
        { key: '1', label: '1', hint: 'Hard' },
        { key: '2', label: '2', hint: 'Lumpy' },
        { key: '3', label: '3', hint: 'Cracked' },
        { key: '4', label: '4', hint: 'Smooth' },
        { key: '5', label: '5', hint: 'Soft blobs' },
        { key: '6', label: '6', hint: 'Mushy' },
        { key: '7', label: '7', hint: 'Watery' },
      ],
      columns: 3 as const,
    },
    {
      key: 'bowel_pain',
      title: 'Pain',
      options: [
        { key: 'none', label: 'None' },
        { key: 'mild', label: 'Mild' },
        { key: 'moderate', label: 'Moderate' },
        { key: 'severe', label: 'Severe' },
      ],
      columns: 2 as const,
    },
    {
      key: 'bowel_blood',
      title: 'Blood',
      options: [
        { key: 'no', label: 'No' },
        { key: 'yes', label: 'Yes' },
      ],
      columns: 2 as const,
    },
    {
      key: 'bowel_urgency',
      title: 'Urgency',
      options: [
        { key: '1', label: '1' },
        { key: '2', label: '2' },
        { key: '3', label: '3' },
        { key: '4', label: '4' },
        { key: '5', label: '5' },
      ],
      columns: 3 as const,
    },
  ].slice(0, 5);
}
