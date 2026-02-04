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
import { ActivatedRoute, Router } from '@angular/router';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { PfAlarmService } from '@core/http/pf/pf-alarm.service';
import {
  PfAlarm,
  PfAlarmQuery,
  PfAlarmSeverity,
  PfAlarmStatus,
  PfAlarmCount,
  AlarmSeverityColors,
  AlarmSeverityLabels,
  AlarmStatusLabels,
  isAlarmActive
} from '@shared/models/pf/pf-alarm.model';
import { PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-pf-alarms-list',
  templateUrl: './pf-alarms-list.component.html',
  styleUrls: ['./pf-alarms-list.component.scss']
})
export class PfAlarmsListComponent implements OnInit, OnDestroy {

  @ViewChild(MatPaginator) paginator: MatPaginator;

  displayedColumns: string[] = [
    'select',
    'severity',
    'type',
    'originatorName',
    'status',
    'startTs',
    'message',
    'actions'
  ];

  dataSource: MatTableDataSource<PfAlarm>;
  selection = new SelectionModel<PfAlarm>(true, []);
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  // Filters
  wellIdFilter: string | null = null;
  severityFilter: PfAlarmSeverity[] = [];
  statusFilter: PfAlarmStatus[] = [PfAlarmStatus.ACTIVE_UNACK, PfAlarmStatus.ACTIVE_ACK];

  // Alarm count
  alarmCount: PfAlarmCount = {
    critical: 0,
    high: 0,
    medium: 0,
    low: 0,
    total: 0
  };

  // Enums for template
  PfAlarmSeverity = PfAlarmSeverity;
  PfAlarmStatus = PfAlarmStatus;
  AlarmSeverityColors = AlarmSeverityColors;
  AlarmSeverityLabels = AlarmSeverityLabels;
  AlarmStatusLabels = AlarmStatusLabels;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private alarmService: PfAlarmService
  ) {
    this.dataSource = new MatTableDataSource<PfAlarm>([]);
  }

  ngOnInit(): void {
    // Check for wellId in query params
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.wellIdFilter = params['wellId'] || null;
      this.loadAlarms();
    });

    this.loadAlarmCount();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAlarms(): void {
    this.isLoading = true;
    this.selection.clear();

    const query: PfAlarmQuery = {
      pageSize: this.pageSize,
      page: this.pageIndex,
      sortProperty: 'startTs',
      sortOrder: 'DESC'
    };

    if (this.wellIdFilter) {
      query.entityId = this.wellIdFilter;
    }
    if (this.severityFilter.length > 0) {
      query.severity = this.severityFilter;
    }
    if (this.statusFilter.length > 0) {
      query.status = this.statusFilter;
    }

    this.alarmService.getAlarms(query).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData: PageData<PfAlarm>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading alarms:', error);
        this.isLoading = false;
      }
    });
  }

  loadAlarmCount(): void {
    this.alarmService.getAlarmCount(this.wellIdFilter || undefined).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (count) => {
        this.alarmCount = count;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadAlarms();
  }

  onSeverityFilterChange(severity: PfAlarmSeverity[]): void {
    this.severityFilter = severity;
    this.pageIndex = 0;
    this.loadAlarms();
  }

  onStatusFilterChange(status: PfAlarmStatus[]): void {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadAlarms();
  }

  clearFilters(): void {
    this.severityFilter = [];
    this.statusFilter = [PfAlarmStatus.ACTIVE_UNACK, PfAlarmStatus.ACTIVE_ACK];
    this.pageIndex = 0;
    this.loadAlarms();
  }

  acknowledgeAlarm(alarm: PfAlarm): void {
    this.alarmService.acknowledgeAlarm(alarm.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadAlarms();
        this.loadAlarmCount();
      }
    });
  }

  clearAlarm(alarm: PfAlarm): void {
    this.alarmService.clearAlarm(alarm.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadAlarms();
        this.loadAlarmCount();
      }
    });
  }

  acknowledgeSelected(): void {
    const selectedIds = this.selection.selected
      .filter(a => isAlarmActive(a.status))
      .map(a => a.id.id);

    if (selectedIds.length === 0) return;

    this.alarmService.acknowledgeAlarms(selectedIds).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadAlarms();
        this.loadAlarmCount();
      }
    });
  }

  viewWellDetails(alarm: PfAlarm): void {
    this.router.navigate(['/pf/wells', alarm.originatorId]);
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  masterToggle(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.dataSource.data.forEach(row => this.selection.select(row));
    }
  }

  getSeverityColor(severity: PfAlarmSeverity): string {
    return AlarmSeverityColors[severity] || '#9e9e9e';
  }

  getStatusLabel(status: PfAlarmStatus): string {
    return AlarmStatusLabels[status] || status;
  }

  isActive(status: PfAlarmStatus): boolean {
    return isAlarmActive(status);
  }

  trackByAlarm(index: number, alarm: PfAlarm): string {
    return alarm.id.id;
  }

  formatAlarmType(type: string): string {
    return type ? type.replace(/_/g, ' ') : '';
  }
}
