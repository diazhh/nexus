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

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DRRigService } from '@core/http/dr';
import { DRRig, RigType, RigStatus, CreateDRRigRequest } from '@shared/models/dr';

export interface RigFormDialogData {
  rig?: DRRig;
  isEdit: boolean;
}

@Component({
  selector: 'tb-dr-rig-form-dialog',
  templateUrl: './dr-rig-form-dialog.component.html',
  styleUrls: ['./dr-rig-form-dialog.component.scss']
})
export class DrRigFormDialogComponent implements OnInit {

  rigForm: FormGroup;
  isEdit: boolean;
  isSubmitting = false;

  rigTypes = Object.values(RigType);
  rigStatuses = Object.values(RigStatus);

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<DrRigFormDialogComponent>,
    private rigService: DRRigService,
    @Inject(MAT_DIALOG_DATA) public data: RigFormDialogData
  ) {
    this.isEdit = data.isEdit;
  }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    const rig = this.data.rig;

    this.rigForm = this.fb.group({
      rigCode: [rig?.rigCode || '', [Validators.required, Validators.maxLength(50)]],
      rigName: [rig?.rigName || '', [Validators.required, Validators.maxLength(255)]],
      rigType: [rig?.rigType || RigType.LAND, Validators.required],
      operationalStatus: [rig?.operationalStatus || RigStatus.STANDBY, Validators.required],
      contractor: [rig?.contractor || '', Validators.maxLength(255)],
      manufacturer: [rig?.manufacturer || '', Validators.maxLength(255)],
      model: [rig?.model || '', Validators.maxLength(255)],
      yearBuilt: [rig?.yearBuilt || null],
      maxHookloadLbs: [rig?.maxHookloadLbs || null],
      maxRotaryTorqueFtLbs: [rig?.maxRotaryTorqueFtLbs || null],
      maxDepthCapabilityFt: [rig?.maxDepthCapabilityFt || null],
      currentLocation: [rig?.currentLocation || '', Validators.maxLength(500)],
      latitude: [rig?.latitude || null],
      longitude: [rig?.longitude || null],
      notes: [rig?.notes || '', Validators.maxLength(1000)]
    });
  }

  onSubmit(): void {
    if (this.rigForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      const formValue = this.rigForm.value;

      if (this.isEdit && this.data.rig) {
        this.rigService.updateRig(this.data.rig.id.id, formValue).subscribe({
          next: (updatedRig) => {
            this.dialogRef.close(updatedRig);
          },
          error: () => {
            this.isSubmitting = false;
          }
        });
      } else {
        const createRequest: CreateDRRigRequest = {
          rigCode: formValue.rigCode,
          rigName: formValue.rigName,
          rigType: formValue.rigType,
          contractor: formValue.contractor || undefined,
          manufacturer: formValue.manufacturer || undefined,
          model: formValue.model || undefined,
          yearBuilt: formValue.yearBuilt || undefined,
          maxHookloadLbs: formValue.maxHookloadLbs || undefined,
          maxRotaryTorqueFtLbs: formValue.maxRotaryTorqueFtLbs || undefined,
          maxDepthCapabilityFt: formValue.maxDepthCapabilityFt || undefined,
          currentLocation: formValue.currentLocation || undefined,
          latitude: formValue.latitude || undefined,
          longitude: formValue.longitude || undefined,
          notes: formValue.notes || undefined
        };

        this.rigService.createRig(createRequest).subscribe({
          next: (newRig) => {
            this.dialogRef.close(newRig);
          },
          error: () => {
            this.isSubmitting = false;
          }
        });
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
