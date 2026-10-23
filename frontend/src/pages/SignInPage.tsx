/*
  SignInPage ("/sign-in"): sign in, create an account, or enter the 6-digit
  code when two-step verification is on. Talks to AuthController (/api/auth).

  Three panels, one shown at a time: "sign-in", "register" and "code".
*/
import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useSearchParams } from 'react-router'
import { useAccount } from '../account-context'
import { api, ApiError, errorMessage } from '../api'
import { Message, type MessageData } from '../components/common'
import type { SignInOptions, SignInResponse } from '../types'

type Panel = 'sign-in' | 'register' | 'code'

// The same password rules as PasswordPolicy.java, for the live checklist.
// \p{Lu} = any uppercase letter, \p{Ll} = lowercase, \p{Nd} = a digit (the
// "u" flag turns these on). The server checks these and a few more.
const RULES: { label: string; test: (p: string) => boolean }[] = [
  { label: 'At least 12 characters', test: (p) => p.length >= 12 },
  { label: 'An uppercase letter', test: (p) => /\p{Lu}/u.test(p) },
  { label: 'A lowercase letter', test: (p) => /\p{Ll}/u.test(p) },
  { label: 'A number', test: (p) => /\p{Nd}/u.test(p) },
  { label: 'A symbol, such as ! ? # $ % &', test: (p) => /[^\p{L}\p{Nd}\s]/u.test(p) },
]

export function SignInPage() {
  const { account, update } = useAccount()
  const [searchParams] = useSearchParams()
  // Where to go once signed in. It must win over the "already signed in"
  // redirect below, which would otherwise always send you to /profile.
  const [goTo, setGoTo] = useState<string | null>(null)

  const [panel, setPanel] = useState<Panel>('sign-in')
  const [message, setMessage] = useState<MessageData | null>(
    searchParams.get('error') === 'social'
      ? { kind: 'error', text: "Signing in with Google or Apple didn't work. Please try again, or use your email." }
      : null,
  )
  const [busy, setBusy] = useState(false) // true while waiting for the server
  const [options, setOptions] = useState<SignInOptions>({ google: false, apple: false })

  // The form fields. Each input shows one of these, and typing updates it:
  // React calls these "controlled" inputs.
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [confirm, setConfirm] = useState('')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [sentTo, setSentTo] = useState<{ sentTo: string; developmentMode: boolean } | null>(null)

  // Which sign-in buttons are set up (keys in secrets.properties)?
  useEffect(() => {
    api<SignInOptions>('GET', '/api/auth/options')
      .then(setOptions)
      .catch(() => {})
  }, [])

  // Just signed in: go where we planned (replace = the Back button won't
  // return to this page). Otherwise, already signed in: nothing to do here.
  if (goTo) return <Navigate to={goTo} replace />
  if (account) return <Navigate to="/profile" replace />

  const show = (next: Panel) => {
    setPanel(next)
    setMessage(null)
  }

  // Runs a form's action: stops the browser reloading the page, shows a
  // "busy" state, and turns any error into a message.
  const submit = (action: () => Promise<void>) => async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    try {
      await action()
    } catch (error) {
      setMessage({
        kind: 'error',
        text: errorMessage(error),
        problems: error instanceof ApiError ? error.problems : [],
      })
    } finally {
      setBusy(false)
    }
  }

  // Signed in: share the account with the whole app, and go home. React
  // applies both changes together, so the next draw sees goTo and leaves.
  const finish = (result: SignInResponse, to = '/') => {
    setGoTo(to)
    update(result.account)
  }

  const signIn = submit(async () => {
    const result = await api<SignInResponse>('POST', '/api/auth/sign-in', { email, password })
    if (result.status === 'CODE_REQUIRED') {
      setSentTo({ sentTo: result.sentTo ?? '', developmentMode: result.developmentMode })
      setCode('')
      show('code')
    } else {
      finish(result)
    }
  })

  const register = submit(async () => {
    if (password !== confirm) throw new Error("The two passwords don't match.")
    const result = await api<SignInResponse>('POST', '/api/auth/register', { displayName, email, password, phone })
    finish(result, '/profile?welcome=1')
  })

  const verify = submit(async () => {
    finish(await api<SignInResponse>('POST', '/api/auth/sign-in/verify', { code }))
  })

  const resend = async () => {
    try {
      const result = await api<SignInResponse>('POST', '/api/auth/sign-in/resend')
      setMessage({ kind: 'info', text: `We sent a new code to ${result.sentTo}.`, developmentMode: result.developmentMode })
    } catch (error) {
      setMessage({ kind: 'error', text: errorMessage(error) })
    }
  }

  return (
    <main className="page narrow">
      <h1>Orthodox Daily</h1>

      {panel !== 'code' && (
        <div className="tabs">
          <button type="button" className={panel === 'sign-in' ? 'tab active' : 'tab'} onClick={() => show('sign-in')}>
            Sign in
          </button>
          <button type="button" className={panel === 'register' ? 'tab active' : 'tab'} onClick={() => show('register')}>
            Create account
          </button>
        </div>
      )}

      <Message message={message} />

      {panel !== 'code' && (
        <div>
          {/* The first Google or Apple sign-in also creates your account. */}
          <SocialButton provider="Google" enabled={options.google} onUnavailable={setMessage} />
          <SocialButton provider="Apple" enabled={options.apple} onUnavailable={setMessage} />
          <p className="divider">or use your email</p>
        </div>
      )}

      {panel === 'sign-in' && (
        <form className="card" onSubmit={signIn}>
          <label>
            Email
            <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label>
            Password
            <input
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <button type="submit" className="primary" disabled={busy}>
            Sign in
          </button>
        </form>
      )}

      {panel === 'register' && (
        <form className="card" onSubmit={register}>
          <label>
            Your name
            <input autoComplete="name" maxLength={60} required value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </label>
          <label>
            Email
            <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label>
            Password
            <input
              type="password"
              autoComplete="new-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          {/* Ticks itself off as you type: each rule re-checks on every draw. */}
          <ul className="rules">
            {RULES.map((rule) => (
              <li key={rule.label} className={rule.test(password) ? 'met' : ''}>
                {rule.label}
              </li>
            ))}
          </ul>
          <label>
            Confirm password
            <input
              type="password"
              autoComplete="new-password"
              required
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
          </label>
          <label>
            Phone number <span className="optional">(optional)</span>
            <input type="tel" autoComplete="tel" placeholder="(214) 555-0123" value={phone} onChange={(e) => setPhone(e.target.value)} />
          </label>
          <p className="hint">With a phone number you can choose to get sign-in codes by text message.</p>
          <button type="submit" className="primary" disabled={busy}>
            Create account
          </button>
        </form>
      )}

      {panel === 'code' && sentTo && (
        <form className="card" onSubmit={verify}>
          <p>
            We sent a 6-digit code to <strong>{sentTo.sentTo}</strong>. It expires in 10 minutes.
          </p>
          {sentTo.developmentMode && (
            <p className="hint">
              Development mode: codes aren't really sent yet. Find this one in the VS Code terminal where the backend is
              running.
            </p>
          )}
          <label>
            Code
            <input
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              required
              autoFocus
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </label>
          <button type="submit" className="primary" disabled={busy}>
            Verify and sign in
          </button>
          <button type="button" className="link" onClick={resend}>
            Send a new code
          </button>{' '}
          ·{' '}
          <button type="button" className="link" onClick={() => show('sign-in')}>
            Start over
          </button>
        </form>
      )}
    </main>
  )
}

/**
 * "Continue with Google / Apple". These are ordinary links (not React Router
 * links), because signing in leaves our app for Google's or Apple's page and
 * comes back. A provider whose keys aren't set up is greyed out.
 */
function SocialButton({
  provider,
  enabled,
  onUnavailable,
}: {
  provider: 'Google' | 'Apple'
  enabled: boolean
  onUnavailable: (message: MessageData) => void
}) {
  const className = `social-button ${provider.toLowerCase()}`
  if (enabled) {
    return (
      <a className={className} href={`/oauth2/authorization/${provider.toLowerCase()}`}>
        Continue with {provider}
      </a>
    )
  }
  return (
    <button
      type="button"
      className={`${className} disabled`}
      aria-disabled="true"
      onClick={() =>
        onUnavailable({
          kind: 'info',
          text: `${provider} sign-in isn't switched on yet: the app needs ${provider} keys in secrets.properties. See "Setting up sign-in" in the README.`,
        })
      }
    >
      Continue with {provider} <span className="soon">(not set up yet)</span>
    </button>
  )
}
