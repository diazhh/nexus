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

export enum HealthLevel {
  EXCELLENT = 'EXCELLENT',
  GOOD = 'GOOD',
  FAIR = 'FAIR',
  POOR = 'POOR',
  CRITICAL = 'CRITICAL'
}

export enum HealthTrend {
  IMPROVING = 'IMPROVING',
  STABLE = 'STABLE',
  DECLINING = 'DECLINING'
}

export interface PoHealthScore {
  wellId: string;
  wellName?: string;
  assetId: string;
  score: number; // 0-100
  level: HealthLevel;
  trend: HealthTrend;
  failureProbability?: number; // 0-1
  estimatedDaysToFailure?: number;
  componentScores: ComponentScore[];
  calculatedAt: number;
  dataQuality?: number;
}

export interface ComponentScore {
  component: string;
  score: number;
  issues: string[];
  trend: HealthTrend;
  weight: number;
}

export interface HealthScoreHistory {
  wellId: string;
  data: HealthScorePoint[];
}

export interface HealthScorePoint {
  ts: number;
  score: number;
  level: HealthLevel;
  failureProbability?: number;
}

export interface HealthScoreQuery {
  wellIds?: string[];
  minScore?: number;
  maxScore?: number;
  levels?: HealthLevel[];
  trends?: HealthTrend[];
  minFailureProbability?: number;
  pageSize?: number;
  page?: number;
  sortProperty?: string;
  sortOrder?: 'ASC' | 'DESC';
}

export interface HealthScoreSummary {
  totalWells: number;
  averageScore: number;
  byLevel: Record<HealthLevel, number>;
  byTrend: Record<HealthTrend, number>;
  wellsAtRisk: number; // score < 50 or failureProbability > 0.5
}

export interface FailurePrediction {
  wellId: string;
  wellName?: string;
  probability: number;
  estimatedDays?: number;
  confidence: number;
  riskFactors: RiskFactor[];
  predictedAt: number;
}

export interface RiskFactor {
  factor: string;
  contribution: number; // 0-1
  description: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
}

// Health level thresholds and colors
export const HealthLevelThresholds: Record<HealthLevel, { min: number; max: number }> = {
  [HealthLevel.EXCELLENT]: { min: 90, max: 100 },
  [HealthLevel.GOOD]: { min: 75, max: 89 },
  [HealthLevel.FAIR]: { min: 50, max: 74 },
  [HealthLevel.POOR]: { min: 25, max: 49 },
  [HealthLevel.CRITICAL]: { min: 0, max: 24 }
};

export const HealthLevelColors: Record<HealthLevel, string> = {
  [HealthLevel.EXCELLENT]: '#4caf50',
  [HealthLevel.GOOD]: '#8bc34a',
  [HealthLevel.FAIR]: '#ff9800',
  [HealthLevel.POOR]: '#ff5722',
  [HealthLevel.CRITICAL]: '#f44336'
};

export const HealthLevelLabels: Record<HealthLevel, string> = {
  [HealthLevel.EXCELLENT]: 'Excellent',
  [HealthLevel.GOOD]: 'Good',
  [HealthLevel.FAIR]: 'Fair',
  [HealthLevel.POOR]: 'Poor',
  [HealthLevel.CRITICAL]: 'Critical'
};

export const HealthTrendColors: Record<HealthTrend, string> = {
  [HealthTrend.IMPROVING]: '#4caf50',
  [HealthTrend.STABLE]: '#2196f3',
  [HealthTrend.DECLINING]: '#f44336'
};

export const HealthTrendIcons: Record<HealthTrend, string> = {
  [HealthTrend.IMPROVING]: 'trending_up',
  [HealthTrend.STABLE]: 'trending_flat',
  [HealthTrend.DECLINING]: 'trending_down'
};

export function getHealthLevelFromScore(score: number): HealthLevel {
  if (score >= 90) return HealthLevel.EXCELLENT;
  if (score >= 75) return HealthLevel.GOOD;
  if (score >= 50) return HealthLevel.FAIR;
  if (score >= 25) return HealthLevel.POOR;
  return HealthLevel.CRITICAL;
}

export function isWellAtRisk(healthScore: PoHealthScore): boolean {
  return healthScore.score < 50 ||
         (healthScore.failureProbability !== undefined && healthScore.failureProbability > 0.5);
}
