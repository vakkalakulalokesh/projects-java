import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { DashboardStats, Job, JobCreateUpdatePayload } from '../models/job.model';
import { Execution, ExecutionStats, ExecutionSummaryRow } from '../models/execution.model';
import { Worker } from '../models/worker.model';
import { TenantService } from './tenant.service';

interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly tenantService = inject(TenantService);

  private get tenantBase(): string {
    return `${environment.apiUrl}/tenants/${this.tenantService.currentTenantId}`;
  }

  private tenantHeaders(): HttpHeaders {
    return new HttpHeaders({ 'Content-Type': 'application/json' });
  }

  private handleError(err: unknown): Observable<never> {
    console.error('[ApiService]', err);
    return throwError(() => err);
  }

  private unwrapPage<T>(obs: Observable<SpringPage<T>>): Observable<T[]> {
    return obs.pipe(
      map((page) => {
        if (Array.isArray(page)) return page;
        return page?.content ?? [];
      }),
    );
  }

  createJob(payload: JobCreateUpdatePayload): Observable<Job> {
    return this.http
      .post<Job>(`${this.tenantBase}/jobs`, payload, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getJobs(): Observable<Job[]> {
    return this.unwrapPage(
      this.http.get<SpringPage<Job>>(`${this.tenantBase}/jobs?size=100`, { headers: this.tenantHeaders() }),
    ).pipe(catchError((e) => this.handleError(e)));
  }

  getJob(id: string): Observable<Job> {
    return this.http
      .get<Job>(`${this.tenantBase}/jobs/${id}`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  updateJob(id: string, payload: JobCreateUpdatePayload): Observable<Job> {
    return this.http
      .put<Job>(`${this.tenantBase}/jobs/${id}`, payload, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  deleteJob(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.tenantBase}/jobs/${id}`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  triggerJob(id: string): Observable<Execution> {
    return this.http
      .post<Execution>(`${this.tenantBase}/jobs/${id}/trigger`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  pauseJob(id: string): Observable<Job> {
    return this.http
      .post<Job>(`${this.tenantBase}/jobs/${id}/pause`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  resumeJob(id: string): Observable<Job> {
    return this.http
      .post<Job>(`${this.tenantBase}/jobs/${id}/resume`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getExecutions(params?: {
    jobId?: string;
    status?: string;
    from?: string;
    to?: string;
  }): Observable<ExecutionSummaryRow[]> {
    let httpParams = new HttpParams().set('size', '50');
    if (params?.jobId) httpParams = httpParams.set('jobId', params.jobId);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.from) httpParams = httpParams.set('from', params.from);
    if (params?.to) httpParams = httpParams.set('to', params.to);
    return this.unwrapPage(
      this.http.get<SpringPage<ExecutionSummaryRow>>(`${this.tenantBase}/executions`, {
        headers: this.tenantHeaders(),
        params: httpParams,
      }),
    ).pipe(catchError((e) => this.handleError(e)));
  }

  getExecution(id: string): Observable<Execution> {
    return this.http
      .get<Execution>(`${this.tenantBase}/executions/${id}`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getExecutionStats(): Observable<ExecutionStats> {
    return this.http
      .get<Record<string, unknown>>(`${this.tenantBase}/executions/stats`, { headers: this.tenantHeaders() })
      .pipe(
        map((s) => ({
          total: (s['totalExecutionsToday'] as number) ?? 0,
          byStatus: (s['executionsByStatus'] as Record<string, number>) ?? {},
          hourlyCounts: ((s['executionsByHour'] as { hour: number; count: number }[]) ?? []).map((h) => ({
            hour: `${String(h.hour).padStart(2, '0')}:00`,
            count: h.count,
          })),
        })),
        catchError((e) => this.handleError(e)),
      );
  }

  getWorkers(): Observable<Worker[]> {
    return this.http
      .get<Worker[]>(`${environment.apiUrl}/workers`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http
      .get<Record<string, unknown>>(`${this.tenantBase}/executions/stats`, { headers: this.tenantHeaders() })
      .pipe(
        map((raw) => ({
          activeJobs: (raw['activeJobs'] as number) ?? 0,
          runningExecutions: (raw['runningExecutions'] as number) ?? 0,
          todaySuccessRate: (raw['successRate'] as number) ?? 0,
          activeWorkers: (raw['workerCount'] as number) ?? 0,
        })),
        catchError((e) => this.handleError(e)),
      );
  }

  retryExecution(id: string): Observable<Execution> {
    return this.http
      .post<Execution>(`${this.tenantBase}/executions/${id}/retry`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }
}
