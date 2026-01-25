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

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CTJobService } from '@core/http/ct/ct-job.service';
import { CTJob, JobStatus, CreateCTJobRequest, UpdateCTJobRequest } from '@shared/models/ct/ct-job.model';

export interface CTJobFormDialogData {
  job?: CTJob;
  tenantId: string;
  unitId?: string;
  reelId?: string;
}

@Component({
  selector: 'tb-ct-job-form-dialog',
  templateUrl: './ct-job-form-dialog.component.html',
  styleUrls: ['./ct-job-form-dialog.component.scss']
})
export class CTJobFormDialogComponent implements OnInit {

  jobForm: FormGroup;
  isEditMode = false;
  isLoading = false;
  error: string | null = null;

  jobStatuses = Object.values(JobStatus);
  jobTypes = ['CLEANOUT', 'STIMULATION', 'DRILLING', 'LOGGING', 'FISHING', 'MILLING', 'OTHER'];
  priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  constructor(
    private fb: FormBuilder,
    private jobService: CTJobService,
    private dialogRef: MatDialogRef<CTJobFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTJobFormDialogData
  ) {
    this.isEditMode = !!data.job;
  }

  ngOnInit() {
    this.buildForm();
    if (this.isEditMode && this.data.job) {
      this.populateForm(this.data.job);
    }
  }

  buildForm() {
    if (this.isEditMode) {
      this.jobForm = this.fb.group({
        jobName: ['', [Validators.required, Validators.maxLength(255)]],
        status: [JobStatus.PLANNED, Validators.required],
        actualStartDate: [null],
        actualEndDate: [null],
        description: ['', Validators.maxLength(1000)],
        notes: ['', Validators.maxLength(2000)]
      });
    } else {
      this.jobForm = this.fb.group({
        jobNumber: ['', [Validators.required, Validators.maxLength(50)]],
        jobName: ['', [Validators.required, Validators.maxLength(255)]],
        jobType: ['', Validators.required],
        priority: ['MEDIUM'],
        wellName: ['', [Validators.required, Validators.maxLength(255)]],
        fieldName: ['', Validators.maxLength(255)],
        clientName: ['', Validators.maxLength(255)],
        unitId: [this.data.unitId || '', Validators.required],
        reelId: [this.data.reelId || '', Validators.required],
        targetDepthToFt: [null, Validators.min(0)],
        plannedStartDate: [null],
        description: ['', Validators.maxLength(1000)]
      });
    }
  }

  populateForm(job: CTJob) {
    this.jobForm.patchValue({
      jobName: job.jobName,
      status: job.status,
      actualStartDate: job.actualStartDate ? new Date(job.actualStartDate) : null,
      actualEndDate: job.actualEndDate ? new Date(job.actualEndDate) : null,
      description: job.description,
      notes: job.notes
    });
  }

  onSubmit() {
    if (this.jobForm.invalid) {
      Object.keys(this.jobForm.controls).forEach(key => {
        this.jobForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    this.error = null;

    if (this.isEditMode) {
      this.updateJob();
    } else {
      this.createJob();
    }
  }

  createJob() {
    const formValue = this.jobForm.value;
    const request: CreateCTJobRequest = {
      jobNumber: formValue.jobNumber,
      jobName: formValue.jobName,
      jobType: formValue.jobType,
      wellName: formValue.wellName,
      unitId: formValue.unitId,
      reelId: formValue.reelId,
      targetDepthToFt: formValue.targetDepthToFt,
      plannedStartDate: formValue.plannedStartDate ? new Date(formValue.plannedStartDate).getTime() : undefined,
      description: formValue.description
    };

    this.jobService.createJob(request).subscribe({
      next: (job) => {
        this.isLoading = false;
        this.dialogRef.close(job);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to create job';
        console.error('Error creating job:', error);
      }
    });
  }

  updateJob() {
    const formValue = this.jobForm.value;
    const request: UpdateCTJobRequest = {
      jobName: formValue.jobName,
      status: formValue.status,
      actualStartDate: formValue.actualStartDate ? new Date(formValue.actualStartDate).getTime() : undefined,
      actualEndDate: formValue.actualEndDate ? new Date(formValue.actualEndDate).getTime() : undefined,
      description: formValue.description,
      notes: formValue.notes
    };

    this.jobService.updateJob(this.data.job!.id.id, request).subscribe({
      next: (job) => {
        this.isLoading = false;
        this.dialogRef.close(job);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to update job';
        console.error('Error updating job:', error);
      }
    });
  }

  onCancel() {
    this.dialogRef.close();
  }

  getErrorMessage(fieldName: string): string {
    const control = this.jobForm.get(fieldName);
    if (control?.hasError('required')) {
      return 'This field is required';
    }
    if (control?.hasError('maxlength')) {
      return `Maximum length is ${control.errors?.['maxlength'].requiredLength} characters`;
    }
    if (control?.hasError('min')) {
      return `Minimum value is ${control.errors?.['min'].min}`;
    }
    return '';
  }
}
