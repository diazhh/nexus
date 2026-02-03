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
import { RvCoreDialogComponent } from './rv-core-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-core-list',
  templateUrl: './rv-core-list.component.html',
  styleUrls: ['./rv-core-list.component.scss']
})
export class RvCoreListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'wellName', 'coreType', 'depthRange', 'recoveredLength', 'porosity', 'permeability', 'lithology', 'actions'];
  dataSource: MatTableDataSource<any>;

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
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar
  ) {
    this.dataSource = new MatTableDataSource<any>([]);
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
    this.rvService.getWells(this.tenantId, new PageLink(1000, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach((w: any) => this.wellMap.set(w.assetId, w.name));
      }
    });
  }

  loadData(): void {
    this.isLoading = true;
    const textSearch = this.searchText?.trim() || null;
    const pageLink = new PageLink(this.pageSize, this.pageIndex, textSearch);

    this.rvService.getCores(this.tenantId, pageLink).subscribe({
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

  getDepthRange(core: any): string {
    if (core.topDepthMd && core.bottomDepthMd) {
      return `${core.topDepthMd.toFixed(1)} - ${core.bottomDepthMd.toFixed(1)} m`;
    }
    return '-';
  }

  getCoreTypeLabel(type: string): string {
    const labels: any = {
      'CONVENTIONAL': 'Convencional',
      'SIDEWALL': 'Sidewall'
    };
    return labels[type] || type;
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvCoreDialogComponent, {
      width: '90vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(core: any): void {
    const dialogRef = this.dialog.open(RvCoreDialogComponent, {
      width: '90vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, core }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  deleteCore(core: any): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro de eliminar la muestra de núcleo "${core.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteCore(this.tenantId, core.assetId).subscribe(() => {
          this.snackBar.open('Núcleo eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadData();
        });
      }
    });
  }

  formatPercent(value: number): string {
    if (value === null || value === undefined) return '-';
    return (value * 100).toFixed(1) + '%';
  }

  formatNumber(value: number, decimals: number = 2): string {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  }

  exportToCsv(): void {
    this.rvExportService.exportCoresToCsv(this.dataSource.data, 'muestras_nucleo');
  }
}
