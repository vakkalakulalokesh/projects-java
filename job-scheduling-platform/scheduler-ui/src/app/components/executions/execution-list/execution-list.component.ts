import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { merge, switchMap, timer } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { TenantService } from '../../../services/tenant.service';
import { WebsocketService } from '../../../services/websocket.service';
import { ExecutionSummaryRow } from '../../../models/execution.model';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-execution-list',
  standalone: true,
  imports: [RouterLink, FormsModule, StatusBadgeComponent, TimeAgoPipe],
  templateUrl: './execution-list.component.html',
  styleUrl: './execution-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutionListComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly tenantService = inject(TenantService);
  private readonly ws = inject(WebsocketService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<ExecutionSummaryRow[]>([]);
  readonly jobFilter = signal('');
  readonly statusFilter = signal('');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly timeTick = signal(0);

  readonly filtered = computed(() => {
    const jf = this.jobFilter().trim().toLowerCase();
    const sf = this.statusFilter().trim().toUpperCase();
    return this.rows().filter((r) => {
      const jobOk = !jf || r.jobName.toLowerCase().includes(jf);
      const stOk = !sf || r.status === sf;
      return jobOk && stOk;
    });
  });

  ngOnInit(): void {
    setInterval(() => this.timeTick.update((v) => v + 1), 60_000);

    const load = () =>
      this.api.getExecutions({
        from: this.fromDate() || undefined,
        to: this.toDate() || undefined,
      });

    this.tenantService.currentTenant$
      .pipe(
        switchMap((tenantId) =>
          merge(
            load(),
            timer(0, 15_000).pipe(switchMap(() => load())),
            this.ws.subscribeToExecutions(tenantId).pipe(switchMap(() => load())),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (list) => {
          this.rows.set(list);
          this.cdr.markForCheck();
        },
        error: () => {
          this.rows.set([]);
          this.cdr.markForCheck();
        },
      });
  }

  applyDateFilters(): void {
    this.api
      .getExecutions({
        from: this.fromDate() || undefined,
        to: this.toDate() || undefined,
      })
      .subscribe({
        next: (list) => {
          this.rows.set(list);
          this.cdr.markForCheck();
        },
        error: () => {},
      });
  }

  rowClick(id: string): void {
    void this.router.navigate(['/executions', id]);
  }

  formatDuration(ms?: number): string {
    if (ms == null || Number.isNaN(ms)) return '—';
    if (ms < 1000) return `${ms}ms`;
    const s = Math.floor(ms / 1000);
    if (s < 60) return `${s}s`;
    const m = Math.floor(s / 60);
    const rs = s % 60;
    return `${m}m ${rs}s`;
  }
}
