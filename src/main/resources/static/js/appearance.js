/*
  appearance.js: applies your text size and theme (see css/theme.css).

  It runs in each page's <head>, BEFORE the page is drawn, so the page doesn't
  flash the wrong colours first. Your choice is saved to your account; a copy
  is kept in this browser (localStorage) so it applies instantly next time.
*/
const APPEARANCE_KEY = 'orthodoxDaily.appearance';

function applyAppearance(settings) {
    const root = document.documentElement; // the <html> tag
    root.dataset.theme = (settings?.theme || 'SYSTEM').toLowerCase();       // data-theme="..."
    root.dataset.textSize = (settings?.textSize || 'NORMAL').toLowerCase(); // data-text-size="..."
}

// Called with the account from /api/account once a page has loaded it.
function rememberAppearance(account) {
    const settings = { theme: account.theme, textSize: account.textSize };
    applyAppearance(settings);
    try {
        localStorage.setItem(APPEARANCE_KEY, JSON.stringify(settings));
    } catch {
        // storage can be switched off (e.g. private browsing); the page still works
    }
}

// On sign-out, so the next person using this browser gets the defaults.
function forgetAppearance() {
    try {
        localStorage.removeItem(APPEARANCE_KEY);
    } catch {
        // nothing to forget
    }
    applyAppearance(null);
}

// Apply the saved copy right away.
try {
    applyAppearance(JSON.parse(localStorage.getItem(APPEARANCE_KEY)));
} catch {
    applyAppearance(null);
}
