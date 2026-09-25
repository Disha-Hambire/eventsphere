import { useEffect, useState } from 'react'
import { motion } from 'motion/react'
import { Building2, Mic2, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { Avatar, Button, Card, ConfirmDialog, EmptyState, Field, Modal, PageHeader, PageLoader } from '../components/ui'

const EMPTY = { fullName: '', email: '', organization: '', designation: '', expertise: '', bio: '' }

export default function Speakers() {
  const toast = useToast()
  const [speakers, setSpeakers] = useState(null)
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState(null) // null | {} (new) | speaker
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})
  const [deleting, setDeleting] = useState(null)

  const load = () => api.get('/speakers').then(setSpeakers).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, [])

  const open = (s) => {
    setEditing(s || {})
    setErrors({})
    setForm(s ? { ...EMPTY, ...Object.fromEntries(Object.entries(s).map(([k, v]) => [k, v ?? ''])) } : EMPTY)
  }

  const save = async (e) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      if (editing.id) await api.put(`/speakers/${editing.id}`, form)
      else await api.post('/speakers', form)
      toast.success(editing.id ? 'Speaker updated' : 'Speaker added')
      setEditing(null)
      load()
    } catch (err) {
      setErrors(err.fieldErrors || {})
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    setSaving(true)
    try {
      await api.del(`/speakers/${deleting.id}`)
      toast.success('Speaker removed')
      setDeleting(null)
      load()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  if (!speakers) return <PageLoader />
  const q = query.trim().toLowerCase()
  const visible = speakers.filter((s) => !q || `${s.fullName} ${s.organization} ${s.expertise}`.toLowerCase().includes(q))
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <>
      <PageHeader eyebrow="Directory" title="Speakers" subtitle="Shared across all organizers. Speakers are checked for double-booking automatically."
        actions={<Button icon={Plus} onClick={() => open(null)}>Add speaker</Button>} />

      <div className="relative mb-6 max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
        <input className="input pl-9" placeholder="Search name, company, expertise" value={query} onChange={(e) => setQuery(e.target.value)} />
      </div>

      {visible.length === 0 ? (
        <EmptyState icon={Mic2} title="No speakers" text="Add speakers to assign them to sessions." action={<Button icon={Plus} onClick={() => open(null)}>Add speaker</Button>} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {visible.map((s, i) => (
            <motion.div key={s.id} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.04 }}>
              <Card hover className="flex h-full flex-col p-5">
                <div className="flex items-start gap-3">
                  <Avatar name={s.fullName} className="size-12 text-sm" />
                  <div className="min-w-0">
                    <p className="font-bold">{s.fullName}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">{s.designation}</p>
                    {s.organization && <p className="mt-0.5 flex items-center gap-1 text-xs text-slate-500 dark:text-slate-400"><Building2 className="size-3" />{s.organization}</p>}
                  </div>
                </div>
                {s.bio && <p className="mt-3 line-clamp-3 text-sm text-slate-600 dark:text-slate-300">{s.bio}</p>}
                {s.expertise && (
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {s.expertise.split(',').map((t) => (
                      <span key={t} className="rounded-full bg-brand-500/10 px-2 py-0.5 text-[11px] font-semibold text-brand-600 dark:text-brand-300">{t.trim()}</span>
                    ))}
                  </div>
                )}
                <div className="mt-auto flex items-center justify-between pt-4">
                  <span className="text-xs text-slate-500">{s.sessionCount} session{s.sessionCount === 1 ? '' : 's'}</span>
                  <div className="flex gap-1">
                    <Button size="sm" variant="ghost" icon={Pencil} onClick={() => open(s)}>Edit</Button>
                    <Button size="sm" variant="ghost" icon={Trash2} className="text-rose-500" onClick={() => setDeleting(s)} aria-label="Delete" />
                  </div>
                </div>
              </Card>
            </motion.div>
          ))}
        </div>
      )}

      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing?.id ? 'Edit speaker' : 'Add speaker'}>
        <form onSubmit={save} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Full name" error={errors.fullName}><input className="input" required value={form.fullName} onChange={set('fullName')} /></Field>
            <Field label="Email" error={errors.email}><input className="input" type="email" required value={form.email} onChange={set('email')} /></Field>
            <Field label="Organization"><input className="input" value={form.organization} onChange={set('organization')} /></Field>
            <Field label="Designation"><input className="input" value={form.designation} onChange={set('designation')} /></Field>
          </div>
          <Field label="Expertise" hint="Comma separated, e.g. AI, Cloud, Design"><input className="input" value={form.expertise} onChange={set('expertise')} /></Field>
          <Field label="Bio"><textarea className="input min-h-24" value={form.bio} onChange={set('bio')} /></Field>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={() => setEditing(null)}>Cancel</Button>
            <Button type="submit" loading={saving}>Save speaker</Button>
          </div>
        </form>
      </Modal>
      <ConfirmDialog open={Boolean(deleting)} danger loading={saving} title="Remove speaker?"
        text={`${deleting?.fullName} will be removed from the directory. Speakers assigned to sessions cannot be removed.`}
        confirmLabel="Remove" onConfirm={remove} onClose={() => setDeleting(null)} />
    </>
  )
}
