// Thin fetch wrapper: adds the JWT, parses JSON, and turns API errors into Error(message).
// Empty = same origin (Vite proxy in dev, nginx in Docker). On Render it is the API's URL or bare host.
const RAW_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
const BASE = RAW_BASE && !/^https?:\/\//.test(RAW_BASE) ? `https://${RAW_BASE}` : RAW_BASE
const TOKEN_KEY = 'es_token'

export const tokenStore = {
  get: () => { try { return localStorage.getItem(TOKEN_KEY) } catch { return null } },
  set: (t) => { try { localStorage.setItem(TOKEN_KEY, t) } catch { /* ignore */ } },
  clear: () => { try { localStorage.removeItem(TOKEN_KEY) } catch { /* ignore */ } },
}

export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors || {}
  }
}

let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

async function request(method, path, body, { raw = false } = {}) {
  const headers = { Accept: raw ? '*/*' : 'application/json' }
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let res
  try {
    res = await fetch(`${BASE}/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', 0)
  }

  if (res.status === 401 && token) onUnauthorized()
  if (!res.ok) {
    let data = null
    try { data = await res.json() } catch { /* not JSON */ }
    const message = data?.message
      || (res.status === 403 ? 'You do not have permission to do that'
        : res.status === 401 ? 'Please log in to continue' : `Request failed (${res.status})`)
    throw new ApiError(message, res.status, data?.fieldErrors)
  }
  if (raw) return res
  if (res.status === 204) return null
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b),
  put: (p, b) => request('PUT', p, b),
  patch: (p, b) => request('PATCH', p, b),
  del: (p) => request('DELETE', p),

  /** Downloads a file from the API (with auth) and saves it via the browser. */
  async download(path, fallbackName) {
    const res = await request('GET', path, undefined, { raw: true })
    const blob = await res.blob()
    const cd = res.headers.get('Content-Disposition') || ''
    const name = /filename="?([^"]+)"?/.exec(cd)?.[1] || fallbackName
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },
}
