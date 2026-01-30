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
import { HasUUID } from '@shared/models/id/has-uuid';

export enum RigStatus {
  STANDBY = 'STANDBY',
  RIGGING_UP = 'RIGGING_UP',
  DRILLING = 'DRILLING',
  TRIPPING = 'TRIPPING',
  CIRCULATING = 'CIRCULATING',
  CASING = 'CASING',
  CEMENTING = 'CEMENTING',
  TESTING = 'TESTING',
  MAINTENANCE = 'MAINTENANCE',
  RIGGING_DOWN = 'RIGGING_DOWN',
  MOVING = 'MOVING',
  OUT_OF_SERVICE = 'OUT_OF_SERVICE'
}

export enum RigType {
  LAND = 'LAND',
  JACKUP = 'JACKUP',
  SEMI_SUBMERSIBLE = 'SEMI_SUBMERSIBLE',
  DRILLSHIP = 'DRILLSHIP',
  PLATFORM = 'PLATFORM',
  BARGE = 'BARGE',
  WORKOVER = 'WORKOVER'
}

export interface DRRig extends BaseData<HasUUID> {
  tenantId: string;
  rigCode: string;
  rigName: string;
  assetId: string;
  rigType: RigType;
  contractor?: string;
  manufacturer?: string;
  model?: string;
  yearBuilt?: number;
  maxHookloadLbs?: number;
  maxRotaryTorqueFtLbs?: number;
  maxDepthCapabilityFt?: number;
  operationalStatus: RigStatus;
  currentWellId?: string;
  currentWellName?: string;
  currentRunId?: string;
  currentLocation?: string;
  latitude?: number;
  longitude?: number;

  // Asset references for subsystems
  drawworksAssetId?: string;
  topDriveAssetId?: string;
  mudPump1AssetId?: string;
  mudPump2AssetId?: string;
  mudPump3AssetId?: string;
  mudSystemAssetId?: string;
  bopStackAssetId?: string;
  gasDetectorAssetId?: string;

  // Accumulated statistics
  totalWellsDrilled?: number;
  totalFootageDrilledFt?: number;
  totalNptHours?: number;
  totalOperationalHours?: number;

  // Maintenance tracking
  lastRigInspectionDate?: number;
  nextRigInspectionDue?: number;
  bopTestDate?: number;
  certificationExpiryDate?: number;

  notes?: string;
  metadata?: any;
  createdBy?: string;
  createdTime: number;
  updatedBy?: string;
  updatedTime?: number;
}

export interface CreateDRRigRequest {
  rigCode: string;
  rigName: string;
  rigType: RigType;
  contractor?: string;
  manufacturer?: string;
  model?: string;
  yearBuilt?: number;
  maxHookloadLbs?: number;
  maxRotaryTorqueFtLbs?: number;
  maxDepthCapabilityFt?: number;
  currentLocation?: string;
  latitude?: number;
  longitude?: number;
  notes?: string;
}

export interface UpdateDRRigRequest {
  rigName?: string;
  rigType?: RigType;
  contractor?: string;
  manufacturer?: string;
  model?: string;
  operationalStatus?: RigStatus;
  currentLocation?: string;
  latitude?: number;
  longitude?: number;
  notes?: string;
}

export interface AssignWellRequest {
  wellId: string;
}
