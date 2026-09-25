import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'motion/react'
import { ArrowLeft, ArrowRight, CalendarCheck2, KeyRound, LogIn, MailCheck, ShieldCheck, User } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { homeFor } from '../lib/nav'
import { Backdrop, Brand } from '../components/Brand'
import ThemeToggle from '../components/ThemeToggle'
import { Button, Card, Field } from '../components/ui'

const DEMO_ACCOUNTS = [
  { role: 'Admin', icon: ShieldCheck, email: 'admin@eventsphere.com', password: 'Admin@123' },
  { role: 'Organizer', icon: CalendarCheck2, email: 'organizer@eventsphere.com', password: 'Organizer@123' },
  { role: 'Participant', icon: User, email: 'participant@eventsphere.com', password: 'Participant@123' },
]

function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="relative flex min-h-screen flex-col">
      <Backdrop />
      <header className="flex items-center justify-between px-4 py-5 sm:px-8">
        <Brand />
        <ThemeToggle />
      </header>
      <div className="flex flex-1 items-center justify-center px-4 pb-16">
        <motion.div initial={{ opacity: 0, y: 20, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.4 }} className="w-full max-w-md">
          <Card className="p-7 sm:p-8">
            <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>
            <div className="mt-7">{children}</div>
          </Card>
          {footer}
        </motion.div>
      </div>
    </div>
  )
}

export function Login() {
  const { user, login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (user) return <Navigate to={homeFor(user)} replace />

  const submit = async (e, creds = form) => {
    e?.preventDefault()
    setLoading(true)
    setError('')
    try {
      const u = await login(creds.email, creds.password)
      toast.success(`Signed in as ${u.fullName}`)
      navigate(homeFor(u))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Log in to manage or attend events."
      footer={
        <Card className="mt-4 p-5">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">One-click demo accounts</p>
          <div className="grid grid-cols-3 gap-2">
            {DEMO_ACCOUNTS.map((a) => (
              <button key={a.role} disabled={loading}
                onClick={() => { setForm(a); submit(null, a) }}
                className="glass flex flex-col items-center gap-1.5 rounded-xl px-2 py-3 text-xs font-semibold transition hover:-translate-y-0.5 hover:text-brand-600 dark:hover:text-brand-300">
                <a.icon className="size-5 text-brand-500" />
                {a.role}
              </button>
            ))}
          </div>
        </Card>
      }>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email">
          <input className="input" type="email" autoComplete="email" required value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@example.com" />
        </Field>
        <Field label="Password">
          <input className="input" type="password" autoComplete="current-password" required value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="••••••••" />
        </Field>
        <div className="-mt-2 flex justify-end">
          <Link to="/forgot-password" state={{ email: form.email }}
            className="text-xs font-semibold text-brand-600 hover:underline dark:text-brand-300">
            Forgot password?
          </Link>
        </div>
        {error && <p className="rounded-xl bg-rose-500/10 px-3 py-2 text-sm font-medium text-rose-600 dark:text-rose-300">{error}</p>}
        <Button type="submit" loading={loading} icon={LogIn} className="w-full">Log in</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500 dark:text-slate-400">
        New here? <Link to="/register" className="font-semibold text-brand-600 hover:underline dark:text-brand-300">Create an account</Link>
      </p>
    </AuthLayout>
  )
}

export function Register() {
  const { user, register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '', organization: '' })
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')

  if (user) return <Navigate to={homeFor(user)} replace />

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setErrors({})
    try {
      const u = await register(form)
      toast.success('Your account is ready. Find an event to join!', `Welcome, ${u.fullName.split(' ')[0]}`)
      navigate(homeFor(u))
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout title="Create your account" subtitle="Join as a participant. Organizer access is granted by an admin.">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Full name" error={errors.fullName}>
          <input className="input" required value={form.fullName} onChange={set('fullName')} placeholder="Ananya Iyer" />
        </Field>
        <Field label="Email" error={errors.email}>
          <input className="input" type="email" required value={form.email} onChange={set('email')} placeholder="you@example.com" />
        </Field>
        <Field label="Password" error={errors.password} hint="At least 6 characters">
          <input className="input" type="password" required minLength={6} value={form.password} onChange={set('password')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Phone (optional)">
            <input className="input" value={form.phone} onChange={set('phone')} placeholder="+91 98xxxxxx" />
          </Field>
          <Field label="College / company">
            <input className="input" value={form.organization} onChange={set('organization')} />
          </Field>
        </div>
        {error && !Object.keys(errors).length && (
          <p className="rounded-xl bg-rose-500/10 px-3 py-2 text-sm font-medium text-rose-600 dark:text-rose-300">{error}</p>
        )}
        <Button type="submit" loading={loading} icon={ArrowRight} className="w-full flex-row-reverse">Create account</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500 dark:text-slate-400">
        Already have an account? <Link to="/login" className="font-semibold text-brand-600 hover:underline dark:text-brand-300">Log in</Link>
      </p>
    </AuthLayout>
  )
}

const BackToLogin = () => (
  <p className="mt-6 text-center text-sm">
    <Link to="/login" className="inline-flex items-center gap-1.5 font-semibold text-brand-600 hover:underline dark:text-brand-300">
      <ArrowLeft className="size-4" /> Back to log in
    </Link>
  </p>
)

const ErrorBox = ({ children }) => (
  <p className="rounded-xl bg-rose-500/10 px-3 py-2 text-sm font-medium text-rose-600 dark:text-rose-300">{children}</p>
)

/**
 * Forgot password in two steps on one page:
 *  1. enter email -> a 6-digit code is emailed (the answer never reveals whether the account exists)
 *  2. enter the code + a new password
 */
export function ForgotPassword() {
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState('email')
  const [email, setEmail] = useState(location.state?.email || '')
  const [info, setInfo] = useState(null) // { message, demoCode }
  const [form, setForm] = useState({ code: '', password: '', confirm: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const sendCode = async (e) => {
    e?.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await api.post('/auth/forgot-password', { email })
      setInfo(res)
      setForm({ code: '', password: '', confirm: '' })
      setStep('code')
      if (e === undefined) toast.info('A new code has been sent')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const reset = async (e) => {
    e.preventDefault()
    if (form.password !== form.confirm) {
      setError('The two passwords do not match')
      return
    }
    setLoading(true)
    setError('')
    try {
      const res = await api.post('/auth/reset-password', { email, code: form.code, newPassword: form.password })
      toast.success(res.message)
      navigate('/login', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  if (step === 'email') {
    return (
      <AuthLayout title="Forgot your password?" subtitle="Enter your account email and we'll send you a 6-digit code.">
        <form onSubmit={sendCode} className="space-y-4">
          <Field label="Email">
            <input className="input" type="email" autoComplete="email" required autoFocus value={email}
              onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </Field>
          {error && <ErrorBox>{error}</ErrorBox>}
          <Button type="submit" loading={loading} icon={MailCheck} className="w-full">Send code</Button>
        </form>
        <BackToLogin />
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Enter the code" subtitle={info?.message}>
      {info?.demoCode && (
        <div className="mb-5 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm">
          <p className="font-semibold text-amber-700 dark:text-amber-300">Demo mode: no email server configured</p>
          <p className="mt-1 text-slate-600 dark:text-slate-300">In production this code is emailed to {email}. Your code is:</p>
          <p className="mt-2 font-mono text-2xl font-bold tracking-[0.4em] text-slate-900 dark:text-white">{info.demoCode}</p>
        </div>
      )}
      <form onSubmit={reset} className="space-y-4">
        <Field label="6-digit code">
          <input className="input text-center font-mono text-2xl tracking-[0.5em]" inputMode="numeric" autoComplete="one-time-code"
            required autoFocus maxLength={6} pattern="\d{6}" placeholder="••••••" value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value.replace(/\D/g, '').slice(0, 6) })} />
        </Field>
        <Field label="New password" hint="At least 6 characters">
          <input className="input" type="password" autoComplete="new-password" required minLength={6} maxLength={72}
            value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <Field label="Confirm new password">
          <input className="input" type="password" autoComplete="new-password" required minLength={6} maxLength={72}
            value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
        </Field>
        {error && <ErrorBox>{error}</ErrorBox>}
        <Button type="submit" loading={loading} icon={KeyRound} className="w-full">Update password</Button>
      </form>
      <div className="mt-4 flex items-center justify-between text-xs">
        <button onClick={() => { setStep('email'); setError('') }} className="font-semibold text-slate-500 hover:text-brand-600">
          Use a different email
        </button>
        <button onClick={() => sendCode()} disabled={loading} className="font-semibold text-brand-600 hover:underline disabled:opacity-50 dark:text-brand-300">
          Resend code
        </button>
      </div>
      <BackToLogin />
    </AuthLayout>
  )
}
