import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import AppShell from './components/AppShell'
import { Spinner } from './components/ui'
import Landing from './pages/Landing'
import { ForgotPassword, Login, Register } from './pages/Auth'
import Dashboard from './pages/Dashboard'
import Events from './pages/Events'
import EventForm from './pages/EventForm'
import EventDetail from './pages/EventDetail'
import MyTickets from './pages/MyTickets'
import CheckIn from './pages/CheckIn'
import Speakers from './pages/Speakers'
import Users from './pages/Users'
import NotFound from './pages/NotFound'
import { homeFor } from './lib/nav'

function RequireAuth({ roles, children }) {
  const { user, loading } = useAuth()
  if (loading) {
    return <div className="grid min-h-screen place-items-center"><Spinner className="size-8" /></div>
  }
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user)} replace />
  return children
}

const MANAGERS = ['ADMIN', 'ORGANIZER']

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />

      <Route element={<RequireAuth><AppShell /></RequireAuth>}>
        <Route path="/dashboard" element={<RequireAuth roles={MANAGERS}><Dashboard /></RequireAuth>} />
        <Route path="/events" element={<Events />} />
        <Route path="/events/new" element={<RequireAuth roles={MANAGERS}><EventForm /></RequireAuth>} />
        <Route path="/events/:id" element={<EventDetail />} />
        <Route path="/events/:id/edit" element={<RequireAuth roles={MANAGERS}><EventForm /></RequireAuth>} />
        <Route path="/tickets" element={<RequireAuth roles={['PARTICIPANT']}><MyTickets /></RequireAuth>} />
        <Route path="/check-in" element={<RequireAuth roles={MANAGERS}><CheckIn /></RequireAuth>} />
        <Route path="/speakers" element={<RequireAuth roles={MANAGERS}><Speakers /></RequireAuth>} />
        <Route path="/users" element={<RequireAuth roles={['ADMIN']}><Users /></RequireAuth>} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
