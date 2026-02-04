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

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from '../http-utils';
import { PageData } from '@shared/models/page/page-data';
import {
  PfAlarm,
  PfAlarmQuery,
  PfAlarmCount,
  PfAlarmSeverity,
  PfAlarmStatus
} from '@shared/models/pf/pf-alarm.model';

@Injectable({
  providedIn: 'root'
})
export class PfAlarmService {

  private readonly BASE_URL = '/api/nexus/pf';

  constructor(private http: HttpClient) {}

  /**
   * Get alarms with filters
   */
  getAlarms(query: PfAlarmQuery, config?: RequestConfig): Observable<PageData<PfAlarm>> {
    let params = new HttpParams()
      .set('pageSize', (query.pageSize || 20).toString())
      .set('page', (query.page || 0).toString());

    if (query.entityId) {
      params = params.set('entityId', query.entityId);
    }
    if (query.status && query.status.length > 0) {
      params = params.set('status', query.status.join(','));
    }
    if (query.severity && query.severity.length > 0) {
      params = params.set('severity', query.severity.join(','));
    }
    if (query.alarmTypes && query.alarmTypes.length > 0) {
      params = params.set('alarmTypes', query.alarmTypes.join(','));
    }
    if (query.startTime) {
      params = params.set('startTime', query.startTime.toString());
    }
    if (query.endTime) {
      params = params.set('endTime', query.endTime.toString());
    }
    if (query.sortProperty) {
      params = params.set('sortProperty', query.sortProperty);
      params = params.set('sortOrder', query.sortOrder || 'DESC');
    }

    return this.http.get<PageData<PfAlarm>>(
      `${this.BASE_URL}/alarms`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get active alarms for a specific well
   */
  getActiveAlarmsByWell(wellId: string, config?: RequestConfig): Observable<PfAlarm[]> {
    return this.http.get<PfAlarm[]>(
      `${this.BASE_URL}/wells/${wellId}/alarms/active`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get all active alarms
   */
  getActiveAlarms(severity?: PfAlarmSeverity[], config?: RequestConfig): Observable<PfAlarm[]> {
    let params = new HttpParams();
    if (severity && severity.length > 0) {
      params = params.set('severity', severity.join(','));
    }

    return this.http.get<PfAlarm[]>(
      `${this.BASE_URL}/alarms/active`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get alarm by ID
   */
  getAlarm(alarmId: string, config?: RequestConfig): Observable<PfAlarm> {
    return this.http.get<PfAlarm>(
      `${this.BASE_URL}/alarms/${alarmId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Acknowledge an alarm
   */
  acknowledgeAlarm(alarmId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `${this.BASE_URL}/alarms/${alarmId}/ack`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Clear an alarm
   */
  clearAlarm(alarmId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `${this.BASE_URL}/alarms/${alarmId}/clear`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Acknowledge multiple alarms
   */
  acknowledgeAlarms(alarmIds: string[], config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `${this.BASE_URL}/alarms/ack`,
      { alarmIds },
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get alarm count by severity
   */
  getAlarmCount(entityId?: string, config?: RequestConfig): Observable<PfAlarmCount> {
    let params = new HttpParams();
    if (entityId) {
      params = params.set('entityId', entityId);
    }

    return this.http.get<PfAlarmCount>(
      `${this.BASE_URL}/alarms/count`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get alarm types
   */
  getAlarmTypes(config?: RequestConfig): Observable<string[]> {
    return this.http.get<string[]>(
      `${this.BASE_URL}/alarms/types`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get alarm history for a well
   */
  getAlarmHistory(
    wellId: string,
    startTime: number,
    endTime: number,
    config?: RequestConfig
  ): Observable<PfAlarm[]> {
    const params = new HttpParams()
      .set('startTime', startTime.toString())
      .set('endTime', endTime.toString());

    return this.http.get<PfAlarm[]>(
      `${this.BASE_URL}/wells/${wellId}/alarms/history`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }
}
