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
  PoRecommendation,
  PoRecommendationQuery,
  PoRecommendationStats,
  ApproveRecommendationRequest,
  RejectRecommendationRequest,
  RecommendationStatus,
  RecommendationPriority,
  OptimizationType
} from '@shared/models/po/po-recommendation.model';

@Injectable({
  providedIn: 'root'
})
export class PoRecommendationService {

  private readonly BASE_URL = '/api/nexus/po';

  constructor(private http: HttpClient) {}

  /**
   * Get recommendations with filters
   */
  getRecommendations(query: PoRecommendationQuery, config?: RequestConfig): Observable<PageData<PoRecommendation>> {
    let params = new HttpParams()
      .set('pageSize', (query.pageSize || 20).toString())
      .set('page', (query.page || 0).toString());

    if (query.wellId) {
      params = params.set('wellId', query.wellId);
    }
    if (query.status && query.status.length > 0) {
      params = params.set('status', query.status.join(','));
    }
    if (query.priority && query.priority.length > 0) {
      params = params.set('priority', query.priority.join(','));
    }
    if (query.types && query.types.length > 0) {
      params = params.set('types', query.types.join(','));
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

    return this.http.get<PageData<PoRecommendation>>(
      `${this.BASE_URL}/recommendations`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get pending recommendations (for dashboard)
   */
  getPendingRecommendations(config?: RequestConfig): Observable<PoRecommendation[]> {
    return this.http.get<PoRecommendation[]>(
      `${this.BASE_URL}/recommendations/pending`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get recommendations for a specific well
   */
  getRecommendationsByWell(wellId: string, config?: RequestConfig): Observable<PoRecommendation[]> {
    return this.http.get<PoRecommendation[]>(
      `${this.BASE_URL}/wells/${wellId}/recommendations`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get recommendation by ID
   */
  getRecommendation(recommendationId: string, config?: RequestConfig): Observable<PoRecommendation> {
    return this.http.get<PoRecommendation>(
      `${this.BASE_URL}/recommendations/${recommendationId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Approve a recommendation
   */
  approveRecommendation(request: ApproveRecommendationRequest, config?: RequestConfig): Observable<PoRecommendation> {
    return this.http.post<PoRecommendation>(
      `${this.BASE_URL}/recommendations/${request.recommendationId}/approve`,
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Reject a recommendation
   */
  rejectRecommendation(request: RejectRecommendationRequest, config?: RequestConfig): Observable<PoRecommendation> {
    return this.http.post<PoRecommendation>(
      `${this.BASE_URL}/recommendations/${request.recommendationId}/reject`,
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Execute an approved recommendation
   */
  executeRecommendation(recommendationId: string, config?: RequestConfig): Observable<PoRecommendation> {
    return this.http.post<PoRecommendation>(
      `${this.BASE_URL}/recommendations/${recommendationId}/execute`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Cancel a recommendation
   */
  cancelRecommendation(recommendationId: string, reason: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `${this.BASE_URL}/recommendations/${recommendationId}/cancel`,
      { reason },
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get recommendation statistics
   */
  getRecommendationStats(config?: RequestConfig): Observable<PoRecommendationStats> {
    return this.http.get<PoRecommendationStats>(
      `${this.BASE_URL}/recommendations/stats`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get recommendation statistics for a well
   */
  getRecommendationStatsByWell(wellId: string, config?: RequestConfig): Observable<PoRecommendationStats> {
    return this.http.get<PoRecommendationStats>(
      `${this.BASE_URL}/wells/${wellId}/recommendations/stats`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Trigger recommendation generation for a well
   */
  generateRecommendations(wellId: string, config?: RequestConfig): Observable<PoRecommendation[]> {
    return this.http.post<PoRecommendation[]>(
      `${this.BASE_URL}/wells/${wellId}/recommendations/generate`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Trigger recommendation generation for all wells
   */
  generateAllRecommendations(config?: RequestConfig): Observable<{ generated: number; skipped: number }> {
    return this.http.post<{ generated: number; skipped: number }>(
      `${this.BASE_URL}/recommendations/generate-all`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get recommendation effectiveness tracking
   */
  getEffectivenessHistory(
    recommendationId: string,
    config?: RequestConfig
  ): Observable<{ ts: number; productionBpd: number; expected: number }[]> {
    return this.http.get<any[]>(
      `${this.BASE_URL}/recommendations/${recommendationId}/effectiveness`,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
