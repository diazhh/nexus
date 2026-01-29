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
import { RvService } from '@core/http/rv/rv.service';
import { RvField, RvFieldStatus, RvFieldType, RvBasin } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export interface RvFieldDialogData {
  tenantId: string;
  field?: RvField;
}

@Component({
  selector: 'tb-rv-field-dialog',
  templateUrl: './rv-field-dialog.component.html',
  styleUrls: ['./rv-field-dialog.component.scss']
})
export class RvFieldDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  basins: RvBasin[] = [];

  fieldStatuses = Object.values(RvFieldStatus);
  fieldTypes = Object.values(RvFieldType);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvFieldDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RvFieldDialogData
  ) {
    this.isEditMode = !!data.field;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadBasins();
  }

  loadBasins(): void {
    this.rvService.getBasins(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => {
        this.basins = pageData.data;
      }
    });
  }

  buildForm(): void {
    const field = this.data.field;
    this.form = this.fb.group({
      name: [field?.name || '', [Validators.required]],
      label: [field?.label || ''],
      code: [field?.code || ''],
      basinAssetId: [field?.basinAssetId || ''],
      operatorName: [field?.operatorName || ''],
      fieldStatus: [field?.fieldStatus || 'EXPLORATION'],
      fieldType: [field?.fieldType || 'OIL'],
      onshoreOffshore: [field?.onshoreOffshore || 'ONSHORE'],
      areaKm2: [field?.areaKm2 || null],
      centerLatitude: [field?.centerLatitude || null],
      centerLongitude: [field?.centerLongitude || null],
      ooipMmbbl: [field?.ooipMmbbl || null],
      currentOilRateBopd: [field?.currentOilRateBopd || null]
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const field: RvField = { ...this.data.field, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateField(this.data.field.assetId, field)
        : this.rvService.createField(this.data.tenantId, field);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: (error) => { console.error(error); this.isLoading = false; }
      });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
