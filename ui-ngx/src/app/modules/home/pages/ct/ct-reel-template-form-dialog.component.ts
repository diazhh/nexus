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
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { TemplateDefinition, CreateFromTemplateRequest } from '@core/http/ct-template.service';

export interface CTReelTemplateFormDialogData {
  template: TemplateDefinition;
  tenantId: string;
}

@Component({
  selector: 'tb-ct-reel-template-form-dialog',
  templateUrl: './ct-reel-template-form-dialog.component.html',
  styleUrls: ['./ct-reel-template-form-dialog.component.scss']
})
export class CTReelTemplateFormDialogComponent implements OnInit {

  reelForm: FormGroup;
  isLoading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private reelService: CTReelService,
    private dialogRef: MatDialogRef<CTReelTemplateFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTReelTemplateFormDialogData
  ) {}

  ngOnInit() {
    this.buildForm();
  }

  buildForm() {
    this.reelForm = this.fb.group({
      reelCode: ['', [Validators.required, Validators.maxLength(50)]],
      reelName: ['', [Validators.required, Validators.maxLength(255)]],
      tubingOD: [null, [Validators.required, Validators.min(0)]],
      tubingID: [null, [Validators.required, Validators.min(0)]],
      wallThickness: [null, [Validators.required, Validators.min(0)]],
      totalLength: [null, [Validators.required, Validators.min(0)]],
      material: ['', Validators.maxLength(100)]
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

    const formValues = this.reelForm.value;
    const request: CreateFromTemplateRequest = {
      templateId: this.data.template.id,
      variables: {
        reelCode: formValues.reelCode,
        reelName: formValues.reelName,
        tubingOD: formValues.tubingOD,
        tubingID: formValues.tubingID,
        wallThickness: formValues.wallThickness,
        totalLength: formValues.totalLength,
        material: formValues.material || this.data.template.templateStructure?.specifications?.material || '',
        grade: this.data.template.templateStructure?.specifications?.grade || ''
      }
    };

    this.reelService.createFromTemplate(this.data.tenantId, request).subscribe({
      next: (reel) => {
        this.isLoading = false;
        this.dialogRef.close(reel);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to create reel from template';
        console.error('Error creating reel from template:', error);
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
    return '';
  }
}
