import { useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { motion } from 'motion/react'
import {
  ArrowUpCircle, BrainCircuit, CheckCircle2, Lightbulb, MessageSquareQuote, RefreshCw, Sparkles, Star, ThumbsUp,
  TrendingDown, TrendingUp, UserX, Users,
} from 'lucide-react'
import { api } from '../../lib/api'
import { useToast } from '../../context/ToastContext'
import { fmtDateTime, fmtTime } from '../../lib/format'
import StarRating from '../StarRating'
import { axisProps, ChartTooltip } from '../charts'
import { Button, Card, EmptyState, Progress, Spinner, StatCard } from '../ui'

const SENTIMENT = {
  POSITIVE: { label: 'Positive', cls: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-300', icon: TrendingUp },
  MIXED: { label: 'Mixed', cls: 'bg-amber-500/10 text-amber-600 dark:text-amber-300', icon: RefreshCw },
  NEGATIVE: { label: 'Negative', cls: 'bg-rose-500/10 text-rose-600 dark:text-rose-300', icon: TrendingDown },
}

function AiSummaryCard({ summary, onGenerate, generating, feedbackCount }) {
  return (
    <Card className="relative overflow-hidden p-6">
      <div className="absolute inset-0 -z-10 bg-gradient-to-br from-brand-500/10 via-violet-500/5 to-fuchsia-500/10" />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="grid size-10 place-items-center rounded-xl bg-brand-gradient text-white shadow-lg shadow-brand-500/30">
            <BrainCircuit className="size-5" />
          </div>
          <div>
            <h3 className="font-bold">AI feedback summary</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {summary ? `${summary.source === 'GEMINI' ? 'Gemini' : 'Built-in analyser'} · ${fmtDateTime(summary.generatedAt)}` : `Turn ${feedbackCount} responses into actions`}
            </p>
          </div>
        </div>
        <Button size="sm" icon={summary ? RefreshCw : Sparkles} loading={generating} onClick={onGenerate} disabled={!feedbackCount}>
          {summary ? 'Regenerate' : 'Generate summary'}
        </Button>
      </div>

      {summary ? (
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="mt-5 space-y-5">
          <div className="flex flex-wrap items-center gap-3">
            {(() => {
              const s = SENTIMENT[summary.sentiment] || SENTIMENT.MIXED
              return <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold ${s.cls}`}><s.icon className="size-3.5" />{s.label} sentiment</span>
            })()}
            <p className="text-sm font-medium">{summary.headline}</p>
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            {[
              { title: 'What worked', icon: ThumbsUp, items: summary.strengths, cls: 'text-emerald-500' },
              { title: 'What to improve', icon: TrendingDown, items: summary.improvements, cls: 'text-amber-500' },
              { title: 'Next-event actions', icon: Lightbulb, items: summary.actionItems, cls: 'text-brand-500' },
            ].map((col) => (
              <div key={col.title} className="rounded-2xl bg-white/60 p-4 ring-1 ring-slate-900/5 dark:bg-slate-950/40 dark:ring-white/5">
                <p className="mb-2 flex items-center gap-2 text-sm font-bold"><col.icon className={`size-4 ${col.cls}`} />{col.title}</p>
                <ul className="space-y-1.5 text-sm text-slate-600 dark:text-slate-300">
                  {col.items?.length ? col.items.map((it, i) => <li key={i} className="flex gap-2"><span className="text-slate-400">•</span>{it}</li>)
                    : <li className="text-slate-400">Nothing notable</li>}
                </ul>
              </div>
            ))}
          </div>
        </motion.div>
      ) : (
        <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
          {feedbackCount ? 'Generate a summary to see strengths, improvement areas and concrete actions.' : 'Feedback opens when the event is completed. Only attendees can respond.'}
        </p>
      )}
    </Card>
  )
}

export default function AnalyticsTab({ event }) {
  const toast = useToast()
  const [report, setReport] = useState(null)
  const [feedback, setFeedback] = useState([])
  const [generating, setGenerating] = useState(false)

  useEffect(() => {
    api.get(`/events/${event.id}/report`).then(setReport).catch((e) => toast.error(e.message))
    api.get(`/events/${event.id}/feedback`).then(setFeedback).catch(() => setFeedback([]))
  }, [event.id])

  const generate = async () => {
    setGenerating(true)
    try {
      const summary = await api.post(`/events/${event.id}/ai-summary`)
      setReport((r) => ({ ...r, aiSummary: summary }))
      toast.success('Summary ready')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setGenerating(false)
    }
  }

  if (!report) return <div className="grid place-items-center py-20"><Spinner /></div>

  const sessionData = report.sessions.map((s) => ({ name: s.title.length > 22 ? `${s.title.slice(0, 20)}…` : s.title, full: s.title, attended: s.attended, rate: s.attendanceRate, time: fmtTime(s.startTime) }))
  const ratingData = Object.entries(report.ratingDistribution).map(([star, count]) => ({ star: `${star}★`, count }))

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Users} label="Fill rate" value={report.fillRate} suffix="%" decimals={1}
          hint={`${report.confirmed}/${report.capacity} seats · ${report.waitlisted} waitlisted`} />
        <StatCard icon={CheckCircle2} label="Attendance rate" value={report.attendanceRate} suffix="%" decimals={1} tone="emerald"
          hint={`${report.attendees} of ${report.confirmed} confirmed attended`} delay={0.05} />
        <StatCard icon={UserX} label="No-shows" value={report.noShows} tone="amber" hint={`${report.cancelled} cancellations`} delay={0.1} />
        <StatCard icon={ArrowUpCircle} label="Waitlist promotions" value={report.promotedFromWaitlist} tone="fuchsia" hint="Seats re-filled automatically" delay={0.15} />
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="p-6 lg:col-span-3">
          <h3 className="font-bold">Session popularity</h3>
          <p className="mb-4 text-xs text-slate-500 dark:text-slate-400">Check-ins per session (share of confirmed participants)</p>
          {sessionData.length ? (
            <div className="h-64">
              <ResponsiveContainer>
                <BarChart data={sessionData} margin={{ left: -20, right: 8, top: 8 }}>
                  <defs>
                    <linearGradient id="barFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#8b5cf6" />
                      <stop offset="100%" stopColor="#6366f1" />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,.18)" vertical={false} />
                  <XAxis dataKey="name" {...axisProps} interval={0} tick={{ fontSize: 10 }} />
                  <YAxis {...axisProps} allowDecimals={false} />
                  <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(99,102,241,.06)' }} />
                  <Bar dataKey="attended" name="Checked in" fill="url(#barFill)" radius={[8, 8, 0, 0]} maxBarSize={56} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : <p className="py-12 text-center text-sm text-slate-500">No sessions</p>}
        </Card>

        <Card className="p-6 lg:col-span-2">
          <h3 className="font-bold">Ratings</h3>
          <p className="mb-4 text-xs text-slate-500 dark:text-slate-400">{report.feedbackCount} verified responses</p>
          {report.feedbackCount ? (
            <>
              <div className="flex items-end gap-3">
                <p className="text-5xl font-extrabold">{report.averageRating?.toFixed(1)}</p>
                <div className="pb-1.5">
                  <StarRating value={Math.round(report.averageRating)} readOnly size="size-4" />
                  <p className="mt-1 text-xs text-slate-500">{Math.round(report.recommendRate)}% would recommend</p>
                </div>
              </div>
              <div className="mt-5 h-36">
                <ResponsiveContainer>
                  <BarChart data={ratingData} layout="vertical" margin={{ left: -18, right: 8 }}>
                    <XAxis type="number" hide allowDecimals={false} />
                    <YAxis type="category" dataKey="star" {...axisProps} width={48} />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(245,158,11,.06)' }} />
                    <Bar dataKey="count" name="Responses" radius={[0, 6, 6, 0]} maxBarSize={16}>
                      {ratingData.map((_, i) => <Cell key={i} fill={['#10b981', '#84cc16', '#f59e0b', '#f97316', '#f43f5e'][i]} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div className="mt-3 space-y-2 text-xs">
                {[['Content & speakers', report.averageContentRating], ['Organisation', report.averageOrganizationRating]].map(([label, v]) => (
                  <div key={label}>
                    <div className="mb-1 flex justify-between"><span className="text-slate-500">{label}</span><span className="font-semibold">{v?.toFixed(1)}/5</span></div>
                    <Progress value={(v / 5) * 100} />
                  </div>
                ))}
              </div>
            </>
          ) : <EmptyState icon={Star} title="No feedback yet" text="Opens after the event is completed." />}
        </Card>
      </div>

      <AiSummaryCard summary={report.aiSummary} onGenerate={generate} generating={generating} feedbackCount={report.feedbackCount} />

      {feedback.length > 0 && (
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-bold"><MessageSquareQuote className="size-5 text-brand-500" /> What attendees said</h3>
          <div className="grid gap-4 md:grid-cols-2">
            {feedback.map((f) => (
              <div key={f.id} className="rounded-2xl bg-white/60 p-4 ring-1 ring-slate-900/5 dark:bg-slate-950/40 dark:ring-white/5">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold">{f.participantName}</p>
                  <StarRating value={f.rating} readOnly size="size-3.5" />
                </div>
                {f.comments && <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">“{f.comments}”</p>}
                <p className="mt-2 text-[11px] text-slate-400">{f.wouldRecommend ? 'Would recommend' : 'Would not recommend'} · {fmtDateTime(f.submittedAt)}</p>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}
