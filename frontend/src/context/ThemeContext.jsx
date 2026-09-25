import { createContext, useCallback, useContext, useState } from 'react'

const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'))

  const toggle = useCallback(() => {
    setDark((d) => {
      const next = !d
      document.documentElement.classList.toggle('dark', next)
      try { localStorage.setItem('es_theme', next ? 'dark' : 'light') } catch { /* ignore */ }
      return next
    })
  }, [])

  return <ThemeContext.Provider value={{ dark, toggle }}>{children}</ThemeContext.Provider>
}

export const useTheme = () => useContext(ThemeContext)
