/*
  HomePage (the Home tab, "/"): the liturgical day.

  Shows the day's feast and its sign from the Typikon, the fasting rule, the
  day's readings (as the Archdiocese lists them), today's prayer intention,
  a link to the service texts, the saints and their lives, for today or for
  any date you choose. The date is kept in the address (/?date=2026-12-25),
  so a day can be bookmarked or shared, and the Back button works.

  The data comes from CalendarController (GET /api/day/{year}/{month}/{day})
  through useApiData, which keeps it: switching tabs and coming back doesn't
  load it again. Pull down at the top of the page to refresh.
*/
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useAccount } from '../account-context'
import { ListenButton, Verses } from '../components/common'
import { FeastSign, Masthead } from '../components/ornaments'
import { feastRank } from '../lib/feasts'
import { useStyle } from '../lib/appearance'
import { intentionFor, type ChurchData } from '../lib/church'
import { useApiData } from '../lib/data'
import { addDays, fromIso, isIsoDate, longDate, toIso } from '../lib/dates'
import { stop } from '../lib/listen'
import { htmlToSpeech, sanitizeHtml } from '../lib/sanitize'
import { toParagraphs } from '../lib/scripture'
import { liturgicalSeason } from '../lib/seasons'
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

  const [y, m, d] = date.split('-').map(Number)
  const { data: day, error } = useApiData<LiturgicalDay>(`/api/day/${y}/${m}/${d}`)
  const { data: church } = useApiData<ChurchData>('/data/church.json')

  // replace: true changes the address without adding a Back-button step per click.
  const goTo = (iso: string) => {
    stop() // stop reading the previous day aloud
    setSearchParams({ date: iso }, { replace: true })
  }

  const weekday = fromIso(date).toLocaleDateString('en-US', { weekday: 'long' })
  const monthDay = fromIso(date).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })

  return (
    <div className="screen">
      <Masthead
        eyebrow={
          <>
            <span>{weekday}</span>
            <span className="dot">·</span>
            <span>{monthDay}</span>
            {day && day.tone > 0 && (
              <>
                <span className="dot">·</span>
                <span>Tone {day.tone}</span>
              </>
            )}
          </>
        }
        title={error ? 'Could not load this day' : day ? day.title : 'Loading…'}
        subtitle={day && day.subtitles.length > 0 ? day.subtitles.join(' · ') : undefined}
      >
        {day && day.feast.level >= 1 && (
          <p className="rank">
            <FeastSign level={day.feast.level} />
            <span>{feastRank(day.feast.level)}</span>
          </p>
        )}
      </Masthead>

      <main className="page">
        <nav className="date-nav" aria-label="Choose a day">
          <button type="button" onClick={() => goTo(addDays(date, -1))} aria-label="Previous day">
            ‹
          </button>
          <input type="date" value={date} aria-label="Choose a date" onChange={(e) => e.target.value && goTo(e.target.value)} />
          <button type="button" onClick={() => goTo(addDays(date, 1))} aria-label="Next day">
            ›
          </button>
          {date !== today && (
            <button type="button" className="today" onClick={() => goTo(today)}>
              Today
            </button>
          )}
        </nav>

        <NameDayGreeting account={account} today={today} date={date} />
        <SeasonNote date={date} />

        {error && (
          <section className="card">
            <p className="error">{error}</p>
            <p className="status">Check that you're online, then pull down to try again.</p>
          </section>
        )}

        {day && (
          <>
            {/* Feasts besides the day's title (the title is already at the top). */}
            {day.feast.names.some((name) => name !== day.title) && (
              <p className="feast-names">✦ {day.feast.names.filter((name) => name !== day.title).join(' · ')} ✦</p>
            )}
            <FastingCard fasting={day.fasting} />
            <ReadingsCard readings={day.readings} />
            {church && (
              <IntentionCard church={church} date={date} />
            )}
            {day.saints.length > 0 && (
              <section className="card">
                <h2>Commemorated today</h2>
                <ul className="saints">
                  {day.saints.map((saint) => (
                    <li key={saint}>{saint}</li>
                  ))}
                </ul>
              </section>
            )}
            {day.lives.length > 0 && (
              <section className="card">
                <h2>Lives of the saints</h2>
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
            <ServiceTextsCard date={date} />
          </>
        )}

        <footer className="footer">
          Calendar data: <a href="https://orthocal.info">orthocal.info</a> (Greek/Antiochian tradition). Readings as
          listed by the Antiochian Archdiocese. Fasting follows strict typikon practice; follow your priest's guidance.
        </footer>
      </main>
    </div>
  )
}

/** "Happy name day!" on your patron saint's feast (nameDay is "MM-DD"). */
function NameDayGreeting({ account, today, date }: { account: Account | null; today: string; date: string }) {
  if (!account?.nameDay || date !== today || today.slice(5) !== account.nameDay) return null
  const firstName = account.displayName.split(' ')[0]
  return (
    <p className="name-day">
      Happy name day, {firstName}!{account.patronSaint ? ` May ${account.patronSaint} pray for you.` : ''} Many years!
    </p>
  )
}

/** In the Liturgical seasons style: the day's vestment color and season. */
function SeasonNote({ date }: { date: string }) {
  const style = useStyle()
  if (style !== 'SEASONS') return null
  const season = liturgicalSeason(date)
  return (
    <p className="season-note">
      <span className="season-swatch" aria-hidden="true" />
      Vestments: {season.color} · {season.name}
    </p>
  )
}

function FastingCard({ fasting }: { fasting: Fasting }) {
  return (
    // The rule-... class picks this rule's color (styles/app.css).
    <section className={`card fast rule-${fasting.rule}`}>
      <h2>Fasting</h2>
      <p className="fast-title">{fasting.label}</p>
      {fasting.season && <p className="fast-season">{fasting.season}</p>}
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
    <section className="card readings">
      <h2>Scripture readings</h2>
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
              <span className="service">{label}</span>
              <span className="reference">{heading}</span>
            </summary>
            <div className="passage">
              <ListenButton title={heading} text={() => `${intro} ${reading.verses.map((v) => v.text).join(' ')}`} />
              <Verses paragraphs={toParagraphs(reading.verses)} />
            </div>
          </details>
        )
      })}
    </section>
  )
}

/** "Pray today for…": the day's intention for the Archdiocese (lib/church.ts). */
function IntentionCard({ church, date }: { church: ChurchData; date: string }) {
  const intention = intentionFor(date, church)
  return (
    <section className="card intention">
      <h2>Pray today for</h2>
      <p className="intention-heading">{intention.heading}</p>
      <p className="intention-prayer">{intention.prayer}</p>
      <Link className="more" to={intention.to}>
        More in the Church tab ›
      </Link>
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
      <h2>Service texts</h2>
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
