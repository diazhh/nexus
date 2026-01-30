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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DataMappingRoutingModule } from './data-mapping-routing.module';
import { DataMappingTemplatesComponent } from './data-mapping-templates.component';
import { DataMappingSourcesComponent } from './data-mapping-sources.component';
import { MappingTemplateDialogComponent } from './mapping-template-dialog.component';
import { MappingTemplateRulesDialogComponent } from './mapping-template-rules-dialog.component';
import { ApplyTemplateDialogComponent } from './apply-template-dialog.component';

@NgModule({
  declarations: [
    DataMappingTemplatesComponent,
    DataMappingSourcesComponent,
    MappingTemplateDialogComponent,
    MappingTemplateRulesDialogComponent,
    ApplyTemplateDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    DataMappingRoutingModule
  ],
  exports: [
    DataMappingTemplatesComponent,
    DataMappingSourcesComponent
  ]
})
export class DataMappingModule { }
