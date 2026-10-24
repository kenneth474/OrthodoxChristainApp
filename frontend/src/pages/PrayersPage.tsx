/*
  PrayersPage (the Prayers tab): the prayer book.

      /prayers            -> the list of sections
      /prayers/morning    -> Morning Prayers

  The prayers live in a data file, public/data/prayers.json, so adding or
  correcting a prayer never means changing this code. Each section has
  "items": prayer lines, a Psalm loaded from the Bible text, or another whole
  section to include (see lib/prayerBook.ts).
*/
import { useMemo } from 'react'
import { Link, useParams } from 'react-router'
import { ListenButton, ScriptureText } from '../components/common'
import { Masthead } from '../components/ornaments'
import { useApiData, usePeekApiData } from '../lib/data'
import { expandItems, spokenLine, timesLabel } from '../lib/prayerBook'
import { plainScripture } from '../lib/scripture'
import type { BibleChapter, BibleVerse, PrayerBook, PrayerItem, PrayerSection, ScriptureRef } from '../types'

export function PrayersPage() {
  const { section: sectionId } = useParams()
  const { data: book, error } = useApiData<PrayerBook>('/data/prayers.json')
  const section = book?.sections.find((s) => s.id === sectionId)

  return (
    <div className="screen">
      <Masthead eyebrow="Prayer book" title={section ? section.title : 'Prayers'} subtitle={section?.subtitle} />
      <main className="page reading">
        {error && <p className="error">{error}</p>}
        {!book && !error && <p className="status">Opening the prayer book…</p>}

        {book && section && <SectionView book={book} section={section} />}

        {/* No section chosen (or an unknown one): the list. */}
        {book && !section && (
          <>
            <p className="intro">{book.about}</p>
            {book.sections
              .filter((s) => s.listed !== false)
              .map((s) => (
                <Link key={s.id} className="card card-link prayer-link" to={`/prayers/${s.id}`}>
                  <span className="title">{s.title}</span>
                  <span className="go" aria-hidden="true">›</span>
                  {s.note && <span className="rubric">{s.note}</span>}
                </Link>
              ))}
          </>
        )}
      </main>
    </div>
  )
}

/** One section, with any included sections and Psalms filled in. */
function SectionView({ book, section }: { book: PrayerBook; section: PrayerSection }) {
  // useMemo works the list out once per section, instead of on every draw.
  const items = useMemo(() => expandItems(book, section.items), [book, section])
  const peek = usePeekApiData()

  // The words to read aloud: titles, prayers (repeated as said), Psalms.
  const spoken = () =>
    [section.title, ...items.flatMap((item) => itemSpeech(item, peek))]
      .map((part) => (/[.!?]$/.test(part) ? part : `${part}.`)) // a pause between parts
      .join(' ')

  return (
    <>
      <Link className="back" to="/prayers">
        ‹ All prayers
      </Link>
      <section className="card">
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
              {item.scripture && <ScriptureItem reference={item.scripture} />}
            </div>
          ))}
        </div>
      </section>
    </>
  )
}

/** A Psalm (or part of one) inside a prayer, loaded from the Bible text. */
function ScriptureItem({ reference }: { reference: ScriptureRef }) {
  const { data: chapter } = useApiData<BibleChapter>(chapterUrl(reference))
  if (!chapter) return null
  return (
    <>
      {inRange(chapter.verses, reference).map((verse) => (
        <p key={verse.number}>
          <sup>{verse.number}</sup>
          <ScriptureText text={verse.text} />
        </p>
      ))}
    </>
  )
}

function chapterUrl(ref: ScriptureRef): string {
  return `/api/bible/${ref.book}/${ref.chapter}`
}

/** Only the verses from ref.from to ref.to (e.g. Psalm 50 from verse 3, skipping its title). */
function inRange(verses: BibleVerse[], ref: ScriptureRef): BibleVerse[] {
  return verses.filter((v) => {
    const n = parseInt(v.number, 10)
    return n >= (ref.from ?? 1) && n <= (ref.to ?? Infinity)
  })
}

function itemSpeech(item: PrayerItem, peek: <T>(url: string) => T | null): string[] {
  const parts: string[] = []
  if (item.title) parts.push(item.title)
  for (const line of item.lines ?? []) parts.push(...spokenLine(line))
  if (item.scripture) {
    const chapter = peek<BibleChapter>(chapterUrl(item.scripture))
    const text = chapter ? inRange(chapter.verses, item.scripture).map((v) => plainScripture(v.text)).join(' ') : ''
    if (text) parts.push(text)
  }
  return parts
}
