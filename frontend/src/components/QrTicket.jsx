import { QRCodeSVG } from 'qrcode.react'
import { CalendarClock, MapPin } from 'lucide-react'
import { fmtRange } from '../lib/format'
import { Logo } from './Brand'

/** A printable ticket: the QR encodes only the ticket code; the server validates it at check-in. */
export default function QrTicket({ ticketCode, eventTitle, participantName, start, end, venue }) {
  return (
    <div className="relative mx-auto w-full max-w-sm overflow-hidden rounded-3xl bg-white text-slate-900 shadow-2xl shadow-brand-500/20 ring-1 ring-slate-200">
      <div className="bg-brand-gradient px-6 pb-8 pt-5 text-white">
        <div className="flex items-center gap-2 text-sm font-bold"><Logo className="size-6" /> EventSphere ticket</div>
        <h3 className="mt-3 text-lg font-extrabold leading-tight">{eventTitle}</h3>
        <p className="mt-1 text-sm text-white/80">{participantName}</p>
      </div>
      {/* perforation */}
      <div className="relative -mt-4 flex items-center">
        <span className="size-8 -translate-x-1/2 rounded-full bg-slate-100 dark:bg-slate-800" />
        <span className="h-px flex-1 border-t-2 border-dashed border-slate-200" />
        <span className="size-8 translate-x-1/2 rounded-full bg-slate-100 dark:bg-slate-800" />
      </div>
      <div className="flex flex-col items-center px-6 pb-6 pt-2">
        <div className="rounded-2xl bg-white p-3 ring-1 ring-slate-100">
          <QRCodeSVG value={ticketCode} size={176} level="M" fgColor="#1e1b4b" />
        </div>
        <p className="mt-3 font-mono text-lg font-bold tracking-[0.2em] text-indigo-950">{ticketCode}</p>
        <div className="mt-4 w-full space-y-1.5 text-xs text-slate-500">
          <p className="flex items-center gap-2"><CalendarClock className="size-3.5" />{fmtRange(start, end)}</p>
          <p className="flex items-center gap-2"><MapPin className="size-3.5" />{venue}</p>
        </div>
        <p className="mt-4 text-center text-[11px] text-slate-400">Show this QR at the entrance of each session to check in.</p>
      </div>
    </div>
  )
}
