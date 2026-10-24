/*
  data.ts: loads data from the backend once and keeps it, so going back to a
  tab or a page shows it instantly instead of loading it again.

      const { data, error } = useApiData<LiturgicalDay>('/api/day/2026/10/23')

  data is null until it arrives; error is a message if it couldn't load.

  WHY NOT JUST useEffect + fetch IN EACH PAGE?
  Tabs that are out of sight are paused (TabsHost uses React's <Activity>),
  and when you come back React runs their effects again, which would fetch
  everything again. Here each address is fetched once and remembered in the
  "entries" map, outside any page, so coming back finds it already loaded.

  PULL TO REFRESH
  Each tab passes its own "scope" (e.g. "home:2") through RefreshContext.
  Pulling down to refresh gives the tab a new scope ("home:3"); its addresses
  then count as new, so they're fetched again. Other tabs are unaffected.
*/
import { createContext, useCallback, useContext, useEffect, useSyncExternalStore } from 'react'
import { api, errorMessage } from '../api'

interface Entry {
  data?: unknown
  error?: string
}

const entries = new Map<string, Entry>()
const listeners = new Set<() => void>()

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

function changed() {
  for (const listener of listeners) listener()
}

/** The current tab's scope; TabsHost sets it. */
export const RefreshContext = createContext('app')

/** Fetches (once per scope) and returns the JSON at url. Pass null to load nothing. */
export function useApiData<T>(url: string | null): { data: T | null; error: string | null } {
  const scope = useContext(RefreshContext)
  const key = url === null ? null : `${scope}|${url}`
  // useSyncExternalStore re-draws this component whenever "entries" changes.
  const entry = useSyncExternalStore(subscribe, () => (key === null ? undefined : entries.get(key)))

  useEffect(() => {
    if (key === null || url === null || entries.has(key)) return // already loaded or loading
    entries.set(key, {}) // loading
    api<T>('GET', url)
      .then((data) => entries.set(key, { data }))
      .catch((error) => entries.set(key, { error: errorMessage(error) }))
      .finally(changed)
  }, [key, url])

  return { data: (entry?.data as T | undefined) ?? null, error: entry?.error ?? null }
}

/**
 * Reads something already loaded, without loading it. Used where many items
 * have loaded (e.g. the Psalms in a prayer) and we need all their text at once.
 */
export function usePeekApiData(): <T>(url: string) => T | null {
  const scope = useContext(RefreshContext)
  return useCallback(<T,>(url: string) => (entries.get(`${scope}|${url}`)?.data as T | undefined) ?? null, [scope])
}

/** Forgets everything a scope loaded (after its tab is refreshed). */
export function forgetScope(scope: string) {
  for (const key of entries.keys()) {
    if (key.startsWith(`${scope}|`)) entries.delete(key)
  }
}
