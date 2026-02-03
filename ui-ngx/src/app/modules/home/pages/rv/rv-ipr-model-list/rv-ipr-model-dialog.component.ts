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
import { RvIprModel, RvWell, RvCompletion } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export enum RvIprMethod {
  VOGEL = 'VOGEL',
  DARCY = 'DARCY',
  FETKOVICH = 'FETKOVICH',
  JONES = 'JONES',
  COMPOSITE = 'COMPOSITE'
}

export enum RvTestType {
  PRODUCTION_TEST = 'PRODUCTION_TEST',
  BUILDUP = 'BUILDUP',
  DST = 'DST'
}

@Component({
  selector: 'tb-rv-ipr-model-dialog',
  templateUrl: './rv-ipr-model-dialog.component.html',
  styleUrls: ['./rv-ipr-model-dialog.component.scss']
})
export class RvIprModelDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  wells: RvWell[] = [];
  completions: RvCompletion[] = [];

  iprMethods = Object.values(RvIprMethod);
  testTypes = Object.values(RvTestType);

  iprMethodLabels: { [key: string]: string } = {
    'VOGEL': 'Vogel (Reservorios saturados)',
    'DARCY': 'Darcy (Flujo laminar)',
    'FETKOVICH': 'Fetkovich (Backpressure)',
    'JONES': 'Jones (Composite)',
    'COMPOSITE': 'Compuesto (Vogel + Darcy)'
  };

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvIprModelDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; iprModel?: RvIprModel }
  ) {
    this.isEditMode = !!data.iprModel;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadWells();
    this.loadCompletions();
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.wells = pageData.data
    });
  }

  loadCompletions(): void {
    this.rvService.getCompletions(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.completions = pageData.data
    });
  }

  buildForm(): void {
    const ipr = this.data.iprModel;
    this.form = this.fb.group({
      // Basic info
      name: [ipr?.name || '', Validators.required],
      modelCode: [ipr?.modelCode || ''],
      wellAssetId: [ipr?.wellAssetId || '', Validators.required],
      completionAssetId: [ipr?.completionAssetId || ''],
      analysisDate: [ipr?.analysisDate ? new Date(ipr.analysisDate) : new Date()],
      iprMethod: [ipr?.iprMethod || 'VOGEL', Validators.required],

      // Reservoir conditions
      reservoirPressurePsi: [ipr?.reservoirPressurePsi || null, Validators.required],
      bubblePointPressurePsi: [ipr?.bubblePointPressurePsi || null],
      reservoirTemperatureF: [ipr?.reservoirTemperatureF || null],
      isBelowBubblePoint: [ipr?.isBelowBubblePoint !== undefined ? ipr.isBelowBubblePoint : false],

      // Test data
      testDate: [ipr?.testDate ? new Date(ipr.testDate) : null],
      testType: [ipr?.testType || 'PRODUCTION_TEST'],
      testRateBopd: [ipr?.testRateBopd || null],
      testPwfPsi: [ipr?.testPwfPsi || null],

      // Productivity indices
      productivityIndexBpdPsi: [ipr?.productivityIndexBpdPsi || null],
      productivityIndexAbovePb: [ipr?.productivityIndexAbovePb || null],
      productivityIndexBelowPb: [ipr?.productivityIndexBelowPb || null],

      // Calculated results (read-only)
      qmaxBopd: [{ value: ipr?.qmaxBopd || null, disabled: true }],
      vogelCoefficient: [{ value: ipr?.vogelCoefficient || null, disabled: true }],

      // Fetkovich parameters
      fetkovichC: [ipr?.fetkovichC || null],
      fetkovichN: [ipr?.fetkovichN || null],

      // Jones parameters
      jonesA: [ipr?.jonesA || null],
      jonesB: [ipr?.jonesB || null],

      // Current operating point
      currentPwfPsi: [ipr?.currentPwfPsi || null],
      currentRateBopd: [ipr?.currentRateBopd || null],
      operatingEfficiencyPercent: [ipr?.operatingEfficiencyPercent || null],

      // Well damage
      skinFactor: [ipr?.skinFactor || null],
      damageRatio: [ipr?.damageRatio || null],
      flowEfficiency: [ipr?.flowEfficiency || null],
      idealQmaxBopd: [ipr?.idealQmaxBopd || null]
    });

    // Watch for method changes to show/hide relevant fields
    this.form.get('iprMethod')?.valueChanges.subscribe(method => {
      this.updateFormValidators(method);
    });
  }

  updateFormValidators(method: string): void {
    // Clear all method-specific validators first
    this.form.get('fetkovichC')?.clearValidators();
    this.form.get('fetkovichN')?.clearValidators();
    this.form.get('jonesA')?.clearValidators();
    this.form.get('jonesB')?.clearValidators();

    // Add validators based on method
    if (method === 'FETKOVICH') {
      this.form.get('fetkovichC')?.setValidators([Validators.required]);
      this.form.get('fetkovichN')?.setValidators([Validators.required]);
    } else if (method === 'JONES') {
      this.form.get('jonesA')?.setValidators([Validators.required]);
      this.form.get('jonesB')?.setValidators([Validators.required]);
    }

    // Update validity
    this.form.get('fetkovichC')?.updateValueAndValidity();
    this.form.get('fetkovichN')?.updateValueAndValidity();
    this.form.get('jonesA')?.updateValueAndValidity();
    this.form.get('jonesB')?.updateValueAndValidity();
  }

  showFetkovichFields(): boolean {
    return this.form.get('iprMethod')?.value === 'FETKOVICH';
  }

  showJonesFields(): boolean {
    return this.form.get('iprMethod')?.value === 'JONES';
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const formValue = this.form.getRawValue();

      // Convert dates to timestamps
      if (formValue.analysisDate instanceof Date) {
        formValue.analysisDate = formValue.analysisDate.getTime();
      }
      if (formValue.testDate instanceof Date) {
        formValue.testDate = formValue.testDate.getTime();
      }

      const iprModel: RvIprModel = { ...this.data.iprModel, ...formValue };

      const operation = this.isEditMode
        ? this.rvService.updateIprModel(this.data.iprModel.assetId, iprModel)
        : this.rvService.createIprModel(this.data.tenantId, iprModel);

      operation.subscribe({
        next: (result) => {
          this.isLoading = false;
          this.dialogRef.close(result);
        },
        error: (err) => {
          console.error('Error saving IPR model:', err);
          this.isLoading = false;
        }
      });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
