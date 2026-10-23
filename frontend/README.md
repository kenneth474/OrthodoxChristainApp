# Orthodox Daily: React frontend

The app's screens, written in **React** with **TypeScript** and built with
**Vite**. The Spring Boot backend (the rest of this repository) provides the
data through its `/api/...` endpoints.

## Commands (run them in this `frontend` folder)

| Command | What it does |
|---|---|
| `npm install` | Downloads the packages listed in `package.json` (once, and after they change) |
| `npm run dev` | Runs the app at http://localhost:5173 and reloads it whenever you save a file. Needs the backend running too (F5 in VS Code). Also reachable from your phone on the same Wi-Fi. |
| `npm run build` | Checks the types, then builds the finished app into `../src/main/resources/static`, where Spring Boot serves it at http://localhost:8080 |
| `npm test` | Runs the tests (`*.test.ts`) |
| `npm run lint` | Checks the code for common mistakes |

## Where things are

```
frontend/
├── index.html              The one HTML page (home-screen icon, theme set before drawing)
├── vite.config.ts          Dev server, proxy to the backend, build output folder, tests
├── public/                 Copied as-is: icons, manifest.webmanifest, data/prayers.json
└── src/
    ├── main.tsx            Where React starts
    ├── App.tsx             The addresses (routes) and the shared layout
    ├── api.ts              Talking to the backend (JSON, CSRF token, errors)
    ├── types.ts            The shapes of the backend's data (mirrors the Java records)
    ├── account-context.ts  useAccount(): who's signed in, from any screen
    ├── AccountProvider.tsx Loads the signed-in account once for the whole app
    ├── components/         TabBar, Player (reading aloud), and small shared pieces
    ├── pages/              One file per screen: Home, Bible, Prayers, Profile, Sign-in
    ├── lib/                Plain helpers: dates, reading aloud, HTML cleaning, prayers
    └── styles/             theme.css (colours, dark mode, tab bar) and app.css
```

Every file starts with a comment explaining what it's for.
