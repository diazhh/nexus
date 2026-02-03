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
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import { RvExportService } from '@core/http/rv/rv-export.service';
import { RvReservoir, formatVolume } from '@shared/models/rv/rv.models';
import { RvReservoirDialogComponent } from './rv-reservoir-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-reservoir-list',
  templateUrl: './rv-reservoir-list.component.html',
  styleUrls: ['./rv-reservoir-list.component.scss']
})
export class RvReservoirListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'formationName', 'lithology', 'reservoirType', 'ooipMmbbl', 'recoveryFactorPercent', 'actions'];
  dataSource: MatTableDataSource<RvReservoir>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  formatVolume = formatVolume;

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<RvReservoir>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadData(): void {
    this.isLoading = true;
    const textSearch = this.searchText?.trim() || null;
    const pageLink = new PageLink(this.pageSize, this.pageIndex, textSearch);

    this.rvService.getReservoirs(this.tenantId, pageLink).subscribe({
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

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvReservoirDialogComponent, {
      width: '90vw',
      maxWidth: '800px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(reservoir: RvReservoir): void {
    const dialogRef = this.dialog.open(RvReservoirDialogComponent, {
      width: '90vw',
      maxWidth: '800px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, reservoir }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  viewDetails(reservoir: RvReservoir): void {
    this.router.navigate(['/rv/reservoirs', reservoir.assetId]);
  }

  calculateOOIP(reservoir: RvReservoir): void {
    this.rvService.calculateOOIP(reservoir.assetId).subscribe({
      next: (result) => {
        this.snackBar.open(`OOIP calculado: ${result.ooip_mmbbl.toFixed(2)} MMbbl`, 'Cerrar', { duration: 5000 });
        this.loadData();
      }
    });
  }

  deleteReservoir(reservoir: RvReservoir): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro de eliminar el yacimiento "${reservoir.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteReservoir(this.tenantId, reservoir.assetId).subscribe(() => {
          this.snackBar.open('Yacimiento eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadData();
        });
      }
    });
  }

  exportToCsv(): void {
    this.rvExportService.exportReservoirsToCsv(this.dataSource.data, 'yacimientos');
  }
}
