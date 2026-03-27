import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, getExecution, getExecutions } from '../../services/api'
import { useWebSocket } from '../../hooks/useWebSocket'
import type { FlowExecutionResponse } from '../../types/execution'
import { formatDuration, formatJSON, formatTimeAgo } from '../../utils/formatters'
import { StatusBadge } from '../shared/StatusBadge'
import { StepTimeline } from './StepTimeline'
import './ExecutionTrace.css'

function mergeExecution(prev: FlowExecutionResponse, patch: Partial<FlowExecutionResponse>): FlowExecutionResponse {
  return {
    ...prev,
    ...patch,
    steps: patch.steps ?? prev.steps,
  }
}

export function ExecutionTrace() {
  const { id } = useParams<{ id: string }>()
  const { connected, subscribe } = useWebSocket()
  const [list, setList] = useState<FlowExecutionResponse[]>([])
  const [execution, setExecution] = useState<FlowExecutionResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [ioOpen, setIoOpen] = useState(false)

  const loadList = useCallback(async () => {
    try {
      const ex = await getExecutions({ limit: 50 })
      setList(ex)
    } catch (e) {
      setError((e as ApiError).message ?? 'Failed to load executions')
    }
  }, [])

  const loadOne = useCallback(async (execId: string) => {
    setLoading(true)
    setError(null)
    try {
      const ex = await getExecution(execId)
      setExecution(ex)
    } catch (e) {
      setError((e as ApiError).message ?? 'Failed to load execution')
      setExecution(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadList()
  }, [loadList])

  useEffect(() => {
    if (!id) {
      setExecution(null)
      setLoading(false)
      return
    }
    void loadOne(id)
  }, [id, loadOne])

  useEffect(() => {
    if (!id || !connected) return
    const dest = `/topic/executions/${id}`
    const unsub = subscribe(dest, (msg) => {
      try {
        const body = JSON.parse(msg.body) as Partial<FlowExecutionResponse> & { executionId?: string }
        setExecution((prev) => {
          if (!prev) return prev
          if (body.executionId && body.executionId !== prev.id) return prev
          return mergeExecution(prev, body)
        })
      } catch {
        /* ignore non-json */
      }
    })
    return unsub
  }, [connected, id, subscribe])

  if (!id) {
    return (
      <div className="execution-trace">
        <div className="execution-list-toolbar card u-flex u-justify-between u-items-center">
          <h2 className="execution-list-title">Executions</h2>
          <Link to="/" className="btn btn-sm">
            Dashboard
          </Link>
        </div>
        {error && <div className="execution-trace-banner">{error}</div>}
        <div className="execution-list card">
          <table className="execution-list-table">
            <thead>
              <tr>
                <th>Flow</th>
                <th>Status</th>
                <th>Started</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {list.map((ex) => (
                <tr key={ex.id}>
                  <td>
                    <div className="execution-list-name">{ex.flowName ?? ex.flowId}</div>
                    <div className="execution-list-id u-text-secondary">{ex.id}</div>
                  </td>
                  <td>
                    <StatusBadge status={ex.status} size="sm" />
                  </td>
                  <td>{formatTimeAgo(ex.startedAt)}</td>
                  <td>
                    <Link to={`/executions/${ex.id}`} className="btn btn-sm btn-primary">
                      Open
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {list.length === 0 && <p className="execution-list-empty u-text-secondary">No executions yet.</p>}
        </div>
      </div>
    )
  }

  if (loading && !execution) {
    return <p className="u-text-secondary">Loading execution…</p>
  }

  if (error && !execution) {
    return (
      <div className="execution-trace">
        <div className="execution-trace-banner">{error}</div>
        <Link to="/executions" className="btn btn-sm">
          Back to list
        </Link>
      </div>
    )
  }

  if (!execution) return null

  return (
    <div className="execution-trace">
      <div className="execution-trace-header card">
        <div className="execution-trace-head-row">
          <div>
            <Link to="/executions" className="execution-trace-back">
              ← Executions
            </Link>
            <h2 className="execution-trace-flow">{execution.flowName ?? execution.flowId}</h2>
            <p className="execution-trace-id u-text-secondary">Execution {execution.id}</p>
          </div>
          <StatusBadge status={execution.status} />
        </div>
        {execution.correlationId && (
          <p className="execution-trace-corr">
            Correlation ID: <code>{execution.correlationId}</code>
          </p>
        )}
        <div className="execution-trace-timing">
          <span>Started {formatTimeAgo(execution.startedAt)}</span>
          <span>Duration {formatDuration(execution.durationMs)}</span>
          {execution.completedAt && <span>Completed {formatTimeAgo(execution.completedAt)}</span>}
        </div>
        {connected && <span className="execution-trace-live">Live updates on</span>}
      </div>

      <div className="execution-trace-io card">
        <button type="button" className="execution-trace-io-toggle btn btn-ghost btn-sm" onClick={() => setIoOpen((o) => !o)}>
          Input / output {ioOpen ? '▲' : '▼'}
        </button>
        {ioOpen && (
          <div className="execution-trace-io-grid">
            <div>
              <span className="execution-trace-io-label">Input</span>
              <pre className="execution-trace-pre">{formatJSON(execution.input ?? {})}</pre>
            </div>
            <div>
              <span className="execution-trace-io-label">Output</span>
              <pre className="execution-trace-pre">{formatJSON(execution.output ?? {})}</pre>
            </div>
          </div>
        )}
      </div>

      <section className="execution-trace-timeline card">
        <h3 className="execution-trace-section-title">Step timeline</h3>
        <StepTimeline steps={execution.steps} />
      </section>
    </div>
  )
}
