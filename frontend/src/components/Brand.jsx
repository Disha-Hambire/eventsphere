import { Link } from 'react-router-dom'

export function Logo({ className = 'size-9' }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <defs>
        <linearGradient id="es-logo" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" />
          <stop offset="0.55" stopColor="#8b5cf6" />
          <stop offset="1" stopColor="#d946ef" />
        </linearGradient>
      </defs>
      <rect width="64" height="64" rx="16" fill="url(#es-logo)" />
      <circle cx="32" cy="32" r="13" fill="none" stroke="#fff" strokeWidth="5" />
      <ellipse cx="32" cy="32" rx="22" ry="8" fill="none" stroke="#fff" strokeWidth="3" opacity=".75" transform="rotate(-25 32 32)" />
    </svg>
  )
}

export function Brand({ to = '/' }) {
  return (
    <Link to={to} className="flex items-center gap-2.5">
      <Logo />
      <span className="text-lg font-extrabold tracking-tight">
        Event<span className="text-gradient">Sphere</span>
      </span>
    </Link>
  )
}

/**
 * Soft gradient blobs behind the page. Animation is opt-in (landing page only): an animated layer under many
 * backdrop-blur cards forces the browser to re-blur every card on every frame.
 */
export function Backdrop({ animated = false }) {
  const float = animated ? 'animate-float' : ''
  const floatSlow = animated ? 'animate-float-slow' : ''
  return (
    <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden" aria-hidden="true">
      <div className={`absolute -top-40 -left-32 size-[34rem] rounded-full bg-brand-400/25 blur-3xl ${floatSlow} dark:bg-brand-600/20`} />
      <div className={`absolute top-1/3 -right-40 size-[30rem] rounded-full bg-fuchsia-400/20 blur-3xl ${float} dark:bg-fuchsia-600/15`} />
      <div className={`absolute -bottom-48 left-1/3 size-[28rem] rounded-full bg-cyan-300/20 blur-3xl ${floatSlow} dark:bg-cyan-500/10`} />
      <div className="absolute inset-0 bg-[radial-gradient(rgba(99,102,241,0.08)_1px,transparent_1px)] [background-size:22px_22px] dark:bg-[radial-gradient(rgba(148,163,184,0.06)_1px,transparent_1px)]" />
    </div>
  )
}
