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

export interface PfAlarmId {
  id: string;
  entityType?: string;
}

export enum PfAlarmSeverity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
  INFO = 'INFO'
}

export enum PfAlarmStatus {
  ACTIVE_UNACK = 'ACTIVE_UNACK',
  ACTIVE_ACK = 'ACTIVE_ACK',
  CLEARED_UNACK = 'CLEARED_UNACK',
  CLEARED_ACK = 'CLEARED_ACK'
}

export interface PfAlarm extends BaseData<PfAlarmId> {
  tenantId: TenantId;
  originatorId: string;
  originatorType: string;
  originatorName?: string;
  type: string;
  severity: PfAlarmSeverity;
  status: PfAlarmStatus;
  startTs: number;
  endTs?: number;
  ackTs?: number;
  clearTs?: number;
  details?: PfAlarmDetails;
  propagate?: boolean;
  propagateToOwner?: boolean;
  propagateToTenant?: boolean;
}

export interface PfAlarmDetails {
  message?: string;
  module?: string;
  currentValue?: number;
  thresholdValue?: number;
  variable?: string;
  unit?: string;
  recommendation?: string;
  [key: string]: any;
}

export interface PfAlarmQuery {
  entityId?: string;
  status?: PfAlarmStatus[];
  severity?: PfAlarmSeverity[];
  alarmTypes?: string[];
  startTime?: number;
  endTime?: number;
  pageSize?: number;
  page?: number;
  sortProperty?: string;
  sortOrder?: 'ASC' | 'DESC';
}

export interface PfAlarmCount {
  critical: number;
  high: number;
  medium: number;
  low: number;
  total: number;
}

// Standard alarm types for PF module
export const PfAlarmTypes = {
  // ESP alarms
  MOTOR_TEMPERATURE_HIGH: 'MOTOR_TEMPERATURE_HIGH',
  MOTOR_TEMPERATURE_LOW: 'MOTOR_TEMPERATURE_LOW',
  CURRENT_HIGH: 'CURRENT_HIGH',
  CURRENT_LOW: 'CURRENT_LOW',
  PIP_LOW: 'PIP_LOW',
  VIBRATION_HIGH: 'VIBRATION_HIGH',
  FREQUENCY_OUT_OF_RANGE: 'FREQUENCY_OUT_OF_RANGE',

  // General alarms
  COMMUNICATION_LOST: 'COMMUNICATION_LOST',
  DATA_QUALITY_LOW: 'DATA_QUALITY_LOW',
  PRODUCTION_LOW: 'PRODUCTION_LOW',
  PRODUCTION_HIGH: 'PRODUCTION_HIGH',
  WATER_CUT_HIGH: 'WATER_CUT_HIGH',

  // PCP alarms
  TORQUE_HIGH: 'TORQUE_HIGH',
  RPM_OUT_OF_RANGE: 'RPM_OUT_OF_RANGE',

  // Gas Lift alarms
  GAS_INJECTION_LOW: 'GAS_INJECTION_LOW',
  GAS_INJECTION_HIGH: 'GAS_INJECTION_HIGH',
  INJECTION_PRESSURE_LOW: 'INJECTION_PRESSURE_LOW',

  // Rod Pump alarms
  PUMP_OFF: 'PUMP_OFF',
  FLUID_POUND: 'FLUID_POUND',
  GAS_INTERFERENCE: 'GAS_INTERFERENCE'
};

export const AlarmSeverityColors: Record<PfAlarmSeverity, string> = {
  [PfAlarmSeverity.CRITICAL]: '#d32f2f',
  [PfAlarmSeverity.HIGH]: '#f57c00',
  [PfAlarmSeverity.MEDIUM]: '#fbc02d',
  [PfAlarmSeverity.LOW]: '#1976d2',
  [PfAlarmSeverity.INFO]: '#7b1fa2'
};

export const AlarmSeverityLabels: Record<PfAlarmSeverity, string> = {
  [PfAlarmSeverity.CRITICAL]: 'Critical',
  [PfAlarmSeverity.HIGH]: 'High',
  [PfAlarmSeverity.MEDIUM]: 'Medium',
  [PfAlarmSeverity.LOW]: 'Low',
  [PfAlarmSeverity.INFO]: 'Info'
};

export const AlarmStatusLabels: Record<PfAlarmStatus, string> = {
  [PfAlarmStatus.ACTIVE_UNACK]: 'Active (Unacknowledged)',
  [PfAlarmStatus.ACTIVE_ACK]: 'Active (Acknowledged)',
  [PfAlarmStatus.CLEARED_UNACK]: 'Cleared (Unacknowledged)',
  [PfAlarmStatus.CLEARED_ACK]: 'Cleared (Acknowledged)'
};

export function isAlarmActive(status: PfAlarmStatus): boolean {
  return status === PfAlarmStatus.ACTIVE_UNACK || status === PfAlarmStatus.ACTIVE_ACK;
}

export function isAlarmAcknowledged(status: PfAlarmStatus): boolean {
  return status === PfAlarmStatus.ACTIVE_ACK || status === PfAlarmStatus.CLEARED_ACK;
}
