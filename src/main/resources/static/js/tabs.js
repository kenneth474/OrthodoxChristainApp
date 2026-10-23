/*
  tabs.js: the tab bar at the bottom of every page, like Instagram's.

      Home  ·  Bible  ·  Prayers  ·  Profile

  Each page loads this file once; it builds the bar, highlights the tab for
  the page you're on, and adds it to the bottom of the page. Keeping it in one
  file means adding a tab later is a one-line change here, not an edit to
  every page. Home is the default tab: the app opens on it.

  The Profile tab shows your photo once you're signed in, and goes to your
  account page; when you're not signed in, it goes to the sign-in page.
*/
(() => {
    // Simple line icons, drawn with SVG. "currentColor" makes each icon the
    // same colour as its label, so the active tab's icon turns gold too.
    const icon = paths =>
        `<svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;

    const TABS = [
        {
            id: 'home',
            label: 'Home',
            href: '/',
            pages: ['/', '/index.html'],
            icon: icon('<path d="M3 11l9-7 9 7"/><path d="M5 10v10h5v-6h4v6h5V10"/>'),
        },
        {
            id: 'bible',
            label: 'Bible',
            href: '/bible.html',
            pages: ['/bible.html'],
            // an open book
            icon: icon('<path d="M12 6c-2-1.5-5-2-8-1.5v14c3-.5 6 0 8 1.5"/><path d="M12 6c2-1.5 5-2 8-1.5v14c-3-.5-6 0-8 1.5"/><path d="M12 6v14"/>'),
        },
        {
            id: 'prayers',
            label: 'Prayers',
            href: '/prayers.html',
            pages: ['/prayers.html'],
            // the Orthodox three-bar cross, with its slanted footrest
            icon: icon('<path d="M12 2v20"/><path d="M9.5 5h5"/><path d="M7 9h10"/><path d="M9 17l6-2.5"/>'),
        },
        {
            id: 'profile',
            label: 'Profile',
            href: '/account.html',
            pages: ['/account.html', '/login.html'],
            // a person
            icon: icon('<circle cx="12" cy="8" r="4"/><path d="M4 21c1-4 4.5-6 8-6s7 2 8 6"/>'),
        },
    ];

    const here = location.pathname;
    const nav = document.createElement('nav');
    nav.className = 'tab-bar';
    nav.setAttribute('aria-label', 'Main');
    nav.innerHTML = TABS.map(tab => {
        const active = tab.pages.includes(here);
        // aria-current tells screen readers which tab is the current page.
        return `<a class="tab-link${active ? ' active' : ''}" data-tab="${tab.id}" href="${tab.href}"
                   ${active ? 'aria-current="page"' : ''}>${tab.icon}<span>${tab.label}</span></a>`;
    }).join('');

    document.body.append(nav);
    document.body.classList.add('has-tab-bar'); // leaves room for the bar (css/theme.css)

    // Profile tab: your photo when signed in; the sign-in page when not.
    // (/api/account answers 401 when nobody is signed in.)
    fetch('/api/account')
        .then(response => response.ok ? response.json() : null)
        .then(account => {
            const profile = nav.querySelector('[data-tab=profile]');
            if (!account) {
                profile.href = '/login.html';
                return;
            }
            if (account.photoVersion) {
                const photo = document.createElement('img');
                photo.className = 'tab-avatar';
                photo.src = `/api/account/photo?v=${account.photoVersion}`;
                photo.alt = '';
                profile.querySelector('.tab-icon').replaceWith(photo);
            }
        })
        .catch(() => {}); // offline: keep the plain icon
})();
