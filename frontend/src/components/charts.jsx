// Shared chart styling so Recharts looks consistent in light and dark mode.
export const axisProps = {
  stroke: '#94a3b8',
  fontSize: 11,
  tickLine: false,
  axisLine: false,
}

export function ChartTooltip({ active, payload, label, formatter }) {
  if (!active || !payload?.length) return null
  return (
    <div className="glass-strong rounded-xl px-3 py-2 text-xs shadow-xl">
      {label != null && <p className="mb-1 font-semibold">{label}</p>}
      {payload.map((p) => (
        <p key={p.dataKey || p.name} className="flex items-center gap-2 text-slate-600 dark:text-slate-300">
          <span className="size-2 rounded-full" style={{ background: p.color || p.payload?.fill }} />
          {p.name}: <span className="font-semibold text-slate-900 dark:text-white">{formatter ? formatter(p.value) : p.value}</span>
        </p>
      ))}
    </div>
  )
}
