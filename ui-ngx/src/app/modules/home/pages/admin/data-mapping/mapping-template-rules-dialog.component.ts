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

import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DataMappingService } from '@core/http/data-mapping.service';
import {
  MappingTemplate,
  MappingTemplateRule,
  TransformationType,
  CommonTelemetryKeys
} from '@shared/models/data-mapping.models';
import { MatTableDataSource } from '@angular/material/table';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogService } from '@core/services/dialog.service';

export interface MappingTemplateRulesDialogData {
  template: MappingTemplate;
}

@Component({
  selector: 'tb-mapping-template-rules-dialog',
  templateUrl: './mapping-template-rules-dialog.component.html',
  styleUrls: ['./mapping-template-rules-dialog.component.scss']
})
export class MappingTemplateRulesDialogComponent implements OnInit, OnDestroy {

  template: MappingTemplate;
  rules: MappingTemplateRule[] = [];
  dataSource = new MatTableDataSource<MappingTemplateRule>();
  displayedColumns: string[] = ['sourceKey', 'targetKey', 'transformationType', 'units', 'priority', 'active', 'actions'];

  isLoading = true;
  showRuleForm = false;
  editingRule: MappingTemplateRule | null = null;
  ruleForm: FormGroup;
  isSaving = false;
  hasChanges = false;

  transformationTypes = Object.values(TransformationType);

  private readonly destroy$ = new Subject<void>();

  constructor(
    private dialogRef: MatDialogRef<MappingTemplateRulesDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: MappingTemplateRulesDialogData,
    private fb: FormBuilder,
    private store: Store<AppState>,
    private dataMappingService: DataMappingService,
    private translate: TranslateService,
    private dialogService: DialogService
  ) {
    this.template = data.template;
  }

  ngOnInit(): void {
    this.initForm();
    this.loadRules();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.ruleForm = this.fb.group({
      sourceKey: ['', [Validators.required, Validators.maxLength(255)]],
      targetKey: ['', [Validators.required, Validators.maxLength(255)]],
      transformationType: [TransformationType.DIRECT, Validators.required],
      transformationConfig: [null],
      unitSource: [''],
      unitTarget: [''],
      description: [''],
      priority: [0, [Validators.min(0)]],
      active: [true]
    });
  }

  loadRules(): void {
    this.isLoading = true;
    this.dataMappingService.getMappingTemplateRules(this.template.id.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rules) => {
          this.rules = rules.sort((a, b) => a.priority - b.priority);
          this.dataSource.data = this.rules;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  getAvailableSourceKeys(): string[] {
    // Return common keys for the module or empty array
    return CommonTelemetryKeys[this.template.moduleKey] || [];
  }

  getAvailableTargetKeys(): string[] {
    return CommonTelemetryKeys[this.template.moduleKey] || [];
  }

  showAddRuleForm(): void {
    this.editingRule = null;
    this.ruleForm.reset({
      sourceKey: '',
      targetKey: '',
      transformationType: TransformationType.DIRECT,
      transformationConfig: null,
      unitSource: '',
      unitTarget: '',
      description: '',
      priority: this.rules.length,
      active: true
    });
    this.showRuleForm = true;
  }

  editRule(rule: MappingTemplateRule): void {
    this.editingRule = rule;
    this.ruleForm.patchValue({
      sourceKey: rule.sourceKey,
      targetKey: rule.targetKey,
      transformationType: rule.transformationType,
      transformationConfig: rule.transformationConfig,
      unitSource: rule.unitSource || '',
      unitTarget: rule.unitTarget || '',
      description: rule.description || '',
      priority: rule.priority,
      active: rule.active
    });
    this.showRuleForm = true;
  }

  cancelRuleForm(): void {
    this.showRuleForm = false;
    this.editingRule = null;
    this.ruleForm.reset();
  }

  saveRule(): void {
    if (this.ruleForm.invalid) {
      Object.keys(this.ruleForm.controls).forEach(key => {
        this.ruleForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSaving = true;
    const formValue = this.ruleForm.value;

    const rule: MappingTemplateRule = {
      ...this.editingRule,
      templateId: { id: this.template.id.id, entityType: 'MAPPING_TEMPLATE' } as any,
      sourceKey: formValue.sourceKey,
      targetKey: formValue.targetKey,
      transformationType: formValue.transformationType,
      transformationConfig: formValue.transformationConfig,
      unitSource: formValue.unitSource || null,
      unitTarget: formValue.unitTarget || null,
      description: formValue.description || null,
      priority: formValue.priority,
      active: formValue.active
    } as MappingTemplateRule;

    this.dataMappingService.saveMappingTemplateRule(rule)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isSaving = false;
          this.showRuleForm = false;
          this.editingRule = null;
          this.hasChanges = true;
          this.showNotification(
            this.editingRule ? 'data-mapping.rule-updated' : 'data-mapping.rule-created'
          );
          this.loadRules();
        },
        error: () => {
          this.isSaving = false;
        }
      });
  }

  deleteRule(rule: MappingTemplateRule): void {
    this.dialogService.confirm(
      this.translate.instant('data-mapping.delete-rule'),
      this.translate.instant('data-mapping.confirm-delete-rule'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.dataMappingService.deleteMappingTemplateRule(rule.id.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.hasChanges = true;
              this.showNotification('data-mapping.rule-deleted');
              this.loadRules();
            },
            error: () => {
              this.showNotification('data-mapping.error-deleting-rule', 'warn');
            }
          });
      }
    });
  }

  getTransformationTypeLabel(type: TransformationType): string {
    return this.translate.instant(`data-mapping.transformation-type.${type.toLowerCase()}`);
  }

  close(): void {
    this.dialogRef.close(this.hasChanges);
  }

  private showNotification(messageKey: string, type: 'success' | 'warn' | 'error' = 'success'): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageKey),
      type,
      duration: 2000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }
}
