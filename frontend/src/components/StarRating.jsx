import { useState } from 'react'
import { Star } from 'lucide-react'

export default function StarRating({ value, onChange, size = 'size-7', readOnly }) {
  const [hover, setHover] = useState(0)
  const shown = hover || value
  return (
    <div className="flex gap-1" onMouseLeave={() => setHover(0)}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button key={n} type="button" disabled={readOnly}
          onClick={() => onChange?.(n)} onMouseEnter={() => !readOnly && setHover(n)}
          className="transition hover:scale-110 disabled:hover:scale-100" aria-label={`${n} star${n > 1 ? 's' : ''}`}>
          <Star className={`${size} ${n <= shown ? 'fill-amber-400 text-amber-400' : 'text-slate-300 dark:text-slate-600'}`} />
        </button>
      ))}
    </div>
  )
}
