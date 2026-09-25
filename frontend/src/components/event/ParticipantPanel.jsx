import { useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'motion/react'
import { CalendarCheck, Clock, Hourglass, MessageSquareHeart, Ticket, XCircle } from 'lucide-react'
import { api } from '../../lib/api'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { fmtDateTime } from '../../lib/format'
import QrTicket from '../QrTicket'
import StarRating from '../StarRating'
import { Badge, Button, Card, ConfirmDialog, Field, Modal } from '../ui'

function FeedbackModal({ open, onClose, event, onDone }) {
  const toast = useToast()
  const [form, setForm] = useState({ rating: 0, contentRating: 0, organizationRating: 0, wouldRecommend: true, comments: '' })
  const [saving, setSaving] = useState(false)
  const complete = form.rating && form.contentRating && form.organizationRating

  const submit = async () => {
    setSaving(true)
    try {
      await api.post(`/events/${event.id}/feedback`, form)
      toast.success('Thanks! Your feedback helps organizers improve the next event.')
      onDone()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="How was it?" subtitle={event.title}
      footer={<>
        <Button variant="ghost" onClick={onClose}>Later</Button>
        <Button onClick={submit} loading={saving} disabled={!complete}>Submit feedback</Button>
      </>}>
      <div className="space-y-5">
        {[['rating', 'Overall experience'], ['contentRating', 'Content & speakers'], ['organizationRating', 'Organisation & venue']].map(([k, label]) => (
          <div key={k} className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <span className="text-sm font-semibold">{label}</span>
            <StarRating value={form[k]} onChange={(v) => setForm({ ...form, [k]: v })} />
          </div>
        ))}
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">Would you recommend it?</span>
          <div className="glass flex rounded-xl p-1 text-sm font-semibold">
            {[true, false].map((v) => (
              <button key={String(v)} type="button" onClick={() => setForm({ ...form, wouldRecommend: v })}
                className={`rounded-lg px-4 py-1.5 transition ${form.wouldRecommend === v ? 'bg-brand-gradient text-white' : 'text-slate-500'}`}>
                {v ? 'Yes' : 'No'}
              </button>
            ))}
          </div>
        </div>
        <Field label="Comments" hint="What worked well? What should change next time?">
          <textarea className="input min-h-24" maxLength={2000} value={form.comments}
            onChange={(e) => setForm({ ...form, comments: e.target.value })} />
        </Field>
      </div>
    </Modal>
  )
}

/** Everything a participant can do on an event page: register, see their seat or queue position, cancel, give feedback. */
export default function ParticipantPanel({ detail, onChanged }) {
  const { user } = useAuth()
  const toast = useToast()
  const { event, myRegistration: reg } = detail
  const [busy, setBusy] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [showTicket, setShowTicket] = useState(false)
  const [feedbackOpen, setFeedbackOpen] = useState(false)

  const register = async () => {
    setBusy(true)
    try {
      const res = await api.post(`/events/${event.id}/registrations`)
      toast[res.registration.status === 'CONFIRMED' ? 'success' : 'info'](res.message)
      onChanged()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusy(false)
    }
  }

  const cancel = async () => {
    setBusy(true)
    try {
      const res = await api.post(`/registrations/${reg.id}/cancel`)
      toast.info(res.message)
      setConfirmCancel(false)
      onChanged()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusy(false)
    }
  }

  const active = reg && reg.status !== 'CANCELLED'

  return (
    <Card className="sticky top-20 overflow-hidden p-6">
      {reg?.status === 'CONFIRMED' && (
        <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}>
          <div className="mb-4 flex items-center gap-3">
            <div className="grid size-11 place-items-center rounded-xl bg-emerald-500/10 text-emerald-500"><CalendarCheck className="size-6" /></div>
            <div>
              <p className="font-bold">You're going!</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">Your seat is confirmed</p>
            </div>
          </div>
          <Button icon={Ticket} className="w-full" onClick={() => setShowTicket(true)}>Show QR ticket</Button>
        </motion.div>
      )}

      {reg?.status === 'WAITLISTED' && (
        <div>
          <div className="mb-3 flex items-center gap-3">
            <div className="grid size-11 place-items-center rounded-xl bg-amber-500/10 text-amber-500"><Hourglass className="size-6" /></div>
            <div>
              <p className="font-bold">You're #{reg.waitlistPosition} on the waitlist</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">We'll confirm you automatically if a seat frees up.</p>
            </div>
          </div>
        </div>
      )}

      {!active && (
        <div>
          <p className="text-3xl font-extrabold">{event.seatsLeft > 0 ? event.seatsLeft : 'Full'}</p>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {event.seatsLeft > 0 ? `of ${event.capacity} seats left` : `${event.waitlistCount} already on the waitlist`}
          </p>
          <div className="mt-2 flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
            <Clock className="size-3.5" /> Registration {event.registrationOpen ? 'closes' : 'closed'} {fmtDateTime(event.registrationDeadline)}
          </div>
          {detail.canRegister ? (
            <Button className="mt-5 w-full" size="lg" loading={busy} onClick={register}>
              {event.seatsLeft > 0 ? 'Register now, it\'s free' : 'Join the waitlist'}
            </Button>
          ) : (
            <p className="mt-5 rounded-xl bg-slate-900/5 px-3 py-2.5 text-center text-sm font-medium text-slate-500 dark:bg-white/5">
              {event.status === 'COMPLETED' ? 'This event has ended' : event.status === 'CANCELLED' ? 'This event was cancelled' : 'Registration is closed'}
            </p>
          )}
        </div>
      )}

      {reg?.canCancel && (
        <button onClick={() => setConfirmCancel(true)} className="mt-4 flex w-full items-center justify-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-rose-500">
          <XCircle className="size-3.5" /> Cancel my {reg.status === 'WAITLISTED' ? 'waitlist spot' : 'registration'}
        </button>
      )}

      {(detail.canGiveFeedback || detail.feedbackGiven) && (
        <div className="mt-5 border-t border-slate-900/5 pt-5 dark:border-white/5">
          {detail.feedbackGiven ? (
            <p className="flex items-center gap-2 text-sm font-semibold text-emerald-600 dark:text-emerald-400">
              <MessageSquareHeart className="size-4" /> Thanks for your feedback!
            </p>
          ) : (
            <Button variant="secondary" icon={MessageSquareHeart} className="w-full" onClick={() => setFeedbackOpen(true)}>
              Rate this event
            </Button>
          )}
        </div>
      )}
      {reg && event.status === 'COMPLETED' && !detail.canGiveFeedback && !detail.feedbackGiven && reg.status === 'CONFIRMED' && (
        <p className="mt-4 text-xs text-slate-500">Feedback is only open to participants who checked in to at least one session.</p>
      )}

      {reg && (
        <div className="mt-4 flex items-center justify-between text-xs text-slate-500">
          <Badge status={reg.status} />
          <Link to="/tickets" className="font-semibold hover:text-brand-600">All my tickets →</Link>
        </div>
      )}

      <Modal open={showTicket} onClose={() => setShowTicket(false)} title="Your ticket" size="sm"
        footer={<Button variant="secondary" onClick={() => window.print()}>Print</Button>}>
        {reg?.ticketCode && (
          <QrTicket ticketCode={reg.ticketCode} eventTitle={event.title} participantName={user.fullName}
            start={event.startDateTime} end={event.endDateTime} venue={event.venue} />
        )}
      </Modal>
      <ConfirmDialog open={confirmCancel} danger loading={busy} title="Cancel registration?"
        text={reg?.status === 'CONFIRMED'
          ? 'Your seat will be released and automatically given to the next person on the waitlist.'
          : 'You will lose your place in the waitlist queue.'}
        confirmLabel="Yes, cancel" onConfirm={cancel} onClose={() => setConfirmCancel(false)} />
      <FeedbackModal open={feedbackOpen} onClose={() => setFeedbackOpen(false)} event={event} onDone={onChanged} />
    </Card>
  )
}
