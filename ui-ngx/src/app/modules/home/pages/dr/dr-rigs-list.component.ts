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
import { DRRigService } from '@core/http/dr/dr-rig.service';
import { DRRig, RigStatus, RigType } from '@shared/models/dr/dr-rig.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { DrRigFormDialogComponent } from './dr-rig-form-dialog.component';

@Component({
  selector: 'tb-dr-rigs-list',
  templateUrl: './dr-rigs-list.component.html',
  styleUrls: ['./dr-rigs-list.component.scss']
})
export class DrRigsListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'rigCode',
    'rigName',
    'rigType',
    'operationalStatus',
    'currentLocation',
    'totalOperationalHours',
    'totalWellsDrilled',
    'currentWell',
    'actions'
  ];

  dataSource: MatTableDataSource<DRRig>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  statusFilter: RigStatus | null = null;
  typeFilter: RigType | null = null;
  searchText = '';

  RigStatus = RigStatus;
  RigType = RigType;

  constructor(
    private store: Store<AppState>,
    private rigService: DRRigService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<DRRig>([]);
  }

  ngOnInit() {
    this.loadRigs();
  }

  loadRigs() {
    this.isLoading = true;

    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.searchText);
    const tenantId = this.getCurrentTenantId();

    this.rigService.getRigs(pageLink, tenantId).subscribe({
      next: (pageData: PageData<DRRig>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading rigs:', error);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRigs();
  }

  onSearch(searchText: string) {
    this.searchText = searchText;
    this.pageIndex = 0;
    this.loadRigs();
  }

  onStatusFilterChange(status: RigStatus | null) {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadRigs();
  }

  onTypeFilterChange(type: RigType | null) {
    this.typeFilter = type;
    this.pageIndex = 0;
    this.loadRigs();
  }

  viewDetails(rig: DRRig) {
    this.router.navigate(['/dr/rigs', rig.id.id]);
  }

  createRig() {
    const dialogRef = this.dialog.open(DrRigFormDialogComponent, {
      width: '800px',
      data: {
        tenantId: this.getCurrentTenantId(),
        mode: 'create'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadRigs();
      }
    });
  }

  editRig(rig: DRRig) {
    const dialogRef = this.dialog.open(DrRigFormDialogComponent, {
      width: '800px',
      data: {
        rig: rig,
        tenantId: this.getCurrentTenantId(),
        mode: 'edit'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadRigs();
      }
    });
  }

  deleteRig(rig: DRRig) {
    if (confirm(`Are you sure you want to delete rig ${rig.rigCode}?`)) {
      this.rigService.deleteRig(rig.id.id).subscribe({
        next: () => {
          this.loadRigs();
        },
        error: (error) => {
          console.error('Error deleting rig:', error);
        }
      });
    }
  }

  assignWell(rig: DRRig) {
    // TODO: Open assign well dialog
    console.log('Assign well to rig:', rig);
  }

  releaseWell(rig: DRRig) {
    if (confirm(`Release rig ${rig.rigCode} from current well?`)) {
      this.rigService.releaseWell(rig.id.id).subscribe({
        next: () => {
          this.loadRigs();
        },
        error: (error) => {
          console.error('Error releasing well:', error);
        }
      });
    }
  }

  getStatusColor(status: RigStatus): string {
    switch (status) {
      case RigStatus.DRILLING:
        return '#4caf50';
      case RigStatus.STANDBY:
        return '#2196f3';
      case RigStatus.TRIPPING:
      case RigStatus.CIRCULATING:
        return '#ff9800';
      case RigStatus.MAINTENANCE:
        return '#ff5722';
      case RigStatus.RIGGING_UP:
      case RigStatus.RIGGING_DOWN:
        return '#9c27b0';
      case RigStatus.CASING:
      case RigStatus.CEMENTING:
        return '#00bcd4';
      case RigStatus.TESTING:
        return '#ffeb3b';
      case RigStatus.MOVING:
        return '#607d8b';
      case RigStatus.OUT_OF_SERVICE:
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  getStatusLabel(status: RigStatus): string {
    return status.replace(/_/g, ' ');
  }

  getRigTypeLabel(type: RigType): string {
    return type.replace(/_/g, ' ');
  }

  getRigTypeIcon(type: RigType): string {
    switch (type) {
      case RigType.LAND:
        return 'landscape';
      case RigType.JACKUP:
        return 'waves';
      case RigType.SEMI_SUBMERSIBLE:
        return 'anchor';
      case RigType.DRILLSHIP:
        return 'directions_boat';
      case RigType.PLATFORM:
        return 'business';
      case RigType.BARGE:
        return 'sailing';
      case RigType.WORKOVER:
        return 'build';
      default:
        return 'precision_manufacturing';
    }
  }

  private getCurrentTenantId(): string {
    const authUser = getCurrentAuthUser(this.store);
    return authUser?.tenantId || '';
  }
}
