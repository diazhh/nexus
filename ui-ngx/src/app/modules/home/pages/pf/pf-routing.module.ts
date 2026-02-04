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
import { MenuId } from '@core/services/menu.models';
import { PfWellsListComponent } from './pf-wells-list/pf-wells-list.component';
import { PfWellDetailComponent } from './pf-well-detail/pf-well-detail.component';
import { PfAlarmsListComponent } from './pf-alarms-list/pf-alarms-list.component';

export const pfRoutes: Routes = [
  {
    path: 'pf',
    children: [
      {
        path: 'wells',
        component: PfWellsListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'pf.wells',
          breadcrumb: {
            menuId: MenuId.pf_wells
          }
        }
      },
      {
        path: 'wells/:wellId',
        component: PfWellDetailComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'pf.well-details',
          breadcrumb: {
            label: 'Well Details',
            icon: 'info'
          }
        }
      },
      {
        path: 'alarms',
        component: PfAlarmsListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'pf.alarms',
          breadcrumb: {
            menuId: MenuId.pf_alarms
          }
        }
      },
      {
        path: '',
        redirectTo: 'wells',
        pathMatch: 'full'
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(pfRoutes)],
  exports: [RouterModule]
})
export class PfRoutingModule { }
