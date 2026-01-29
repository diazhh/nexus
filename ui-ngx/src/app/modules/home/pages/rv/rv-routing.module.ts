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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';

import { RvDashboardComponent } from './rv-dashboard/rv-dashboard.component';
import { RvBasinListComponent } from './rv-basin-list/rv-basin-list.component';
import { RvFieldListComponent } from './rv-field-list/rv-field-list.component';
import { RvReservoirListComponent } from './rv-reservoir-list/rv-reservoir-list.component';
import { RvWellListComponent } from './rv-well-list/rv-well-list.component';
import { RvWellDetailsComponent } from './rv-well-details/rv-well-details.component';
import { RvCalculatorComponent } from './rv-calculator/rv-calculator.component';

export const rvRoutes: Routes = [
  {
    path: 'rv',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'rv.reservoir-module',
      breadcrumb: {
        label: 'rv.reservoir-module',
        icon: 'oil_barrel'
      }
    },
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: RvDashboardComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.dashboard',
          breadcrumb: {
            label: 'rv.dashboard',
            icon: 'dashboard'
          }
        }
      },
      {
        path: 'basins',
        component: RvBasinListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.basins',
          breadcrumb: {
            label: 'rv.basins',
            icon: 'terrain'
          }
        }
      },
      {
        path: 'fields',
        component: RvFieldListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.fields',
          breadcrumb: {
            label: 'rv.fields',
            icon: 'location_on'
          }
        }
      },
      {
        path: 'reservoirs',
        component: RvReservoirListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.reservoirs',
          breadcrumb: {
            label: 'rv.reservoirs',
            icon: 'layers'
          }
        }
      },
      {
        path: 'wells',
        component: RvWellListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.wells',
          breadcrumb: {
            label: 'rv.wells',
            icon: 'build'
          }
        }
      },
      {
        path: 'wells/:wellId',
        component: RvWellDetailsComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.well-details',
          breadcrumb: {
            label: 'rv.well-details',
            icon: 'info'
          }
        }
      },
      {
        path: 'calculator',
        component: RvCalculatorComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.calculator',
          breadcrumb: {
            label: 'rv.calculator',
            icon: 'calculate'
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(rvRoutes)],
  exports: [RouterModule]
})
export class RvRoutingModule { }
