import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, deleteFlow, getFlows, triggerFlow, updateFlow, type FlowResponse } from '../../services/api'
import { formatDuration } from '../../utils/formatters'
import { Modal } from '../shared/Modal'
import { StatusBadge } from '../shared/StatusBadge'
import './FlowLibrary.css'

export function FlowLibrary() {
  const navigate = useNavigate()
  const [flows, setFlows] = useState<FlowResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [deleteTarget, setDeleteTarget] = useState<FlowResponse | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getFlows()
      setFlows(data)
    } catch (e) {
      setError((e as ApiError).message ?? 'Failed to load flows')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return flows.filter((f) => {
      const matchQ =
        !q ||
        f.name.toLowerCase().includes(q) ||
        (f.description ?? '').toLowerCase().includes(q) ||
        (f.tags ?? []).some((t) => t.toLowerCase().includes(q))
      const matchS = statusFilter === 'ALL' || (f.status ?? 'DRAFT') === statusFilter
      return matchQ && matchS
    })
  }, [flows, query, statusFilter])

  const toggleActive = async (f: FlowResponse) => {
    const next = f.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
    try {
      await updateFlow(f.id, { status: next })
      await load()
    } catch (e) {
      setError((e as ApiError).message ?? 'Update failed')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      await deleteFlow(deleteTarget.id)
      setDeleteTarget(null)
      await load()
    } catch (e) {
      setError((e as ApiError).message ?? 'Delete failed')
    }
  }

  const handleTrigger = async (f: FlowResponse) => {
    try {
      const ex = await triggerFlow(f.id, {})
      navigate(`/executions/${ex.id}`)
    } catch (e) {
      setError((e as ApiError).message ?? 'Trigger failed')
    }
  }

  return (
    <div className="flow-library">
      <div className="flow-library-toolbar card">
        <input
          className="input flow-library-search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search flows, tags…"
          aria-label="Search flows"
        />
        <select
          className="input flow-library-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          aria-label="Filter by status"
        >
          <option value="ALL">All statuses</option>
          <option value="DRAFT">Draft</option>
          <option value="ACTIVE">Active</option>
          <option value="DISABLED">Disabled</option>
        </select>
        <Link to="/flows/new" className="btn btn-primary btn-sm">
          New Flow
        </Link>
      </div>

      {error && <div className="flow-library-error">{error}</div>}
      {loading && <p className="u-text-secondary">Loading flows…</p>}

      <div className="flow-library-grid">
        {filtered.map((f) => (
          <article key={f.id} className="flow-card card">
            <div className="flow-card-head">
              <h3 className="flow-card-title u-truncate" title={f.name}>
                {f.name}
              </h3>
              <StatusBadge status={f.status ?? 'DRAFT'} size="sm" />
            </div>
            {f.version && <span className="flow-card-version">v{f.version}</span>}
            <p className="flow-card-desc">{f.description || 'No description'}</p>
            <div className="flow-card-stats">
              <span>{f.totalRuns ?? 0} runs</span>
              <span>{((f.successRate ?? 0) * 100).toFixed(0)}% success</span>
              <span>{formatDuration(f.avgDurationMs)} avg</span>
            </div>
            {f.tags && f.tags.length > 0 && (
              <div className="flow-card-tags">
                {f.tags.map((t) => (
                  <span key={t} className="flow-card-tag">
                    {t}
                  </span>
                ))}
              </div>
            )}
            <div className="flow-card-actions">
              <Link to={`/flows/${f.id}`} className="btn btn-sm">
                Edit
              </Link>
              <button type="button" className="btn btn-sm btn-primary" onClick={() => void handleTrigger(f)}>
                Trigger
              </button>
              <button type="button" className="btn btn-sm" onClick={() => void toggleActive(f)}>
                {f.status === 'ACTIVE' ? 'Disable' : 'Activate'}
              </button>
              <button type="button" className="btn btn-sm btn-danger" onClick={() => setDeleteTarget(f)}>
                Delete
              </button>
            </div>
          </article>
        ))}
      </div>

      {!loading && filtered.length === 0 && (
        <p className="flow-library-empty u-text-secondary">No flows match your filters.</p>
      )}

      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Delete flow">
        <p>Delete &quot;{deleteTarget?.name}&quot;? This cannot be undone.</p>
        <div className="flow-library-modal-actions u-mt-md u-flex u-gap-sm">
          <button type="button" className="btn btn-danger" onClick={() => void handleDelete()}>
            Delete
          </button>
          <button type="button" className="btn" onClick={() => setDeleteTarget(null)}>
            Cancel
          </button>
        </div>
      </Modal>
    </div>
  )
}
