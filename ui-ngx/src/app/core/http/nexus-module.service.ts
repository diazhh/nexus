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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay, tap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { NexusModule, TenantModule, ModuleKey } from '@shared/models/nexus-module.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class NexusModuleService {

  private readonly BASE_URL = '/api/nexus/module';

  // Cache for module keys - shared across components
  private moduleKeysCache$: Observable<Set<string>> | null = null;

  constructor(private http: HttpClient) {}

  // ========================
  // Module Management (SysAdmin)
  // ========================

  /**
   * Get all modules with pagination (SysAdmin only)
   */
  getModules(pageLink: PageLink, config?: RequestConfig): Observable<PageData<NexusModule>> {
    return this.http.get<PageData<NexusModule>>(
      `${this.BASE_URL}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get module by ID (SysAdmin only)
   */
  getModuleById(moduleId: string, config?: RequestConfig): Observable<NexusModule> {
    return this.http.get<NexusModule>(
      `${this.BASE_URL}/${moduleId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get module by key (SysAdmin only)
   */
  getModuleByKey(moduleKey: string, config?: RequestConfig): Observable<NexusModule> {
    return this.http.get<NexusModule>(
      `${this.BASE_URL}/key/${moduleKey}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get all available modules (SysAdmin only)
   */
  getAvailableModules(config?: RequestConfig): Observable<NexusModule[]> {
    return this.http.get<NexusModule[]>(
      `${this.BASE_URL}/available`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Save module (SysAdmin only)
   */
  saveModule(module: NexusModule, config?: RequestConfig): Observable<NexusModule> {
    return this.http.post<NexusModule>(
      this.BASE_URL,
      module,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Delete module (SysAdmin only)
   */
  deleteModule(moduleId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/${moduleId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ========================
  // Tenant Module Assignment (SysAdmin)
  // ========================

  /**
   * Get modules assigned to a tenant (SysAdmin only)
   */
  getTenantModules(tenantId: string, config?: RequestConfig): Observable<TenantModule[]> {
    return this.http.get<TenantModule[]>(
      `${this.BASE_URL}/tenant/${tenantId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get active modules for a tenant (SysAdmin only)
   */
  getActiveTenantModules(tenantId: string, config?: RequestConfig): Observable<TenantModule[]> {
    return this.http.get<TenantModule[]>(
      `${this.BASE_URL}/tenant/${tenantId}/active`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Assign module to tenant (SysAdmin only)
   */
  assignModuleToTenant(tenantId: string, moduleId: string, config?: RequestConfig): Observable<TenantModule> {
    return this.http.post<TenantModule>(
      `${this.BASE_URL}/tenant/${tenantId}/assign/${moduleId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Unassign module from tenant (SysAdmin only)
   */
  unassignModuleFromTenant(tenantId: string, moduleId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/tenant/${tenantId}/unassign/${moduleId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Activate module for tenant (SysAdmin only)
   */
  activateModuleForTenant(tenantId: string, moduleId: string, config?: RequestConfig): Observable<TenantModule> {
    return this.http.post<TenantModule>(
      `${this.BASE_URL}/tenant/${tenantId}/activate/${moduleId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Deactivate module for tenant (SysAdmin only)
   */
  deactivateModuleForTenant(tenantId: string, moduleId: string, config?: RequestConfig): Observable<TenantModule> {
    return this.http.post<TenantModule>(
      `${this.BASE_URL}/tenant/${tenantId}/deactivate/${moduleId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Count tenants using a module (SysAdmin only)
   */
  countTenantsUsingModule(moduleId: string, config?: RequestConfig): Observable<number> {
    return this.http.get<number>(
      `${this.BASE_URL}/${moduleId}/tenants/count`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ========================
  // Current User APIs
  // ========================

  /**
   * Get modules for current tenant
   */
  getMyModules(config?: RequestConfig): Observable<TenantModule[]> {
    return this.http.get<TenantModule[]>(
      `${this.BASE_URL}/my`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get active module keys for current tenant (with caching)
   */
  getMyModuleKeys(config?: RequestConfig): Observable<Set<string>> {
    if (!this.moduleKeysCache$) {
      this.moduleKeysCache$ = this.http.get<string[]>(
        `${this.BASE_URL}/my/keys`,
        defaultHttpOptionsFromConfig(config)
      ).pipe(
        map(keys => new Set(keys)),
        shareReplay(1),
        catchError(() => of(new Set<string>()))
      );
    }
    return this.moduleKeysCache$;
  }

  /**
   * Check if current tenant has access to a module
   */
  hasModuleAccess(moduleKey: ModuleKey, config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>(
      `${this.BASE_URL}/my/access/${moduleKey}`,
      defaultHttpOptionsFromConfig(config)
    ).pipe(
      catchError(() => of(false))
    );
  }

  /**
   * Check if current tenant has access to a module (using cached keys)
   */
  hasModuleAccessCached(moduleKey: ModuleKey): Observable<boolean> {
    return this.getMyModuleKeys().pipe(
      map(keys => keys.has(moduleKey))
    );
  }

  /**
   * Clear the module keys cache (call after module assignments change)
   */
  clearModuleKeysCache(): void {
    this.moduleKeysCache$ = null;
  }

  /**
   * Refresh the module keys cache
   */
  refreshModuleKeysCache(config?: RequestConfig): Observable<Set<string>> {
    this.clearModuleKeysCache();
    return this.getMyModuleKeys(config);
  }

  // ========================
  // Current Tenant Module Actions
  // ========================

  /**
   * Activate a module for the current tenant
   */
  activateTenantModule(moduleId: string, config?: RequestConfig): Observable<TenantModule> {
    return this.http.post<TenantModule>(
      `${this.BASE_URL}/my/activate/${moduleId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    ).pipe(
      tap(() => this.clearModuleKeysCache())
    );
  }

  /**
   * Deactivate a module for the current tenant
   */
  deactivateTenantModule(moduleId: string, config?: RequestConfig): Observable<TenantModule> {
    return this.http.post<TenantModule>(
      `${this.BASE_URL}/my/deactivate/${moduleId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    ).pipe(
      tap(() => this.clearModuleKeysCache())
    );
  }
}
