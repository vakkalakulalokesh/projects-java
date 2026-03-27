import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { JobListComponent } from './components/jobs/job-list/job-list.component';
import { JobFormComponent } from './components/jobs/job-form/job-form.component';
import { ExecutionListComponent } from './components/executions/execution-list/execution-list.component';
import { ExecutionDetailComponent } from './components/executions/execution-detail/execution-detail.component';
import { LogViewerComponent } from './components/log-viewer/log-viewer.component';
import { WorkerListComponent } from './components/workers/worker-list.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'jobs', component: JobListComponent },
  { path: 'jobs/new', component: JobFormComponent },
  { path: 'jobs/:id/edit', component: JobFormComponent },
  { path: 'executions', component: ExecutionListComponent },
  { path: 'executions/:id', component: ExecutionDetailComponent },
  { path: 'executions/:id/logs', component: LogViewerComponent },
  { path: 'workers', component: WorkerListComponent },
];
