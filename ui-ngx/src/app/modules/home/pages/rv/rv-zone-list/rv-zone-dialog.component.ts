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
import { RvZone, RvReservoir, RvLithology } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export enum RvZoneStatus {
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
  selector: 'tb-rv-zone-dialog',
  templateUrl: './rv-zone-dialog.component.html',
  styleUrls: ['./rv-zone-dialog.component.scss']
})
export class RvZoneDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  reservoirs: RvReservoir[] = [];

  lithologies = Object.values(RvLithology);
  zoneStatuses = Object.values(RvZoneStatus);
  fluidTypes = Object.values(RvFluidType);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvZoneDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; zone?: RvZone }
  ) {
    this.isEditMode = !!data.zone;
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
    const z = this.data.zone;
    this.form = this.fb.group({
      // Identification
      name: [z?.name || '', Validators.required],
      code: [z?.code || ''],
      reservoirAssetId: [z?.reservoirAssetId || '', Validators.required],
      zoneNumber: [z?.zoneNumber || null],

      // Depths
      topDepthMdM: [z?.topDepthMdM || null],
      bottomDepthMdM: [z?.bottomDepthMdM || null],
      topDepthTvdM: [z?.topDepthTvdM || null],
      bottomDepthTvdM: [z?.bottomDepthTvdM || null],

      // Thickness
      grossThicknessM: [z?.grossThicknessM || null],
      netPayThicknessM: [z?.netPayThicknessM || null],
      netToGrossRatio: [z?.netToGrossRatio || null],

      // Petrophysics
      porosityFrac: [z?.porosityFrac || null],
      permeabilityMd: [z?.permeabilityMd || null],
      waterSaturationFrac: [z?.waterSaturationFrac || null],
      shaleFrac: [z?.shaleFrac || null],

      // Classification
      lithology: [z?.lithology || ''],
      fluidType: [z?.fluidType || ''],
      zoneStatus: [z?.zoneStatus || 'PRODUCTIVE'],
      isPerforated: [z?.isPerforated || false]
    });

    // Auto-calculate thickness when depths change
    this.form.get('topDepthMdM').valueChanges.subscribe(() => this.calculateThickness());
    this.form.get('bottomDepthMdM').valueChanges.subscribe(() => this.calculateThickness());

    // Auto-calculate NTG when thicknesses change
    this.form.get('grossThicknessM').valueChanges.subscribe(() => this.calculateNTG());
    this.form.get('netPayThicknessM').valueChanges.subscribe(() => this.calculateNTG());
  }

  calculateThickness(): void {
    const top = this.form.get('topDepthMdM').value;
    const bottom = this.form.get('bottomDepthMdM').value;
    if (top !== null && bottom !== null && bottom > top) {
      this.form.patchValue({ grossThicknessM: bottom - top }, { emitEvent: false });
    }
  }

  calculateNTG(): void {
    const gross = this.form.get('grossThicknessM').value;
    const net = this.form.get('netPayThicknessM').value;
    if (gross !== null && net !== null && gross > 0) {
      this.form.patchValue({ netToGrossRatio: net / gross }, { emitEvent: false });
    }
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const zone: RvZone = { ...this.data.zone, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateZone(this.data.zone.assetId, zone)
        : this.rvService.createZone(this.data.tenantId, zone);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
