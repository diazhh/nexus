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
import { RvCompletion, RvWell } from '@shared/models/rv/rv.models';
import { RvCompletionDialogComponent } from './rv-completion-dialog.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-rv-completion-list',
  templateUrl: './rv-completion-list.component.html',
  styleUrls: ['./rv-completion-list.component.scss']
})
export class RvCompletionListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'wellName', 'completionType', 'interval', 'liftMethod', 'currentRate', 'status', 'actions'];
  dataSource: MatTableDataSource<RvCompletion>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  wellMap: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private snackBar: MatSnackBar
  ) {
    this.dataSource = new MatTableDataSource<RvCompletion>([]);
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
    this.rvService.getWells(this.tenantId, new PageLink(200, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach(w => this.wellMap.set(w.assetId, w.name));
      }
    });
  }

  loadData(): void {
    this.isLoading = true;
    const textSearch = this.searchText?.trim() || null;
    const pageLink = new PageLink(this.pageSize, this.pageIndex, textSearch);

    this.rvService.getCompletions(this.tenantId, pageLink).subscribe({
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

  getInterval(completion: RvCompletion): string {
    if (completion.topPerforationMdM && completion.bottomPerforationMdM) {
      return `${completion.topPerforationMdM.toFixed(1)} - ${completion.bottomPerforationMdM.toFixed(1)} m`;
    }
    return '-';
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'primary';
      case 'INACTIVE': return '';
      case 'ISOLATED': return 'accent';
      case 'ABANDONED': return 'warn';
      default: return '';
    }
  }

  getLiftMethodIcon(method: string): string {
    switch (method) {
      case 'NATURAL': return 'water_drop';
      case 'ESP': return 'electric_bolt';
      case 'SRP': return 'pending';
      case 'GAS_LIFT': return 'air';
      case 'JET_PUMP': return 'compress';
      case 'PCP': return 'rotate_right';
      default: return 'help';
    }
  }

  formatNumber(value: number, decimals: number = 0): string {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvCompletionDialogComponent, {
      width: '90vw',
      maxWidth: '850px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(completion: RvCompletion): void {
    const dialogRef = this.dialog.open(RvCompletionDialogComponent, {
      width: '90vw',
      maxWidth: '850px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, completion }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  updateStatus(completion: RvCompletion, newStatus: string): void {
    this.rvService.updateCompletionStatus(completion.assetId, newStatus).subscribe({
      next: () => this.loadData(),
      error: (err) => this.snackBar.open('Error actualizando estado: ' + err.message, 'Cerrar', { duration: 4000 })
    });
  }

  deleteCompletion(completion: RvCompletion): void {
    this.dialogService.confirm(
      'Confirmar eliminación',
      `¿Está seguro de eliminar la completación "${completion.name}"?`,
      'Cancelar',
      'Eliminar'
    ).subscribe(result => {
      if (result) {
        this.rvService.deleteCompletion(this.tenantId, completion.assetId).subscribe(() => {
          this.snackBar.open('Completación eliminada correctamente', 'Cerrar', { duration: 3000 });
          this.loadData();
        });
      }
    });
  }

  exportToCsv(): void {
    this.rvExportService.exportCompletionsToCsv(this.dataSource.data, 'completaciones');
  }
}
