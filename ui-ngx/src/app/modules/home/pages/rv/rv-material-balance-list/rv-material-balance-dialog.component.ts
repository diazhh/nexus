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
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RvService } from '@core/http/rv/rv.service';
import { RvReservoir } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';
import { RvMaterialBalance } from './rv-material-balance-list.component';

export enum RvDriveMechanism {
  WATER_DRIVE = 'WATER_DRIVE',
  GAS_CAP = 'GAS_CAP',
  SOLUTION_GAS = 'SOLUTION_GAS',
  COMBINATION = 'COMBINATION',
  COMPACTION = 'COMPACTION'
}

@Component({
  selector: 'tb-rv-material-balance-dialog',
  templateUrl: './rv-material-balance-dialog.component.html',
  styleUrls: ['./rv-material-balance-dialog.component.scss']
})
export class RvMaterialBalanceDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  reservoirs: RvReservoir[] = [];

  driveMechanisms = Object.values(RvDriveMechanism);
  driveMechanismLabels: { [key: string]: string } = {
    'WATER_DRIVE': 'Empuje por Agua',
    'GAS_CAP': 'Capa de Gas',
    'SOLUTION_GAS': 'Gas en Solucion',
    'COMBINATION': 'Combinado',
    'COMPACTION': 'Compactacion'
  };

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvMaterialBalanceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; materialBalance?: RvMaterialBalance }
  ) {
    this.isEditMode = !!data.materialBalance;
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
    const m = this.data.materialBalance;
    this.form = this.fb.group({
      // Basic info
      name: [m?.name || '', Validators.required],
      reservoirAssetId: [m?.reservoirAssetId || '', Validators.required],
      analysisDate: [m?.analysisDate ? new Date(m.analysisDate) : new Date()],

      // Initial conditions
      initialPressurePsi: [m?.initialPressurePsi || null, Validators.required],
      bubblePointPressurePsi: [m?.bubblePointPressurePsi || null],
      initialTemperatureF: [m?.initialTemperatureF || null],
      initialWaterSatFrac: [m?.initialWaterSatFrac || 0.25],
      initialOilFvfRbStb: [m?.initialOilFvfRbStb || null],
      initialGasOilRatioScfStb: [m?.['initialGasOilRatioScfStb'] || null],

      // Water influx parameters
      aquiferModel: [m?.['aquiferModel'] || 'NONE'],
      aquiferVolumeFactor: [m?.['aquiferVolumeFactor'] || null],
      aquiferPermeabilityMd: [m?.['aquiferPermeabilityMd'] || null],
      aquiferEncroachmentAngle: [m?.['aquiferEncroachmentAngle'] || 360],

      // Gas cap parameters
      initialGasCapRatio: [m?.['initialGasCapRatio'] || 0],
      gasFvfAtPi: [m?.['gasFvfAtPi'] || null],

      // Results (read-only, populated by analysis)
      calculatedOoipMmbbl: [m?.calculatedOoipMmbbl || null],
      driveMechanism: [m?.driveMechanism || ''],

      // Data points for Havlena-Odeh plot
      dataPoints: this.fb.array([])
    });

    // Populate data points if editing
    if (m?.dataPoints && Array.isArray(m.dataPoints)) {
      m.dataPoints.forEach(dp => this.addDataPoint(dp));
    }
  }

  get dataPoints(): FormArray {
    return this.form.get('dataPoints') as FormArray;
  }

  addDataPoint(dp?: any): void {
    const dpForm = this.fb.group({
      date: [dp?.date ? new Date(dp.date) : new Date()],
      pressurePsi: [dp?.pressurePsi || null, Validators.required],
      cumulativeOilProducedMstb: [dp?.cumulativeOilProducedMstb || null, Validators.required],
      cumulativeGasProducedMmscf: [dp?.cumulativeGasProducedMmscf || null],
      cumulativeWaterProducedMstb: [dp?.cumulativeWaterProducedMstb || null],
      cumulativeWaterInjectedMstb: [dp?.cumulativeWaterInjectedMstb || null],
      cumulativeGasInjectedMmscf: [dp?.cumulativeGasInjectedMmscf || null],
      oilFvfRbStb: [dp?.oilFvfRbStb || null],
      gasOilRatioScfStb: [dp?.gasOilRatioScfStb || null]
    });
    this.dataPoints.push(dpForm);
  }

  removeDataPoint(index: number): void {
    this.dataPoints.removeAt(index);
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const formValue = this.form.value;

      // Convert dates
      if (formValue.analysisDate instanceof Date) {
        formValue.analysisDate = formValue.analysisDate.getTime();
      }
      formValue.dataPoints = formValue.dataPoints.map((dp: any) => ({
        ...dp,
        date: dp.date instanceof Date ? dp.date.getTime() : dp.date
      }));

      const mbe: RvMaterialBalance = { ...this.data.materialBalance, ...formValue };

      const op = this.isEditMode
        ? this.rvService.updateMaterialBalanceStudy(this.data.materialBalance.assetId, mbe)
        : this.rvService.createMaterialBalanceStudy(this.data.tenantId, mbe);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
