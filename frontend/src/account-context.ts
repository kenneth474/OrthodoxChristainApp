/*
  account-context.ts: who is signed in, shared with every screen.

  WHAT IS A CONTEXT?
  Many parts of the app need the signed-in account: the Profile tab (for
  your photo), the home page (for your name day), the Profile page itself.
  Instead of passing it down through every component, React "context" makes
  it available anywhere inside <AccountProvider> (see AccountProvider.tsx):
      const { account } = useAccount()
*/
import { createContext, useContext } from 'react'
import type { Account } from './types'

export interface AccountState {
  account: Account | null // null = not signed in
  loading: boolean // true until we know whether someone is signed in
  refresh: () => Promise<void> // ask the backend again (e.g. after signing in)
  update: (account: Account | null) => void // use an account the backend just sent back
}

export const AccountContext = createContext<AccountState | null>(null)

export function useAccount(): AccountState {
  const value = useContext(AccountContext)
  if (!value) {
    throw new Error('useAccount() must be used inside <AccountProvider>')
  }
  return value
}
