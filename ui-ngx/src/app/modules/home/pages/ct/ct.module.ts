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
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
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
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { CTRoutingModule } from './ct-routing.module';
import { CTUnitsListComponent } from './ct-units-list.component';
import { CTReelsListComponent } from './ct-reels-list.component';
import { CTJobsListComponent } from './ct-jobs-list.component';
import { CTJobSimulationDialogComponent } from './ct-job-simulation-dialog.component';
import { CTFatigueHistoryDialogComponent } from './ct-fatigue-history-dialog.component';
import { CTUnitDetailsComponent } from './ct-unit-details.component';
import { CTReelDetailsComponent } from './ct-reel-details.component';
import { CTJobDetailsComponent } from './ct-job-details.component';
import { CTUnitFormDialogComponent } from './ct-unit-form-dialog.component';
import { CTReelFormDialogComponent } from './ct-reel-form-dialog.component';
import { CTJobFormDialogComponent } from './ct-job-form-dialog.component';
import { CTRealtimeDashboardComponent } from './ct-realtime-dashboard.component';
import { CTFleetDashboardComponent } from './ct-fleet-dashboard.component';
import { CTAnalyticsDashboardComponent } from './ct-analytics-dashboard.component';
import { CTReportsComponent } from './ct-reports.component';

@NgModule({
  declarations: [
    CTUnitsListComponent,
    CTReelsListComponent,
    CTJobsListComponent,
    CTJobSimulationDialogComponent,
    CTFatigueHistoryDialogComponent,
    CTUnitDetailsComponent,
    CTReelDetailsComponent,
    CTJobDetailsComponent,
    CTUnitFormDialogComponent,
    CTReelFormDialogComponent,
    CTJobFormDialogComponent,
    CTRealtimeDashboardComponent,
    CTFleetDashboardComponent,
    CTAnalyticsDashboardComponent,
    CTReportsComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    CTRoutingModule,
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
    MatToolbarModule,
    MatTooltipModule,
    MatDividerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressBarModule
  ],
  exports: [
    CTUnitsListComponent,
    CTReelsListComponent,
    CTJobsListComponent
  ]
})
export class CTModule { }
