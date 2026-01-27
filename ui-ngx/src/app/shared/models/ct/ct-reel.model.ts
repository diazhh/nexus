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

export enum ReelStatus {
  AVAILABLE = 'AVAILABLE',
  IN_USE = 'IN_USE',
  MAINTENANCE = 'MAINTENANCE',
  RETIRED = 'RETIRED'
}

export interface CTReel extends BaseData<HasUUID> {
  tenantId: string;
  reelCode: string;
  reelName: string;
  assetId: string;
  manufacturer?: string;
  manufacturingDate?: number;
  serialNumber?: string;
  yearManufactured?: number;
  tubingOdInch?: number;
  outerDiameterInch?: number;
  tubingIdInch?: number;
  tubingWallThicknessInch?: number;
  wallThicknessInch?: number;
  weightPerFootLbs?: number;
  materialGrade?: string;
  materialType?: string;
  totalLengthFt?: number;
  reelCoreDiameterInch?: number;
  reelFlangeDiameterInch?: number;
  typicalGooseneckRadiusInch?: number;
  status: ReelStatus;
  currentUnitId?: string;
  currentUnitCode?: string;
  currentLocation?: string;
  coupledDate?: number;
  currentJobNumber?: string;
  accumulatedFatiguePercent?: number;
  totalCycles?: number;
  totalOperationalHours?: number;
  totalJobsCompleted?: number;
  estimatedRemainingCycles?: number;
  estimatedRemainingLifeCycles?: number;
  lastFatigueCalculationDate?: number;
  corrosionEnvironment?: string;
  weldStressConcentrationFactor?: number;
  lastInspectionDate?: number;
  lastInspectionResult?: string;
  nextInspectionDueDate?: number;
  description?: string;
  notes?: string;
  metadata?: any;
  createdTime: number;
  updatedTime?: number;
}

export interface CreateCTReelRequest {
  reelCode: string;
  reelName: string;
  assetId: string;
  manufacturer?: string;
  serialNumber?: string;
  tubingOdInch?: number;
  tubingIdInch?: number;
  materialGrade?: string;
  totalLengthFt?: number;
  description?: string;
}

export interface UpdateCTReelRequest {
  reelName?: string;
  status?: ReelStatus;
  currentLocation?: string;
  description?: string;
  notes?: string;
}
