import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'motion/react'
import {
  ArrowRight, BarChart3, BrainCircuit, CalendarCheck2, CheckCircle2, ListOrdered, QrCode, ScanLine, ShieldCheck,
  Sparkles, Star, Ticket, Users,
} from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { homeFor } from '../lib/nav'
import { Backdrop, Brand } from '../components/Brand'
import ThemeToggle from '../components/ThemeToggle'
import EventCard from '../components/EventCard'
import { Button, Card, CountUp } from '../components/ui'

const FEATURES = [
  { icon: ListOrdered, title: 'Smart registration & waitlist', text: 'Seats are allocated in real time. When the event is full, people join a FIFO waitlist and are promoted automatically the moment a seat frees up.' },
  { icon: QrCode, title: 'QR tickets', text: 'Every confirmed participant gets a unique, unguessable QR ticket, available instantly in their wallet.' },
  { icon: ScanLine, title: 'QR attendance scanner', text: 'Scan tickets at the door with any phone or laptop camera. Session-wise attendance, duplicate-scan detection and manual fallback.' },
  { icon: CalendarCheck2, title: 'Conflict-free scheduling', text: 'Sessions are validated against the event window, speaker double-booking and room clashes, before they happen.' },
  { icon: BrainCircuit, title: 'AI copilots (Gemini)', text: 'Generate polished event descriptions in one click and turn hundreds of feedback comments into an actionable summary.' },
  { icon: BarChart3, title: 'Analytics that drive decisions', text: 'Fill rate, attendance, no-shows, session popularity and ratings, for every event and across your portfolio.' },
]

const STEPS = ['Plan', 'Publish', 'Register', 'Check-in', 'Close', 'Feedback', 'Analyse']

export default function Landing() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [events, setEvents] = useState([])

  useEffect(() => {
    api.get('/public/stats').then(setStats).catch(() => setStats({}))
    api.get('/public/events').then(setEvents).catch(() => setEvents([]))
  }, [])

  const cta = user ? { to: homeFor(user), label: 'Open dashboard' } : { to: '/register', label: 'Get started free' }

  return (
    <div className="relative min-h-screen overflow-hidden">
      <Backdrop animated />
      <header className="mx-auto flex max-w-7xl items-center justify-between px-4 py-5 sm:px-6 lg:px-8">
        <Brand />
        <div className="flex items-center gap-2">
          <ThemeToggle />
          {user ? (
            <Link to={homeFor(user)}><Button size="md">Dashboard</Button></Link>
          ) : (
            <>
              <Link to="/login" className="hidden sm:block"><Button variant="ghost">Log in</Button></Link>
              <Link to="/register"><Button>Sign up</Button></Link>
            </>
          )}
        </div>
      </header>

      {/* Hero */}
      <section className="mx-auto max-w-7xl px-4 pb-16 pt-10 text-center sm:px-6 sm:pt-16 lg:px-8">
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}
          className="glass mx-auto mb-6 inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-xs font-semibold text-brand-600 dark:text-brand-300">
          <Sparkles className="size-3.5" /> AI-powered event operations for colleges, companies and conferences
        </motion.div>
        <motion.h1 initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08, duration: 0.6 }}
          className="mx-auto max-w-4xl text-4xl font-extrabold tracking-tight sm:text-6xl lg:text-7xl">
          Run every event <span className="text-gradient">end to end</span>, without the spreadsheets.
        </motion.h1>
        <motion.p initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.16, duration: 0.6 }}
          className="mx-auto mt-6 max-w-2xl text-base text-slate-600 sm:text-lg dark:text-slate-300">
          EventSphere handles the whole lifecycle: agenda and speakers, smart registration with automatic waitlists,
          QR tickets and check-in, attendance-verified feedback, and AI-powered insights.
        </motion.p>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.24, duration: 0.6 }}
          className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Link to={cta.to}><Button size="lg" icon={ArrowRight} className="flex-row-reverse">{cta.label}</Button></Link>
          {!user && <Link to="/login"><Button size="lg" variant="secondary">Try the demo</Button></Link>}
        </motion.div>

        {/* Lifecycle strip */}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.4 }}
          className="mx-auto mt-12 flex max-w-3xl flex-wrap items-center justify-center gap-2 text-xs font-semibold text-slate-500 dark:text-slate-400">
          {STEPS.map((s, i) => (
            <span key={s} className="flex items-center gap-2">
              <span className="glass rounded-full px-3 py-1">{s}</span>
              {i < STEPS.length - 1 && <ArrowRight className="size-3 text-brand-400" />}
            </span>
          ))}
        </motion.div>

        {/* Animated statistics */}
        <div className="mx-auto mt-14 grid max-w-5xl grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          {[
            { icon: CalendarCheck2, label: 'Events hosted', value: stats?.events },
            { icon: Ticket, label: 'Registrations', value: stats?.registrations },
            { icon: CheckCircle2, label: 'QR check-ins', value: stats?.checkIns },
            { icon: Star, label: 'Avg. rating', value: stats?.averageRating, decimals: 1, suffix: '/5' },
          ].map((s, i) => (
            <motion.div key={s.label} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 + i * 0.08 }}>
              <Card className="p-5 text-left">
                <s.icon className="size-5 text-brand-500" />
                <p className="mt-3 text-3xl font-extrabold tracking-tight sm:text-4xl">
                  {stats ? <CountUp value={s.value ?? 0} decimals={s.decimals} suffix={s.suffix} /> : <span className="skeleton inline-block h-9 w-20" />}
                </p>
                <p className="mt-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{s.label}</p>
              </Card>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Featured events */}
      {events.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="mb-6 flex items-end justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-brand-500">Happening soon</p>
              <h2 className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">Featured events</h2>
            </div>
            <Link to={user ? '/events' : '/login'} className="text-sm font-semibold text-brand-600 hover:underline dark:text-brand-300">
              View all →
            </Link>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {events.slice(0, 3).map((e, i) => <EventCard key={e.id} event={e} index={i} to={user ? `/events/${e.id}` : '/login'} />)}
          </div>
        </section>
      )}

      {/* Features */}
      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="mx-auto mb-12 max-w-2xl text-center">
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-500">Built around the real process</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Every step has rules. EventSphere enforces them.</h2>
        </div>
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f, i) => (
            <motion.div key={f.title} initial={{ opacity: 0, y: 24 }} whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-60px' }} transition={{ delay: i * 0.06, duration: 0.45 }}>
              <Card hover className="h-full p-6">
                <div className="grid size-12 place-items-center rounded-2xl bg-brand-gradient text-white shadow-lg shadow-brand-500/30">
                  <f.icon className="size-6" />
                </div>
                <h3 className="mt-5 text-lg font-bold">{f.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-400">{f.text}</p>
              </Card>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Roles */}
      <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6 lg:px-8">
        <Card className="relative overflow-hidden p-8 sm:p-12">
          <div className="absolute inset-0 -z-10 bg-brand-gradient opacity-[0.07]" />
          <div className="grid gap-8 lg:grid-cols-3">
            {[
              { icon: ShieldCheck, role: 'Admin', text: 'Governs the platform, approves organizers and oversees every event.' },
              { icon: CalendarCheck2, role: 'Organizer', text: 'Plans agendas, publishes events, scans tickets and reads the analytics.' },
              { icon: Users, role: 'Participant', text: 'Discovers events, holds QR tickets, tracks waitlist position and shares feedback.' },
            ].map((r) => (
              <div key={r.role} className="flex gap-4">
                <r.icon className="size-7 shrink-0 text-brand-500" />
                <div>
                  <h3 className="font-bold">{r.role}</h3>
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{r.text}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-10 flex flex-col items-center justify-between gap-4 border-t border-slate-900/5 pt-8 sm:flex-row dark:border-white/10">
            <p className="text-lg font-semibold">Ready to run your next event?</p>
            <Link to={cta.to}><Button size="lg" icon={ArrowRight} className="flex-row-reverse">{cta.label}</Button></Link>
          </div>
        </Card>
      </section>

      <footer className="border-t border-slate-900/5 py-8 text-center text-xs text-slate-500 dark:border-white/10 dark:text-slate-400">
        © {new Date().getFullYear()} EventSphere · Built with Spring Boot, React & Gemini
      </footer>
    </div>
  )
}
