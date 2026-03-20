import type { StoolType } from "./types";

export const stoolTypeMeta: Record<StoolType, { short: string; label: string; description: string; emoji: string }> = {
  1: { short: "Hard", label: "Type 1", description: "Hard pellets, difficult to pass", emoji: "🪨" },
  2: { short: "Lumpy", label: "Type 2", description: "Lumpy and compact", emoji: "🥔" },
  3: { short: "Firm", label: "Type 3", description: "Sausage-like with surface cracks", emoji: "🌭" },
  4: { short: "Smooth", label: "Type 4", description: "Smooth, soft, ideal", emoji: "🐍" },
  5: { short: "Soft", label: "Type 5", description: "Soft blobs, easy to pass", emoji: "☁️" },
  6: { short: "Mushy", label: "Type 6", description: "Mushy, fluffy pieces", emoji: "🌊" },
  7: { short: "Liquid", label: "Type 7", description: "Watery, no solid pieces", emoji: "💧" },
};

export const quickLogPresets: Array<{ stoolType: StoolType; title: string }> = [
  { stoolType: 1, title: "Hard" },
  { stoolType: 3, title: "Firm" },
  { stoolType: 4, title: "Smooth" },
  { stoolType: 6, title: "Mushy" },
];
