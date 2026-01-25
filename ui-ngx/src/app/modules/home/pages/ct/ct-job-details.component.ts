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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CTJobService } from '@core/http/ct/ct-job.service';
import { CTJob, JobStatus } from '@shared/models/ct/ct-job.model';
import { MatDialog } from '@angular/material/dialog';
import { CTJobSimulationDialogComponent } from './ct-job-simulation-dialog.component';

@Component({
  selector: 'tb-ct-job-details',
  templateUrl: './ct-job-details.component.html',
  styleUrls: ['./ct-job-details.component.scss']
})
export class CTJobDetailsComponent implements OnInit {

  job: CTJob | null = null;
  isLoading = false;
  error: string | null = null;

  JobStatus = JobStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobService: CTJobService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    const jobId = this.route.snapshot.paramMap.get('id');
    if (jobId) {
      this.loadJobDetails(jobId);
    }
  }

  loadJobDetails(jobId: string) {
    this.isLoading = true;
    this.error = null;

    this.jobService.getJob(jobId).subscribe({
      next: (job) => {
        this.job = job;
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load job details';
        this.isLoading = false;
        console.error('Error loading job:', error);
      }
    });
  }

  editJob() {
    console.log('Edit job:', this.job);
  }

  simulateJob() {
    if (this.job) {
      this.dialog.open(CTJobSimulationDialogComponent, {
        width: '900px',
        data: {
          jobId: this.job.id.id,
          jobName: this.job.jobName,
          customMode: false
        }
      });
    }
  }

  startJob() {
    if (this.job && confirm(`Start job ${this.job.jobNumber}?`)) {
      this.jobService.startJob(this.job.id.id).subscribe({
        next: () => {
          this.loadJobDetails(this.job!.id.id);
        },
        error: (error) => {
          console.error('Error starting job:', error);
        }
      });
    }
  }

  completeJob() {
    if (this.job && confirm(`Complete job ${this.job.jobNumber}?`)) {
      this.jobService.completeJob(this.job.id.id).subscribe({
        next: () => {
          this.loadJobDetails(this.job!.id.id);
        },
        error: (error) => {
          console.error('Error completing job:', error);
        }
      });
    }
  }

  cancelJob() {
    if (this.job && confirm(`Cancel job ${this.job.jobNumber}?`)) {
      const updatedJob = { ...this.job, status: JobStatus.CANCELLED };
      this.jobService.updateJob(this.job.id.id, updatedJob).subscribe({
        next: () => {
          this.loadJobDetails(this.job!.id.id);
        },
        error: (error) => {
          console.error('Error cancelling job:', error);
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/ct/jobs']);
  }

  getStatusColor(status: JobStatus): string {
    switch (status) {
      case JobStatus.PLANNED:
        return '#2196f3';
      case JobStatus.IN_PROGRESS:
        return '#4caf50';
      case JobStatus.COMPLETED:
        return '#9e9e9e';
      case JobStatus.CANCELLED:
        return '#f44336';
      case JobStatus.ON_HOLD:
        return '#ff9800';
      default:
        return '#9e9e9e';
    }
  }

  getStatusLabel(status: JobStatus): string {
    return status.replace(/_/g, ' ');
  }

  getPriorityColor(priority: string): string {
    switch (priority?.toUpperCase()) {
      case 'CRITICAL':
        return '#f44336';
      case 'HIGH':
        return '#ff9800';
      case 'MEDIUM':
        return '#ffc107';
      case 'LOW':
        return '#4caf50';
      default:
        return '#9e9e9e';
    }
  }

  canStart(): boolean {
    return this.job?.status === JobStatus.PLANNED;
  }

  canComplete(): boolean {
    return this.job?.status === JobStatus.IN_PROGRESS;
  }

  canCancel(): boolean {
    return this.job?.status === JobStatus.PLANNED || this.job?.status === JobStatus.IN_PROGRESS;
  }

  getProgressPercentage(): number {
    if (!this.job) return 0;
    
    if (this.job.status === JobStatus.COMPLETED) return 100;
    if (this.job.status === JobStatus.CANCELLED) return 0;
    if (this.job.status === JobStatus.PLANNED) return 0;
    
    if (this.job.actualDurationHours && this.job.plannedDurationHours) {
      return Math.min(100, (this.job.actualDurationHours / this.job.plannedDurationHours) * 100);
    }
    
    return 50;
  }
}
