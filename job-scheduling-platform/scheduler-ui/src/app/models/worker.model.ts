export type WorkerStatus = 'ONLINE' | 'BUSY' | 'OFFLINE';

export interface Worker {
  id: string;
  tenantId?: string;
  hostname: string;
  status: WorkerStatus;
  activeExecutions: number;
  maxConcurrency: number;
  uptimeSeconds?: number;
  lastHeartbeatAt?: string;
  version?: string;
}
