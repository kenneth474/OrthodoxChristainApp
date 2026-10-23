/*
  Tests for the helpers in src/lib. Run them with:  npm test

  Vitest works like JUnit on the Java side: describe() groups tests, it()
  is one test, and expect(actual).toBe(expected) is like assertEquals.
  The tests run in "jsdom", a pretend browser (see vite.config.ts), so the
  HTML sanitizer can use DOMParser just as it does in a real browser.
*/
import { describe, expect, it } from 'vitest'
import type { PrayerBook } from '../types'
import { addDays, isIsoDate } from './dates'
import { forSpeaking, toChunks } from './listen'
import { expandItems, spokenLine, timesLabel } from './prayerBook'
import { htmlToSpeech, sanitizeHtml } from './sanitize'
import { plainScripture, toParagraphs } from './scripture'

describe('dates', () => {
  it('moves across the ends of months and years', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01')
    expect(addDays('2027-01-01', -1)).toBe('2026-12-31')
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29') // leap year
  })

  it('recognises YYYY-MM-DD dates only', () => {
    expect(isIsoDate('2026-12-25')).toBe(true)
    expect(isIsoDate('12/25/2026')).toBe(false)
    expect(isIsoDate(null)).toBe(false)
  })
})

describe('sanitizeHtml (protection against XSS)', () => {
  it('keeps simple formatting', () => {
    expect(sanitizeHtml('<p>Holy <em>Apostle</em> James</p>')).toBe('<p>Holy <em>Apostle</em> James</p>')
  })

  it('removes scripts and every attribute', () => {
    const dirty = '<p onclick="steal()">Hi<script>steal()</script><img src=x onerror="steal()"></p>'
    expect(sanitizeHtml(dirty)).toBe('<p>Hi</p>')
  })

  it('keeps the text of unknown tags', () => {
    expect(sanitizeHtml('<p><a href="https://example.com">a link</a></p>')).toBe('<p>a link</p>')
  })

  it('turns paragraphs into sentences for reading aloud', () => {
    expect(htmlToSpeech('<p>First</p><p>Second</p>')).toBe('First. Second')
  })
})

describe('reading aloud', () => {
  it('says "Saint" instead of "St."', () => {
    expect(forSpeaking('St. Paul and Sts Peter and Paul')).toBe('Saint Paul and Saints Peter and Paul')
  })

  it('splits long text into pieces of about 200 characters at sentence ends', () => {
    const sentence = 'Lord, have mercy on us all. ' // 28 characters
    const chunks = toChunks(sentence.repeat(20))
    expect(chunks.length).toBeGreaterThan(1)
    for (const chunk of chunks) {
      expect(chunk.length).toBeLessThanOrEqual(200)
      expect(chunk.endsWith('.')).toBe(true)
    }
  })
})

describe('scripture', () => {
  it('removes the translators’ brackets for speaking', () => {
    expect(plainScripture('And thou givest [them] their food')).toBe('And thou givest them their food')
  })

  it('starts paragraphs where they are marked', () => {
    const verses = [
      { number: '1', paragraphStart: true },
      { number: '2', paragraphStart: false },
      { number: '3', paragraphStart: true },
    ]
    expect(toParagraphs(verses).map((p) => p.length)).toEqual([2, 1])
  })

  it('puts each verse on its own line when nothing is marked (the Septuagint)', () => {
    const verses = [
      { number: '1', paragraphStart: false },
      { number: '2', paragraphStart: false },
    ]
    expect(toParagraphs(verses).length).toBe(2)
  })
})

describe('prayer book', () => {
  const book: PrayerBook = {
    title: 'Prayer Book',
    about: '',
    sections: [
      { id: 'a', title: 'Beginning Prayers', items: [{ lines: ['Glory to Thee'] }] },
      { id: 'b', title: 'Morning', items: [{ include: 'a' }, { title: 'Psalm', lines: ['…'] }] },
      { id: 'loop', title: 'Loop', items: [{ include: 'loop' }] },
    ],
  }

  it('includes other sections, titled with the section’s name', () => {
    const items = expandItems(book, book.sections[1].items)
    expect(items.map((i) => i.title)).toEqual(['Beginning Prayers', 'Psalm'])
  })

  it('never includes a section inside itself forever', () => {
    expect(expandItems(book, book.sections[2].items)).toEqual([])
  })

  it('reads "three times" prayers three times and skips instructions', () => {
    expect(spokenLine({ text: 'Lord, have mercy.', times: 3 })).toHaveLength(3)
    expect(spokenLine({ rubric: 'Prostration' })).toEqual([])
    expect(timesLabel(3)).toBe('Three times')
  })
})
