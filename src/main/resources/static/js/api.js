/*
  api.js: a small helper the sign-in and account pages share for talking to
  our backend's JSON endpoints.

      const result = await api('POST', '/api/auth/sign-in', { email, password });

  - Sends the body as JSON.
  - For anything except GET, first asks /api/auth/csrf for this session's CSRF
    token and sends it in a header. Spring Security refuses POST/PUT requests
    without it (see SecurityConfig.java for why).
  - Returns the reply's JSON. If the server says no, it throws an Error whose
    message is the server's friendly "detail" text, ready to show the user.
*/
async function api(method, url, body) {
    const headers = {};
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    if (method !== 'GET') {
        const csrf = await (await fetch('/api/auth/csrf')).json();
        headers[csrf.headerName] = csrf.token;
    }

    const response = await fetch(url, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
    });

    // Some replies (like signing out) have no body, so read text first.
    const text = await response.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        // not JSON (e.g. an HTML error page): keep data as null
    }

    if (!response.ok) {
        const error = new Error(data?.detail || `Something went wrong (status ${response.status}).`);
        error.status = response.status;        // e.g. 401 = not signed in
        error.problems = data?.problems ?? []; // password rules that weren't met
        throw error;
    }
    return data;
}

// Makes text safe to put into HTML (same helper as on the home page).
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text ?? '';
    return div.innerHTML;
}

// Shows a message box: kind "error" (red) or "info" (green), with an
// optional bullet list underneath (e.g. password rules).
function showMessage(text, problems = [], kind = 'error') {
    const box = document.getElementById('message');
    box.className = 'message ' + kind;
    box.innerHTML = escapeHtml(text)
        + (problems.length ? '<ul>' + problems.map(p => `<li>${escapeHtml(p)}</li>`).join('') + '</ul>' : '');
    box.hidden = false;
    box.scrollIntoView({ block: 'nearest' });
}

function hideMessage() {
    document.getElementById('message').hidden = true;
}
