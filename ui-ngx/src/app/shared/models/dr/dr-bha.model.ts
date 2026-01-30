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

export enum BhaType {
  ROTARY = 'ROTARY',
  MOTOR = 'MOTOR',
  RSS = 'RSS',
  HYBRID = 'HYBRID',
  VERTICAL = 'VERTICAL'
}

export enum BhaStatus {
  AVAILABLE = 'AVAILABLE',
  IN_USE = 'IN_USE',
  MAINTENANCE = 'MAINTENANCE',
  RETIRED = 'RETIRED'
}

export interface BhaComponent {
  position: number;
  componentType: string;
  description: string;
  od: number;
  id?: number;
  lengthFt: number;
  weightLbs?: number;
  serialNumber?: string;
  connection?: string;
}

export interface DRBha extends BaseData<HasUUID> {
  tenantId: string;
  bhaNumber: string;
  assetId: string;
  bhaType: BhaType;
  isDirectional: boolean;

  // Bit information
  bitSerial?: string;
  bitType?: string;
  bitSizeIn?: number;
  bitIadcCode?: string;
  bitTfaSqIn?: number;

  // Dimensions
  totalLengthFt?: number;
  totalWeightLbs?: number;

  // Tracking
  status: BhaStatus;
  totalFootageDrilled?: number;
  totalHoursOnBottom?: number;

  // Components
  componentsJson?: BhaComponent[];

  // Dull grading
  bitDullInner?: string;
  bitDullOuter?: string;
  bitDullChar?: string;
  bitReasonPulled?: string;

  createdTime: number;
  updatedTime?: number;
}

export interface CreateDRBhaRequest {
  bhaNumber: string;
  bhaType: BhaType;
  isDirectional?: boolean;
  bitSerial?: string;
  bitType?: string;
  bitSizeIn?: number;
  bitIadcCode?: string;
  bitTfaSqIn?: number;
  totalLengthFt?: number;
  totalWeightLbs?: number;
  componentsJson?: BhaComponent[];
}

export interface UpdateDRBhaRequest {
  bhaType?: BhaType;
  bitSerial?: string;
  bitType?: string;
  bitSizeIn?: number;
  bitIadcCode?: string;
  bitTfaSqIn?: number;
  totalLengthFt?: number;
  totalWeightLbs?: number;
  componentsJson?: BhaComponent[];
}

export interface DullGradeRequest {
  bitDullInner: string;
  bitDullOuter: string;
  bitDullChar?: string;
  bitReasonPulled?: string;
}
