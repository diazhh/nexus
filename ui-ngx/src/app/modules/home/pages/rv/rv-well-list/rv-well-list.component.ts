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
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import { RvWell, formatOilRate, getWellStatusIcon } from '@shared/models/rv/rv.models';
import { RvWellDialogComponent } from './rv-well-dialog.component';

@Component({
  selector: 'tb-rv-well-list',
  templateUrl: './rv-well-list.component.html',
  styleUrls: ['./rv-well-list.component.scss']
})
export class RvWellListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;

  displayedColumns: string[] = ['name', 'wellType', 'wellStatus', 'wellCategory', 'currentRateBopd', 'currentWaterCutPercent', 'actions'];
  dataSource: MatTableDataSource<RvWell>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  formatOilRate = formatOilRate;
  getWellStatusIcon = getWellStatusIcon;

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<RvWell>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

    this.rvService.getWells(this.tenantId, pageLink).subscribe({
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
    const dialogRef = this.dialog.open(RvWellDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(well: RvWell): void {
    const dialogRef = this.dialog.open(RvWellDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId, well }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  viewDetails(well: RvWell): void {
    this.router.navigate(['/rv/wells', well.assetId]);
  }

  deleteWell(well: RvWell): void {
    if (confirm(`Eliminar pozo "${well.name}"?`)) {
      this.rvService.deleteWell(this.tenantId, well.assetId).subscribe(() => this.loadData());
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PRODUCING': return 'primary';
      case 'DRILLING': return 'accent';
      case 'COMPLETING': return 'accent';
      case 'SHUT_IN': return 'warn';
      default: return '';
    }
  }
}
