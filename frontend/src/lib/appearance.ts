/*
  appearance.ts: applies your text size and theme (styles/theme.css).

  They're set as two attributes on the <html> tag:
      data-theme="system" | "light" | "dark"
      data-text-size="normal" | "large" | "larger"
  Your choice is saved to your account; a copy is also kept in this browser
  (localStorage) so index.html can apply it before the page is drawn.
*/
import type { Account, TextSize, Theme } from '../types'

const KEY = 'orthodoxDaily.appearance'

interface Settings {
  theme?: Theme
  textSize?: TextSize
}

function apply(settings: Settings | null) {
  const root = document.documentElement
  root.dataset.theme = (settings?.theme ?? 'SYSTEM').toLowerCase()
  root.dataset.textSize = (settings?.textSize ?? 'NORMAL').toLowerCase()
}

/** Apply the signed-in account's settings, and remember them in this browser. */
export function rememberAppearance(account: Account) {
  const settings: Settings = { theme: account.theme, textSize: account.textSize }
  apply(settings)
  try {
    localStorage.setItem(KEY, JSON.stringify(settings))
  } catch {
    // storage can be switched off (e.g. private browsing); the app still works
  }
}

/** On sign-out, so the next person using this browser gets the defaults. */
export function forgetAppearance() {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // nothing to forget
  }
  apply(null)
}
