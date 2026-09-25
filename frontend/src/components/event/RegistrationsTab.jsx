import { useEffect, useMemo, useState } from 'react'
import { ArrowUpCircle, Download, Search, Users } from 'lucide-react'
import { api } from '../../lib/api'
import { useToast } from '../../context/ToastContext'
import { fmtDateTime } from '../../lib/format'
import Timeline from '../Timeline'
import { Avatar, Badge, Button, Card, EmptyState, Modal, Spinner, Tabs } from '../ui'

export default function RegistrationsTab({ event }) {
  const toast = useToast()
  const [rows, setRows] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(null)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    api.get(`/events/${event.id}/registrations`).then(setRows).catch((e) => toast.error(e.message))
  }, [event.id])

  const counts = useMemo(() => ({
    ALL: rows?.length ?? 0,
    CONFIRMED: rows?.filter((r) => r.status === 'CONFIRMED').length ?? 0,
    WAITLISTED: rows?.filter((r) => r.status === 'WAITLISTED').length ?? 0,
    CANCELLED: rows?.filter((r) => r.status === 'CANCELLED').length ?? 0,
  }), [rows])

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (rows || [])
      .filter((r) => filter === 'ALL' || r.status === filter)
      .filter((r) => !q || `${r.participantName} ${r.participantEmail} ${r.ticketCode || ''}`.toLowerCase().includes(q))
      .sort((a, b) => (a.status === 'WAITLISTED' && b.status === 'WAITLISTED' ? a.waitlistPosition - b.waitlistPosition : 0))
  }, [rows, filter, query])

  const exportCsv = async () => {
    setExporting(true)
    try {
      await api.download(`/events/${event.id}/registrations/export`, `registrations-${event.id}.csv`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setExporting(false)
    }
  }

  if (!rows) return <div className="grid place-items-center py-20"><Spinner /></div>

  return (
    <>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <Tabs value={filter} onChange={setFilter} tabs={[
          { value: 'ALL', label: 'All', count: counts.ALL },
          { value: 'CONFIRMED', label: 'Confirmed', count: counts.CONFIRMED },
          { value: 'WAITLISTED', label: 'Waitlist', count: counts.WAITLISTED },
          { value: 'CANCELLED', label: 'Cancelled', count: counts.CANCELLED },
        ]} />
        <div className="mb-6 flex gap-2">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <input className="input pl-9 lg:w-60" placeholder="Name, email or ticket" value={query} onChange={(e) => setQuery(e.target.value)} />
          </div>
          <Button variant="secondary" icon={Download} loading={exporting} onClick={exportCsv}>CSV</Button>
        </div>
      </div>

      {visible.length === 0 ? (
        <EmptyState icon={Users} title="No registrations" text="Registrations appear here as soon as participants sign up." />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-900/5 text-xs uppercase tracking-wide text-slate-500 dark:border-white/5 dark:text-slate-400">
                <tr>
                  <th className="px-5 py-3 font-semibold">Participant</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Ticket</th>
                  <th className="px-5 py-3 font-semibold">Registered</th>
                  <th className="px-5 py-3 text-center font-semibold">Sessions</th>
                  <th className="px-5 py-3 text-center font-semibold">Feedback</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-900/5 dark:divide-white/5">
                {visible.map((r) => (
                  <tr key={r.id} onClick={() => setSelected(r)} className="cursor-pointer transition hover:bg-brand-500/[0.04]">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar name={r.participantName} className="size-8 text-[10px]" />
                        <div className="min-w-0">
                          <p className="truncate font-semibold">{r.participantName}</p>
                          <p className="truncate text-xs text-slate-500 dark:text-slate-400">{r.participantEmail}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1.5">
                        <Badge status={r.status}>
                          {r.status === 'WAITLISTED' ? `Waitlist #${r.waitlistPosition}` : undefined}
                        </Badge>
                        {r.promotedFromWaitlist && <ArrowUpCircle className="size-4 text-brand-500" title="Promoted from waitlist" />}
                      </div>
                    </td>
                    <td className="px-5 py-3 font-mono text-xs">{r.ticketCode || '-'}</td>
                    <td className="whitespace-nowrap px-5 py-3 text-xs text-slate-500 dark:text-slate-400">{fmtDateTime(r.registeredAt)}</td>
                    <td className="px-5 py-3 text-center font-semibold">{r.sessionsAttended}</td>
                    <td className="px-5 py-3 text-center">{r.feedbackGiven ? '✓' : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={Boolean(selected)} onClose={() => setSelected(null)} title={selected?.participantName}
        subtitle={selected?.participantEmail}>
        {selected && (
          <>
            <div className="mb-5 flex flex-wrap gap-2">
              <Badge status={selected.status} />
              {selected.ticketCode && <span className="rounded-full bg-slate-900/5 px-2.5 py-0.5 font-mono text-xs dark:bg-white/10">{selected.ticketCode}</span>}
            </div>
            <Timeline entries={selected.timeline} />
          </>
        )}
      </Modal>
    </>
  )
}
