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
  swatches: string[] // background, text, then any number of accent colors (for the picker preview)
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
    about: 'Always dark: candle gold and a warm glow, like a church at Vespers.',
    swatches: ['#0f0c0a', '#f6e7c6', '#e0b866', '#e07a4f'],
  },
  {
    id: 'ATHOS',
    name: 'Holy Mountain',
    about: 'Forest green, grey stone and terracotta roof tiles, like a monastery on Athos.',
    swatches: ['#2e3a24', '#f1ebdc', '#a6472a', '#a5ad8f'],
  },
  {
    id: 'SEASONS',
    name: 'Liturgical seasons',
    about: 'Bright and modern, in the color of the season: purple in Lent, red at Pascha, green at Pentecost...',
    swatches: ['#ffffff', '#1f2328', '#5b2a6e', '#b0221b', '#2f6b3a', '#2a5d9f'],
  },
]

export const DEFAULT_STYLE: AppStyle = 'ILLUMINATED'

export function isAppStyle(value: unknown): value is AppStyle {
  return STYLES.some((style) => style.id === value)
}
