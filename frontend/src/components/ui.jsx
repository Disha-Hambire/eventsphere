import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion, useInView } from 'motion/react'
import { Loader2, X } from 'lucide-react'
import { PHASE_LABEL, STATUS_STYLES } from '../lib/constants'

const cx = (...c) => c.filter(Boolean).join(' ')

// ------------------------------------------------------------------ Button

const VARIANTS = {
  primary: 'bg-brand-gradient text-white shadow-lg shadow-brand-500/25 hover:shadow-brand-500/40 hover:brightness-110',
  secondary: 'glass text-slate-700 hover:bg-white dark:text-slate-200 dark:hover:bg-slate-800/70',
  ghost: 'text-slate-600 hover:bg-slate-900/5 dark:text-slate-300 dark:hover:bg-white/5',
  danger: 'bg-rose-500/10 text-rose-600 ring-1 ring-rose-500/20 hover:bg-rose-500/15 dark:text-rose-300',
  success: 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/25 hover:bg-emerald-600',
}
const SIZES = {
  sm: 'h-8 px-3 text-xs gap-1.5',
  md: 'h-10 px-4 text-sm gap-2',
  lg: 'h-12 px-6 text-base gap-2',
}

export function Button({ variant = 'primary', size = 'md', loading, icon: Icon, className, children, disabled, ...props }) {
  return (
    <button
      className={cx(
        'inline-flex shrink-0 items-center justify-center rounded-xl font-semibold transition-all duration-200 active:scale-[0.98]',
        'disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/25',
        VARIANTS[variant], SIZES[size], className,
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? <Loader2 className="size-4 animate-spin" /> : Icon && <Icon className="size-4" />}
      {children}
    </button>
  )
}

// ------------------------------------------------------------------ Surfaces

export function Card({ className, children, hover, ...props }) {
  return (
    <div
      className={cx('glass rounded-2xl', hover && 'transition duration-300 hover:-translate-y-0.5 hover:shadow-xl', className)}
      {...props}
    >
      {children}
    </div>
  )
}

export function Badge({ status, children, className }) {
  const label = children ?? PHASE_LABEL[status] ?? status
  return (
    <span className={cx('inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset',
      STATUS_STYLES[status] || STATUS_STYLES.DRAFT, className)}>
      {status === 'LIVE' && (
        <span className="relative flex size-2">
          <span className="absolute inline-flex size-full animate-ping rounded-full bg-emerald-400 opacity-75" />
          <span className="relative inline-flex size-2 rounded-full bg-emerald-500" />
        </span>
      )}
      {label}
    </span>
  )
}

export function PageHeader({ title, subtitle, actions, eyebrow }) {
  return (
    <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        {eyebrow && <p className="mb-1 text-xs font-semibold uppercase tracking-widest text-brand-500">{eyebrow}</p>}
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{title}</h1>
        {subtitle && <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <Card className="flex flex-col items-center px-6 py-14 text-center">
      {Icon && (
        <div className="mb-4 grid size-14 place-items-center rounded-2xl bg-brand-500/10 text-brand-500">
          <Icon className="size-7" />
        </div>
      )}
      <h3 className="text-lg font-semibold">{title}</h3>
      {text && <p className="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">{text}</p>}
      {action && <div className="mt-5">{action}</div>}
    </Card>
  )
}

export function Spinner({ className }) {
  return <Loader2 className={cx('size-6 animate-spin text-brand-500', className)} />
}

export function PageLoader() {
  return (
    <div className="space-y-4">
      <div className="skeleton h-9 w-64" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[0, 1, 2, 3].map((i) => <div key={i} className="skeleton h-28" />)}
      </div>
      <div className="skeleton h-72" />
    </div>
  )
}

// ------------------------------------------------------------------ Animated numbers

export function CountUp({ value, decimals = 0, suffix = '', duration = 1200 }) {
  const ref = useRef(null)
  const inView = useInView(ref, { once: true })
  const [display, setDisplay] = useState(0)
  const settled = useRef(false)

  // Count up when the number scrolls into view.
  useEffect(() => {
    if (!inView || value == null || settled.current) return
    let frame
    const start = performance.now()
    const tick = (now) => {
      const p = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - p, 3)
      setDisplay(value * eased)
      if (p < 1) frame = requestAnimationFrame(tick)
      else settled.current = true
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [inView, value, duration])

  // Safety net: IntersectionObserver and requestAnimationFrame both pause while a tab is hidden or not painting,
  // so always land on the real number shortly after it arrives, never leaving a misleading 0 on screen.
  useEffect(() => {
    if (value == null) return
    settled.current = false
    const t = setTimeout(() => { settled.current = true; setDisplay(value) }, duration + 300)
    return () => clearTimeout(t)
  }, [value, duration])

  if (value == null) return <span ref={ref}>–</span>
  return <span ref={ref}>{display.toLocaleString('en-IN', { maximumFractionDigits: decimals, minimumFractionDigits: decimals })}{suffix}</span>
}

export function StatCard({ icon: Icon, label, value, suffix, decimals, hint, tone = 'brand', delay = 0 }) {
  const tones = {
    brand: 'from-brand-500 to-violet-500 shadow-brand-500/30',
    emerald: 'from-emerald-500 to-teal-500 shadow-emerald-500/30',
    amber: 'from-amber-500 to-orange-500 shadow-amber-500/30',
    fuchsia: 'from-fuchsia-500 to-pink-500 shadow-fuchsia-500/30',
    sky: 'from-sky-500 to-cyan-500 shadow-sky-500/30',
  }
  return (
    <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay, duration: 0.4 }}>
      <Card hover className="relative overflow-hidden p-5">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</p>
            <p className="mt-2 text-3xl font-bold tracking-tight">
              <CountUp value={value} suffix={suffix} decimals={decimals} />
            </p>
            {hint && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p>}
          </div>
          {Icon && (
            <div className={cx('grid size-11 place-items-center rounded-xl bg-gradient-to-br text-white shadow-lg', tones[tone])}>
              <Icon className="size-5" />
            </div>
          )}
        </div>
      </Card>
    </motion.div>
  )
}

// ------------------------------------------------------------------ Progress

export function Progress({ value, className, tone = 'brand' }) {
  const color = tone === 'emerald' ? 'from-emerald-400 to-teal-500'
    : tone === 'amber' ? 'from-amber-400 to-orange-500' : 'from-brand-500 via-violet-500 to-fuchsia-500'
  return (
    <div className={cx('h-2 overflow-hidden rounded-full bg-slate-200/70 dark:bg-white/10', className)}>
      <motion.div
        className={cx('h-full rounded-full bg-gradient-to-r', color)}
        initial={{ width: 0 }}
        animate={{ width: `${Math.min(100, Math.max(0, value || 0))}%` }}
        transition={{ duration: 0.9, ease: 'easeOut' }}
      />
    </div>
  )
}

// ------------------------------------------------------------------ Modal

export function Modal({ open, onClose, title, subtitle, children, footer, size = 'md' }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const widths = { sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl' }
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/50 p-0 backdrop-blur-sm sm:items-center sm:p-4"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}
        >
          <motion.div
            role="dialog" aria-modal="true"
            className={cx('glass-strong max-h-[92vh] w-full overflow-y-auto rounded-t-3xl p-6 shadow-2xl sm:rounded-3xl', widths[size])}
            initial={{ y: 40, opacity: 0, scale: 0.98 }} animate={{ y: 0, opacity: 1, scale: 1 }}
            exit={{ y: 30, opacity: 0, scale: 0.98 }} transition={{ type: 'spring', stiffness: 400, damping: 34 }}
          >
            <div className="mb-5 flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold">{title}</h2>
                {subtitle && <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>}
              </div>
              <button onClick={onClose} className="rounded-lg p-1 text-slate-400 hover:bg-slate-900/5 hover:text-slate-700 dark:hover:bg-white/5" aria-label="Close">
                <X className="size-5" />
              </button>
            </div>
            {children}
            {footer && <div className="mt-6 flex flex-wrap justify-end gap-2">{footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

export function ConfirmDialog({ open, title, text, confirmLabel = 'Confirm', danger, loading, onConfirm, onClose }) {
  return (
    <Modal open={open} onClose={onClose} title={title} size="sm"
      footer={<>
        <Button variant="ghost" onClick={onClose}>Keep it</Button>
        <Button variant={danger ? 'danger' : 'primary'} loading={loading} onClick={onConfirm}>{confirmLabel}</Button>
      </>}>
      <p className="text-sm text-slate-600 dark:text-slate-300">{text}</p>
    </Modal>
  )
}

// ------------------------------------------------------------------ Form fields

export function Field({ label, error, hint, children, className }) {
  return (
    <label className={cx('block', className)}>
      {label && <span className="label">{label}</span>}
      {children}
      {error ? <span className="mt-1 block text-xs font-medium text-rose-500">{error}</span>
        : hint && <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{hint}</span>}
    </label>
  )
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="glass mb-6 inline-flex max-w-full gap-1 overflow-x-auto rounded-2xl p-1">
      {tabs.map((t) => (
        <button
          key={t.value}
          onClick={() => onChange(t.value)}
          className={cx('relative flex items-center gap-2 whitespace-nowrap rounded-xl px-4 py-2 text-sm font-semibold transition',
            value === t.value ? 'text-white' : 'text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-white')}
        >
          {value === t.value && (
            <motion.span layoutId="tab-pill" className="absolute inset-0 rounded-xl bg-brand-gradient shadow-lg shadow-brand-500/30"
              transition={{ type: 'spring', stiffness: 420, damping: 34 }} />
          )}
          <span className="relative flex items-center gap-2">
            {t.icon && <t.icon className="size-4" />}
            {t.label}
            {t.count != null && <span className="rounded-full bg-black/10 px-1.5 text-[11px] dark:bg-white/15">{t.count}</span>}
          </span>
        </button>
      ))}
    </div>
  )
}

export function Avatar({ name, className }) {
  const letters = (name || '?').split(' ').filter(Boolean).slice(0, 2).map((w) => w[0]).join('').toUpperCase()
  return (
    <span className={cx('grid size-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-brand-500 to-fuchsia-500 text-xs font-bold text-white', className)}>
      {letters}
    </span>
  )
}
