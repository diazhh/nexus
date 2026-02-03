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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import { RvExportService } from '@core/http/rv/rv-export.service';
import { RvReservoir } from '@shared/models/rv/rv.models';
import { RvMaterialBalanceDialogComponent } from './rv-material-balance-dialog.component';

export interface RvMaterialBalance {
  assetId?: string;
  name: string;
  reservoirAssetId?: string;
  analysisDate?: number;
  initialPressurePsi?: number;
  bubblePointPressurePsi?: number;
  initialTemperatureF?: number;
  initialWaterSatFrac?: number;
  initialOilFvfRbStb?: number;
  calculatedOoipMmbbl?: number;
  driveMechanism?: string;
  driveIndices?: any;
  dataPoints?: any[];
}

@Component({
  selector: 'tb-rv-material-balance-list',
  templateUrl: './rv-material-balance-list.component.html',
  styleUrls: ['./rv-material-balance-list.component.scss']
})
export class RvMaterialBalanceListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'reservoirName', 'analysisDate', 'initialPressure', 'calculatedOoip', 'driveMechanism', 'actions'];
  dataSource: MatTableDataSource<RvMaterialBalance>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  reservoirMap: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<RvMaterialBalance>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadReservoirs();
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadReservoirs(): void {
    this.rvService.getReservoirs(this.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach(r => this.reservoirMap.set(r.assetId, r.name));
      }
    });
  }

  loadData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

    this.rvService.getMaterialBalanceStudies(this.tenantId, pageLink).subscribe({
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

  getReservoirName(reservoirId: string): string {
    return this.reservoirMap.get(reservoirId) || '-';
  }

  formatDate(timestamp: number): string {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleDateString();
  }

  formatNumber(value: number, decimals: number = 1): string {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  }

  getDriveMechanismColor(mechanism: string): string {
    switch (mechanism) {
      case 'WATER_DRIVE': return 'primary';
      case 'GAS_CAP': return 'accent';
      case 'SOLUTION_GAS': return 'warn';
      case 'COMBINATION': return '';
      default: return '';
    }
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvMaterialBalanceDialogComponent, {
      width: '900px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(mbe: RvMaterialBalance): void {
    const dialogRef = this.dialog.open(RvMaterialBalanceDialogComponent, {
      width: '900px',
      data: { tenantId: this.tenantId, materialBalance: mbe }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  runAnalysis(mbe: RvMaterialBalance): void {
    this.rvService.performHavlenaOdehAnalysis(mbe.assetId, mbe).subscribe({
      next: (result) => {
        alert(`Analisis completado. OOIP calculado: ${result.calculatedOoipMmbbl?.toFixed(2) || 'N/A'} MMbbl`);
        this.loadData();
      },
      error: (err) => alert('Error ejecutando analisis: ' + err.message)
    });
  }

  deleteMaterialBalance(mbe: RvMaterialBalance): void {
    if (confirm(`¿Eliminar estudio de Material Balance "${mbe.name}"?`)) {
      this.rvService.deleteMaterialBalanceStudy(this.tenantId, mbe.assetId).subscribe(() => this.loadData());
    }
  }

  exportToCsv(): void {
    this.rvExportService.exportMaterialBalancesToCsv(this.dataSource.data, 'balance_materiales');
  }
}
