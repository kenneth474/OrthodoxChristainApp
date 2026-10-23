/*
  App: the whole app's layout and its addresses ("routes").

      /                      Home tab (the default): the liturgical day
      /bible, /bible/PSA, /bible/PSA/50                    Bible tab
      /prayers, /prayers/morning                            Prayers tab
      /profile               Profile tab: your account
      /sign-in               signing in (also reached from the Profile tab)

  Every screen shares the tab bar at the bottom and the reading-aloud player.
  The backend sends this same app for each of these addresses
  (SpaController.java), so reloading or bookmarking any of them works.
*/
import { Navigate, Route, Routes } from 'react-router'
import { AccountProvider } from './AccountProvider'
import { Player } from './components/Player'
import { TabBar } from './components/TabBar'
import { BiblePage } from './pages/BiblePage'
import { HomePage } from './pages/HomePage'
import { PrayersPage } from './pages/PrayersPage'
import { ProfilePage } from './pages/ProfilePage'
import { SignInPage } from './pages/SignInPage'

export function App() {
  return (
    <AccountProvider>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/bible" element={<BiblePage />} />
        <Route path="/bible/:book" element={<BiblePage />} />
        <Route path="/bible/:book/:chapter" element={<BiblePage />} />
        <Route path="/prayers" element={<PrayersPage />} />
        <Route path="/prayers/:section" element={<PrayersPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/sign-in" element={<SignInPage />} />
        {/* Any other address: back to Home. */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Player />
      <TabBar />
    </AccountProvider>
  )
}
