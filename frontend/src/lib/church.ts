/*
  church.ts: the Archdiocese information (public/data/church.json) and the
  daily prayer intention ("Pray today for...").

  Each day features one intention: the Patriarch, the Metropolitan, one of
  the eight dioceses, or one of the ministries, in a fixed order. About once
  a month the list starts over, so over a year the whole Archdiocese is
  remembered many times. The choice depends only on the date, so everyone
  sees the same intention on the same day, with no server needed.
*/
import { fromIso } from './dates'

export interface Hierarch {
  name: string
  title: string
  prayer: string
  link: string
}

export interface Diocese {
  id: string
  name: string
  shepherd: string // the bishop, or the vicar caring for the diocese
  role: string
  region: string
  churches?: string
  note?: string
  link: string
}

export interface Ministry {
  id: string
  name: string
  about: string
  prayer: string
  link: string
}

export interface ChurchData {
  checked: string // when the facts were last checked against antiochian.org
  source: string
  about: string[]
  patriarch: Hierarch
  metropolitan: Hierarch
  dioceses: Diocese[]
  ministries: Ministry[]
}

export interface Intention {
  heading: string // what we pray for, e.g. "Diocese of Wichita and Mid-America"
  prayer: string // the petition, e.g. "For Archimandrite CALINIC, and for..."
  to: string // where to read more in the app
}

/** Every intention, in the order they come round. */
export function allIntentions(data: ChurchData): Intention[] {
  return [
    { heading: data.patriarch.title, prayer: data.patriarch.prayer, to: '/church' },
    { heading: 'Our Metropolitan', prayer: data.metropolitan.prayer, to: '/church' },
    ...data.dioceses.map((d) => ({
      heading: d.name,
      prayer: `For ${d.shepherd}, and for the clergy and faithful of the ${d.name}.`,
      to: `/church/dioceses/${d.id}`,
    })),
    ...data.ministries.map((m) => ({ heading: m.name, prayer: m.prayer, to: '/church/ministries' })),
  ]
}

/**
 * The intention for a date ("YYYY-MM-DD"). Days are counted from 1 January
 * 1970, so each new day moves one step along the list.
 */
export function intentionFor(iso: string, data: ChurchData): Intention {
  const list = allIntentions(data)
  const date = fromIso(iso)
  const dayNumber = Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86_400_000)
  return list[dayNumber % list.length]
}
