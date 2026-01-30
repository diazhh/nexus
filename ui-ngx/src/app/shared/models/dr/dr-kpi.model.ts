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

export interface RunKpi {
  runId: string;
  runNumber: string;
  totalFootageFt: number;
  avgRopFtHr: number;
  maxRopFtHr: number;
  minRopFtHr: number;
  totalRotatingHours: number;
  totalCirculatingHours: number;
  totalConnectionTimeHours: number;
  totalTrippingHours: number;
  totalNptHours: number;
  drillingEfficiencyPercent: number;
  avgMsePsi: number;
  avgWobKlbs: number;
  avgTorqueFtLbs: number;
  avgRpm: number;
}

export interface RigKpi {
  rigId: string;
  rigCode: string;
  rigName: string;
  totalWellsDrilled: number;
  totalFootageDrilledFt: number;
  totalOperationalHours: number;
  totalNptHours: number;
  nptPercentage: number;
  avgRopFtHr: number;
  avgDrillingEfficiencyPercent: number;
  avgConnectionTimeMinutes: number;
  totalRunsCompleted: number;
  avgRunDurationHours: number;
  uptimePercent: number;
}

export interface WellKpi {
  wellId: string;
  wellName: string;
  totalDepthMdFt: number;
  totalDepthTvdFt: number;
  totalDrillingDays: number;
  totalNptHours: number;
  nptPercentage: number;
  avgRopFtHr: number;
  totalRuns: number;
  avgDrillingEfficiencyPercent: number;
  maxInclinationDeg: number;
  totalDisplacementFt: number;
  costPerFootUsd?: number;
}

export interface ConnectionTimeKpi {
  runId: string;
  avgConnectionTimeMinutes: number;
  minConnectionTimeMinutes: number;
  maxConnectionTimeMinutes: number;
  totalConnections: number;
  connectionsBelowTarget: number;
  connectionsAboveTarget: number;
  targetConnectionTimeMinutes: number;
  connectionTimeDistribution: ConnectionTimeDistribution[];
}

export interface ConnectionTimeDistribution {
  rangeLabel: string;
  minMinutes: number;
  maxMinutes: number;
  count: number;
  percentage: number;
}

export interface DrillingEfficiencyKpi {
  runId: string;
  overallEfficiencyPercent: number;
  rotatingTimePercent: number;
  circulatingTimePercent: number;
  connectionTimePercent: number;
  trippingTimePercent: number;
  nptPercent: number;
  otherTimePercent: number;
  efficiencyTrend: EfficiencyTrendPoint[];
}

export interface EfficiencyTrendPoint {
  timestamp: number;
  efficiencyPercent: number;
  ropFtHr: number;
  depthFt: number;
}

export interface KpiTimeRange {
  startTime: number;
  endTime: number;
}
