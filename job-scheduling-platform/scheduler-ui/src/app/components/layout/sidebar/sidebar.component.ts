import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AsyncPipe } from '@angular/common';
import { TenantService } from '../../../services/tenant.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FormsModule, AsyncPipe],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  readonly tenantService = inject(TenantService);

  tenantOptions = ['tenant-1', 'tenant-2', 'demo-tenant'] as const;

  onTenantChange(value: string): void {
    this.tenantService.switchTenant(value);
  }
}
