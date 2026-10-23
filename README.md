# Orthodox Daily: An Antiochian Orthodox Companion App

[![Build and test](https://github.com/kenneth474/OrthodoxChristainApp/actions/workflows/ci.yml/badge.svg)](https://github.com/kenneth474/OrthodoxChristainApp/actions/workflows/ci.yml)

A cross-platform app for Antiochian Orthodox Christians, inspired by the
features of [antiochian.org](https://www.antiochian.org): each day's **fasting
rule, scripture readings, saints and their lives**, a **parish finder** for
churches near you, **user accounts**, **church news**, **prayers** and more.
One codebase is planned to run on the web, Windows, macOS, Linux, Android and iOS.

> **Status:** early development (v0.3). The liturgical calendar works for any
> date, with full scripture text, saints' lives and a fasting rule engine.
> Accounts work too: email and password with strong password rules, an
> optional phone number, two-step verification by email or text, and
> Google/Apple sign-in ready to switch on. See the [Roadmap](#roadmap) below.
>
> This is an independent project, not an official app of the Antiochian
> Orthodox Christian Archdiocese of North America.

---

## Features

| Feature | What it does | Status |
|---|---|---|
| **Daily liturgical calendar** | Feast or saint, week title, tone and commemorations for any date | ✅ Working |
| **Scripture readings** | Epistle and Gospel (and other services) with full verse text | ✅ Working |
| **Lives of the saints** | The story of each saint or feast for the day | ✅ Working |
| **Fasting rule engine** | Maps each day to a clear `FastingRule` (strict, wine only, wine & oil, fish, meat fast, fast-free) | ✅ Working |
| **Find a parish near me** | Search churches by your location, city or ZIP, with distance and directions | 🔜 Next |
| **Accounts** | Sign up and sign in with email and password; optional phone number | ✅ Working |
| **Strong passwords** | 12+ characters with upper and lower case, a number and a symbol; live checklist; lockout after 5 wrong tries | ✅ Working |
| **Two-step verification** | A 6-digit code by email or text message after your password | ✅ Working (codes print in the terminal until [email/SMS are set up](#4-setting-up-sign-in-email-and-text-messages)) |
| **Sign in with Google / Apple** | One-tap sign-in that creates or links your account (buttons show "not set up yet" until keys are added) | 🔧 Built: needs [keys](#4-setting-up-sign-in-email-and-text-messages) |
| **Profile** | Profile photo (cropped, resized, location data stripped), patron saint, name day (with a greeting on the day), home parish | ✅ Working |
| **Text size & dark mode** | Normal / Large / Larger text; match device, light or dark theme; saved to your account | ✅ Working |
| **Service texts (PDF)** | Each day links to the Archdiocese's Online Liturgical Guide (Orthros, Divine Liturgy, Vespers) | 🔗 Linked; in-app viewer waiting on permission |
| **Home parish & bookmarks** | Choose a home parish, bookmark saints and feasts | 🔜 Planned |
| **Church news** | Archdiocese and parish news articles, managed by admins | 🔜 Planned |
| **Parish events** | Parish admins post events (e.g. *"Vespers moved to 6:30 PM"*) | 🔜 Planned |
| **Orthodox prayers** | Morning, evening and occasional prayers | 🔜 Planned |
| **Dioceses, ministries & about** | Information pages about the Archdiocese's structure and ministries | 🔜 Planned |
| **Calendar caching** | Stores each day's data in Redis so the app is fast and gentle on the calendar API | 🔜 Planned |
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
| **Database** | H2 (stored in `data/`) + Spring Data JPA; PostgreSQL when live | User accounts now; parishes, events, news, bookmarks next |
| **Security** | Spring Security: sessions, CSRF protection, BCrypt password hashing | Sign-in, two-step verification, protected account pages |
| **Google / Apple sign-in** | OAuth 2.0 / OpenID Connect (Spring Security OAuth2 Client) | Sign in without a new password |
| **Email & text messages** | Spring Mail (SMTP) and Twilio | Verification codes; printed in the terminal while developing |
| **Maps & location** *(planned)* | Browser Geolocation API + OpenStreetMap | "Near me" parish search and maps |
| **Cache** *(planned)* | Redis + Spring Cache | Each day's calendar data, cached until midnight |
| **Frontend** *(planned)* | React (Vite) as a PWA | Dashboard UI that runs in any browser and installs to a home screen |
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
 │  Today: preview pages    │        │  • OrthocalClient            │        └──────────────────┘
 │  (static/index.html,     │        │  • AuthController, accounts  │  ────▶  Google / Apple sign-in,
 │   login.html, account)   │        │  • Parishes, news (planned)  │         email (SMTP), texts (Twilio)
 └──────────────────────────┘        └──────┬───────────────┬───────┘
                                            │               │
                                     ┌──────▼─────┐   ┌─────▼──────┐
                                     │ H2 now,    │   │   Redis    │
                                     │ PostgreSQL │   │ daily      │
                                     │ when live: │   │ calendar   │
                                     │ users, ... │   │ cache      │
                                     └────────────┘   └────────────┘
                                                          (planned)
```

**The backend is built in layers, each with one job:**

```
CalendarController  ──▶  CalendarService  ──▶  OrthocalClient  ──▶  orthocal.info
 (web requests)          (business rules)      (outside API)
```

**How a request flows:**
1. The browser opens `http://localhost:8080`, which loads the preview page.
2. The page calls our backend, e.g. `/api/day/2026/12/25`.
3. `CalendarController` passes the date to `CalendarService`.
4. `CalendarService` asks `OrthocalClient`, which fetches Orthocal's JSON.
   Jackson turns it into an `OrthocalDay` record (a mirror of Orthocal's format).
5. `CalendarService` converts that into our own `LiturgicalDay` record and works
   out the `FastingRule`.
6. `LiturgicalDay` goes back to the page as JSON, and the page draws the cards.
7. If anything goes wrong, `ApiExceptionHandler` sends a clear JSON error.

### About the calendar data

- We use Orthocal's **Greek tradition** (`/api/greek/...`). The Antiochian
  Archdiocese follows Greek practice for fasting and commemorations.
- We use the **New calendar** (`/gregorian/`), so fixed feasts such as
  Christmas fall on the civil date.
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
- **Two-step verification:** after a correct password, `AuthController` sends
  a 6-digit code (by email or text) and only finishes signing in once the code
  is right. Codes are hashed, expire after 10 minutes and allow 5 tries.
- **Google / Apple:** Spring Security handles the OAuth/OpenID Connect
  exchange; `SocialSignInHandler` then finds, links or creates the account.
  An account whose email was never verified loses its password when the
  verified owner signs in with Google/Apple, so nobody can claim someone
  else's email in advance.

---

## Project Structure

```
OrthodoxChristianApp/
├── pom.xml                          # Maven "recipe": Spring Boot version, libraries, Java version
├── mvnw, mvnw.cmd, .mvn/            # Maven Wrapper: run Maven without installing it
├── secrets.example.properties       # Template for secrets.properties (keys; never committed)
├── README.md                        # This file
├── .gitignore                       # Files Git should not track (build output, secrets, data/)
├── .github/
│   ├── workflows/ci.yml             # GitHub Actions: build and test on every push
│   ├── workflows/backlog.yml        # GitHub Actions: turns backlog.json into Issues
│   ├── backlog.json                 # The app's goals, written as user stories
│   └── ISSUE_TEMPLATE/              # Forms for new user stories and bug reports
├── src/test/java/com/orthodox/orthodoxapp/
│   ├── FastingRuleTest.java         # Tests for the fasting rules
│   └── auth/                        # Tests for passwords, phones, Apple keys and the whole sign-in flow
└── src/main/
    ├── java/com/orthodox/orthodoxapp/
    │   ├── OrthodoxappApplication.java  # Starting point: main() boots Spring
    │   ├── CalendarController.java      # Web endpoints: /api/today, /api/day/{y}/{m}/{d}
    │   ├── CalendarService.java         # Business logic: converts Orthocal data, applies rules
    │   ├── OrthocalClient.java          # Calls the Orthocal API (with timeouts)
    │   ├── OrthocalDay.java             # Mirror of Orthocal's JSON (input)
    │   ├── LiturgicalDay.java           # Our clean format sent to the frontend (output)
    │   ├── FastingRule.java             # Enum: STRICT, WINE_ONLY, WINE_AND_OIL, ...
    │   ├── ApiExceptionHandler.java     # Turns errors into clear JSON replies
    │   ├── account/                     # Who you are: AppUser (database table), your account settings
    │   │   ├── AppUser.java, AppUserRepository.java, TwoFactorMethod.java, TextSize.java, Theme.java
    │   │   ├── AccountController.java   # /api/account/...: profile, photo, appearance, phone, verification, two-step
    │   │   ├── AccountService.java, AccountView.java
    │   │   └── ProfilePhoto*.java       # Photo table, and checking/cropping/re-saving uploads
    │   ├── auth/                        # Signing in
    │   │   ├── SecurityConfig.java      # Who can see what, CSRF, sign-out, Google/Apple
    │   │   ├── AuthController.java      # /api/auth/...: register, sign in, codes
    │   │   ├── AuthService.java         # Password checks, lockout, Google/Apple accounts
    │   │   ├── PasswordPolicy.java      # The strong-password rules
    │   │   ├── VerificationCodeService.java  # 6-digit codes: create, send, check
    │   │   ├── SessionSignIn.java       # Marks a session as signed in
    │   │   └── SocialClientRegistrations.java, AppleClientSecret.java, SocialSignInHandler.java, ...
    │   └── notify/                      # Sending emails and texts (or printing them while developing)
    └── resources/
        ├── application.properties       # App settings (port, database, secrets file)
        └── static/
            ├── index.html               # Today's calendar (replaced by React later)
            ├── login.html               # Sign in / create account / enter code
            ├── account.html             # Your account and two-step verification
            ├── css/theme.css            # Light and dark colours, text sizes (every page)
            ├── js/appearance.js         # Applies your text size and theme before a page draws
            ├── css/forms.css, js/api.js # Shared look and fetch helper for the sign-in and account pages
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

### 3. Run it

Use any of these:

- Press **F5** (if asked, choose **Java**).
- Open `OrthodoxappApplication.java` and click **Run** above `main`.
- Use the **Spring Boot Dashboard** in the left sidebar and click ▶ next to `orthodoxapp`.

When the terminal shows `Started OrthodoxappApplication`, open:

| URL | What you'll see |
|---|---|
| http://localhost:8080 | The preview page (use ‹ Prev / Next › or the date picker) |
| http://localhost:8080/?date=2026-12-25 | The preview page for a specific date |
| http://localhost:8080/api/today | Today's raw JSON from our backend |
| http://localhost:8080/api/day/2026/12/25 | Raw JSON for any date |
| http://localhost:8080/login.html | Sign in, or create an account |
| http://localhost:8080/account.html | Your account: verify email/phone, two-step verification |

To stop the app, click the red ■ square in the toolbar or press **Shift+F5**.

**Where are my codes?** Until email and texts are set up (below), every code
is printed in the VS Code terminal where the app runs (the **Debug Console**
tab if you started it with F5), in a box marked `EMAIL (development mode...)`
or `TEXT MESSAGE (development mode...)`. The pages say so too.

### 4. Setting up sign-in, email and text messages

Everything here is optional. Keys go in **`secrets.properties`**, which Git
ignores. Copy `secrets.example.properties` to `secrets.properties`, fill in a
section, and restart the app. The example file explains each setting.

| What | You need | Cost |
|---|---|---|
| **Google sign-in** | An OAuth client ID from [Google Cloud Console](https://console.cloud.google.com/apis/credentials), with redirect URI `http://localhost:8080/login/oauth2/code/google` | Free |
| **Real emails** | An SMTP server, e.g. a Gmail [app password](https://myaccount.google.com/apppasswords) | Free for testing |
| **Real texts** | A [Twilio](https://www.twilio.com) account and phone number | About 1¢ per text, plus US registration |
| **Apple sign-in** | An [Apple Developer](https://developer.apple.com/programs/) account and the app online at an `https://` address (Apple doesn't allow `localhost`) | $99/year |

---

## Watching the App Get Built (Live Preview)

The app is set up so you can watch changes appear while it runs:

1. **Start the app once** (F5) and leave it running.
2. Open **http://localhost:8080** in your browser and keep the tab open.
3. When a file changes (whether you edit it or Claude does):
   - **Java files:** VS Code recompiles on save, and **Spring Boot DevTools**
     restarts the app in about a second. Watch the terminal for a new
     `Started…` line, then refresh the browser.
   - **The preview page (`index.html`):** just refresh the browser.
4. Use **VS Code's Source Control panel** (Ctrl+Shift+G) to see exactly what
   changed in each file. Green lines are added; red lines are removed.

**Tip:** Commit after each working step (Source Control → type a message →
✓ Commit → Sync). Your GitHub history then becomes a timeline of the build.

---

## Roadmap

### How we track work

- **User stories:** every goal is a GitHub [Issue](https://github.com/kenneth474/OrthodoxChristainApp/issues),
  written as *"As a …, I want …, so that …"* with a checklist of acceptance criteria.
  Each one has a phase [milestone](https://github.com/kenneth474/OrthodoxChristainApp/milestones)
  and labels such as `parishes` or `accounts`.
- **Board:** the [Projects](https://github.com/kenneth474/OrthodoxChristainApp/projects) tab shows
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
- [x] Two-step verification by email or text message
- [ ] Sign in with Google (built; needs keys) and Apple (built; needs Apple Developer account and HTTPS)
- [ ] Real email and text delivery (built; needs SMTP and Twilio settings)
- [ ] Reset a forgotten password
- [x] Profile: photo, patron saint, name day greeting, home parish (free text for now)
- [x] Text size and dark mode
- [ ] Roles: member, parish admin, site admin
- [ ] Home parish linked to the parish finder; bookmarked saints and feasts

**Phase 4: Content** *(antiochian.org: Church News, Orthodox Prayers, Dioceses, Ministries, About)*
- [ ] News articles and parish events with an admin editor (CRUD)
- [ ] Prayer library, diocese and ministry pages
- [x] Link each day to the Archdiocese's Online Liturgical Guide (service texts)
- [ ] Read and download the day's service texts in the app (waiting on permission)

**Phase 5: Real app and every platform**
- [ ] React frontend (Vite) replacing the preview page
- [ ] Redis caching, `@Scheduled` 6:00 AM summaries and push notifications
- [ ] PWA install support, then Android/iOS apps with Capacitor
- [ ] Deploy (backend, database and frontend hosted online)

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Port 8080 was already in use` | Another copy is still running. Stop it (red ■), or change `server.port` in `application.properties`. |
| Where's my 6-digit code? | Until email/texts are set up, codes are printed in the VS Code terminal running the app. |
| No "Continue with Google" button | Fill in both Google settings in `secrets.properties`, then restart the app. |
| I want an empty database | Stop the app and delete the `data/` folder. It's recreated on the next start. |
| Red squiggles everywhere after opening | The Java extension is still downloading libraries. Wait, or run **Ctrl+Shift+P → Java: Clean Java Language Server Workspace**. |
| Page says *"Could not load this day"* | Make sure the app is running and you're online. Check the VS Code terminal for an error. |
| Startup error about `/api/today` being mapped twice | An old copy of `TodayController.java` still has code in it. It should contain only comments; you can delete it. |
| `java -version` shows 11 or older | Install Java 17+ from adoptium.net, then restart VS Code. |

---

## Credits

- Liturgical calendar data: [orthocal.info](https://orthocal.info), by Brian Glass
  ([source code](https://github.com/brianglass/orthocal-python)).
- Fasting guidance shown in the app follows strict typikon practice. Always
  follow the direction of your parish priest.
- Feature ideas follow [antiochian.org](https://www.antiochian.org). Content
  and data from the Archdiocese are used only with its permission.
