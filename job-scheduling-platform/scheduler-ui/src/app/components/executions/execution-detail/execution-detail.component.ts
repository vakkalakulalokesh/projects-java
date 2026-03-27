import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { merge, switchMap, timer } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { TenantService } from '../../../services/tenant.service';
import { WebsocketService } from '../../../services/websocket.service';
import { Execution, ExecutionStatus } from '../../../models/execution.model';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-execution-detail',
  standalone: true,
  imports: [RouterLink, StatusBadgeComponent],
  templateUrl: './execution-detail.component.html',
  styleUrl: './execution-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutionDetailComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(ApiService);
  private readonly tenantService = inject(TenantService);
  private readonly ws = inject(WebsocketService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly execution = signal<Execution | null>(null);
  readonly outputOpen = signal(true);

  readonly flowSteps: { key: ExecutionStatus; label: string }[] = [
    { key: 'PENDING', label: 'Pending' },
    { key: 'QUEUED', label: 'Queued' },
    { key: 'RUNNING', label: 'Running' },
    { key: 'COMPLETED', label: 'Completed' },
  ];

  ngOnInit(): void {
    this.tenantService.currentTenant$
      .pipe(
        switchMap((tenantId) =>
          merge(
            timer(0).pipe(switchMap(() => this.api.getExecution(this.id()))),
            timer(0, 10_000).pipe(switchMap(() => this.api.getExecution(this.id()))),
            this.ws.subscribeToExecutions(tenantId).pipe(
              switchMap(() => this.api.getExecution(this.id())),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (ex) => {
          this.execution.set(ex);
          this.cdr.markForCheck();
        },
        error: () => {
          this.execution.set(null);
          this.cdr.markForCheck();
        },
      });
  }

  toggleOutput(): void {
    this.outputOpen.update((v) => !v);
  }

  viewLogs(): void {
    void this.router.navigate(['/executions', this.id(), 'logs']);
  }

  retry(): void {
    this.api.retryExecution(this.id()).subscribe({
      next: () =>
        this.api.getExecution(this.id()).subscribe({
          next: (ex) => {
            this.execution.set(ex);
            this.cdr.markForCheck();
          },
        }),
      error: () => {},
    });
  }

  stepState(
    step: ExecutionStatus,
    ex: Execution,
  ): 'done' | 'active' | 'pending' | 'error' {
    const order: ExecutionStatus[] = ['PENDING', 'QUEUED', 'RUNNING', 'COMPLETED'];
    const stepIdx = order.indexOf(step);
    const cur = ex.status;

    if (step === 'COMPLETED') {
      if (cur === 'COMPLETED') return 'done';
      if (cur === 'FAILED' || cur === 'CANCELLED' || cur === 'TIMEOUT') return 'error';
      return 'pending';
    }

    const curIdx =
      cur === 'PENDING'
        ? 0
        : cur === 'QUEUED'
          ? 1
          : cur === 'RUNNING'
            ? 2
            : cur === 'COMPLETED'
              ? 3
              : cur === 'FAILED' || cur === 'CANCELLED' || cur === 'TIMEOUT'
                ? 2
                : 0;

    if (curIdx > stepIdx) return 'done';
    if (curIdx === stepIdx) {
      if (cur === 'FAILED' || cur === 'CANCELLED' || cur === 'TIMEOUT') return 'error';
      return 'active';
    }
    return 'pending';
  }

  formatDuration(ms?: number): string {
    if (ms == null || Number.isNaN(ms)) return '—';
    if (ms < 1000) return `${ms}ms`;
    const s = Math.floor(ms / 1000);
    if (s < 60) return `${s}s`;
    const m = Math.floor(s / 60);
    return `${m}m ${s % 60}s`;
  }
}
