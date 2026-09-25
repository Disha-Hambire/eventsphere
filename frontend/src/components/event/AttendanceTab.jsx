import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CheckCircle2, ClipboardCheck, ScanLine, Search } from 'lucide-react'
import { api } from '../../lib/api'
import { useToast } from '../../context/ToastContext'
import { fmtTime } from '../../lib/format'
import { Avatar, Button, Card, EmptyState, Progress, Spinner } from '../ui'

/** Pick the session running now (or the next one) as the default. */
export function defaultSession(sessions) {
  const now = new Date()
  const running = sessions.find((s) => new Date(s.startTime) <= now && new Date(s.endTime) >= now)
  const next = sessions.find((s) => new Date(s.startTime) > now)
  return (running || next || sessions[0])?.id
}

export default function AttendanceTab({ event, sessions }) {
  const toast = useToast()
  const [sessionId, setSessionId] = useState(() => defaultSession(sessions))
  const [rows, setRows] = useState(null)
  const [query, setQuery] = useState('')
  const [pending, setPending] = useState(null)
  const locked = event.status !== 'PUBLISHED'

  const load = useCallback(() => {
    if (!sessionId) return
    setRows(null)
    api.get(`/sessions/${sessionId}/attendance`).then(setRows).catch((e) => toast.error(e.message))
  }, [sessionId])

  useEffect(load, [load])

  const toggle = async (row) => {
    setPending(row.registrationId)
    try {
      if (row.present) {
        await api.del(`/sessions/${sessionId}/attendance/${row.registrationId}`)
      } else {
        const res = await api.put(`/sessions/${sessionId}/attendance/${row.registrationId}`)
        toast.success(res.message)
      }
      load()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setPending(null)
    }
  }

  if (!sessions.length) {
    return <EmptyState icon={ClipboardCheck} title="No sessions" text="Add sessions to the agenda to take attendance." />
  }

  const present = rows?.filter((r) => r.present).length ?? 0
  const q = query.trim().toLowerCase()
  const visible = (rows || []).filter((r) => !q || `${r.participantName} ${r.participantEmail} ${r.ticketCode}`.toLowerCase().includes(q))

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-3">
        <Card className="p-4">
          <p className="label">Session</p>
          <div className="space-y-1.5">
            {sessions.map((s) => (
              <button key={s.id} onClick={() => setSessionId(s.id)}
                className={`w-full rounded-xl px-3 py-2.5 text-left text-sm transition ${
                  s.id === sessionId ? 'bg-brand-gradient text-white shadow-lg shadow-brand-500/25' : 'hover:bg-slate-900/5 dark:hover:bg-white/5'}`}>
                <p className="truncate font-semibold">{s.title}</p>
                <p className={`text-xs ${s.id === sessionId ? 'text-white/80' : 'text-slate-500 dark:text-slate-400'}`}>
                  {fmtTime(s.startTime)} – {fmtTime(s.endTime)}{s.room ? ` · ${s.room}` : ''}
                </p>
              </button>
            ))}
          </div>
        </Card>
        {!locked && (
          <Link to={`/check-in?event=${event.id}&session=${sessionId}`}>
            <Button icon={ScanLine} className="w-full">Open QR scanner</Button>
          </Link>
        )}
      </div>

      <div className="lg:col-span-2">
        <Card className="mb-4 p-5">
          <div className="mb-2 flex items-end justify-between">
            <div>
              <p className="text-3xl font-bold">{present}<span className="text-lg text-slate-400">/{rows?.length ?? 0}</span></p>
              <p className="text-xs text-slate-500 dark:text-slate-400">confirmed participants checked in</p>
            </div>
            {locked && <span className="text-xs font-semibold text-slate-500">Attendance is locked ({event.status.toLowerCase()})</span>}
          </div>
          <Progress value={rows?.length ? (present / rows.length) * 100 : 0} tone="emerald" />
        </Card>

        <div className="relative mb-3">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <input className="input pl-9" placeholder="Search attendee or ticket code" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>

        {!rows ? <div className="grid place-items-center py-16"><Spinner /></div> : (
          <Card className="divide-y divide-slate-900/5 dark:divide-white/5">
            {visible.map((r) => (
              <div key={r.registrationId} className="flex items-center gap-3 px-5 py-3">
                <Avatar name={r.participantName} className="size-8 text-[10px]" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{r.participantName}</p>
                  <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                    <span className="font-mono">{r.ticketCode}</span>
                    {r.present && ` · in at ${fmtTime(r.checkedInAt)} (${r.method === 'QR' ? 'QR' : 'manual'})`}
                  </p>
                </div>
                <Button size="sm" variant={r.present ? 'success' : 'secondary'} icon={r.present ? CheckCircle2 : undefined}
                  disabled={locked} loading={pending === r.registrationId} onClick={() => toggle(r)}>
                  {r.present ? 'Present' : 'Mark present'}
                </Button>
              </div>
            ))}
            {visible.length === 0 && <p className="px-5 py-10 text-center text-sm text-slate-500">No confirmed participants.</p>}
          </Card>
        )}
      </div>
    </div>
  )
}
