/*
  TabBar: Home · Bible · Prayers · Church · Profile, fixed to the bottom of
  the screen, like Instagram's. Home is the default tab: the app opens on it.

  Each tab remembers where you were (see TabsHost.tsx): tapping Bible takes
  you back to the chapter you were reading. Tapping the tab you're already
  on goes back to that tab's first page (or to the top of it).
*/
import type { MouseEvent, ReactNode } from 'react'
import { Link } from 'react-router'
import { useAccount } from '../account-context'
import { TABS, type TabId } from '../lib/tabs'
import { Cross } from './ornaments'
import { useTabs } from '../tabs-context'

// Simple line icons, drawn with SVG in "currentColor", so the active tab's
// icon takes the style's accent color too.
function Icon({ children }: { children: ReactNode }) {
  return (
    <svg className="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  )
}

const ICONS: Record<TabId, () => ReactNode> = {
  // a house with a little cross: home
  home: () => (
    <Icon>
      <path d="M3.5 11 12 4l8.5 7" />
      <path d="M5.5 9.5V20h13V9.5" />
      <path d="M12 11.5v5M10 13.2h4" />
    </Icon>
  ),
  // an open book
  bible: () => (
    <Icon>
      <path d="M12 6c-2-1.5-5-2-8-1.5v14c3-.5 6 0 8 1.5" />
      <path d="M12 6c2-1.5 5-2 8-1.5v14c-3-.5-6 0-8 1.5" />
      <path d="M12 6v14" />
    </Icon>
  ),
  // the three-bar cross
  prayers: () => <Cross className="tab-icon" />,
  // a church with an onion dome
  church: () => (
    <Icon>
      <path d="M12 1.8v2.6M10.9 2.9h2.2" />
      <path d="M12 4.6c-2.6 1.9-4 3.5-4 5.6h8c0-2.1-1.4-3.7-4-5.6z" />
      <path d="M6.5 21v-10.8h11V21" />
      <path d="M10.2 21v-3.6a1.8 1.8 0 0 1 3.6 0V21" />
      <path d="M4 21h16" />
    </Icon>
  ),
  profile: () => (
    <Icon>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c1-4 4.5-6 8-6s7 2 8 6" />
    </Icon>
  ),
}

export function TabBar({ onReselect }: { onReselect: (tab: TabId) => void }) {
  const { account } = useAccount()
  const { active, pathFor } = useTabs()

  return (
    <nav className="tab-bar" aria-label="Main">
      {TABS.map((tab) => {
        const isActive = tab.id === active
        const onClick = (event: MouseEvent) => {
          if (!isActive) return // a normal link: go to the tab's last page
          event.preventDefault()
          onReselect(tab.id)
        }
        return (
          <Link
            key={tab.id}
            to={pathFor(tab.id)}
            className={isActive ? 'tab-link active' : 'tab-link'}
            aria-current={isActive ? 'page' : undefined}
            onClick={onClick}
          >
            {/* Your photo on the Profile tab when signed in, like Instagram. */}
            {tab.id === 'profile' && account?.photoVersion ? (
              <img className="tab-avatar" src={`/api/account/photo?v=${account.photoVersion}`} alt="" />
            ) : (
              ICONS[tab.id]()
            )}
            <span>{tab.label}</span>
          </Link>
        )
      })}
    </nav>
  )
}
