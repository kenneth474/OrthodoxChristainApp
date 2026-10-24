/*
  feasts.ts: the names of the Typikon's day ranks ("feast levels" 1 to 8 in
  Orthocal's data). components/ornaments.tsx draws their signs.
*/
const RANKS: Record<number, string> = {
  1: 'Presanctified Liturgy',
  2: 'Six stichera',
  3: 'Doxology',
  4: 'Polyeleos',
  5: 'Vigil',
  6: 'Great feast',
  7: 'Great feast of the Theotokos',
  8: 'Great feast of the Lord',
}

/** The name of a day's rank, or "" for an ordinary day. */
export function feastRank(level: number): string {
  return RANKS[level] ?? ''
}
