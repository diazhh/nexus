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
  DRMudLog,
  CreateDRMudLogRequest,
  BatchMudLogRequest,
  GasAnalysis,
  LithologyInterval,
  LithologyType
} from '@shared/models/dr/dr-mudlog.model';

@Injectable({
  providedIn: 'root'
})
export class DRMudLogService {

  private baseUrl = '/api/nexus/dr/mudlogs';

  constructor(private http: HttpClient) {}

  getMudLog(id: string): Observable<DRMudLog> {
    return this.http.get<DRMudLog>(`${this.baseUrl}/${id}`);
  }

  getMudLogsByRun(runId: string, pageLink: PageLink): Observable<PageData<DRMudLog>> {
    return this.http.get<PageData<DRMudLog>>(`${this.baseUrl}/run/${runId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getMudLogsByDepthRange(runId: string, startDepthFt: number, endDepthFt: number): Observable<DRMudLog[]> {
    return this.http.get<DRMudLog[]>(`${this.baseUrl}/run/${runId}/depth-range`, {
      params: {
        startDepthFt: startDepthFt.toString(),
        endDepthFt: endDepthFt.toString()
      }
    });
  }

  getGasAnalysis(runId: string): Observable<GasAnalysis[]> {
    return this.http.get<GasAnalysis[]>(`${this.baseUrl}/run/${runId}/gas-analysis`);
  }

  getLithologyIntervals(runId: string): Observable<LithologyInterval[]> {
    return this.http.get<LithologyInterval[]>(`${this.baseUrl}/run/${runId}/lithology-intervals`);
  }

  getMudLogsByLithology(runId: string, lithology: LithologyType): Observable<DRMudLog[]> {
    return this.http.get<DRMudLog[]>(`${this.baseUrl}/run/${runId}/lithology/${lithology}`);
  }

  getOilShows(runId: string): Observable<DRMudLog[]> {
    return this.http.get<DRMudLog[]>(`${this.baseUrl}/run/${runId}/oil-shows`);
  }

  getGasShows(runId: string): Observable<DRMudLog[]> {
    return this.http.get<DRMudLog[]>(`${this.baseUrl}/run/${runId}/gas-shows`);
  }

  getHighGasReadings(runId: string, threshold: number): Observable<DRMudLog[]> {
    return this.http.get<DRMudLog[]>(`${this.baseUrl}/run/${runId}/high-gas`, {
      params: { threshold: threshold.toString() }
    });
  }

  createMudLog(request: CreateDRMudLogRequest): Observable<DRMudLog> {
    return this.http.post<DRMudLog>(this.baseUrl, request);
  }

  createBatchMudLogs(request: BatchMudLogRequest): Observable<DRMudLog[]> {
    return this.http.post<DRMudLog[]>(`${this.baseUrl}/batch`, request);
  }

  deleteMudLog(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
