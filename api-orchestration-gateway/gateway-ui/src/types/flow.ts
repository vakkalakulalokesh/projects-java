export const StepType = {
  HTTP_CALL: 'HTTP_CALL',
  TRANSFORM: 'TRANSFORM',
  CONDITION: 'CONDITION',
  DELAY: 'DELAY',
  AGGREGATE: 'AGGREGATE',
  SCRIPT: 'SCRIPT',
} as const

export type StepType = (typeof StepType)[keyof typeof StepType]

export type FlowStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED'

export interface RetryConfig {
  maxAttempts: number
  strategy: 'FIXED' | 'EXPONENTIAL' | 'LINEAR'
  delayMs: number
}

export interface CompensationConfig {
  enabled?: boolean
  stepId?: string
  payload?: Record<string, unknown>
}

export interface HttpStepConfig {
  url: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  headers?: Record<string, string>
  body?: string
  responseExtractors?: Record<string, string>
}

export interface TransformStepConfig {
  mappings: Record<string, string>
}

export interface ConditionStepConfig {
  expression: string
  onTrueStepId?: string
  onFalseStepId?: string
}

export interface DelayStepConfig {
  durationMs: number
}

export interface AggregateStepConfig {
  waitForStepIds: string[]
  mergeStrategy: 'MERGE_OBJECTS' | 'ARRAY' | 'FIRST_WINS' | 'LAST_WINS'
}

export interface ScriptStepConfig {
  language: 'javascript' | 'groovy' | 'python'
  code: string
}

export type StepConfig =
  | HttpStepConfig
  | TransformStepConfig
  | ConditionStepConfig
  | DelayStepConfig
  | AggregateStepConfig
  | ScriptStepConfig
  | Record<string, unknown>

export interface StepDefinition {
  id: string
  name: string
  type: StepType
  config: StepConfig
  retry?: RetryConfig
  timeoutMs?: number
  compensation?: CompensationConfig
}

export interface EdgeDefinition {
  id: string
  source: string
  target: string
  sourceHandle?: string | null
  targetHandle?: string | null
  label?: string
}

export interface FlowDefinition {
  id?: string
  name: string
  description?: string
  version?: string
  status?: FlowStatus
  tags?: string[]
  steps: StepDefinition[]
  edges: EdgeDefinition[]
  metadata?: Record<string, unknown>
}

export interface FlowCreateRequest {
  name: string
  description?: string
  definition: Omit<FlowDefinition, 'id' | 'status'>
}

export interface FlowResponse extends FlowDefinition {
  id: string
  createdAt?: string
  updatedAt?: string
  totalRuns?: number
  successRate?: number
  avgDurationMs?: number
}

export interface FlowTriggerRequest {
  payload?: Record<string, unknown>
  correlationId?: string
}

export interface FlowValidationResult {
  valid: boolean
  errors: string[]
  warnings?: string[]
}
