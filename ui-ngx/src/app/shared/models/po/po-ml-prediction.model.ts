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

export interface PoMlPredictionId {
  id: string;
  entityType?: string;
}

export enum PredictionType {
  FAILURE = 'FAILURE',
  ANOMALY = 'ANOMALY',
  HEALTH_SCORE = 'HEALTH_SCORE'
}

export enum HealthLevel {
  HEALTHY = 'HEALTHY',
  WARNING = 'WARNING',
  AT_RISK = 'AT_RISK',
  CRITICAL = 'CRITICAL'
}

export enum HealthTrend {
  IMPROVING = 'IMPROVING',
  STABLE = 'STABLE',
  DEGRADING = 'DEGRADING'
}

export enum FactorImpact {
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW'
}

export enum FactorTrend {
  INCREASING = 'INCREASING',
  STABLE = 'STABLE',
  DECREASING = 'DECREASING'
}

export interface ContributingFactor {
  feature: string;
  featureLabel?: string;
  currentValue: number;
  threshold: number;
  unit?: string;
  impact: FactorImpact;
  trend: FactorTrend;
}

export interface AnomalousFeature {
  feature: string;
  featureLabel?: string;
  value: number;
  expectedMin: number;
  expectedMax: number;
  unit?: string;
}

export interface ComponentScores {
  mechanical: number;
  electrical: number;
  production: number;
  thermal?: number;
  hydraulic?: number;
}

export interface PoMlPrediction extends BaseData<PoMlPredictionId> {
  tenantId: TenantId;
  wellAssetId: string;
  wellName?: string;
  modelId?: string;
  modelName?: string;
  predictionType: PredictionType;

  // Failure prediction
  probability?: number;
  daysToFailure?: number;
  confidence?: number;
  predictedFailureType?: string;

  // Anomaly detection
  isAnomaly?: boolean;
  anomalyScore?: number;

  // Health score
  healthScore?: number;
  healthLevel?: HealthLevel;
  healthTrend?: HealthTrend;

  // Details
  contributingFactors?: ContributingFactor[];
  anomalousFeatures?: AnomalousFeature[];
  componentScores?: ComponentScores;

  // Actions taken
  alarmCreated?: boolean;
  alarmId?: string;
  notificationSent?: boolean;
  workOrderCreated?: boolean;
  workOrderId?: string;
}

export interface WellPredictionSummary {
  wellAssetId: string;
  wellName: string;
  liftSystemType: string;
  status: string;

  // Latest predictions
  failureProbability?: number;
  daysToFailure?: number;
  healthScore?: number;
  healthLevel?: HealthLevel;
  healthTrend?: HealthTrend;
  isAnomaly?: boolean;
  anomalyScore?: number;

  // Primary issue
  primaryIssue?: string;
  primaryFactor?: ContributingFactor;

  lastUpdated: number;
}

export interface PredictionHistoryItem {
  timestamp: number;
  probability?: number;
  healthScore?: number;
  anomalyScore?: number;
}

export interface WellPredictionDetail {
  wellAssetId: string;
  wellName: string;
  liftSystemType: string;

  // Current predictions
  failurePrediction?: PoMlPrediction;
  anomalyPrediction?: PoMlPrediction;
  healthScorePrediction?: PoMlPrediction;

  // History
  probabilityHistory?: PredictionHistoryItem[];
  healthScoreHistory?: PredictionHistoryItem[];

  // Recommended actions
  recommendedActions?: RecommendedAction[];
}

export interface RecommendedAction {
  priority: number;
  action: string;
  impact?: string;
  estimatedCost?: number;
}

export interface PredictionQuery {
  wellAssetId?: string;
  predictionType?: PredictionType;
  healthLevel?: HealthLevel[];
  minProbability?: number;
  startTime?: number;
  endTime?: number;
  pageSize?: number;
  page?: number;
  sortProperty?: string;
  sortOrder?: 'ASC' | 'DESC';
}

export interface PredictionSummary {
  totalWells: number;
  healthyCount: number;
  warningCount: number;
  atRiskCount: number;
  criticalCount: number;
  anomalyCount: number;
  highRiskCount: number;  // Probability > 60%
  avgHealthScore: number;
  avgFailureProbability: number;
}

// Labels and colors
export const HealthLevelColors: Record<HealthLevel, string> = {
  [HealthLevel.HEALTHY]: '#4caf50',
  [HealthLevel.WARNING]: '#fbc02d',
  [HealthLevel.AT_RISK]: '#f57c00',
  [HealthLevel.CRITICAL]: '#d32f2f'
};

export const HealthLevelLabels: Record<HealthLevel, string> = {
  [HealthLevel.HEALTHY]: 'Healthy',
  [HealthLevel.WARNING]: 'Warning',
  [HealthLevel.AT_RISK]: 'At Risk',
  [HealthLevel.CRITICAL]: 'Critical'
};

export const HealthTrendIcons: Record<HealthTrend, string> = {
  [HealthTrend.IMPROVING]: 'trending_up',
  [HealthTrend.STABLE]: 'trending_flat',
  [HealthTrend.DEGRADING]: 'trending_down'
};

export const HealthTrendColors: Record<HealthTrend, string> = {
  [HealthTrend.IMPROVING]: '#4caf50',
  [HealthTrend.STABLE]: '#9e9e9e',
  [HealthTrend.DEGRADING]: '#f44336'
};

export const FactorImpactColors: Record<FactorImpact, string> = {
  [FactorImpact.HIGH]: '#d32f2f',
  [FactorImpact.MEDIUM]: '#f57c00',
  [FactorImpact.LOW]: '#1976d2'
};

export const FactorTrendIcons: Record<FactorTrend, string> = {
  [FactorTrend.INCREASING]: 'arrow_upward',
  [FactorTrend.STABLE]: 'remove',
  [FactorTrend.DECREASING]: 'arrow_downward'
};

export const PredictionTypeLabels: Record<PredictionType, string> = {
  [PredictionType.FAILURE]: 'Failure Prediction',
  [PredictionType.ANOMALY]: 'Anomaly Detection',
  [PredictionType.HEALTH_SCORE]: 'Health Score'
};

// Feature labels mapping
export const FeatureLabels: Record<string, string> = {
  'motor_temp_f': 'Motor Temperature',
  'intake_temp_f': 'Intake Temperature',
  'vibration_g': 'Vibration',
  'current_amps': 'Current Draw',
  'intake_pressure_psi': 'Intake Pressure',
  'discharge_pressure_psi': 'Discharge Pressure',
  'frequency_hz': 'Frequency',
  'production_bpd': 'Production',
  'water_cut_percent': 'Water Cut',
  'gor_scf_stb': 'GOR',
  'pip_psi': 'PIP',
  'torque_ftlb': 'Torque',
  'speed_rpm': 'Speed'
};

export const FeatureUnits: Record<string, string> = {
  'motor_temp_f': '°F',
  'intake_temp_f': '°F',
  'vibration_g': 'g',
  'current_amps': 'A',
  'intake_pressure_psi': 'PSI',
  'discharge_pressure_psi': 'PSI',
  'frequency_hz': 'Hz',
  'production_bpd': 'BPD',
  'water_cut_percent': '%',
  'gor_scf_stb': 'SCF/STB',
  'pip_psi': 'PSI',
  'torque_ftlb': 'ft-lb',
  'speed_rpm': 'RPM'
};

// Helper functions
export function getHealthLevelFromScore(score: number, config?: {
  healthy: number;
  warning: number;
  atRisk: number;
}): HealthLevel {
  const thresholds = config || { healthy: 80, warning: 60, atRisk: 40 };

  if (score >= thresholds.healthy) return HealthLevel.HEALTHY;
  if (score >= thresholds.warning) return HealthLevel.WARNING;
  if (score >= thresholds.atRisk) return HealthLevel.AT_RISK;
  return HealthLevel.CRITICAL;
}

export function getRiskLevel(probability: number): 'low' | 'medium' | 'high' | 'critical' {
  if (probability >= 80) return 'critical';
  if (probability >= 60) return 'high';
  if (probability >= 40) return 'medium';
  return 'low';
}

export function getRiskColor(probability: number): string {
  if (probability >= 80) return '#d32f2f';
  if (probability >= 60) return '#f57c00';
  if (probability >= 40) return '#fbc02d';
  return '#4caf50';
}
