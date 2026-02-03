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
import { RvBasin } from '@shared/models/rv/rv.models';
import { RvBasinDialogComponent } from './rv-basin-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-basin-list',
  templateUrl: './rv-basin-list.component.html',
  styleUrls: ['./rv-basin-list.component.scss']
})
export class RvBasinListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'code', 'basinType', 'country', 'areaKm2', 'actions'];
  dataSource: MatTableDataSource<RvBasin>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<RvBasin>([]);
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

    this.rvService.getBasins(this.tenantId, pageLink).subscribe({
      next: (pageData) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading basins:', error);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadData();
  }

  applyFilter(): void {
    this.pageIndex = 0; // Reset to first page when filtering
    this.loadData();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvBasinDialogComponent, {
      width: '90vw',
      maxWidth: '600px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadData();
      }
    });
  }

  openEditDialog(basin: RvBasin): void {
    const dialogRef = this.dialog.open(RvBasinDialogComponent, {
      width: '90vw',
      maxWidth: '600px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, basin }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadData();
      }
    });
  }

  viewDetails(basin: RvBasin): void {
    this.router.navigate(['/rv/basins', basin.assetId]);
  }

  deleteBasin(basin: RvBasin): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro de eliminar la cuenca "${basin.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteBasin(this.tenantId, basin.assetId).subscribe({
          next: () => {
            this.snackBar.open('Cuenca eliminada correctamente', 'Cerrar', { duration: 3000 });
            this.loadData();
          },
          error: (error) => {
            console.error('Error deleting basin:', error);
          }
        });
      }
    });
  }
}
