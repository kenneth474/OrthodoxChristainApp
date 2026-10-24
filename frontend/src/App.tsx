/*
  App: the whole app's layout and its addresses.

      /                      Home tab (the default): the liturgical day
      /bible, /bible/PSA, /bible/PSA/50                    Bible tab
      /prayers, /prayers/morning                            Prayers tab
      /church, /church/dioceses/wichita, /church/ministries Church tab
      /profile               Profile tab: your account
      /sign-in               signing in (also reached from the Profile tab)

  TabsHost (components/TabsHost.tsx) draws the tabs and keeps each one where
  you left it; it also has the list of screens for each address. The
  reading-aloud player floats above the tab bar on every screen.

  The backend sends this same app for each of these addresses
  (SpaController.java), so reloading or bookmarking any of them works.
*/
import { AccountProvider } from './AccountProvider'
import { Player } from './components/Player'
import { TabsHost } from './components/TabsHost'

export function App() {
  return (
    <AccountProvider>
      <TabsHost />
      <Player />
    </AccountProvider>
  )
}
