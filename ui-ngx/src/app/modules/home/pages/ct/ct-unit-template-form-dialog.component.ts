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
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { TemplateDefinition, CreateFromTemplateRequest } from '@core/http/ct-template.service';

export interface CTUnitTemplateFormDialogData {
  template: TemplateDefinition;
  tenantId: string;
}

@Component({
  selector: 'tb-ct-unit-template-form-dialog',
  templateUrl: './ct-unit-template-form-dialog.component.html',
  styleUrls: ['./ct-unit-template-form-dialog.component.scss']
})
export class CTUnitTemplateFormDialogComponent implements OnInit {

  unitForm: FormGroup;
  isLoading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private unitService: CTUnitService,
    private dialogRef: MatDialogRef<CTUnitTemplateFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTUnitTemplateFormDialogData
  ) {}

  ngOnInit() {
    this.buildForm();
  }

  buildForm() {
    this.unitForm = this.fb.group({
      unitCode: ['', [Validators.required, Validators.maxLength(50)]],
      unitName: ['', [Validators.required, Validators.maxLength(255)]],
      manufacturer: ['', Validators.maxLength(255)],
      model: ['', Validators.maxLength(255)],
      serialNumber: ['', Validators.maxLength(100)],
      yearManufactured: [null, [Validators.min(1900), Validators.max(new Date().getFullYear())]],
      location: ['', Validators.maxLength(255)]
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

    const formValues = this.unitForm.value;
    const request: CreateFromTemplateRequest = {
      templateId: this.data.template.id,
      variables: {
        unitCode: formValues.unitCode,
        unitName: formValues.unitName,
        manufacturer: formValues.manufacturer || '',
        model: formValues.model || '',
        serialNumber: formValues.serialNumber || '',
        yearManufactured: formValues.yearManufactured || new Date().getFullYear(),
        location: formValues.location || '',
        maxPressurePsi: this.data.template.templateStructure?.specifications?.maxPressurePsi || 0,
        maxTensionLbf: this.data.template.templateStructure?.specifications?.maxTensionLbf || 0,
        maxSpeedFtMin: this.data.template.templateStructure?.specifications?.maxSpeedFtMin || 0
      }
    };

    this.unitService.createFromTemplate(this.data.tenantId, request).subscribe({
      next: (unit) => {
        this.isLoading = false;
        this.dialogRef.close(unit);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to create unit from template';
        console.error('Error creating unit from template:', error);
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
