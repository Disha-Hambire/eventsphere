import { createContext, useCallback, useContext, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { AlertTriangle, CheckCircle2, Info, X } from 'lucide-react'

const ToastContext = createContext(null)

const STYLES = {
  success: { icon: CheckCircle2, cls: 'text-emerald-500' },
  error: { icon: AlertTriangle, cls: 'text-rose-500' },
  info: { icon: Info, cls: 'text-brand-500' },
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), [])

  const push = useCallback((type, message, title) => {
    const id = Math.random().toString(36).slice(2)
    setToasts((t) => [...t.slice(-3), { id, type, message, title }])
    setTimeout(() => dismiss(id), type === 'error' ? 6500 : 4500)
  }, [dismiss])

  const toast = {
    success: (m, t) => push('success', m, t),
    error: (m, t) => push('error', m, t),
    info: (m, t) => push('info', m, t),
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-4 z-[100] flex flex-col items-center gap-2 px-4 sm:bottom-6 sm:right-6 sm:left-auto sm:items-end">
        <AnimatePresence>
          {toasts.map((t) => {
            const { icon: Icon, cls } = STYLES[t.type]
            return (
              <motion.div
                key={t.id}
                layout
                initial={{ opacity: 0, y: 24, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, x: 40, scale: 0.96 }}
                transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                className="glass-strong pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-2xl p-4 shadow-xl"
                role="status"
              >
                <Icon className={`mt-0.5 size-5 shrink-0 ${cls}`} />
                <div className="min-w-0 flex-1 text-sm">
                  {t.title && <p className="font-semibold">{t.title}</p>}
                  <p className="text-slate-600 dark:text-slate-300">{t.message}</p>
                </div>
                <button onClick={() => dismiss(t.id)} className="text-slate-400 hover:text-slate-600" aria-label="Dismiss">
                  <X className="size-4" />
                </button>
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
