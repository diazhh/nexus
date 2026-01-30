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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { NexusModuleService } from '@core/http/nexus-module.service';
import { NexusModule, TenantModule } from '@shared/models/nexus-module.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatTableDataSource } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-nexus-modules',
  templateUrl: './nexus-modules.component.html',
  styleUrls: ['./nexus-modules.component.scss']
})
export class NexusModulesComponent extends PageComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['icon', 'moduleKey', 'name', 'category', 'version', 'isActive', 'actions'];
  dataSource = new MatTableDataSource<NexusModule>();
  tenantModules: TenantModule[] = [];
  tenantModuleMap: Map<string, TenantModule> = new Map();

  isLoading = true;
  isSysAdmin = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    protected store: Store<AppState>,
    private nexusModuleService: NexusModuleService,
    private translate: TranslateService,
    private dialogService: DialogService
  ) {
    super(store);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.isSysAdmin = authUser?.authority === Authority.SYS_ADMIN;

    this.loadModules();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadModules(): void {
    this.isLoading = true;

    if (this.isSysAdmin) {
      // Sys admin sees all available modules
      this.nexusModuleService.getAvailableModules()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (modules) => {
            this.dataSource.data = modules;
            this.isLoading = false;
          },
          error: () => {
            this.isLoading = false;
          }
        });
    } else {
      // Tenant admin sees tenant modules
      this.nexusModuleService.getMyModules()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (tenantModules) => {
            this.tenantModules = tenantModules;
            this.tenantModuleMap.clear();
            tenantModules.forEach(tm => {
              if (tm.moduleId) {
                this.tenantModuleMap.set(tm.moduleId.id, tm);
              }
            });

            // Get full module details for each tenant module
            const moduleIds = tenantModules.map(tm => tm.moduleId?.id).filter(id => !!id) as string[];
            if (moduleIds.length > 0) {
              // Load modules one by one and combine using forkJoin
              import('rxjs').then(({ forkJoin }) => {
                const moduleObservables = moduleIds.map(id =>
                  this.nexusModuleService.getModuleById(id)
                );
                forkJoin(moduleObservables)
                  .pipe(takeUntil(this.destroy$))
                  .subscribe({
                    next: (modules) => {
                      this.dataSource.data = modules.filter(m => !!m) as NexusModule[];
                      this.isLoading = false;
                    },
                    error: () => {
                      this.isLoading = false;
                    }
                  });
              });
            } else {
              this.dataSource.data = [];
              this.isLoading = false;
            }
          },
          error: () => {
            this.isLoading = false;
          }
        });
    }
  }

  isModuleActive(module: NexusModule): boolean {
    if (this.isSysAdmin) {
      return module.available;
    }
    const tenantModule = this.tenantModuleMap.get(module.id.id);
    return tenantModule?.active ?? false;
  }

  toggleModuleStatus(module: NexusModule): void {
    const isActive = this.isModuleActive(module);
    const confirmKey = isActive ? 'nexus-module.confirm-deactivate' : 'nexus-module.confirm-activate';

    this.dialogService.confirm(
      this.translate.instant(isActive ? 'nexus-module.deactivate-module' : 'nexus-module.activate-module'),
      this.translate.instant(confirmKey)
    ).subscribe(result => {
      if (result) {
        if (this.isSysAdmin) {
          // Sys admin can toggle availability
          // This would require additional API endpoint
        } else {
          // Tenant admin toggles activation
          if (isActive) {
            this.nexusModuleService.deactivateTenantModule(module.id.id)
              .pipe(takeUntil(this.destroy$))
              .subscribe(() => this.loadModules());
          } else {
            this.nexusModuleService.activateTenantModule(module.id.id)
              .pipe(takeUntil(this.destroy$))
              .subscribe(() => this.loadModules());
          }
        }
      }
    });
  }

  getModuleIcon(module: NexusModule): string {
    return module.icon || 'extension';
  }

  getCategoryLabel(category: string): string {
    const categoryKey = `nexus-module.categories.${category?.toLowerCase() || 'operations'}`;
    return this.translate.instant(categoryKey);
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }
}
