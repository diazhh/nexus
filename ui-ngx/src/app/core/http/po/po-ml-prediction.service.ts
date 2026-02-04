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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import {
  PoMlPrediction,
  WellPredictionSummary,
  WellPredictionDetail,
  PredictionSummary,
  PredictionQuery,
  PredictionType,
  HealthLevel
} from '@shared/models/po/po-ml-prediction.model';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';

@Injectable({
  providedIn: 'root'
})
export class PoMlPredictionService {

  constructor(private http: HttpClient) {}

  // Predictions
  getPredictions(pageLink: PageLink, query?: PredictionQuery, config?: RequestConfig): Observable<PageData<PoMlPrediction>> {
    let url = `/api/po/ml/predictions${pageLink.toQuery()}`;
    if (query) {
      if (query.wellAssetId) {
        url += `&wellAssetId=${query.wellAssetId}`;
      }
      if (query.predictionType) {
        url += `&predictionType=${query.predictionType}`;
      }
      if (query.minProbability) {
        url += `&minProbability=${query.minProbability}`;
      }
    }
    return this.http.get<PageData<PoMlPrediction>>(url, defaultHttpOptionsFromConfig(config));
  }

  getLatestPredictions(config?: RequestConfig): Observable<WellPredictionSummary[]> {
    return this.http.get<WellPredictionSummary[]>(
      '/api/po/ml/predictions/latest',
      defaultHttpOptionsFromConfig(config)
    );
  }

  getPredictionSummary(config?: RequestConfig): Observable<PredictionSummary> {
    return this.http.get<PredictionSummary>(
      '/api/po/ml/predictions/summary',
      defaultHttpOptionsFromConfig(config)
    );
  }

  // Well-specific predictions
  getWellPredictions(wellAssetId: string, config?: RequestConfig): Observable<PoMlPrediction[]> {
    return this.http.get<PoMlPrediction[]>(
      `/api/po/ml/predictions/well/${wellAssetId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellPredictionDetail(wellAssetId: string, config?: RequestConfig): Observable<WellPredictionDetail> {
    return this.http.get<WellPredictionDetail>(
      `/api/po/ml/predictions/well/${wellAssetId}/detail`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellPredictionHistory(
    wellAssetId: string,
    predictionType: PredictionType,
    startTime: number,
    endTime: number,
    config?: RequestConfig
  ): Observable<PoMlPrediction[]> {
    return this.http.get<PoMlPrediction[]>(
      `/api/po/ml/predictions/well/${wellAssetId}/history?type=${predictionType}&startTs=${startTime}&endTs=${endTime}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // Trigger predictions
  runPredictions(config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      '/api/po/ml/predictions/run',
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  runPredictionForWell(wellAssetId: string, config?: RequestConfig): Observable<PoMlPrediction[]> {
    return this.http.post<PoMlPrediction[]>(
      `/api/po/ml/predictions/run/${wellAssetId}`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  // Filtered queries
  getHighRiskWells(threshold: number = 60, config?: RequestConfig): Observable<WellPredictionSummary[]> {
    return this.http.get<WellPredictionSummary[]>(
      `/api/po/ml/predictions/high-risk?threshold=${threshold}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellsByHealthLevel(healthLevel: HealthLevel, config?: RequestConfig): Observable<WellPredictionSummary[]> {
    return this.http.get<WellPredictionSummary[]>(
      `/api/po/ml/predictions/health-level/${healthLevel}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getAnomalousWells(config?: RequestConfig): Observable<WellPredictionSummary[]> {
    return this.http.get<WellPredictionSummary[]>(
      '/api/po/ml/predictions/anomalies',
      defaultHttpOptionsFromConfig(config)
    );
  }

  // Actions
  acknowledgePrediction(predictionId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `/api/po/ml/predictions/${predictionId}/acknowledge`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  dismissPrediction(predictionId: string, reason: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(
      `/api/po/ml/predictions/${predictionId}/dismiss`,
      { reason },
      defaultHttpOptionsFromConfig(config)
    );
  }

  createWorkOrderFromPrediction(predictionId: string, config?: RequestConfig): Observable<any> {
    return this.http.post<any>(
      `/api/po/ml/predictions/${predictionId}/create-work-order`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }
}
