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

export interface PoRecommendationId {
  id: string;
  entityType?: string;
}

export enum RecommendationStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  EXECUTED = 'EXECUTED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED'
}

export enum RecommendationPriority {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW'
}

export enum OptimizationType {
  ESP_FREQUENCY = 'ESP_FREQUENCY',
  PCP_SPEED = 'PCP_SPEED',
  GAS_LIFT_ALLOCATION = 'GAS_LIFT_ALLOCATION',
  ROD_PUMP_SPM = 'ROD_PUMP_SPM',
  ROD_PUMP_STROKE = 'ROD_PUMP_STROKE',
  DILUENT_RATE = 'DILUENT_RATE',
  PREVENTIVE_MAINTENANCE = 'PREVENTIVE_MAINTENANCE'
}

export interface PoRecommendation extends BaseData<PoRecommendationId> {
  tenantId: TenantId;
  wellId: string;
  wellName?: string;
  type: OptimizationType;
  priority: RecommendationPriority;
  status: RecommendationStatus;
  currentValue?: number;
  recommendedValue?: number;
  unit?: string;
  expectedBenefitBpd?: number;
  expectedSavingsUsd?: number;
  confidence?: number;
  description?: string;
  risks?: string[];
  requiredApprovalLevel?: string;
  createdBy?: string;
  approvedBy?: string;
  executedBy?: string;
  approvedAt?: number;
  executedAt?: number;
  expiresAt?: number;
  approvalComments?: string;
  effectivenessScore?: number;
  simulationResult?: SimulationResult;
}

export interface SimulationResult {
  estimatedProductionBpd: number;
  estimatedPowerKw?: number;
  estimatedEfficiency?: number;
  confidenceInterval?: {
    lower: number;
    upper: number;
  };
  warnings?: string[];
}

export interface PoRecommendationQuery {
  wellId?: string;
  status?: RecommendationStatus[];
  priority?: RecommendationPriority[];
  types?: OptimizationType[];
  startTime?: number;
  endTime?: number;
  pageSize?: number;
  page?: number;
  sortProperty?: string;
  sortOrder?: 'ASC' | 'DESC';
}

export interface PoRecommendationStats {
  pending: number;
  approved: number;
  executed: number;
  rejected: number;
  totalBenefitBpd: number;
  totalSavingsUsd: number;
  avgEffectiveness: number;
}

export interface ApproveRecommendationRequest {
  recommendationId: string;
  comments?: string;
  scheduleAt?: number;
}

export interface RejectRecommendationRequest {
  recommendationId: string;
  reason: string;
}

// Labels and colors
export const RecommendationStatusColors: Record<RecommendationStatus, string> = {
  [RecommendationStatus.PENDING]: '#ff9800',
  [RecommendationStatus.APPROVED]: '#2196f3',
  [RecommendationStatus.REJECTED]: '#f44336',
  [RecommendationStatus.EXECUTED]: '#4caf50',
  [RecommendationStatus.EXPIRED]: '#9e9e9e',
  [RecommendationStatus.CANCELLED]: '#795548'
};

export const RecommendationStatusLabels: Record<RecommendationStatus, string> = {
  [RecommendationStatus.PENDING]: 'Pending',
  [RecommendationStatus.APPROVED]: 'Approved',
  [RecommendationStatus.REJECTED]: 'Rejected',
  [RecommendationStatus.EXECUTED]: 'Executed',
  [RecommendationStatus.EXPIRED]: 'Expired',
  [RecommendationStatus.CANCELLED]: 'Cancelled'
};

export const RecommendationPriorityColors: Record<RecommendationPriority, string> = {
  [RecommendationPriority.CRITICAL]: '#d32f2f',
  [RecommendationPriority.HIGH]: '#f57c00',
  [RecommendationPriority.MEDIUM]: '#fbc02d',
  [RecommendationPriority.LOW]: '#1976d2'
};

export const OptimizationTypeLabels: Record<OptimizationType, string> = {
  [OptimizationType.ESP_FREQUENCY]: 'ESP Frequency',
  [OptimizationType.PCP_SPEED]: 'PCP Speed',
  [OptimizationType.GAS_LIFT_ALLOCATION]: 'Gas Lift Allocation',
  [OptimizationType.ROD_PUMP_SPM]: 'Rod Pump SPM',
  [OptimizationType.ROD_PUMP_STROKE]: 'Rod Pump Stroke',
  [OptimizationType.DILUENT_RATE]: 'Diluent Rate',
  [OptimizationType.PREVENTIVE_MAINTENANCE]: 'Preventive Maintenance'
};

export const OptimizationTypeUnits: Record<OptimizationType, string> = {
  [OptimizationType.ESP_FREQUENCY]: 'Hz',
  [OptimizationType.PCP_SPEED]: 'RPM',
  [OptimizationType.GAS_LIFT_ALLOCATION]: 'Mscfd',
  [OptimizationType.ROD_PUMP_SPM]: 'SPM',
  [OptimizationType.ROD_PUMP_STROKE]: 'in',
  [OptimizationType.DILUENT_RATE]: 'BPD',
  [OptimizationType.PREVENTIVE_MAINTENANCE]: ''
};
