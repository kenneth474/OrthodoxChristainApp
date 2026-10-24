/*
  ProfileTab: what the Profile tab shows, at /profile or /sign-in: your
  profile when you're signed in, the sign-in form when you're not.

  Deciding here, instead of each page sending you to the other one with a
  redirect, matters because tabs stay alive in the background (TabsHost):
  a redirect remembered by a hidden page would fire again later, at the
  wrong moment. Signing in or out now simply changes what this shows.
*/
import { useAccount } from '../account-context'
import { ProfilePage } from './ProfilePage'
import { SignInPage } from './SignInPage'

export function ProfileTab() {
  const { account, loading } = useAccount()
  if (loading) {
    return (
      <main className="page narrow">
        <p className="status">Loading…</p>
      </main>
    )
  }
  return account ? <ProfilePage /> : <SignInPage />
}
