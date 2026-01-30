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
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RunKpi,
  RigKpi,
  WellKpi,
  ConnectionTimeKpi,
  DrillingEfficiencyKpi,
  KpiTimeRange
} from '@shared/models/dr/dr-kpi.model';

@Injectable({
  providedIn: 'root'
})
export class DRKpiService {

  private baseUrl = '/api/nexus/dr/kpis';

  constructor(private http: HttpClient) {}

  getRunKpis(runId: string): Observable<RunKpi> {
    return this.http.get<RunKpi>(`${this.baseUrl}/run/${runId}`);
  }

  getRigKpis(rigId: string, timeRange?: KpiTimeRange): Observable<RigKpi> {
    const params: any = {};
    if (timeRange) {
      params.startTime = timeRange.startTime.toString();
      params.endTime = timeRange.endTime.toString();
    }
    return this.http.get<RigKpi>(`${this.baseUrl}/rig/${rigId}`, { params });
  }

  getWellKpis(wellId: string): Observable<WellKpi> {
    return this.http.get<WellKpi>(`${this.baseUrl}/well/${wellId}`);
  }

  getConnectionTimeKpis(runId: string): Observable<ConnectionTimeKpi> {
    return this.http.get<ConnectionTimeKpi>(`${this.baseUrl}/run/${runId}/connection-time`);
  }

  getDrillingEfficiencyKpis(runId: string): Observable<DrillingEfficiencyKpi> {
    return this.http.get<DrillingEfficiencyKpi>(`${this.baseUrl}/run/${runId}/drilling-efficiency`);
  }

  getTenantRigKpis(tenantId: string, timeRange?: KpiTimeRange): Observable<RigKpi[]> {
    const params: any = {};
    if (timeRange) {
      params.startTime = timeRange.startTime.toString();
      params.endTime = timeRange.endTime.toString();
    }
    return this.http.get<RigKpi[]>(`${this.baseUrl}/tenant/${tenantId}/rigs`, { params });
  }

  getFleetKpisSummary(tenantId: string, timeRange?: KpiTimeRange): Observable<any> {
    const params: any = {};
    if (timeRange) {
      params.startTime = timeRange.startTime.toString();
      params.endTime = timeRange.endTime.toString();
    }
    return this.http.get<any>(`${this.baseUrl}/tenant/${tenantId}/fleet-summary`, { params });
  }
}
