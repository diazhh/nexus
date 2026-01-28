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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TemplateDefinition, CTTemplateService } from '@core/http/ct-template.service';

export interface TemplateSelectorData {
  tenantId: string;
  type: 'unit' | 'reel';
}

@Component({
  selector: 'tb-ct-template-selector-dialog',
  templateUrl: './ct-template-selector-dialog.component.html',
  styleUrls: ['./ct-template-selector-dialog.component.scss']
})
export class CTTemplateSelectorDialogComponent implements OnInit {

  templates: TemplateDefinition[] = [];
  selectedTemplate: TemplateDefinition | null = null;
  loading = true;

  constructor(
    public dialogRef: MatDialogRef<CTTemplateSelectorDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TemplateSelectorData,
    private templateService: CTTemplateService
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    const service$ = this.data.type === 'unit'
      ? this.templateService.getUnitTemplates(this.data.tenantId)
      : this.templateService.getReelTemplates(this.data.tenantId);

    service$.subscribe({
      next: (templates) => {
        this.templates = templates;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading templates:', error);
        this.loading = false;
      }
    });
  }

  selectTemplate(template: TemplateDefinition): void {
    this.selectedTemplate = template;
  }

  confirm(): void {
    if (this.selectedTemplate) {
      this.dialogRef.close(this.selectedTemplate);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
