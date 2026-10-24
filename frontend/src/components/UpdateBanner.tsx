/*
  UpdateBanner: "A new version of the app is ready. Tap to update."

  WHY
  A phone keeps a home-screen app running in the background for days. When
  a new version goes online, that open copy keeps running the OLD code until
  it's fully closed, so people can be stuck with bugs that are long fixed.

  HOW
  Each build of the app has its own file names (assets/index-AbC123.js).
  Whenever the app comes back to the screen (and every 10 minutes while it's
  open), we fetch the page again and compare its script's name with the one
  we're running. Different = a new version is online, and the banner offers
  to reload. Nothing happens while developing (npm run dev), where there's
  no built file to compare.
*/
import { useEffect, useState } from 'react'

const CHECK_EVERY = 10 * 60 * 1000 // 10 minutes

// The built script this page is running, e.g. "/assets/index-AbC123.js".
function runningScript(): string | null {
  const script = document.querySelector<HTMLScriptElement>('script[type="module"][src*="/assets/index-"]')
  return script ? new URL(script.src).pathname : null
}

async function onlineScript(): Promise<string | null> {
  // no-store: always ask the server, never a saved copy.
  const html = await (await fetch('/', { cache: 'no-store' })).text()
  return html.match(/\/assets\/index-[\w-]+\.js/)?.[0] ?? null
}

export function UpdateBanner() {
  const [ready, setReady] = useState(false)

  useEffect(() => {
    const running = runningScript()
    if (!running) return // development: nothing to compare

    const check = () => {
      if (document.visibilityState !== 'visible') return
      onlineScript()
        .then((online) => {
          if (online && online !== running) setReady(true)
        })
        .catch(() => {}) // offline: try again next time
    }
    document.addEventListener('visibilitychange', check)
    const timer = window.setInterval(check, CHECK_EVERY)
    return () => {
      document.removeEventListener('visibilitychange', check)
      window.clearInterval(timer)
    }
  }, [])

  if (!ready) return null
  return (
    <button type="button" className="update-banner" onClick={() => window.location.reload()}>
      A new version of the app is ready. <strong>Tap to update</strong>
    </button>
  )
}
