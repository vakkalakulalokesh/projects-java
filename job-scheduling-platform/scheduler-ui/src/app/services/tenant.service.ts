import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly tenantId$ = new BehaviorSubject<string>('tenant-1');

  readonly currentTenant$: Observable<string> = this.tenantId$.asObservable();

  get currentTenantId(): string {
    return this.tenantId$.value;
  }

  switchTenant(tenantId: string): void {
    if (tenantId?.trim()) {
      this.tenantId$.next(tenantId.trim());
    }
  }
}
