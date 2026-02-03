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
import { RvCompletion, RvWell, RvZone, RvCompletionType, RvLiftMethod } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

export enum RvCompletionStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ISOLATED = 'ISOLATED',
  ABANDONED = 'ABANDONED'
}

@Component({
  selector: 'tb-rv-completion-dialog',
  templateUrl: './rv-completion-dialog.component.html',
  styleUrls: ['./rv-completion-dialog.component.scss']
})
export class RvCompletionDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  wells: RvWell[] = [];
  zones: RvZone[] = [];

  completionTypes = Object.values(RvCompletionType);
  completionStatuses = Object.values(RvCompletionStatus);
  liftMethods = Object.values(RvLiftMethod);

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvCompletionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; completion?: RvCompletion }
  ) {
    this.isEditMode = !!data.completion;
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadWells();
    this.loadZones();
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(200, 0)).subscribe({
      next: (pageData) => this.wells = pageData.data
    });
  }

  loadZones(): void {
    this.rvService.getZones(this.data.tenantId, new PageLink(200, 0)).subscribe({
      next: (pageData) => this.zones = pageData.data
    });
  }

  buildForm(): void {
    const c = this.data.completion;
    this.form = this.fb.group({
      // Basic info
      name: [c?.name || '', Validators.required],
      completionNumber: [c?.completionNumber || null],
      wellAssetId: [c?.wellAssetId || '', Validators.required],
      zoneAssetId: [c?.zoneAssetId || ''],

      // Type and status
      completionType: [c?.completionType || 'CASED_PERFORATED'],
      completionStatus: [c?.completionStatus || 'ACTIVE'],
      completionDate: [c?.completionDate ? new Date(c.completionDate) : null],

      // Perforation interval
      topPerforationMdM: [c?.topPerforationMdM || null],
      bottomPerforationMdM: [c?.bottomPerforationMdM || null],
      perforationIntervalM: [c?.perforationIntervalM || null],

      // Perforation details
      perforationDensitySpf: [c?.perforationDensitySpf || null],
      shotPhasing: [c?.shotPhasing || null],
      perforationDiameterIn: [c?.perforationDiameterIn || null],
      totalShots: [c?.totalShots || null],
      openShots: [c?.openShots || null],

      // Artificial lift
      liftMethod: [c?.liftMethod || 'NATURAL'],
      liftDepthM: [c?.liftDepthM || null],
      liftCapacityBopd: [c?.liftCapacityBopd || null],

      // Stimulation
      lastStimulationType: [c?.lastStimulationType || ''],
      lastStimulationDate: [c?.lastStimulationDate ? new Date(c.lastStimulationDate) : null],
      skinBeforeStim: [c?.skinBeforeStim || null],
      skinAfterStim: [c?.skinAfterStim || null],

      // Current production
      currentRateBopd: [c?.currentRateBopd || null],
      currentPiPbdPsi: [c?.currentPiPbdPsi || null]
    });

    // Auto-calculate interval
    this.form.get('topPerforationMdM').valueChanges.subscribe(() => this.calculateInterval());
    this.form.get('bottomPerforationMdM').valueChanges.subscribe(() => this.calculateInterval());
  }

  calculateInterval(): void {
    const top = this.form.get('topPerforationMdM').value;
    const bottom = this.form.get('bottomPerforationMdM').value;
    if (top !== null && bottom !== null && bottom > top) {
      this.form.patchValue({ perforationIntervalM: bottom - top }, { emitEvent: false });
    }
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const formValue = this.form.value;

      // Convert dates
      if (formValue.completionDate instanceof Date) {
        formValue.completionDate = formValue.completionDate.getTime();
      }
      if (formValue.lastStimulationDate instanceof Date) {
        formValue.lastStimulationDate = formValue.lastStimulationDate.getTime();
      }

      const completion: RvCompletion = { ...this.data.completion, ...formValue };

      const op = this.isEditMode
        ? this.rvService.updateCompletion(this.data.completion.assetId, completion)
        : this.rvService.createCompletion(this.data.tenantId, completion);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
