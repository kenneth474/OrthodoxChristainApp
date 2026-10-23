/*
  scripture.ts: small helpers for scripture text.
*/

/** Scripture text for reading aloud: without the translators' [brackets]. */
export function plainScripture(text: string): string {
  return text.replace(/[[\]]/g, '')
}

/**
 * Groups verses into paragraphs. A new paragraph starts at the first verse
 * and wherever a verse is marked paragraphStart. If no verse is marked
 * (the Septuagint text has no paragraph marks), every verse gets its own line.
 */
export function toParagraphs<V extends { paragraphStart: boolean }>(verses: V[]): V[][] {
  const marked = verses.some((v) => v.paragraphStart)
  const paragraphs: V[][] = []
  verses.forEach((verse, i) => {
    if (i === 0 || !marked || verse.paragraphStart) paragraphs.push([])
    paragraphs[paragraphs.length - 1].push(verse)
  })
  return paragraphs
}
