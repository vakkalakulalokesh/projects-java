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
import { merge, switchMap, timer } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { TenantService } from '../../services/tenant.service';
import { WebsocketService } from '../../services/websocket.service';
import { Worker } from '../../models/worker.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-worker-list',
  standalone: true,
  imports: [DecimalPipe, StatusBadgeComponent, TimeAgoPipe],
  templateUrl: './worker-list.component.html',
  styleUrl: './worker-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkerListComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly tenantService = inject(TenantService);
  private readonly ws = inject(WebsocketService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly workers = signal<Worker[]>([]);
  readonly timeTick = signal(0);

  ngOnInit(): void {
    setInterval(() => this.timeTick.update((v) => v + 1), 60_000);

    this.tenantService.currentTenant$
      .pipe(
        switchMap((tenantId) =>
          merge(
            timer(0).pipe(switchMap(() => this.api.getWorkers())),
            timer(0, 20_000).pipe(switchMap(() => this.api.getWorkers())),
            this.ws.subscribeToExecutions(tenantId).pipe(switchMap(() => this.api.getWorkers())),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (list) => {
          this.workers.set(list);
          this.cdr.markForCheck();
        },
        error: () => {
          this.workers.set([]);
          this.cdr.markForCheck();
        },
      });
  }

  loadPercent(w: Worker): number {
    return (w.activeExecutions / Math.max(1, w.maxConcurrency)) * 100;
  }
}
