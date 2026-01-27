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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { CTReel, ReelStatus } from '@shared/models/ct/ct-reel.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { CTFatigueHistoryDialogComponent } from './ct-fatigue-history-dialog.component';
import { CTTemplateSelectorDialogComponent } from './ct-template-selector-dialog.component';
import { CTReelTemplateFormDialogComponent } from './ct-reel-template-form-dialog.component';

@Component({
  selector: 'tb-ct-reels-list',
  templateUrl: './ct-reels-list.component.html',
  styleUrls: ['./ct-reels-list.component.scss']
})
export class CTReelsListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'reelCode',
    'reelName',
    'status',
    'materialGrade',
    'totalLengthFt',
    'accumulatedFatiguePercent',
    'totalCycles',
    'currentLocation',
    'actions'
  ];

  dataSource: MatTableDataSource<CTReel>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  statusFilter: ReelStatus | null = null;
  searchText = '';

  ReelStatus = ReelStatus;

  constructor(
    private store: Store<AppState>,
    private reelService: CTReelService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<CTReel>([]);
  }

  ngOnInit() {
    this.loadReels();
  }

  loadReels() {
    this.isLoading = true;
    
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.searchText);
    const tenantId = this.getCurrentTenantId();

    this.reelService.getReels(pageLink, tenantId).subscribe({
      next: (pageData: PageData<CTReel>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading reels:', error);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadReels();
  }

  onSearch(searchText: string) {
    this.searchText = searchText;
    this.pageIndex = 0;
    this.loadReels();
  }

  onStatusFilterChange(status: ReelStatus | null) {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadReels();
  }

  viewDetails(reel: CTReel) {
    this.router.navigate(['/ct/reels', reel.id.id]);
  }

  viewFatigueHistory(reel: CTReel) {
    this.dialog.open(CTFatigueHistoryDialogComponent, {
      width: '1000px',
      maxHeight: '90vh',
      data: {
        reelId: reel.id.id,
        reelCode: reel.reelCode
      }
    });
  }

  createReel() {
    const dialogRef = this.dialog.open(CTTemplateSelectorDialogComponent, {
      width: '600px',
      data: { category: 'CT_REEL', type: 'reel' }
    });

    dialogRef.afterClosed().subscribe(template => {
      if (template) {
        this.openCreateReelForm(template);
      }
    });
  }

  private openCreateReelForm(template: any) {
    const dialogRef = this.dialog.open(CTReelTemplateFormDialogComponent, {
      width: '800px',
      data: { 
        template: template,
        tenantId: this.getCurrentTenantId()
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadReels();
      }
    });
  }

  editReel(reel: CTReel) {
    // TODO: Open edit reel dialog
    console.log('Edit reel:', reel);
  }

  deleteReel(reel: CTReel) {
    // TODO: Confirm and delete reel
    console.log('Delete reel:', reel);
  }

  getStatusColor(status: ReelStatus): string {
    switch (status) {
      case ReelStatus.AVAILABLE:
        return 'green';
      case ReelStatus.IN_USE:
        return 'blue';
      case ReelStatus.MAINTENANCE:
        return 'orange';
      case ReelStatus.RETIRED:
        return 'red';
      default:
        return 'gray';
    }
  }

  getStatusLabel(status: ReelStatus): string {
    return status.replace(/_/g, ' ');
  }

  getFatigueColor(fatigue: number): string {
    if (fatigue >= 95) return '#f44336';
    if (fatigue >= 80) return '#ff9800';
    if (fatigue >= 60) return '#ffc107';
    return '#4caf50';
  }

  getFatigueClass(fatigue: number): string {
    if (fatigue >= 95) return 'critical';
    if (fatigue >= 80) return 'high';
    if (fatigue >= 60) return 'medium';
    return 'low';
  }

  private getCurrentTenantId(): string {
    const authUser = getCurrentAuthUser(this.store);
    return authUser?.tenantId || '';
  }
}
