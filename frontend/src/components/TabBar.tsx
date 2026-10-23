/*
  TabBar: Home · Bible · Prayers · Profile, fixed to the bottom of the
  screen, like Instagram's. Home is the default tab: the app opens on it.

  NavLink is a link that knows whether it points to the current page; we use
  that to highlight the active tab. Moving between tabs doesn't reload the
  page: React Router just draws the other screen, which is what makes it feel
  like an app.
*/
import { NavLink, useLocation } from 'react-router'
import { useAccount } from '../account-context'
import type { ReactNode } from 'react'

// Simple line icons, drawn with SVG. stroke="currentColor" makes each icon
// the same colour as its label, so the active tab's icon turns gold too.
function Icon({ children }: { children: ReactNode }) {
  return (
    <svg
      className="tab-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

const HomeIcon = () => (
  <Icon>
    <path d="M3 11l9-7 9 7" />
    <path d="M5 10v10h5v-6h4v6h5V10" />
  </Icon>
)

// an open book
const BibleIcon = () => (
  <Icon>
    <path d="M12 6c-2-1.5-5-2-8-1.5v14c3-.5 6 0 8 1.5" />
    <path d="M12 6c2-1.5 5-2 8-1.5v14c-3-.5-6 0-8 1.5" />
    <path d="M12 6v14" />
  </Icon>
)

// The Orthodox three-bar cross. Its slanted footrest rises on the viewer's
// left: Christ's right, toward the good thief.
const CrossIcon = () => (
  <Icon>
    <path d="M12 2v20" />
    <path d="M9.5 5h5" />
    <path d="M7 9h10" />
    <path d="M9 14.5l6 2.5" />
  </Icon>
)

const PersonIcon = () => (
  <Icon>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c1-4 4.5-6 8-6s7 2 8 6" />
  </Icon>
)

export function TabBar() {
  const { account } = useAccount()
  const { pathname } = useLocation()

  // NavLink gives us isActive; this adds the "active" class for the CSS.
  const tabClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'tab-link active' : 'tab-link')

  // The Profile tab also counts as active on the sign-in page.
  const onProfile = pathname === '/profile' || pathname === '/sign-in'

  return (
    <nav className="tab-bar" aria-label="Main">
      {/* "end" = only active on exactly "/", not on every page starting with "/" */}
      <NavLink to="/" end className={tabClass}>
        <HomeIcon />
        <span>Home</span>
      </NavLink>
      <NavLink to="/bible" className={tabClass}>
        <BibleIcon />
        <span>Bible</span>
      </NavLink>
      <NavLink to="/prayers" className={tabClass}>
        <CrossIcon />
        <span>Prayers</span>
      </NavLink>
      {/* Your photo when signed in, like Instagram; the sign-in page when not. */}
      <NavLink
        to={account ? '/profile' : '/sign-in'}
        className={onProfile ? 'tab-link active' : 'tab-link'}
        aria-current={onProfile ? 'page' : undefined}
      >
        {account?.photoVersion ? (
          <img className="tab-avatar" src={`/api/account/photo?v=${account.photoVersion}`} alt="" />
        ) : (
          <PersonIcon />
        )}
        <span>Profile</span>
      </NavLink>
    </nav>
  )
}
