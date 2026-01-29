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

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import { RvBasin } from '@shared/models/rv/rv.models';
import { RvBasinDialogComponent } from './rv-basin-dialog.component';

@Component({
  selector: 'tb-rv-basin-list',
  templateUrl: './rv-basin-list.component.html',
  styleUrls: ['./rv-basin-list.component.scss']
})
export class RvBasinListComponent implements OnInit {

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
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<RvBasin>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

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
    this.dataSource.filter = this.searchText.trim().toLowerCase();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvBasinDialogComponent, {
      width: '600px',
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
      width: '600px',
      data: { tenantId: this.tenantId, basin }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadData();
      }
    });
  }

  deleteBasin(basin: RvBasin): void {
    if (confirm(`Esta seguro de eliminar la cuenca "${basin.name}"?`)) {
      this.rvService.deleteBasin(this.tenantId, basin.assetId).subscribe({
        next: () => {
          this.loadData();
        },
        error: (error) => {
          console.error('Error deleting basin:', error);
        }
      });
    }
  }
}
