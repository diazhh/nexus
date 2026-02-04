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
  PoHealthScore,
  HealthScoreHistory,
  HealthScoreQuery,
  HealthScoreSummary,
  FailurePrediction,
  HealthLevel,
  HealthTrend
} from '@shared/models/po/po-health-score.model';

@Injectable({
  providedIn: 'root'
})
export class PoHealthScoreService {

  private readonly BASE_URL = '/api/nexus/po';

  constructor(private http: HttpClient) {}

  /**
   * Get health scores with filters
   */
  getHealthScores(query: HealthScoreQuery, config?: RequestConfig): Observable<PageData<PoHealthScore>> {
    let params = new HttpParams()
      .set('pageSize', (query.pageSize || 20).toString())
      .set('page', (query.page || 0).toString());

    if (query.wellIds && query.wellIds.length > 0) {
      params = params.set('wellIds', query.wellIds.join(','));
    }
    if (query.minScore !== undefined) {
      params = params.set('minScore', query.minScore.toString());
    }
    if (query.maxScore !== undefined) {
      params = params.set('maxScore', query.maxScore.toString());
    }
    if (query.levels && query.levels.length > 0) {
      params = params.set('levels', query.levels.join(','));
    }
    if (query.trends && query.trends.length > 0) {
      params = params.set('trends', query.trends.join(','));
    }
    if (query.minFailureProbability !== undefined) {
      params = params.set('minFailureProbability', query.minFailureProbability.toString());
    }
    if (query.sortProperty) {
      params = params.set('sortProperty', query.sortProperty);
      params = params.set('sortOrder', query.sortOrder || 'ASC');
    }

    return this.http.get<PageData<PoHealthScore>>(
      `${this.BASE_URL}/health-scores`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get health score for a specific well
   */
  getHealthScore(wellId: string, config?: RequestConfig): Observable<PoHealthScore> {
    return this.http.get<PoHealthScore>(
      `${this.BASE_URL}/wells/${wellId}/health-score`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get health score history for a well
   */
  getHealthScoreHistory(
    wellId: string,
    startTs: number,
    endTs: number,
    config?: RequestConfig
  ): Observable<HealthScoreHistory> {
    const params = new HttpParams()
      .set('startTs', startTs.toString())
      .set('endTs', endTs.toString());

    return this.http.get<HealthScoreHistory>(
      `${this.BASE_URL}/wells/${wellId}/health-score/history`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get health score summary across all wells
   */
  getHealthScoreSummary(config?: RequestConfig): Observable<HealthScoreSummary> {
    return this.http.get<HealthScoreSummary>(
      `${this.BASE_URL}/health-scores/summary`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get wells at risk (low health score or high failure probability)
   */
  getWellsAtRisk(config?: RequestConfig): Observable<PoHealthScore[]> {
    return this.http.get<PoHealthScore[]>(
      `${this.BASE_URL}/health-scores/at-risk`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Recalculate health score for a well
   */
  recalculateHealthScore(wellId: string, config?: RequestConfig): Observable<PoHealthScore> {
    return this.http.post<PoHealthScore>(
      `${this.BASE_URL}/wells/${wellId}/health-score/recalculate`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Recalculate health scores for all wells
   */
  recalculateAllHealthScores(config?: RequestConfig): Observable<{ calculated: number; failed: number }> {
    return this.http.post<{ calculated: number; failed: number }>(
      `${this.BASE_URL}/health-scores/recalculate-all`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ FAILURE PREDICTION ============

  /**
   * Get failure prediction for a well
   */
  getFailurePrediction(wellId: string, config?: RequestConfig): Observable<FailurePrediction> {
    return this.http.get<FailurePrediction>(
      `${this.BASE_URL}/wells/${wellId}/failure-prediction`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get wells with high failure probability
   */
  getHighRiskWells(
    minProbability: number = 0.5,
    config?: RequestConfig
  ): Observable<FailurePrediction[]> {
    const params = new HttpParams().set('minProbability', minProbability.toString());

    return this.http.get<FailurePrediction[]>(
      `${this.BASE_URL}/failure-predictions/high-risk`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Run failure prediction for a well
   */
  runFailurePrediction(wellId: string, config?: RequestConfig): Observable<FailurePrediction> {
    return this.http.post<FailurePrediction>(
      `${this.BASE_URL}/wells/${wellId}/failure-prediction/run`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }
}
