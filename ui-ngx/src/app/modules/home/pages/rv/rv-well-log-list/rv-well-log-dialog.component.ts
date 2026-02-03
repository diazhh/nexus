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
import { RvWellLog, RvReservoir, RvLithology } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export enum RvWellLogStatus {
  PRODUCTIVE = 'PRODUCTIVE',
  DEPLETED = 'DEPLETED',
  WATER_OUT = 'WATER_OUT',
  BEHIND_PIPE = 'BEHIND_PIPE'
}

export enum RvFluidType {
  OIL = 'OIL',
  GAS = 'GAS',
  WATER = 'WATER',
  OIL_GAS = 'OIL_GAS'
}

@Component({
  selector: 'tb-rv-well-log-dialog',
  templateUrl: './rv-well-log-dialog.component.html',
  styleUrls: ['./rv-well-log-dialog.component.scss']
})
export class RvWellLogDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  wells: any[] = [];

  lithologies = Object.values(RvLithology);
  fluidTypes = Object.values(RvFluidType);
  logTypes = ['WIRELINE', 'LWD', 'MWD', 'PRODUCTION_LOG'];
  dataQualities = ['EXCELLENT', 'GOOD', 'FAIR', 'POOR'];

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvWellLogDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; wellLog?: RvWellLog }
  ) {
    this.isEditMode = !!data.wellLog;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadWells();
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.wells = pageData.data
    });
  }

  buildForm(): void {
    const wl = this.data.wellLog;
    this.form = this.fb.group({
      // Identification
      name: [wl?.name || '', Validators.required],
      runName: [wl?.runName || ''],
      runNumber: [wl?.runNumber || null],
      wellAssetId: [wl?.wellAssetId || ''],

      // Log Info
      logDate: [wl?.logDate || null],
      logType: [wl?.logType || ''],
      serviceCompany: [wl?.serviceCompany || ''],

      // Depths
      topDepthMdM: [wl?.topDepthMdM || null],
      bottomDepthMdM: [wl?.bottomDepthMdM || null],
      topDepthTvdM: [wl?.topDepthTvdM || null],
      bottomDepthTvdM: [wl?.bottomDepthTvdM || null],

      // Log Parameters
      loggingSpeedMMin: [wl?.loggingSpeedMMin || null],
      samplingRateM: [wl?.samplingRateM || null],
      availableCurves: [wl?.availableCurves || []],

      // Classification
      lithology: [wl?.lithology || ''],
      fluidType: [wl?.fluidType || ''],
      dataQuality: [wl?.dataQuality || 'GOOD'],
      dataFormat: [wl?.dataFormat || '']
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const wellLog: RvWellLog = { ...this.data.wellLog, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateWellLog(this.data.wellLog.assetId, wellLog)
        : this.rvService.createWellLog(this.data.tenantId, wellLog);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
