import { useEffect, useMemo, useState } from 'react'
import { Search, ShieldCheck } from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { fmtDate } from '../lib/format'
import { Avatar, Badge, Card, PageHeader, PageLoader, StatCard } from '../components/ui'
import { CalendarCheck2, UserRound, Users as UsersIcon } from 'lucide-react'

export default function Users() {
  const { user: me } = useAuth()
  const toast = useToast()
  const [users, setUsers] = useState(null)
  const [query, setQuery] = useState('')
  const [pending, setPending] = useState(null)

  useEffect(() => {
    api.get('/admin/users').then(setUsers).catch((e) => toast.error(e.message))
  }, [])

  const replace = (u) => setUsers((list) => list.map((x) => (x.id === u.id ? u : x)))

  const changeRole = async (u, role) => {
    setPending(u.id)
    try {
      replace(await api.patch(`/admin/users/${u.id}/role`, { role }))
      toast.success(`${u.fullName} is now ${role.toLowerCase()}`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setPending(null)
    }
  }

  const toggleActive = async (u) => {
    setPending(u.id)
    try {
      replace(await api.patch(`/admin/users/${u.id}/status`, { active: !u.active }))
      toast.success(`${u.fullName} ${u.active ? 'deactivated' : 'reactivated'}`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setPending(null)
    }
  }

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (users || []).filter((u) => !q || `${u.fullName} ${u.email} ${u.organization || ''}`.toLowerCase().includes(q))
  }, [users, query])

  if (!users) return <PageLoader />
  const count = (r) => users.filter((u) => u.role === r).length

  return (
    <>
      <PageHeader eyebrow="Administration" title="Users & roles"
        subtitle="Participants sign up themselves; promote trusted people to organizer so they can run events." />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard icon={ShieldCheck} label="Admins" value={count('ADMIN')} tone="fuchsia" />
        <StatCard icon={CalendarCheck2} label="Organizers" value={count('ORGANIZER')} delay={0.05} />
        <StatCard icon={UserRound} label="Participants" value={count('PARTICIPANT')} tone="sky" delay={0.1} />
      </div>

      <div className="relative mb-4 max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
        <input className="input pl-9" placeholder="Search users" value={query} onChange={(e) => setQuery(e.target.value)} />
      </div>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-900/5 text-xs uppercase tracking-wide text-slate-500 dark:border-white/5 dark:text-slate-400">
              <tr>
                <th className="px-5 py-3 font-semibold">User</th>
                <th className="px-5 py-3 font-semibold">Role</th>
                <th className="px-5 py-3 font-semibold">Joined</th>
                <th className="px-5 py-3 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-900/5 dark:divide-white/5">
              {visible.map((u) => {
                const self = u.id === me.id
                return (
                  <tr key={u.id} className={u.active ? '' : 'opacity-60'}>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar name={u.fullName} className="size-8 text-[10px]" />
                        <div className="min-w-0">
                          <p className="truncate font-semibold">{u.fullName} {self && <span className="text-xs text-brand-500">(you)</span>}</p>
                          <p className="truncate text-xs text-slate-500 dark:text-slate-400">{u.email}{u.organization ? ` · ${u.organization}` : ''}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      {self ? <Badge status={u.role} /> : (
                        <select className="input h-9 w-36 py-1 text-xs" value={u.role} disabled={pending === u.id}
                          onChange={(e) => changeRole(u, e.target.value)}>
                          <option value="PARTICIPANT">Participant</option>
                          <option value="ORGANIZER">Organizer</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-5 py-3 text-xs text-slate-500">{fmtDate(u.createdAt)}</td>
                    <td className="px-5 py-3">
                      <button disabled={self || pending === u.id} onClick={() => toggleActive(u)}
                        className={`relative h-6 w-11 rounded-full transition disabled:opacity-40 ${u.active ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-700'}`}
                        aria-label={u.active ? 'Deactivate' : 'Activate'}>
                        <span className={`absolute top-0.5 size-5 rounded-full bg-white shadow transition-all ${u.active ? 'left-[22px]' : 'left-0.5'}`} />
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        {visible.length === 0 && <p className="flex items-center justify-center gap-2 py-10 text-sm text-slate-500"><UsersIcon className="size-4" /> No users match</p>}
      </Card>
    </>
  )
}
