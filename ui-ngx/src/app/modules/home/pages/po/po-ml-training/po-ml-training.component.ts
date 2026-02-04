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
  PoMlModel,
  PoMlTrainingJob,
  MlModelStatus,
  MlModelStatusColors,
  MlModelStatusLabels,
  MlModelTypeLabels,
  TrainingJobStatus,
  TrainingJobStatusColors,
  StartTrainingRequest
} from '@shared/models/po/po-ml-config.model';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-po-ml-training',
  templateUrl: './po-ml-training.component.html',
  styleUrls: ['./po-ml-training.component.scss']
})
export class PoMlTrainingComponent implements OnInit, OnDestroy {

  models: PoMlModel[] = [];
  selectedModel: PoMlModel;
  trainingJobs: PoMlTrainingJob[] = [];
  runningJobs: PoMlTrainingJob[] = [];

  trainingForm: FormGroup;

  isLoading = true;
  isStartingTraining = false;

  // Enums for template
  MlModelStatus = MlModelStatus;
  MlModelStatusColors = MlModelStatusColors;
  MlModelStatusLabels = MlModelStatusLabels;
  MlModelTypeLabels = MlModelTypeLabels;
  TrainingJobStatus = TrainingJobStatus;
  TrainingJobStatusColors = TrainingJobStatusColors;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private mlConfigService: PoMlConfigService,
    private snackBar: MatSnackBar
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadModels();
    this.loadTrainingJobs();
    this.loadRunningJobs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    const today = new Date();
    const oneYearAgo = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate());

    this.trainingForm = this.fb.group({
      modelName: ['', Validators.required],
      dataStartDate: [oneYearAgo.toISOString().split('T')[0], Validators.required],
      dataEndDate: [today.toISOString().split('T')[0], Validators.required],
      // Hyperparameters
      lstmUnits: [64, [Validators.required, Validators.min(16), Validators.max(256)]],
      dropoutRate: [0.2, [Validators.required, Validators.min(0), Validators.max(0.5)]],
      learningRate: [0.001, [Validators.required, Validators.min(0.0001), Validators.max(0.1)]],
      epochs: [50, [Validators.required, Validators.min(10), Validators.max(200)]],
      batchSize: [32, [Validators.required, Validators.min(8), Validators.max(128)]],
      earlyStopEnabled: [true]
    });
  }

  loadModels(): void {
    this.mlConfigService.getAllModels().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (models) => {
        this.models = models;
        this.isLoading = false;
        if (models.length > 0 && !this.selectedModel) {
          this.selectModel(models[0]);
        }
      },
      error: (error) => {
        console.error('Error loading models:', error);
        this.isLoading = false;
      }
    });
  }

  loadTrainingJobs(): void {
    const pageLink = new PageLink(20, 0, null, { property: 'createdTime', direction: Direction.DESC });
    this.mlConfigService.getTrainingJobs(pageLink).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        this.trainingJobs = result.data;
      },
      error: (error) => {
        console.error('Error loading training jobs:', error);
      }
    });
  }

  loadRunningJobs(): void {
    this.mlConfigService.getRunningJobs().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (jobs) => {
        this.runningJobs = jobs;
      },
      error: (error) => {
        console.error('Error loading running jobs:', error);
      }
    });
  }

  selectModel(model: PoMlModel): void {
    this.selectedModel = model;
    this.trainingForm.patchValue({ modelName: model.name });
  }

  startTraining(): void {
    if (this.trainingForm.invalid) {
      this.trainingForm.markAllAsTouched();
      return;
    }

    this.isStartingTraining = true;

    const formValue = this.trainingForm.value;
    const request: StartTrainingRequest = {
      modelName: formValue.modelName,
      dataStartDate: formValue.dataStartDate,
      dataEndDate: formValue.dataEndDate,
      hyperparameters: {
        lstmUnits: formValue.lstmUnits,
        dropoutRate: formValue.dropoutRate,
        learningRate: formValue.learningRate,
        epochs: formValue.epochs,
        batchSize: formValue.batchSize,
        earlyStopEnabled: formValue.earlyStopEnabled
      }
    };

    this.mlConfigService.startTrainingJob(request).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (job) => {
        this.isStartingTraining = false;
        this.snackBar.open('Training job started', 'Close', { duration: 3000 });
        this.loadTrainingJobs();
        this.loadRunningJobs();
      },
      error: (error) => {
        console.error('Error starting training:', error);
        this.isStartingTraining = false;
        this.snackBar.open('Error starting training job', 'Close', { duration: 3000 });
      }
    });
  }

  cancelJob(job: PoMlTrainingJob): void {
    this.mlConfigService.cancelTrainingJob(job.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.snackBar.open('Training job cancelled', 'Close', { duration: 3000 });
        this.loadTrainingJobs();
        this.loadRunningJobs();
      },
      error: (error) => {
        console.error('Error cancelling job:', error);
        this.snackBar.open('Error cancelling job', 'Close', { duration: 3000 });
      }
    });
  }

  deployModel(model: PoMlModel): void {
    this.mlConfigService.deployModel(model.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.snackBar.open('Model deployed successfully', 'Close', { duration: 3000 });
        this.loadModels();
      },
      error: (error) => {
        console.error('Error deploying model:', error);
        this.snackBar.open('Error deploying model', 'Close', { duration: 3000 });
      }
    });
  }

  rollbackModel(model: PoMlModel): void {
    this.mlConfigService.deployModel(model.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.snackBar.open('Rolled back to model ' + model.version, 'Close', { duration: 3000 });
        this.loadModels();
      },
      error: (error) => {
        console.error('Error rolling back:', error);
        this.snackBar.open('Error rolling back model', 'Close', { duration: 3000 });
      }
    });
  }

  getStatusColor(status: MlModelStatus | TrainingJobStatus): string {
    return MlModelStatusColors[status as MlModelStatus] ||
           TrainingJobStatusColors[status as TrainingJobStatus] ||
           '#9e9e9e';
  }

  formatAccuracy(accuracy: number): string {
    return accuracy ? `${(accuracy * 100).toFixed(1)}%` : '-';
  }

  formatDate(timestamp: number | string): string {
    if (!timestamp) return '-';
    const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp);
    return date.toLocaleDateString();
  }

  formatDateTime(timestamp: number): string {
    return timestamp ? new Date(timestamp).toLocaleString() : '-';
  }

  getFeatureImportanceEntries(): { key: string; value: number }[] {
    if (!this.selectedModel?.featureImportance) return [];
    return Object.entries(this.selectedModel.featureImportance)
      .map(([key, value]) => ({ key, value: value as number }))
      .sort((a, b) => b.value - a.value);
  }
}
