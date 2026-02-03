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
import { PageLink } from '@shared/models/page/page-link';

export enum RvCoreType {
  CONVENTIONAL = 'CONVENTIONAL',
  SIDEWALL = 'SIDEWALL'
}

export enum RvLithology {
  SANDSTONE = 'SANDSTONE',
  LIMESTONE = 'LIMESTONE',
  DOLOMITE = 'DOLOMITE',
  SHALE = 'SHALE',
  MARL = 'MARL',
  CHALK = 'CHALK',
  CONGLOMERATE = 'CONGLOMERATE'
}

@Component({
  selector: 'tb-rv-core-dialog',
  templateUrl: './rv-core-dialog.component.html',
  styleUrls: ['./rv-core-dialog.component.scss']
})
export class RvCoreDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  wells: any[] = [];

  coreTypes = Object.values(RvCoreType);
  lithologies = Object.values(RvLithology);

  coreTypeLabels: any = {
    'CONVENTIONAL': 'Convencional',
    'SIDEWALL': 'Sidewall'
  };

  lithologyLabels: any = {
    'SANDSTONE': 'Arenisca',
    'LIMESTONE': 'Caliza',
    'DOLOMITE': 'Dolomita',
    'SHALE': 'Lutita',
    'MARL': 'Marga',
    'CHALK': 'Creta',
    'CONGLOMERATE': 'Conglomerado'
  };

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvCoreDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; core?: any }
  ) {
    this.isEditMode = !!data.core;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadWells();
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(1000, 0)).subscribe({
      next: (pageData) => this.wells = pageData.data
    });
  }

  buildForm(): void {
    const c = this.data.core;
    this.form = this.fb.group({
      // Identification
      name: [c?.name || '', Validators.required],
      coreNumber: [c?.coreNumber || ''],
      wellAssetId: [c?.wellAssetId || '', Validators.required],
      coreType: [c?.coreType || 'CONVENTIONAL', Validators.required],

      // Depth Interval
      topDepthMd: [c?.topDepthMd || null],
      bottomDepthMd: [c?.bottomDepthMd || null],
      topDepthTvd: [c?.topDepthTvd || null],
      bottomDepthTvd: [c?.bottomDepthTvd || null],

      // Recovery
      cutLengthM: [c?.cutLengthM || null],
      recoveredLengthM: [c?.recoveredLengthM || null],
      recoveryPercentage: [c?.recoveryPercentage || null],

      // Lithology and Shows
      lithology: [c?.lithology || ''],
      hydrocarbonShows: [c?.hydrocarbonShows || ''],
      fluorescence: [c?.fluorescence || false],
      odor: [c?.odor || false],

      // RCA Results (Routine Core Analysis)
      porosityFrac: [c?.porosityFrac || null],
      permeabilityMd: [c?.permeabilityMd || null],
      grainDensityGCc: [c?.grainDensityGCc || null],
      bulkDensityGCc: [c?.bulkDensityGCc || null],

      // SCAL Results (Special Core Analysis)
      hasScalData: [c?.hasScalData || false],
      swirr: [c?.swirr || null],
      sor: [c?.sor || null],
      krwEndpoint: [c?.krwEndpoint || null],
      kroEndpoint: [c?.kroEndpoint || null],

      // Metadata
      analysisDate: [c?.analysisDate || null],
      laboratory: [c?.laboratory || ''],
      notes: [c?.notes || '']
    });

    // Auto-calculate recovery percentage
    this.form.get('cutLengthM').valueChanges.subscribe(() => this.calculateRecovery());
    this.form.get('recoveredLengthM').valueChanges.subscribe(() => this.calculateRecovery());
  }

  calculateRecovery(): void {
    const cut = this.form.get('cutLengthM').value;
    const recovered = this.form.get('recoveredLengthM').value;
    if (cut !== null && recovered !== null && cut > 0) {
      this.form.patchValue({ recoveryPercentage: (recovered / cut) * 100 }, { emitEvent: false });
    }
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const core: any = { ...this.data.core, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateCore(this.data.core.assetId, core)
        : this.rvService.createCore(this.data.tenantId, core);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
