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
import { DrRigsListComponent } from './dr-rigs-list.component';
import { DrRigDetailsComponent } from './dr-rig-details.component';
import { DrRunsListComponent } from './dr-runs-list.component';
import { DrRunDetailsComponent } from './dr-run-details.component';
import { DrBhasListComponent } from './dr-bhas-list.component';
import { DrRealtimeDashboardComponent } from './dr-realtime-dashboard.component';
import { DrDirectionalDashboardComponent } from './dr-directional-dashboard.component';
import { DrMudlogDashboardComponent } from './dr-mudlog-dashboard.component';
import { DrFleetDashboardComponent } from './dr-fleet-dashboard.component';
import { DrWellcontrolMonitorComponent } from './dr-wellcontrol-monitor.component';

export const drRoutes: Routes = [
  {
    path: 'dr',
    children: [
      {
        path: 'dashboards/realtime',
        component: DrRealtimeDashboardComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.realtime-dashboard',
          breadcrumb: {
            label: 'Real-Time Drilling',
            icon: 'dashboard'
          }
        }
      },
      {
        path: 'dashboards/fleet',
        component: DrFleetDashboardComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.fleet-dashboard',
          breadcrumb: {
            label: 'Fleet Management',
            icon: 'dashboard'
          }
        }
      },
      {
        path: 'dashboards/directional',
        component: DrDirectionalDashboardComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.directional-dashboard',
          breadcrumb: {
            label: 'Directional Drilling',
            icon: 'dashboard'
          }
        }
      },
      {
        path: 'dashboards/mudlog',
        component: DrMudlogDashboardComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.mudlog-dashboard',
          breadcrumb: {
            label: 'Mud Logging',
            icon: 'dashboard'
          }
        }
      },
      {
        path: 'dashboards/wellcontrol',
        component: DrWellcontrolMonitorComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.wellcontrol-monitor',
          breadcrumb: {
            label: 'Well Control',
            icon: 'security'
          }
        }
      },
      {
        path: 'rigs',
        component: DrRigsListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.dr-rigs',
          breadcrumb: {
            menuId: MenuId.dr_rigs
          }
        }
      },
      {
        path: 'rigs/:id',
        component: DrRigDetailsComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.rig-details',
          breadcrumb: {
            label: 'Rig Details',
            icon: 'info'
          }
        }
      },
      {
        path: 'runs',
        component: DrRunsListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.dr-runs',
          breadcrumb: {
            menuId: MenuId.dr_runs
          }
        }
      },
      {
        path: 'runs/:id',
        component: DrRunDetailsComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.run-details',
          breadcrumb: {
            label: 'Run Details',
            icon: 'info'
          }
        }
      },
      {
        path: 'bhas',
        component: DrBhasListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'dr.dr-bhas',
          breadcrumb: {
            menuId: MenuId.dr_bhas
          }
        }
      },
      {
        path: '',
        redirectTo: 'dashboards/realtime',
        pathMatch: 'full'
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(drRoutes)],
  exports: [RouterModule]
})
export class DrRoutingModule { }
