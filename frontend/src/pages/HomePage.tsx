/*
  HomePage (the Home tab, "/"): the liturgical day.

  Shows the fasting rule, the day's readings (as the Archdiocese lists them),
  a link to the service texts, the saints and their lives, for today or for
  any date you choose. The date is kept in the address (/?date=2026-12-25), so
  a day can be bookmarked or shared, and the Back button works.

  The data comes from CalendarController (GET /api/day/{year}/{month}/{day}).
*/
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useAccount } from '../account-context'
import { api, errorMessage } from '../api'
import { ListenButton, ScriptureText } from '../components/common'
import { addDays, isIsoDate, longDate, toIso } from '../lib/dates'
import { stop } from '../lib/listen'
import { htmlToSpeech, sanitizeHtml } from '../lib/sanitize'
import { toParagraphs } from '../lib/scripture'
import type { Account, Fasting, LiturgicalDay, Reading } from '../types'

const ALL_FOODS = ['meat', 'fish', 'dairy', 'eggs', 'wine', 'oil']

export function HomePage() {
  // Today's date, read from the clock once when the screen first appears.
  // (The function form of useState runs only that first time.)
  const [today] = useState(() => toIso(new Date()))

  // ?date=... in the address; today if it's missing or not a real date.
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('date')
  const date = isIsoDate(requested) ? requested : today
  const { account } = useAccount()

  // The last day loaded (or its error), together with the date it's for.
  // While a new date loads, the old result simply doesn't match it, so the
  // screen shows "Loading…" without having to clear anything first.
  const [loaded, setLoaded] = useState<{ date: string; day?: LiturgicalDay; error?: string } | null>(null)
  const day = loaded?.date === date ? (loaded.day ?? null) : null
  const error = loaded?.date === date ? (loaded.error ?? null) : null

  // Load the day whenever the date changes. "cancelled" ignores a slow reply
  // that arrives after you've already moved on to another date.
  useEffect(() => {
    let cancelled = false
    stop() // stop reading the previous day aloud
    const [y, m, d] = date.split('-').map(Number)
    api<LiturgicalDay>('GET', `/api/day/${y}/${m}/${d}`)
      .then((result) => !cancelled && setLoaded({ date, day: result }))
      .catch((e) => !cancelled && setLoaded({ date, error: errorMessage(e) }))
    // This "cleanup" function runs when the date changes again.
    return () => {
      cancelled = true
    }
  }, [date])

  // replace: true changes the address without adding a Back-button step per click.
  const goTo = (iso: string) => setSearchParams({ date: iso }, { replace: true })

  return (
    <main className="page">
      <NameDayGreeting account={account} today={today} />

      <nav className="date-nav">
        <button type="button" onClick={() => goTo(addDays(date, -1))} title="Previous day">
          ‹ Prev
        </button>
        <input
          type="date"
          value={date}
          aria-label="Choose a date"
          onChange={(e) => e.target.value && goTo(e.target.value)}
        />
        <button type="button" onClick={() => goTo(addDays(date, 1))} title="Next day">
          Next ›
        </button>
      </nav>

      <div className="day-header">
        <p className="date">{longDate(date)}</p>
        {error ? (
          <>
            <h1>Could not load this day</h1>
            <p className="error">{error}</p>
            <p className="status">Is the backend running, and are you online?</p>
          </>
        ) : !day ? (
          <h1>Loading…</h1>
        ) : (
          <>
            <h1>{day.title}</h1>
            <p className="subtitle">{[...day.subtitles, day.tone > 0 ? `Tone ${day.tone}` : ''].filter(Boolean).join(' · ')}</p>
            {day.feast.names.length > 0 && <span className="feast-badge">✦ {day.feast.names.join(', ')}</span>}
          </>
        )}
      </div>

      {day && (
        <>
          <FastingCard fasting={day.fasting} />
          <ReadingsCard readings={day.readings} />
          <ServiceTextsCard date={date} />
          {day.saints.length > 0 && (
            <section className="card">
              <h2>Commemorated Today</h2>
              <ul>
                {day.saints.map((saint) => (
                  <li key={saint}>{saint}</li>
                ))}
              </ul>
            </section>
          )}
          {day.lives.length > 0 && (
            <section className="card">
              <h2>Lives of the Saints</h2>
              {day.lives.map((life) => (
                <details key={life.title}>
                  <summary>{life.title}</summary>
                  <div className="life">
                    <ListenButton title={life.title} text={() => `${life.title}. ${htmlToSpeech(life.html)}`} />
                    {/* Safe: sanitizeHtml keeps only harmless formatting (lib/sanitize.ts). */}
                    <div dangerouslySetInnerHTML={{ __html: sanitizeHtml(life.html) }} />
                  </div>
                </details>
              ))}
            </section>
          )}
        </>
      )}

      <footer className="footer">
        Calendar data: <a href="https://orthocal.info">orthocal.info</a> (Greek/Antiochian tradition). Readings as
        listed by the Antiochian Archdiocese. Fasting follows strict typikon practice; follow your priest's guidance.
      </footer>
    </main>
  )
}

/** "Happy name day!" on your patron saint's feast (nameDay is "MM-DD"). */
function NameDayGreeting({ account, today }: { account: Account | null; today: string }) {
  if (!account?.nameDay || today.slice(5) !== account.nameDay) return null
  const firstName = account.displayName.split(' ')[0]
  return (
    <p className="name-day">
      ✦ Happy name day, {firstName}!{account.patronSaint ? ` May ${account.patronSaint} pray for you.` : ''} Many
      years! ✦
    </p>
  )
}

function FastingCard({ fasting }: { fasting: Fasting }) {
  return (
    // The rule-... class picks this rule's colour (styles/app.css).
    <section className={`card fast rule-${fasting.rule}`}>
      <h2>Fasting</h2>
      <p className="fast-title">{fasting.label}</p>
      <p className="fast-season">{fasting.season || 'No fasting season'}</p>
      {/* On fasting days, every food: crossed out if avoided today, plain if allowed. */}
      {fasting.abstainFrom.length > 0 && (
        <div className="pills">
          {ALL_FOODS.map((food) => (
            <span key={food} className={`pill ${fasting.abstainFrom.includes(food) ? 'avoid' : 'allowed'}`}>
              {food}
            </span>
          ))}
        </div>
      )}
    </section>
  )
}

function ReadingsCard({ readings }: { readings: Reading[] }) {
  if (readings.length === 0) return null
  return (
    <section className="card">
      <h2>Scripture Readings</h2>
      {readings.map((reading) => {
        // e.g. "St. Paul's Letter to the Galatians 1:11-19", labelled "Epistle · St James"
        const heading = [reading.book, reading.chapters].filter(Boolean).join(' ')
        const label = [reading.service, reading.note].filter(Boolean).join(' · ')
        // Announce it the way a reader does in church.
        const intro =
          reading.service === 'Gospel'
            ? `The reading from the Holy Gospel according to St. ${reading.book}.`
            : `The reading from ${reading.book}.`
        return (
          <details key={reading.reference}>
            <summary>
              {heading}
              <span className="service">{label}</span>
            </summary>
            <div className="passage">
              <ListenButton title={heading} text={() => `${intro} ${reading.verses.map((v) => v.text).join(' ')}`} />
              {toParagraphs(reading.verses).map((paragraph) => (
                <p key={`${paragraph[0].chapter}:${paragraph[0].number}`}>
                  {paragraph.map((verse) => (
                    <span key={`${verse.chapter}:${verse.number}`}>
                      <sup>{verse.number}</sup>
                      <ScriptureText text={verse.text} />{' '}
                    </span>
                  ))}
                </p>
              ))}
            </div>
          </details>
        )
      })}
    </section>
  )
}

/**
 * The Archdiocese publishes the service texts (PDF) in its Online Liturgical
 * Guide. Its page can't be opened at a particular date from a link, so we say
 * which date to pick. (Showing them in the app waits on its permission.)
 */
function ServiceTextsCard({ date }: { date: string }) {
  return (
    <section className="card">
      <h2>Service Texts</h2>
      <p>
        The texts of Orthros, the Divine Liturgy and Vespers (PDF) for Sundays and feasts are published by the
        Archdiocese in its Online Liturgical Guide.
      </p>
      <a className="button-link" href="https://www.antiochian.org/liturgicday" target="_blank" rel="noopener">
        Open the Online Liturgical Guide ↗
      </a>
      <p className="note">Choose {longDate(date)} there.</p>
    </section>
  )
}
