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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';

export interface PoMlConfigId {
  id: string;
  entityType?: string;
}

export interface PoMlConfig extends BaseData<PoMlConfigId> {
  tenantId: TenantId;

  // Failure prediction config
  failureAlertThreshold: number;
  predictionHorizonDays: number;
  analysisFrequencyHours: number;
  lookbackDays: number;

  // Anomaly config
  anomalyContamination: number;
  anomalyWindowHours: number;
  anomalyFeatures: string[];

  // Health score config
  healthWeightMechanical: number;
  healthWeightElectrical: number;
  healthWeightProduction: number;
  healthWeightThermal: number;
  healthThresholdHealthy: number;
  healthThresholdWarning: number;
  healthThresholdAtRisk: number;

  // Auto actions
  autoEmailEnabled: boolean;
  autoEmailThreshold: number;
  autoAlarmEnabled: boolean;
  autoWorkOrderEnabled: boolean;
  autoPushNotificationEnabled: boolean;
}

export enum MlModelType {
  FAILURE_PREDICTION = 'FAILURE_PREDICTION',
  ANOMALY_DETECTION = 'ANOMALY_DETECTION',
  HEALTH_SCORE = 'HEALTH_SCORE'
}

export enum MlModelStatus {
  TRAINING = 'TRAINING',
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
  FAILED = 'FAILED'
}

export enum TrainingJobStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface PoMlModelId {
  id: string;
  entityType?: string;
}

export interface PoMlModel extends BaseData<PoMlModelId> {
  tenantId: TenantId;
  name: string;
  modelType: MlModelType;
  liftSystemType?: string;
  version: string;
  status: MlModelStatus;

  // Metrics
  accuracy?: number;
  precision?: number;
  recall?: number;
  f1Score?: number;
  aucRoc?: number;

  // Training info
  trainingStartTime?: number;
  trainingEndTime?: number;
  trainingSamples?: number;
  failureEvents?: number;
  wellsCount?: number;

  // Configuration
  hyperparameters?: Record<string, any>;
  featureImportance?: Record<string, number>;

  // Storage
  modelPath?: string;
  mlflowRunId?: string;
}

export interface PoMlTrainingJobId {
  id: string;
  entityType?: string;
}

export interface PoMlTrainingJob extends BaseData<PoMlTrainingJobId> {
  tenantId: TenantId;
  modelName: string;
  status: TrainingJobStatus;

  // Config
  dataStartDate?: string;
  dataEndDate?: string;
  hyperparameters?: Record<string, any>;

  // Progress
  progressPercent: number;
  currentEpoch?: number;
  totalEpochs?: number;

  // Result
  resultModelId?: string;
  errorMessage?: string;

  startedTime?: number;
  completedTime?: number;
  createdBy?: string;
}

export interface StartTrainingRequest {
  modelName: string;
  dataStartDate: string;
  dataEndDate: string;
  hyperparameters?: Record<string, any>;
}

// Labels and colors
export const MlModelTypeLabels: Record<MlModelType, string> = {
  [MlModelType.FAILURE_PREDICTION]: 'Failure Prediction',
  [MlModelType.ANOMALY_DETECTION]: 'Anomaly Detection',
  [MlModelType.HEALTH_SCORE]: 'Health Score'
};

export const MlModelStatusColors: Record<MlModelStatus, string> = {
  [MlModelStatus.TRAINING]: '#ff9800',
  [MlModelStatus.ACTIVE]: '#4caf50',
  [MlModelStatus.ARCHIVED]: '#9e9e9e',
  [MlModelStatus.FAILED]: '#f44336'
};

export const MlModelStatusLabels: Record<MlModelStatus, string> = {
  [MlModelStatus.TRAINING]: 'Training',
  [MlModelStatus.ACTIVE]: 'Active',
  [MlModelStatus.ARCHIVED]: 'Archived',
  [MlModelStatus.FAILED]: 'Failed'
};

export const TrainingJobStatusColors: Record<TrainingJobStatus, string> = {
  [TrainingJobStatus.PENDING]: '#9e9e9e',
  [TrainingJobStatus.RUNNING]: '#2196f3',
  [TrainingJobStatus.COMPLETED]: '#4caf50',
  [TrainingJobStatus.FAILED]: '#f44336',
  [TrainingJobStatus.CANCELLED]: '#ff9800'
};

// Default configuration
export const DEFAULT_ML_CONFIG: Partial<PoMlConfig> = {
  failureAlertThreshold: 60,
  predictionHorizonDays: 14,
  analysisFrequencyHours: 1,
  lookbackDays: 7,
  anomalyContamination: 0.05,
  anomalyWindowHours: 24,
  anomalyFeatures: ['motor_temp_f', 'vibration_g', 'current_amps', 'intake_pressure_psi'],
  healthWeightMechanical: 0.40,
  healthWeightElectrical: 0.35,
  healthWeightProduction: 0.15,
  healthWeightThermal: 0.10,
  healthThresholdHealthy: 80,
  healthThresholdWarning: 60,
  healthThresholdAtRisk: 40,
  autoEmailEnabled: true,
  autoEmailThreshold: 70,
  autoAlarmEnabled: true,
  autoWorkOrderEnabled: false,
  autoPushNotificationEnabled: false
};

// Available features for anomaly detection
export const AVAILABLE_ANOMALY_FEATURES = [
  { key: 'motor_temp_f', label: 'Motor Temperature' },
  { key: 'vibration_g', label: 'Vibration' },
  { key: 'current_amps', label: 'Current Draw' },
  { key: 'intake_pressure_psi', label: 'Intake Pressure' },
  { key: 'discharge_pressure_psi', label: 'Discharge Pressure' },
  { key: 'frequency_hz', label: 'Frequency' },
  { key: 'production_bpd', label: 'Production' },
  { key: 'water_cut_percent', label: 'Water Cut' }
];
