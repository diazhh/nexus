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
  PoMlConfig,
  PoMlModel,
  PoMlTrainingJob,
  StartTrainingRequest,
  DEFAULT_ML_CONFIG
} from '@shared/models/po/po-ml-config.model';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';

@Injectable({
  providedIn: 'root'
})
export class PoMlConfigService {

  constructor(private http: HttpClient) {}

  // ML Configuration
  getMlConfig(config?: RequestConfig): Observable<PoMlConfig> {
    return this.http.get<PoMlConfig>(
      '/api/po/ml/config',
      defaultHttpOptionsFromConfig(config)
    );
  }

  saveMlConfig(mlConfig: PoMlConfig, config?: RequestConfig): Observable<PoMlConfig> {
    return this.http.put<PoMlConfig>(
      '/api/po/ml/config',
      mlConfig,
      defaultHttpOptionsFromConfig(config)
    );
  }

  resetMlConfigToDefaults(config?: RequestConfig): Observable<PoMlConfig> {
    return this.http.post<PoMlConfig>(
      '/api/po/ml/config/reset',
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  getDefaultConfig(): Partial<PoMlConfig> {
    return { ...DEFAULT_ML_CONFIG };
  }

  // ML Models
  getModels(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PoMlModel>> {
    return this.http.get<PageData<PoMlModel>>(
      `/api/po/ml/models${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getAllModels(config?: RequestConfig): Observable<PoMlModel[]> {
    return this.http.get<PoMlModel[]>(
      '/api/po/ml/models/all',
      defaultHttpOptionsFromConfig(config)
    );
  }

  getModel(modelId: string, config?: RequestConfig): Observable<PoMlModel> {
    return this.http.get<PoMlModel>(
      `/api/po/ml/models/${modelId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getActiveModels(config?: RequestConfig): Observable<PoMlModel[]> {
    return this.http.get<PoMlModel[]>(
      '/api/po/ml/models/active',
      defaultHttpOptionsFromConfig(config)
    );
  }

  getModelsByType(modelType: string, config?: RequestConfig): Observable<PoMlModel[]> {
    return this.http.get<PoMlModel[]>(
      `/api/po/ml/models/type/${modelType}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  deployModel(modelId: string, config?: RequestConfig): Observable<PoMlModel> {
    return this.http.post<PoMlModel>(
      `/api/po/ml/models/${modelId}/deploy`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  archiveModel(modelId: string, config?: RequestConfig): Observable<PoMlModel> {
    return this.http.post<PoMlModel>(
      `/api/po/ml/models/${modelId}/archive`,
      {},
      defaultHttpOptionsFromConfig(config)
    );
  }

  deleteModel(modelId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `/api/po/ml/models/${modelId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // Training Jobs
  getTrainingJobs(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PoMlTrainingJob>> {
    return this.http.get<PageData<PoMlTrainingJob>>(
      `/api/po/ml/training/jobs${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getTrainingJob(jobId: string, config?: RequestConfig): Observable<PoMlTrainingJob> {
    return this.http.get<PoMlTrainingJob>(
      `/api/po/ml/training/jobs/${jobId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  startTrainingJob(request: StartTrainingRequest, config?: RequestConfig): Observable<PoMlTrainingJob> {
    return this.http.post<PoMlTrainingJob>(
      '/api/po/ml/training/jobs',
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  cancelTrainingJob(jobId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `/api/po/ml/training/jobs/${jobId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getRunningJobs(config?: RequestConfig): Observable<PoMlTrainingJob[]> {
    return this.http.get<PoMlTrainingJob[]>(
      '/api/po/ml/training/jobs/running',
      defaultHttpOptionsFromConfig(config)
    );
  }
}
