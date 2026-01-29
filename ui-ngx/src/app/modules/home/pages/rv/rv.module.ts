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
import { SharedModule } from '@shared/shared.module';

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
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatCheckboxModule } from '@angular/material/checkbox';

// Routing Module
import { RvRoutingModule } from './rv-routing.module';

// Components
import { RvDashboardComponent } from './rv-dashboard/rv-dashboard.component';
import { RvBasinListComponent } from './rv-basin-list/rv-basin-list.component';
import { RvBasinDialogComponent } from './rv-basin-list/rv-basin-dialog.component';
import { RvFieldListComponent } from './rv-field-list/rv-field-list.component';
import { RvFieldDialogComponent } from './rv-field-list/rv-field-dialog.component';
import { RvReservoirListComponent } from './rv-reservoir-list/rv-reservoir-list.component';
import { RvReservoirDialogComponent } from './rv-reservoir-list/rv-reservoir-dialog.component';
import { RvWellListComponent } from './rv-well-list/rv-well-list.component';
import { RvWellDialogComponent } from './rv-well-list/rv-well-dialog.component';
import { RvWellDetailsComponent } from './rv-well-details/rv-well-details.component';
import { RvIprChartComponent } from './rv-charts/rv-ipr-chart.component';
import { RvDeclineChartComponent } from './rv-charts/rv-decline-chart.component';
import { RvCalculatorComponent } from './rv-calculator/rv-calculator.component';

@NgModule({
  declarations: [
    RvDashboardComponent,
    RvBasinListComponent,
    RvBasinDialogComponent,
    RvFieldListComponent,
    RvFieldDialogComponent,
    RvReservoirListComponent,
    RvReservoirDialogComponent,
    RvWellListComponent,
    RvWellDialogComponent,
    RvWellDetailsComponent,
    RvIprChartComponent,
    RvDeclineChartComponent,
    RvCalculatorComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    RvRoutingModule,
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
    MatToolbarModule,
    MatTooltipModule,
    MatDividerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressBarModule,
    MatChipsModule,
    MatListModule,
    MatTabsModule,
    MatExpansionModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MatCheckboxModule
  ],
  exports: [
    RvDashboardComponent,
    RvBasinListComponent,
    RvFieldListComponent,
    RvReservoirListComponent,
    RvWellListComponent
  ]
})
export class RvModule { }
