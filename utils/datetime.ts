export function isoNow(): string {
  return new Date().toISOString();
}

export function toDateKeyLocal(inputIso: string | Date): string {
  const d = typeof inputIso === 'string' ? new Date(inputIso) : inputIso;
  const year = d.getFullYear();
  const month = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatMmSs(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const mm = `${Math.floor(s / 60)}`.padStart(2, '0');
  const ss = `${s % 60}`.padStart(2, '0');
  return `${mm}:${ss}`;
}

export function startOfWeekDateKey(date = new Date()): string {
  // ISO-ish week: Monday start.
  const d = new Date(date);
  const day = d.getDay(); // 0 Sun ... 6 Sat
  const diff = (day === 0 ? -6 : 1) - day;
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return toDateKeyLocal(d);
}

export function addDaysDateKey(dateKey: string, days: number): string {
  const [y, m, d] = dateKey.split('-').map((x) => Number(x));
  const dt = new Date(y, m - 1, d);
  dt.setDate(dt.getDate() + days);
  return toDateKeyLocal(dt);
}

export function weekRange(date = new Date()): { from: string; to: string } {
  const from = startOfWeekDateKey(date);
  const to = addDaysDateKey(from, 6);
  return { from, to };
}

export function isSameDateKey(a: string, b: string): boolean {
  return a === b;
}

