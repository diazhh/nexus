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

import { BaseData } from '@shared/models/base-data';
import { HasUUID } from '@shared/models/id/has-uuid';

export enum UnitStatus {
  STANDBY = 'STANDBY',
  ACTIVE = 'ACTIVE',
  MAINTENANCE = 'MAINTENANCE',
  OUT_OF_SERVICE = 'OUT_OF_SERVICE'
}

export interface CTUnit extends BaseData<HasUUID> {
  tenantId: string;
  unitCode: string;
  unitName: string;
  assetId: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  yearManufactured?: number;
  maxPressurePsi?: number;
  maxTensionLbf?: number;
  maxSpeedFtMin?: number;
  maxTubingOdInch?: number;
  operationalStatus: UnitStatus;
  currentLocation?: string;
  latitude?: number;
  longitude?: number;
  totalOperationalHours?: number;
  totalJobsCompleted?: number;
  totalMetersDeployed?: number;
  currentReelId?: string;
  currentReelCode?: string;
  reelCoupledDate?: number;
  lastMaintenanceDate?: number;
  lastMaintenanceHours?: number;
  nextMaintenanceDueHours?: number;
  maintenanceIntervalHours?: number;
  maintenanceOverdue?: boolean;
  lastPressureTestDate?: number;
  lastPressureTestPsi?: number;
  certificationExpiryDate?: number;
  notes?: string;
  metadata?: any;
  createdTime: number;
  updatedTime?: number;
}

export interface CreateCTUnitRequest {
  unitCode: string;
  unitName: string;
  assetId: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  yearManufactured?: number;
  maxPressurePsi?: number;
  maxTensionLbf?: number;
  maxSpeedFtMin?: number;
  currentLocation?: string;
  notes?: string;
}

export interface UpdateCTUnitRequest {
  unitName?: string;
  manufacturer?: string;
  model?: string;
  operationalStatus?: UnitStatus;
  currentLocation?: string;
  notes?: string;
}

export interface AssignReelRequest {
  reelId: string;
}
