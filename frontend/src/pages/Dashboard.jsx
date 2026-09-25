import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Area, AreaChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import {
  Activity, CalendarDays, CalendarPlus, CheckCircle2, Clock3, Hourglass, Radio, ScanLine, Star, Ticket, TrendingUp,
} from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { categoryMeta, CHART_COLORS } from '../lib/constants'
import { fmtDay, fmtRange, relative } from '../lib/format'
import { Badge, Button, Card, EmptyState, PageHeader, PageLoader, Progress, StatCard } from '../components/ui'
import { axisProps, ChartTooltip } from '../components/charts'

export default function Dashboard() {
  const { user, isAdmin } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)

  useEffect(() => {
    api.get('/dashboard').then(setData).catch((e) => toast.error(e.message))
  }, [])

  if (!data) return <PageLoader />

  const trend = data.registrationTrend.map((p) => ({ ...p, label: fmtDay(p.date).replace(/^\w+,?\s/, '') }))
  const categories = data.eventsByCategory.map((c) => ({ name: categoryMeta(c.category).label, value: c.count }))

  return (
    <>
      <PageHeader
        eyebrow={isAdmin ? 'Platform overview' : 'Organizer workspace'}
        title="Dashboard"
        subtitle={isAdmin ? 'Every event on EventSphere at a glance.' : `Your events at a glance, ${user.fullName.split(' ')[0]}.`}
        actions={<>
          <Link to="/check-in"><Button variant="secondary" icon={ScanLine}>Open scanner</Button></Link>
          <Link to="/events/new"><Button icon={CalendarPlus}>Create event</Button></Link>
        </>}
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={CalendarDays} label="Total events" value={data.totalEvents}
          hint={`${data.upcomingEvents} upcoming · ${data.draftEvents} draft`} delay={0} />
        <StatCard icon={Radio} label="Live now" value={data.liveEvents} tone="emerald" hint={`${data.completedEvents} completed`} delay={0.05} />
        <StatCard icon={Ticket} label="Active registrations" value={data.totalRegistrations} tone="fuchsia"
          hint={`${data.waitlisted} on waitlists`} delay={0.1} />
        <StatCard icon={CheckCircle2} label="QR check-ins" value={data.totalCheckIns} tone="sky" hint="Across all sessions" delay={0.15} />
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <StatCard icon={Activity} label="Avg. attendance rate" value={data.averageAttendanceRate} suffix="%" decimals={1}
          tone="emerald" hint="Attendees ÷ confirmed, completed events" delay={0.2} />
        <StatCard icon={Star} label="Avg. rating" value={data.averageRating} decimals={1} suffix=" / 5" tone="amber"
          hint="Attendance-verified feedback only" delay={0.25} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="p-6 lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="font-bold">Registrations</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">Last 14 days</p>
            </div>
            <TrendingUp className="size-5 text-brand-500" />
          </div>
          <div className="h-64">
            <ResponsiveContainer>
              <AreaChart data={trend} margin={{ left: -24, right: 8, top: 8 }}>
                <defs>
                  <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#8b5cf6" stopOpacity={0.45} />
                    <stop offset="100%" stopColor="#8b5cf6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,.18)" vertical={false} />
                <XAxis dataKey="label" {...axisProps} interval="preserveStartEnd" />
                <YAxis {...axisProps} allowDecimals={false} />
                <Tooltip content={<ChartTooltip />} />
                <Area type="monotone" dataKey="count" name="Registrations" stroke="#8b5cf6" strokeWidth={2.5} fill="url(#trendFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="font-bold">Events by category</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">Portfolio mix</p>
          {categories.length ? (
            <>
              <div className="h-44">
                <ResponsiveContainer>
                  <PieChart>
                    <Pie data={categories} dataKey="value" nameKey="name" innerRadius={48} outerRadius={72} paddingAngle={3} stroke="none">
                      {categories.map((_, i) => <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1.5 text-xs">
                {categories.map((c, i) => (
                  <span key={c.name} className="flex items-center gap-2 text-slate-600 dark:text-slate-300">
                    <span className="size-2.5 rounded-full" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
                    {c.name} <span className="ml-auto font-semibold">{c.value}</span>
                  </span>
                ))}
              </div>
            </>
          ) : <p className="py-16 text-center text-sm text-slate-500">No events yet</p>}
        </Card>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="mb-4 font-bold">Best-filled events</h2>
          {data.topEvents.length ? (
            <div className="space-y-4">
              {data.topEvents.map((e) => (
                <Link key={e.id} to={`/events/${e.id}`} className="block group">
                  <div className="mb-1.5 flex items-center justify-between gap-3 text-sm">
                    <span className="truncate font-semibold group-hover:text-brand-600 dark:group-hover:text-brand-300">{e.title}</span>
                    <span className="shrink-0 text-xs text-slate-500 dark:text-slate-400">{e.confirmed}/{e.capacity} · {Math.round(e.fillRate)}%</span>
                  </div>
                  <Progress value={e.fillRate} tone={e.fillRate >= 100 ? 'amber' : 'brand'} />
                </Link>
              ))}
            </div>
          ) : <p className="text-sm text-slate-500">Publish an event to see how it fills up.</p>}
        </Card>

        <Card className="p-6">
          <h2 className="mb-4 font-bold">Coming up next</h2>
          {data.upcoming.length ? (
            <ul className="divide-y divide-slate-900/5 dark:divide-white/5">
              {data.upcoming.map((e) => (
                <li key={e.id}>
                  <Link to={`/events/${e.id}`} className="flex items-center gap-3 py-3 group">
                    <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-500/10 text-brand-500">
                      {e.phase === 'LIVE' ? <Radio className="size-5" /> : <Clock3 className="size-5" />}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold group-hover:text-brand-600 dark:group-hover:text-brand-300">{e.title}</p>
                      <p className="truncate text-xs text-slate-500 dark:text-slate-400">{fmtRange(e.startDateTime, e.endDateTime)}</p>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      <Badge status={e.phase} />
                      <span className="text-[11px] text-slate-500">{e.phase === 'LIVE' ? 'happening now' : relative(e.startDateTime)}</span>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState icon={Hourglass} title="Nothing scheduled" text="Create and publish an event to fill your calendar."
              action={<Link to="/events/new"><Button size="sm" icon={CalendarPlus}>Create event</Button></Link>} />
          )}
        </Card>
      </div>
    </>
  )
}
