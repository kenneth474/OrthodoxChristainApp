# Orthodox Daily: An Antiochian Orthodox Companion App

[![Build and test](https://github.com/kenneth474/OrthodoxChristianApp/actions/workflows/ci.yml/badge.svg)](https://github.com/kenneth474/OrthodoxChristianApp/actions/workflows/ci.yml)

A cross-platform app for Antiochian Orthodox Christians, inspired by the
features of [antiochian.org](https://www.antiochian.org): each day's **fasting
rule, scripture readings, saints and their lives**, a **parish finder** for
churches near you, **user accounts**, **church news**, **prayers** and more.
One codebase is planned to run on the web, Windows, macOS, Linux, Android and iOS.

> **Status:** early development (v0.3). The liturgical calendar works for any
> date, with full scripture text, saints' lives and a fasting rule engine.
> Accounts work too: email and password with strong password rules, an
> optional phone number, two-step verification with an authenticator app
> (or by text or email, once those are set up), and Google sign-in ready to
> switch on. See the [Roadmap](#roadmap) below.
>
> **Try it: [orthodox-daily.onrender.com](https://orthodox-daily.onrender.com)**
> (on Render's free plan, so the first visit after a quiet spell takes about
> a minute while it wakes up; see [Putting the App Online](#putting-the-app-online)).
>
> This is an independent project, not an official app of the Antiochian
> Orthodox Christian Archdiocese of North America.

---

## Features

| Feature | What it does | Status |
|---|---|---|
| **Daily liturgical calendar** | Feast or saint, week title, tone and commemorations for any date | ✅ Working |
| **Scripture readings** | The day's Epistle and Gospel with full verse text, matching antiochian.org | ✅ Working |
| **Lives of the saints** | The story of each saint or feast for the day | ✅ Working |
| **Listen** | Readings and saints' lives read aloud with the device's own voice (no online AI voices); pause, speed, voice | ✅ Working |
| **Fasting rule engine** | Maps each day to a clear `FastingRule` (strict, wine only, wine & oil, fish, meat fast, fast-free) | ✅ Working |
| **Find a parish near me** | Search churches by your location, city or ZIP, with distance and directions | 🔜 Next |
| **Accounts** | Sign up and sign in with email and password; optional phone number | ✅ Working |
| **Strong passwords** | 12+ characters with upper and lower case, a number and a symbol; live checklist; lockout after 5 wrong tries | ✅ Working |
| **Two-step verification** | After your password, a 6-digit code from an authenticator app (Google Authenticator, Microsoft Authenticator, the iPhone Passwords app...) with 10 one-time backup codes; or a code by text message or email | ✅ Authenticator app working online; texts need [Twilio Verify](#codes-by-text-message-twilio-verify), email needs a mail service |
| **Sign in with Google** | One-tap sign-in that creates or links your account (the button shows "not set up yet" until keys are added) | 🔧 Built: needs [Google keys](#sign-in-with-google) |
| **Profile** | Profile photo (cropped, resized, location data stripped), patron saint, name day (with a greeting on the day), home parish | ✅ Working |
| **Text size & dark mode** | Normal / Large / Larger text; match device, light or dark theme; saved to your account | ✅ Working |
| **Five Orthodox styles** | Illuminated manuscript (parchment and red rubrics), Byzantine mosaic (lapis and gold tesserae), Candlelit vigil (always dark, candle glow), Holy Mountain (forest green, stone and terracotta roof tiles) and Liturgical seasons (bright and modern, in the color of the season's vestments); light and dark versions; chosen when creating an account, changed in Profile | ✅ Working |
| **Typikon feast signs** | Each day's rank shown with the service books' signs (red cross in a circle for great feasts, and so on); a red drop capital opens each reading | ✅ Working |
| **Service texts (PDF)** | Each day links to the Archdiocese's Online Liturgical Guide (Orthros, Divine Liturgy, Vespers) | 🔗 Linked; in-app viewer waiting on permission |
| **Home parish & bookmarks** | Choose a home parish, bookmark saints and feasts | 🔜 Planned |
| **Church news** | Archdiocese and parish news articles, managed by admins | 🔜 Planned |
| **Parish events** | Parish admins post events (e.g. *"Vespers moved to 6:30 PM"*) | 🔜 Planned |
| **Tab bar** | Home · Bible · Prayers · Church · Profile at the bottom of every page, like Instagram's; Home is the default. Each tab stays exactly where you left it (page, scroll, open readings); pull down to refresh; tap the current tab to go back to its first page | ✅ Working |
| **Orthodox Bible** | The whole Bible: Septuagint Old Testament (with Tobit, Wisdom, Sirach, Maccabees... and Orthodox Psalm numbering) and King James New Testament; continue reading; listen | ✅ Working |
| **Prayer book** | Beginning, morning and evening prayers, the Creed, meals, the Theotokos, the Jesus Prayer, Communion, the departed, Lent and Pascha; listen | ✅ Working |
| **Church tab** | The Archdiocese: hierarchs, the eight dioceses (with their bishops and states), 20 ministries, and about the Archdiocese, with links to antiochian.org | ✅ Working (parishes come with the parish finder) |
| **Pray today for…** | A different diocese, hierarch or ministry to pray for each day, on Home and in the Church tab | ✅ Working |
| **Calendar caching** | Each day's calendar is kept in the server's memory until midnight (Central), so it loads instantly and spares the free Orthocal service; an older copy is used if Orthocal is down | ✅ Working |
| **Morning notifications** | A 6:00 AM summary of major feasts and parish events | 🔜 Planned |
| **Install on any device** | Installable web app (PWA), plus Android and iOS store apps | 🔜 Planned |

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| **Language (backend)** | Java 17+ | Strongly typed, widely used in industry |
| **Backend framework** | Spring Boot 4 (Spring Web MVC) | REST APIs, dependency injection, scheduling, security |
| **HTTP client** | Spring `RestClient` | Calls the external calendar API |
| **JSON** | Jackson | Converts JSON to and from Java objects |
| **Build tool** | Maven (with the Maven Wrapper, `mvnw`) | Downloads libraries, compiles, tests, packages the app |
| **Calendar data** | [Orthocal API](https://orthocal.info/api/) | Free Orthodox calendar API with a Greek/Antiochian tradition setting |
| **Database** | Spring Data JPA with H2 on your computer (stored in `data/`) and PostgreSQL online | User accounts now; parishes, events, news, bookmarks next |
| **Packaging** | Docker (a multi-stage `Dockerfile`) | One container with the React app, the backend and Java, runnable on any host |
| **Hosting** | [Render](https://render.com) (free plan to start), set up by `render.yaml` | Builds and deploys from GitHub after the checks pass; free HTTPS address |
| **Security** | Spring Security: sessions, CSRF protection, BCrypt password hashing | Sign-in, two-step verification, protected account pages |
| **Google sign-in** | OAuth 2.0 / OpenID Connect (Spring Security OAuth2 Client) | Sign in without a new password |
| **Authenticator codes** | TOTP (RFC 6238), written in `Totp.java`; QR codes drawn with the `qrcode` package | Free two-step verification that needs no email or text service |
| **Email & text messages** | Spring Mail (SMTP) and Twilio Verify | Verification codes; printed in the terminal while developing |
| **Maps & location** *(planned)* | Browser Geolocation API + OpenStreetMap | "Near me" parish search and maps |
| **Cache** | `CalendarCache`: in the server's memory | Each day's calendar data, kept until midnight Central time (one server doesn't need Redis) |
| **Fonts** | EB Garamond, Cormorant Garamond and Cinzel (SIL Open Font License), packed into the app with Fontsource | Book-like reading text and carved-inscription capitals, with no requests to Google |
| **Frontend** | React 19 + TypeScript, built with Vite; React Router for the tabs | One app for every screen; adds to a phone's home screen and opens full screen |
| **Frontend tests** | Vitest (with jsdom) | Tests the helpers: dates, HTML cleaning, reading aloud, prayer book |
| **Mobile apps** *(planned)* | Capacitor | Wraps the React app as native Android and iOS apps |
| **Dev tools** | VS Code, Spring Boot DevTools, Git/GitHub | Editing, auto-restart on save, version control |

---

## Architecture

```
 ┌──────────────────────────┐        ┌──────────────────────────────┐        ┌──────────────────┐
 │  Frontend                │  JSON  │  Backend (Spring Boot)       │  JSON  │  Orthocal API    │
 │  Web / PWA / Android /   │ ─────▶ │                              │ ─────▶ │  orthocal.info   │
 │  iOS (React + Capacitor) │ ◀───── │  • CalendarController        │ ◀───── │  Greek tradition │
 │                          │        │  • CalendarService           │        │  New calendar    │
 │  Today: the React app    │        │  • OrthocalClient            │        └──────────────────┘
 │  (frontend/), on any     │        │  • AuthController, accounts  │  ────▶  Google sign-in,
 │  browser or home screen  │        │  • Bible, parishes (planned) │         email (SMTP), texts (Twilio Verify)
 └──────────────────────────┘        └──────┬───────────────┬───────┘
                                            │               │
                                     ┌──────▼─────┐   ┌─────▼──────┐
                                     │ H2 locally,│   │ Calendar-  │
                                     │ PostgreSQL │   │ Cache: each│
                                     │ online:    │   │ day, kept  │
                                     │ users, ... │   │ in memory  │
                                     └────────────┘   └────────────┘
```

**The backend is built in layers, each with one job:**

```
CalendarController  ──▶  CalendarService  ──▶  OrthocalClient  ──▶  orthocal.info
 (web requests)          (business rules)      (outside API)
```

**How a request flows:**
1. The browser opens `http://localhost:8080`, which loads the React app
   (`index.html` plus its JavaScript, built from `frontend/`).
2. The Home screen (`frontend/src/pages/HomePage.tsx`) calls our backend, e.g. `/api/day/2026/12/25`.
3. `CalendarController` passes the date to `CalendarService`.
4. `CalendarService` looks in `CalendarCache` first; if the day isn't there
   (or is from before midnight), it asks `OrthocalClient`, which fetches Orthocal's JSON.
   Jackson turns it into an `OrthocalDay` record (a mirror of Orthocal's format).
5. `CalendarService` converts that into our own `LiturgicalDay` record and works
   out the `FastingRule`.
6. `LiturgicalDay` goes back as JSON, and React draws the cards.
7. If anything goes wrong, `ApiExceptionHandler` sends a clear JSON error.

### About the calendar data

- We use Orthocal's **Greek tradition** (`/api/greek/...`). The Antiochian
  Archdiocese follows Greek practice for fasting and commemorations.
- We use the **New calendar** (`/gregorian/`), so fixed feasts such as
  Christmas fall on the civil date.
- Orthocal lists every reading of the day (Vespers, Matins, weekday and
  saints' readings). `ReadingSelector` keeps the ones **antiochian.org** lists:
  the Liturgy's Epistle and Gospel, with a saint's or feast's readings replacing
  the weekday ones (on Sundays the Sunday Gospel stays). Checked against 47 days
  on antiochian.org. `ScriptureBooks` writes references their way, e.g.
  "St. Paul's Letter to the Galatians 1:11-19".
- `fast_level` names the **fasting season** (e.g. Nativity Fast). It is *not* a
  strictness scale. Strictness comes from `fast_exception` (a code from 0 to
  11), which `FastingRule.from(...)` turns into a clear rule.
- Orthocal reflects strict typikon practice. Parishes may relax rules
  pastorally, so the app should always point people to their priest.

### How signing in works

- **Sessions, not tokens.** Signing in stores your account ID in a server-side
  session; the browser keeps only a random session cookie. Every POST/PUT also
  needs a CSRF token, which the pages fetch from `/api/auth/csrf`.
- **Passwords** are checked against `PasswordPolicy` and stored only as BCrypt
  hashes. 5 wrong passwords in a row lock the account for 15 minutes, and a
  wrong email gets the same reply as a wrong password.
- **Two-step verification:** after a correct password, `AuthController` asks
  for a 6-digit code and only finishes signing in once it's right:
  - **Authenticator app** (`AuthenticatorService`, `Totp`): the app and the
    server share a secret (the QR code) and both work out the same code every
    30 seconds. Each code works once; 10 one-time backup codes (stored only as
    hashes) cover a lost phone; 10 wrong codes lock the account for 15 minutes.
  - **Text or email** (`VerificationCodeService`): a code we send, hashed,
    valid for 10 minutes with 5 tries. Texts go through Twilio Verify, which
    makes and checks its own codes. Online, these are only offered when they
    can really be sent (`CodeChannels`), so nobody can lock themselves out.
- **Google:** Spring Security handles the OAuth/OpenID Connect exchange;
  `SocialSignInHandler` then finds, links or creates the account. An account
  whose email was never verified loses its password when the verified owner
  signs in with Google, so nobody can claim someone else's email in advance.

---

## Project Structure

```
OrthodoxChristianApp/
├── pom.xml                          # Maven "recipe": Spring Boot version, libraries, Java version
├── mvnw, mvnw.cmd, .mvn/            # Maven Wrapper: run Maven without installing it
├── secrets.example.properties       # Template for secrets.properties (keys; never committed)
├── README.md                        # This file
├── .gitignore                       # Files Git should not track (build output, secrets, data/)
├── Dockerfile                       # Packs the whole app into a container image (how it runs online)
├── .dockerignore                    # Files the Dockerfile must not copy in (secrets, local builds)
├── render.yaml                      # Render "Blueprint": the online web app and its PostgreSQL database
├── frontend/                        # The React app (all the screens); see frontend/README.md
│   ├── src/pages/                   # Home, Bible, Prayers, Church, Profile, Sign-in
│   ├── src/components/              # TabsHost (keeps every tab alive), tab bar, pull to refresh, ornaments, style picker, player
│   ├── src/lib/                     # Helpers: dates, data loading, styles, Church seasons, daily intention, reading aloud
│   ├── src/styles/                  # theme.css (the five styles' colors and fonts) and app.css (the screens)
│   ├── public/data/prayers.json     # The prayers themselves (edit this to add or fix a prayer)
│   ├── public/data/church.json      # The Archdiocese: hierarchs, dioceses, ministries (edit when they change)
│   └── public/icons/                # The app icon (gold Orthodox cross)
├── .github/
│   ├── workflows/ci.yml             # GitHub Actions: test and build everything on every push, then try the container
│   ├── scripts/smoke-test.sh        # Quick checks of a running container (used by ci.yml)
│   ├── workflows/backlog.yml        # GitHub Actions: turns backlog.json into Issues
│   ├── backlog.json                 # The app's goals, written as user stories
│   └── ISSUE_TEMPLATE/              # Forms for new user stories and bug reports
├── src/test/java/com/orthodox/orthodoxapp/
│   ├── FastingRuleTest.java         # Tests for the fasting rules
│   └── auth/                        # Tests for passwords, phones, authenticator codes and the whole sign-in flow
└── src/main/
    ├── java/com/orthodox/orthodoxapp/
    │   ├── OrthodoxappApplication.java  # Starting point: main() boots Spring
    │   ├── DatabaseUrl.java             # Online: turns the host's DATABASE_URL into database settings
    │   ├── CalendarController.java      # Web endpoints: /api/today, /api/day/{y}/{m}/{d}
    │   ├── CalendarService.java         # Business logic: converts Orthocal data, applies rules
    │   ├── CalendarCache.java           # Keeps each day's calendar until midnight (Central)
    │   ├── OrthocalClient.java          # Calls the Orthocal API (with timeouts)
    │   ├── OrthocalDay.java             # Mirror of Orthocal's JSON (input)
    │   ├── LiturgicalDay.java           # Our clean format sent to the frontend (output)
    │   ├── FastingRule.java             # Enum: STRICT, WINE_ONLY, WINE_AND_OIL, ...
    │   ├── ReadingSelector.java         # Picks the day's readings the way the Archdiocese lists them
    │   ├── ScriptureBooks.java          # "Galatians 1.11-19" -> "St. Paul's Letter to the Galatians 1:11-19"
    │   ├── ApiExceptionHandler.java     # Turns errors into clear JSON replies
    │   ├── SpaController.java           # Sends the React app for /bible, /prayers, /profile... (bookmarks work)
    │   ├── account/                     # Who you are: AppUser (database table), your account settings
    │   │   ├── AppUser.java, AppUserRepository.java, TwoFactorMethod.java, TextSize.java, Theme.java, AppStyle.java
    │   │   ├── AccountController.java   # /api/account/...: profile, photo, appearance, phone, verification, two-step
    │   │   ├── AccountService.java, AccountView.java
    │   │   └── ProfilePhoto*.java       # Photo table, and checking/cropping/re-saving uploads
    │   ├── auth/                        # Signing in
    │   │   ├── SecurityConfig.java      # Who can see what, CSRF, sign-out, Google
    │   │   ├── AuthController.java      # /api/auth/...: register, sign in, codes
    │   │   ├── AuthService.java         # Password checks, lockout, Google accounts
    │   │   ├── AuthenticatorService.java, Totp.java, BackupCode.java  # Authenticator-app codes and backup codes
    │   │   ├── PasswordPolicy.java      # The strong-password rules
    │   │   ├── VerificationCodeService.java  # 6-digit codes: create, send, check
    │   │   ├── SessionSignIn.java       # Marks a session as signed in
    │   │   └── SocialClientRegistrations.java, SocialSignInHandler.java, ...
    │   └── notify/                      # Sending emails and texts (or printing them while developing)
    └── resources/
        ├── application.properties       # App settings (port, database, secrets file)
        ├── application-prod.properties  # Extra settings used only online (PostgreSQL, https)
        ├── bible/                       # The Bible text (public domain); see its README.md
        └── static/                      # The BUILT React app (made by "npm run build"; not in Git)
```

Every code file has comments explaining what each part does.

---

## Getting Started (Windows + VS Code)

### 1. Install the prerequisites

- **Java 17 or newer**. Check by running `java -version` in a terminal. If you
  need it, install **Temurin** from [adoptium.net](https://adoptium.net).
- **VS Code** with these extensions:
  - *Extension Pack for Java* (Microsoft)
  - *Spring Boot Extension Pack* (VMware/Broadcom)
- **Git** (to save your progress and push to GitHub).
- **Node.js LTS** (runs the React app's tools). Check with `node --version`;
  install from [nodejs.org](https://nodejs.org) if needed.

You don't need to install Maven. VS Code's Java extension reads `pom.xml` and
downloads the libraries itself, and the **Maven Wrapper** runs Maven from a
terminal. In VS Code's terminal (**Ctrl+`**):

```powershell
.\mvnw.cmd test
```

runs every automated test (the first run downloads Maven, about 10 MB).

### 2. Open the project

1. In VS Code: **File → Open Folder…** → choose `OrthodoxChristianApp`.
2. Wait 1–2 minutes the first time. The Java extension downloads Spring Boot
   (watch the status bar at the bottom). Trust the workspace if asked.

### 3. Build the React app (once, and after changing it)

In VS Code's terminal (**Ctrl+`**):

```powershell
cd frontend
npm install
npm run build
cd ..
```

`npm install` downloads the React app's packages (only needed the first time
and when they change). `npm run build` builds the app into
`src/main/resources/static`, where the backend serves it.

### 4. Run it

Use any of these:

- Press **F5** (if asked, choose **Java**).
- Open `OrthodoxappApplication.java` and click **Run** above `main`.
- Use the **Spring Boot Dashboard** in the left sidebar and click ▶ next to `orthodoxapp`.

When the terminal shows `Started OrthodoxappApplication`, open:

| URL | What you'll see |
|---|---|
| http://localhost:8080 | The app, on the Home tab (use ‹ Prev / Next › or the date picker) |
| http://localhost:8080/?date=2026-12-25 | Home for a specific date |
| http://localhost:8080/bible/PSA/50 | The Bible tab (here at Psalm 50) |
| http://localhost:8080/prayers/morning | The Prayers tab (here at Morning Prayers) |
| http://localhost:8080/profile | The Profile tab (sign in first) |
| http://localhost:8080/sign-in | Sign in, or create an account |
| http://localhost:8080/api/today | Today's raw JSON from our backend |
| http://localhost:8080/api/day/2026/12/25 | Raw JSON for any date |
| http://localhost:8080/api/bible/JOH/3 | Raw JSON for one Bible chapter |

To stop the app, click the red ■ square in the toolbar or press **Shift+F5**.

**Where are my codes?** Until email and texts are set up (below), every code
is printed in the VS Code terminal where the app runs (the **Debug Console**
tab if you started it with F5), in a box marked `EMAIL (development mode...)`
or `TEXT MESSAGE (development mode...)`. The pages say so too.

### 5. Setting up sign-in, email and text messages

Everything here is optional. Keys go in **`secrets.properties`**, which Git
ignores. Copy `secrets.example.properties` to `secrets.properties`, fill in a
section, and restart the app. The example file explains each setting.

| What | You need | Cost |
|---|---|---|
| **Google sign-in** | An OAuth client from Google Cloud Console (steps below) | Free |
| **Authenticator-app codes** | Nothing: they work as soon as someone turns them on in Profile | Free |
| **Codes by text** | A [Twilio](https://www.twilio.com) account with a Verify service (steps below) | About $0.05 per successful verification plus about a cent per text (US) |
| **Real emails** | An SMTP server, e.g. a Gmail [app password](https://myaccount.google.com/apppasswords). Online this also needs a paid Render plan (the free plan blocks email) | Free on your computer |

#### Sign in with Google

1. Go to [console.cloud.google.com](https://console.cloud.google.com) and sign
   in with your Google account (accept Google Cloud's terms the first time).
2. Create a project: the project menu at the top → **New project** → name it
   *Orthodox Daily* → **Create**, then make sure it's selected.
3. Open **Google Auth Platform** (search for it at the top) → **Get started**:
   app name *Orthodox Daily*, your email as the support email, audience
   **External**, your email as the contact → **Create**.
4. **Clients** → **Create client** → type **Web application**, name
   *Orthodox Daily web*. Under **Authorized redirect URIs** add both:
   - `http://localhost:8080/login/oauth2/code/google`
   - `https://orthodox-daily.onrender.com/login/oauth2/code/google`
5. **Create**. Copy the **Client ID** and **Client secret** it shows.
6. **Audience** → **Publish app**, so everyone (not only test users) can sign
   in. The app only asks for name and email, so Google doesn't need to review it.
7. Put the two values in `secrets.properties` (`app.auth.google.client-id`,
   `app.auth.google.client-secret`) for your computer, and in Render →
   orthodox-daily → **Environment** as `APP_AUTH_GOOGLE_CLIENTID` and
   `APP_AUTH_GOOGLE_CLIENTSECRET` for the live app. Restart / save.

The client secret is a password for the app: never put it in a file that goes
to GitHub.

#### Codes by text message (Twilio Verify)

Twilio Verify texts the codes from Twilio's own numbers, so there's no number
to buy and no US carrier registration (which ordinary app texts now need).

1. Sign up at [twilio.com](https://www.twilio.com/try-twilio) (a free trial).
2. In the [Twilio Console](https://console.twilio.com), copy the **Account
   SID** and **Auth Token** from the account's home page.
3. **Verify** → **Services** → **Create new**: friendly name *Orthodox Daily*
   (it appears in the text: "Your Orthodox Daily verification code is..."),
   channel **SMS** → **Create**. Copy its **Service SID** (starts with `VA`).
4. Put the three values in `secrets.properties` (`app.sms.twilio.account-sid`,
   `app.sms.twilio.auth-token`, `app.sms.twilio.verify-service-sid`) and in
   Render's Environment as `APP_SMS_TWILIO_ACCOUNTSID`,
   `APP_SMS_TWILIO_AUTHTOKEN` and `APP_SMS_TWILIO_VERIFYSERVICESID`.
5. A trial account only texts numbers you've verified in Twilio (yours), which
   is enough to test. To text everyone, upgrade the Twilio account (add a
   payment method there).

Then **Text me a code** appears in Profile → Two-step verification, after a
person verifies their phone number. Until then it shows "coming soon".

---

## Watching the App Get Built (Live Preview)

The app is set up so you can watch changes appear while it runs:

1. **Start the backend** (F5) and leave it running.
2. **Start the React dev server** in a second terminal:
   ```powershell
   cd frontend
   npm run dev
   ```
3. Open **http://localhost:5173** (not 8080) and keep the tab open.
4. When a file changes (whether you edit it or Claude does):
   - **React files (`frontend/src/...`):** the page updates by itself, instantly.
   - **Java files:** VS Code recompiles on save, and **Spring Boot DevTools**
     restarts the backend in about a second.
5. Use **VS Code's Source Control panel** (Ctrl+Shift+G) to see exactly what
   changed in each file. Green lines are added; red lines are removed.

### On your iPhone (or any phone)

Your phone must be on the **same Wi-Fi** as your computer.

1. Find your computer's address: run `ipconfig` and look for **IPv4 Address**
   under "Wireless LAN adapter Wi-Fi" (for example `192.168.4.49`).
2. In Safari on the phone, open one of:
   - `http://192.168.4.49:8080`: the built app (after `npm run build`), or
   - `http://192.168.4.49:5173`: the live dev server (updates as code changes).
     The first time, Windows asks whether Node.js may use the network: choose
     **Allow**.
3. To use it like an app: tap **Share** → **Add to Home Screen**. It gets the
   gold cross icon and opens full screen.

These addresses use `http`, not `https`. That's fine for trying things out;
features that need `https` (location for "parishes near me", Google sign-in on
the phone, working offline) work on the online address instead (see
[Putting the App Online](#putting-the-app-online)).

**Tip:** Commit after each working step (Source Control → type a message →
✓ Commit → Sync). Your GitHub history then becomes a timeline of the build.

---

## Putting the App Online

The app runs online at **https://orthodox-daily.onrender.com**, on
[Render](https://render.com), which builds it straight from this GitHub
repository. Three files make that work:

| File | What it does |
|---|---|
| `Dockerfile` | Packs the React app, the backend and Java into one container image |
| `render.yaml` | Tells Render what to create: the web app (from the `Dockerfile`) and a PostgreSQL database |
| `src/main/resources/application-prod.properties` | Settings used only online: PostgreSQL instead of H2, https, smaller memory limits |

Online, accounts live in **PostgreSQL** instead of H2, because files inside
the app's container are wiped on every restart and update. Render gives the
app the database's address as `DATABASE_URL`, and `DatabaseUrl.java` turns it
into the settings Spring Boot needs.

### The first time (about 15 minutes)

1. Go to [render.com](https://render.com) and **sign up with GitHub** (the
   `kenneth474` account). When GitHub asks which repositories Render may see,
   choose **OrthodoxChristianApp**.
2. In Render's dashboard: **New** → **Blueprint**, then pick
   **OrthodoxChristianApp**.
3. Render reads `render.yaml` and shows what it will create:
   **orthodox-daily** (web service, Free) and **orthodox-daily-db**
   (PostgreSQL, Free). Name the Blueprint (e.g. *Orthodox Daily*) and click
   **Deploy Blueprint**.
4. Wait for the first build, about 5 to 10 minutes. You can watch it under
   orthodox-daily → **Logs**.
5. When it says **Live**, open the address at the top of the page, for
   example `https://orthodox-daily.onrender.com` (Render adds a few letters if
   that name is taken).
6. On your iPhone: open that address in Safari → **Share** → **Add to Home
   Screen**.

Never put card details, passwords or keys in a file in this project. Anything
like that is typed only into Render's own pages.

### After that, updates are automatic

Every push to `main` runs the **Build and test** checks on GitHub. When they
pass, Render builds and starts the new version by itself
(`autoDeployTrigger: checksPass` in `render.yaml`). If a check fails, the
working version stays online.

The second check, **Container (as it runs online)**, builds the container the
same way Render does, starts it with a real PostgreSQL database on a server
as small as Render's free one, and tries it out
(`.github/scripts/smoke-test.sh`). Its log also says how long the app took to
start.

### What the free plan means

| Limit | What happens | When it matters |
|---|---|---|
| [Sleeps after 15 minutes](https://render.com/docs/free) with no visitors | The next visitor waits up to about a minute while it wakes | Upgrade the web service to **Starter** (about $7/month) for always-on |
| [The free database expires after 30 days](https://render.com/docs/free) | Render emails a warning; after a 14-day grace period the database, with every account in it, is deleted | Upgrade the database (about $6/month) **before** people rely on their accounts |
| [Email can't be sent](https://render.com/changelog/free-web-services-will-no-longer-allow-outbound-traffic-to-smtp-ports) | Free services can't reach email servers (SMTP), so verification codes are only written to Render's **Logs** | Upgrade to Starter, or send email through a service's web API (Issue #32) |

Prices change; check [render.com/pricing](https://render.com/pricing). To
upgrade: the service or database in Render → **Settings** → **Instance Type**.

### Secret settings online

`secrets.properties` stays on your computer: it never goes to GitHub or
Render. Online, the same settings are **environment variables**: in Render,
open **orthodox-daily** → **Environment** → **Add Environment Variable**, then
save (the app restarts with them). The name is the setting in capitals, with
`_` instead of `.` and no `-`:

| In `secrets.properties` | Render environment variable |
|---|---|
| `app.auth.google.client-id` | `APP_AUTH_GOOGLE_CLIENTID` |
| `app.auth.google.client-secret` | `APP_AUTH_GOOGLE_CLIENTSECRET` |
| `spring.mail.host`, `spring.mail.port` | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` |
| `spring.mail.username`, `spring.mail.password` | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` |
| `spring.mail.properties.mail.smtp.auth` | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` |
| `spring.mail.properties.mail.smtp.starttls.enable` | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` |
| `app.sms.twilio.account-sid` | `APP_SMS_TWILIO_ACCOUNTSID` |
| `app.sms.twilio.auth-token` | `APP_SMS_TWILIO_AUTHTOKEN` |
| `app.sms.twilio.verify-service-sid` | `APP_SMS_TWILIO_VERIFYSERVICESID` |

For Google sign-in online, also add
`https://orthodox-daily.onrender.com/login/oauth2/code/google` (your real
address) as an **Authorized redirect URI** in Google Cloud Console, next to the
`localhost` one.

### Moving to another host later

Nothing in the app is tied to Render except `render.yaml`. Any host that runs
containers (Railway, Fly.io, Google Cloud Run, your own server) can build the
same `Dockerfile`. It needs:

- a PostgreSQL database, with its address in a `DATABASE_URL` environment
  variable (`postgresql://USER:PASSWORD@HOST:5432/NAME`);
- the same secret environment variables as above.

To bring the accounts along, copy the database across with PostgreSQL's
`pg_dump` and `pg_restore` tools.

---

## Roadmap

### How we track work

- **User stories:** every goal is a GitHub [Issue](https://github.com/kenneth474/OrthodoxChristianApp/issues),
  written as *"As a …, I want …, so that …"* with a checklist of acceptance criteria.
  Each one has a phase [milestone](https://github.com/kenneth474/OrthodoxChristianApp/milestones)
  and labels such as `parishes` or `accounts`.
- **Board:** the [Projects](https://github.com/kenneth474/OrthodoxChristianApp/projects) tab shows
  every story in **Todo → In Progress → Done** columns.
- **Done means done:** put `Closes #12` (the Issue's number) in a commit message.
  When it's pushed, GitHub closes Issue #12 and the board moves it to **Done**.
- **Actions:** every push runs the build and tests ([ci.yml](.github/workflows/ci.yml)).
  New stories added to [backlog.json](.github/backlog.json) become Issues automatically
  ([backlog.yml](.github/workflows/backlog.yml)), or you can use **New issue → User story**.

**Phase 1: Liturgics** *(antiochian.org: Liturgics, Spiritual Readings)*
- [x] Spring Boot project and `/api/today` endpoint (Greek/Antiochian, New calendar)
- [x] Preview page showing fast, readings and saints
- [x] `FastingRule` enum and a controller → service → client layer structure
- [x] Any-date lookups (`/api/day/2026/12/25`), full scripture text, saints' lives
- [x] Error handling (Orthocal offline, slow network, impossible dates)
- [x] Automated tests and GitHub Actions build on every push

**Phase 2: Find a Parish** *(antiochian.org: Parishes)*
- [ ] Database with Spring Data JPA (H2 to start, PostgreSQL later)
- [ ] `Parish` entity: name, address, phone, website, diocese, latitude/longitude
- [ ] "Near me" search using the browser's location and the Haversine distance formula
- [ ] Search by city, state or ZIP; filter by diocese; map and directions links

**Phase 3: Accounts**
- [x] Spring Security: sign up and sign in with sessions, BCrypt password hashes, CSRF protection
- [x] Strong password rules with a live checklist; lockout after 5 wrong tries
- [x] Optional phone number, verified by text
- [x] Two-step verification with an authenticator app (QR code, backup codes), or by text or email
- [ ] Sign in with Google (built; needs keys). Sign in with Apple was dropped (it needs a $99/year Apple Developer account)
- [ ] Real email and text delivery (built; texts need a Twilio Verify service, email a mail service and a paid Render plan)
- [ ] Reset a forgotten password
- [x] Profile: photo, patron saint, name day greeting, home parish (free text for now)
- [x] Text size and dark mode
- [ ] Roles: member, parish admin, site admin
- [ ] Home parish linked to the parish finder; bookmarked saints and feasts

**Phase 4: Content** *(antiochian.org: Church News, Orthodox Prayers, Dioceses, Ministries, About)*
- [ ] News articles and parish events with an admin editor (CRUD)
- [x] Bottom tab bar: Home, Bible, Prayers, Profile
- [x] The Orthodox Bible (Septuagint + King James New Testament)
- [x] Prayer book (traditional prayers; the Archdiocese's own translations with its permission)
- [x] Church tab: hierarchs, diocese and ministry pages, and a daily prayer intention (parish lists come with the parish finder)
- [x] Link each day to the Archdiocese's Online Liturgical Guide (service texts)
- [ ] Read and download the day's service texts in the app (waiting on permission)

**Phase 5: Real app and every platform**
- [x] React frontend (Vite + TypeScript) replacing the preview pages; adds to a phone's home screen
- [x] Calendar caching (in memory, until midnight Central; an older copy if Orthocal is down)
- [x] Five Orthodox styles, each light and dark; tabs that remember where you were; pull to refresh
- [ ] `@Scheduled` 6:00 AM summaries and push notifications
- [ ] PWA install support, then Android/iOS apps with Capacitor
- [x] Put the app online: [orthodox-daily.onrender.com](https://orthodox-daily.onrender.com) on Render (`Dockerfile`, `render.yaml`, PostgreSQL), tested in GitHub Actions before every deploy
- [ ] Before people rely on it: upgrade Render's free database (deleted after 30 days) and send email online (Issue #32)

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Port 8080 was already in use` | Another copy is still running. Stop it (red ■), or change `server.port` in `application.properties`. |
| Where's my 6-digit code? | On your computer, until email/texts are set up, codes are printed in the VS Code terminal running the app. With an authenticator app, the code is in the app. |
| "Continue with Google" says "not set up yet" | Fill in both Google settings (`secrets.properties`, or Render's Environment online), then restart the app. |
| Google says "redirect_uri_mismatch" | The address in the error must be listed exactly under **Authorized redirect URIs** in Google Cloud Console (see [Sign in with Google](#sign-in-with-google)). |
| The authenticator code is refused | Use the newest code (it changes every 30 seconds), and check the phone's clock is set automatically. Lost the phone? Use a backup code. |
| "Text me a code" says "coming soon" | Texts aren't set up on this server yet: see [Codes by text message](#codes-by-text-message-twilio-verify). |
| I want an empty database | Stop the app and delete the `data/` folder. It's recreated on the next start. |
| Red squiggles everywhere after opening | The Java extension is still downloading libraries. Wait, or run **Ctrl+Shift+P → Java: Clean Java Language Server Workspace**. |
| Page says *"Could not load this day"* | Make sure the app is running and you're online. Check the VS Code terminal for an error. |
| http://localhost:8080 shows a blank page or "Not Found" | Build the React app: `cd frontend`, `npm install`, `npm run build`, then restart the backend. |
| Phone can't open the app | Same Wi-Fi as the computer? Right address (`ipconfig`)? For port 5173, allow Node.js through Windows Firewall when asked. |
| Startup error about `/api/today` being mapped twice | An old copy of `TodayController.java` still has code in it. It should contain only comments; you can delete it. |
| `java -version` shows 11 or older | Install Java 17+ from adoptium.net, then restart VS Code. |
| The online app takes a minute to open | On Render's free plan it sleeps after 15 minutes without visitors and wakes on the next visit. Starter plan = always on. |
| Codes don't arrive on the online app | Render's free plan blocks email. Find the code in Render → orthodox-daily → **Logs**. |
| A Blueprint sync failed with "name already in use" | Click **Manual sync** on the Blueprint once more. (The very first sync did this; the second one created the web service.) |
| Render shows "Deploy failed" | Open the deploy's log in Render. If the app started but stopped with `'url' must start with "jdbc"`, the `DATABASE_URL` environment variable is missing. |
| My push didn't update the online app | Render waits for the GitHub checks. A red ✗ in the **Actions** tab means a check failed, so the old version stays online. |

---

## Credits

- Liturgical calendar data: [orthocal.info](https://orthocal.info), by Brian Glass
  ([source code](https://github.com/brianglass/orthocal-python)).
- Bible: Sir Lancelot Brenton's English Septuagint (1851; updated spelling by
  Adam Boyd, CC0) and the King James Version New Testament, both public domain,
  from [eBible.org](https://ebible.org). See `src/main/resources/bible/README.md`.
- Prayers: traditional Orthodox prayers in their common English wording. The
  Archdiocese's own prayer book translations can be added with its permission.
- Fasting guidance shown in the app follows strict typikon practice. Always
  follow the direction of your parish priest.
- Feature ideas follow [antiochian.org](https://www.antiochian.org). Content
  and data from the Archdiocese are used only with its permission.
