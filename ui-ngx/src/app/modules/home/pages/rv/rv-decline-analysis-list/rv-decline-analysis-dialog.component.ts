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
import { RvDeclineAnalysis, RvWell } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export enum RvDeclineType {
  EXPONENTIAL = 'EXPONENTIAL',
  HYPERBOLIC = 'HYPERBOLIC',
  HARMONIC = 'HARMONIC'
}

@Component({
  selector: 'tb-rv-decline-analysis-dialog',
  templateUrl: './rv-decline-analysis-dialog.component.html',
  styleUrls: ['./rv-decline-analysis-dialog.component.scss']
})
export class RvDeclineAnalysisDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  wells: RvWell[] = [];
  reservoirs: any[] = [];

  declineTypes = Object.values(RvDeclineType);

  declineTypeLabels: { [key: string]: string } = {
    'EXPONENTIAL': 'Exponencial (b=0)',
    'HYPERBOLIC': 'Hiperbólico (0<b<1)',
    'HARMONIC': 'Armónico (b=1)'
  };

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvDeclineAnalysisDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; analysis?: RvDeclineAnalysis }
  ) {
    this.isEditMode = !!data.analysis;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadWells();
    this.loadReservoirs();
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.wells = pageData.data
    });
  }

  loadReservoirs(): void {
    this.rvService.getReservoirs(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.reservoirs = pageData.data
    });
  }

  buildForm(): void {
    const analysis = this.data.analysis;
    this.form = this.fb.group({
      // Basic info
      name: [analysis?.name || '', Validators.required],
      analysisCode: [analysis?.analysisCode || ''],
      wellAssetId: [analysis?.wellAssetId || '', Validators.required],
      reservoirAssetId: [analysis?.reservoirAssetId || ''],
      analysisDate: [analysis?.analysisDate ? new Date(analysis.analysisDate) : new Date()],
      declineType: [analysis?.declineType || 'EXPONENTIAL', Validators.required],

      // Arps parameters
      qiBopd: [analysis?.qiBopd || null, [Validators.required, Validators.min(0)]],
      diPerYear: [analysis?.diPerYear || null, [Validators.required, Validators.min(0), Validators.max(100)]],
      bExponent: [analysis?.bExponent !== undefined ? analysis.bExponent : 0, [Validators.required, Validators.min(0), Validators.max(1)]],

      // Economic limit
      economicLimitBopd: [analysis?.economicLimitBopd || 5, Validators.min(0)],

      // Historical data
      dataStartDate: [analysis?.dataStartDate ? new Date(analysis.dataStartDate) : null],
      dataEndDate: [analysis?.dataEndDate ? new Date(analysis.dataEndDate) : null],

      // Calculated results (read-only)
      eurBbl: [{ value: analysis?.eurBbl || null, disabled: true }],
      remainingReservesBbl: [{ value: analysis?.remainingReservesBbl || null, disabled: true }],
      cumulativeProductionBbl: [{ value: analysis?.cumulativeProductionBbl || null, disabled: true }],
      remainingLifeMonths: [{ value: analysis?.remainingLifeMonths || null, disabled: true }],
      r2Coefficient: [{ value: analysis?.r2Coefficient || null, disabled: true }],

      // Forecast results
      forecastEndDate: [{ value: analysis?.forecastEndDate ? new Date(analysis.forecastEndDate) : null, disabled: true }],
      forecastEurBbl: [{ value: analysis?.forecastEurBbl || null, disabled: true }]
    });

    // Watch for decline type changes to update b-exponent
    this.form.get('declineType')?.valueChanges.subscribe(type => {
      this.updateBExponent(type);
    });
  }

  updateBExponent(type: string): void {
    const bControl = this.form.get('bExponent');
    if (type === 'EXPONENTIAL') {
      bControl?.setValue(0);
      bControl?.disable();
    } else if (type === 'HARMONIC') {
      bControl?.setValue(1);
      bControl?.disable();
    } else {
      bControl?.enable();
    }
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const formValue = this.form.getRawValue();

      // Convert dates to timestamps
      if (formValue.analysisDate instanceof Date) {
        formValue.analysisDate = formValue.analysisDate.getTime();
      }
      if (formValue.dataStartDate instanceof Date) {
        formValue.dataStartDate = formValue.dataStartDate.getTime();
      }
      if (formValue.dataEndDate instanceof Date) {
        formValue.dataEndDate = formValue.dataEndDate.getTime();
      }

      const analysis: RvDeclineAnalysis = { ...this.data.analysis, ...formValue };

      const operation = this.isEditMode
        ? this.rvService.updateDeclineAnalysis(this.data.analysis.assetId, analysis)
        : this.rvService.createDeclineAnalysis(this.data.tenantId, analysis);

      operation.subscribe({
        next: (result) => {
          this.isLoading = false;
          this.dialogRef.close(result);
        },
        error: (err) => {
          console.error('Error saving decline analysis:', err);
          this.isLoading = false;
        }
      });
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
