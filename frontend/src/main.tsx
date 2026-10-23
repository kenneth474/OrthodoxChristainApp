/*
  main.tsx: where the React app starts.

  It finds <div id="root"> in index.html and tells React to draw the App
  inside it. BrowserRouter lets the app change the address (/bible,
  /prayers...) and swap screens WITHOUT reloading the page.
  StrictMode is a development helper: it runs some checks twice to catch
  mistakes early (it does nothing in the finished build).
*/
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { App } from './App'
import './styles/theme.css'
import './styles/app.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
