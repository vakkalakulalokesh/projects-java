import type { CircuitBreakerStatus } from '../types/circuit-breaker'
import type {
  FlowCreateRequest,
  FlowResponse,
  FlowStatus,
  FlowTriggerRequest,
  FlowValidationResult,
  StepType,
} from '../types/flow'
import type { DashboardStats, FlowExecutionResponse, FlowPerformanceSummary } from '../types/execution'

const API_BASE = '/api/v1'

export class ApiError extends Error {
  status: number
  body?: string

  constructor(message: string, status: number, body?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

async function parseResponse<T>(res: Response): Promise<T> {
  const text = await res.text()
  if (!res.ok) {
    throw new ApiError(res.statusText || 'Request failed', res.status, text)
  }
  if (!text) return undefined as T
  try {
    return JSON.parse(text) as T
  } catch {
    return text as unknown as T
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers as Record<string, string>),
    },
  })
  return parseResponse<T>(res)
}

function unwrapPage<T>(data: unknown): T[] {
  if (data && typeof data === 'object' && 'content' in data && Array.isArray((data as { content: unknown }).content)) {
    return (data as { content: T[] }).content
  }
  if (Array.isArray(data)) return data as T[]
  return []
}

export async function createFlow(body: FlowCreateRequest): Promise<FlowResponse> {
  return request<FlowResponse>('/flows', { method: 'POST', body: JSON.stringify(body) })
}

export async function getFlows(): Promise<FlowResponse[]> {
  const data = await request<unknown>('/flows')
  return unwrapPage<FlowResponse>(data)
}

export async function getFlow(id: string): Promise<FlowResponse> {
  return request<FlowResponse>(`/flows/${encodeURIComponent(id)}`)
}

export type FlowUpdateBody = Partial<FlowCreateRequest> & { status?: FlowStatus }

export async function updateFlow(id: string, body: FlowUpdateBody): Promise<FlowResponse> {
  return request<FlowResponse>(`/flows/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export async function deleteFlow(id: string): Promise<void> {
  await request<void>(`/flows/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

export async function triggerFlow(id: string, body?: FlowTriggerRequest): Promise<FlowExecutionResponse> {
  return request<FlowExecutionResponse>(`/flows/${encodeURIComponent(id)}/trigger`, {
    method: 'POST',
    body: JSON.stringify(body ?? {}),
  })
}

export async function validateFlow(id: string, definition?: unknown): Promise<FlowValidationResult> {
  return request<FlowValidationResult>(`/flows/${encodeURIComponent(id)}/validate`, {
    method: 'POST',
    body: JSON.stringify(definition ? { definition } : {}),
  })
}

export async function getExecutions(params?: { limit?: number; flowId?: string }): Promise<FlowExecutionResponse[]> {
  const q = new URLSearchParams()
  if (params?.limit != null) q.set('size', String(params.limit))
  if (params?.flowId) q.set('flowId', params.flowId)
  const qs = q.toString()
  const data = await request<unknown>(`/executions${qs ? `?${qs}` : ''}`)
  return unwrapPage<FlowExecutionResponse>(data)
}

export async function getExecution(id: string): Promise<FlowExecutionResponse> {
  return request<FlowExecutionResponse>(`/executions/${encodeURIComponent(id)}`)
}

export async function cancelExecution(id: string): Promise<void> {
  await request<void>(`/executions/${encodeURIComponent(id)}/cancel`, { method: 'POST' })
}

export async function getExecutionStats(): Promise<DashboardStats> {
  return request<DashboardStats>('/executions/stats')
}

export async function getFlowPerformance(): Promise<FlowPerformanceSummary[]> {
  return request<FlowPerformanceSummary[]>('/executions/stats').then((stats) => [stats] as unknown as FlowPerformanceSummary[])
}

export async function getCircuitBreakers(): Promise<CircuitBreakerStatus[]> {
  const data = await request<unknown>('/circuit-breakers')
  return unwrapPage<CircuitBreakerStatus>(data)
}

export async function resetCircuitBreaker(key: string): Promise<void> {
  await request<void>(`/circuit-breakers/${encodeURIComponent(key)}/reset`, { method: 'POST' })
}

export async function getStepTypes(): Promise<{ value: StepType; label: string; description?: string }[]> {
  return request(`/step-templates`)
}
