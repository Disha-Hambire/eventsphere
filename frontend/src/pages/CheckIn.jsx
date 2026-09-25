import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AnimatePresence, motion } from 'motion/react'
import { Html5Qrcode } from 'html5-qrcode'
import { AlertTriangle, Camera, CameraOff, CheckCircle2, Keyboard, RotateCcw, ScanLine, UserCheck } from 'lucide-react'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { fmtTime } from '../lib/format'
import { defaultSession } from '../components/event/AttendanceTab'
import { Button, Card, EmptyState, PageHeader, Spinner } from '../components/ui'

const RESULT_STYLE = {
  CHECKED_IN: { cls: 'from-emerald-500 to-teal-500', icon: CheckCircle2, title: 'Checked in' },
  ALREADY_CHECKED_IN: { cls: 'from-amber-500 to-orange-500', icon: RotateCcw, title: 'Already checked in' },
  ERROR: { cls: 'from-rose-500 to-pink-500', icon: AlertTriangle, title: 'Not allowed' },
}

function beep(ok) {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.frequency.value = ok ? 880 : 220
    gain.gain.value = 0.08
    osc.connect(gain).connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + (ok ? 0.12 : 0.3))
  } catch { /* audio not available */ }
}

export default function CheckIn() {
  const toast = useToast()
  const [params] = useSearchParams()
  const [events, setEvents] = useState(null)
  const [eventId, setEventId] = useState(params.get('event') || '')
  const [sessions, setSessions] = useState([])
  const [sessionId, setSessionId] = useState(params.get('session') || '')
  const [scanning, setScanning] = useState(false)
  const [manual, setManual] = useState('')
  const [result, setResult] = useState(null)
  const [log, setLog] = useState([])
  const scannerRef = useRef(null)
  const busyRef = useRef(false)
  const lastRef = useRef({ code: null, at: 0 })
  const sessionRef = useRef(sessionId)
  sessionRef.current = sessionId

  // Only published events can take attendance
  useEffect(() => {
    api.get('/events?status=PUBLISHED').then((list) => {
      setEvents(list)
      if (!params.get('event') && list.length) {
        const live = list.find((e) => e.phase === 'LIVE') || list[0]
        setEventId(String(live.id))
      }
    }).catch((e) => toast.error(e.message))
  }, [])

  useEffect(() => {
    if (!eventId) return
    api.get(`/events/${eventId}`).then((d) => {
      setSessions(d.sessions)
      setSessionId((cur) => (d.sessions.some((s) => String(s.id) === String(cur)) ? cur : String(defaultSession(d.sessions) || '')))
    }).catch((e) => toast.error(e.message))
  }, [eventId])

  const submitCode = useCallback(async (raw) => {
    const code = raw.trim().toUpperCase()
    if (!code || busyRef.current || !sessionRef.current) return
    // ignore the same QR held in front of the camera
    if (lastRef.current.code === code && Date.now() - lastRef.current.at < 3000) return
    lastRef.current = { code, at: Date.now() }
    busyRef.current = true
    try {
      const res = await api.post('/check-in', { ticketCode: code, sessionId: Number(sessionRef.current) })
      setResult({ kind: res.outcome, ...res })
      beep(res.outcome === 'CHECKED_IN')
      setLog((l) => [{ id: Date.now(), kind: res.outcome, name: res.participantName, code, at: res.checkedInAt }, ...l].slice(0, 12))
    } catch (e) {
      setResult({ kind: 'ERROR', message: e.message, ticketCode: code })
      beep(false)
      setLog((l) => [{ id: Date.now(), kind: 'ERROR', name: e.message, code, at: new Date().toISOString() }, ...l].slice(0, 12))
    } finally {
      busyRef.current = false
    }
  }, [])

  const stopCamera = useCallback(async () => {
    const s = scannerRef.current
    scannerRef.current = null
    if (s) {
      try { await s.stop() } catch { /* already stopped */ }
      try { s.clear() } catch { /* ignore */ }
    }
    setScanning(false)
  }, [])

  const startCamera = async () => {
    try {
      const scanner = new Html5Qrcode('qr-reader', { verbose: false })
      scannerRef.current = scanner
      setScanning(true)
      await scanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: { width: 230, height: 230 } },
        (text) => submitCode(text), () => {})
    } catch (e) {
      setScanning(false)
      scannerRef.current = null
      toast.error(typeof e === 'string' ? e : 'Camera unavailable. Allow camera access or type the ticket code instead.')
    }
  }

  useEffect(() => () => { stopCamera() }, [stopCamera])

  const onManual = (e) => {
    e.preventDefault()
    lastRef.current = { code: null, at: 0 }
    submitCode(manual)
    setManual('')
  }

  if (!events) return <div className="grid place-items-center py-24"><Spinner /></div>
  if (!events.length) {
    return (
      <>
        <PageHeader eyebrow="Door check-in" title="QR check-in" />
        <EmptyState icon={ScanLine} title="No published events" text="Attendance can be taken for published events only." />
      </>
    )
  }

  const session = sessions.find((s) => String(s.id) === String(sessionId))
  const style = result && RESULT_STYLE[result.kind]

  return (
    <>
      <PageHeader eyebrow="Door check-in" title="QR check-in"
        subtitle="Scan tickets at the session entrance. Duplicate scans and invalid tickets are detected instantly." />

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="space-y-4 lg:col-span-3">
          <Card className="grid gap-3 p-4 sm:grid-cols-2">
            <label>
              <span className="label">Event</span>
              <select className="input" value={eventId} onChange={(e) => setEventId(e.target.value)}>
                {events.map((e) => <option key={e.id} value={e.id}>{e.phase === 'LIVE' ? '🔴 ' : ''}{e.title}</option>)}
              </select>
            </label>
            <label>
              <span className="label">Session</span>
              <select className="input" value={sessionId} onChange={(e) => setSessionId(e.target.value)}>
                {sessions.map((s) => <option key={s.id} value={s.id}>{fmtTime(s.startTime)} · {s.title}</option>)}
              </select>
            </label>
          </Card>

          <Card className="overflow-hidden p-4">
            <div className="relative mx-auto aspect-square w-full max-w-md overflow-hidden rounded-2xl bg-slate-950">
              <div id="qr-reader" className="size-full [&>div]:!border-0" />
              {!scanning && (
                <div className="absolute inset-0 grid place-items-center bg-gradient-to-br from-slate-900 to-indigo-950 text-center text-white">
                  <div>
                    <ScanLine className="mx-auto size-14 text-brand-300" />
                    <p className="mt-3 font-semibold">Camera is off</p>
                    <p className="text-sm text-white/60">Start the scanner to check people in</p>
                  </div>
                </div>
              )}
              {scanning && (
                <div className="pointer-events-none absolute inset-8 rounded-2xl border-2 border-white/40">
                  <span className="absolute inset-x-4 h-0.5 animate-scan bg-gradient-to-r from-transparent via-brand-400 to-transparent shadow-[0_0_12px_2px_rgba(129,140,248,.8)]" />
                </div>
              )}
            </div>
            <div className="mt-4 flex flex-col gap-2 sm:flex-row">
              {scanning
                ? <Button variant="secondary" icon={CameraOff} className="flex-1" onClick={stopCamera}>Stop camera</Button>
                : <Button icon={Camera} className="flex-1" onClick={startCamera} disabled={!sessionId}>Start scanner</Button>}
              <form onSubmit={onManual} className="flex flex-1 gap-2">
                <div className="relative flex-1">
                  <Keyboard className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                  <input className="input pl-9 font-mono uppercase" placeholder="ES-XXXX-XXXX" value={manual} onChange={(e) => setManual(e.target.value)} />
                </div>
                <Button type="submit" variant="secondary" disabled={!manual.trim()}>Check in</Button>
              </form>
            </div>
          </Card>
        </div>

        <div className="space-y-4 lg:col-span-2">
          <AnimatePresence mode="wait">
            {result ? (
              <motion.div key={`${result.kind}-${result.ticketCode}-${log[0]?.id}`}
                initial={{ opacity: 0, scale: 0.94, y: 10 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.96 }}
                className={`rounded-3xl bg-gradient-to-br ${style.cls} p-6 text-white shadow-2xl`}>
                <style.icon className="size-10" />
                <p className="mt-3 text-sm font-semibold uppercase tracking-wide text-white/80">{style.title}</p>
                <p className="mt-1 text-2xl font-extrabold">{result.kind === 'ERROR' ? 'Entry refused' : result.participantName}</p>
                <p className="mt-2 text-sm text-white/90">{result.message}</p>
                <p className="mt-3 font-mono text-xs text-white/70">{result.ticketCode}</p>
              </motion.div>
            ) : (
              <Card className="p-6 text-center">
                <UserCheck className="mx-auto size-10 text-brand-500" />
                <p className="mt-3 font-semibold">Ready to scan</p>
                <p className="text-sm text-slate-500 dark:text-slate-400">{session ? `Checking in to “${session.title}”` : 'Choose a session'}</p>
              </Card>
            )}
          </AnimatePresence>

          <Card className="p-5">
            <h3 className="mb-3 text-sm font-bold">Recent scans</h3>
            {log.length === 0 ? <p className="text-sm text-slate-500">Nothing scanned yet.</p> : (
              <ul className="space-y-2">
                {log.map((l) => (
                  <li key={l.id} className="flex items-center gap-3 text-sm">
                    <span className={`size-2.5 shrink-0 rounded-full ${l.kind === 'CHECKED_IN' ? 'bg-emerald-500' : l.kind === 'ERROR' ? 'bg-rose-500' : 'bg-amber-500'}`} />
                    <span className="min-w-0 flex-1 truncate">{l.name}</span>
                    <span className="font-mono text-[11px] text-slate-400">{fmtTime(l.at)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>
    </>
  )
}
