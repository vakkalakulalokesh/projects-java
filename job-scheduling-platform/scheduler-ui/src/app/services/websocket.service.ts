import { Injectable, OnDestroy, inject } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { ExecutionEvent, LogEntry } from '../models/execution.model';
import { TenantService } from './tenant.service';

/**
 * STOMP client over WebSocket. Uses {@link RxStomp} with reconnect delay on disconnect.
 */
@Injectable({ providedIn: 'root' })
export class WebsocketService implements OnDestroy {
  private readonly tenantService = inject(TenantService);
  private readonly rxStomp = new RxStomp();
  private configured = false;

  constructor() {
    this.ensureConfigured();
  }

  private ensureConfigured(): void {
    if (this.configured) {
      return;
    }
    this.rxStomp.configure({
      brokerURL: environment.wsUrl,
      connectHeaders: {},
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      reconnectDelay: 3000,
    });
    this.configured = true;
    this.rxStomp.activate();
  }

  ngOnDestroy(): void {
    void this.rxStomp.deactivate();
  }

  /** Subscribe to a STOMP destination; parses JSON bodies when possible. */
  subscribe<T = unknown>(destination: string): Observable<T> {
    this.ensureConfigured();
    return this.rxStomp.watch({ destination }).pipe(
      map((message: { body: string }) => this.parseBody<T>(message)),
    );
  }

  private parseBody<T>(message: { body: string }): T {
    const body = message.body;
    if (!body) {
      return undefined as T;
    }
    try {
      return JSON.parse(body) as T;
    } catch {
      return body as T;
    }
  }

  /** Backend topic for execution events for a tenant. */
  subscribeToExecutions(tenantId: string): Observable<ExecutionEvent> {
    const id = tenantId || this.tenantService.currentTenantId;
    return this.subscribe<ExecutionEvent>(`/topic/executions/${id}`);
  }

  /** Real-time log lines for an execution. */
  subscribeToLogs(executionId: string): Observable<LogEntry> {
    return this.subscribe<LogEntry>(`/topic/logs/${executionId}`);
  }
}
