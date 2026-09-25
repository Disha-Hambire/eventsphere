import { Link } from 'react-router-dom'
import { motion } from 'motion/react'
import { CalendarClock, MapPin, Users } from 'lucide-react'
import { categoryMeta } from '../lib/constants'
import { fmtRange } from '../lib/format'
import { Badge, Progress } from './ui'

export default function EventCard({ event, index = 0, to }) {
  const cat = categoryMeta(event.category)
  const Icon = cat.icon
  const fill = event.capacity ? (event.confirmedCount / event.capacity) * 100 : 0
  const full = event.seatsLeft === 0

  return (
    <motion.div
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: Math.min(index * 0.05, 0.4), duration: 0.4 }}
    >
      <Link to={to || `/events/${event.id}`}
        className="glass group flex h-full flex-col overflow-hidden rounded-2xl transition duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-brand-500/10">
        <div className={`relative h-28 overflow-hidden bg-gradient-to-br ${cat.gradient}`}>
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_20%,rgba(255,255,255,.35),transparent_45%)]" />
          <Icon className="absolute -bottom-4 -right-3 size-28 text-white/20 transition duration-500 group-hover:scale-110 group-hover:-rotate-6" />
          <div className="absolute left-4 top-4 flex gap-2">
            <span className="rounded-full bg-white/20 px-2.5 py-0.5 text-xs font-semibold text-white backdrop-blur">{cat.label}</span>
          </div>
          <div className="absolute right-4 top-4"><Badge status={event.phase} className="bg-white/90 dark:bg-slate-900/80" /></div>
        </div>
        <div className="flex flex-1 flex-col p-5">
          <h3 className="line-clamp-2 text-base font-bold leading-snug group-hover:text-brand-600 dark:group-hover:text-brand-300">{event.title}</h3>
          <div className="mt-3 space-y-1.5 text-xs text-slate-500 dark:text-slate-400">
            <p className="flex items-center gap-2"><CalendarClock className="size-3.5 shrink-0" />{fmtRange(event.startDateTime, event.endDateTime)}</p>
            <p className="flex items-center gap-2"><MapPin className="size-3.5 shrink-0" /><span className="truncate">{event.venue}</span></p>
          </div>
          <div className="mt-auto pt-5">
            <div className="mb-1.5 flex items-center justify-between text-xs">
              <span className="flex items-center gap-1.5 font-semibold text-slate-600 dark:text-slate-300">
                <Users className="size-3.5" /> {event.confirmedCount}/{event.capacity}
              </span>
              <span className={full ? 'font-semibold text-amber-600 dark:text-amber-400' : 'text-slate-500 dark:text-slate-400'}>
                {full ? (event.waitlistCount ? `Full · ${event.waitlistCount} waitlisted` : 'Full') : `${event.seatsLeft} seats left`}
              </span>
            </div>
            <Progress value={fill} tone={full ? 'amber' : 'brand'} />
          </div>
        </div>
      </Link>
    </motion.div>
  )
}
