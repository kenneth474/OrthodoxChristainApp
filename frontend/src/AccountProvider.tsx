/*
  AccountProvider: loads the signed-in account once, and shares it with
  every screen through AccountContext (see account-context.ts).
*/
import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { AccountContext } from './account-context'
import { api } from './api'
import { rememberAppearance } from './lib/appearance'
import type { Account } from './types'

export function AccountProvider({ children }: { children: ReactNode }) {
  // useState = a value React remembers between draws; changing it re-draws.
  const [account, setAccount] = useState<Account | null>(null)
  const [loading, setLoading] = useState(true)

  // useCallback keeps the same function between draws, so components that
  // use it in their own effects don't re-run them needlessly.
  const update = useCallback((next: Account | null) => {
    setAccount(next)
    if (next) rememberAppearance(next) // apply their text size and theme
  }, [])

  // Asks the backend who's signed in. 401 = nobody (or the backend is offline).
  const refresh = useCallback(
    () =>
      api<Account>('GET', '/api/account')
        .then(update)
        .catch(() => update(null))
        .finally(() => setLoading(false)),
    [update],
  )

  // useEffect runs after the first draw: here, ask who's signed in, once.
  useEffect(() => {
    void refresh()
  }, [refresh])

  return <AccountContext.Provider value={{ account, loading, refresh, update }}>{children}</AccountContext.Provider>
}
