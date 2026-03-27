export type WorkerStatus = 'ONLINE' | 'BUSY' | 'OFFLINE';

export interface Worker {
  workerId: string;
  hostname: string;
  status: WorkerStatus;
  activeExecutions: number;
  maxConcurrency: number;
  uptime?: number;
  lastHeartbeatAt?: string;
}
