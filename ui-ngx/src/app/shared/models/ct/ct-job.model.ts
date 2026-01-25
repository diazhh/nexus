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

export enum JobStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  ON_HOLD = 'ON_HOLD'
}

export interface CTJob extends BaseData<HasUUID> {
  tenantId: string;
  jobNumber: string;
  jobName: string;
  jobType: string;
  priority?: string;
  wellId?: string;
  wellName: string;
  fieldName?: string;
  clientName?: string;
  wellDepthMdFt?: number;
  wellDepthTvdFt?: number;
  targetDepthFromFt?: number;
  targetDepthToFt?: number;
  unitId: string;
  reelId: string;
  status: JobStatus;
  plannedStartDate?: number;
  plannedEndDate?: number;
  actualStartDate?: number;
  actualEndDate?: number;
  plannedDurationHours?: number;
  actualDurationHours?: number;
  plannedPumpRateBpm?: number;
  actualPumpRateBpm?: number;
  plannedMaxPressurePsi?: number;
  actualMaxPressurePsi?: number;
  plannedMaxTensionLbf?: number;
  actualMaxTensionLbf?: number;
  totalLengthRunFt?: number;
  maxDepthReachedFt?: number;
  totalCyclesPerformed?: number;
  fatigueIncrementPercent?: number;
  description?: string;
  notes?: string;
  metadata?: any;
  createdTime: number;
  updatedTime?: number;
}

export interface CreateCTJobRequest {
  jobNumber: string;
  jobName: string;
  jobType: string;
  wellName: string;
  unitId: string;
  reelId: string;
  targetDepthToFt?: number;
  plannedStartDate?: number;
  description?: string;
}

export interface UpdateCTJobRequest {
  jobName?: string;
  status?: JobStatus;
  actualStartDate?: number;
  actualEndDate?: number;
  description?: string;
  notes?: string;
}
