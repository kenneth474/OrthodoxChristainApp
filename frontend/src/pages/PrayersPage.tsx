/*
  PrayersPage (the Prayers tab): the prayer book.

      /prayers            -> the list of sections
      /prayers/morning    -> Morning Prayers

  The prayers live in a data file, public/data/prayers.json, so adding or
  correcting a prayer never means changing this code. Each section has
  "items": prayer lines, a Psalm loaded from the Bible text, or another whole
  section to include (see lib/prayerBook.ts).
*/
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, errorMessage } from '../api'
import { ListenButton, ScriptureText } from '../components/common'
import { expandItems, spokenLine, timesLabel } from '../lib/prayerBook'
import { plainScripture } from '../lib/scripture'
import type { BibleChapter, BibleVerse, PrayerBook, PrayerItem, PrayerSection, ScriptureRef } from '../types'

let bookCache: PrayerBook | null = null

export function PrayersPage() {
  const { section: sectionId } = useParams()
  const [book, setBook] = useState<PrayerBook | null>(bookCache)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (bookCache) return
    api<PrayerBook>('GET', '/data/prayers.json')
      .then((loaded) => setBook((bookCache = loaded)))
      .catch((e) => setError(errorMessage(e)))
  }, [])

  const section = book?.sections.find((s) => s.id === sectionId)

  return (
    <main className="page reading">
      <h1>Prayer Book</h1>
      {book && <p className="subtitle">{book.about}</p>}
      {error && <p className="error">{error}</p>}
      {!book && !error && <p className="status">Opening the prayer book…</p>}

      {book && section && <SectionView book={book} section={section} />}

      {/* No section chosen (or an unknown one): the list. */}
      {book &&
        !section &&
        book.sections
          .filter((s) => s.listed !== false)
          .map((s) => (
            <Link key={s.id} className="card card-link" to={`/prayers/${s.id}`}>
              <strong>{s.title}</strong> <span className="gold">›</span>
              {s.note && <div className="rubric">{s.note}</div>}
            </Link>
          ))}
    </main>
  )
}

/** One section, with any included sections and Psalms filled in. */
function SectionView({ book, section }: { book: PrayerBook; section: PrayerSection }) {
  // useMemo works the list out once per section, instead of on every draw.
  const items = useMemo(() => expandItems(book, section.items), [book, section])
  // The verses for each scripture item, once loaded (keyed "PSA.50.3-").
  const [verses, setVerses] = useState<Record<string, BibleVerse[]>>({})

  // Load the Psalms whenever the section (and so its items) changes.
  useEffect(() => {
    window.scrollTo(0, 0)
    for (const item of items) {
      if (!item.scripture) continue
      const ref = item.scripture
      api<BibleChapter>('GET', `/api/bible/${ref.book}/${ref.chapter}`).then((chapter) =>
        // "prev => ({...prev, ...})" adds to what's already loaded instead of replacing it.
        setVerses((prev) => ({ ...prev, [refKey(ref)]: inRange(chapter.verses, ref) })),
      )
    }
  }, [items])

  // The words to read aloud: titles, prayers (repeated as said), Psalms.
  const spoken = () =>
    [section.title, ...items.flatMap((item) => itemSpeech(item, verses))]
      .map((part) => (/[.!?]$/.test(part) ? part : `${part}.`)) // a pause between parts
      .join(' ')

  return (
    <>
      <Link className="back" to="/prayers">
        ‹ All prayers
      </Link>
      <section className="card">
        <h2>{section.title}</h2>
        {section.subtitle && <p className="rubric">{section.subtitle}</p>}
        {section.note && <p className="rubric">{section.note}</p>}
        <ListenButton title={section.title} text={spoken} />
        <div className="text">
          {items.map((item, i) => (
            <div key={i}>
              {item.title && <h3>{item.title}</h3>}
              {item.lines?.map((line, j) =>
                typeof line === 'string' ? (
                  <p key={j}>{line}</p>
                ) : 'rubric' in line ? (
                  <p key={j} className="rubric">
                    {line.rubric}
                  </p>
                ) : (
                  <p key={j}>
                    {line.text} <span className="rubric">({timesLabel(line.times)})</span>
                  </p>
                ),
              )}
              {item.scripture &&
                (verses[refKey(item.scripture)] ?? []).map((verse) => (
                  <p key={verse.number}>
                    <sup>{verse.number}</sup>
                    <ScriptureText text={verse.text} />
                  </p>
                ))}
            </div>
          ))}
        </div>
      </section>
    </>
  )
}

function refKey(ref: ScriptureRef): string {
  return `${ref.book}.${ref.chapter}.${ref.from ?? ''}-${ref.to ?? ''}`
}

/** Only the verses from ref.from to ref.to (e.g. Psalm 50 from verse 3, skipping its title). */
function inRange(verses: BibleVerse[], ref: ScriptureRef): BibleVerse[] {
  return verses.filter((v) => {
    const n = parseInt(v.number, 10)
    return n >= (ref.from ?? 1) && n <= (ref.to ?? Infinity)
  })
}

function itemSpeech(item: PrayerItem, verses: Record<string, BibleVerse[]>): string[] {
  const parts: string[] = []
  if (item.title) parts.push(item.title)
  for (const line of item.lines ?? []) parts.push(...spokenLine(line))
  if (item.scripture) {
    const text = (verses[refKey(item.scripture)] ?? []).map((v) => plainScripture(v.text)).join(' ')
    if (text) parts.push(text)
  }
  return parts
}
