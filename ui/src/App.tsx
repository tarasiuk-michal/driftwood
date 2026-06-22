import { useState, useEffect, useRef, useCallback } from 'react'
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

const EVENT_COLORS: Record<EventType, string> = {
  SUBMITTED: '#3b82f6',
  STEP_DISPATCHED: '#64748b',
  STEP_COMPLETED: '#22c55e',
  RETRYING: '#f59e0b',
  DEAD_LETTERED: '#ef4444',
  WORKFLOW_COMPLETED: '#10b981',
}

const SCENARIOS = [
  { name: 'clean-batch',    label: 'Clean Batch',    desc: '5 × trivial-workflow — no failures' },
  { name: 'flaky-batch',    label: 'Flaky Batch',    desc: '5 × flaky-workflow — step-2 retries' },
  { name: 'poison-job',     label: 'Poison Job',     desc: '1 × poison-workflow → dead-lettered' },
  { name: 'duplicate-test', label: 'Duplicate Test', desc: 'Same idempotency key twice' },
]

export default function App() {
  const [events, setEvents]     = useState<WorkflowEvent[]>([])
  const [filter, setFilter]     = useState<Set<EventType>>(new Set())
  const [connected, setConnected] = useState(false)
  const logRef     = useRef<HTMLDivElement>(null)
  const autoScroll = useRef(true)

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

  useEffect(() => {
    if (autoScroll.current && logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [events])

  const runScenario = useCallback(async (name: string) => {
    await fetch(`/scenarios/${name}`, { method: 'POST' })
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
      </header>

      <section className="scenarios">
        {SCENARIOS.map(s => (
          <button key={s.name} className="scenario-btn" onClick={() => runScenario(s.name)}>
            <strong>{s.label}</strong>
            <span>{s.desc}</span>
          </button>
        ))}
      </section>

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
          <button className="clear-btn" onClick={() => setFilter(new Set())}>
            clear
          </button>
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
