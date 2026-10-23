/*
  sanitize.ts: makes outside HTML safe to show.

  Saints' lives arrive from Orthocal as HTML. React normally refuses to insert
  HTML (it shows text only), because HTML from elsewhere could contain
  scripts: an attack called XSS ("cross-site scripting"). To show the
  formatting anyway, we copy across only a short list of harmless tags, drop
  every attribute (no onclick=..., no links), and keep the text of anything
  else. Only the result of this function is ever given to React's
  dangerouslySetInnerHTML.
*/

const SAFE_TAGS = new Set(['P', 'BR', 'EM', 'I', 'B', 'STRONG', 'BLOCKQUOTE', 'UL', 'OL', 'LI'])
const DROP_COMPLETELY = new Set(['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED'])

export function sanitizeHtml(html: string | null | undefined): string {
  // DOMParser builds a separate, inactive document: nothing in it runs.
  const source = new DOMParser().parseFromString(html ?? '', 'text/html')
  const container = document.createElement('div')

  // Copies the safe parts of "from" into "to", going down through every level.
  function copySafe(from: Node, to: Node) {
    for (const node of Array.from(from.childNodes)) {
      if (node.nodeType === Node.TEXT_NODE) {
        to.appendChild(document.createTextNode(node.textContent ?? ''))
      } else if (node instanceof Element) {
        if (SAFE_TAGS.has(node.tagName)) {
          const clean = document.createElement(node.tagName) // a new, attribute-free copy
          copySafe(node, clean)
          to.appendChild(clean)
        } else if (!DROP_COMPLETELY.has(node.tagName)) {
          copySafe(node, to) // an unknown tag: keep its text only
        }
      }
    }
  }

  copySafe(source.body, container)
  return container.innerHTML
}

/** The same HTML as plain text, with a pause between paragraphs (for reading aloud). */
export function htmlToSpeech(html: string): string {
  const doc = new DOMParser().parseFromString(sanitizeHtml(html), 'text/html')
  return Array.from(doc.body.childNodes)
    .map((node) => (node.textContent ?? '').trim())
    .filter(Boolean)
    .join('. ')
}
