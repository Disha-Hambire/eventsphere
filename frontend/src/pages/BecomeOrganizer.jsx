import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'motion/react'
import { BadgeCheck, CalendarPlus, Clock, Send, XCircle } from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { fmtDateTime } from '../lib/format'
import { Button, Card, Field, PageHeader, PageLoader } from '../components/ui'

/** Participants apply for organizer access; an admin approves or rejects the request. */
export default function BecomeOrganizer() {
  const { user, refreshUser } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [request, setRequest] = useState(undefined) // undefined = loading, null = never applied
  const [form, setForm] = useState({ organization: user.organization || '', reason: '' })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = () => api.get('/organizer-requests/my').then((r) => setRequest(r || null)).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // If an admin approved the request, refresh the user so the organizer menu appears
  useEffect(() => {
    if (request?.status === 'APPROVED' && user.role === 'PARTICIPANT') refreshUser()
  }, [request, user.role, refreshUser])

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      setRequest(await api.post('/organizer-requests', form))
      toast.success('Request sent. An admin will review it shortly.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (request === undefined) return <PageLoader />

  const canApply = !request || request.status === 'REJECTED'

  return (
    <>
      <PageHeader eyebrow="Organizer access" title="Become an organizer"
        subtitle="Organizers create and run events on EventSphere. Tell us a little about you and an admin will review your request." />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          {request && (
            <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
              <Card className="p-6">
                {request.status === 'PENDING' && (
                  <div className="flex gap-4">
                    <Clock className="size-8 shrink-0 text-amber-500" />
                    <div>
                      <p className="text-lg font-bold">Your request is being reviewed</p>
                      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Sent {fmtDateTime(request.createdAt)}. You'll get an email when an admin decides.</p>
                      <p className="mt-3 rounded-xl bg-slate-900/5 p-3 text-sm italic text-slate-600 dark:bg-white/5 dark:text-slate-300">“{request.reason}”</p>
                    </div>
                  </div>
                )}
                {request.status === 'APPROVED' && (
                  <div className="flex gap-4">
                    <BadgeCheck className="size-8 shrink-0 text-emerald-500" />
                    <div>
                      <p className="text-lg font-bold">You're an organizer!</p>
                      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Approved {fmtDateTime(request.decidedAt)}{request.decidedByName ? ` by ${request.decidedByName}` : ''}.</p>
                      {request.adminNote && <p className="mt-2 text-sm">Note: {request.adminNote}</p>}
                      <Button className="mt-4" icon={CalendarPlus} onClick={async () => { await refreshUser(); navigate('/events/new') }}>
                        Create your first event
                      </Button>
                    </div>
                  </div>
                )}
                {request.status === 'REJECTED' && (
                  <div className="flex gap-4">
                    <XCircle className="size-8 shrink-0 text-rose-500" />
                    <div>
                      <p className="text-lg font-bold">Your last request wasn't approved</p>
                      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Decided {fmtDateTime(request.decidedAt)}.</p>
                      {request.adminNote && <p className="mt-2 text-sm"><span className="font-semibold">Admin's note:</span> {request.adminNote}</p>}
                      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">You can apply again below with more details.</p>
                    </div>
                  </div>
                )}
              </Card>
            </motion.div>
          )}

          {canApply && (
            <Card className="p-6">
              <form onSubmit={submit} className="space-y-4">
                <Field label="College / company / community">
                  <input className="input" value={form.organization} maxLength={160}
                    onChange={(e) => setForm({ ...form, organization: e.target.value })} placeholder="e.g. PIT Tech Club" />
                </Field>
                <Field label="Why do you want to organize events?" hint={`${form.reason.length}/1000 · at least 20 characters`}>
                  <textarea className="input min-h-32" required minLength={20} maxLength={1000} value={form.reason}
                    onChange={(e) => setForm({ ...form, reason: e.target.value })}
                    placeholder="What kind of events will you run, and for whom?" />
                </Field>
                {error && <p className="rounded-xl bg-rose-500/10 px-3 py-2 text-sm font-medium text-rose-600 dark:text-rose-300">{error}</p>}
                <Button type="submit" icon={Send} loading={saving}>Send request</Button>
              </form>
            </Card>
          )}
        </div>

        <Card className="h-fit p-6 text-sm">
          <h2 className="mb-3 font-bold">What organizers can do</h2>
          <ul className="space-y-2 text-slate-600 dark:text-slate-300">
            <li>• Create events and build the agenda</li>
            <li>• Publish events and manage registrations</li>
            <li>• Check people in with the QR scanner</li>
            <li>• See analytics and AI feedback summaries</li>
          </ul>
          <p className="mt-4 text-xs text-slate-500 dark:text-slate-400">
            Requests are reviewed by an admin to keep events on EventSphere trustworthy.
          </p>
        </Card>
      </div>
    </>
  )
}
