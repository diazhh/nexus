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
import { RvPvtStudy, RvReservoir, RvWell, RvPvtCorrelation } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export enum RvSampleType {
  BOTTOMHOLE = 'BOTTOMHOLE',
  RECOMBINED = 'RECOMBINED',
  SEPARATOR = 'SEPARATOR'
}

@Component({
  selector: 'tb-rv-pvt-study-dialog',
  templateUrl: './rv-pvt-study-dialog.component.html',
  styleUrls: ['./rv-pvt-study-dialog.component.scss']
})
export class RvPvtStudyDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  reservoirs: RvReservoir[] = [];
  wells: RvWell[] = [];

  sampleTypes = Object.values(RvSampleType);
  correlations = Object.values(RvPvtCorrelation);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvPvtStudyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; pvtStudy?: RvPvtStudy }
  ) {
    this.isEditMode = !!data.pvtStudy;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadReservoirs();
    this.loadWells();
  }

  loadReservoirs(): void {
    this.rvService.getReservoirs(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.reservoirs = pageData.data
    });
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.wells = pageData.data
    });
  }

  buildForm(): void {
    const p = this.data.pvtStudy;
    this.form = this.fb.group({
      // Basic info
      name: [p?.name || '', Validators.required],
      studyCode: [p?.studyCode || ''],
      reservoirAssetId: [p?.reservoirAssetId || ''],
      wellAssetId: [p?.wellAssetId || ''],
      laboratoryName: [p?.laboratoryName || ''],

      // Sample info
      sampleDate: [p?.sampleDate ? new Date(p.sampleDate) : null],
      sampleType: [p?.sampleType || 'BOTTOMHOLE'],
      sampleDepthM: [p?.sampleDepthM || null],
      samplePressurePsi: [p?.samplePressurePsi || null],
      sampleTemperatureF: [p?.sampleTemperatureF || null],

      // Oil properties
      apiGravity: [p?.apiGravity || null],
      specificGravityOil: [p?.specificGravityOil || null],
      stockTankOilViscosityCp: [p?.stockTankOilViscosityCp || null],

      // Bubble point properties
      bubblePointPressurePsi: [p?.bubblePointPressurePsi || null],
      solutionGorAtPbScfStb: [p?.solutionGorAtPbScfStb || p?.solutionGorScfStb || null],
      oilFvfAtPbRbStb: [p?.oilFvfAtPbRbStb || p?.boAtPbRbStb || null],
      oilViscosityAtPbCp: [p?.oilViscosityAtPbCp || null],

      // Gas properties
      gasSpecificGravity: [p?.gasSpecificGravity || p?.gasGravity || null],
      gasZFactorAtPb: [p?.gasZFactorAtPb || null],

      // Water properties
      waterSalinity: [p?.waterSalinity || null],
      waterFvfAtReservoirRbStb: [p?.waterFvfAtReservoirRbStb || null],

      // Reservoir conditions
      initialPressurePsia: [p?.initialPressurePsia || null],
      reservoirTemperatureF: [p?.reservoirTemperatureF || null],

      // Correlations
      usesCorrelations: [p?.usesCorrelations || false],
      pbCorrelation: [p?.pbCorrelation || 'STANDING'],
      boCorrelation: [p?.boCorrelation || 'STANDING'],
      viscosityCorrelation: [p?.viscosityCorrelation || 'BEGGS_ROBINSON'],

      // Foamy oil (Venezuela)
      hasFoamyBehavior: [p?.hasFoamyBehavior || false],
      pseudoBubblePointPsi: [p?.pseudoBubblePointPsi || p?.pseudoBubblePoint || null],
      foamyOilFactor: [p?.foamyOilFactor || null]
    });

    // Auto-calculate specific gravity from API
    this.form.get('apiGravity').valueChanges.subscribe(api => {
      if (api !== null && api > 0) {
        const sg = 141.5 / (api + 131.5);
        this.form.patchValue({ specificGravityOil: parseFloat(sg.toFixed(4)) }, { emitEvent: false });
      }
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const formValue = this.form.value;

      // Convert date to timestamp
      if (formValue.sampleDate instanceof Date) {
        formValue.sampleDate = formValue.sampleDate.getTime();
      }

      const pvtStudy: RvPvtStudy = { ...this.data.pvtStudy, ...formValue };

      const op = this.isEditMode
        ? this.rvService.updatePvtStudy(this.data.pvtStudy.assetId, pvtStudy)
        : this.rvService.createPvtStudy(this.data.tenantId, pvtStudy);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
