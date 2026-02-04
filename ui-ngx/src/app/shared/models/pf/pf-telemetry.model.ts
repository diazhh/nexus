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

export interface PfTelemetryData {
  entityId: string;
  timestamp: number;
  values: Record<string, number | string | boolean>;
}

export interface PfLatestTelemetry {
  entityId: string;
  timestamp: number;
  // ESP telemetry
  frequencyHz?: number;
  currentAmps?: number;
  motorTempF?: number;
  intakeTempF?: number;
  pipPsi?: number;
  dischargePressurePsi?: number;
  vibrationG?: number;
  // Production telemetry
  productionBpd?: number;
  waterCutPercent?: number;
  gorScfStb?: number;
  // PCP telemetry
  rpm?: number;
  torqueNm?: number;
  // Gas Lift telemetry
  gasInjectionRateMscfd?: number;
  injectionPressurePsi?: number;
  // Rod Pump telemetry
  spm?: number;
  strokeLengthIn?: number;
  polishedRodLoadLbs?: number;
  // Quality
  qualityScore?: number;
}

export interface PfHistoricalTelemetry {
  entityId: string;
  data: PfTelemetryPoint[];
}

export interface PfTelemetryPoint {
  ts: number;
  [key: string]: number | string | boolean;
}

export interface PfTelemetryQuery {
  entityId: string;
  keys: string[];
  startTs: number;
  endTs: number;
  interval?: number;
  aggregation?: TelemetryAggregation;
  limit?: number;
}

export enum TelemetryAggregation {
  NONE = 'NONE',
  MIN = 'MIN',
  MAX = 'MAX',
  AVG = 'AVG',
  SUM = 'SUM',
  COUNT = 'COUNT'
}

// Standard telemetry keys by lift system type
export const EspTelemetryKeys = [
  'frequency_hz',
  'current_amps',
  'motor_temp_f',
  'intake_temp_f',
  'pip_psi',
  'discharge_pressure_psi',
  'vibration_g',
  'production_bpd',
  'water_cut_percent',
  'gor_scf_stb'
];

export const PcpTelemetryKeys = [
  'rpm',
  'torque_nm',
  'motor_temp_f',
  'fluid_level_ft',
  'production_bpd',
  'water_cut_percent'
];

export const GasLiftTelemetryKeys = [
  'gas_injection_rate_mscfd',
  'injection_pressure_psi',
  'casing_pressure_psi',
  'tubing_pressure_psi',
  'production_bpd',
  'water_cut_percent',
  'gor_scf_stb'
];

export const RodPumpTelemetryKeys = [
  'spm',
  'stroke_length_in',
  'polished_rod_load_lbs',
  'motor_current_amps',
  'production_bpd',
  'water_cut_percent',
  'fluid_level_ft'
];

export const TelemetryKeyLabels: Record<string, string> = {
  'frequency_hz': 'Frequency (Hz)',
  'current_amps': 'Current (A)',
  'motor_temp_f': 'Motor Temp (°F)',
  'intake_temp_f': 'Intake Temp (°F)',
  'pip_psi': 'PIP (psi)',
  'discharge_pressure_psi': 'Discharge Pressure (psi)',
  'vibration_g': 'Vibration (g)',
  'production_bpd': 'Production (BPD)',
  'water_cut_percent': 'Water Cut (%)',
  'gor_scf_stb': 'GOR (scf/stb)',
  'rpm': 'RPM',
  'torque_nm': 'Torque (Nm)',
  'gas_injection_rate_mscfd': 'Gas Rate (Mscfd)',
  'injection_pressure_psi': 'Injection Pressure (psi)',
  'casing_pressure_psi': 'Casing Pressure (psi)',
  'tubing_pressure_psi': 'Tubing Pressure (psi)',
  'spm': 'SPM',
  'stroke_length_in': 'Stroke Length (in)',
  'polished_rod_load_lbs': 'PRLoad (lbs)',
  'fluid_level_ft': 'Fluid Level (ft)'
};
