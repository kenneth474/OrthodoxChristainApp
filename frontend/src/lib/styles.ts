/*
  styles.ts: the app's five looks ("styles"), shown in the style picker when
  creating an account and in Profile › Appearance.

  The colors and fonts themselves are in styles/theme.css, chosen by the
  data-style attribute on the <html> tag (see lib/appearance.ts). The
  swatches here are only for the little previews in the picker.
*/
import type { AppStyle } from '../types'

export interface StyleInfo {
  id: AppStyle
  name: string
  about: string
  swatches: string[] // background, text, accents (for the picker preview)
}

export const STYLES: StyleInfo[] = [
  {
    id: 'ILLUMINATED',
    name: 'Illuminated manuscript',
    about: 'Parchment, red rubrics and gold, like a service book.',
    swatches: ['#f3ecdd', '#2a1f17', '#a3241b', '#b08a3e'],
  },
  {
    id: 'MOSAIC',
    name: 'Byzantine mosaic',
    about: 'Deep lapis blue and gold, like the mosaics of Ravenna.',
    swatches: ['#1e3a66', '#f6ebcb', '#c9a24a', '#9b2c2c'],
  },
  {
    id: 'VIGIL',
    name: 'Candlelit vigil',
    about: 'Candle gold on near-black, like a church at Vespers.',
    swatches: ['#14110e', '#ede3d1', '#d9b26a', '#c2412f'],
  },
  {
    id: 'ATHOS',
    name: 'Holy Mountain',
    about: 'Stone, olive wood and terracotta, like a monastery on Athos.',
    swatches: ['#ece8e1', '#2e2a25', '#5e6b3a', '#a9502f'],
  },
  {
    id: 'SEASONS',
    name: 'Liturgical seasons',
    about: 'Changes with the Church year, like the colors of the vestments.',
    swatches: ['#f3ecdd', '#5b2a6e', '#9b1c1c', '#2f6b3a'],
  },
]

export const DEFAULT_STYLE: AppStyle = 'ILLUMINATED'

export function isAppStyle(value: unknown): value is AppStyle {
  return STYLES.some((style) => style.id === value)
}
