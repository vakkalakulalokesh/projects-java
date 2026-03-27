export type JobType = 'HTTP' | 'SHELL_SCRIPT' | 'SQL_QUERY' | 'PYTHON_SCRIPT';

export type JobStatus = 'ACTIVE' | 'PAUSED' | 'DISABLED';

export type JobPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface HttpJobConfig {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  headers?: Record<string, string>;
  body?: string;
}

export interface ShellJobConfig {
  script: string;
}

export interface SqlJobConfig {
  connectionString: string;
  query: string;
}

export interface PythonJobConfig {
  script: string;
}

export type JobTypeConfig =
  | { type: 'HTTP'; config: HttpJobConfig }
  | { type: 'SHELL_SCRIPT'; config: ShellJobConfig }
  | { type: 'SQL_QUERY'; config: SqlJobConfig }
  | { type: 'PYTHON_SCRIPT'; config: PythonJobConfig };

export interface Job {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  jobType: JobType;
  status: JobStatus;
  priority: JobPriority;
  cronExpression: string;
  timeoutSeconds: number;
  maxRetries: number;
  tags: string[];
  configuration: Record<string, unknown>;
  totalExecutions?: number;
  successfulExecutions?: number;
  failedExecutions?: number;
  lastExecutedAt?: string;
  nextRunAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface JobCreateUpdatePayload {
  name: string;
  description?: string;
  jobType: JobType;
  status?: JobStatus;
  priority: JobPriority;
  cronExpression: string;
  timeoutSeconds: number;
  maxRetries: number;
  tags: string[];
  configuration: Record<string, unknown>;
}

export interface DashboardStats {
  activeJobs: number;
  runningExecutions: number;
  todaySuccessRate: number;
  activeWorkers: number;
  activeJobsTrend?: number;
  runningExecutionsTrend?: number;
  todaySuccessRateTrend?: number;
  activeWorkersTrend?: number;
}
