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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PoMlConfigService } from '@core/http/po/po-ml-config.service';
import {
  PoMlConfig,
  PoMlModel,
  MlModelStatus,
  MlModelStatusColors,
  MlModelStatusLabels,
  MlModelTypeLabels,
  AVAILABLE_ANOMALY_FEATURES,
  DEFAULT_ML_CONFIG
} from '@shared/models/po/po-ml-config.model';

@Component({
  selector: 'tb-po-ml-config',
  templateUrl: './po-ml-config.component.html',
  styleUrls: ['./po-ml-config.component.scss']
})
export class PoMlConfigComponent implements OnInit, OnDestroy {

  configForm: FormGroup;
  mlConfig: PoMlConfig;
  activeModels: PoMlModel[] = [];

  isLoading = true;
  isSaving = false;

  // Enums for template
  MlModelStatus = MlModelStatus;
  MlModelStatusColors = MlModelStatusColors;
  MlModelStatusLabels = MlModelStatusLabels;
  MlModelTypeLabels = MlModelTypeLabels;

  // Available features for anomaly detection
  availableAnomalyFeatures = AVAILABLE_ANOMALY_FEATURES;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private mlConfigService: PoMlConfigService,
    private snackBar: MatSnackBar
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadConfig();
    this.loadActiveModels();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.configForm = this.fb.group({
      // Failure prediction
      failureAlertThreshold: [60, [Validators.required, Validators.min(0), Validators.max(100)]],
      predictionHorizonDays: [14, [Validators.required, Validators.min(1), Validators.max(90)]],
      analysisFrequencyHours: [1, [Validators.required, Validators.min(1), Validators.max(24)]],
      lookbackDays: [7, [Validators.required, Validators.min(1), Validators.max(30)]],

      // Anomaly detection
      anomalyContamination: [0.05, [Validators.required, Validators.min(0.01), Validators.max(0.5)]],
      anomalyWindowHours: [24, [Validators.required, Validators.min(1), Validators.max(168)]],
      anomalyFeatures: [[]],

      // Health score weights
      healthWeightMechanical: [0.40, [Validators.required, Validators.min(0), Validators.max(1)]],
      healthWeightElectrical: [0.35, [Validators.required, Validators.min(0), Validators.max(1)]],
      healthWeightProduction: [0.15, [Validators.required, Validators.min(0), Validators.max(1)]],
      healthWeightThermal: [0.10, [Validators.required, Validators.min(0), Validators.max(1)]],

      // Health thresholds
      healthThresholdHealthy: [80, [Validators.required, Validators.min(0), Validators.max(100)]],
      healthThresholdWarning: [60, [Validators.required, Validators.min(0), Validators.max(100)]],
      healthThresholdAtRisk: [40, [Validators.required, Validators.min(0), Validators.max(100)]],

      // Auto actions
      autoEmailEnabled: [true],
      autoEmailThreshold: [70, [Validators.min(0), Validators.max(100)]],
      autoAlarmEnabled: [true],
      autoWorkOrderEnabled: [false],
      autoPushNotificationEnabled: [false]
    });
  }

  private loadConfig(): void {
    this.isLoading = true;
    this.mlConfigService.getMlConfig().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (config) => {
        this.mlConfig = config;
        this.patchFormValues(config);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading ML config:', error);
        // Use defaults if config doesn't exist
        this.patchFormValues(DEFAULT_ML_CONFIG as PoMlConfig);
        this.isLoading = false;
      }
    });
  }

  private loadActiveModels(): void {
    this.mlConfigService.getActiveModels().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (models) => {
        this.activeModels = models;
      },
      error: (error) => {
        console.error('Error loading active models:', error);
      }
    });
  }

  private patchFormValues(config: Partial<PoMlConfig>): void {
    this.configForm.patchValue({
      failureAlertThreshold: config.failureAlertThreshold,
      predictionHorizonDays: config.predictionHorizonDays,
      analysisFrequencyHours: config.analysisFrequencyHours,
      lookbackDays: config.lookbackDays,
      anomalyContamination: config.anomalyContamination,
      anomalyWindowHours: config.anomalyWindowHours,
      anomalyFeatures: config.anomalyFeatures || [],
      healthWeightMechanical: config.healthWeightMechanical,
      healthWeightElectrical: config.healthWeightElectrical,
      healthWeightProduction: config.healthWeightProduction,
      healthWeightThermal: config.healthWeightThermal,
      healthThresholdHealthy: config.healthThresholdHealthy,
      healthThresholdWarning: config.healthThresholdWarning,
      healthThresholdAtRisk: config.healthThresholdAtRisk,
      autoEmailEnabled: config.autoEmailEnabled,
      autoEmailThreshold: config.autoEmailThreshold,
      autoAlarmEnabled: config.autoAlarmEnabled,
      autoWorkOrderEnabled: config.autoWorkOrderEnabled,
      autoPushNotificationEnabled: config.autoPushNotificationEnabled
    });
  }

  saveConfig(): void {
    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const configToSave: PoMlConfig = {
      ...this.mlConfig,
      ...this.configForm.value
    };

    this.mlConfigService.saveMlConfig(configToSave).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (savedConfig) => {
        this.mlConfig = savedConfig;
        this.isSaving = false;
        this.snackBar.open('Configuration saved successfully', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error saving ML config:', error);
        this.isSaving = false;
        this.snackBar.open('Error saving configuration', 'Close', { duration: 3000 });
      }
    });
  }

  resetToDefaults(): void {
    this.mlConfigService.resetMlConfigToDefaults().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (config) => {
        this.mlConfig = config;
        this.patchFormValues(config);
        this.snackBar.open('Configuration reset to defaults', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error resetting config:', error);
        this.snackBar.open('Error resetting configuration', 'Close', { duration: 3000 });
      }
    });
  }

  getTotalWeight(): number {
    const mechanical = this.configForm.get('healthWeightMechanical')?.value || 0;
    const electrical = this.configForm.get('healthWeightElectrical')?.value || 0;
    const production = this.configForm.get('healthWeightProduction')?.value || 0;
    const thermal = this.configForm.get('healthWeightThermal')?.value || 0;
    return Math.round((mechanical + electrical + production + thermal) * 100);
  }

  getModelStatusColor(status: MlModelStatus): string {
    return MlModelStatusColors[status] || '#9e9e9e';
  }

  formatAccuracy(accuracy: number): string {
    return accuracy ? `${(accuracy * 100).toFixed(1)}%` : '-';
  }

  formatDate(timestamp: number): string {
    return timestamp ? new Date(timestamp).toLocaleDateString() : '-';
  }

  deployModel(model: PoMlModel): void {
    this.mlConfigService.deployModel(model.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadActiveModels();
        this.snackBar.open(`Model ${model.name} deployed successfully`, 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error deploying model:', error);
        this.snackBar.open('Error deploying model', 'Close', { duration: 3000 });
      }
    });
  }

  archiveModel(model: PoMlModel): void {
    this.mlConfigService.archiveModel(model.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadActiveModels();
        this.snackBar.open(`Model ${model.name} archived`, 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error archiving model:', error);
        this.snackBar.open('Error archiving model', 'Close', { duration: 3000 });
      }
    });
  }

  toggleFeature(event: any, featureKey: string): void {
    const currentFeatures: string[] = this.configForm.get('anomalyFeatures')?.value || [];
    if (event.checked) {
      if (!currentFeatures.includes(featureKey)) {
        this.configForm.patchValue({
          anomalyFeatures: [...currentFeatures, featureKey]
        });
      }
    } else {
      this.configForm.patchValue({
        anomalyFeatures: currentFeatures.filter(f => f !== featureKey)
      });
    }
  }

  isFeatureSelected(featureKey: string): boolean {
    const currentFeatures: string[] = this.configForm.get('anomalyFeatures')?.value || [];
    return currentFeatures.includes(featureKey);
  }
}
