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

export enum RvFaultType {
  NORMAL = 'NORMAL',
  REVERSE = 'REVERSE',
  STRIKE_SLIP = 'STRIKE_SLIP'
}

export enum RvSealingPotential {
  SEALING = 'SEALING',
  PARTIALLY_SEALING = 'PARTIALLY_SEALING',
  NON_SEALING = 'NON_SEALING'
}

@Component({
  selector: 'tb-rv-fault-dialog',
  templateUrl: './rv-fault-dialog.component.html',
  styleUrls: ['./rv-fault-dialog.component.scss']
})
export class RvFaultDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  fields: any[] = [];
  reservoirs: any[] = [];

  faultTypes = Object.values(RvFaultType);
  sealingPotentials = Object.values(RvSealingPotential);

  faultTypeLabels: any = {
    'NORMAL': 'Normal',
    'REVERSE': 'Inversa',
    'STRIKE_SLIP': 'Rumbo'
  };

  sealingPotentialLabels: any = {
    'SEALING': 'Sellante',
    'PARTIALLY_SEALING': 'Parcialmente Sellante',
    'NON_SEALING': 'No Sellante'
  };

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvFaultDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; fault?: any }
  ) {
    this.isEditMode = !!data.fault;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadFields();
    this.loadReservoirs();
  }

  loadFields(): void {
    this.rvService.getFields(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.fields = pageData.data
    });
  }

  loadReservoirs(): void {
    this.rvService.getReservoirs(this.data.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => this.reservoirs = pageData.data
    });
  }

  buildForm(): void {
    const f = this.data.fault;
    this.form = this.fb.group({
      // Identification
      name: [f?.name || '', Validators.required],
      faultCode: [f?.faultCode || ''],
      fieldAssetId: [f?.fieldAssetId || ''],
      reservoirAssetId: [f?.reservoirAssetId || ''],

      // Fault Type
      faultType: [f?.faultType || 'NORMAL', Validators.required],

      // Geometry
      strikeDeg: [f?.strikeDeg || null],
      dipDeg: [f?.dipDeg || null],
      lengthM: [f?.lengthM || null],
      throwM: [f?.throwM || null],
      heaveM: [f?.heaveM || null],

      // Sealing Analysis
      sealingPotential: [f?.sealingPotential || 'NON_SEALING'],
      shaleSmearedGouge: [f?.shaleSmearedGouge || false],
      juxtapositionSeal: [f?.juxtapositionSeal || false],

      // Compartmentalization
      causesCompartmentalization: [f?.causesCompartmentalization || false],
      compartmentNames: [f?.compartmentNames || ''],

      // Additional Data
      seismicQuality: [f?.seismicQuality || ''],
      interpretation: [f?.interpretation || ''],
      notes: [f?.notes || '']
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const fault: any = { ...this.data.fault, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateFault(this.data.fault.assetId, fault)
        : this.rvService.createFault(this.data.tenantId, fault);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
