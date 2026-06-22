interface MetricsSummary {
  inFlight: number
  completed: number
  deadLettered: number
  retrying: number
  avgStepLatencyMs: number
}

interface MetricsHistory {
  inFlight: number[]
  retrying: number[]
  completed: number[]
}

interface SparklineProps {
  data: number[]
  color: string
  width?: number
  height?: number
}

function Sparkline({ data, color, width = 160, height = 40 }: SparklineProps) {
  if (data.length < 2) {
    return <svg width={width} height={height}><line x1={0} y1={height / 2} x2={width} y2={height / 2} stroke="#1e293b" strokeWidth={1} /></svg>
  }
  const max = Math.max(...data, 1)
  const pts = data
    .map((v, i) => {
      const x = (i / (data.length - 1)) * width
      const y = height - (v / max) * (height - 4) - 2
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
  return (
    <svg width={width} height={height}>
      <polyline points={pts} fill="none" stroke={color} strokeWidth={1.5} strokeLinejoin="round" />
    </svg>
  )
}

interface MetricsPanelProps {
  summary: MetricsSummary | null
  history: MetricsHistory
}

export default function MetricsPanel({ summary, history }: MetricsPanelProps) {
  const fmt = (n: number) => n.toFixed(0)
  const ms  = summary ? summary.avgStepLatencyMs.toFixed(0) : '—'

  return (
    <section className="metrics-panel">
      <div className="metric-card">
        <div className="metric-label">In-flight</div>
        <div className="metric-value" style={{ color: '#3b82f6' }}>{summary ? summary.inFlight : '—'}</div>
        <Sparkline data={history.inFlight} color="#3b82f6" />
      </div>

      <div className="metric-card">
        <div className="metric-label">Retrying</div>
        <div className="metric-value" style={{ color: '#f59e0b' }}>{summary ? summary.retrying : '—'}</div>
        <Sparkline data={history.retrying} color="#f59e0b" />
      </div>

      <div className="metric-card">
        <div className="metric-label">Completed</div>
        <div className="metric-value" style={{ color: '#22c55e' }}>{summary ? fmt(summary.completed) : '—'}</div>
        <Sparkline data={history.completed} color="#22c55e" />
      </div>

      <div className="metric-card">
        <div className="metric-label">Dead-lettered</div>
        <div className="metric-value" style={{ color: '#ef4444' }}>{summary ? summary.deadLettered : '—'}</div>
        <div className="metric-sublabel">cumulative</div>
      </div>

      <div className="metric-card">
        <div className="metric-label">Avg step latency</div>
        <div className="metric-value" style={{ color: '#a78bfa' }}>{ms}</div>
        <div className="metric-sublabel">ms</div>
      </div>
    </section>
  )
}
