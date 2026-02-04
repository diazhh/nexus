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
import {
  PfTelemetryData,
  PfLatestTelemetry,
  PfHistoricalTelemetry,
  PfTelemetryQuery,
  TelemetryAggregation
} from '@shared/models/pf/pf-telemetry.model';

@Injectable({
  providedIn: 'root'
})
export class PfTelemetryService {

  private readonly BASE_URL = '/api/nexus/pf';

  constructor(private http: HttpClient) {}

  /**
   * Get latest telemetry for a well
   */
  getLatestTelemetry(wellId: string, config?: RequestConfig): Observable<PfLatestTelemetry> {
    return this.http.get<PfLatestTelemetry>(
      `${this.BASE_URL}/wells/${wellId}/telemetry/latest`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get latest telemetry for specific keys
   */
  getLatestTelemetryByKeys(wellId: string, keys: string[], config?: RequestConfig): Observable<PfLatestTelemetry> {
    const params = new HttpParams().set('keys', keys.join(','));
    return this.http.get<PfLatestTelemetry>(
      `${this.BASE_URL}/wells/${wellId}/telemetry/latest`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get historical telemetry for a well
   */
  getHistoricalTelemetry(query: PfTelemetryQuery, config?: RequestConfig): Observable<PfHistoricalTelemetry> {
    let params = new HttpParams()
      .set('keys', query.keys.join(','))
      .set('startTs', query.startTs.toString())
      .set('endTs', query.endTs.toString());

    if (query.interval) {
      params = params.set('interval', query.interval.toString());
    }
    if (query.aggregation && query.aggregation !== TelemetryAggregation.NONE) {
      params = params.set('agg', query.aggregation);
    }
    if (query.limit) {
      params = params.set('limit', query.limit.toString());
    }

    return this.http.get<PfHistoricalTelemetry>(
      `${this.BASE_URL}/wells/${query.entityId}/telemetry`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get telemetry for multiple wells (for comparison)
   */
  getMultiWellTelemetry(
    wellIds: string[],
    keys: string[],
    startTs: number,
    endTs: number,
    config?: RequestConfig
  ): Observable<Record<string, PfHistoricalTelemetry>> {
    const params = new HttpParams()
      .set('wellIds', wellIds.join(','))
      .set('keys', keys.join(','))
      .set('startTs', startTs.toString())
      .set('endTs', endTs.toString());

    return this.http.get<Record<string, PfHistoricalTelemetry>>(
      `${this.BASE_URL}/telemetry/multi-well`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get aggregated telemetry (e.g., daily averages)
   */
  getAggregatedTelemetry(
    wellId: string,
    key: string,
    startTs: number,
    endTs: number,
    aggregation: TelemetryAggregation,
    intervalMs: number,
    config?: RequestConfig
  ): Observable<PfHistoricalTelemetry> {
    const params = new HttpParams()
      .set('keys', key)
      .set('startTs', startTs.toString())
      .set('endTs', endTs.toString())
      .set('agg', aggregation)
      .set('interval', intervalMs.toString());

    return this.http.get<PfHistoricalTelemetry>(
      `${this.BASE_URL}/wells/${wellId}/telemetry`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get wellpad aggregated production
   */
  getWellpadProduction(wellpadId: string, config?: RequestConfig): Observable<{
    totalProductionBpd: number;
    wellCount: number;
    byWell: { wellId: string; wellName: string; productionBpd: number }[];
  }> {
    return this.http.get<any>(
      `${this.BASE_URL}/wellpads/${wellpadId}/production`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get field-level summary statistics
   */
  getFieldSummary(config?: RequestConfig): Observable<{
    totalWells: number;
    producingWells: number;
    totalProductionBpd: number;
    avgEfficiency: number;
    activeAlarms: number;
    byLiftType: Record<string, { count: number; productionBpd: number }>;
  }> {
    return this.http.get<any>(
      `${this.BASE_URL}/summary`,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
