import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AnimatePresence, motion } from 'motion/react'
import { ArrowLeft, Save, Sparkles, Wand2 } from 'lucide-react'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { CATEGORIES } from '../lib/constants'
import { toInputValue } from '../lib/format'
import { Button, Card, Field, PageHeader, PageLoader } from '../components/ui'

const EMPTY = {
  title: '', description: '', category: 'CONFERENCE', venue: '',
  startDateTime: '', endDateTime: '', registrationDeadline: '', capacity: 100,
}

export default function EventForm() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const toast = useToast()
  const [form, setForm] = useState(EMPTY)
  const [loading, setLoading] = useState(editing)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})
  const [ai, setAi] = useState({ open: false, audience: '', highlights: '', tone: 'professional and inviting', loading: false, source: null })

  useEffect(() => {
    if (!editing) return
    api.get(`/events/${id}`)
      .then(({ event }) => setForm({
        title: event.title, description: event.description || '', category: event.category, venue: event.venue,
        startDateTime: toInputValue(event.startDateTime), endDateTime: toInputValue(event.endDateTime),
        registrationDeadline: toInputValue(event.registrationDeadline), capacity: event.capacity,
      }))
      .catch((e) => { toast.error(e.message); navigate('/events') })
      .finally(() => setLoading(false))
  }, [id])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  // Convenience: when the start is picked first, suggest an end 8h later and a deadline the day before
  const onStartChange = (e) => {
    const value = e.target.value
    setForm((f) => {
      const next = { ...f, startDateTime: value }
      if (value && !f.endDateTime) {
        const d = new Date(value); d.setHours(d.getHours() + 8)
        next.endDateTime = localInput(d)
      }
      if (value && !f.registrationDeadline) {
        const d = new Date(value); d.setDate(d.getDate() - 1)
        next.registrationDeadline = localInput(d)
      }
      return next
    })
  }

  const generate = async () => {
    if (!form.title.trim()) {
      toast.info('Give the event a title first; the AI uses it as the starting point.')
      return
    }
    setAi((a) => ({ ...a, loading: true }))
    try {
      const res = await api.post('/ai/event-description', {
        title: form.title, category: form.category, venue: form.venue,
        audience: ai.audience, highlights: ai.highlights, tone: ai.tone,
      })
      setForm((f) => ({ ...f, description: res.text }))
      setAi((a) => ({ ...a, source: res.source }))
      toast.success(res.source === 'GEMINI' ? 'Description written by Gemini' : 'Description generated (built-in writer, Gemini not configured)')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setAi((a) => ({ ...a, loading: false }))
    }
  }

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    const payload = { ...form, capacity: Number(form.capacity) }
    try {
      const saved = editing ? await api.put(`/events/${id}`, payload) : await api.post('/events', payload)
      toast.success(editing ? 'Event updated' : 'Draft created. Now add sessions to the agenda, then publish.')
      navigate(`/events/${saved.id}${editing ? '' : '?tab=agenda'}`)
    } catch (err) {
      setErrors(err.fieldErrors || {})
      toast.error(err.message, 'Could not save')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <PageLoader />

  return (
    <>
      <button onClick={() => navigate(-1)} className="mb-4 flex items-center gap-1.5 text-sm font-semibold text-slate-500 hover:text-brand-600">
        <ArrowLeft className="size-4" /> Back
      </button>
      <PageHeader eyebrow={editing ? 'Edit event' : 'New event'} title={editing ? form.title || 'Edit event' : 'Create an event'}
        subtitle={editing ? 'Changes to capacity automatically promote people from the waitlist.' : 'New events start as a draft, invisible to participants until you publish.'} />

      <form onSubmit={submit} className="grid gap-6 lg:grid-cols-3">
        <Card className="space-y-5 p-6 lg:col-span-2">
          <Field label="Title" error={errors.title}>
            <input className="input text-base font-semibold" required value={form.title} onChange={set('title')} placeholder="e.g. Campus Hackathon 2026" />
          </Field>

          <div>
            <span className="label">Category</span>
            <div className="flex flex-wrap gap-2">
              {CATEGORIES.map((c) => (
                <button type="button" key={c.value} onClick={() => setForm((f) => ({ ...f, category: c.value }))}
                  className={`flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-semibold transition ${
                    form.category === c.value ? 'bg-brand-gradient text-white shadow-lg shadow-brand-500/25' : 'glass text-slate-600 hover:text-brand-600 dark:text-slate-300'}`}>
                  <c.icon className="size-3.5" /> {c.label}
                </button>
              ))}
            </div>
          </div>

          <Field label="Venue" error={errors.venue}>
            <input className="input" required value={form.venue} onChange={set('venue')} placeholder="Auditorium, address or 'Online (Zoom)'" />
          </Field>

          <div>
            <div className="mb-1.5 flex items-center justify-between">
              <span className="label mb-0">Description</span>
              <button type="button" onClick={() => setAi((a) => ({ ...a, open: !a.open }))}
                className="flex items-center gap-1.5 rounded-lg bg-gradient-to-r from-brand-500/10 to-fuchsia-500/10 px-2.5 py-1 text-xs font-semibold text-brand-600 ring-1 ring-brand-500/20 transition hover:ring-brand-500/40 dark:text-brand-300">
                <Sparkles className="size-3.5" /> Write with AI
              </button>
            </div>
            <AnimatePresence>
              {ai.open && (
                <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}
                  className="overflow-hidden">
                  <div className="mb-3 space-y-3 rounded-2xl border border-brand-500/20 bg-gradient-to-br from-brand-500/5 to-fuchsia-500/5 p-4">
                    <div className="grid gap-3 sm:grid-cols-2">
                      <Field label="Target audience">
                        <input className="input" value={ai.audience} onChange={(e) => setAi({ ...ai, audience: e.target.value })} placeholder="Final-year CS students" />
                      </Field>
                      <Field label="Tone">
                        <select className="input" value={ai.tone} onChange={(e) => setAi({ ...ai, tone: e.target.value })}>
                          <option>professional and inviting</option>
                          <option>energetic and fun</option>
                          <option>formal and executive</option>
                          <option>friendly and community-focused</option>
                        </select>
                      </Field>
                    </div>
                    <Field label="Key highlights (comma separated)">
                      <input className="input" value={ai.highlights} onChange={(e) => setAi({ ...ai, highlights: e.target.value })}
                        placeholder="industry mentors, 36-hour build, cash prizes" />
                    </Field>
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {ai.source === 'FALLBACK' ? 'Used the built-in writer (add GEMINI_API_KEY for Gemini).' : 'Powered by Google Gemini.'}
                      </p>
                      <Button type="button" size="sm" icon={Wand2} loading={ai.loading} onClick={generate}>Generate</Button>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
            <textarea className="input min-h-44 leading-relaxed" value={form.description} onChange={set('description')}
              placeholder="What is this event about? Who should attend?" maxLength={5000} />
            {errors.description && <span className="mt-1 block text-xs text-rose-500">{errors.description}</span>}
          </div>
        </Card>

        <div className="space-y-6">
          <Card className="space-y-4 p-6">
            <h2 className="font-bold">Schedule</h2>
            <Field label="Starts" error={errors.startDateTime}>
              <input type="datetime-local" className="input" required value={form.startDateTime} onChange={onStartChange} />
            </Field>
            <Field label="Ends" error={errors.endDateTime}>
              <input type="datetime-local" className="input" required value={form.endDateTime} onChange={set('endDateTime')} />
            </Field>
            <Field label="Registration closes" error={errors.registrationDeadline} hint="Must be on or before the start">
              <input type="datetime-local" className="input" required value={form.registrationDeadline} onChange={set('registrationDeadline')} />
            </Field>
          </Card>
          <Card className="space-y-4 p-6">
            <h2 className="font-bold">Capacity</h2>
            <Field label="Seats" error={errors.capacity} hint="When full, new registrations join the waitlist automatically.">
              <input type="number" min={1} className="input" required value={form.capacity} onChange={set('capacity')} />
            </Field>
          </Card>
          <Button type="submit" size="lg" icon={Save} loading={saving} className="w-full">
            {editing ? 'Save changes' : 'Create draft'}
          </Button>
        </div>
      </form>
    </>
  )
}

function localInput(d) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
