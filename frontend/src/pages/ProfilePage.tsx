/*
  ProfilePage (the Profile tab, "/profile"): your account.

  Profile (photo, name, patron saint, name day, home parish), how the app
  looks, your email and phone, two-step verification, and signing out.
  Talks to AccountController (/api/account/...). Every change sends back the
  updated account, which update() shares with the whole app (so the photo in
  the tab bar changes at once, for example).
*/
import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Navigate, useSearchParams } from 'react-router'
import { useAccount } from '../account-context'
import { api, ApiError, errorMessage } from '../api'
import { Avatar, Message, type MessageData } from '../components/common'
import { forgetAppearance } from '../lib/appearance'
import type { Account, SentCode, TextSize, Theme, TwoFactorMethod } from '../types'

export function ProfilePage() {
  const { account, loading, update } = useAccount()
  const [searchParams] = useSearchParams()
  const [message, setMessage] = useState<MessageData | null>(
    searchParams.get('welcome')
      ? { kind: 'info', text: 'Welcome! Verify your email next, so you can use it for two-step verification.' }
      : null,
  )
  // Set when signing out, so we go Home instead of to the sign-in page
  // (which is where a signed-out visit to this page would normally go).
  const [leaving, setLeaving] = useState<string | null>(null)

  if (leaving) return <Navigate to={leaving} replace />
  if (loading) return <main className="page narrow"><p className="status">Loading…</p></main>
  if (!account) return <Navigate to="/sign-in" replace />

  /**
   * Runs an account change and shows the result. Anything that changes the
   * account sends the new one back, which we share with the whole app.
   * A 401 means the session ended: go to sign-in.
   */
  const run = async (action: () => Promise<Account | void>, success?: string) => {
    try {
      const changed = await action()
      if (changed) update(changed)
      if (success) setMessage({ kind: 'info', text: success })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        update(null) // signed out: the check above then sends you to /sign-in
        return
      }
      setMessage({ kind: 'error', text: errorMessage(error), problems: error instanceof ApiError ? error.problems : [] })
    }
    window.scrollTo({ top: 0, behavior: 'smooth' }) // the message is at the top
  }

  const signOut = () =>
    run(async () => {
      await api('POST', '/api/auth/sign-out')
      forgetAppearance() // the next person on this device gets the defaults
      setLeaving('/')
      update(null)
    })

  return (
    <main className="page narrow">
      <h1>Your account</h1>
      <Message message={message} />

      {/* key=...: if a different account signs in, start these forms afresh. */}
      <ProfileCard key={account.email} account={account} run={run} />
      <AppearanceCard account={account} run={run} />
      <EmailCard account={account} run={run} setMessage={setMessage} />
      <PhoneCard key={`phone-${account.email}`} account={account} run={run} setMessage={setMessage} />
      <TwoFactorCard account={account} run={run} />

      <section className="card">
        <h2>Ways you sign in</h2>
        <ul className="plain">
          {account.hasPassword && <li>✓ Email and password</li>}
          {account.googleLinked && <li>✓ Google</li>}
          {account.appleLinked && <li>✓ Apple</li>}
        </ul>
      </section>

      <button type="button" onClick={signOut}>
        Sign out
      </button>
    </main>
  )
}

type Run = (action: () => Promise<Account | void>, success?: string) => Promise<void>
interface CardProps {
  account: Account
  run: Run
}

function Card({ title, children }: { title: ReactNode; children: ReactNode }) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {children}
    </section>
  )
}

// ----- Profile: photo, name, patron saint, name day, home parish -----

function ProfileCard({ account, run }: CardProps) {
  // The fields start with the saved values; typing changes them here until "Save".
  const [displayName, setDisplayName] = useState(account.displayName)
  const [patronSaint, setPatronSaint] = useState(account.patronSaint ?? '')
  // The date picker needs a full date: use this year (or leap year 2028 for
  // Feb 29). The function form of useState works this out only once.
  const [nameDay, setNameDay] = useState(() => {
    if (!account.nameDay) return ''
    const year = account.nameDay === '02-29' ? 2028 : new Date().getFullYear()
    return `${year}-${account.nameDay}`
  })
  const [homeParish, setHomeParish] = useState(account.homeParish ?? '')

  // useRef = a handle on a real page element: here, the hidden file picker.
  const filePicker = useRef<HTMLInputElement>(null)

  const save = (event: FormEvent) => {
    event.preventDefault()
    void run(
      () => api<Account>('PUT', '/api/account/profile', { displayName, patronSaint, nameDay, homeParish }),
      'Profile saved.',
    )
  }

  const upload = (file: File | undefined) => {
    if (!file) return
    void run(async () => {
      let square: Blob
      try {
        square = await squarePhoto(file)
      } catch {
        throw new Error('Please choose a JPEG or PNG photo.')
      }
      const form = new FormData()
      form.append('photo', square, 'photo.jpg')
      return api<Account>('POST', '/api/account/photo', form)
    }, 'Photo saved.')
  }

  return (
    <Card title="Profile">
      <div className="photo-row">
        <Avatar account={account} />
        <div className="buttons">
          <button type="button" onClick={() => filePicker.current?.click()}>
            Change photo
          </button>
          {account.photoVersion && (
            <button
              type="button"
              className="link"
              onClick={() => run(() => api<Account>('DELETE', '/api/account/photo'), 'Photo removed.')}
            >
              Remove photo
            </button>
          )}
          <input
            ref={filePicker}
            type="file"
            accept="image/jpeg,image/png"
            hidden
            onChange={(e) => {
              upload(e.target.files?.[0])
              e.target.value = '' // so choosing the same file again still counts
            }}
          />
        </div>
      </div>
      <form onSubmit={save}>
        <label>
          Name
          <input maxLength={60} required autoComplete="name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
        </label>
        <label>
          Patron saint <span className="optional">(optional)</span>
          <input maxLength={100} placeholder="e.g. St. Nicholas the Wonderworker" value={patronSaint} onChange={(e) => setPatronSaint(e.target.value)} />
        </label>
        <label>
          Name day <span className="optional">(optional: your patron saint's feast)</span>
          <input type="date" value={nameDay} onChange={(e) => setNameDay(e.target.value)} />
        </label>
        <label>
          Home parish <span className="optional">(optional)</span>
          <input
            maxLength={120}
            placeholder="e.g. Saints Constantine and Helen, Carrollton"
            value={homeParish}
            onChange={(e) => setHomeParish(e.target.value)}
          />
        </label>
        <button type="submit">Save profile</button>
      </form>
    </Card>
  )
}

/**
 * Crops a photo to the middle square and shrinks it to 512 x 512, using a
 * <canvas> (a picture you can draw on in code). This also turns phone photos
 * the right way up and makes the upload tiny. The server still checks and
 * re-saves it (ProfilePhotoService.java).
 */
async function squarePhoto(file: File): Promise<Blob> {
  const image = await createImageBitmap(file) // fails if it isn't a picture
  const side = Math.min(image.width, image.height)
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = 512
  canvas
    .getContext('2d')!
    .drawImage(image, (image.width - side) / 2, (image.height - side) / 2, side, side, 0, 0, 512, 512)
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('no image'))), 'image/jpeg', 0.9),
  )
}

// ----- Appearance: saved the moment you pick an option -----

function AppearanceCard({ account, run }: CardProps) {
  const save = (textSize: TextSize, theme: Theme) =>
    run(() => api<Account>('PUT', '/api/account/appearance', { textSize, theme }))

  return (
    <Card title="Appearance">
      <fieldset className="choices">
        <legend>Text size</legend>
        <div className="row">
          {(['NORMAL', 'LARGE', 'LARGER'] as TextSize[]).map((size) => (
            <label key={size}>
              <input
                type="radio"
                name="textSize"
                checked={account.textSize === size}
                onChange={() => save(size, account.theme)}
              />{' '}
              {size === 'NORMAL' ? 'Normal' : size === 'LARGE' ? 'Large' : 'Larger'}
            </label>
          ))}
        </div>
      </fieldset>
      <fieldset className="choices">
        <legend>Theme</legend>
        <div className="row">
          {(['SYSTEM', 'LIGHT', 'DARK'] as Theme[]).map((theme) => (
            <label key={theme}>
              <input
                type="radio"
                name="theme"
                checked={account.theme === theme}
                onChange={() => save(account.textSize, theme)}
              />{' '}
              {theme === 'SYSTEM' ? 'Match my device' : theme === 'LIGHT' ? 'Light' : 'Dark'}
            </label>
          ))}
        </div>
      </fieldset>
    </Card>
  )
}

// ----- Email and phone verification -----

interface VerifyProps extends CardProps {
  setMessage: (message: MessageData) => void
}

/** "Not verified" / "Verified" */
function Badge({ verified }: { verified: boolean }) {
  return <span className={`badge ${verified ? 'ok' : 'warn'}`}>{verified ? 'Verified' : 'Not verified'}</span>
}

/** The little "6-digit code + Verify" form, shown after a code is sent. */
function CodeForm({ label, onVerify }: { label: string; onVerify: (code: string) => Promise<void> }) {
  const [code, setCode] = useState('')
  return (
    <form
      className="inline"
      onSubmit={(e) => {
        e.preventDefault()
        void onVerify(code).then(() => setCode(''))
      }}
    >
      <input
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        placeholder="6-digit code"
        aria-label={label}
        required
        autoFocus
        value={code}
        onChange={(e) => setCode(e.target.value)}
      />
      <button type="submit">Verify</button>
    </form>
  )
}

function codeSentMessage(result: SentCode, verb: string): MessageData {
  return { kind: 'info', text: `We ${verb} a code to ${result.sentTo}.`, developmentMode: result.developmentMode }
}

function EmailCard({ account, run, setMessage }: VerifyProps) {
  const [codeSent, setCodeSent] = useState(false)

  const sendCode = () =>
    run(async () => {
      const result = await api<SentCode>('POST', '/api/account/email/send-code')
      setCodeSent(true)
      setMessage(codeSentMessage(result, 'emailed'))
    })

  return (
    <Card title="Email">
      <p>
        {account.email} <Badge verified={account.emailVerified} />
      </p>
      {!account.emailVerified && (
        <>
          <button type="button" onClick={sendCode}>
            Email me a code to verify it
          </button>
          {codeSent && (
            <CodeForm
              label="Email code"
              onVerify={(code) =>
                run(() => api<Account>('POST', '/api/account/email/verify', { code }), 'Your email is verified.')
              }
            />
          )}
        </>
      )}
    </Card>
  )
}

function PhoneCard({ account, run, setMessage }: VerifyProps) {
  const [phone, setPhone] = useState(account.phoneNumber ?? '')
  const [codeSent, setCodeSent] = useState(false)

  const save = (event: FormEvent) => {
    event.preventDefault()
    void run(async () => {
      const result = await api<SentCode>('PUT', '/api/account/phone', { phone })
      const updated = await api<Account>('GET', '/api/account')
      setPhone(updated.phoneNumber ?? '')
      if (result.sentTo) {
        setCodeSent(true)
        setMessage(codeSentMessage(result, 'texted'))
      } else {
        setMessage({ kind: 'info', text: 'Phone number saved.' })
      }
      return updated
    })
  }

  const sendCode = () =>
    run(async () => {
      const result = await api<SentCode>('POST', '/api/account/phone/send-code')
      setCodeSent(true)
      setMessage(codeSentMessage(result, 'texted'))
    })

  return (
    <Card title={<>Phone <span className="optional">(optional)</span></>}>
      <form className="inline" onSubmit={save}>
        <input
          type="tel"
          autoComplete="tel"
          placeholder="(214) 555-0123"
          aria-label="Phone number"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
        <button type="submit">Save</button>
      </form>
      {account.phoneNumber && (
        <p>
          <Badge verified={account.phoneVerified} />
        </p>
      )}
      {account.phoneNumber && !account.phoneVerified && (
        <>
          <button type="button" onClick={sendCode}>
            Text me a code to verify it
          </button>
          {codeSent && (
            <CodeForm
              label="Text message code"
              onVerify={(code) =>
                run(() => api<Account>('POST', '/api/account/phone/verify', { code }), 'Your phone number is verified.')
              }
            />
          )}
        </>
      )}
      <p className="hint">To remove your number, clear the box and save.</p>
    </Card>
  )
}

// ----- Two-step verification -----

function TwoFactorCard({ account, run }: CardProps) {
  const [method, setMethod] = useState<TwoFactorMethod>(account.twoFactorMethod)

  if (!account.hasPassword) {
    return (
      <Card title="Two-step verification">
        <p className="hint">
          You sign in with Google or Apple, which protect your account with their own security, including their own
          two-step verification.
        </p>
      </Card>
    )
  }

  const choices: { value: TwoFactorMethod; label: string; needs: string; allowed: boolean }[] = [
    { value: 'NONE', label: 'Off', needs: '', allowed: true },
    { value: 'EMAIL', label: 'Email me a code', needs: '(verify your email first)', allowed: account.emailVerified },
    { value: 'SMS', label: 'Text me a code', needs: '(add and verify a phone first)', allowed: account.phoneVerified },
  ]

  return (
    <Card title="Two-step verification">
      <p className="hint">
        After your password, we'll also ask for a 6-digit code, so a stolen password alone isn't enough to get into your
        account.
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          void run(() => api<Account>('PUT', '/api/account/two-factor', { method }), 'Two-step verification updated.')
        }}
      >
        {choices.map((choice) => (
          <label key={choice.value} className="choice">
            <input
              type="radio"
              name="method"
              disabled={!choice.allowed}
              checked={method === choice.value}
              onChange={() => setMethod(choice.value)}
            />{' '}
            {choice.label} {!choice.allowed && <span className="hint">{choice.needs}</span>}
          </label>
        ))}
        <button type="submit">Save</button>
      </form>
    </Card>
  )
}
