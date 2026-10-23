/*
  prayerBook.ts: helpers for the prayer book data (public/data/prayers.json).
*/
import type { PrayerBook, PrayerItem, PrayerLine } from '../types'

/**
 * Replaces each { "include": "trisagion" } with that section's items, so
 * Morning Prayers contains the Beginning Prayers, the Creed and so on.
 * "seen" stops a section from (accidentally) including itself forever.
 */
export function expandItems(book: PrayerBook, items: PrayerItem[], seen: Set<string> = new Set()): PrayerItem[] {
  return items.flatMap((item) => {
    if (!item.include) return [item]
    if (seen.has(item.include)) return []
    const section = book.sections.find((s) => s.id === item.include)
    if (!section) return []
    const inner = expandItems(book, section.items, new Set([...seen, item.include]))
    // The included section's title becomes the heading of its first item.
    return inner.map((innerItem, i) => (i === 0 && !innerItem.title ? { ...innerItem, title: section.title } : innerItem))
  })
}

const TIMES: Record<number, string> = { 2: 'Twice', 3: 'Three times', 12: 'Twelve times' }

/** 3 -> "Three times" */
export function timesLabel(times: number): string {
  return TIMES[times] ?? `${times} times`
}

/** What a line says aloud: a prayer said three times is read three times; instructions aren't read. */
export function spokenLine(line: PrayerLine): string[] {
  if (typeof line === 'string') return [line]
  if ('rubric' in line) return []
  return Array.from({ length: line.times }, () => line.text)
}
