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
  DRRun,
  CreateDRRunRequest,
  UpdateDRRunRequest,
  StartRunRequest,
  CompleteRunRequest,
  AssignBhaRequest,
  AssignToolRequest,
  RunStatus
} from '@shared/models/dr/dr-run.model';
import { RunKpi } from '@shared/models/dr/dr-kpi.model';

@Injectable({
  providedIn: 'root'
})
export class DRRunService {

  private baseUrl = '/api/nexus/dr/runs';

  constructor(private http: HttpClient) {}

  getRun(id: string): Observable<DRRun> {
    return this.http.get<DRRun>(`${this.baseUrl}/${id}`);
  }

  getRunsByRig(rigId: string, pageLink: PageLink): Observable<PageData<DRRun>> {
    return this.http.get<PageData<DRRun>>(`${this.baseUrl}/rig/${rigId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getRunsByWell(wellId: string, pageLink: PageLink): Observable<PageData<DRRun>> {
    return this.http.get<PageData<DRRun>>(`${this.baseUrl}/well/${wellId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getRunsByStatus(tenantId: string, status: RunStatus): Observable<DRRun[]> {
    return this.http.get<DRRun[]>(`${this.baseUrl}/tenant/${tenantId}/status/${status}`);
  }

  createRun(request: CreateDRRunRequest): Observable<DRRun> {
    return this.http.post<DRRun>(this.baseUrl, request);
  }

  updateRun(id: string, request: UpdateDRRunRequest): Observable<DRRun> {
    return this.http.put<DRRun>(`${this.baseUrl}/${id}`, request);
  }

  deleteRun(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  startRun(id: string, request: StartRunRequest): Observable<DRRun> {
    return this.http.put<DRRun>(`${this.baseUrl}/${id}/start`, request);
  }

  completeRun(id: string, request: CompleteRunRequest): Observable<DRRun> {
    return this.http.put<DRRun>(`${this.baseUrl}/${id}/complete`, request);
  }

  suspendRun(id: string): Observable<DRRun> {
    return this.http.put<DRRun>(`${this.baseUrl}/${id}/suspend`, {});
  }

  resumeRun(id: string): Observable<DRRun> {
    return this.http.put<DRRun>(`${this.baseUrl}/${id}/resume`, {});
  }

  assignBha(runId: string, request: AssignBhaRequest): Observable<DRRun> {
    return this.http.post<DRRun>(`${this.baseUrl}/${runId}/assign-bha/${request.bhaId}`, {});
  }

  assignMwdTool(runId: string, request: AssignToolRequest): Observable<DRRun> {
    return this.http.post<DRRun>(`${this.baseUrl}/${runId}/assign-mwd`, request);
  }

  assignLwdTool(runId: string, request: AssignToolRequest): Observable<DRRun> {
    return this.http.post<DRRun>(`${this.baseUrl}/${runId}/assign-lwd`, request);
  }

  getRunKpis(id: string): Observable<RunKpi> {
    return this.http.get<RunKpi>(`${this.baseUrl}/${id}/kpis`);
  }
}
