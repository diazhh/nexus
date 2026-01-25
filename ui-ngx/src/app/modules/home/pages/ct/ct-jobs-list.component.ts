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
import { CTJobService } from '@core/http/ct/ct-job.service';
import { CTJob, JobStatus } from '@shared/models/ct/ct-job.model';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { CTJobSimulationDialogComponent } from './ct-job-simulation-dialog.component';

@Component({
  selector: 'tb-ct-jobs-list',
  templateUrl: './ct-jobs-list.component.html',
  styleUrls: ['./ct-jobs-list.component.scss']
})
export class CTJobsListComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  displayedColumns: string[] = [
    'jobNumber',
    'jobName',
    'wellName',
    'status',
    'jobType',
    'plannedStartDate',
    'actualDurationHours',
    'priority',
    'actions'
  ];

  dataSource: MatTableDataSource<CTJob>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  statusFilter: JobStatus | null = null;
  searchText = '';

  JobStatus = JobStatus;

  constructor(
    private jobService: CTJobService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<CTJob>([]);
  }

  ngOnInit() {
    this.loadJobs();
  }

  loadJobs() {
    this.isLoading = true;
    
    const pageLink = new PageLink(this.pageSize, this.pageIndex, this.searchText);
    const tenantId = this.getCurrentTenantId();

    this.jobService.getJobs(pageLink, tenantId).subscribe({
      next: (pageData: PageData<CTJob>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading jobs:', error);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadJobs();
  }

  onSearch(searchText: string) {
    this.searchText = searchText;
    this.pageIndex = 0;
    this.loadJobs();
  }

  onStatusFilterChange(status: JobStatus | null) {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadJobs();
  }

  viewDetails(job: CTJob) {
    this.router.navigate(['/ct/jobs', job.id.id]);
  }

  simulateJob(job: CTJob) {
    this.dialog.open(CTJobSimulationDialogComponent, {
      width: '900px',
      data: {
        jobId: job.id.id,
        jobName: job.jobName,
        customMode: false
      }
    });
  }

  startJob(job: CTJob) {
    if (confirm(`Start job ${job.jobNumber}?`)) {
      this.jobService.startJob(job.id.id).subscribe({
        next: () => {
          this.loadJobs();
        },
        error: (error) => {
          console.error('Error starting job:', error);
        }
      });
    }
  }

  completeJob(job: CTJob) {
    if (confirm(`Complete job ${job.jobNumber}?`)) {
      this.jobService.completeJob(job.id.id).subscribe({
        next: () => {
          this.loadJobs();
        },
        error: (error) => {
          console.error('Error completing job:', error);
        }
      });
    }
  }

  createJob() {
    // TODO: Open create job dialog
    console.log('Create job dialog');
  }

  editJob(job: CTJob) {
    // TODO: Open edit job dialog
    console.log('Edit job:', job);
  }

  deleteJob(job: CTJob) {
    // TODO: Confirm and delete job
    console.log('Delete job:', job);
  }

  getStatusColor(status: JobStatus): string {
    switch (status) {
      case JobStatus.PLANNED:
        return 'blue';
      case JobStatus.IN_PROGRESS:
        return 'green';
      case JobStatus.COMPLETED:
        return 'gray';
      case JobStatus.CANCELLED:
        return 'red';
      case JobStatus.ON_HOLD:
        return 'orange';
      default:
        return 'gray';
    }
  }

  getStatusLabel(status: JobStatus): string {
    return status.replace(/_/g, ' ');
  }

  getPriorityColor(priority: string): string {
    switch (priority?.toLowerCase()) {
      case 'critical':
        return '#f44336';
      case 'high':
        return '#ff9800';
      case 'medium':
        return '#ffc107';
      case 'low':
        return '#4caf50';
      default:
        return '#9e9e9e';
    }
  }

  canSimulate(job: CTJob): boolean {
    return job.status === JobStatus.PLANNED;
  }

  canStart(job: CTJob): boolean {
    return job.status === JobStatus.PLANNED;
  }

  canComplete(job: CTJob): boolean {
    return job.status === JobStatus.IN_PROGRESS;
  }

  canEdit(job: CTJob): boolean {
    return job.status !== JobStatus.COMPLETED && job.status !== JobStatus.CANCELLED;
  }

  private getCurrentTenantId(): string {
    // TODO: Get from auth service
    return 'tenant-id';
  }
}
