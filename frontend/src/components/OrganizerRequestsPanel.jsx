import { useEffect, useState } from 'react'
import { BadgeCheck, Check, Inbox, X } from 'lucide-react'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'
import { fmtDateTime } from '../lib/format'
import { Avatar, Button, Card, Field, Modal } from './ui'

/** Admin view: pending organizer requests with approve / reject. */
export default function OrganizerRequestsPanel({ onDecided }) {
  const toast = useToast()
  const [requests, setRequests] = useState(null)
  const [busy, setBusy] = useState(null)
  const [rejecting, setRejecting] = useState(null)
  const [note, setNote] = useState('')

  const load = () => api.get('/admin/organizer-requests?status=PENDING').then(setRequests).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const decide = async (req, action, adminNote) => {
    setBusy(req.id)
    try {
      await api.post(`/admin/organizer-requests/${req.id}/${action}`, { note: adminNote || null })
      toast.success(action === 'approve' ? `${req.userName} is now an organizer` : `Request from ${req.userName} rejected`)
      setRejecting(null)
      setNote('')
      await load()
      onDecided?.()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusy(null)
    }
  }

  if (!requests) return null

  return (
    <Card className="mb-6 p-6">
      <div className="mb-4 flex items-center gap-2">
        <BadgeCheck className="size-5 text-brand-500" />
        <h2 className="font-bold">Organizer requests</h2>
        {requests.length > 0 && (
          <span className="rounded-full bg-amber-500/15 px-2 py-0.5 text-xs font-bold text-amber-600 dark:text-amber-300">{requests.length} pending</span>
        )}
      </div>
      {requests.length === 0 ? (
        <p className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400"><Inbox className="size-4" /> No pending requests.</p>
      ) : (
        <ul className="divide-y divide-slate-900/5 dark:divide-white/5">
          {requests.map((r) => (
            <li key={r.id} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-start">
              <Avatar name={r.userName} />
              <div className="min-w-0 flex-1">
                <p className="font-semibold">{r.userName} <span className="text-xs font-normal text-slate-500">· {r.userEmail}</span></p>
                <p className="text-xs text-slate-500 dark:text-slate-400">{r.organization ? `${r.organization} · ` : ''}asked {fmtDateTime(r.createdAt)}</p>
                <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">“{r.reason}”</p>
              </div>
              <div className="flex gap-2">
                <Button size="sm" variant="success" icon={Check} loading={busy === r.id} onClick={() => decide(r, 'approve')}>Approve</Button>
                <Button size="sm" variant="danger" icon={X} disabled={busy === r.id} onClick={() => { setRejecting(r); setNote('') }}>Reject</Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Modal open={Boolean(rejecting)} onClose={() => setRejecting(null)} title="Reject request?"
        subtitle={rejecting ? `${rejecting.userName} will be told by email and can apply again.` : ''}
        footer={<>
          <Button variant="ghost" onClick={() => setRejecting(null)}>Cancel</Button>
          <Button variant="danger" loading={busy === rejecting?.id} onClick={() => decide(rejecting, 'reject', note)}>Reject</Button>
        </>}>
        <Field label="Note to the applicant (optional)" hint="e.g. what extra details you need">
          <textarea className="input min-h-24" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </Modal>
    </Card>
  )
}
