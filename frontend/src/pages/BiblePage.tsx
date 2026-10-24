/*
  BiblePage (the Bible tab): the whole Orthodox Bible.
  Old Testament: the Septuagint (Brenton). New Testament: King James.

  Three addresses, three views (see TabsHost.tsx):
      /bible            -> all the books
      /bible/PSA        -> the chapters of Psalms
      /bible/PSA/50     -> Psalm 50
  useParams() reads the :book and :chapter parts of the address.

  The text comes from BibleController (/api/bible/...) through useApiData,
  which keeps what's loaded: leave the Bible tab in the middle of a chapter
  and it's still there, at the same place, when you come back.
*/
import { useEffect } from 'react'
import { Link, useParams } from 'react-router'
import { ListenButton, Verses } from '../components/common'
import { Masthead } from '../components/ornaments'
import { useApiData } from '../lib/data'
import { plainScripture, toParagraphs } from '../lib/scripture'
import type { BibleBook, BibleChapter, Place } from '../types'

const OLD_TESTAMENT = ['Law', 'History', 'Wisdom', 'Prophets', 'Appendix']
const NEW_TESTAMENT = ['Gospels', 'Acts and Epistles', 'Revelation']
const LAST_READ_KEY = 'orthodoxDaily.bible.lastRead'

export function BiblePage() {
  const { book, chapter } = useParams()
  const { data: books, error } = useApiData<BibleBook[]>('/api/bible/books')

  return (
    <div className="screen">
      <Masthead
        eyebrow="Holy Scripture"
        title="The Holy Bible"
        subtitle="The Septuagint and the New Testament"
      />
      <main className="page reading">
        {error && <p className="error">{error}</p>}
        {!books && !error && <p className="status">Opening the Bible…</p>}
        {books &&
          (book && chapter ? (
            <ChapterView code={book} number={Number(chapter)} />
          ) : book ? (
            <ChapterList book={books.find((b) => b.code === book)} />
          ) : (
            <BookList books={books} />
          ))}

        <footer className="footer">
          Old Testament: Sir Lancelot Brenton's translation of the Septuagint (1851), with the Septuagint's Psalm
          numbering used in Orthodox services. New Testament: King James Version. Both are public domain. Words in{' '}
          <i>italics</i> were added by the translators.
        </footer>
      </main>
    </div>
  )
}

/** All the books, grouped by section, plus "Continue reading". */
function BookList({ books }: { books: BibleBook[] }) {
  const section = (name: string) => {
    const inSection = books.filter((b) => b.section === name)
    if (inSection.length === 0) return null
    return (
      <div key={name}>
        <h3>{name}</h3>
        <div className="chips">
          {inSection.map((b) => (
            <Link key={b.code} className="chip" to={`/bible/${b.code}`}>
              {b.name}
            </Link>
          ))}
        </div>
      </div>
    )
  }

  // Where you left off, e.g. "PSA.50" (saved by ChapterView).
  let lastRead: string | null = null
  try {
    lastRead = localStorage.getItem(LAST_READ_KEY)
  } catch {
    // storage switched off
  }
  const [lastCode, lastChapter] = lastRead?.split('.') ?? []
  const lastBook = books.find((b) => b.code === lastCode)

  return (
    <>
      {lastBook && (
        <Link className="card card-link continue" to={`/bible/${lastCode}/${lastChapter}`}>
          <span className="label">Continue reading</span>
          <span className="title">
            {lastBook.name} {lastChapter}
          </span>
          <span className="go" aria-hidden="true">›</span>
        </Link>
      )}
      <section className="card">
        <h2>Old Testament</h2>
        {OLD_TESTAMENT.map(section)}
      </section>
      <section className="card">
        <h2>New Testament</h2>
        {NEW_TESTAMENT.map(section)}
      </section>
    </>
  )
}

/** One book's chapter numbers. */
function ChapterList({ book }: { book: BibleBook | undefined }) {
  if (!book) {
    return (
      <p>
        That book wasn't found. <Link to="/bible">‹ All books</Link>
      </p>
    )
  }
  // Array.from({ length: n }, ...) makes the list 1, 2, ..., n.
  const numbers = Array.from({ length: book.chapters }, (_, i) => i + 1)
  return (
    <>
      <Link className="back" to="/bible">
        ‹ All books
      </Link>
      <section className="card">
        <h2>{book.name}</h2>
        <div className="chips numbers">
          {numbers.map((n) => (
            <Link key={n} className="chip" to={`/bible/${book.code}/${n}`}>
              {n}
            </Link>
          ))}
        </div>
      </section>
    </>
  )
}

/** One chapter's text, with Listen and previous/next. */
function ChapterView({ code, number }: { code: string; number: number }) {
  const { data: chapter, error } = useApiData<BibleChapter>(`/api/bible/${encodeURIComponent(code)}/${number}`)

  // Remember it for "Continue reading" once it has loaded.
  useEffect(() => {
    if (!chapter) return
    try {
      localStorage.setItem(LAST_READ_KEY, `${chapter.code}.${chapter.chapter}`)
    } catch {
      // storage switched off: "Continue reading" just won't appear
    }
  }, [chapter])

  if (error) return <p className="error">{error}</p>
  if (!chapter) return <p className="status">Loading…</p>

  // "Psalm 50" for Psalms, "John 3" for everything else.
  const title = chapter.code === 'PSA' ? `Psalm ${chapter.chapter}` : `${chapter.book} ${chapter.chapter}`
  const link = (place: Place | null, label: (text: string) => string) =>
    place ? <Link to={`/bible/${place.code}/${place.chapter}`}>{label(`${place.book} ${place.chapter}`)}</Link> : <span />

  return (
    <>
      <Link className="back" to={`/bible/${chapter.code}`}>
        ‹ {chapter.book}
      </Link>
      <section className="card">
        <h2>{title}</h2>
        <ListenButton
          title={title}
          text={() => `${title}. ${chapter.verses.map((v) => plainScripture(v.text)).join(' ')}`}
        />
        <div className="text">
          {/* King James chapters come in paragraphs; Septuagint ones one verse per line. */}
          <Verses paragraphs={toParagraphs(chapter.verses)} />
        </div>
        <nav className="pager">
          {link(chapter.previous, (t) => `‹ ${t}`)}
          {link(chapter.next, (t) => `${t} ›`)}
        </nav>
      </section>
    </>
  )
}
