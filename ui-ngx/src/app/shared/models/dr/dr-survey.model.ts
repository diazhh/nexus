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

export enum SurveyType {
  MWD = 'MWD',
  GYRO = 'GYRO',
  SINGLE_SHOT = 'SINGLE_SHOT',
  MULTI_SHOT = 'MULTI_SHOT'
}

export enum SurveyQuality {
  GOOD = 'GOOD',
  POOR = 'POOR',
  REJECTED = 'REJECTED'
}

export interface DRDirectionalSurvey extends BaseData<HasUUID> {
  tenantId: string;
  runId: string;
  wellId: string;

  mdFt: number;
  tvdFt?: number;
  inclinationDeg?: number;
  azimuthDeg?: number;
  toolfaceDeg?: number;

  // Calculated coordinates
  northFt?: number;
  eastFt?: number;
  verticalSectionFt?: number;
  dlsDegPer100ft?: number;

  surveyType: SurveyType;
  isDefinitive: boolean;
  surveyQuality: SurveyQuality;
  surveyTime: number;

  rawData?: any;
  createdTime: number;
}

export interface CreateDRSurveyRequest {
  runId: string;
  wellId: string;
  mdFt: number;
  inclinationDeg: number;
  azimuthDeg: number;
  toolfaceDeg?: number;
  surveyType: SurveyType;
  isDefinitive?: boolean;
  surveyQuality?: SurveyQuality;
  surveyTime: number;
  rawData?: any;
}

export interface BatchSurveyRequest {
  surveys: CreateDRSurveyRequest[];
}

export interface TrajectoryPoint {
  mdFt: number;
  tvdFt: number;
  inclinationDeg: number;
  azimuthDeg: number;
  northFt: number;
  eastFt: number;
  verticalSectionFt: number;
  dlsDegPer100ft: number;
}

export interface WellTrajectory {
  wellId: string;
  wellName?: string;
  points: TrajectoryPoint[];
  totalMd: number;
  totalTvd: number;
  maxInclination: number;
  totalDisplacement: number;
}
