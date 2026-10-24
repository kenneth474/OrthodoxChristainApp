/*
  ChurchPage (the Church tab): the Antiochian Archdiocese of North America.

      /church                      -> today's prayer intention, the hierarchs,
                                      the dioceses, and about the Archdiocese
      /church/dioceses/wichita     -> one diocese
      /church/ministries           -> the ministries and departments

  Everything comes from public/data/church.json, written for this app from
  facts checked on antiochian.org (their own pages are theirs, so we link to
  them rather than copy them). "Pray today for..." changes every day; see
  lib/church.ts.
*/
import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { Masthead } from '../components/ornaments'
import { intentionFor, type ChurchData, type Diocese } from '../lib/church'
import { useApiData } from '../lib/data'
import { fromIso, longDate, toIso } from '../lib/dates'

export function ChurchPage() {
  const { diocese: dioceseId } = useParams()
  const { pathname } = useLocation()
  const { data: church, error } = useApiData<ChurchData>('/data/church.json')
  const [today] = useState(() => toIso(new Date()))

  const diocese = church?.dioceses.find((d) => d.id === dioceseId)
  const onMinistries = pathname === '/church/ministries'

  return (
    <div className="screen">
      <Masthead
        eyebrow="Antiochian Orthodox Christian Archdiocese"
        title={diocese ? diocese.name : onMinistries ? 'Ministries' : 'Our Church'}
        subtitle={diocese || onMinistries ? undefined : 'of North America'}
      />
      <main className="page">
        {error && <p className="error">{error}</p>}
        {!church && !error && <p className="status">Loading…</p>}
        {church &&
          (diocese ? (
            <DioceseView diocese={diocese} />
          ) : onMinistries ? (
            <MinistriesView church={church} />
          ) : (
            <Overview church={church} today={today} />
          ))}

        {church && (
          <footer className="footer">
            Facts checked against <a href={church.source}>antiochian.org</a> on{' '}
            {fromIso(church.checked).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}.
            For news, parishes and more, visit the Archdiocese's own website. This is an independent app, not an
            official app of the Archdiocese.
          </footer>
        )}
      </main>
    </div>
  )
}

function Overview({ church, today }: { church: ChurchData; today: string }) {
  const intention = intentionFor(today, church)
  const bishops = church.dioceses.filter((d) => d.role === 'Bishop')
  return (
    <>
      <section className="card intention featured">
        <h2>Pray today for</h2>
        <p className="intention-heading">{intention.heading}</p>
        <p className="intention-prayer">{intention.prayer}</p>
        <p className="note">{longDate(today)}. A different intention each day.</p>
      </section>

      <section className="card">
        <h2>Our hierarchs</h2>
        <ul className="people">
          <li>
            <a href={church.patriarch.link} target="_blank" rel="noopener">{church.patriarch.name}</a>
            <span>{church.patriarch.title}</span>
          </li>
          <li>
            <a href={church.metropolitan.link} target="_blank" rel="noopener">{church.metropolitan.name}</a>
            <span>{church.metropolitan.title}</span>
          </li>
          {bishops.map((d) => (
            <li key={d.id}>
              <Link to={`/church/dioceses/${d.id}`}>{d.shepherd}</Link>
              <span>{d.name}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2>The dioceses</h2>
        <ul className="link-list">
          {church.dioceses.map((d) => (
            <li key={d.id}>
              <Link to={`/church/dioceses/${d.id}`}>
                <span className="title">{d.name}</span>
                <span className="detail">{d.shepherd}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <Link className="card card-link continue" to="/church/ministries">
        <span className="label">Ministries and departments</span>
        <span className="title">{church.ministries.length} ways the Archdiocese serves</span>
        <span className="go" aria-hidden="true">›</span>
      </Link>

      <section className="card">
        <h2>About the Archdiocese</h2>
        {church.about.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
      </section>
    </>
  )
}

function DioceseView({ diocese }: { diocese: Diocese }) {
  return (
    <>
      <Link className="back" to="/church">
        ‹ Our Church
      </Link>
      <section className="card">
        <h2>{diocese.role === 'Bishop' ? 'Its bishop' : 'Its shepherd'}</h2>
        <p className="person">{diocese.shepherd}</p>
        {diocese.role !== 'Bishop' && <p className="note">{diocese.role}</p>}

        <h3>Where</h3>
        <p>{diocese.region}</p>
        {diocese.churches && <p>{diocese.churches}.</p>}
        {diocese.note && <p>{diocese.note}</p>}

        <h3>Let us pray</h3>
        <p className="intention-prayer">
          For {diocese.shepherd}, and for the clergy and faithful of the {diocese.name}.
        </p>

        <a className="button-link" href={diocese.link} target="_blank" rel="noopener">
          The diocese on antiochian.org ↗
        </a>
      </section>
    </>
  )
}

function MinistriesView({ church }: { church: ChurchData }) {
  return (
    <>
      <Link className="back" to="/church">
        ‹ Our Church
      </Link>
      <p className="intro">
        Much of the Archdiocese's work is done by its departments and ministries, through staff and volunteers in
        every diocese.
      </p>
      {church.ministries.map((m) => (
        <section className="card ministry" key={m.id}>
          <h2>{m.name}</h2>
          <p>{m.about}</p>
          <p className="intention-prayer">{m.prayer}</p>
          <a href={m.link} target="_blank" rel="noopener">
            Learn more ↗
          </a>
        </section>
      ))}
    </>
  )
}
