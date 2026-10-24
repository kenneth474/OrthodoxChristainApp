/*
  PullToRefresh: at the top of a page, pull down and let go to refresh it,
  like in most phone apps. A gold cross follows your finger and turns as you
  pull; past the line, letting go refreshes the current tab.

  It listens to touches on the whole window, so it only works with a finger
  (on a computer, use the browser's reload). styles/app.css sets
  "overscroll-behavior: none" so the browser's own pull-to-reload, which
  would restart the whole app, doesn't happen instead.
*/
import { useEffect, useRef, useState } from 'react'
import { Cross } from './ornaments'

const TRIGGER = 70 // how far (in pixels) to pull before letting go refreshes
const MAX = 110

export function PullToRefresh({ onRefresh }: { onRefresh: () => void }) {
  const [pull, setPull] = useState(0)
  const [refreshing, setRefreshing] = useState(false)

  // Refs hold values the touch handlers need without re-attaching them.
  const startY = useRef<number | null>(null)
  const pulled = useRef(0)
  const busy = useRef(false)
  const latestOnRefresh = useRef(onRefresh)
  useEffect(() => {
    latestOnRefresh.current = onRefresh
  })

  useEffect(() => {
    const show = (distance: number) => {
      pulled.current = distance
      setPull(distance)
    }
    const onStart = (e: TouchEvent) => {
      startY.current = window.scrollY <= 0 && !busy.current ? e.touches[0].clientY : null
    }
    const onMove = (e: TouchEvent) => {
      if (startY.current === null) return
      const distance = e.touches[0].clientY - startY.current
      if (distance <= 0 || window.scrollY > 0) {
        show(0)
        return
      }
      e.preventDefault() // we're pulling: don't let the page bounce as well
      show(Math.min(MAX, distance * 0.5)) // half speed feels like stretching
    }
    const onEnd = () => {
      if (startY.current === null) return
      startY.current = null
      if (pulled.current >= TRIGGER) {
        busy.current = true
        setRefreshing(true)
        latestOnRefresh.current()
        window.setTimeout(() => {
          busy.current = false
          setRefreshing(false)
        }, 800)
      }
      show(0)
    }
    window.addEventListener('touchstart', onStart, { passive: true })
    window.addEventListener('touchmove', onMove, { passive: false })
    window.addEventListener('touchend', onEnd)
    window.addEventListener('touchcancel', onEnd)
    return () => {
      window.removeEventListener('touchstart', onStart)
      window.removeEventListener('touchmove', onMove)
      window.removeEventListener('touchend', onEnd)
      window.removeEventListener('touchcancel', onEnd)
    }
  }, [])

  const offset = refreshing ? TRIGGER * 0.8 : pull
  if (offset === 0) return null
  return (
    <div
      className={refreshing ? 'pull-indicator refreshing' : pull >= TRIGGER ? 'pull-indicator ready' : 'pull-indicator'}
      style={{ transform: `translate(-50%, ${offset - 44}px)` }}
      role="status"
    >
      {/* Turns with the pull; while refreshing, the CSS spins it instead. */}
      <span className="pull-cross" style={refreshing ? undefined : { transform: `rotate(${pull * 3}deg)` }}>
        <Cross />
      </span>
      <span className="sr-only">{refreshing ? 'Refreshing' : 'Pull to refresh'}</span>
    </div>
  )
}
