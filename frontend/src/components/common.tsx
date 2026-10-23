/*
  common.tsx: small pieces used on several screens.
*/
import { Fragment } from 'react'
import { listenSupported, play } from '../lib/listen'
import type { Account } from '../types'

/** A "🔊 Listen" button that reads the given text aloud with the device's voice. */
export function ListenButton({ title, text }: { title: string; text: () => string }) {
  if (!listenSupported) return null
  // "text" is a function so the (possibly long) text is only built when clicked.
  return (
    <button type="button" className="listen" onClick={() => play(title, text())}>
      🔊 Listen
    </button>
  )
}

/** Your photo, or your first initial in a circle if you haven't added one. */
export function Avatar({ account, className = 'avatar' }: { account: Account; className?: string }) {
  if (account.photoVersion) {
    return <img className={className} src={`/api/account/photo?v=${account.photoVersion}`} alt="Your profile photo" />
  }
  return (
    <span className={className} aria-hidden="true">
      {account.displayName.charAt(0).toUpperCase()}
    </span>
  )
}

export interface MessageData {
  kind: 'error' | 'info'
  text: string
  problems?: string[] // e.g. password rules that weren't met
  developmentMode?: boolean // a code was printed in the backend's terminal, not sent
}

/** A red (error) or green (info) message box. */
export function Message({ message }: { message: MessageData | null }) {
  if (!message) return null
  return (
    <div className={`message ${message.kind}`} role={message.kind === 'error' ? 'alert' : 'status'}>
      {message.text}
      {message.problems && message.problems.length > 0 && (
        <ul>
          {message.problems.map((problem) => (
            <li key={problem}>{problem}</li>
          ))}
        </ul>
      )}
      {message.developmentMode && (
        <span className="dev">
          Development mode: codes aren't really sent yet. Find this one in the VS Code terminal where the backend is
          running.
        </span>
      )}
    </div>
  )
}

/**
 * Shows scripture text with the translators' added words in italics. Those
 * words are in [square brackets] in the text; split() on a pattern with a
 * (group) keeps the bracketed parts at the odd positions of the list.
 */
export function ScriptureText({ text }: { text: string }) {
  const parts = text.split(/\[([^\]]+)\]/)
  return (
    <>
      {parts.map((part, i) => (i % 2 === 1 ? <i key={i}>{part}</i> : <Fragment key={i}>{part}</Fragment>))}
    </>
  )
}
