import { Check, XCircle } from 'lucide-react'

const STEPS = [
  { key: 'DRAFT', label: 'Draft' },
  { key: 'PUBLISHED', label: 'Registration open' },
  { key: 'LIVE', label: 'Live' },
  { key: 'COMPLETED', label: 'Completed' },
]

function stepIndex(event) {
  if (event.status === 'DRAFT') return 0
  if (event.status === 'COMPLETED') return 3
  if (event.phase === 'LIVE' || event.phase === 'ENDED') return 2
  return 1
}

/** Visualises the event state machine: DRAFT → PUBLISHED → LIVE → COMPLETED (or CANCELLED). */
export default function LifecycleStepper({ event }) {
  if (event.status === 'CANCELLED') {
    return (
      <div className="flex items-center gap-2 rounded-2xl bg-rose-500/10 px-4 py-3 text-sm font-semibold text-rose-600 dark:text-rose-300">
        <XCircle className="size-5" /> This event was cancelled. It is kept read-only for the record.
      </div>
    )
  }
  const current = stepIndex(event)
  return (
    <ol className="flex items-center">
      {STEPS.map((s, i) => {
        const done = i < current || (i === 3 && current === 3)
        const active = i === current && !done
        return (
          <li key={s.key} className="flex flex-1 items-center last:flex-none">
            <div className="flex flex-col items-center gap-1.5">
              <span className={`grid size-8 place-items-center rounded-full text-xs font-bold transition ${
                done ? 'bg-brand-gradient text-white shadow-lg shadow-brand-500/30'
                  : active ? 'bg-white text-brand-600 ring-2 ring-brand-500 dark:bg-slate-900 dark:text-brand-300'
                    : 'bg-slate-200 text-slate-500 dark:bg-white/10 dark:text-slate-400'}`}>
                {done ? <Check className="size-4" /> : i + 1}
              </span>
              <span className={`whitespace-nowrap text-[11px] font-semibold ${active || done ? 'text-slate-800 dark:text-slate-100' : 'text-slate-400'}`}>
                {s.label}
              </span>
            </div>
            {i < STEPS.length - 1 && (
              <span className={`mx-2 mb-5 h-0.5 flex-1 rounded-full ${i < current ? 'bg-gradient-to-r from-brand-500 to-violet-500' : 'bg-slate-200 dark:bg-white/10'}`} />
            )}
          </li>
        )
      })}
    </ol>
  )
}
