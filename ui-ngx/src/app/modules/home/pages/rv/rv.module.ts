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
import { RvZoneListComponent } from './rv-zone-list/rv-zone-list.component';
import { RvZoneDialogComponent } from './rv-zone-list/rv-zone-dialog.component';
import { RvPvtStudyListComponent } from './rv-pvt-study-list/rv-pvt-study-list.component';
import { RvPvtStudyDialogComponent } from './rv-pvt-study-list/rv-pvt-study-dialog.component';
import { RvWellListComponent } from './rv-well-list/rv-well-list.component';
import { RvWellDialogComponent } from './rv-well-list/rv-well-dialog.component';
import { RvWellDetailsComponent } from './rv-well-details/rv-well-details.component';
import { RvReservoirDetailsComponent } from './rv-reservoir-details/rv-reservoir-details.component';
import { RvFieldDetailsComponent } from './rv-field-details/rv-field-details.component';
import { RvBasinDetailsComponent } from './rv-basin-details/rv-basin-details.component';
import { RvCompletionListComponent } from './rv-completion-list/rv-completion-list.component';
import { RvCompletionDialogComponent } from './rv-completion-list/rv-completion-dialog.component';
import { RvMaterialBalanceListComponent } from './rv-material-balance-list/rv-material-balance-list.component';
import { RvMaterialBalanceDialogComponent } from './rv-material-balance-list/rv-material-balance-dialog.component';
import { RvIprModelListComponent } from './rv-ipr-model-list/rv-ipr-model-list.component';
import { RvIprModelDialogComponent } from './rv-ipr-model-list/rv-ipr-model-dialog.component';
import { RvDeclineAnalysisListComponent } from './rv-decline-analysis-list/rv-decline-analysis-list.component';
import { RvDeclineAnalysisDialogComponent } from './rv-decline-analysis-list/rv-decline-analysis-dialog.component';
import { RvIprChartComponent } from './rv-charts/rv-ipr-chart.component';
import { RvDeclineChartComponent } from './rv-charts/rv-decline-chart.component';
import { RvMaterialBalanceChartComponent } from './rv-charts/rv-material-balance-chart.component';
import { RvPvtChartComponent } from './rv-charts/rv-pvt-chart.component';
import { RvWellMapComponent } from './rv-charts/rv-well-map.component';
import { RvCalculatorComponent } from './rv-calculator/rv-calculator.component';
import { RvCoreListComponent } from './rv-core-list/rv-core-list.component';
import { RvCoreDialogComponent } from './rv-core-list/rv-core-dialog.component';
import { RvFaultListComponent } from './rv-fault-list/rv-fault-list.component';
import { RvFaultDialogComponent } from './rv-fault-list/rv-fault-dialog.component';
import { RvSeismicSurveyListComponent } from './rv-seismic-survey-list/rv-seismic-survey-list.component';
import { RvSeismicSurveyDialogComponent } from './rv-seismic-survey-list/rv-seismic-survey-dialog.component';
import { RvWellLogListComponent } from './rv-well-log-list/rv-well-log-list.component';
import { RvWellLogDialogComponent } from './rv-well-log-list/rv-well-log-dialog.component';
import { RvWellLogViewerComponent } from './rv-well-log-viewer/rv-well-log-viewer.component';
import { RvWellLogViewerDialogComponent } from './rv-well-log-viewer/rv-well-log-viewer-dialog.component';
import { RvLasImportDialogComponent } from './rv-las-import/rv-las-import-dialog.component';

@NgModule({
  declarations: [
    RvDashboardComponent,
    RvBasinListComponent,
    RvBasinDialogComponent,
    RvFieldListComponent,
    RvFieldDialogComponent,
    RvReservoirListComponent,
    RvReservoirDialogComponent,
    RvZoneListComponent,
    RvZoneDialogComponent,
    RvPvtStudyListComponent,
    RvPvtStudyDialogComponent,
    RvWellListComponent,
    RvWellDialogComponent,
    RvWellDetailsComponent,
    RvReservoirDetailsComponent,
    RvFieldDetailsComponent,
    RvBasinDetailsComponent,
    RvCompletionListComponent,
    RvCompletionDialogComponent,
    RvMaterialBalanceListComponent,
    RvMaterialBalanceDialogComponent,
    RvIprModelListComponent,
    RvIprModelDialogComponent,
    RvDeclineAnalysisListComponent,
    RvDeclineAnalysisDialogComponent,
    RvIprChartComponent,
    RvDeclineChartComponent,
    RvMaterialBalanceChartComponent,
    RvPvtChartComponent,
    RvWellMapComponent,
    RvCalculatorComponent,
    RvCoreListComponent,
    RvCoreDialogComponent,
    RvFaultListComponent,
    RvFaultDialogComponent,
    RvSeismicSurveyListComponent,
    RvSeismicSurveyDialogComponent,
    RvWellLogListComponent,
    RvWellLogDialogComponent,
    RvWellLogViewerComponent,
    RvWellLogViewerDialogComponent,
    RvLasImportDialogComponent
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
    RvZoneListComponent,
    RvPvtStudyListComponent,
    RvWellListComponent
  ]
})
export class RvModule { }
