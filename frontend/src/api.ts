/*
  api.ts: one helper for talking to our backend's JSON endpoints.

      const day = await api<LiturgicalDay>('GET', '/api/day/2026/12/25')

  - <LiturgicalDay> tells TypeScript what kind of data comes back, so the
    rest of the code knows day.title is text, day.readings is a list, etc.
  - Sends the body as JSON, or as-is for FormData (file uploads).
  - For anything except GET, first fetches this session's CSRF token from
    /api/auth/csrf and sends it in a header. Spring Security refuses changes
    without it (see SecurityConfig.java for why).
  - If the server says no, throws an ApiError carrying the server's friendly
    message, the status (e.g. 401 = not signed in) and any password problems.
*/

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

export class ApiError extends Error {
  readonly status: number
  readonly problems: string[]

  constructor(message: string, status: number, problems: string[]) {
    super(message)
    this.status = status
    this.problems = problems
  }
}

export async function api<T>(method: Method, url: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const isUpload = body instanceof FormData

  // For FormData the browser sets the Content-Type itself (with the
  // "boundary" text that separates the parts of the upload).
  if (body !== undefined && !isUpload) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    const csrf = (await (await fetch('/api/auth/csrf')).json()) as { headerName: string; token: string }
    headers[csrf.headerName] = csrf.token
  }

  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : isUpload ? (body as FormData) : JSON.stringify(body),
  })

  // Some replies (like signing out) have no body, so read plain text first.
  const text = await response.text()
  let data: unknown = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    // not JSON (e.g. an HTML error page): keep data as null
  }

  if (!response.ok) {
    const problem = (data ?? {}) as { detail?: string; problems?: string[] }
    throw new ApiError(
      problem.detail ?? `Something went wrong (status ${response.status}).`,
      response.status,
      problem.problems ?? [],
    )
  }
  return data as T
}

/** The message to show for any error, ApiError or otherwise. */
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong.'
}
