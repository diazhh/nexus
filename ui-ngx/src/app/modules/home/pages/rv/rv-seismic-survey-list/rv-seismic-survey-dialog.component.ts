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

@Component({
  selector: 'tb-rv-seismic-survey-dialog',
  templateUrl: './rv-seismic-survey-dialog.component.html',
  styleUrls: ['./rv-seismic-survey-dialog.component.scss']
})
export class RvSeismicSurveyDialogComponent implements OnInit {

  form: FormGroup;
  isEditMode = false;
  isLoading = false;
  fields: any[] = [];

  surveyTypes = ['TWO_D', 'THREE_D', 'FOUR_D', 'VSP'];
  qualityRatings = ['EXCELLENT', 'GOOD', 'FAIR', 'POOR'];
  interpretationStatuses = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'];

  surveyTypeLabels: any = {
    'TWO_D': '2D',
    'THREE_D': '3D',
    'FOUR_D': '4D',
    'VSP': 'VSP'
  };

  constructor(
    private fb: FormBuilder,
    private rvService: RvService,
    private dialogRef: MatDialogRef<RvSeismicSurveyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { tenantId: string; survey?: any }
  ) {
    this.isEditMode = !!data.survey;
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
    const s = this.data.survey;
    this.form = this.fb.group({
      name: [s?.name || '', Validators.required],
      surveyCode: [s?.surveyCode || ''],
      fieldAssetId: [s?.fieldAssetId || '', Validators.required],
      surveyType: [s?.surveyType || 'THREE_D', Validators.required],
      acquisitionDate: [s?.acquisitionDate || null],
      coverageAreaKm2: [s?.coverageAreaKm2 || null],
      lineKm: [s?.lineKm || null],
      fold: [s?.fold || null],
      snRatio: [s?.snRatio || null],
      qualityRating: [s?.qualityRating || 'GOOD'],
      processingSequence: [s?.processingSequence || ''],
      isPsdm: [s?.isPsdm || false],
      interpretationStatus: [s?.interpretationStatus || 'NOT_STARTED'],
      interpreterName: [s?.interpreterName || ''],
      horizonsIdentified: [s?.horizonsIdentified || ''],
      faultsIdentified: [s?.faultsIdentified || ''],
      notes: [s?.notes || '']
    });
  }

  save(): void {
    if (this.form.valid) {
      this.isLoading = true;
      const survey: any = { ...this.data.survey, ...this.form.value };

      const op = this.isEditMode
        ? this.rvService.updateSeismicSurvey(this.data.survey.assetId, survey)
        : this.rvService.createSeismicSurvey(this.data.tenantId, survey);

      op.subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.isLoading = false
      });
    }
  }

  cancel(): void { this.dialogRef.close(); }
}
