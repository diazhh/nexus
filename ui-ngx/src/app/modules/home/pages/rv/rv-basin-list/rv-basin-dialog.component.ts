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
import { RvBasin, RvBasinType } from '@shared/models/rv/rv.models';

export interface RvBasinDialogData {
  tenantId: string;
  basin?: RvBasin;
}

@Component({
  selector: 'tb-rv-basin-dialog',
  templateUrl: './rv-basin-dialog.component.html',
  styleUrls: ['./rv-basin-dialog.component.scss']
})
export class RvBasinDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;

  basinTypes = Object.values(RvBasinType);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvBasinDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RvBasinDialogData
  ) {
    this.isEditMode = !!data.basin;
  }

  ngOnInit(): void {
    this.buildForm();
  }

  buildForm(): void {
    const basin = this.data.basin;

    this.form = this.fb.group({
      name: [basin?.name || '', [Validators.required, Validators.maxLength(255)]],
      label: [basin?.label || ''],
      code: [basin?.code || ''],
      basinType: [basin?.basinType || ''],
      country: [basin?.country || 'Venezuela'],
      state: [basin?.state || ''],
      areaKm2: [basin?.areaKm2 || null, [Validators.min(0)]],
      geologicAge: [basin?.geologicAge || ''],
      tectonicSetting: [basin?.tectonicSetting || ''],
      petroleumSystem: [basin?.petroleumSystem || '']
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const formValue = this.form.value;

      const basin: RvBasin = {
        ...this.data.basin,
        ...formValue
      };

      const saveOperation = this.isEditMode
        ? this.rvService.updateBasin(this.data.basin.assetId, basin)
        : this.rvService.createBasin(this.data.tenantId, basin);

      saveOperation.subscribe({
        next: (result) => {
          this.dialogRef.close(result);
        },
        error: (error) => {
          console.error('Save error:', error);
          this.isLoading = false;
        }
      });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
