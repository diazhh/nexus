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
import { RoleComponent } from '@modules/home/pages/role/role.component';
import { RoleRoutingModule } from '@modules/home/pages/role/role-routing.module';
import { RoleDialogComponent } from '@modules/home/pages/role/role-dialog.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { RoleTabsComponent } from '@home/pages/role/role-tabs.component';
import { PermissionMatrixComponent } from '@modules/home/pages/role/permission-matrix.component';

@NgModule({
  declarations: [
    RoleComponent,
    RoleTabsComponent,
    RoleDialogComponent,
    PermissionMatrixComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    RoleRoutingModule
  ]
})
export class RoleModule { }
