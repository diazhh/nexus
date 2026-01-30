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
import { DataMappingTemplatesComponent } from './data-mapping-templates.component';
import { DataMappingSourcesComponent } from './data-mapping-sources.component';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';

export const dataMappingRoutes: Routes = [
  {
    path: 'dataMapping',
    data: {
      breadcrumb: {
        menuId: MenuId.data_mapping
      }
    },
    children: [
      {
        path: '',
        redirectTo: 'templates',
        pathMatch: 'full'
      },
      {
        path: 'templates',
        component: DataMappingTemplatesComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'data-mapping.mapping-templates',
          breadcrumb: {
            menuId: MenuId.data_mapping_templates
          }
        }
      },
      {
        path: 'sources',
        component: DataMappingSourcesComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'data-mapping.data-sources',
          breadcrumb: {
            menuId: MenuId.data_mapping_sources
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(dataMappingRoutes)],
  exports: [RouterModule]
})
export class DataMappingRoutingModule { }
