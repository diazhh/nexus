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
import { PoRecommendationsListComponent } from './po-recommendations-list/po-recommendations-list.component';
import { PoHealthDashboardComponent } from './po-health-dashboard/po-health-dashboard.component';
import { PoMlConfigComponent } from './po-ml-config/po-ml-config.component';
import { PoMlTrainingComponent } from './po-ml-training/po-ml-training.component';
import { PoPredictionDetailComponent } from './po-prediction-detail/po-prediction-detail.component';

export const poRoutes: Routes = [
  {
    path: 'po',
    children: [
      {
        path: 'health',
        component: PoHealthDashboardComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'po.health-dashboard',
          breadcrumb: {
            menuId: MenuId.po_health_dashboard
          }
        }
      },
      {
        path: 'recommendations',
        component: PoRecommendationsListComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'po.recommendations',
          breadcrumb: {
            menuId: MenuId.po_recommendations
          }
        }
      },
      {
        path: 'ml-config',
        component: PoMlConfigComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'po.ml.config.title',
          breadcrumb: {
            menuId: MenuId.po_ml_config
          }
        }
      },
      {
        path: 'ml-training',
        component: PoMlTrainingComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'po.ml.training.title',
          breadcrumb: {
            menuId: MenuId.po_ml_training
          }
        }
      },
      {
        path: 'prediction/:wellId',
        component: PoPredictionDetailComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'po.ml.prediction.detail',
          breadcrumb: {
            menuId: MenuId.po_prediction_detail
          }
        }
      },
      {
        path: '',
        redirectTo: 'health',
        pathMatch: 'full'
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(poRoutes)],
  exports: [RouterModule]
})
export class PoRoutingModule { }
