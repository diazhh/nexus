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
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DRRunService } from '@core/http/dr/dr-run.service';
import { DRRun, RunStatus, HoleSection } from '@shared/models/dr/dr-run.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-dr-runs-list',
  templateUrl: './dr-runs-list.component.html',
  styleUrls: ['./dr-runs-list.component.scss']
})
export class DrRunsListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'runNumber',
    'rigCode',
    'wellName',
    'holeSection',
    'status',
    'currentDepth',
    'avgRop',
    'actions'
  ];

  dataSource: MatTableDataSource<DRRun>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  statusFilter: RunStatus | null = null;
  searchText = '';

  RunStatus = RunStatus;
  HoleSection = HoleSection;

  constructor(
    private store: Store<AppState>,
    private runService: DRRunService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<DRRun>([]);
  }

  ngOnInit() {
    this.loadRuns();
  }

  loadRuns() {
    this.isLoading = true;
    const tenantId = this.getCurrentTenantId();

    if (this.statusFilter) {
      this.runService.getRunsByStatus(tenantId, this.statusFilter).subscribe({
        next: (runs) => {
          this.dataSource.data = runs;
          this.totalElements = runs.length;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading runs:', error);
          this.isLoading = false;
        }
      });
    } else {
      // For all runs, we need a different endpoint - using status IN_PROGRESS as default
      this.runService.getRunsByStatus(tenantId, RunStatus.IN_PROGRESS).subscribe({
        next: (runs) => {
          this.dataSource.data = runs;
          this.totalElements = runs.length;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading runs:', error);
          this.isLoading = false;
        }
      });
    }
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRuns();
  }

  onSearch(searchText: string) {
    this.searchText = searchText;
    this.pageIndex = 0;
    this.loadRuns();
  }

  onStatusFilterChange(status: RunStatus | null) {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadRuns();
  }

  viewDetails(run: DRRun) {
    this.router.navigate(['/dr/runs', run.id.id]);
  }

  createRun() {
    // TODO: Open create run dialog
    console.log('Create new run');
  }

  getStatusColor(status: RunStatus): string {
    switch (status) {
      case RunStatus.IN_PROGRESS:
        return '#4caf50';
      case RunStatus.PLANNED:
        return '#2196f3';
      case RunStatus.COMPLETED:
        return '#9e9e9e';
      case RunStatus.SUSPENDED:
        return '#ff9800';
      case RunStatus.CANCELLED:
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  getStatusLabel(status: RunStatus): string {
    return status.replace(/_/g, ' ');
  }

  getSectionLabel(section: HoleSection): string {
    return section.replace(/_/g, ' ');
  }

  private getCurrentTenantId(): string {
    const authUser = getCurrentAuthUser(this.store);
    return authUser?.tenantId || '';
  }
}
