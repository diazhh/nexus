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
import { RvZone, RvReservoir } from '@shared/models/rv/rv.models';
import { RvZoneDialogComponent } from './rv-zone-dialog.component';

@Component({
  selector: 'tb-rv-zone-list',
  templateUrl: './rv-zone-list.component.html',
  styleUrls: ['./rv-zone-list.component.scss']
})
export class RvZoneListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'reservoirName', 'depthRange', 'thickness', 'porosity', 'permeability', 'waterSat', 'status', 'actions'];
  dataSource: MatTableDataSource<RvZone>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  // For displaying reservoir names
  reservoirMap: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<RvZone>([]);
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

    this.rvService.getZones(this.tenantId, pageLink).subscribe({
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

  getDepthRange(zone: RvZone): string {
    if (zone.topDepthMdM && zone.bottomDepthMdM) {
      return `${zone.topDepthMdM.toFixed(1)} - ${zone.bottomDepthMdM.toFixed(1)} m`;
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
    const dialogRef = this.dialog.open(RvZoneDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(zone: RvZone): void {
    const dialogRef = this.dialog.open(RvZoneDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId, zone }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  deleteZone(zone: RvZone): void {
    if (confirm(`¿Eliminar zona "${zone.name}"?`)) {
      this.rvService.deleteZone(this.tenantId, zone.assetId).subscribe(() => this.loadData());
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
    this.rvExportService.exportZonesToCsv(this.dataSource.data, 'zonas_rv');
  }
}
