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
import { RvIprModel, RvWell } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';
import { RvIprModelDialogComponent } from './rv-ipr-model-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-ipr-model-list',
  templateUrl: './rv-ipr-model-list.component.html',
  styleUrls: ['./rv-ipr-model-list.component.scss']
})
export class RvIprModelListComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'name',
    'modelCode',
    'wellName',
    'iprMethod',
    'reservoirPressurePsi',
    'qmaxBopd',
    'productivityIndexBpdPsi',
    'analysisDate',
    'actions'
  ];

  dataSource: MatTableDataSource<RvIprModel>;
  isLoading = false;
  totalElements = 0;
  pageSize = 10;
  tenantId: string;

  wells: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar
  ) {
    this.dataSource = new MatTableDataSource<RvIprModel>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadWells();
    this.loadIprModels();
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

  loadIprModels(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.paginator?.pageIndex || 0);

    this.rvService.getIprModels(this.tenantId, pageLink).subscribe({
      next: (pageData) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading IPR models:', err);
        this.isLoading = false;
      }
    });
  }

  getWellName(wellAssetId: string): string {
    return this.wells.get(wellAssetId) || 'N/A';
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  openDialog(iprModel?: RvIprModel): void {
    const dialogRef = this.dialog.open(RvIprModelDialogComponent, {
      width: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, iprModel }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadIprModels();
      }
    });
  }

  calculateVogel(iprModel: RvIprModel): void {
    if (!iprModel.assetId || !iprModel.reservoirPressurePsi || !iprModel.bubblePointPressurePsi ||
        !iprModel.testRateBopd || !iprModel.testPwfPsi) {
      this.snackBar.open('Faltan datos necesarios para calcular IPR con Vogel', 'Cerrar', { duration: 4000 });
      return;
    }

    const params = {
      reservoirPressure: iprModel.reservoirPressurePsi,
      bubblePointPressure: iprModel.bubblePointPressurePsi,
      testRate: iprModel.testRateBopd,
      testPwf: iprModel.testPwfPsi
    };

    this.rvService.calculateVogelIpr(iprModel.assetId, params).subscribe({
      next: (updated) => {
        this.snackBar.open(`IPR calculado: Qmax = ${updated.qmaxBopd?.toFixed(1)} bopd`, 'Cerrar', { duration: 5000 });
        this.loadIprModels();
      },
      error: (err) => {
        console.error('Error calculating Vogel IPR:', err);
        this.snackBar.open('Error al calcular IPR', 'Cerrar', { duration: 4000 });
      }
    });
  }

  delete(iprModel: RvIprModel): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro que desea eliminar el modelo IPR "${iprModel.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteIprModel(this.tenantId, iprModel.assetId).subscribe({
          next: () => {
            this.snackBar.open('Modelo IPR eliminado correctamente', 'Cerrar', { duration: 3000 });
            this.loadIprModels();
          },
          error: (err) => console.error('Error deleting IPR model:', err)
        });
      }
    });
  }

  onPageChange(event: any): void {
    this.pageSize = event.pageSize;
    this.loadIprModels();
  }

  formatDate(timestamp: number): string {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString();
  }

  getMethodLabel(method: string): string {
    const labels: { [key: string]: string } = {
      'VOGEL': 'Vogel',
      'DARCY': 'Darcy',
      'FETKOVICH': 'Fetkovich',
      'JONES': 'Jones',
      'COMPOSITE': 'Compuesto'
    };
    return labels[method] || method;
  }

  exportToCsv(): void {
    this.rvExportService.exportIprModelsToCsv(this.dataSource.data, 'modelos_ipr');
  }
}
