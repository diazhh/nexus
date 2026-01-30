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

// MSE Calculation
export interface MseCalculationRequest {
  torqueFtLbs: number;
  rpm: number;
  wobLbs: number;
  bitDiameterIn: number;
  ropFtHr: number;
}

export interface MseCalculationResult {
  msePsi: number;
  rotationalEnergyPsi: number;
  thrustEnergyPsi: number;
  efficiency?: number;
  isFoundering: boolean;
  founderingThreshold?: number;
}

// ECD Calculation
export interface EcdCalculationRequest {
  mudWeightPpg: number;
  tvdFt: number;
  annularPressureLossPsi: number;
}

export interface EcdCalculationResult {
  ecdPpg: number;
  hydrostaticPressurePsi: number;
  totalBottomholePressurePsi: number;
  isAboveFracGradient: boolean;
  isBelowPorePressure: boolean;
}

// Swab/Surge Calculation
export interface SwabSurgeCalculationRequest {
  pipeOdIn: number;
  holeIdIn: number;
  pipeLengthFt: number;
  tripSpeedFtMin: number;
  mudWeightPpg: number;
  plasticViscosityCp: number;
  yieldPointLbf100sqft: number;
}

export interface SwabSurgeCalculationResult {
  swabPressurePsi: number;
  surgePressurePsi: number;
  swabEcdPpg: number;
  surgeEcdPpg: number;
  maxSafeTripSpeedFtMin: number;
}

// Kick Tolerance Calculation
export interface KickToleranceCalculationRequest {
  mudWeightPpg: number;
  tvdFt: number;
  porePressurePpg: number;
  fracGradientPpg: number;
  casingShoeDepthFt: number;
  annularCapacityBblFt: number;
}

export interface KickToleranceCalculationResult {
  kickToleranceBbl: number;
  maxInfluxHeightFt: number;
  safetyMarginPpg: number;
  maasp: number;
}

// Torque & Drag Calculation
export interface TorqueDragCalculationRequest {
  surveyPoints: TDSurveyPoint[];
  drillstringWeight: number;
  frictionFactor: number;
  mudWeightPpg: number;
  wob: number;
}

export interface TDSurveyPoint {
  mdFt: number;
  inclinationDeg: number;
  azimuthDeg: number;
  weightPerFtLbs: number;
}

export interface TorqueDragCalculationResult {
  pickupWeightLbs: number;
  slackOffWeightLbs: number;
  rotatingWeightLbs: number;
  surfaceTorqueFtLbs: number;
  torqueAtBitFtLbs: number;
  maxDragForceLbs: number;
  criticalBucklingLoadLbs: number;
}

// DLS Calculation
export interface DlsCalculationRequest {
  md1Ft: number;
  inc1Deg: number;
  azm1Deg: number;
  md2Ft: number;
  inc2Deg: number;
  azm2Deg: number;
}

export interface DlsCalculationResult {
  dlsDegPer100ft: number;
  doglegAngleDeg: number;
  courseLengthFt: number;
  buildRateDegPer100ft: number;
  turnRateDegPer100ft: number;
}

// Bit Hydraulics Calculation
export interface BitHydraulicsCalculationRequest {
  flowRateGpm: number;
  mudWeightPpg: number;
  nozzleSizes: number[];
}

export interface BitHydraulicsCalculationResult {
  totalFlowAreaSqIn: number;
  nozzleVelocityFtSec: number;
  pressureDropPsi: number;
  hydraulicHorsepowerHp: number;
  impactForceLbs: number;
  jetImpactForcePerSqInPsi: number;
}
