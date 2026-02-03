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

import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { RvService } from '@core/http/rv/rv.service';
import { RvExportService } from '@core/http/rv/rv-export.service';
import { RvDeclineAnalysis, RvWell } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';
import { RvDeclineAnalysisDialogComponent } from './rv-decline-analysis-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-decline-analysis-list',
  templateUrl: './rv-decline-analysis-list.component.html',
  styleUrls: ['./rv-decline-analysis-list.component.scss']
})
export class RvDeclineAnalysisListComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'name',
    'analysisCode',
    'wellName',
    'declineType',
    'qiBopd',
    'diPerYear',
    'eurBbl',
    'remainingReservesBbl',
    'analysisDate',
    'actions'
  ];

  dataSource: MatTableDataSource<RvDeclineAnalysis>;
  isLoading = false;
  totalElements = 0;
  pageSize = 10;
  tenantId: string;
  searchText = '';

  wells: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar
  ) {
    this.dataSource = new MatTableDataSource<RvDeclineAnalysis>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadWells();
    this.loadDeclineAnalyses();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadWells(): void {
    this.rvService.getWells(this.tenantId, new PageLink(1000, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach((well: RvWell) => {
          this.wells.set(well.assetId, well.name);
        });
      }
    });
  }

  loadDeclineAnalyses(): void {
    this.isLoading = true;
    const textSearch = this.searchText?.trim() || null;
    const pageLink = new PageLink(this.pageSize, this.paginator?.pageIndex || 0, textSearch);

    this.rvService.getDeclineAnalyses(this.tenantId, pageLink).subscribe({
      next: (pageData) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading decline analyses:', err);
        this.isLoading = false;
      }
    });
  }

  getWellName(wellAssetId: string): string {
    return this.wells.get(wellAssetId) || 'N/A';
  }

  applyFilter(event: Event): void {
    this.searchText = (event.target as HTMLInputElement).value;
    if (this.paginator) {
      this.paginator.pageIndex = 0; // Reset to first page when filtering
    }
    this.loadDeclineAnalyses();
  }

  openDialog(analysis?: RvDeclineAnalysis): void {
    const dialogRef = this.dialog.open(RvDeclineAnalysisDialogComponent, {
      width: '90vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, analysis }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadDeclineAnalyses();
      }
    });
  }

  performAnalysis(analysis: RvDeclineAnalysis): void {
    if (!analysis.assetId || !analysis.qiBopd || !analysis.diPerYear || analysis.bExponent === undefined) {
      this.snackBar.open('Faltan parámetros necesarios (Qi, Di, b) para realizar el analisis', 'Cerrar', { duration: 4000 });
      return;
    }

    const params = {
      qi: analysis.qiBopd,
      di: analysis.diPerYear / 100,  // Convert to decimal
      b: analysis.bExponent,
      economicLimit: analysis.economicLimitBopd,
      forecastYears: 20
    };

    this.rvService.performDeclineAnalysis(analysis.assetId, params).subscribe({
      next: (updated) => {
        this.snackBar.open(`Analisis completado: EUR = ${updated.eurBbl?.toFixed(0)} bbl, Vida remanente = ${updated.remainingLifeMonths?.toFixed(1)} meses`, 'Cerrar', { duration: 5000 });
        this.loadDeclineAnalyses();
      },
      error: (err) => {
        console.error('Error performing decline analysis:', err);
        this.snackBar.open('Error al realizar el analisis de declinacion', 'Cerrar', { duration: 4000 });
      }
    });
  }

  delete(analysis: RvDeclineAnalysis): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro que desea eliminar el analisis de declinacion "${analysis.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteDeclineAnalysis(this.tenantId, analysis.assetId).subscribe({
          next: () => {
            this.snackBar.open('Analisis de declinacion eliminado correctamente', 'Cerrar', { duration: 3000 });
            this.loadDeclineAnalyses();
          },
          error: (err) => console.error('Error deleting decline analysis:', err)
        });
      }
    });
  }

  onPageChange(event: any): void {
    this.pageSize = event.pageSize;
    this.loadDeclineAnalyses();
  }

  formatDate(timestamp: number): string {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString();
  }

  getDeclineTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'EXPONENTIAL': 'Exponencial',
      'HYPERBOLIC': 'Hiperbólico',
      'HARMONIC': 'Armónico'
    };
    return labels[type] || type;
  }

  formatNumber(value: number, decimals: number = 0): string {
    if (!value) return 'N/A';
    return value.toLocaleString('en-US', { maximumFractionDigits: decimals, minimumFractionDigits: decimals });
  }

  exportToCsv(): void {
    this.rvExportService.exportDeclineAnalysesToCsv(this.dataSource.data, 'analisis_declinacion');
  }
}
