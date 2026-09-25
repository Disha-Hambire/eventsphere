/** Where each role lands after logging in. */
export const homeFor = (user) => (user?.role === 'PARTICIPANT' ? '/events' : '/dashboard')
