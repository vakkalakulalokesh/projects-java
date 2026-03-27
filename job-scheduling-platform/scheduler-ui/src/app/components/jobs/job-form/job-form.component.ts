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
import {
  FormArray,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgClass, TitleCasePipe } from '@angular/common';
import { switchMap } from 'rxjs';
import { JobType, JobPriority, JobStatus, JobCreateUpdatePayload } from '../../../models/job.model';
import { ApiService } from '../../../services/api.service';
import { TenantService } from '../../../services/tenant.service';

@Component({
  selector: 'app-job-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgClass, TitleCasePipe],
  templateUrl: './job-form.component.html',
  styleUrl: './job-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobFormComponent implements OnInit {
  readonly id = input<string | undefined>(undefined);

  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly tenantService = inject(TenantService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly saving = signal(false);
  readonly jobTypes: JobType[] = ['HTTP', 'SHELL_SCRIPT', 'SQL_QUERY', 'PYTHON_SCRIPT'];
  readonly priorities: JobPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    jobType: this.fb.nonNullable.control<JobType>('HTTP', Validators.required),
    status: this.fb.nonNullable.control<JobStatus>('ACTIVE'),
    cronExpression: ['0 * * * *', Validators.required],
    priority: this.fb.nonNullable.control<JobPriority>('MEDIUM', Validators.required),
    timeoutSeconds: [300, [Validators.required, Validators.min(1)]],
    maxRetries: [3, [Validators.required, Validators.min(0)]],
    tags: this.fb.array<FormControl<string>>([]),
    httpUrl: ['https://'],
    httpMethod: this.fb.nonNullable.control<'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'>('GET'),
    httpHeaders: this.fb.array([this.fb.group({ key: [''], value: [''] })]),
    httpBody: [''],
    shellScript: [''],
    sqlConnection: [''],
    sqlQuery: [''],
    pythonScript: [''],
  });

  ngOnInit(): void {
    this.form
      .get('jobType')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cdr.markForCheck());

    const jobId = this.id();
    if (jobId) {
      this.tenantService.currentTenant$
        .pipe(
          switchMap(() => this.api.getJob(jobId)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (job) => {
            this.form.patchValue({
              name: job.name,
              description: job.description ?? '',
              jobType: job.jobType,
              status: job.status,
              cronExpression: job.cronExpression,
              priority: job.priority,
              timeoutSeconds: job.timeoutSeconds,
              maxRetries: job.maxRetries,
            });
            this.tagsArray.clear();
            (job.tags ?? []).forEach((t) => this.tagsArray.push(this.fb.nonNullable.control(t)));
            this.patchTypeConfig(job.jobType, job.configuration ?? {});
            this.cdr.markForCheck();
          },
          error: () => {},
        });
    }
  }

  get tagsArray(): FormArray<FormControl<string>> {
    return this.form.get('tags') as FormArray<FormControl<string>>;
  }

  get headerRows(): FormArray {
    return this.form.get('httpHeaders') as FormArray;
  }

  addTag(input: HTMLInputElement): void {
    const v = input.value.trim();
    if (!v) return;
    this.tagsArray.push(this.fb.nonNullable.control(v));
    input.value = '';
    this.cdr.markForCheck();
  }

  removeTag(i: number): void {
    this.tagsArray.removeAt(i);
    this.cdr.markForCheck();
  }

  addHeaderRow(): void {
    this.headerRows.push(this.fb.group({ key: [''], value: [''] }));
    this.cdr.markForCheck();
  }

  removeHeaderRow(i: number): void {
    this.headerRows.removeAt(i);
    this.cdr.markForCheck();
  }

  selectType(t: JobType): void {
    this.form.patchValue({ jobType: t });
    this.cdr.markForCheck();
  }

  formatTypeName(t: JobType): string {
    return t.replace(/_/g, ' ');
  }

  typeBlurb(t: JobType): string {
    const m: Record<JobType, string> = {
      HTTP: 'Call a REST endpoint with headers and body.',
      SHELL_SCRIPT: 'Run commands on a worker shell.',
      SQL_QUERY: 'Execute SQL against a database.',
      PYTHON_SCRIPT: 'Run Python in a managed runtime.',
    };
    return m[t];
  }

  cronPreview(): string {
    const c = this.form.get('cronExpression')?.value ?? '';
    return this.describeCron(c);
  }

  private describeCron(expr: string): string {
    const e = expr.trim();
    if (e === '* * * * *') return 'Every minute';
    if (e === '0 * * * *') return 'Every hour at minute 0';
    if (e === '0 0 * * *') return 'Daily at midnight';
    if (e === '0 0 * * 0') return 'Weekly on Sunday at midnight';
    if (e === '*/15 * * * *') return 'Every 15 minutes';
    if (e === '0 */6 * * *') return 'Every 6 hours';
    return 'Custom schedule — verify expression with your orchestrator';
  }

  private patchTypeConfig(type: JobType, cfg: Record<string, unknown>): void {
    switch (type) {
      case 'HTTP':
        {
          const rawMethod = String(cfg['method'] ?? 'GET').toUpperCase();
          const allowed = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'] as const;
          const httpMethod = (allowed as readonly string[]).includes(rawMethod)
            ? (rawMethod as (typeof allowed)[number])
            : 'GET';
          this.form.patchValue({
            httpUrl: (cfg['url'] as string) ?? 'https://',
            httpMethod,
            httpBody: (cfg['body'] as string) ?? '',
          });
        }
        this.headerRows.clear();
        const headers = (cfg['headers'] as Record<string, string>) ?? {};
        const keys = Object.keys(headers);
        if (keys.length === 0) {
          this.headerRows.push(this.fb.group({ key: [''], value: [''] }));
        } else {
          keys.forEach((k) =>
            this.headerRows.push(this.fb.group({ key: [k], value: [headers[k] ?? ''] })),
          );
        }
        break;
      case 'SHELL_SCRIPT':
        this.form.patchValue({ shellScript: (cfg['script'] as string) ?? '' });
        break;
      case 'SQL_QUERY':
        this.form.patchValue({
          sqlConnection: (cfg['connectionString'] as string) ?? '',
          sqlQuery: (cfg['query'] as string) ?? '',
        });
        break;
      case 'PYTHON_SCRIPT':
        this.form.patchValue({ pythonScript: (cfg['script'] as string) ?? '' });
        break;
    }
  }

  private buildTypeConfig(): Record<string, unknown> {
    const t = this.form.get('jobType')?.value;
    switch (t) {
      case 'HTTP': {
        const headers: Record<string, string> = {};
        this.headerRows.controls.forEach((g) => {
          const k = g.get('key')?.value?.trim();
          const v = g.get('value')?.value ?? '';
          if (k) headers[k] = v;
        });
        return {
          url: this.form.get('httpUrl')?.value,
          method: this.form.get('httpMethod')?.value,
          headers,
          body: this.form.get('httpBody')?.value ?? '',
        };
      }
      case 'SHELL_SCRIPT':
        return { script: this.form.get('shellScript')?.value ?? '' };
      case 'SQL_QUERY':
        return {
          connectionString: this.form.get('sqlConnection')?.value ?? '',
          query: this.form.get('sqlQuery')?.value ?? '',
        };
      case 'PYTHON_SCRIPT':
        return { script: this.form.get('pythonScript')?.value ?? '' };
      default:
        return {};
    }
  }

  cancel(): void {
    void this.router.navigateByUrl('/jobs');
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const payload: JobCreateUpdatePayload = {
      name: v.name ?? '',
      description: v.description || undefined,
      jobType: v.jobType,
      status: v.status,
      priority: v.priority,
      cronExpression: v.cronExpression ?? '',
      timeoutSeconds: Number(v.timeoutSeconds),
      maxRetries: Number(v.maxRetries),
      tags: this.tagsArray.controls.map((c) => c.value as string),
      configuration: this.buildTypeConfig(),
    };
    const jobId = this.id();
    const req = jobId ? this.api.updateJob(jobId, payload) : this.api.createJob(payload);
    req.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigateByUrl('/jobs');
      },
      error: () => {
        this.saving.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
