/*
  ProfilePage (the Profile tab, "/profile"): your account.

  Profile (photo, name, patron saint, name day, home parish), how the app
  looks, your email and phone, two-step verification (an authenticator app,
  or codes by text or email where this server can send them), and signing out.
  Talks to AccountController (/api/account/...). Every change sends back the
  updated account, which update() shares with the whole app (so the photo in
  the tab bar changes at once, for example).
*/
import QRCode from 'qrcode'
import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { useAccount } from '../account-context'
import { api, ApiError, errorMessage } from '../api'
import { Avatar, Message, type MessageData } from '../components/common'
import { Masthead } from '../components/ornaments'
import { StylePicker } from '../components/StylePicker'
import { forgetAppearance } from '../lib/appearance'
import { useApiData } from '../lib/data'
import type {
  Account,
  AppStyle,
  AuthenticatorSetup,
  SentCode,
  SignInOptions,
  TextSize,
  Theme,
  TwoFactorMethod,
} from '../types'

export function ProfilePage() {
  const { account, update } = useAccount()
  const [searchParams] = useSearchParams()
  const [message, setMessage] = useState<MessageData | null>(null)
  // Just created your account (/profile?welcome=1)? Greet you, until another message replaces it.
  const shown: MessageData | null =
    message ??
    (searchParams.get('welcome')
      ? { kind: 'info', text: 'Welcome! To protect your account, turn on two-step verification below.' }
      : null)
  const navigate = useNavigate()
  // What this server can do: send codes by email? by text? (Until it's
  // known, nothing is offered that might not work.)
  const options = useApiData<SignInOptions>('/api/auth/options').data ?? { google: false, emailCodes: false, textCodes: false }

  if (!account) return null // ProfileTab shows the sign-in page instead

  /**
   * Runs an account change and shows the result. Anything that changes the
   * account sends the new one back, which we share with the whole app.
   * A 401 means the session ended: go to sign-in.
   */
  const run = async (action: () => Promise<Account | void>, success?: string) => {
    try {
      const changed = await action()
      if (changed) update(changed)
      if (!success) return // nothing to show (e.g. a new style): stay where you are
      setMessage({ kind: 'info', text: success })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        update(null) // signed out: the check above then sends you to /sign-in
        return
      }
      setMessage({ kind: 'error', text: errorMessage(error), problems: error instanceof ApiError ? error.problems : [] })
    }
    window.scrollTo({ top: 0, behavior: 'smooth' }) // the message is at the top
  }

  // Signing out goes Home; the Profile tab then shows the sign-in page
  // (see ProfileTab).
  const signOut = () =>
    run(async () => {
      await api('POST', '/api/auth/sign-out')
      forgetAppearance() // the next person on this device gets the defaults
      navigate('/', { replace: true })
      update(null)
    })

  return (
    <div className="screen">
      <Masthead eyebrow={<Avatar account={account} className="avatar masthead-avatar" />} title={account.displayName} subtitle={account.email} />
      <main className="page narrow">
      <Message message={shown} />

      {/* key=...: if a different account signs in, start these forms afresh. */}
      <ProfileCard key={account.email} account={account} run={run} />
      <AppearanceCard account={account} run={run} />
      <EmailCard account={account} run={run} setMessage={setMessage} options={options} />
      <PhoneCard key={`phone-${account.email}`} account={account} run={run} setMessage={setMessage} options={options} />
      <TwoFactorCard key={`2fa-${account.email}`} account={account} run={run} options={options} />

      <section className="card">
        <h2>Ways you sign in</h2>
        <ul className="plain">
          {account.hasPassword && <li>✓ Email and password</li>}
          {account.googleLinked && <li>✓ Google</li>}
        </ul>
      </section>

      <button type="button" onClick={signOut}>
        Sign out
      </button>
      </main>
    </div>
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
  const save = (textSize: TextSize, theme: Theme, style: AppStyle) =>
    run(() => api<Account>('PUT', '/api/account/appearance', { textSize, theme, style }))

  return (
    <Card title="Appearance">
      <fieldset className="choices">
        <legend>Style</legend>
        <StylePicker value={account.style} onChange={(style) => save(account.textSize, account.theme, style)} />
      </fieldset>
      <fieldset className="choices">
        <legend>Text size</legend>
        <div className="row">
          {(['NORMAL', 'LARGE', 'LARGER'] as TextSize[]).map((size) => (
            <label key={size}>
              <input
                type="radio"
                name="textSize"
                checked={account.textSize === size}
                onChange={() => save(size, account.theme, account.style)}
              />{' '}
              {size === 'NORMAL' ? 'Normal' : size === 'LARGE' ? 'Large' : 'Larger'}
            </label>
          ))}
        </div>
      </fieldset>
      <fieldset className="choices">
        <legend>Light or dark</legend>
        <div className="row">
          {(['SYSTEM', 'LIGHT', 'DARK'] as Theme[]).map((theme) => (
            <label key={theme}>
              <input
                type="radio"
                name="theme"
                checked={account.theme === theme}
                onChange={() => save(account.textSize, theme, account.style)}
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
  options: SignInOptions
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

function EmailCard({ account, run, setMessage, options }: VerifyProps) {
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
      {!account.emailVerified &&
        (options.emailCodes ? (
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
        ) : (
          <p className="hint">Coming soon: verifying your email, once this server can send email.</p>
        ))}
    </Card>
  )
}

function PhoneCard({ account, run, setMessage, options }: VerifyProps) {
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
      {account.phoneNumber &&
        !account.phoneVerified &&
        (options.textCodes ? (
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
        ) : (
          <p className="hint">Coming soon: verifying your number by text, so you can get sign-in codes by text.</p>
        ))}
      <p className="hint">With a verified number you can get your sign-in codes by text. To remove your number, clear the box and save.</p>
    </Card>
  )
}

// ----- Two-step verification -----

interface TwoFactorProps extends CardProps {
  options: SignInOptions
}

function TwoFactorCard({ account, run, options }: TwoFactorProps) {
  const [method, setMethod] = useState<TwoFactorMethod>(account.twoFactorMethod)
  // Shown once, right after they're made.
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null)

  if (!account.hasPassword) {
    return (
      <Card title="Two-step verification">
        <p className="hint">
          You sign in with Google, which protects your account with its own security, including its own two-step
          verification.
        </p>
      </Card>
    )
  }

  if (backupCodes) {
    return (
      <Card title="Two-step verification">
        <BackupCodes codes={backupCodes} onDone={() => setBackupCodes(null)} />
      </Card>
    )
  }

  // Each choice, and why it can't be picked yet (if it can't).
  const why = (available: boolean, ready: boolean, needs: string) =>
    !available ? '(coming soon)' : !ready ? needs : ''
  const choices: { value: TwoFactorMethod; label: string; note: string }[] = [
    { value: 'NONE', label: 'Off', note: '' },
    { value: 'AUTHENTICATOR', label: 'Authenticator app (recommended)', note: '' },
    {
      value: 'SMS',
      label: 'Text me a code',
      note: why(options.textCodes, account.phoneVerified, '(add and verify your phone first)'),
    },
    {
      value: 'EMAIL',
      label: 'Email me a code',
      note: why(options.emailCodes, account.emailVerified, '(verify your email first)'),
    },
  ]

  const settingUpApp = method === 'AUTHENTICATOR' && account.twoFactorMethod !== 'AUTHENTICATOR'

  return (
    <Card title="Two-step verification">
      <p className="hint">
        After your password, we'll also ask for a 6-digit code, so a stolen password alone isn't enough to get into your
        account.
      </p>
      {account.twoFactorMethod === 'AUTHENTICATOR' && (
        <p>
          <span className="badge ok">On</span> Codes come from your authenticator app.
        </p>
      )}
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
              disabled={choice.note !== ''}
              checked={method === choice.value}
              onChange={() => setMethod(choice.value)}
            />{' '}
            {choice.label} {choice.note && <span className="hint">{choice.note}</span>}
          </label>
        ))}
        {!settingUpApp && method !== account.twoFactorMethod && <button type="submit">Save</button>}
      </form>

      {settingUpApp && (
        <AuthenticatorSetupForm
          run={run}
          onDone={(codes) => setBackupCodes(codes)}
          onCancel={() => setMethod(account.twoFactorMethod)}
        />
      )}

      {account.twoFactorMethod === 'AUTHENTICATOR' && method === 'AUTHENTICATOR' && (
        <button
          type="button"
          className="link"
          onClick={() =>
            run(async () => {
              const result = await api<{ backupCodes: string[] }>('POST', '/api/account/authenticator/backup-codes')
              setBackupCodes(result.backupCodes)
            })
          }
        >
          Make new backup codes
        </button>
      )}
    </Card>
  )
}

/**
 * Setting up an authenticator app: scan the QR code (or type the key, or on
 * an iPhone tap "Add to Passwords"), then type the code the app shows. The
 * server only switches the app on once that code is right.
 */
function AuthenticatorSetupForm({
  run,
  onDone,
  onCancel,
}: {
  run: Run
  onDone: (backupCodes: string[]) => void
  onCancel: () => void
}) {
  const [setup, setSetup] = useState<AuthenticatorSetup | null>(null)
  const [qr, setQr] = useState<string | null>(null)
  const [code, setCode] = useState('')

  // Ask the server for a new secret once, when this form appears.
  useEffect(() => {
    let cancelled = false
    api<AuthenticatorSetup>('POST', '/api/account/authenticator/setup')
      .then((result) => {
        if (cancelled) return
        setSetup(result)
        // Draw the QR code as a picture (a "data:" address the <img> can show).
        return QRCode.toDataURL(result.uri, { margin: 1, width: 220 }).then((url) => !cancelled && setQr(url))
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  if (!setup) return <p className="status">Preparing…</p>

  // "ABCD EFGH ..." is easier to type than one long run of letters.
  const groupedSecret = setup.secret.match(/.{1,4}/g)?.join(' ') ?? setup.secret

  return (
    <div className="authenticator-setup">
      <h3>Set up your authenticator app</h3>
      <ol>
        <li>
          Open an authenticator app: Google Authenticator, Microsoft Authenticator, or on iPhone the Passwords app.
        </li>
        <li>Scan this code with it:</li>
      </ol>
      {qr && <img className="qr" src={qr} width={220} height={220} alt="QR code for your authenticator app" />}
      <p className="hint">
        On this phone? <a href={setup.uri}>Add it to your authenticator app</a>, or type this key:{' '}
        <code className="secret">{groupedSecret}</code>
      </p>
      <form
        className="inline"
        onSubmit={(e) => {
          e.preventDefault()
          void run(async () => {
            const result = await api<{ account: Account; backupCodes: string[] }>(
              'POST',
              '/api/account/authenticator/confirm',
              { code },
            )
            onDone(result.backupCodes)
            return result.account
          })
        }}
      >
        <input
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          placeholder="6-digit code from the app"
          aria-label="Code from your authenticator app"
          required
          value={code}
          onChange={(e) => setCode(e.target.value)}
        />
        <button type="submit">Turn on</button>
      </form>
      <button type="button" className="link" onClick={onCancel}>
        Cancel
      </button>
    </div>
  )
}

/** The 10 backup codes, shown once, with a button to copy them. */
function BackupCodes({ codes, onDone }: { codes: string[]; onDone: () => void }) {
  const [copied, setCopied] = useState(false)
  return (
    <div className="backup-codes">
      <p>
        <span className="badge ok">On</span> Your authenticator app is set up.
      </p>
      <p>
        <strong>Save these backup codes</strong> somewhere safe (a password manager, or on paper). If you lose your phone,
        each one lets you sign in once. They won't be shown again.
      </p>
      <ul>
        {codes.map((code) => (
          <li key={code}>
            <code>{code}</code>
          </li>
        ))}
      </ul>
      <button
        type="button"
        onClick={() =>
          navigator.clipboard
            ?.writeText(codes.join('\n'))
            .then(() => setCopied(true))
            .catch(() => {})
        }
      >
        {copied ? 'Copied' : 'Copy the codes'}
      </button>{' '}
      <button type="button" className="primary-inline" onClick={onDone}>
        I've saved them
      </button>
    </div>
  )
}
