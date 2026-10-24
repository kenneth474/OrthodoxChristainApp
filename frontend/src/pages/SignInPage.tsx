/*
  SignInPage ("/sign-in"): sign in, create an account, or enter the 6-digit
  code when two-step verification is on. Talks to AuthController (/api/auth).

  Three panels, one shown at a time: "sign-in", "register" and "code".
*/
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { useAccount } from '../account-context'
import { api, ApiError, errorMessage } from '../api'
import { Message, type MessageData } from '../components/common'
import { Masthead } from '../components/ornaments'
import { StylePicker } from '../components/StylePicker'
import { currentStyle, previewStyle } from '../lib/appearance'
import { useApiData } from '../lib/data'
import type { AppStyle, SignInOptions, SignInResponse, TwoFactorMethod } from '../types'

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
  const { update } = useAccount()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const [panel, setPanel] = useState<Panel>('sign-in')
  const [message, setMessage] = useState<MessageData | null>(
    searchParams.get('error') === 'social'
      ? { kind: 'error', text: "Signing in with Google didn't work. Please try again, or use your email." }
      : null,
  )
  const [busy, setBusy] = useState(false) // true while waiting for the server
  // Which sign-in buttons are set up (keys in secrets.properties)?
  const options = useApiData<SignInOptions>('/api/auth/options').data

  // The form fields. Each input shows one of these, and typing updates it:
  // React calls these "controlled" inputs.
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [confirm, setConfirm] = useState('')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  // Where the second-step code is (after a right password, with two-step verification on).
  const [challenge, setChallenge] = useState<{ method: TwoFactorMethod; sentTo: string; developmentMode: boolean } | null>(null)
  const [useBackupCode, setUseBackupCode] = useState(false)
  // The look chosen for the new account. Picking one shows it straight away.
  const [style, setStyle] = useState<AppStyle>(currentStyle)
  const chooseStyle = (next: AppStyle) => {
    setStyle(next)
    previewStyle(next)
  }

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

  // Signed in: share the account with the whole app (the Profile tab then
  // shows your profile, see ProfileTab) and go where we planned (replace =
  // the Back button won't return to this page).
  const finish = (result: SignInResponse, to = '/') => {
    navigate(to, { replace: true })
    update(result.account)
  }

  const signIn = submit(async () => {
    const result = await api<SignInResponse>('POST', '/api/auth/sign-in', { email, password })
    if (result.status === 'CODE_REQUIRED') {
      setChallenge({ method: result.method ?? 'EMAIL', sentTo: result.sentTo ?? '', developmentMode: result.developmentMode })
      setUseBackupCode(false)
      setCode('')
      show('code')
    } else {
      finish(result)
    }
  })

  const register = submit(async () => {
    if (password !== confirm) throw new Error("The two passwords don't match.")
    const result = await api<SignInResponse>('POST', '/api/auth/register', { displayName, email, password, phone, style })
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
    <div className="screen">
      <Masthead eyebrow="Welcome to" title="Orthodox Daily" subtitle="Sign in to keep your profile, settings and style on every device." />
      <main className="page narrow">

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
          {/* The first Google sign-in also creates your account. */}
          <GoogleButton enabled={options?.google ?? false} onUnavailable={setMessage} />
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
          <fieldset className="choices">
            <legend>Choose a style</legend>
            <p className="hint">How the app looks. You can change it any time in Profile.</p>
            <StylePicker value={style} onChange={chooseStyle} />
          </fieldset>
          <button type="submit" className="primary" disabled={busy}>
            Create account
          </button>
        </form>
      )}

      {panel === 'code' && challenge && (
        <form className="card" onSubmit={verify}>
          {challenge.method === 'AUTHENTICATOR' ? (
            <p>
              {useBackupCode
                ? 'Type one of the backup codes you saved when you set up your authenticator app. Each works once.'
                : 'Open your authenticator app and type the 6-digit code it shows for Orthodox Daily.'}
            </p>
          ) : (
            <p>
              We {challenge.method === 'SMS' ? 'texted' : 'emailed'} a 6-digit code to <strong>{challenge.sentTo}</strong>.
              It expires in 10 minutes.
            </p>
          )}
          {challenge.developmentMode && (
            <p className="hint">
              Development mode: codes aren't really sent yet. Find this one in the VS Code terminal where the backend is
              running.
            </p>
          )}
          <label>
            {useBackupCode ? 'Backup code' : 'Code'}
            <input
              inputMode={useBackupCode ? 'text' : 'numeric'}
              autoComplete="one-time-code"
              autoCapitalize="characters"
              maxLength={useBackupCode ? 11 : 6}
              required
              autoFocus
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </label>
          <button type="submit" className="primary" disabled={busy}>
            Verify and sign in
          </button>
          {challenge.method === 'AUTHENTICATOR' ? (
            <button
              type="button"
              className="link"
              onClick={() => {
                setUseBackupCode(!useBackupCode)
                setCode('')
              }}
            >
              {useBackupCode ? 'Use the code from my app' : 'Lost your phone? Use a backup code'}
            </button>
          ) : (
            <button type="button" className="link" onClick={resend}>
              Send a new code
            </button>
          )}{' '}
          ·{' '}
          <button type="button" className="link" onClick={() => show('sign-in')}>
            Start over
          </button>
        </form>
      )}
      </main>
    </div>
  )
}

/**
 * "Continue with Google". It's an ordinary link (not a React Router link),
 * because signing in leaves our app for Google's page and comes back. Until
 * the Google keys are set up, it's greyed out.
 */
function GoogleButton({ enabled, onUnavailable }: { enabled: boolean; onUnavailable: (message: MessageData) => void }) {
  const className = 'social-button google'
  if (enabled) {
    return (
      <a className={className} href="/oauth2/authorization/google">
        Continue with Google
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
          text: `Google sign-in isn't switched on yet: the app needs its Google keys. See "Setting up sign-in" in the README.`,
        })
      }
    >
      Continue with Google <span className="soon">(not set up yet)</span>
    </button>
  )
}
