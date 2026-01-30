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
  DRRig,
  CreateDRRigRequest,
  UpdateDRRigRequest,
  AssignWellRequest,
  RigStatus
} from '@shared/models/dr/dr-rig.model';

@Injectable({
  providedIn: 'root'
})
export class DRRigService {

  private baseUrl = '/api/nexus/dr/rigs';

  constructor(private http: HttpClient) {}

  getRigs(pageLink: PageLink, tenantId: string): Observable<PageData<DRRig>> {
    return this.http.get<PageData<DRRig>>(`${this.baseUrl}/tenant/${tenantId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.textSearch && { textSearch: pageLink.textSearch }),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getRig(id: string): Observable<DRRig> {
    return this.http.get<DRRig>(`${this.baseUrl}/${id}`);
  }

  createRig(request: CreateDRRigRequest): Observable<DRRig> {
    return this.http.post<DRRig>(this.baseUrl, request);
  }

  updateRig(id: string, request: UpdateDRRigRequest): Observable<DRRig> {
    return this.http.put<DRRig>(`${this.baseUrl}/${id}`, request);
  }

  deleteRig(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: string, status: RigStatus): Observable<DRRig> {
    return this.http.put<DRRig>(`${this.baseUrl}/${id}/status`, { status });
  }

  assignWell(rigId: string, request: AssignWellRequest): Observable<DRRig> {
    return this.http.post<DRRig>(`${this.baseUrl}/${rigId}/assign-well/${request.wellId}`, {});
  }

  releaseWell(rigId: string): Observable<DRRig> {
    return this.http.post<DRRig>(`${this.baseUrl}/${rigId}/release-well`, {});
  }

  getRigsByStatus(tenantId: string, status: RigStatus): Observable<DRRig[]> {
    return this.http.get<DRRig[]>(`${this.baseUrl}/tenant/${tenantId}/status/${status}`);
  }

  recordBopTest(rigId: string, testDate: number, testPressure: number): Observable<DRRig> {
    return this.http.post<DRRig>(`${this.baseUrl}/${rigId}/bop-test`, {
      testDate,
      testPressure
    });
  }

  recordInspection(rigId: string, inspectionDate: number, notes?: string): Observable<DRRig> {
    return this.http.post<DRRig>(`${this.baseUrl}/${rigId}/inspection`, {
      inspectionDate,
      notes
    });
  }
}
