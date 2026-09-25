const dateFmt = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const timeFmt = new Intl.DateTimeFormat('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true })
const dayFmt = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })

// The API sends local date-times without a zone ("2026-10-01T09:00:00"), which JS parses as local time.
export const toDate = (v) => (v ? new Date(v) : null)

export const fmtDate = (v) => (v ? dateFmt.format(toDate(v)) : '-')
export const fmtTime = (v) => (v ? timeFmt.format(toDate(v)) : '-')
export const fmtDateTime = (v) => (v ? `${dateFmt.format(toDate(v))}, ${timeFmt.format(toDate(v))}` : '-')
export const fmtDay = (v) => (v ? dayFmt.format(toDate(v)) : '-')

export function fmtRange(start, end) {
  if (!start) return '-'
  const s = toDate(start)
  const e = toDate(end)
  if (e && s.toDateString() === e.toDateString()) {
    return `${fmtDay(start)} · ${fmtTime(start)} – ${fmtTime(end)}`
  }
  return `${fmtDay(start)} ${fmtTime(start)} → ${fmtDay(end)} ${fmtTime(end)}`
}

export function relative(v) {
  if (!v) return ''
  const diff = toDate(v) - new Date()
  const abs = Math.abs(diff)
  const mins = Math.round(abs / 60000)
  const hours = Math.round(abs / 3600000)
  const days = Math.round(abs / 86400000)
  const text = mins < 60 ? `${mins} min` : hours < 48 ? `${hours} h` : `${days} days`
  return diff >= 0 ? `in ${text}` : `${text} ago`
}

/** Value for <input type="datetime-local"> from an API date-time. */
export const toInputValue = (v) => (v ? String(v).slice(0, 16) : '')

export const pct = (v) => `${Math.round(v ?? 0)}%`

export const initials = (name = '') =>
  name.split(' ').filter(Boolean).slice(0, 2).map((w) => w[0]).join('').toUpperCase()

export const titleCase = (s = '') => s.charAt(0) + s.slice(1).toLowerCase()
