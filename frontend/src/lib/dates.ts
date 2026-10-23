/*
  dates.ts: small helpers for "YYYY-MM-DD" dates, the format our backend,
  the date picker and the ?date= address all use.
*/

/** A JavaScript Date -> "2026-10-23" (in this device's own time zone). */
export function toIso(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0') // months start at 0 in JavaScript
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** "2026-10-23" -> a Date at midnight of that day. */
export function fromIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** "2026-10-23" plus 1 day -> "2026-10-24" (handles month and year ends). */
export function addDays(iso: string, days: number): string {
  const date = fromIso(iso)
  date.setDate(date.getDate() + days)
  return toIso(date)
}

/** True for text like "2026-10-23". */
export function isIsoDate(text: string | null): text is string {
  return text !== null && /^\d{4}-\d{2}-\d{2}$/.test(text)
}

/** "2026-10-23" -> "Friday, October 23, 2026". */
export function longDate(iso: string): string {
  return fromIso(iso).toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}
