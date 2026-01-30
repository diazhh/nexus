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
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DRBhaService } from '@core/http/dr/dr-bha.service';
import { DRBha, BhaType, BhaStatus } from '@shared/models/dr/dr-bha.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-dr-bhas-list',
  templateUrl: './dr-bhas-list.component.html',
  styleUrls: ['./dr-bhas-list.component.scss']
})
export class DrBhasListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;

  displayedColumns: string[] = ['bhaNumber', 'bhaType', 'bitType', 'bitSizeIn', 'status', 'totalFootage', 'actions'];

  dataSource: MatTableDataSource<DRBha>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  statusFilter: BhaStatus | null = null;
  typeFilter: BhaType | null = null;

  BhaType = BhaType;
  BhaStatus = BhaStatus;

  constructor(
    private store: Store<AppState>,
    private bhaService: DRBhaService,
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource<DRBha>([]);
  }

  ngOnInit() {
    this.loadBhas();
  }

  loadBhas() {
    this.isLoading = true;
    const tenantId = this.getCurrentTenantId();
    const pageLink = new PageLink(this.pageSize, this.pageIndex);

    this.bhaService.getBhas(pageLink, tenantId).subscribe({
      next: (pageData: PageData<DRBha>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading BHAs:', error);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadBhas();
  }

  createBha() {
    console.log('Create new BHA');
  }

  viewBha(bha: DRBha) {
    console.log('View BHA:', bha);
  }

  getStatusColor(status: BhaStatus): string {
    switch (status) {
      case BhaStatus.AVAILABLE: return '#4caf50';
      case BhaStatus.IN_USE: return '#2196f3';
      case BhaStatus.MAINTENANCE: return '#ff9800';
      case BhaStatus.RETIRED: return '#9e9e9e';
      default: return '#9e9e9e';
    }
  }

  getTypeLabel(type: BhaType): string {
    return type.replace(/_/g, ' ');
  }

  private getCurrentTenantId(): string {
    const authUser = getCurrentAuthUser(this.store);
    return authUser?.tenantId || '';
  }
}
