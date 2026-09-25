import { Briefcase, GraduationCap, Mic2, Music, Presentation, Trophy, Users, Wrench, Video } from 'lucide-react'

export const CATEGORIES = [
  { value: 'CONFERENCE', label: 'Conference', icon: Presentation, gradient: 'from-indigo-500 to-violet-500' },
  { value: 'CORPORATE', label: 'Corporate', icon: Briefcase, gradient: 'from-slate-600 to-indigo-600' },
  { value: 'COLLEGE', label: 'College', icon: GraduationCap, gradient: 'from-sky-500 to-indigo-500' },
  { value: 'WORKSHOP', label: 'Workshop', icon: Wrench, gradient: 'from-emerald-500 to-teal-500' },
  { value: 'MEETUP', label: 'Meetup', icon: Users, gradient: 'from-amber-500 to-orange-500' },
  { value: 'WEBINAR', label: 'Webinar', icon: Video, gradient: 'from-cyan-500 to-blue-500' },
  { value: 'CULTURAL', label: 'Cultural', icon: Music, gradient: 'from-pink-500 to-rose-500' },
  { value: 'SPORTS', label: 'Sports', icon: Trophy, gradient: 'from-lime-500 to-emerald-500' },
]

export const categoryMeta = (value) =>
  CATEGORIES.find((c) => c.value === value) || { value, label: value, icon: Mic2, gradient: 'from-indigo-500 to-violet-500' }

/** Colours for event phases and registration statuses. */
export const STATUS_STYLES = {
  DRAFT: 'bg-slate-500/10 text-slate-600 ring-slate-500/20 dark:text-slate-300',
  UPCOMING: 'bg-sky-500/10 text-sky-700 ring-sky-500/20 dark:text-sky-300',
  PUBLISHED: 'bg-sky-500/10 text-sky-700 ring-sky-500/20 dark:text-sky-300',
  LIVE: 'bg-emerald-500/10 text-emerald-700 ring-emerald-500/25 dark:text-emerald-300',
  ENDED: 'bg-amber-500/10 text-amber-700 ring-amber-500/25 dark:text-amber-300',
  COMPLETED: 'bg-violet-500/10 text-violet-700 ring-violet-500/20 dark:text-violet-300',
  CANCELLED: 'bg-rose-500/10 text-rose-700 ring-rose-500/20 dark:text-rose-300',
  CONFIRMED: 'bg-emerald-500/10 text-emerald-700 ring-emerald-500/25 dark:text-emerald-300',
  WAITLISTED: 'bg-amber-500/10 text-amber-700 ring-amber-500/25 dark:text-amber-300',
  ADMIN: 'bg-fuchsia-500/10 text-fuchsia-700 ring-fuchsia-500/20 dark:text-fuchsia-300',
  ORGANIZER: 'bg-indigo-500/10 text-indigo-700 ring-indigo-500/20 dark:text-indigo-300',
  PARTICIPANT: 'bg-slate-500/10 text-slate-600 ring-slate-500/20 dark:text-slate-300',
}

export const PHASE_LABEL = {
  DRAFT: 'Draft',
  UPCOMING: 'Upcoming',
  LIVE: 'Live now',
  ENDED: 'Ended',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  PUBLISHED: 'Published',
  CONFIRMED: 'Confirmed',
  WAITLISTED: 'Waitlisted',
  ADMIN: 'Admin',
  ORGANIZER: 'Organizer',
  PARTICIPANT: 'Participant',
}

export const CHART_COLORS = ['#6366f1', '#8b5cf6', '#d946ef', '#06b6d4', '#10b981', '#f59e0b', '#f43f5e', '#64748b']
