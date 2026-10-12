/**
 * Small SVG bar chart, enough for the admin dashboard without pulling in a chart library.
 * series: [{ key, label, color }], data: [{ label, values: { [key]: number } }], stacked bars.
 */
export default function BarChart({ data, series, height = 220 }) {
  const width = 640
  const pad = { top: 12, right: 8, bottom: 28, left: 36 }
  const innerW = width - pad.left - pad.right
  const innerH = height - pad.top - pad.bottom
  const totals = data.map((d) => series.reduce((s, x) => s + (d.values[x.key] || 0), 0))
  const max = Math.max(1, ...totals)
  const step = Math.pow(10, Math.floor(Math.log10(max)))
  const top = Math.ceil(max / step) * step
  const ticks = [0, top / 2, top]
  const bw = innerW / Math.max(1, data.length)

  return (
    <div className="chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img">
        {ticks.map((v) => {
          const y = pad.top + innerH - (v / top) * innerH
          return (
            <g key={v}>
              <line x1={pad.left} x2={width - pad.right} y1={y} y2={y} stroke="#e3e9e0" />
              <text x={pad.left - 6} y={y + 4} textAnchor="end">{Math.round(v)}</text>
            </g>
          )
        })}
        {data.map((d, i) => {
          let y = pad.top + innerH
          const x = pad.left + i * bw + bw * 0.18
          return (
            <g key={d.label}>
              {series.map((s) => {
                const v = d.values[s.key] || 0
                const h = (v / top) * innerH
                y -= h
                return v > 0 ? (
                  <rect key={s.key} x={x} y={y} width={bw * 0.64} height={h} fill={s.color} rx="2">
                    <title>{`${d.label} ${s.label}: ${v}`}</title>
                  </rect>
                ) : null
              })}
              <text x={x + bw * 0.32} y={height - 10} textAnchor="middle">{d.label}</text>
            </g>
          )
        })}
      </svg>
      {series.length > 1 && (
        <div className="chart-legend">
          {series.map((s) => (
            <span key={s.key}><i style={{ background: s.color }} />{s.label}</span>
          ))}
        </div>
      )}
    </div>
  )
}
