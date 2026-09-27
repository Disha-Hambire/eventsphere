// Demo shortcuts (one-click demo logins, "Try the demo") appear in local development only,
// or when a build sets VITE_SHOW_DEMO=true. The live site never shows public demo passwords.
export const SHOW_DEMO = import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO === 'true'
