/*
  vite.config.ts: settings for Vite, the tool that runs and builds the React app.

  TWO WAYS TO RUN THE APP
  1. While developing:  npm run dev
     Vite serves the React app at http://localhost:5173 and reloads the page
     the moment you save a file. Requests for /api/... are passed on ("proxied")
     to the Spring Boot backend on port 8080, so start that too (F5 in VS Code).
     To the browser everything comes from one address, so cookies and
     sign-in work exactly as in the real app.
  2. For real:  npm run build
     Vite turns the TypeScript/React code into plain, compressed HTML, CSS and
     JavaScript, and writes it into the Spring Boot project
     (src/main/resources/static). Spring Boot then serves the finished app at
     http://localhost:8080 by itself, with no Node.js needed.
*/
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Everything the React app asks the backend for.
const BACKEND = 'http://localhost:8080'
const proxied = ['/api', '/oauth2', '/login/oauth2']

export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    // "host: true" also listens on this computer's Wi-Fi address, so a phone
    // on the same Wi-Fi can open the dev server (see README: phone preview).
    host: true,
    proxy: Object.fromEntries(proxied.map((path) => [path, BACKEND])),
  },

  build: {
    // Build straight into Spring Boot's folder for web pages.
    outDir: '../src/main/resources/static',
    // That folder is outside this project, so say clearly that Vite may
    // empty it first (it only ever contains the last build).
    emptyOutDir: true,
  },

  // Settings for the tests (npm test). jsdom is a pretend browser, so code
  // that uses the page (like the HTML sanitizer) can be tested without one.
  test: {
    environment: 'jsdom',
  },
})
