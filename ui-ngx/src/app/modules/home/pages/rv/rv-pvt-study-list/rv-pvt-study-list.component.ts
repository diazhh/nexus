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
import { RvPvtStudy, RvReservoir } from '@shared/models/rv/rv.models';
import { RvPvtStudyDialogComponent } from './rv-pvt-study-dialog.component';

@Component({
  selector: 'tb-rv-pvt-study-list',
  templateUrl: './rv-pvt-study-list.component.html',
  styleUrls: ['./rv-pvt-study-list.component.scss']
})
export class RvPvtStudyListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'reservoirName', 'sampleDate', 'apiGravity', 'bubblePoint', 'solutionGor', 'oilFvf', 'actions'];
  dataSource: MatTableDataSource<RvPvtStudy>;

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
    this.dataSource = new MatTableDataSource<RvPvtStudy>([]);
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

    this.rvService.getPvtStudies(this.tenantId, pageLink).subscribe({
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

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvPvtStudyDialogComponent, {
      width: '900px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(pvtStudy: RvPvtStudy): void {
    const dialogRef = this.dialog.open(RvPvtStudyDialogComponent, {
      width: '900px',
      data: { tenantId: this.tenantId, pvtStudy }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  deletePvtStudy(pvtStudy: RvPvtStudy): void {
    if (confirm(`¿Eliminar estudio PVT "${pvtStudy.name}"?`)) {
      this.rvService.deletePvtStudy(this.tenantId, pvtStudy.assetId).subscribe(() => this.loadData());
    }
  }

  calculateCorrelations(pvtStudy: RvPvtStudy): void {
    const params = {
      temperature: pvtStudy.sampleTemperatureF || pvtStudy.reservoirTemperatureF || 180,
      rs: pvtStudy.solutionGorAtPbScfStb || pvtStudy.solutionGorScfStb || 500,
      gasGravity: pvtStudy.gasSpecificGravity || pvtStudy.gasGravity || 0.7,
      apiGravity: pvtStudy.apiGravity || 30
    };

    this.rvService.calculatePvtCorrelations(pvtStudy.assetId, params).subscribe({
      next: (result) => {
        alert('Correlaciones PVT calculadas exitosamente');
        this.loadData();
      },
      error: (err) => alert('Error calculando correlaciones: ' + err.message)
    });
  }

  exportToCsv(): void {
    this.rvExportService.exportPvtStudiesToCsv(this.dataSource.data, 'estudios_pvt');
  }
}
