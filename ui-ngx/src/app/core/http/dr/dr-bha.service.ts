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
  DRBha,
  CreateDRBhaRequest,
  UpdateDRBhaRequest,
  DullGradeRequest,
  BhaType,
  BhaStatus
} from '@shared/models/dr/dr-bha.model';

@Injectable({
  providedIn: 'root'
})
export class DRBhaService {

  private baseUrl = '/api/nexus/dr/bhas';

  constructor(private http: HttpClient) {}

  getBha(id: string): Observable<DRBha> {
    return this.http.get<DRBha>(`${this.baseUrl}/${id}`);
  }

  getBhas(pageLink: PageLink, tenantId: string): Observable<PageData<DRBha>> {
    return this.http.get<PageData<DRBha>>(`${this.baseUrl}/tenant/${tenantId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.textSearch && { textSearch: pageLink.textSearch }),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getAvailableBhas(tenantId: string): Observable<DRBha[]> {
    return this.http.get<DRBha[]>(`${this.baseUrl}/tenant/${tenantId}/available`);
  }

  getBhasByType(tenantId: string, bhaType: BhaType): Observable<DRBha[]> {
    return this.http.get<DRBha[]>(`${this.baseUrl}/tenant/${tenantId}/type/${bhaType}`);
  }

  getBhasByStatus(tenantId: string, status: BhaStatus): Observable<DRBha[]> {
    return this.http.get<DRBha[]>(`${this.baseUrl}/tenant/${tenantId}/status/${status}`);
  }

  getBhasByBitSize(tenantId: string, bitSizeIn: number): Observable<DRBha[]> {
    return this.http.get<DRBha[]>(`${this.baseUrl}/tenant/${tenantId}/bit-size/${bitSizeIn}`);
  }

  createBha(request: CreateDRBhaRequest): Observable<DRBha> {
    return this.http.post<DRBha>(this.baseUrl, request);
  }

  updateBha(id: string, request: UpdateDRBhaRequest): Observable<DRBha> {
    return this.http.put<DRBha>(`${this.baseUrl}/${id}`, request);
  }

  deleteBha(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  recordDullGrade(id: string, request: DullGradeRequest): Observable<DRBha> {
    return this.http.put<DRBha>(`${this.baseUrl}/${id}/dull-grade`, request);
  }

  assignToRun(bhaId: string, runId: string): Observable<DRBha> {
    return this.http.post<DRBha>(`${this.baseUrl}/${bhaId}/assign-run/${runId}`, {});
  }

  retireBha(id: string): Observable<DRBha> {
    return this.http.post<DRBha>(`${this.baseUrl}/${id}/retire`, {});
  }

  sendToMaintenance(id: string): Observable<DRBha> {
    return this.http.post<DRBha>(`${this.baseUrl}/${id}/maintenance`, {});
  }

  markAvailable(id: string): Observable<DRBha> {
    return this.http.post<DRBha>(`${this.baseUrl}/${id}/available`, {});
  }
}
