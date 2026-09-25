import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'
import { Backdrop } from '../components/Brand'
import { Button } from '../components/ui'

export default function NotFound() {
  return (
    <div className="grid min-h-screen place-items-center px-4 text-center">
      <Backdrop />
      <div>
        <p className="text-gradient text-8xl font-extrabold">404</p>
        <h1 className="mt-4 text-2xl font-bold">This page wandered off</h1>
        <p className="mt-2 text-slate-500 dark:text-slate-400">The link may be broken or the page may have moved.</p>
        <Link to="/" className="mt-6 inline-block"><Button icon={Compass}>Back to home</Button></Link>
      </div>
    </div>
  )
}
