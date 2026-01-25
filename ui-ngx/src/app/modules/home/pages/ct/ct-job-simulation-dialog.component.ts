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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CTSimulationService } from '@core/http/ct/ct-simulation.service';
import { JobParameters, SimulationResult } from '@shared/models/ct/ct-simulation.model';

export interface CTJobSimulationDialogData {
  jobId?: string;
  jobName?: string;
  customMode?: boolean;
}

@Component({
  selector: 'tb-ct-job-simulation-dialog',
  templateUrl: './ct-job-simulation-dialog.component.html',
  styleUrls: ['./ct-job-simulation-dialog.component.scss']
})
export class CTJobSimulationDialogComponent implements OnInit {

  simulationForm: FormGroup;
  simulationResult: SimulationResult | null = null;
  isSimulating = false;
  simulationError: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<CTJobSimulationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTJobSimulationDialogData,
    private fb: FormBuilder,
    private simulationService: CTSimulationService
  ) {}

  ngOnInit() {
    this.buildForm();
  }

  private buildForm() {
    if (this.data.customMode) {
      this.simulationForm = this.fb.group({
        wellName: ['', Validators.required],
        targetDepthFt: [10000, [Validators.required, Validators.min(100)]],
        wellboreDiameterInch: [7.0, [Validators.required, Validators.min(2)]],
        maxInclinationDeg: [30, [Validators.required, Validators.min(0), Validators.max(90)]],
        tubingOdInch: [2.375, [Validators.required, Validators.min(1)]],
        tubingIdInch: [1.995, [Validators.required, Validators.min(0.5)]],
        tubingLengthFt: [20000, [Validators.required, Validators.min(1000)]],
        fluidDensityPpg: [8.33, [Validators.required, Validators.min(7), Validators.max(20)]],
        pumpRateBpm: [5.0, [Validators.min(0.1), Validators.max(20)]],
        maxPressurePsi: [10000, [Validators.required, Validators.min(1000)]],
        maxRunningSpeedFtMin: [100, [Validators.required, Validators.min(10)]],
        unitMaxPressurePsi: [15000, [Validators.required, Validators.min(5000)]],
        unitMaxTensionLbf: [50000, [Validators.required, Validators.min(10000)]],
        estimatedTreatmentHours: [2.0, [Validators.min(0.1)]]
      });
    }
  }

  runSimulation() {
    if (this.data.customMode && this.simulationForm.invalid) {
      return;
    }

    this.isSimulating = true;
    this.simulationError = null;
    this.simulationResult = null;

    const simulation$ = this.data.customMode
      ? this.simulationService.simulateCustomJob(this.simulationForm.value as JobParameters)
      : this.simulationService.simulateJob(this.data.jobId!);

    simulation$.subscribe({
      next: (result) => {
        this.simulationResult = result;
        this.isSimulating = false;
      },
      error: (error) => {
        this.simulationError = error.message || 'Simulation failed';
        this.isSimulating = false;
      }
    });
  }

  getFeasibilityClass(): string {
    if (!this.simulationResult?.feasibility) return '';
    return this.simulationResult.feasibility.isFeasible ? 'feasible' : 'not-feasible';
  }

  getRiskSeverityClass(severity: string): string {
    const severityMap: { [key: string]: string } = {
      'LOW': 'risk-low',
      'MEDIUM': 'risk-medium',
      'HIGH': 'risk-high',
      'CRITICAL': 'risk-critical'
    };
    return severityMap[severity] || '';
  }

  close() {
    this.dialogRef.close();
  }
}
