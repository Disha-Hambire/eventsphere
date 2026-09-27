import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from '../lib/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(tokenStore.get()))

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) return
    api.get('/auth/me')
      .then(setUser)
      .catch(logout)
      .finally(() => setLoading(false))
  }, [logout])

  const acceptAuth = useCallback(({ token, user: u }) => {
    tokenStore.set(token)
    setUser(u)
    return u
  }, [])

  const login = useCallback((email, password) =>
    api.post('/auth/login', { email, password }).then(acceptAuth), [acceptAuth])

  const register = useCallback((form) => api.post('/auth/register', form).then(acceptAuth), [acceptAuth])

  // Re-read the user (e.g. after an admin approves an organizer request, the role changes)
  const refreshUser = useCallback(() => api.get('/auth/me').then((u) => { setUser(u); return u }), [])

  const value = useMemo(() => ({
    user,
    loading,
    login,
    register,
    logout,
    refreshUser,
    isAdmin: user?.role === 'ADMIN',
    isOrganizer: user?.role === 'ORGANIZER',
    isParticipant: user?.role === 'PARTICIPANT',
    canManage: user?.role === 'ADMIN' || user?.role === 'ORGANIZER',
  }), [user, loading, login, register, logout, refreshUser])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
