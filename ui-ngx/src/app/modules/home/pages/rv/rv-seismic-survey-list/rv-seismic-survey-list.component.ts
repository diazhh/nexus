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

import { Component, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import { RvExportService } from '@core/http/rv/rv-export.service';
import { RvSeismicSurveyDialogComponent } from './rv-seismic-survey-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-seismic-survey-list',
  templateUrl: './rv-seismic-survey-list.component.html',
  styleUrls: ['./rv-seismic-survey-list.component.scss']
})
export class RvSeismicSurveyListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'fieldName', 'surveyType', 'coverage', 'quality', 'interpretationStatus', 'actions'];
  dataSource: MatTableDataSource<any>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  // For displaying field names
  fieldMap: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar
  ) {
    this.dataSource = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadFields();
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadFields(): void {
    this.rvService.getFields(this.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach((f: any) => this.fieldMap.set(f.assetId, f.name));
      }
    });
  }

  loadData(): void {
    this.isLoading = true;
    const textSearch = this.searchText?.trim() || null;
    const pageLink = new PageLink(this.pageSize, this.pageIndex, textSearch);

    this.rvService.getSeismicSurveys(this.tenantId, pageLink).subscribe({
      next: (pageData) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadData();
  }

  applyFilter(): void {
    this.dataSource.filter = this.searchText.trim().toLowerCase();
  }

  getFieldName(fieldId: string): string {
    return this.fieldMap.get(fieldId) || '-';
  }

  getSurveyTypeLabel(type: string): string {
    const labels: any = {
      'TWO_D': '2D',
      'THREE_D': '3D',
      'FOUR_D': '4D',
      'VSP': 'VSP'
    };
    return labels[type] || type;
  }

  getQualityColor(rating: string): string {
    switch (rating) {
      case 'EXCELLENT': return 'primary';
      case 'GOOD': return 'accent';
      case 'FAIR': return '';
      case 'POOR': return 'warn';
      default: return '';
    }
  }

  getQualityLabel(rating: string): string {
    const labels: any = {
      'EXCELLENT': 'Excelente',
      'GOOD': 'Buena',
      'FAIR': 'Regular',
      'POOR': 'Pobre'
    };
    return labels[rating] || rating;
  }

  getStatusLabel(status: string): string {
    const labels: any = {
      'NOT_STARTED': 'No Iniciado',
      'IN_PROGRESS': 'En Progreso',
      'COMPLETED': 'Completado'
    };
    return labels[status] || status;
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvSeismicSurveyDialogComponent, {
      width: '90vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(survey: any): void {
    const dialogRef = this.dialog.open(RvSeismicSurveyDialogComponent, {
      width: '90vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, survey }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  deleteSeismicSurvey(survey: any): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro de eliminar el estudio sísmico "${survey.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteSeismicSurvey(this.tenantId, survey.assetId).subscribe(() => {
          this.snackBar.open('Estudio sísmico eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadData();
        });
      }
    });
  }

  formatNumber(value: number, decimals: number = 1): string {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  }

  exportToCsv(): void {
    this.rvExportService.exportSeismicSurveysToCsv(this.dataSource.data, 'estudios_sismicos');
  }
}
