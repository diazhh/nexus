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

export enum RunStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  SUSPENDED = 'SUSPENDED',
  CANCELLED = 'CANCELLED'
}

export enum HoleSection {
  CONDUCTOR = 'CONDUCTOR',
  SURFACE = 'SURFACE',
  INTERMEDIATE = 'INTERMEDIATE',
  PRODUCTION = 'PRODUCTION',
  LINER = 'LINER',
  SIDETRACK = 'SIDETRACK'
}

export interface DRRun extends BaseData<HasUUID> {
  tenantId: string;
  runNumber: string;
  rigId: string;
  rigCode?: string;
  wellId: string;
  wellName?: string;
  bhaId?: string;
  bhaNumber?: string;

  // MWD/LWD tool assets assigned to this run
  mwdToolAssetId?: string;
  lwdToolAssetId?: string;

  // Hole section
  holeSection: HoleSection;
  holeSizeIn?: number;
  casingSizeIn?: number;

  // Depths
  startDepthMdFt?: number;
  endDepthMdFt?: number;
  currentDepthMdFt?: number;
  startDepthTvdFt?: number;
  endDepthTvdFt?: number;

  // Dates
  spudDate?: number;
  startDate?: number;
  endDate?: number;

  // Status
  status: RunStatus;

  // Calculated KPIs
  totalFootageFt?: number;
  avgRopFtHr?: number;
  totalRotatingHours?: number;
  totalCirculatingHours?: number;
  totalConnectionTimeHours?: number;
  totalNptHours?: number;
  drillingEfficiencyPercent?: number;

  metadata?: any;
  createdTime: number;
  updatedTime?: number;
}

export interface CreateDRRunRequest {
  runNumber: string;
  rigId: string;
  wellId: string;
  bhaId?: string;
  holeSection: HoleSection;
  holeSizeIn?: number;
  casingSizeIn?: number;
  startDepthMdFt?: number;
  spudDate?: number;
}

export interface UpdateDRRunRequest {
  bhaId?: string;
  holeSection?: HoleSection;
  holeSizeIn?: number;
  casingSizeIn?: number;
  endDepthMdFt?: number;
  currentDepthMdFt?: number;
  mwdToolAssetId?: string;
  lwdToolAssetId?: string;
}

export interface StartRunRequest {
  startDepthMdFt: number;
  startDepthTvdFt?: number;
}

export interface CompleteRunRequest {
  endDepthMdFt: number;
  endDepthTvdFt?: number;
}

export interface AssignBhaRequest {
  bhaId: string;
}

export interface AssignToolRequest {
  toolAssetId: string;
}
