import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { DashboardStats, Job, JobCreateUpdatePayload } from '../models/job.model';
import { Execution, ExecutionStats, ExecutionSummaryRow } from '../models/execution.model';
import { Worker } from '../models/worker.model';
import { TenantService } from './tenant.service';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly tenantService = inject(TenantService);

  private tenantHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Tenant-Id': this.tenantService.currentTenantId,
      'Content-Type': 'application/json',
    });
  }

  private handleError(err: unknown): Observable<never> {
    console.error('[ApiService]', err);
    return throwError(() => err);
  }

  createJob(payload: JobCreateUpdatePayload): Observable<Job> {
    return this.http
      .post<Job>(`${environment.apiUrl}/jobs`, payload, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getJobs(): Observable<Job[]> {
    return this.http
      .get<Job[]>(`${environment.apiUrl}/jobs`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getJob(id: string): Observable<Job> {
    return this.http
      .get<Job>(`${environment.apiUrl}/jobs/${id}`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  updateJob(id: string, payload: JobCreateUpdatePayload): Observable<Job> {
    return this.http
      .put<Job>(`${environment.apiUrl}/jobs/${id}`, payload, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  deleteJob(id: string): Observable<void> {
    return this.http
      .delete<void>(`${environment.apiUrl}/jobs/${id}`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  triggerJob(id: string): Observable<Execution> {
    return this.http
      .post<Execution>(`${environment.apiUrl}/jobs/${id}/trigger`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  pauseJob(id: string): Observable<Job> {
    return this.http
      .post<Job>(`${environment.apiUrl}/jobs/${id}/pause`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  resumeJob(id: string): Observable<Job> {
    return this.http
      .post<Job>(`${environment.apiUrl}/jobs/${id}/resume`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getExecutions(params?: {
    jobId?: string;
    status?: string;
    from?: string;
    to?: string;
  }): Observable<ExecutionSummaryRow[]> {
    let httpParams = new HttpParams();
    if (params?.jobId) httpParams = httpParams.set('jobId', params.jobId);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.from) httpParams = httpParams.set('from', params.from);
    if (params?.to) httpParams = httpParams.set('to', params.to);
    return this.http
      .get<ExecutionSummaryRow[]>(`${environment.apiUrl}/executions`, {
        headers: this.tenantHeaders(),
        params: httpParams,
      })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getExecution(id: string): Observable<Execution> {
    return this.http
      .get<Execution>(`${environment.apiUrl}/executions/${id}`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getExecutionStats(): Observable<ExecutionStats> {
    return this.http
      .get<ExecutionStats>(`${environment.apiUrl}/executions/stats`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getWorkers(): Observable<Worker[]> {
    return this.http
      .get<Worker[]>(`${environment.apiUrl}/workers`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http
      .get<DashboardStats>(`${environment.apiUrl}/dashboard/stats`, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }

  retryExecution(id: string): Observable<Execution> {
    return this.http
      .post<Execution>(`${environment.apiUrl}/executions/${id}/retry`, {}, { headers: this.tenantHeaders() })
      .pipe(catchError((e) => this.handleError(e)));
  }
}
