import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft, BarChart3, CalendarClock, CheckCheck, ClipboardCheck, Clock, Info, LayoutList, MapPin, Pencil, Rocket,
  Trash2, User, Users, XCircle,
} from 'lucide-react'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { categoryMeta } from '../lib/constants'
import { fmtDateTime, fmtRange, relative } from '../lib/format'
import AgendaTab from '../components/event/AgendaTab'
import RegistrationsTab from '../components/event/RegistrationsTab'
import AttendanceTab from '../components/event/AttendanceTab'
import AnalyticsTab from '../components/event/AnalyticsTab'
import ParticipantPanel from '../components/event/ParticipantPanel'
import LifecycleStepper from '../components/event/LifecycleStepper'
import { Badge, Button, Card, ConfirmDialog, PageLoader, Progress, Tabs } from '../components/ui'

/** What the organizer should do next - turns the state machine into guidance. */
function nextStep(event, sessionCount) {
  if (event.status === 'DRAFT') {
    return sessionCount === 0
      ? 'Add at least one session to the agenda, then publish to open registration.'
      : 'Your agenda is ready. Publish the event to open registration.'
  }
  if (event.status === 'PUBLISHED') {
    if (event.phase === 'UPCOMING') return `Registration is open until ${fmtDateTime(event.registrationDeadline)}. Share the event!`
    if (event.phase === 'LIVE') return 'The event is live. Scan QR tickets at each session to record attendance.'
    return 'The event has ended. Mark it as completed to lock attendance and open feedback.'
  }
  if (event.status === 'COMPLETED') return 'Feedback is open to attendees. Check the analytics tab for the report and AI summary.'
  return null
}

function Hero({ event }) {
  const cat = categoryMeta(event.category)
  const Icon = cat.icon
  const fill = event.capacity ? (event.confirmedCount / event.capacity) * 100 : 0
  return (
    <div className={`relative mb-6 overflow-hidden rounded-3xl bg-gradient-to-br ${cat.gradient} p-6 text-white shadow-2xl shadow-brand-500/20 sm:p-8`}>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_85%_15%,rgba(255,255,255,.3),transparent_40%)]" />
      <Icon className="absolute -bottom-10 -right-6 size-64 text-white/10" />
      <div className="relative">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full bg-white/20 px-3 py-1 text-xs font-semibold backdrop-blur">{cat.label}</span>
          <Badge status={event.phase} className="bg-white/90 dark:bg-slate-900/80" />
        </div>
        <h1 className="mt-4 max-w-3xl text-3xl font-extrabold tracking-tight sm:text-4xl">{event.title}</h1>
        <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-white/90">
          <span className="flex items-center gap-2"><CalendarClock className="size-4" />{fmtRange(event.startDateTime, event.endDateTime)}</span>
          <span className="flex items-center gap-2"><MapPin className="size-4" />{event.venue}</span>
          <span className="flex items-center gap-2"><User className="size-4" />By {event.organizerName}</span>
        </div>
        <div className="mt-6 max-w-md">
          <div className="mb-1.5 flex justify-between text-xs font-semibold">
            <span className="flex items-center gap-1.5"><Users className="size-3.5" />{event.confirmedCount} / {event.capacity} confirmed</span>
            <span>{event.waitlistCount > 0 ? `${event.waitlistCount} on waitlist` : `${event.seatsLeft} seats left`}</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-white/25">
            <div className="h-full rounded-full bg-white transition-all duration-700" style={{ width: `${Math.min(100, fill)}%` }} />
          </div>
        </div>
      </div>
    </div>
  )
}

function OverviewTab({ detail, onAction, busy }) {
  const { event, sessions } = detail
  const step = nextStep(event, sessions.length)
  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-6 lg:col-span-2">
        <Card className="p-6">
          <LifecycleStepper event={event} />
          {step && (
            <p className="mt-5 flex items-start gap-2 rounded-2xl bg-brand-500/5 px-4 py-3 text-sm text-slate-700 ring-1 ring-brand-500/15 dark:text-slate-200">
              <Info className="mt-0.5 size-4 shrink-0 text-brand-500" /> {step}
            </p>
          )}
        </Card>
        <Card className="p-6">
          <h2 className="mb-3 font-bold">About this event</h2>
          <p className="whitespace-pre-line text-sm leading-relaxed text-slate-600 dark:text-slate-300">
            {event.description || 'No description yet. Edit the event and let AI write one for you.'}
          </p>
        </Card>
      </div>
      <div className="space-y-4">
        <Card className="space-y-3 p-6">
          <h2 className="font-bold">Actions</h2>
          {event.status === 'DRAFT' && (
            <Button icon={Rocket} className="w-full" loading={busy === 'publish'} onClick={() => onAction('publish')}>Publish event</Button>
          )}
          {event.status === 'PUBLISHED' && event.phase !== 'UPCOMING' && (
            <Button icon={CheckCheck} variant="success" className="w-full" loading={busy === 'complete'} onClick={() => onAction('complete')}>
              Mark as completed
            </Button>
          )}
          {!['COMPLETED', 'CANCELLED'].includes(event.status) && (
            <Link to={`/events/${event.id}/edit`} className="block"><Button variant="secondary" icon={Pencil} className="w-full">Edit details</Button></Link>
          )}
          {['DRAFT', 'PUBLISHED'].includes(event.status) && (
            <Button variant="danger" icon={XCircle} className="w-full" onClick={() => onAction('cancel', true)}>Cancel event</Button>
          )}
          {event.status === 'DRAFT' && (
            <Button variant="ghost" icon={Trash2} className="w-full text-rose-500" onClick={() => onAction('delete', true)}>Delete draft</Button>
          )}
          {['COMPLETED', 'CANCELLED'].includes(event.status) && (
            <p className="text-sm text-slate-500 dark:text-slate-400">This event is closed and kept read-only.</p>
          )}
        </Card>
        <Card className="space-y-3 p-6 text-sm">
          <h2 className="font-bold">Key dates</h2>
          <p className="flex justify-between gap-3"><span className="text-slate-500">Registration closes</span><span className="text-right font-semibold">{fmtDateTime(event.registrationDeadline)}</span></p>
          <p className="flex justify-between gap-3"><span className="text-slate-500">Starts</span><span className="text-right font-semibold">{fmtDateTime(event.startDateTime)}</span></p>
          <p className="flex justify-between gap-3"><span className="text-slate-500">Ends</span><span className="text-right font-semibold">{fmtDateTime(event.endDateTime)}</span></p>
          <div className="pt-2">
            <div className="mb-1 flex justify-between text-xs"><span className="text-slate-500">Seats filled</span><span className="font-semibold">{event.confirmedCount}/{event.capacity}</span></div>
            <Progress value={(event.confirmedCount / event.capacity) * 100} />
          </div>
        </Card>
      </div>
    </div>
  )
}

const CONFIRM_TEXT = {
  cancel: { title: 'Cancel this event?', text: 'Participants will see it as cancelled and registrations will be frozen. This cannot be undone.', label: 'Cancel event' },
  delete: { title: 'Delete this draft?', text: 'The draft and its agenda will be permanently deleted.', label: 'Delete' },
}

export default function EventDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const [detail, setDetail] = useState(null)
  const [busy, setBusy] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const tab = params.get('tab') || 'overview'

  const load = useCallback(() => {
    api.get(`/events/${id}`).then(setDetail).catch((e) => { toast.error(e.message); navigate('/events') })
  }, [id])

  useEffect(load, [load])

  const runAction = async (action) => {
    setBusy(action)
    try {
      if (action === 'delete') {
        await api.del(`/events/${id}`)
        toast.success('Draft deleted')
        navigate('/events')
        return
      }
      await api.post(`/events/${id}/${action}`)
      toast.success({ publish: 'Published! Registration is now open.', complete: 'Event completed. Feedback is now open to attendees.', cancel: 'Event cancelled' }[action])
      setConfirm(null)
      load()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusy(null)
    }
  }

  const onAction = (action, needsConfirm) => (needsConfirm ? setConfirm(action) : runAction(action))

  if (!detail) return <PageLoader />
  const { event, sessions, canManage } = detail
  const editable = canManage && !['COMPLETED', 'CANCELLED'].includes(event.status)

  return (
    <>
      <Link to="/events" className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 hover:text-brand-600">
        <ArrowLeft className="size-4" /> All events
      </Link>
      <Hero event={event} />

      {canManage ? (
        <>
          <Tabs value={tab} onChange={(t) => setParams(t === 'overview' ? {} : { tab: t }, { replace: true })} tabs={[
            { value: 'overview', label: 'Overview', icon: Info },
            { value: 'agenda', label: 'Agenda', icon: LayoutList, count: sessions.length },
            ...(event.status !== 'DRAFT' ? [
              { value: 'registrations', label: 'Registrations', icon: Users, count: event.confirmedCount + event.waitlistCount },
              { value: 'attendance', label: 'Attendance', icon: ClipboardCheck },
              { value: 'analytics', label: 'Analytics', icon: BarChart3 },
            ] : []),
          ]} />
          {tab === 'overview' && <OverviewTab detail={detail} onAction={onAction} busy={busy} />}
          {tab === 'agenda' && <AgendaTab event={event} sessions={sessions} editable={editable} manager onChanged={load} />}
          {tab === 'registrations' && <RegistrationsTab event={event} />}
          {tab === 'attendance' && <AttendanceTab event={event} sessions={sessions} />}
          {tab === 'analytics' && <AnalyticsTab event={event} />}
        </>
      ) : (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <Card className="p-6">
              <h2 className="mb-3 font-bold">About this event</h2>
              <p className="whitespace-pre-line text-sm leading-relaxed text-slate-600 dark:text-slate-300">{event.description}</p>
              {event.phase === 'UPCOMING' && (
                <p className="mt-4 flex items-center gap-1.5 text-xs font-semibold text-brand-600 dark:text-brand-300">
                  <Clock className="size-3.5" /> Starts {relative(event.startDateTime)}
                </p>
              )}
            </Card>
            <div>
              <h2 className="mb-4 text-lg font-bold">Agenda</h2>
              <AgendaTab event={event} sessions={sessions} editable={false} onChanged={load} />
            </div>
          </div>
          <div><ParticipantPanel detail={detail} onChanged={load} /></div>
        </div>
      )}

      <ConfirmDialog open={Boolean(confirm)} danger loading={busy === confirm}
        title={CONFIRM_TEXT[confirm]?.title} text={CONFIRM_TEXT[confirm]?.text} confirmLabel={CONFIRM_TEXT[confirm]?.label}
        onConfirm={() => runAction(confirm)} onClose={() => setConfirm(null)} />
    </>
  )
}
