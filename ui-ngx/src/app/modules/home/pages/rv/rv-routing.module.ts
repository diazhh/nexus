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
import { RvBasinDetailsComponent } from './rv-basin-details/rv-basin-details.component';
import { RvFieldListComponent } from './rv-field-list/rv-field-list.component';
import { RvFieldDetailsComponent } from './rv-field-details/rv-field-details.component';
import { RvReservoirListComponent } from './rv-reservoir-list/rv-reservoir-list.component';
import { RvReservoirDetailsComponent } from './rv-reservoir-details/rv-reservoir-details.component';
import { RvZoneListComponent } from './rv-zone-list/rv-zone-list.component';
import { RvPvtStudyListComponent } from './rv-pvt-study-list/rv-pvt-study-list.component';
import { RvWellListComponent } from './rv-well-list/rv-well-list.component';
import { RvWellDetailsComponent } from './rv-well-details/rv-well-details.component';
import { RvCompletionListComponent } from './rv-completion-list/rv-completion-list.component';
import { RvMaterialBalanceListComponent } from './rv-material-balance-list/rv-material-balance-list.component';
import { RvIprModelListComponent } from './rv-ipr-model-list/rv-ipr-model-list.component';
import { RvDeclineAnalysisListComponent } from './rv-decline-analysis-list/rv-decline-analysis-list.component';
import { RvWellLogListComponent } from './rv-well-log-list/rv-well-log-list.component';
import { RvCalculatorComponent } from './rv-calculator/rv-calculator.component';
import { RvCoreListComponent } from './rv-core-list/rv-core-list.component';
import { RvFaultListComponent } from './rv-fault-list/rv-fault-list.component';
import { RvSeismicSurveyListComponent } from './rv-seismic-survey-list/rv-seismic-survey-list.component';

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
        path: 'basins/:basinId',
        component: RvBasinDetailsComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.basin-details',
          breadcrumb: {
            label: 'rv.basin-details',
            icon: 'info'
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
        path: 'fields/:fieldId',
        component: RvFieldDetailsComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.field-details',
          breadcrumb: {
            label: 'rv.field-details',
            icon: 'info'
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
        path: 'reservoirs/:reservoirId',
        component: RvReservoirDetailsComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.reservoir-details',
          breadcrumb: {
            label: 'rv.reservoir-details',
            icon: 'info'
          }
        }
      },
      {
        path: 'zones',
        component: RvZoneListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.zones',
          breadcrumb: {
            label: 'rv.zones',
            icon: 'view_column'
          }
        }
      },
      {
        path: 'pvt-studies',
        component: RvPvtStudyListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.pvt-studies',
          breadcrumb: {
            label: 'rv.pvt-studies',
            icon: 'science'
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
        path: 'completions',
        component: RvCompletionListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.completions',
          breadcrumb: {
            label: 'rv.completions',
            icon: 'format_list_bulleted'
          }
        }
      },
      {
        path: 'cores',
        component: RvCoreListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.cores',
          breadcrumb: {
            label: 'rv.cores',
            icon: 'layers'
          }
        }
      },
      {
        path: 'faults',
        component: RvFaultListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.faults',
          breadcrumb: {
            label: 'rv.faults',
            icon: 'waves'
          }
        }
      },
      {
        path: 'seismic-surveys',
        component: RvSeismicSurveyListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.seismic-surveys',
          breadcrumb: {
            label: 'rv.seismic-surveys',
            icon: 'map'
          }
        }
      },
      {
        path: 'well-logs',
        component: RvWellLogListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.well-logs',
          breadcrumb: {
            label: 'rv.well-logs',
            icon: 'show_chart'
          }
        }
      },
      {
        path: 'material-balance',
        component: RvMaterialBalanceListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.material-balance',
          breadcrumb: {
            label: 'rv.material-balance',
            icon: 'assessment'
          }
        }
      },
      {
        path: 'ipr-models',
        component: RvIprModelListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.ipr-models',
          breadcrumb: {
            label: 'rv.ipr-models',
            icon: 'show_chart'
          }
        }
      },
      {
        path: 'decline-analysis',
        component: RvDeclineAnalysisListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'rv.decline-analysis',
          breadcrumb: {
            label: 'rv.decline-analysis',
            icon: 'trending_down'
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
