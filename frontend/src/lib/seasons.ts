/*
  seasons.ts: where a date falls in the Church year, for the "Liturgical
  seasons" style, whose colors follow the vestments.

  Many Orthodox parishes change the color of the vestments and altar cloths
  with the feasts and seasons: purple in Great Lent, red (or white) at Pascha,
  green at Pentecost, blue on feasts of the Theotokos, white at Theophany and
  the Transfiguration, gold the rest of the year. Customs differ from parish
  to parish; these are common ones, not rules.

  Fixed feasts use the civil date (the Archdiocese keeps the New Calendar);
  the moving ones count days from Pascha.
*/
import { fromIso, toIso } from './dates'

export type SeasonId =
  | 'ordinary'
  | 'lent'
  | 'palms'
  | 'holy-week'
  | 'pascha'
  | 'ascension'
  | 'pentecost'
  | 'theotokos'
  | 'cross'
  | 'nativity'
  | 'theophany'
  | 'transfiguration'

export interface Season {
  id: SeasonId
  name: string // e.g. "Great Lent"
  color: string // the vestment color, in words
}

const SEASONS: Record<SeasonId, Omit<Season, 'id'>> = {
  ordinary: { name: 'The Church year', color: 'gold' },
  lent: { name: 'Great Lent', color: 'purple' },
  palms: { name: 'Palm Sunday', color: 'green' },
  'holy-week': { name: 'Holy Week', color: 'black' },
  pascha: { name: 'Pascha', color: 'red' },
  ascension: { name: 'Ascension', color: 'white and gold' },
  pentecost: { name: 'Pentecost', color: 'green' },
  theotokos: { name: 'Feast of the Theotokos', color: 'blue' },
  cross: { name: 'Feast of the Cross', color: 'purple' },
  nativity: { name: 'Nativity of Christ', color: 'white and gold' },
  theophany: { name: 'Theophany', color: 'white' },
  transfiguration: { name: 'Transfiguration', color: 'white' },
}

/**
 * The date of Orthodox Pascha in a given year, as "YYYY-MM-DD".
 *
 * This is the old Julian-calendar formula (Meeus): it gives Pascha as a date
 * on the Julian calendar, and adding 13 days turns that into our ordinary
 * (Gregorian) calendar. The 13-day gap is right from 1900 to 2099.
 */
export function orthodoxPascha(year: number): string {
  const a = year % 4
  const b = year % 7
  const c = year % 19
  const d = (19 * c + 15) % 30
  const e = (2 * a + 4 * b - d + 34) % 7
  const month = Math.floor((d + e + 114) / 31) // 3 = March, 4 = April (Julian)
  const day = ((d + e + 114) % 31) + 1
  // new Date(...) with day + 13 rolls over into the next month by itself.
  return toIso(new Date(year, month - 1, day + 13))
}

/** Whole days from Pascha to the date (negative = before Pascha), like Orthocal's "pascha_distance". */
export function daysFromPascha(iso: string): number {
  const date = fromIso(iso)
  const pascha = fromIso(orthodoxPascha(date.getFullYear()))
  // Math.round irons out the hour lost or gained when the clocks change.
  return Math.round((date.getTime() - pascha.getTime()) / 86_400_000)
}

/** The season (and vestment color) for a date. */
export function liturgicalSeason(iso: string): Season {
  const id = seasonId(iso)
  return { id, ...SEASONS[id] }
}

function seasonId(iso: string): SeasonId {
  const monthDay = iso.slice(5) // "MM-DD"
  const pdist = daysFromPascha(iso)

  // 1. Great feasts on fixed dates win even inside a season (the
  //    Annunciation is blue, even in Lent).
  const greatFeasts: Record<string, SeasonId> = {
    '03-25': 'theotokos', // Annunciation
    '02-02': 'theotokos', // Meeting of the Lord in the Temple
    '08-06': 'transfiguration',
    '09-08': 'theotokos', // Nativity of the Theotokos
    '09-14': 'cross', // Exaltation of the Holy Cross
    '10-01': 'theotokos', // Protection of the Theotokos
    '11-21': 'theotokos', // Entry of the Theotokos into the Temple
  }
  if (greatFeasts[monthDay]) return greatFeasts[monthDay]

  // 2. The seasons that move with Pascha.
  if (pdist >= -48 && pdist <= -8) return 'lent' // Clean Monday to Lazarus Saturday
  if (pdist === -7) return 'palms'
  if (pdist >= -6 && pdist <= -1) return 'holy-week'
  if (pdist >= 0 && pdist <= 38) return 'pascha'
  if (pdist >= 39 && pdist <= 48) return 'ascension'
  if (pdist >= 49 && pdist <= 56) return 'pentecost'

  // 3. Seasons around fixed feasts.
  if (monthDay >= '12-25' || monthDay <= '01-01') return 'nativity' // Nativity to its leave-taking
  if (monthDay >= '01-06' && monthDay <= '01-14') return 'theophany' // Theophany to its leave-taking
  if (monthDay >= '08-01' && monthDay <= '08-23') return 'theotokos' // Dormition fast and feast

  return 'ordinary'
}
