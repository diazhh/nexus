///
/// Copyright © 2016-2026 The Thingsboard Authors
///

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import { RvReservoir, formatVolume } from '@shared/models/rv/rv.models';
import { RvReservoirDialogComponent } from './rv-reservoir-dialog.component';

@Component({
  selector: 'tb-rv-reservoir-list',
  templateUrl: './rv-reservoir-list.component.html',
  styleUrls: ['./rv-reservoir-list.component.scss']
})
export class RvReservoirListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;

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
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<RvReservoir>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

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
      width: '800px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(reservoir: RvReservoir): void {
    const dialogRef = this.dialog.open(RvReservoirDialogComponent, {
      width: '800px',
      data: { tenantId: this.tenantId, reservoir }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  calculateOOIP(reservoir: RvReservoir): void {
    this.rvService.calculateOOIP(reservoir.assetId).subscribe({
      next: (result) => {
        alert(`OOIP calculado: ${result.ooip_mmbbl.toFixed(2)} MMbbl`);
        this.loadData();
      }
    });
  }

  deleteReservoir(reservoir: RvReservoir): void {
    if (confirm(`Eliminar yacimiento "${reservoir.name}"?`)) {
      this.rvService.deleteReservoir(this.tenantId, reservoir.assetId).subscribe(() => this.loadData());
    }
  }
}
