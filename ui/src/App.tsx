import { useState, useEffect, useRef, useCallback } from 'react'
import MetricsPanel from './MetricsPanel.tsx'
import './App.css'

type EventType =
  | 'SUBMITTED'
  | 'STEP_DISPATCHED'
  | 'STEP_COMPLETED'
  | 'RETRYING'
  | 'DEAD_LETTERED'
  | 'WORKFLOW_COMPLETED'

interface WorkflowEvent {
  type: EventType
  instanceId: string
  workflowId: string
  stepId: string | null
  status: string
  timestamp: string
}

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

const EVENT_COLORS: Record<EventType, string> = {
  SUBMITTED: '#3b82f6',
  STEP_DISPATCHED: '#64748b',
  STEP_COMPLETED: '#22c55e',
  RETRYING: '#f59e0b',
  DEAD_LETTERED: '#ef4444',
  WORKFLOW_COMPLETED: '#10b981',
}

const SCENARIOS = [
  {
    name: 'clean-batch',
    label: 'Clean Batch',
    desc: '20 × trivial-workflow — no failures',
    detail: 'Submits 20 trivial-workflow instances in parallel. Each has 2 steps, zero failures configured. Tests happy-path throughput and baseline metrics.',
  },
  {
    name: 'flaky-batch',
    label: 'Flaky Batch',
    desc: '10 × flaky-workflow — step-2 fails 30%',
    detail: 'Submits 10 flaky-workflow instances. Step-2 has a 30% random failure rate with maxAttempts=5. Watch RETRYING spike then resolve as all instances eventually complete.',
  },
  {
    name: 'poison-job',
    label: 'Poison Job',
    desc: '1 × poison-workflow → dead-lettered',
    detail: 'Submits 1 poison-workflow. Step-2 always fails; maxAttempts=3 exhausts in ~12 s with exponential backoff. Tests the dead-letter path and error message recording.',
  },
  {
    name: 'duplicate-test',
    label: 'Duplicate Test',
    desc: 'Same idempotency key twice',
    detail: 'Posts the same workflow twice with an identical Idempotency-Key header. The second call returns the same instance ID — no duplicate execution is created.',
  },
]

const HISTORY_LIMIT = 40

export default function App() {
  const [events, setEvents]         = useState<WorkflowEvent[]>([])
  const [filter, setFilter]         = useState<Set<EventType>>(new Set())
  const [connected, setConnected]   = useState(false)
  const [metrics, setMetrics]       = useState<MetricsSummary | null>(null)
  const [history, setHistory]       = useState<MetricsHistory>({ inFlight: [], retrying: [], completed: [] })
  const logRef     = useRef<HTMLDivElement>(null)
  const autoScroll = useRef(true)

  // SSE connection
  useEffect(() => {
    const es = new EventSource('/events/stream')
    es.addEventListener('workflow-event', (e: MessageEvent) => {
      const event: WorkflowEvent = JSON.parse(e.data)
      setEvents(prev => [...prev.slice(-1000), event])
    })
    es.onopen  = () => setConnected(true)
    es.onerror = () => setConnected(false)
    return () => es.close()
  }, [])

  // metrics polling
  useEffect(() => {
    const poll = async () => {
      try {
        const res = await fetch('/metrics/summary')
        if (!res.ok) return
        const s: MetricsSummary = await res.json()
        setMetrics(s)
        setHistory(h => ({
          inFlight:  [...h.inFlight.slice(-(HISTORY_LIMIT - 1)), s.inFlight],
          retrying:  [...h.retrying.slice(-(HISTORY_LIMIT - 1)), s.retrying],
          completed: [...h.completed.slice(-(HISTORY_LIMIT - 1)), s.completed],
        }))
      } catch { /* backend not up yet */ }
    }
    poll()
    const id = setInterval(poll, 1000)
    return () => clearInterval(id)
  }, [])

  // auto-scroll
  useEffect(() => {
    if (autoScroll.current && logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [events])

  const runScenario = useCallback(async (name: string) => {
    await fetch(`/scenarios/${name}`, { method: 'POST' })
  }, [])

  const clearAll = useCallback(async () => {
    await fetch('/admin/reset', { method: 'POST' })
    setEvents([])
    setHistory({ inFlight: [], retrying: [], completed: [] })
    setMetrics(null)
  }, [])

  const toggleFilter = (type: EventType) => {
    setFilter(prev => {
      const next = new Set(prev)
      if (next.has(type)) next.delete(type)
      else next.add(type)
      return next
    })
  }

  const visible = filter.size === 0
    ? events
    : events.filter(e => filter.has(e.type))

  return (
    <div className="app">
      <header>
        <h1>Driftwood Control Room</h1>
        <span className={`badge ${connected ? 'connected' : 'disconnected'}`}>
          {connected ? '● live' : '○ connecting…'}
        </span>
        <button className="reset-btn" onClick={clearAll}>↺ Clear All</button>
      </header>

      <section className="scenarios">
        {SCENARIOS.map(s => (
          <div key={s.name} className="scenario-wrap">
            <button className="scenario-btn" onClick={() => runScenario(s.name)}>
              <strong>{s.label}</strong>
              <span>{s.desc}</span>
            </button>
            <button className="info-btn" type="button" aria-label={`Info: ${s.label}`}>
              ℹ
              <span className="tooltip">{s.detail}</span>
            </button>
          </div>
        ))}
      </section>

      <MetricsPanel summary={metrics} history={history} />

      <section className="filters">
        <span className="filter-label">Filter:</span>
        {(Object.keys(EVENT_COLORS) as EventType[]).map(type => (
          <button
            key={type}
            className={`filter-btn ${filter.has(type) ? 'active' : ''}`}
            style={{ '--c': EVENT_COLORS[type] } as React.CSSProperties}
            onClick={() => toggleFilter(type)}
          >
            {type.replace(/_/g, ' ')}
          </button>
        ))}
        {filter.size > 0 && (
          <button className="clear-btn" onClick={() => setFilter(new Set())}>clear</button>
        )}
      </section>

      <div
        className="event-log"
        ref={logRef}
        onScroll={() => {
          if (!logRef.current) return
          const { scrollTop, scrollHeight, clientHeight } = logRef.current
          autoScroll.current = scrollTop + clientHeight >= scrollHeight - 24
        }}
      >
        {visible.length === 0 && (
          <div className="empty">No events yet — run a scenario to start.</div>
        )}
        {visible.map((e, i) => (
          <div key={i} className="event-row" style={{ borderLeftColor: EVENT_COLORS[e.type] }}>
            <span className="col-time">{new Date(e.timestamp).toLocaleTimeString()}</span>
            <span className="col-type" style={{ color: EVENT_COLORS[e.type] }}>{e.type}</span>
            <span className="col-workflow">{e.workflowId}</span>
            <span className="col-step">{e.stepId ? e.stepId.split('-').slice(-2).join('-') : '—'}</span>
            <span className="col-id">{e.instanceId.substring(0, 8)}</span>
          </div>
        ))}
      </div>

      <footer>{events.length} events{filter.size > 0 ? ` · ${visible.length} shown` : ''}</footer>
    </div>
  )
}
