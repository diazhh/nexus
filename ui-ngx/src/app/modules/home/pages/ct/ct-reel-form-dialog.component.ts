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
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { CTReel, ReelStatus, CreateCTReelRequest, UpdateCTReelRequest } from '@shared/models/ct/ct-reel.model';

export interface CTReelFormDialogData {
  reel?: CTReel;
  tenantId: string;
  assetId?: string;
}

@Component({
  selector: 'tb-ct-reel-form-dialog',
  templateUrl: './ct-reel-form-dialog.component.html',
  styleUrls: ['./ct-reel-form-dialog.component.scss']
})
export class CTReelFormDialogComponent implements OnInit {

  reelForm: FormGroup;
  isEditMode = false;
  isLoading = false;
  error: string | null = null;

  reelStatuses = Object.values(ReelStatus);

  constructor(
    private fb: FormBuilder,
    private reelService: CTReelService,
    private dialogRef: MatDialogRef<CTReelFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTReelFormDialogData
  ) {
    this.isEditMode = !!data.reel;
  }

  ngOnInit() {
    this.buildForm();
    if (this.isEditMode && this.data.reel) {
      this.populateForm(this.data.reel);
    }
  }

  buildForm() {
    if (this.isEditMode) {
      this.reelForm = this.fb.group({
        reelName: ['', [Validators.required, Validators.maxLength(255)]],
        status: [ReelStatus.AVAILABLE, Validators.required],
        currentLocation: ['', Validators.maxLength(255)],
        description: ['', Validators.maxLength(1000)],
        notes: ['', Validators.maxLength(2000)]
      });
    } else {
      this.reelForm = this.fb.group({
        reelCode: ['', [Validators.required, Validators.maxLength(50)]],
        reelName: ['', [Validators.required, Validators.maxLength(255)]],
        assetId: [this.data.assetId || '', Validators.required],
        manufacturer: ['', Validators.maxLength(255)],
        serialNumber: ['', Validators.maxLength(100)],
        tubingOdInch: [null, [Validators.min(0), Validators.max(10)]],
        tubingIdInch: [null, [Validators.min(0), Validators.max(10)]],
        materialGrade: ['', Validators.maxLength(100)],
        totalLengthFt: [null, [Validators.min(0), Validators.max(50000)]],
        description: ['', Validators.maxLength(1000)]
      });
    }
  }

  populateForm(reel: CTReel) {
    this.reelForm.patchValue({
      reelName: reel.reelName,
      status: reel.status,
      currentLocation: reel.currentLocation,
      description: reel.description,
      notes: reel.notes
    });
  }

  onSubmit() {
    if (this.reelForm.invalid) {
      Object.keys(this.reelForm.controls).forEach(key => {
        this.reelForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    this.error = null;

    if (this.isEditMode) {
      this.updateReel();
    } else {
      this.createReel();
    }
  }

  createReel() {
    const request: CreateCTReelRequest = this.reelForm.value;

    this.reelService.createReel(request).subscribe({
      next: (reel) => {
        this.isLoading = false;
        this.dialogRef.close(reel);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to create reel';
        console.error('Error creating reel:', error);
      }
    });
  }

  updateReel() {
    const request: UpdateCTReelRequest = this.reelForm.value;

    this.reelService.updateReel(this.data.reel!.id.id, request).subscribe({
      next: (reel) => {
        this.isLoading = false;
        this.dialogRef.close(reel);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to update reel';
        console.error('Error updating reel:', error);
      }
    });
  }

  onCancel() {
    this.dialogRef.close();
  }

  getErrorMessage(fieldName: string): string {
    const control = this.reelForm.get(fieldName);
    if (control?.hasError('required')) {
      return 'This field is required';
    }
    if (control?.hasError('maxlength')) {
      return `Maximum length is ${control.errors?.['maxlength'].requiredLength} characters`;
    }
    if (control?.hasError('min')) {
      return `Minimum value is ${control.errors?.['min'].min}`;
    }
    if (control?.hasError('max')) {
      return `Maximum value is ${control.errors?.['max'].max}`;
    }
    return '';
  }
}
