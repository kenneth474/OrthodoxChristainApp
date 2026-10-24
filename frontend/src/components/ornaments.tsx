/*
  ornaments.tsx: the small Orthodox details that give every style its
  character: the three-bar cross, the divider under each page title, the
  page header ("masthead") and the Typikon's feast signs.

  All are drawn with SVG in "currentColor", so each style colors them
  through its CSS (styles/theme.css).
*/
import type { ReactNode } from 'react'
import { feastRank } from '../lib/feasts'

/**
 * The Orthodox three-bar cross. Its slanted footrest rises on the viewer's
 * left: Christ's right, toward the good thief.
 */
export function Cross({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" aria-hidden="true">
      <path d="M12 2v20M9.5 5h5M7 9h10M9 14.5l6 2.5" />
    </svg>
  )
}

/** A hairline with a small cross in the middle, under each page title. */
export function Ornament() {
  return (
    <div className="ornament" aria-hidden="true">
      <span />
      <Cross />
      <span />
    </div>
  )
}

/** The top of every screen: a small line above the title, the title, a subtitle, the ornament. */
export function Masthead({
  eyebrow,
  title,
  subtitle,
  children,
}: {
  eyebrow?: ReactNode
  title: ReactNode
  subtitle?: ReactNode
  children?: ReactNode
}) {
  return (
    <header className="masthead">
      <div className="masthead-inner">
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
        {children}
        <Ornament />
      </div>
    </header>
  )
}

/*
  FEAST SIGNS
  Service books mark how solemn each day is with a small sign, the oldest
  ones in red ink. Orthocal numbers them 1 to 8 ("feast level"):
     2  six stichera   a black semicircle with three dots
     3  doxology       the same, in red
     4  polyeleos      a red cross
     5  vigil          a red cross in a semicircle
     6  great feast    a red cross in a circle
     7, 8  great feasts of the Theotokos and of the Lord: also a cross in a circle
*/
export function FeastSign({ level }: { level: number }) {
  if (level < 2) return null
  const cross = <path d="M12 6.5v11M6.5 12h11" />
  return (
    <svg className={level === 2 ? 'feast-sign black' : 'feast-sign'} viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" role="img" aria-label={feastRank(level)}>
      {level <= 3 && (
        <>
          <path d="M5 9a7 7 0 0 0 14 0" />
          <circle cx="9" cy="11.5" r="1" fill="currentColor" stroke="none" />
          <circle cx="12" cy="13.5" r="1" fill="currentColor" stroke="none" />
          <circle cx="15" cy="11.5" r="1" fill="currentColor" stroke="none" />
        </>
      )}
      {level >= 4 && cross}
      {level === 5 && <path d="M3.5 12a8.5 8.5 0 0 0 17 0" />}
      {level >= 6 && <circle cx="12" cy="12" r="8.5" />}
    </svg>
  )
}
