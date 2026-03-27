import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { TenantService } from '../../services/tenant.service';
import { WebsocketService } from '../../services/websocket.service';
import { DashboardStats } from '../../models/job.model';
import { ExecutionSummaryRow } from '../../models/execution.model';
import { Worker } from '../../models/worker.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe, StatusBadgeComponent, TimeAgoPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly tenantService = inject(TenantService);
  private readonly ws = inject(WebsocketService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly stats = signal<DashboardStats>({
    activeJobs: 0,
    runningExecutions: 0,
    todaySuccessRate: 0,
    activeWorkers: 0,
  });
  readonly recent = signal<ExecutionSummaryRow[]>([]);
  readonly workers = signal<Worker[]>([]);
  readonly hourly = signal<{ hour: string; count: number }[]>([]);
  readonly timeTick = signal(0);

  ngOnInit(): void {
    setInterval(() => this.timeTick.update((v) => v + 1), 60_000);

    this.tenantService.currentTenant$
      .pipe(
        switchMap(() =>
          forkJoin({
            stats: this.api.getDashboardStats().pipe(
              catchError(() =>
                of({
                  activeJobs: 0,
                  runningExecutions: 0,
                  todaySuccessRate: 0,
                  activeWorkers: 0,
                } as DashboardStats),
              ),
            ),
            executions: this.api.getExecutions().pipe(catchError(() => of([] as ExecutionSummaryRow[]))),
            workers: this.api.getWorkers().pipe(catchError(() => of([] as Worker[]))),
            execStats: this.api.getExecutionStats().pipe(
              catchError(() => of({ total: 0, byStatus: {}, hourlyCounts: [] })),
            ),
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ stats, executions, workers, execStats }) => {
        this.stats.set(stats);
        this.recent.set(executions.slice(0, 10));
        this.workers.set(workers);
        const hc = execStats.hourlyCounts?.length
          ? execStats.hourlyCounts
          : this.buildPlaceholderHourly();
        this.hourly.set(hc);
        this.cdr.markForCheck();
      });

    this.tenantService.currentTenant$
      .pipe(
        switchMap((tenantId) => this.ws.subscribeToExecutions(tenantId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.api.getExecutions().subscribe({
          next: (rows) => {
            this.recent.set(rows.slice(0, 10));
            this.cdr.markForCheck();
          },
        });
        this.api.getDashboardStats().subscribe({
          next: (s) => {
            this.stats.set(s);
            this.cdr.markForCheck();
          },
          error: () => {},
        });
      });
  }

  private buildPlaceholderHourly(): { hour: string; count: number }[] {
    const out: { hour: string; count: number }[] = [];
    for (let i = 0; i < 12; i++) {
      const h = new Date();
      h.setHours(h.getHours() - (11 - i));
      out.push({
        hour: `${h.getHours().toString().padStart(2, '0')}:00`,
        count: Math.floor(Math.random() * 8) + 1,
      });
    }
    return out;
  }

  maxHourly(): number {
    const h = this.hourly();
    return Math.max(1, ...h.map((x) => x.count));
  }

  trendText(v?: number): string {
    if (v === undefined || v === null || Number.isNaN(v)) return '—';
    const sign = v >= 0 ? '+' : '';
    return `${sign}${v}%`;
  }

  loadPercent(w: Worker): number {
    return (w.activeExecutions / Math.max(1, w.maxConcurrency)) * 100;
  }
}
