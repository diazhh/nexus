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
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { CTUnit, UnitStatus, CreateCTUnitRequest, UpdateCTUnitRequest } from '@shared/models/ct/ct-unit.model';

export interface CTUnitFormDialogData {
  unit?: CTUnit;
  tenantId: string;
  assetId?: string;
}

@Component({
  selector: 'tb-ct-unit-form-dialog',
  templateUrl: './ct-unit-form-dialog.component.html',
  styleUrls: ['./ct-unit-form-dialog.component.scss']
})
export class CTUnitFormDialogComponent implements OnInit {

  unitForm: FormGroup;
  isEditMode = false;
  isLoading = false;
  error: string | null = null;

  unitStatuses = Object.values(UnitStatus);

  constructor(
    private fb: FormBuilder,
    private unitService: CTUnitService,
    private dialogRef: MatDialogRef<CTUnitFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTUnitFormDialogData
  ) {
    this.isEditMode = !!data.unit;
  }

  ngOnInit() {
    this.buildForm();
    if (this.isEditMode && this.data.unit) {
      this.populateForm(this.data.unit);
    }
  }

  buildForm() {
    if (this.isEditMode) {
      this.unitForm = this.fb.group({
        unitName: ['', [Validators.required, Validators.maxLength(255)]],
        manufacturer: ['', Validators.maxLength(255)],
        model: ['', Validators.maxLength(255)],
        operationalStatus: [UnitStatus.STANDBY, Validators.required],
        currentLocation: ['', Validators.maxLength(255)],
        description: ['', Validators.maxLength(1000)],
        notes: ['', Validators.maxLength(2000)]
      });
    } else {
      this.unitForm = this.fb.group({
        unitCode: ['', [Validators.required, Validators.maxLength(50)]],
        unitName: ['', [Validators.required, Validators.maxLength(255)]],
        assetId: [this.data.assetId || '', Validators.required],
        manufacturer: ['', Validators.maxLength(255)],
        model: ['', Validators.maxLength(255)],
        serialNumber: ['', Validators.maxLength(100)],
        yearManufactured: [null, [Validators.min(1900), Validators.max(new Date().getFullYear())]],
        maxPressureRatingPsi: [null, Validators.min(0)],
        maxTensionRatingLbf: [null, Validators.min(0)],
        maxSpeedRatingFtMin: [null, Validators.min(0)],
        powerRatingHp: [null, Validators.min(0)],
        currentLocation: ['', Validators.maxLength(255)],
        description: ['', Validators.maxLength(1000)]
      });
    }
  }

  populateForm(unit: CTUnit) {
    this.unitForm.patchValue({
      unitName: unit.unitName,
      manufacturer: unit.manufacturer,
      model: unit.model,
      operationalStatus: unit.operationalStatus,
      currentLocation: unit.currentLocation,
      description: unit.description,
      notes: unit.notes
    });
  }

  onSubmit() {
    if (this.unitForm.invalid) {
      Object.keys(this.unitForm.controls).forEach(key => {
        this.unitForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    this.error = null;

    if (this.isEditMode) {
      this.updateUnit();
    } else {
      this.createUnit();
    }
  }

  createUnit() {
    const request: CreateCTUnitRequest = this.unitForm.value;

    this.unitService.createUnit(request).subscribe({
      next: (unit) => {
        this.isLoading = false;
        this.dialogRef.close(unit);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to create unit';
        console.error('Error creating unit:', error);
      }
    });
  }

  updateUnit() {
    const request: UpdateCTUnitRequest = this.unitForm.value;

    this.unitService.updateUnit(this.data.unit!.id.id, request).subscribe({
      next: (unit) => {
        this.isLoading = false;
        this.dialogRef.close(unit);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to update unit';
        console.error('Error updating unit:', error);
      }
    });
  }

  onCancel() {
    this.dialogRef.close();
  }

  getErrorMessage(fieldName: string): string {
    const control = this.unitForm.get(fieldName);
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
