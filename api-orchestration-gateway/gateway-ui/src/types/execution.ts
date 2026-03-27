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
  id?: string
  stepId: string
  stepName: string
  stepType: string
  status: StepExecutionStatus
  attempt?: number
  maxAttempts?: number
  startedAt?: string
  completedAt?: string
  durationMs?: number
  inputData?: Record<string, unknown>
  outputData?: Record<string, unknown>
  error?: string
  errorMessage?: string
  compensation?: boolean
}

export interface FlowExecutionResponse {
  id: string
  executionId?: string
  flowId: string
  flowName?: string
  correlationId?: string
  status: ExecutionStatus
  startedAt: string
  completedAt?: string
  durationMs?: number
  inputData?: Record<string, unknown>
  outputData?: Record<string, unknown>
  stepExecutions?: StepExecutionResponse[]
  createdAt?: string
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
  activeFlows: number
  totalExecutions: number
  runningExecutions: number
  completedToday: number
  failedToday: number
  successRate: number
  avgDurationMs: number
  circuitBreakers: { endpoint: string; state: string; failureCount: number }[]
}

export interface FlowPerformanceSummary {
  flowId: string
  flowName: string
  runs24h: number
  avgDurationMs: number
  successRate: number
}
