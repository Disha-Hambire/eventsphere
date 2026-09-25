import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'motion/react'
import { CalendarClock, Compass, History, MapPin, MessageSquareHeart, QrCode, Ticket } from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { fmtRange } from '../lib/format'
import QrTicket from '../components/QrTicket'
import Timeline from '../components/Timeline'
import { Badge, Button, Card, EmptyState, Modal, PageHeader, PageLoader, Tabs } from '../components/ui'

function isUpcoming(r) {
  return r.status !== 'CANCELLED' && r.eventStatus === 'PUBLISHED' && new Date(r.eventEnd) > new Date()
}

export default function MyTickets() {
  const { user } = useAuth()
  const toast = useToast()
  const [rows, setRows] = useState(null)
  const [tab, setTab] = useState('upcoming')
  const [ticket, setTicket] = useState(null)
  const [history, setHistory] = useState(null)

  useEffect(() => {
    api.get('/registrations/my').then(setRows).catch((e) => toast.error(e.message))
  }, [])

  if (!rows) return <PageLoader />

  const upcoming = rows.filter(isUpcoming).sort((a, b) => new Date(a.eventStart) - new Date(b.eventStart))
  const past = rows.filter((r) => !isUpcoming(r))
  const list = tab === 'upcoming' ? upcoming : past
  const feedbackDue = rows.filter((r) => r.canGiveFeedback)

  return (
    <>
      <PageHeader eyebrow="Wallet" title="My tickets" subtitle="Your seats, waitlist positions and event history."
        actions={<Link to="/events"><Button variant="secondary" icon={Compass}>Discover events</Button></Link>} />

      {feedbackDue.length > 0 && (
        <Card className="mb-6 flex flex-col gap-3 border-fuchsia-500/20 p-5 sm:flex-row sm:items-center">
          <MessageSquareHeart className="size-8 shrink-0 text-fuchsia-500" />
          <div className="flex-1">
            <p className="font-semibold">You attended {feedbackDue.length === 1 ? `“${feedbackDue[0].eventTitle}”` : `${feedbackDue.length} events`}. How was it?</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">Your feedback shapes the next edition.</p>
          </div>
          <Link to={`/events/${feedbackDue[0].eventId}`}><Button size="sm">Give feedback</Button></Link>
        </Card>
      )}

      <Tabs value={tab} onChange={setTab} tabs={[
        { value: 'upcoming', label: 'Upcoming', icon: Ticket, count: upcoming.length },
        { value: 'past', label: 'Past & cancelled', icon: History, count: past.length },
      ]} />

      {list.length === 0 ? (
        <EmptyState icon={Ticket} title={tab === 'upcoming' ? 'No upcoming tickets' : 'No history yet'}
          text="Register for an event and your QR ticket will appear here."
          action={<Link to="/events"><Button icon={Compass}>Browse events</Button></Link>} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {list.map((r, i) => (
            <motion.div key={r.id} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}>
              <Card hover className="flex h-full flex-col p-5">
                <div className="flex items-start justify-between gap-3">
                  <Link to={`/events/${r.eventId}`} className="font-bold leading-snug hover:text-brand-600 dark:hover:text-brand-300">{r.eventTitle}</Link>
                  <Badge status={r.eventStatus === 'CANCELLED' ? 'CANCELLED' : r.status}>
                    {r.status === 'WAITLISTED' ? `Waitlist #${r.waitlistPosition}` : r.eventStatus === 'CANCELLED' ? 'Event cancelled' : undefined}
                  </Badge>
                </div>
                <div className="mt-3 space-y-1.5 text-xs text-slate-500 dark:text-slate-400">
                  <p className="flex items-center gap-2"><CalendarClock className="size-3.5" />{fmtRange(r.eventStart, r.eventEnd)}</p>
                  <p className="flex items-center gap-2"><MapPin className="size-3.5" />{r.venue}</p>
                </div>
                {r.promotedFromWaitlist && r.status === 'CONFIRMED' && (
                  <p className="mt-3 rounded-lg bg-brand-500/10 px-2.5 py-1.5 text-xs font-semibold text-brand-600 dark:text-brand-300">
                    🎉 A seat opened up. You were promoted from the waitlist!
                  </p>
                )}
                <div className="mt-auto flex flex-wrap gap-2 pt-5">
                  {r.ticketCode && r.status === 'CONFIRMED' && (
                    <Button size="sm" icon={QrCode} onClick={() => setTicket(r)}>QR ticket</Button>
                  )}
                  <Button size="sm" variant="ghost" icon={History} onClick={() => setHistory(r)}>Timeline</Button>
                  {r.sessionsAttended > 0 && <span className="self-center text-xs text-slate-500">Attended {r.sessionsAttended} session{r.sessionsAttended > 1 ? 's' : ''}</span>}
                </div>
              </Card>
            </motion.div>
          ))}
        </div>
      )}

      <Modal open={Boolean(ticket)} onClose={() => setTicket(null)} title="Your ticket" size="sm"
        footer={<Button variant="secondary" onClick={() => window.print()}>Print</Button>}>
        {ticket && <QrTicket ticketCode={ticket.ticketCode} eventTitle={ticket.eventTitle} participantName={user.fullName}
          start={ticket.eventStart} end={ticket.eventEnd} venue={ticket.venue} />}
      </Modal>
      <Modal open={Boolean(history)} onClose={() => setHistory(null)} title="Registration timeline" subtitle={history?.eventTitle} size="sm">
        {history && <Timeline entries={history.timeline} />}
      </Modal>
    </>
  )
}
