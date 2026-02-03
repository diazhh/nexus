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
import { RvWellLog, RvWell } from '@shared/models/rv/rv.models';
import { RvWellLogDialogComponent } from './rv-well-log-dialog.component';
import { RvWellLogViewerDialogComponent } from '../rv-well-log-viewer/rv-well-log-viewer-dialog.component';
import { RvLasImportDialogComponent } from '../rv-las-import/rv-las-import-dialog.component';

@Component({
  selector: 'tb-rv-well-log-list',
  templateUrl: './rv-well-log-list.component.html',
  styleUrls: ['./rv-well-log-list.component.scss']
})
export class RvWellLogListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['runName', 'wellName', 'logType', 'depthRange', 'curves', 'quality', 'runDate', 'actions'];
  dataSource: MatTableDataSource<RvWellLog>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  // For displaying well names
  wellMap: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<RvWellLog>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadWells();
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadWells(): void {
    this.rvService.getWells(this.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach(r => this.wellMap.set(r.assetId, r.name));
      }
    });
  }

  loadData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

    this.rvService.getWellLogs(this.tenantId, pageLink).subscribe({
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

  getWellName(wellId: string): string {
    return this.wellMap.get(wellId) || '-';
  }

  getDepthRange(wellLog: RvWellLog): string {
    if (wellLog.topDepthMdM && wellLog.bottomDepthMdM) {
      return `${wellLog.topDepthMdM.toFixed(1)} - ${wellLog.bottomDepthMdM.toFixed(1)} m`;
    }
    return '-';
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PRODUCTIVE': return 'primary';
      case 'DEPLETED': return 'warn';
      case 'WATER_OUT': return 'accent';
      case 'BEHIND_PIPE': return '';
      default: return '';
    }
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvWellLogDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(wellLog: RvWellLog): void {
    const dialogRef = this.dialog.open(RvWellLogDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId, wellLog }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  deleteWellLog(wellLog: RvWellLog): void {
    if (confirm(`¿Eliminar registro de pozo "${wellLog.name}"?`)) {
      this.rvService.deleteWellLog(this.tenantId, wellLog.assetId).subscribe(() => this.loadData());
    }
  }

  formatPercent(value: number): string {
    if (value === null || value === undefined) return '-';
    return (value * 100).toFixed(1) + '%';
  }

  formatNumber(value: number, decimals: number = 1): string {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  }

  exportToCsv(): void {
    this.rvExportService.exportWellLogsToCsv(this.dataSource.data, 'registros_pozo');
  }

  openViewerDialog(wellLog: RvWellLog): void {
    this.dialog.open(RvWellLogViewerDialogComponent, {
      width: '90vw',
      maxWidth: '1200px',
      maxHeight: '90vh',
      data: {
        wellLog,
        wellName: this.getWellName(wellLog.wellAssetId)
      }
    });
  }

  openImportLasDialog(): void {
    const dialogRef = this.dialog.open(RvLasImportDialogComponent, {
      width: '90vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadData();
      }
    });
  }
}
