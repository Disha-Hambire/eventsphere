import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'motion/react'
import {
  BadgeCheck, CalendarDays, LayoutDashboard, LogOut, Menu, Mic2, ScanLine, ShieldCheck, Ticket, X, Compass,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { Backdrop, Brand } from './Brand'
import ThemeToggle from './ThemeToggle'
import { Avatar, Badge } from './ui'

function navFor(role) {
  if (role === 'PARTICIPANT') {
    return [
      { to: '/events', label: 'Discover events', icon: Compass },
      { to: '/tickets', label: 'My tickets', icon: Ticket },
      { to: '/become-organizer', label: 'Become an organizer', icon: BadgeCheck },
    ]
  }
  const items = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/events', label: 'Events', icon: CalendarDays },
    { to: '/check-in', label: 'QR check-in', icon: ScanLine },
    { to: '/speakers', label: 'Speakers', icon: Mic2 },
  ]
  if (role === 'ADMIN') items.push({ to: '/users', label: 'Users & roles', icon: ShieldCheck })
  return items
}

function NavItems({ items, onNavigate }) {
  return (
    <nav className="flex flex-col gap-1">
      {items.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          className={({ isActive }) =>
            `group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition ${
              isActive ? 'text-brand-600 dark:text-white' : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}
        >
          {({ isActive }) => (
            <>
              {isActive && (
                <motion.span layoutId="nav-active"
                  className="absolute inset-0 rounded-xl bg-brand-500/10 ring-1 ring-brand-500/20 dark:bg-white/[0.07] dark:ring-white/10"
                  transition={{ type: 'spring', stiffness: 420, damping: 36 }} />
              )}
              <Icon className="relative size-[18px]" />
              <span className="relative">{label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  )
}

export default function AppShell() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)
  const items = navFor(user.role)

  const doLogout = () => {
    logout()
    navigate('/')
  }

  const sidebar = (
    <div className="flex h-full flex-col">
      <div className="px-2 pb-8 pt-1"><Brand to={items[0].to} /></div>
      <NavItems items={items} onNavigate={() => setMobileOpen(false)} />
      <div className="mt-auto">
        <div className="glass rounded-2xl p-3">
          <div className="flex items-center gap-3">
            <Avatar name={user.fullName} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">{user.fullName}</p>
              <p className="truncate text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between">
            <Badge status={user.role} />
            <button onClick={doLogout} className="flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-rose-500">
              <LogOut className="size-3.5" /> Log out
            </button>
          </div>
        </div>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen">
      <Backdrop />
      {/* Desktop sidebar */}
      <aside className="glass fixed inset-y-3 left-3 z-30 hidden w-64 rounded-3xl p-4 lg:block">{sidebar}</aside>

      {/* Mobile drawer */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div className="fixed inset-0 z-40 bg-slate-950/40 backdrop-blur-sm lg:hidden"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setMobileOpen(false)} />
            <motion.aside className="glass-strong fixed inset-y-0 left-0 z-50 w-72 p-4 lg:hidden"
              initial={{ x: -300 }} animate={{ x: 0 }} exit={{ x: -300 }} transition={{ type: 'spring', stiffness: 380, damping: 36 }}>
              <button className="absolute right-4 top-4 text-slate-400" onClick={() => setMobileOpen(false)} aria-label="Close menu">
                <X className="size-5" />
              </button>
              {sidebar}
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      <div className="lg:pl-[17.5rem]">
        <header className="no-print sticky top-0 z-20 flex items-center justify-between gap-3 border-b border-slate-900/5 bg-slate-50/80 px-4 py-3 backdrop-blur-md sm:px-6 lg:static lg:border-0 lg:bg-transparent lg:px-8 lg:backdrop-blur-none dark:border-white/5 dark:bg-[#070b19]/80 lg:dark:bg-transparent">
          <div className="flex items-center gap-2 lg:hidden">
            <button onClick={() => setMobileOpen(true)} className="glass grid size-10 place-items-center rounded-xl" aria-label="Open menu">
              <Menu className="size-5" />
            </button>
            <Brand to={items[0].to} />
          </div>
          <div className="hidden text-sm text-slate-500 lg:block dark:text-slate-400">
            Welcome back, <span className="font-semibold text-slate-800 dark:text-slate-100">{user.fullName.split(' ')[0]}</span> 👋
          </div>
          <ThemeToggle />
        </header>
        <main className="mx-auto max-w-7xl px-4 pb-16 pt-2 sm:px-6 lg:px-8">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.25, ease: 'easeOut' }}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  )
}
