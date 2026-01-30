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
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import {
  DRDirectionalSurvey,
  CreateDRSurveyRequest,
  BatchSurveyRequest,
  WellTrajectory,
  TrajectoryPoint
} from '@shared/models/dr/dr-survey.model';

@Injectable({
  providedIn: 'root'
})
export class DRSurveyService {

  private baseUrl = '/api/nexus/dr/surveys';

  constructor(private http: HttpClient) {}

  getSurvey(id: string): Observable<DRDirectionalSurvey> {
    return this.http.get<DRDirectionalSurvey>(`${this.baseUrl}/${id}`);
  }

  getSurveysByRun(runId: string, pageLink: PageLink): Observable<PageData<DRDirectionalSurvey>> {
    return this.http.get<PageData<DRDirectionalSurvey>>(`${this.baseUrl}/run/${runId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getSurveysByWell(wellId: string, pageLink: PageLink): Observable<PageData<DRDirectionalSurvey>> {
    return this.http.get<PageData<DRDirectionalSurvey>>(`${this.baseUrl}/well/${wellId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getDefinitiveSurveys(wellId: string): Observable<DRDirectionalSurvey[]> {
    return this.http.get<DRDirectionalSurvey[]>(`${this.baseUrl}/well/${wellId}/definitive`);
  }

  getWellTrajectory(wellId: string): Observable<WellTrajectory> {
    return this.http.get<WellTrajectory>(`${this.baseUrl}/well/${wellId}/trajectory`);
  }

  getInterpolatedPoint(wellId: string, mdFt: number): Observable<TrajectoryPoint> {
    return this.http.get<TrajectoryPoint>(`${this.baseUrl}/well/${wellId}/interpolate`, {
      params: { mdFt: mdFt.toString() }
    });
  }

  createSurvey(request: CreateDRSurveyRequest): Observable<DRDirectionalSurvey> {
    return this.http.post<DRDirectionalSurvey>(this.baseUrl, request);
  }

  createBatchSurveys(request: BatchSurveyRequest): Observable<DRDirectionalSurvey[]> {
    return this.http.post<DRDirectionalSurvey[]>(`${this.baseUrl}/batch`, request);
  }

  deleteSurvey(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  markAsDefinitive(id: string): Observable<DRDirectionalSurvey> {
    return this.http.put<DRDirectionalSurvey>(`${this.baseUrl}/${id}/definitive`, {});
  }

  updateQuality(id: string, quality: string): Observable<DRDirectionalSurvey> {
    return this.http.put<DRDirectionalSurvey>(`${this.baseUrl}/${id}/quality`, { quality });
  }

  getHighDlsSurveys(wellId: string, threshold: number): Observable<DRDirectionalSurvey[]> {
    return this.http.get<DRDirectionalSurvey[]>(`${this.baseUrl}/well/${wellId}/high-dls`, {
      params: { threshold: threshold.toString() }
    });
  }
}
