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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface JobParameters {
  jobId?: string;
  wellName: string;
  targetDepthFt: number;
  wellboreDiameterInch: number;
  maxInclinationDeg: number;
  tubingOdInch: number;
  tubingIdInch: number;
  tubingLengthFt: number;
  fluidDensityPpg: number;
  pumpRateBpm?: number;
  maxPressurePsi: number;
  maxRunningSpeedFtMin: number;
  unitMaxPressurePsi: number;
  unitMaxTensionLbf: number;
  estimatedTreatmentHours?: number;
}

export interface FeasibilityCheck {
  isFeasible: boolean;
  limitingFactors: string[];
  warnings: string[];
}

export interface ForceAnalysis {
  depths: number[];
  hookloads: number[];
  maxHookload: number;
  bucklingMargins: number[];
}

export interface HydraulicAnalysis {
  depths: number[];
  pressures: number[];
  maxPressure: number;
  velocities: number[];
}

export interface TimeEstimation {
  riggingUpHours: number;
  runningInHours: number;
  onDepthHours: number;
  pullingOutHours: number;
  riggingDownHours: number;
  totalDurationHours: number;
}

export interface FatiguePrediction {
  estimatedCycles: number;
  estimatedFatiguePercent: number;
  remainingLifePercent: number;
}

export interface Risk {
  category: string;
  severity: string;
  description: string;
  mitigation: string;
}

export interface SimulationResult {
  jobId?: string;
  wellName: string;
  feasibility: FeasibilityCheck;
  forces?: ForceAnalysis;
  hydraulics?: HydraulicAnalysis;
  times?: TimeEstimation;
  fatigue?: FatiguePrediction;
  risks?: Risk[];
  error?: string;
}
