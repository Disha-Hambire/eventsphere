import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarPlus, CalendarX2, Search } from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { CATEGORIES } from '../lib/constants'
import EventCard from '../components/EventCard'
import { Button, EmptyState, PageHeader, Tabs } from '../components/ui'

const MANAGER_TABS = [
  { value: 'ALL', label: 'All' },
  { value: 'DRAFT', label: 'Drafts' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]
const PARTICIPANT_TABS = [
  { value: 'OPEN', label: 'Upcoming & live' },
  { value: 'COMPLETED', label: 'Past events' },
]

/** Which tab an event belongs to. Participants see "past" = completed or already ended. */
function matchesTab(e, tab, canManage) {
  if (tab === 'ALL') return true
  if (tab === 'OPEN') return e.status === 'PUBLISHED' && e.phase !== 'ENDED'
  if (tab === 'COMPLETED' && !canManage) return e.status === 'COMPLETED' || e.phase === 'ENDED'
  return e.status === tab
}

export default function Events() {
  const { canManage, isAdmin } = useAuth()
  const toast = useToast()
  const [events, setEvents] = useState(null)
  const [tab, setTab] = useState(canManage ? 'ALL' : 'OPEN')
  const [category, setCategory] = useState('')
  const [query, setQuery] = useState('')

  useEffect(() => {
    api.get('/events').then(setEvents).catch((e) => toast.error(e.message))
  }, [])

  const filtered = useMemo(() => {
    if (!events) return []
    const q = query.trim().toLowerCase()
    return events.filter((e) => {
      if (category && e.category !== category) return false
      if (q && !`${e.title} ${e.venue}`.toLowerCase().includes(q)) return false
      return matchesTab(e, tab, canManage)
    })
  }, [events, tab, category, query, canManage])

  const tabs = (canManage ? MANAGER_TABS : PARTICIPANT_TABS).map((t) => ({
    ...t,
    count: events ? events.filter((e) => matchesTab(e, t.value, canManage)).length : null,
  }))

  return (
    <>
      <PageHeader
        eyebrow={canManage ? (isAdmin ? 'All events' : 'My events') : 'Discover'}
        title={canManage ? 'Events' : 'Find your next event'}
        subtitle={canManage ? 'Plan, publish and run your events.' : 'Conferences, workshops, meetups and more. Grab a seat before it fills up.'}
        actions={canManage && <Link to="/events/new"><Button icon={CalendarPlus}>Create event</Button></Link>}
      />

      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <Tabs tabs={tabs} value={tab} onChange={setTab} />
        <div className="mb-6 flex flex-col gap-2 sm:flex-row">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <input className="input pl-9 sm:w-64" placeholder="Search title or venue" value={query} onChange={(e) => setQuery(e.target.value)} />
          </div>
          <select className="input sm:w-44" value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All categories</option>
            {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
          </select>
        </div>
      </div>

      {!events ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2, 3, 4, 5].map((i) => <div key={i} className="skeleton h-72" />)}
        </div>
      ) : filtered.length ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((e, i) => <EventCard key={e.id} event={e} index={i} />)}
        </div>
      ) : (
        <EmptyState icon={CalendarX2} title="No events here"
          text={query || category ? 'Try a different search or category.' : canManage ? 'Create your first event to get started.' : 'Check back soon for new events.'}
          action={canManage && <Link to="/events/new"><Button icon={CalendarPlus}>Create event</Button></Link>} />
      )}
    </>
  )
}
