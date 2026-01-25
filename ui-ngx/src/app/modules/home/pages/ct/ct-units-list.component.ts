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
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { CTUnit, UnitStatus } from '@shared/models/ct/ct-unit.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-ct-units-list',
  templateUrl: './ct-units-list.component.html',
  styleUrls: ['./ct-units-list.component.scss']
})
export class CTUnitsListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'unitCode',
    'unitName',
    'operationalStatus',
    'currentLocation',
    'totalOperationalHours',
    'totalJobsCompleted',
    'currentReel',
    'actions'
  ];

  dataSource: MatTableDataSource<CTUnit>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  statusFilter: UnitStatus | null = null;
  searchText = '';

  UnitStatus = UnitStatus;

  constructor(
    private unitService: CTUnitService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<CTUnit>([]);
  }

  ngOnInit() {
    this.loadUnits();
  }

  loadUnits() {
    this.isLoading = true;
    
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.searchText);
    const tenantId = this.getCurrentTenantId();

    this.unitService.getUnits(pageLink, tenantId).subscribe({
      next: (pageData: PageData<CTUnit>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading units:', error);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUnits();
  }

  onSearch(searchText: string) {
    this.searchText = searchText;
    this.pageIndex = 0;
    this.loadUnits();
  }

  onStatusFilterChange(status: UnitStatus | null) {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadUnits();
  }

  viewDetails(unit: CTUnit) {
    this.router.navigate(['/ct/units', unit.id.id]);
  }

  createUnit() {
    // TODO: Open create unit dialog
    console.log('Create unit dialog');
  }

  editUnit(unit: CTUnit) {
    // TODO: Open edit unit dialog
    console.log('Edit unit:', unit);
  }

  deleteUnit(unit: CTUnit) {
    // TODO: Confirm and delete unit
    console.log('Delete unit:', unit);
  }

  assignReel(unit: CTUnit) {
    // TODO: Open assign reel dialog
    console.log('Assign reel to unit:', unit);
  }

  detachReel(unit: CTUnit) {
    if (confirm(`Detach reel from unit ${unit.unitCode}?`)) {
      this.unitService.detachReel(unit.id.id).subscribe({
        next: () => {
          this.loadUnits();
        },
        error: (error) => {
          console.error('Error detaching reel:', error);
        }
      });
    }
  }

  getStatusColor(status: UnitStatus): string {
    switch (status) {
      case UnitStatus.ACTIVE:
        return 'green';
      case UnitStatus.STANDBY:
        return 'blue';
      case UnitStatus.MAINTENANCE:
        return 'orange';
      case UnitStatus.OUT_OF_SERVICE:
        return 'red';
      default:
        return 'gray';
    }
  }

  getStatusLabel(status: UnitStatus): string {
    return status.replace(/_/g, ' ');
  }

  private getCurrentTenantId(): string {
    // TODO: Get from auth service
    return 'tenant-id';
  }
}
