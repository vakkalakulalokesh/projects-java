import type { JobPriority } from './job.model';

export type ExecutionStatus =
  | 'PENDING'
  | 'QUEUED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'TIMEOUT';

export type ExecutionTrigger = 'SCHEDULED' | 'MANUAL' | 'RETRY' | 'API';

export interface Execution {
  id: string;
  tenantId: string;
  jobId: string;
  jobName?: string;
  status: ExecutionStatus;
  triggerType: ExecutionTrigger;
  priority: string;
  workerId?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  output?: string;
  errorMessage?: string;
  createdAt?: string;
  steps?: ExecutionStep[];
}

export interface ExecutionStep {
  state: ExecutionStatus;
  enteredAt: string;
  exitedAt?: string;
}

export interface ExecutionStats {
  total: number;
  byStatus: Record<string, number>;
  hourlyCounts?: { hour: string; count: number }[];
}

export interface ExecutionEvent {
  type: string;
  executionId: string;
  tenantId: string;
  status?: ExecutionStatus;
  payload?: Record<string, unknown>;
  timestamp?: string;
}

export interface LogEntry {
  timestamp: string;
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  message: string;
  executionId?: string;
}

export interface ExecutionSummaryRow {
  id: string;
  jobId: string;
  jobName: string;
  status: ExecutionStatus;
  triggerType: string;
  priority: JobPriority;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  workerId?: string;
}
