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
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, NgClass, TitleCasePipe } from '@angular/common';
import { switchMap } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { TenantService } from '../../../services/tenant.service';
import { Job, JobStatus, JobType } from '../../../models/job.model';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';

type StatusTab = 'ALL' | JobStatus;

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [RouterLink, FormsModule, NgClass, TitleCasePipe, DecimalPipe, StatusBadgeComponent, TimeAgoPipe],
  templateUrl: './job-list.component.html',
  styleUrl: './job-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobListComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly tenantService = inject(TenantService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly jobs = signal<Job[]>([]);
  readonly search = signal('');
  readonly statusTab = signal<StatusTab>('ALL');
  readonly openMenuId = signal<string | null>(null);
  readonly timeTick = signal(0);

  readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    const tab = this.statusTab();
    return this.jobs().filter((j) => {
      const nameOk = !q || j.name.toLowerCase().includes(q);
      const statusOk = tab === 'ALL' || j.status === tab;
      return nameOk && statusOk;
    });
  });

  ngOnInit(): void {
    setInterval(() => this.timeTick.update((v) => v + 1), 60_000);

    this.tenantService.currentTenant$
      .pipe(
        switchMap(() => this.api.getJobs()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (list) => {
          this.jobs.set(list);
          this.cdr.markForCheck();
        },
        error: () => {
          this.jobs.set([]);
          this.cdr.markForCheck();
        },
      });
  }

  setTab(tab: StatusTab): void {
    this.statusTab.set(tab);
  }

  typeClass(t: JobType): string {
    const map: Record<JobType, string> = {
      HTTP: 'type-http',
      SHELL_SCRIPT: 'type-shell',
      SQL_QUERY: 'type-sql',
      PYTHON_SCRIPT: 'type-python',
    };
    return map[t] ?? '';
  }

  formatJobType(t: JobType): string {
    return t.replace(/_/g, ' ');
  }

  priorityClass(p: string): string {
    const map: Record<string, string> = {
      LOW: 'pri-low',
      MEDIUM: 'pri-med',
      HIGH: 'pri-high',
      CRITICAL: 'pri-crit',
    };
    return map[p] ?? 'pri-med';
  }

  toggleMenu(id: string, ev: Event): void {
    ev.stopPropagation();
    this.openMenuId.update((cur) => (cur === id ? null : id));
  }

  closeMenu(): void {
    this.openMenuId.set(null);
  }

  trigger(id: string, ev: Event): void {
    ev.stopPropagation();
    this.closeMenu();
    this.api.triggerJob(id).subscribe({ error: () => {} });
  }

  pause(id: string, ev: Event): void {
    ev.stopPropagation();
    this.closeMenu();
    this.api.pauseJob(id).subscribe({
      next: (j) => this.patchJob(j),
      error: () => {},
    });
  }

  resume(id: string, ev: Event): void {
    ev.stopPropagation();
    this.closeMenu();
    this.api.resumeJob(id).subscribe({
      next: (j) => this.patchJob(j),
      error: () => {},
    });
  }

  delete(id: string, ev: Event): void {
    ev.stopPropagation();
    this.closeMenu();
    if (!confirm('Delete this job?')) return;
    this.api.deleteJob(id).subscribe({
      next: () => this.jobs.update((list) => list.filter((j) => j.id !== id)),
      error: () => {},
    });
  }

  private patchJob(updated: Job): void {
    this.jobs.update((list) => list.map((j) => (j.id === updated.id ? updated : j)));
    this.cdr.markForCheck();
  }
}
