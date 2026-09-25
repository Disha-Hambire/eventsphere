import { motion } from 'motion/react'
import { ArrowUpCircle, CheckCircle2, Clock, MessageSquareHeart, ScanLine, XCircle } from 'lucide-react'
import { fmtDateTime } from '../lib/format'

const ICONS = {
  CONFIRMED: { icon: CheckCircle2, cls: 'bg-emerald-500 text-white' },
  REGISTERED: { icon: CheckCircle2, cls: 'bg-slate-400 text-white' },
  WAITLISTED: { icon: Clock, cls: 'bg-amber-500 text-white' },
  PROMOTED: { icon: ArrowUpCircle, cls: 'bg-brand-500 text-white' },
  CHECKED_IN: { icon: ScanLine, cls: 'bg-sky-500 text-white' },
  CANCELLED: { icon: XCircle, cls: 'bg-rose-500 text-white' },
  FEEDBACK: { icon: MessageSquareHeart, cls: 'bg-fuchsia-500 text-white' },
}

/** The life story of one registration: registered → waitlisted → promoted → checked in → feedback. */
export default function Timeline({ entries }) {
  if (!entries?.length) return null
  return (
    <ol className="relative space-y-4 pl-1">
      <span className="absolute bottom-3 left-[15px] top-3 w-px bg-gradient-to-b from-brand-500/40 to-fuchsia-500/10" />
      {entries.map((e, i) => {
        const { icon: Icon, cls } = ICONS[e.type] || ICONS.REGISTERED
        return (
          <motion.li key={i} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.06 }}
            className="relative flex items-start gap-3">
            <span className={`relative z-10 grid size-[30px] shrink-0 place-items-center rounded-full shadow-md ${cls}`}>
              <Icon className="size-4" />
            </span>
            <div className="pt-1">
              <p className="text-sm font-semibold">{e.label}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">{fmtDateTime(e.at)}</p>
            </div>
          </motion.li>
        )
      })}
    </ol>
  )
}
