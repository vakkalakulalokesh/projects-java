import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [NgClass],
  template: `
    <span class="badge" [ngClass]="[variantClass, { pulse: pulse }]">{{ label }}</span>
  `,
  styleUrl: './status-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  @Input({ required: true }) status = '';

  get label(): string {
    return (this.status || 'UNKNOWN').replace(/_/g, ' ');
  }

  get pulse(): boolean {
    return this.status === 'RUNNING';
  }

  get variantClass(): string {
    const s = (this.status || '').toUpperCase();
    const map: Record<string, string> = {
      PENDING: 'muted',
      QUEUED: 'info',
      RUNNING: 'running',
      COMPLETED: 'success',
      FAILED: 'danger',
      CANCELLED: 'muted',
      TIMEOUT: 'warning',
      ACTIVE: 'success',
      PAUSED: 'warning',
      DISABLED: 'muted',
      ONLINE: 'success',
      BUSY: 'warning',
      OFFLINE: 'danger',
    };
    return map[s] ?? 'muted';
  }
}
