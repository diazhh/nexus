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
import { RvWell, RvReservoir, RvWellType, RvWellStatus, RvWellCategory, RvLiftMethod } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-rv-well-dialog',
  templateUrl: './rv-well-dialog.component.html',
  styleUrls: ['./rv-well-dialog.component.scss']
})
export class RvWellDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  reservoirs: RvReservoir[] = [];

  wellTypes = Object.values(RvWellType);
  wellStatuses = Object.values(RvWellStatus);
  wellCategories = Object.values(RvWellCategory);
  liftMethods = Object.values(RvLiftMethod);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvWellDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; well?: RvWell }
  ) {
    this.isEditMode = !!data.well;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadReservoirs();
  }

  loadReservoirs(): void {
    this.rvService.getReservoirs(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.reservoirs = pageData.data
    });
  }

  buildForm(): void {
    const w = this.data.well;
    this.form = this.fb.group({
      name: [w?.name || '', Validators.required],
      uwi: [w?.uwi || ''],
      apiNumber: [w?.apiNumber || ''],
      reservoirAssetId: [w?.reservoirAssetId || ''],
      wellType: [w?.wellType || 'PRODUCER'],
      wellStatus: [w?.wellStatus || 'DRILLING'],
      wellCategory: [w?.wellCategory || 'VERTICAL'],
      operatorName: [w?.operatorName || ''],
      surfaceLatitude: [w?.surfaceLatitude || null],
      surfaceLongitude: [w?.surfaceLongitude || null],
      totalDepthMdM: [w?.totalDepthMdM || null],
      totalDepthTvdM: [w?.totalDepthTvdM || null],
      currentRateBopd: [w?.currentRateBopd || null],
      currentGasRateMscfd: [w?.currentGasRateMscfd || null],
      currentWaterCutPercent: [w?.currentWaterCutPercent || null],
      artificialLiftType: [w?.artificialLiftType || 'NATURAL']
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const well: RvWell = { ...this.data.well, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateWell(this.data.well.assetId, well)
        : this.rvService.createWell(this.data.tenantId, well);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
