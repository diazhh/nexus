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

import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort, Sort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { FormControl } from '@angular/forms';

import { PfWellService } from '@core/http/pf/pf-well.service';
import { PfAlarmService } from '@core/http/pf/pf-alarm.service';
import {
  PfWell,
  WellStatus,
  LiftSystemType,
  WellStatusColors,
  LiftSystemTypeLabels,
  OperationalStatusColors
} from '@shared/models/pf/pf-well.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { PfAlarmCount } from '@shared/models/pf/pf-alarm.model';

@Component({
  selector: 'tb-pf-wells-list',
  templateUrl: './pf-wells-list.component.html',
  styleUrls: ['./pf-wells-list.component.scss']
})
export class PfWellsListComponent implements OnInit, OnDestroy {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'status',
    'name',
    'wellpadName',
    'liftSystemType',
    'currentProductionBpd',
    'alarms',
    'actions'
  ];

  dataSource: MatTableDataSource<PfWell>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  // Filters
  searchControl = new FormControl('');
  statusFilter: WellStatus | null = null;
  liftSystemFilter: LiftSystemType | null = null;

  // Enums for template
  WellStatus = WellStatus;
  LiftSystemType = LiftSystemType;
  WellStatusColors = WellStatusColors;
  LiftSystemTypeLabels = LiftSystemTypeLabels;
  OperationalStatusColors = OperationalStatusColors;

  // Summary stats
  fieldSummary: {
    totalWells: number;
    producingWells: number;
    totalProductionBpd: number;
    activeAlarms: number;
  } = {
    totalWells: 0,
    producingWells: 0,
    totalProductionBpd: 0,
    activeAlarms: 0
  };

  private destroy$ = new Subject<void>();

  constructor(
    private wellService: PfWellService,
    private alarmService: PfAlarmService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<PfWell>([]);
  }

  ngOnInit(): void {
    this.loadWells();
    this.loadFieldSummary();
    this.setupSearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearch(): void {
    this.searchControl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadWells();
    });
  }

  loadWells(): void {
    this.isLoading = true;
    const searchText = this.searchControl.value || '';
    const pageLink = new PageLink(this.pageSize, this.pageIndex, searchText);

    let request$;
    if (this.statusFilter) {
      request$ = this.wellService.getWellsByStatus(this.statusFilter, pageLink);
    } else if (this.liftSystemFilter) {
      request$ = this.wellService.getWellsByLiftSystemType(this.liftSystemFilter, pageLink);
    } else {
      request$ = this.wellService.getWells(pageLink);
    }

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData: PageData<PfWell>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading wells:', error);
        this.isLoading = false;
      }
    });
  }

  private loadFieldSummary(): void {
    // Get alarm count
    this.alarmService.getAlarmCount().pipe(takeUntil(this.destroy$)).subscribe({
      next: (count: PfAlarmCount) => {
        this.fieldSummary.activeAlarms = count.total;
      }
    });

    // Calculate summary from wells data
    this.wellService.getWells(new PageLink(1000, 0)).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData: PageData<PfWell>) => {
        this.fieldSummary.totalWells = pageData.totalElements;
        this.fieldSummary.producingWells = pageData.data.filter(w => w.status === WellStatus.PRODUCING).length;
        this.fieldSummary.totalProductionBpd = pageData.data
          .filter(w => w.status === WellStatus.PRODUCING)
          .reduce((sum, w) => sum + (w.currentProductionBpd || 0), 0);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadWells();
  }

  onStatusFilterChange(status: WellStatus | null): void {
    this.statusFilter = status;
    this.liftSystemFilter = null;
    this.pageIndex = 0;
    this.loadWells();
  }

  onLiftSystemFilterChange(type: LiftSystemType | null): void {
    this.liftSystemFilter = type;
    this.statusFilter = null;
    this.pageIndex = 0;
    this.loadWells();
  }

  clearFilters(): void {
    this.statusFilter = null;
    this.liftSystemFilter = null;
    this.searchControl.setValue('');
    this.pageIndex = 0;
    this.loadWells();
  }

  viewWellDetails(well: PfWell): void {
    this.router.navigate(['/pf/wells', well.assetId]);
  }

  viewWellAlarms(well: PfWell): void {
    this.router.navigate(['/pf/alarms'], { queryParams: { wellId: well.assetId } });
  }

  getStatusColor(status: WellStatus): string {
    return WellStatusColors[status] || '#9e9e9e';
  }

  getStatusLabel(status: WellStatus): string {
    return status.replace(/_/g, ' ');
  }

  getLiftSystemLabel(type: LiftSystemType): string {
    return LiftSystemTypeLabels[type] || type;
  }

  getLiftSystemShortLabel(type: LiftSystemType): string {
    return type.replace(/_/g, ' ');
  }

  formatProduction(value: number | undefined): string {
    if (value === undefined || value === null) return '-';
    return value.toLocaleString('en-US', { maximumFractionDigits: 1 });
  }

  trackByWell(index: number, well: PfWell): string {
    return well.assetId;
  }
}
