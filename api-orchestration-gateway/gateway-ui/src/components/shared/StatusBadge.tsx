import './StatusBadge.css'

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'status-pending',
  RUNNING: 'status-running',
  COMPLETED: 'status-completed',
  FAILED: 'status-failed',
  CANCELLED: 'status-cancelled',
  COMPENSATING: 'status-compensating',
  SKIPPED: 'status-skipped',
  DRAFT: 'status-draft',
  ACTIVE: 'status-active',
  DISABLED: 'status-disabled',
  CLOSED: 'status-closed',
  OPEN: 'status-open',
  HALF_OPEN: 'status-half-open',
}

export interface StatusBadgeProps {
  status: string
  size?: 'sm' | 'md'
}

export function StatusBadge({ status, size = 'md' }: StatusBadgeProps) {
  const key = status.toUpperCase()
  const cls = STATUS_COLORS[key] ?? 'status-default'
  return (
    <span className={`status-badge ${cls} status-badge--${size}`} title={status}>
      {status}
    </span>
  )
}
