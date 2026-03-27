import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { TenantService } from '../../../services/tenant.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [AsyncPipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  private readonly router = inject(Router);
  readonly tenantService = inject(TenantService);

  readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.resolveTitle(this.router.url)),
      startWith(this.resolveTitle(this.router.url)),
    ),
    { initialValue: 'Dashboard' },
  );

  private resolveTitle(url: string): string {
    if (url.includes('/dashboard')) return 'Dashboard';
    if (url.includes('/jobs/new')) return 'Create Job';
    if (url.match(/\/jobs\/.+\/edit/)) return 'Edit Job';
    if (url.includes('/jobs')) return 'Jobs';
    if (url.includes('/executions') && /\/executions\/[^/]+\/logs/.test(url)) return 'Execution Logs';
    if (url.match(/\/executions\/[^/]+$/)) return 'Execution Detail';
    if (url.includes('/executions')) return 'Executions';
    if (url.includes('/workers')) return 'Workers';
    return 'Scheduler';
  }
}
