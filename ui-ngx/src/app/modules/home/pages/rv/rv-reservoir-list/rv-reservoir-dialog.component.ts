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
import { RvReservoir, RvField, RvLithology, RvReservoirType, RvDriveType } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-rv-reservoir-dialog',
  templateUrl: './rv-reservoir-dialog.component.html',
  styleUrls: ['./rv-reservoir-dialog.component.scss']
})
export class RvReservoirDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  fields: RvField[] = [];

  lithologies = Object.values(RvLithology);
  reservoirTypes = Object.values(RvReservoirType);
  driveTypes = Object.values(RvDriveType);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvReservoirDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; reservoir?: RvReservoir }
  ) {
    this.isEditMode = !!data.reservoir;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadFields();
  }

  loadFields(): void {
    this.rvService.getFields(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.fields = pageData.data
    });
  }

  buildForm(): void {
    const r = this.data.reservoir;
    this.form = this.fb.group({
      name: [r?.name || '', Validators.required],
      code: [r?.code || ''],
      fieldAssetId: [r?.fieldAssetId || ''],
      formationName: [r?.formationName || ''],
      lithology: [r?.lithology || ''],
      reservoirType: [r?.reservoirType || 'CONVENTIONAL'],
      driveType: [r?.driveType || ''],
      areaAcres: [r?.areaAcres || null],
      grossThicknessM: [r?.grossThicknessM || null],
      netPayThicknessM: [r?.netPayThicknessM || null],
      avgPorosityFrac: [r?.avgPorosityFrac || null],
      avgPermeabilityMd: [r?.avgPermeabilityMd || null],
      avgWaterSatFrac: [r?.avgWaterSatFrac || null],
      initialReservoirPressurePsi: [r?.initialReservoirPressurePsi || null],
      reservoirTemperatureF: [r?.reservoirTemperatureF || null],
      apiGravity: [r?.apiGravity || null],
      oilFvfRbStb: [r?.oilFvfRbStb || null],
      hasFoamyOil: [r?.hasFoamyOil || false],
      requiresDiluent: [r?.requiresDiluent || false]
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const reservoir: RvReservoir = { ...this.data.reservoir, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateReservoir(this.data.reservoir.assetId, reservoir)
        : this.rvService.createReservoir(this.data.tenantId, reservoir);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
