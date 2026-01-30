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
import {
  CanActivate,
  CanActivateChild,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
  UrlTree
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError, take } from 'rxjs/operators';
import { NexusModuleService } from '@core/http/nexus-module.service';
import { AuthService } from '@core/auth/auth.service';
import { Authority } from '@shared/models/authority.enum';
import { ModuleKey } from '@shared/models/nexus-module.models';

/**
 * Guard that checks if the current tenant has access to a specific NEXUS module.
 *
 * Usage in routes:
 * ```typescript
 * {
 *   path: 'ct',
 *   canActivate: [ModuleAccessGuard],
 *   data: { moduleKey: 'CT' },
 *   loadChildren: () => import('./ct/ct.module').then(m => m.CtModule)
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class ModuleAccessGuard implements CanActivate, CanActivateChild {

  constructor(
    private moduleService: NexusModuleService,
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | boolean | UrlTree {
    return this.checkModuleAccess(route);
  }

  canActivateChild(
    childRoute: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | boolean | UrlTree {
    return this.checkModuleAccess(childRoute);
  }

  private checkModuleAccess(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> | boolean | UrlTree {
    const moduleKey: ModuleKey = route.data?.moduleKey;

    // If no moduleKey specified, allow access
    if (!moduleKey) {
      return true;
    }

    // SYS_ADMIN has access to all modules
    const authority = this.authService.getAuthority();
    if (authority === Authority.SYS_ADMIN) {
      return true;
    }

    // For TENANT_ADMIN and CUSTOMER_USER, check module access
    return this.moduleService.hasModuleAccessCached(moduleKey).pipe(
      take(1),
      map(hasAccess => {
        if (hasAccess) {
          return true;
        }
        // Redirect to home or show access denied
        console.warn(`Access denied to module: ${moduleKey}`);
        return this.router.createUrlTree(['/home']);
      }),
      catchError(error => {
        console.error(`Error checking module access for ${moduleKey}:`, error);
        return of(this.router.createUrlTree(['/home']));
      })
    );
  }
}

/**
 * Factory function for creating route data with module key.
 * Helper for route configuration.
 */
export function withModuleAccess(moduleKey: ModuleKey): { moduleKey: ModuleKey } {
  return { moduleKey };
}
