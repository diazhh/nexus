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

export interface PfWellId {
  id: string;
  entityType?: string;
}

export enum WellStatus {
  PRODUCING = 'PRODUCING',
  SHUT_IN = 'SHUT_IN',
  UNDER_WORKOVER = 'UNDER_WORKOVER',
  ABANDONED = 'ABANDONED',
  SUSPENDED = 'SUSPENDED',
  INACTIVE = 'INACTIVE'
}

export enum LiftSystemType {
  ESP = 'ESP',
  PCP = 'PCP',
  GAS_LIFT = 'GAS_LIFT',
  ROD_PUMP = 'ROD_PUMP',
  JET_PUMP = 'JET_PUMP',
  PLUNGER_LIFT = 'PLUNGER_LIFT',
  NATURAL_FLOW = 'NATURAL_FLOW',
  DILUENT_INJECTION = 'DILUENT_INJECTION'
}

export enum OperationalStatus {
  OPERATIONAL = 'OPERATIONAL',
  STANDBY = 'STANDBY',
  MAINTENANCE = 'MAINTENANCE',
  ALARM = 'ALARM',
  OFFLINE = 'OFFLINE'
}

export interface PfWell extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  apiNumber?: string;
  status: WellStatus;
  liftSystemType: LiftSystemType;
  operationalStatus?: OperationalStatus;
  latitude?: number;
  longitude?: number;
  measuredDepthFt?: number;
  trueVerticalDepthFt?: number;
  spudDate?: string;
  firstProductionDate?: string;
  currentProductionBpd?: number;
  wellpadId?: string;
  wellpadName?: string;
  rvWellId?: string;
  activeAlarmCount?: number;
}

export interface PfWellpad extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  code?: string;
  latitude?: number;
  longitude?: number;
  capacityWells?: number;
  currentWellCount?: number;
  totalProductionBpd?: number;
  commissioningDate?: string;
  operationalStatus?: OperationalStatus;
  flowStationId?: string;
}

export interface PfFlowStation extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  code?: string;
  latitude?: number;
  longitude?: number;
  designCapacityBpd?: number;
  currentThroughputBpd?: number;
  operationalStatus?: OperationalStatus;
  wellpadCount?: number;
}

export interface PfEspSystem extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  wellId: string;
  wellName?: string;
  pumpModel?: string;
  pumpSerialNumber?: string;
  stages?: number;
  ratedHeadFt?: number;
  ratedFlowBpd?: number;
  motorHp?: number;
  motorVoltage?: number;
  frequencyHz?: number;
  settingDepthFt?: number;
  installationDate?: string;
  lastPullingDate?: string;
  runLifeDays?: number;
  // Operational Limits
  minFrequencyHz?: number;
  maxFrequencyHz?: number;
  minCurrentAmps?: number;
  maxCurrentAmps?: number;
  maxMotorTempF?: number;
  minPipPsi?: number;
  maxVibrationG?: number;
}

export interface PfPcpSystem extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  wellId: string;
  wellName?: string;
  pumpModel?: string;
  rotorType?: string;
  statorType?: string;
  ratedRpm?: number;
  ratedFlowBpd?: number;
  settingDepthFt?: number;
  installationDate?: string;
  runLifeDays?: number;
  minRpm?: number;
  maxRpm?: number;
  maxTorqueNm?: number;
}

export interface PfGasLiftSystem extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  wellId: string;
  wellName?: string;
  mandrelCount?: number;
  operatingValveDepthFt?: number;
  designGasRateMscfd?: number;
  currentGasRateMscfd?: number;
  injectionPressurePsi?: number;
  minGasRateMscfd?: number;
  maxGasRateMscfd?: number;
}

export interface PfRodPumpSystem extends BaseData<PfWellId> {
  tenantId: TenantId;
  assetId: string;
  name: string;
  wellId: string;
  wellName?: string;
  pumpingUnitType?: string;
  strokeLengthIn?: number;
  strokesPerMinute?: number;
  rodDiameterIn?: number;
  pumpDiameterIn?: number;
  settingDepthFt?: number;
  installationDate?: string;
  minSpm?: number;
  maxSpm?: number;
  minStrokeLengthIn?: number;
  maxStrokeLengthIn?: number;
}

// Status color mappings
export const WellStatusColors: Record<WellStatus, string> = {
  [WellStatus.PRODUCING]: '#4caf50',
  [WellStatus.SHUT_IN]: '#ff9800',
  [WellStatus.UNDER_WORKOVER]: '#2196f3',
  [WellStatus.ABANDONED]: '#9e9e9e',
  [WellStatus.SUSPENDED]: '#ff5722',
  [WellStatus.INACTIVE]: '#795548'
};

export const LiftSystemTypeLabels: Record<LiftSystemType, string> = {
  [LiftSystemType.ESP]: 'Electric Submersible Pump',
  [LiftSystemType.PCP]: 'Progressing Cavity Pump',
  [LiftSystemType.GAS_LIFT]: 'Gas Lift',
  [LiftSystemType.ROD_PUMP]: 'Rod Pump',
  [LiftSystemType.JET_PUMP]: 'Jet Pump',
  [LiftSystemType.PLUNGER_LIFT]: 'Plunger Lift',
  [LiftSystemType.NATURAL_FLOW]: 'Natural Flow',
  [LiftSystemType.DILUENT_INJECTION]: 'Diluent Injection'
};

export const OperationalStatusColors: Record<OperationalStatus, string> = {
  [OperationalStatus.OPERATIONAL]: '#4caf50',
  [OperationalStatus.STANDBY]: '#2196f3',
  [OperationalStatus.MAINTENANCE]: '#ff9800',
  [OperationalStatus.ALARM]: '#f44336',
  [OperationalStatus.OFFLINE]: '#9e9e9e'
};
