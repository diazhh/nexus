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
import { RvFaultDialogComponent } from './rv-fault-dialog.component';

@Component({
  selector: 'tb-rv-fault-list',
  templateUrl: './rv-fault-list.component.html',
  styleUrls: ['./rv-fault-list.component.scss']
})
export class RvFaultListComponent implements OnInit, AfterViewInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = ['name', 'fieldName', 'faultType', 'geometry', 'sealingPotential', 'compartmentalization', 'actions'];
  dataSource: MatTableDataSource<any>;

  tenantId: string;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  searchText = '';

  // For displaying field names
  fieldMap: Map<string, string> = new Map();

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private rvExportService: RvExportService,
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadFields();
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadFields(): void {
    this.rvService.getFields(this.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => {
        pageData.data.forEach((f: any) => this.fieldMap.set(f.assetId, f.name));
      }
    });
  }

  loadData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

    this.rvService.getFaults(this.tenantId, pageLink).subscribe({
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

  getFieldName(fieldId: string): string {
    return this.fieldMap.get(fieldId) || '-';
  }

  getFaultTypeLabel(type: string): string {
    const labels: any = {
      'NORMAL': 'Normal',
      'REVERSE': 'Inversa',
      'STRIKE_SLIP': 'Rumbo'
    };
    return labels[type] || type;
  }

  getSealingPotentialLabel(potential: string): string {
    const labels: any = {
      'SEALING': 'Sellante',
      'PARTIALLY_SEALING': 'Parcialmente Sellante',
      'NON_SEALING': 'No Sellante'
    };
    return labels[potential] || potential;
  }

  getSealingColor(potential: string): string {
    switch (potential) {
      case 'SEALING': return 'primary';
      case 'PARTIALLY_SEALING': return 'accent';
      case 'NON_SEALING': return 'warn';
      default: return '';
    }
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(RvFaultDialogComponent, {
      width: '900px',
      data: { tenantId: this.tenantId }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  openEditDialog(fault: any): void {
    const dialogRef = this.dialog.open(RvFaultDialogComponent, {
      width: '900px',
      data: { tenantId: this.tenantId, fault }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.loadData(); });
  }

  deleteFault(fault: any): void {
    if (confirm(`¿Eliminar falla "${fault.name}"?`)) {
      this.rvService.deleteFault(this.tenantId, fault.assetId).subscribe(() => this.loadData());
    }
  }

  formatNumber(value: number, decimals: number = 1): string {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  }

  exportToCsv(): void {
    this.rvExportService.exportFaultsToCsv(this.dataSource.data, 'fallas_geologicas');
  }
}
