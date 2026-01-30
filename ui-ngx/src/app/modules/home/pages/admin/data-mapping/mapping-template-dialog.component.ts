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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DataMappingService } from '@core/http/data-mapping.service';
import { MappingTemplate, DistributionMode } from '@shared/models/data-mapping.models';

export interface MappingTemplateDialogData {
  template: MappingTemplate | null;
  isEdit: boolean;
  selectedModule?: string;
}

@Component({
  selector: 'tb-mapping-template-dialog',
  templateUrl: './mapping-template-dialog.component.html',
  styleUrls: ['./mapping-template-dialog.component.scss']
})
export class MappingTemplateDialogComponent implements OnInit {

  templateForm: FormGroup;
  isEdit: boolean;
  isSaving = false;

  moduleOptions: string[] = ['CT', 'DR', 'RV'];
  distributionModes = Object.values(DistributionMode);

  assetTypeOptions: { [module: string]: string[] } = {
    'CT': ['CT_UNIT', 'CT_REEL', 'CT_HYDRAULIC', 'CT_POWER_PACK'],
    'DR': ['DR_RIG', 'DR_BHA', 'DR_MUD_SYSTEM'],
    'RV': ['RV_WELL', 'RV_RESERVOIR', 'RV_FIELD', 'RV_BASIN']
  };

  constructor(
    private dialogRef: MatDialogRef<MappingTemplateDialogComponent, MappingTemplate>,
    @Inject(MAT_DIALOG_DATA) public data: MappingTemplateDialogData,
    private fb: FormBuilder,
    private store: Store<AppState>,
    private dataMappingService: DataMappingService
  ) {
    this.isEdit = data.isEdit;
  }

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    const template = this.data.template;

    this.templateForm = this.fb.group({
      name: [template?.name || '', [Validators.required, Validators.maxLength(255)]],
      description: [template?.description || '', Validators.maxLength(1000)],
      moduleKey: [template?.moduleKey || this.data.selectedModule || '', Validators.required],
      targetAssetType: [template?.targetAssetType || ''],
      distributionMode: [template?.distributionMode || DistributionMode.MAPPED, Validators.required],
      isDefault: [template?.isDefault || false],
      active: [template?.active !== false]
    });
  }

  getAvailableAssetTypes(): string[] {
    const moduleKey = this.templateForm.get('moduleKey')?.value;
    return this.assetTypeOptions[moduleKey] || [];
  }

  onModuleChange(): void {
    // Reset asset type when module changes
    this.templateForm.patchValue({ targetAssetType: '' });
  }

  save(): void {
    if (this.templateForm.invalid) {
      Object.keys(this.templateForm.controls).forEach(key => {
        this.templateForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSaving = true;

    const formValue = this.templateForm.value;
    const template: MappingTemplate = {
      ...this.data.template,
      name: formValue.name,
      description: formValue.description,
      moduleKey: formValue.moduleKey,
      targetAssetType: formValue.targetAssetType || null,
      distributionMode: formValue.distributionMode,
      isDefault: formValue.isDefault,
      active: formValue.active
    } as MappingTemplate;

    this.dataMappingService.saveMappingTemplate(template)
      .subscribe({
        next: (savedTemplate) => {
          this.isSaving = false;
          this.dialogRef.close(savedTemplate);
        },
        error: () => {
          this.isSaving = false;
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
