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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Angular Material Modules
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';

// Routing
import { DrRoutingModule } from './dr-routing.module';

// Components
import { DrRigsListComponent } from './dr-rigs-list.component';
import { DrRigDetailsComponent } from './dr-rig-details.component';
import { DrRunsListComponent } from './dr-runs-list.component';
import { DrRunDetailsComponent } from './dr-run-details.component';
import { DrBhasListComponent } from './dr-bhas-list.component';
import { DrRealtimeDashboardComponent } from './dr-realtime-dashboard.component';
import { DrFleetDashboardComponent } from './dr-fleet-dashboard.component';
import { DrDirectionalDashboardComponent } from './dr-directional-dashboard.component';
import { DrMudlogDashboardComponent } from './dr-mudlog-dashboard.component';
import { DrRigFormDialogComponent } from './dr-rig-form-dialog.component';
import { DrWellcontrolMonitorComponent } from './dr-wellcontrol-monitor.component';

@NgModule({
  declarations: [
    DrRigsListComponent,
    DrRigDetailsComponent,
    DrRunsListComponent,
    DrRunDetailsComponent,
    DrBhasListComponent,
    DrRealtimeDashboardComponent,
    DrFleetDashboardComponent,
    DrDirectionalDashboardComponent,
    DrMudlogDashboardComponent,
    DrRigFormDialogComponent,
    DrWellcontrolMonitorComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DrRoutingModule,
    // Material Modules
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    MatTabsModule,
    MatToolbarModule,
    MatTooltipModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule
  ],
  exports: [
    DrRigsListComponent,
    DrRigDetailsComponent,
    DrRunsListComponent,
    DrRunDetailsComponent,
    DrBhasListComponent,
    DrRealtimeDashboardComponent,
    DrFleetDashboardComponent,
    DrDirectionalDashboardComponent,
    DrMudlogDashboardComponent,
    DrWellcontrolMonitorComponent
  ]
})
export class DrModule { }
