/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { CTUnitsListComponent } from './ct-units-list.component';
import { CTReelsListComponent } from './ct-reels-list.component';
import { CTJobsListComponent } from './ct-jobs-list.component';
import { CTUnitDetailsComponent } from './ct-unit-details.component';
import { CTReelDetailsComponent } from './ct-reel-details.component';
import { CTJobDetailsComponent } from './ct-job-details.component';
import { CTRealtimeDashboardComponent } from './ct-realtime-dashboard.component';
import { CTFleetDashboardComponent } from './ct-fleet-dashboard.component';
import { CTAnalyticsDashboardComponent } from './ct-analytics-dashboard.component';
import { CTReportsComponent } from './ct-reports.component';

const routes: Routes = [
  {
    path: 'dashboards/realtime',
    component: CTRealtimeDashboardComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.realtime-dashboard',
      breadcrumb: {
        label: 'Real-Time Operations',
        icon: 'dashboard'
      }
    }
  },
  {
    path: 'dashboards/fleet',
    component: CTFleetDashboardComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.fleet-dashboard',
      breadcrumb: {
        label: 'Fleet Management',
        icon: 'dashboard'
      }
    }
  },
  {
    path: 'dashboards/analytics',
    component: CTAnalyticsDashboardComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.analytics-dashboard',
      breadcrumb: {
        label: 'Analytics',
        icon: 'dashboard'
      }
    }
  },
  {
    path: 'reports',
    component: CTReportsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.reports',
      breadcrumb: {
        label: 'Reports',
        icon: 'description'
      }
    }
  },
  {
    path: 'units',
    component: CTUnitsListComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.ct-units',
      breadcrumb: {
        menuId: MenuId.ct_units
      }
    }
  },
  {
    path: 'units/:id',
    component: CTUnitDetailsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.unit-details',
      breadcrumb: {
        label: 'Unit Details',
        icon: 'info'
      }
    }
  },
  {
    path: 'reels',
    component: CTReelsListComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.ct-reels',
      breadcrumb: {
        menuId: MenuId.ct_reels
      }
    }
  },
  {
    path: 'reels/:id',
    component: CTReelDetailsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.reel-details',
      breadcrumb: {
        label: 'Reel Details',
        icon: 'info'
      }
    }
  },
  {
    path: 'jobs',
    component: CTJobsListComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.ct-jobs',
      breadcrumb: {
        menuId: MenuId.ct_jobs
      }
    }
  },
  {
    path: 'jobs/:id',
    component: CTJobDetailsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'ct.job-details',
      breadcrumb: {
        label: 'Job Details',
        icon: 'info'
      }
    }
  },
  {
    path: '',
    redirectTo: 'dashboards/realtime',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CTRoutingModule { }
