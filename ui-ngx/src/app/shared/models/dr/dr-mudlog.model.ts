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

export enum LithologyType {
  SANDSTONE = 'SANDSTONE',
  SHALE = 'SHALE',
  LIMESTONE = 'LIMESTONE',
  DOLOMITE = 'DOLOMITE',
  SILTSTONE = 'SILTSTONE',
  CLAYSTONE = 'CLAYSTONE',
  MARL = 'MARL',
  COAL = 'COAL',
  ANHYDRITE = 'ANHYDRITE',
  GYPSUM = 'GYPSUM',
  SALT = 'SALT',
  CHERT = 'CHERT',
  CONGLOMERATE = 'CONGLOMERATE',
  GRANITE = 'GRANITE',
  BASITE = 'BASALT'
}

export interface DRMudLog extends BaseData<HasUUID> {
  tenantId: string;
  runId: string;

  depthFt: number;
  lagDepthFt?: number;

  // Drilling parameters
  ropFtHr?: number;
  wobKlbs?: number;
  rpm?: number;
  torqueFtLbs?: number;
  sppPsi?: number;

  // Gases
  totalGasUnits?: number;
  c1MethanePpm?: number;
  c2EthanePpm?: number;
  c3PropanePpm?: number;
  ic4IsobutanePpm?: number;
  nc4NButanePpm?: number;
  c5PentanePpm?: number;
  h2sPpm?: number;
  co2Ppm?: number;

  // Calculated gas ratios
  wetnessRatio?: number;
  balanceRatio?: number;
  characterRatio?: number;

  // Lithology and shows
  lithology?: LithologyType;
  lithologyPercent?: number;
  hasOilShow: boolean;
  hasGasShow: boolean;
  fluorescence?: string;
  cut?: string;
  stain?: string;

  logTime: number;
  createdTime: number;
}

export interface CreateDRMudLogRequest {
  runId: string;
  depthFt: number;
  lagDepthFt?: number;
  ropFtHr?: number;
  wobKlbs?: number;
  rpm?: number;
  torqueFtLbs?: number;
  sppPsi?: number;
  totalGasUnits?: number;
  c1MethanePpm?: number;
  c2EthanePpm?: number;
  c3PropanePpm?: number;
  ic4IsobutanePpm?: number;
  nc4NButanePpm?: number;
  c5PentanePpm?: number;
  h2sPpm?: number;
  co2Ppm?: number;
  lithology?: LithologyType;
  lithologyPercent?: number;
  hasOilShow?: boolean;
  hasGasShow?: boolean;
  fluorescence?: string;
  cut?: string;
  stain?: string;
  logTime: number;
}

export interface BatchMudLogRequest {
  mudLogs: CreateDRMudLogRequest[];
}

export interface GasAnalysis {
  depthFt: number;
  totalGas: number;
  c1: number;
  c2: number;
  c3: number;
  ic4: number;
  nc4: number;
  c5: number;
  wetnessRatio: number;
  balanceRatio: number;
  characterRatio: number;
}

export interface LithologyInterval {
  startDepthFt: number;
  endDepthFt: number;
  lithology: LithologyType;
  avgLithologyPercent: number;
  hasOilShow: boolean;
  hasGasShow: boolean;
}
