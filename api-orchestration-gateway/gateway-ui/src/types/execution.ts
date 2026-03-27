export type ExecutionStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'COMPENSATING'

export type StepExecutionStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'SKIPPED'
  | 'COMPENSATING'

export interface StepExecutionResponse {
  stepId: string
  stepName: string
  stepType: string
  status: StepExecutionStatus
  attempt?: number
  maxAttempts?: number
  startedAt?: string
  completedAt?: string
  durationMs?: number
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  errorMessage?: string
  compensation?: boolean
}

export interface FlowExecutionResponse {
  id: string
  flowId: string
  flowName?: string
  correlationId?: string
  status: ExecutionStatus
  startedAt: string
  completedAt?: string
  durationMs?: number
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  steps: StepExecutionResponse[]
}

export interface ExecutionTraceEvent {
  executionId: string
  type: 'STEP_STARTED' | 'STEP_COMPLETED' | 'STEP_FAILED' | 'EXECUTION_UPDATED'
  stepId?: string
  payload?: Record<string, unknown>
  timestamp?: string
}

export interface DashboardStats {
  totalFlows: number
  activeExecutions: number
  successRate: number
  circuitBreakersOpen: number
}

export interface FlowPerformanceSummary {
  flowId: string
  flowName: string
  runs24h: number
  avgDurationMs: number
  successRate: number
}
