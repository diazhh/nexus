///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { NexusModuleService } from '@core/http/nexus-module.service';
import { NexusModule, TenantModule } from '@shared/models/nexus-module.models';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-tenant-modules',
  templateUrl: './tenant-modules.component.html',
  styleUrls: ['./tenant-modules.component.scss']
})
export class TenantModulesComponent extends PageComponent implements OnInit, OnDestroy, OnChanges {

  @Input() tenantId: string;
  @Input() active: boolean = false;

  availableModules: NexusModule[] = [];
  assignedModuleIds: Set<string> = new Set();
  activeModuleIds: Set<string> = new Set();

  isLoading = false;
  private loaded = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    protected store: Store<AppState>,
    private nexusModuleService: NexusModuleService,
    private translate: TranslateService
  ) {
    super(store);
  }

  ngOnInit(): void {
    if (this.active && this.tenantId) {
      this.loadData();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.active && this.active && !this.loaded && this.tenantId) {
      this.loadData();
    }
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadData(): void {
    this.isLoading = true;
    this.loaded = true;

    forkJoin([
      this.nexusModuleService.getAvailableModules(),
      this.nexusModuleService.getTenantModules(this.tenantId)
    ]).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ([modules, tenantModules]) => {
          this.availableModules = modules.filter(m => !m.systemModule);
          this.assignedModuleIds.clear();
          this.activeModuleIds.clear();

          tenantModules.forEach(tm => {
            if (tm.moduleId?.id) {
              this.assignedModuleIds.add(tm.moduleId.id);
              if (tm.active) {
                this.activeModuleIds.add(tm.moduleId.id);
              }
            }
          });

          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  isModuleAssigned(module: NexusModule): boolean {
    return this.assignedModuleIds.has(module.id.id);
  }

  isModuleActive(module: NexusModule): boolean {
    return this.activeModuleIds.has(module.id.id);
  }

  toggleModuleAssignment(module: NexusModule): void {
    const isAssigned = this.isModuleAssigned(module);

    if (isAssigned) {
      this.nexusModuleService.unassignModuleFromTenant(this.tenantId, module.id.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.assignedModuleIds.delete(module.id.id);
            this.activeModuleIds.delete(module.id.id);
            this.showNotification('nexus-module.module-unassigned');
          },
          error: () => {
            this.showNotification('nexus-module.error-unassigning', 'warn');
          }
        });
    } else {
      this.nexusModuleService.assignModuleToTenant(this.tenantId, module.id.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (tenantModule) => {
            this.assignedModuleIds.add(module.id.id);
            if (tenantModule.active) {
              this.activeModuleIds.add(module.id.id);
            }
            this.showNotification('nexus-module.module-assigned');
          },
          error: () => {
            this.showNotification('nexus-module.error-assigning', 'warn');
          }
        });
    }
  }

  toggleModuleActivation(module: NexusModule): void {
    if (!this.isModuleAssigned(module)) {
      return;
    }

    const isActive = this.isModuleActive(module);

    if (isActive) {
      this.nexusModuleService.deactivateModuleForTenant(this.tenantId, module.id.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.activeModuleIds.delete(module.id.id);
            this.showNotification('nexus-module.module-deactivated');
          },
          error: () => {
            this.showNotification('nexus-module.error-deactivating', 'warn');
          }
        });
    } else {
      this.nexusModuleService.activateModuleForTenant(this.tenantId, module.id.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.activeModuleIds.add(module.id.id);
            this.showNotification('nexus-module.module-activated');
          },
          error: () => {
            this.showNotification('nexus-module.error-activating', 'warn');
          }
        });
    }
  }

  getModuleIcon(module: NexusModule): string {
    return module.icon || 'extension';
  }

  private showNotification(messageKey: string, type: 'success' | 'warn' | 'error' = 'success'): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageKey),
      type,
      duration: 2000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }
}
