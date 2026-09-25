import { useEffect, useState } from 'react'
import { CalendarPlus, Clock, DoorOpen, Mic2, Pencil, Trash2, Users } from 'lucide-react'
import { api } from '../../lib/api'
import { useToast } from '../../context/ToastContext'
import { fmtDay, fmtTime, toInputValue } from '../../lib/format'
import { Avatar, Button, Card, ConfirmDialog, EmptyState, Field, Modal } from '../ui'

const EMPTY = { title: '', description: '', speakerId: '', room: '', startTime: '', endTime: '' }

function SessionModal({ open, onClose, event, session, onSaved }) {
  const toast = useToast()
  const [speakers, setSpeakers] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open) return
    setError('')
    api.get('/speakers').then(setSpeakers).catch(() => setSpeakers([]))
    setForm(session ? {
      title: session.title, description: session.description || '', speakerId: session.speakerId || '',
      room: session.room || '', startTime: toInputValue(session.startTime), endTime: toInputValue(session.endTime),
    } : { ...EMPTY, startTime: toInputValue(event.startDateTime), endTime: '' })
  }, [open, session, event])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const save = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    const payload = { ...form, speakerId: form.speakerId ? Number(form.speakerId) : null }
    try {
      if (session) await api.put(`/sessions/${session.id}`, payload)
      else await api.post(`/events/${event.id}/sessions`, payload)
      toast.success(session ? 'Session updated' : 'Session added to the agenda')
      onSaved()
      onClose()
    } catch (err) {
      setError(err.message) // business-rule messages (speaker/room clash etc.) are shown inline
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={session ? 'Edit session' : 'Add session'}
      subtitle="Speaker and room clashes are checked automatically.">
      <form onSubmit={save} className="space-y-4">
        <Field label="Title">
          <input className="input" required value={form.title} onChange={set('title')} placeholder="Keynote: The future of AI" />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Speaker">
            <select className="input" value={form.speakerId} onChange={set('speakerId')}>
              <option value="">No speaker</option>
              {speakers.map((s) => <option key={s.id} value={s.id}>{s.fullName}{s.organization ? ` · ${s.organization}` : ''}</option>)}
            </select>
          </Field>
          <Field label="Room">
            <input className="input" value={form.room} onChange={set('room')} placeholder="Hall A" />
          </Field>
          <Field label="Starts">
            <input type="datetime-local" className="input" required value={form.startTime} onChange={set('startTime')} />
          </Field>
          <Field label="Ends">
            <input type="datetime-local" className="input" required value={form.endTime} onChange={set('endTime')} />
          </Field>
        </div>
        <Field label="Description (optional)">
          <textarea className="input min-h-20" value={form.description} onChange={set('description')} />
        </Field>
        {error && <p className="rounded-xl bg-rose-500/10 px-3 py-2 text-sm font-medium text-rose-600 dark:text-rose-300">{error}</p>}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>Cancel</Button>
          <Button type="submit" loading={saving}>{session ? 'Save session' : 'Add session'}</Button>
        </div>
      </form>
    </Modal>
  )
}

/** Agenda grouped by day. Organizers can add/edit/delete while the event is not closed. */
export default function AgendaTab({ event, sessions, editable, manager, onChanged }) {
  const toast = useToast()
  const [modal, setModal] = useState({ open: false, session: null })
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)

  const byDay = sessions.reduce((acc, s) => {
    const day = fmtDay(s.startTime)
    ;(acc[day] ||= []).push(s)
    return acc
  }, {})

  const remove = async () => {
    setBusy(true)
    try {
      await api.del(`/sessions/${deleting.id}`)
      toast.success('Session removed')
      onChanged()
      setDeleting(null)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      {editable && (
        <div className="mb-4 flex justify-end">
          <Button icon={CalendarPlus} onClick={() => setModal({ open: true, session: null })}>Add session</Button>
        </div>
      )}
      {sessions.length === 0 ? (
        <EmptyState icon={Clock} title="No sessions yet"
          text={editable ? 'An event needs at least one session before it can be published.' : 'The agenda will be announced soon.'}
          action={editable && <Button icon={CalendarPlus} onClick={() => setModal({ open: true, session: null })}>Add the first session</Button>} />
      ) : (
        <div className="space-y-8">
          {Object.entries(byDay).map(([day, list]) => (
            <div key={day}>
              <h3 className="mb-3 text-xs font-bold uppercase tracking-widest text-brand-500">{day}</h3>
              <div className="space-y-3">
                {list.map((s) => (
                  <Card key={s.id} className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
                    <div className="w-28 shrink-0">
                      <p className="text-sm font-bold">{fmtTime(s.startTime)}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">to {fmtTime(s.endTime)}</p>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold">{s.title}</p>
                      {s.description && <p className="mt-0.5 line-clamp-2 text-sm text-slate-500 dark:text-slate-400">{s.description}</p>}
                      <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
                        {s.speakerName && (
                          <span className="flex items-center gap-1.5">
                            <Avatar name={s.speakerName} className="size-5 text-[9px]" />
                            <span className="font-semibold text-slate-700 dark:text-slate-200">{s.speakerName}</span>
                            {s.speakerDesignation && <span>· {s.speakerDesignation}</span>}
                          </span>
                        )}
                        {!s.speakerName && <span className="flex items-center gap-1.5"><Mic2 className="size-3.5" /> No speaker</span>}
                        {s.room && <span className="flex items-center gap-1.5"><DoorOpen className="size-3.5" />{s.room}</span>}
                        {manager && s.attendanceCount > 0 && (
                          <span className="flex items-center gap-1.5"><Users className="size-3.5" />{s.attendanceCount} checked in</span>
                        )}
                      </div>
                    </div>
                    {editable && (
                      <div className="flex gap-1">
                        <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setModal({ open: true, session: s })}>Edit</Button>
                        <Button size="sm" variant="ghost" icon={Trash2} className="text-rose-500" onClick={() => setDeleting(s)} aria-label="Delete" />
                      </div>
                    )}
                  </Card>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
      <SessionModal open={modal.open} session={modal.session} event={event}
        onClose={() => setModal({ open: false, session: null })} onSaved={onChanged} />
      <ConfirmDialog open={Boolean(deleting)} danger loading={busy} title="Delete session?"
        text={`"${deleting?.title}" will be removed from the agenda.`} confirmLabel="Delete"
        onConfirm={remove} onClose={() => setDeleting(null)} />
    </>
  )
}
