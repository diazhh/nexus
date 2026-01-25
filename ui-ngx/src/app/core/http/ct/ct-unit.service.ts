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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { CTUnit, CreateCTUnitRequest, UpdateCTUnitRequest, AssignReelRequest } from '@shared/models/ct/ct-unit.model';

@Injectable({
  providedIn: 'root'
})
export class CTUnitService {

  private baseUrl = '/api/nexus/ct/units';

  constructor(private http: HttpClient) {}

  getUnits(pageLink: PageLink, tenantId: string): Observable<PageData<CTUnit>> {
    return this.http.get<PageData<CTUnit>>(`${this.baseUrl}/tenant/${tenantId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.textSearch && { textSearch: pageLink.textSearch }),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getUnit(id: string): Observable<CTUnit> {
    return this.http.get<CTUnit>(`${this.baseUrl}/${id}`);
  }

  createUnit(request: CreateCTUnitRequest): Observable<CTUnit> {
    return this.http.post<CTUnit>(this.baseUrl, request);
  }

  updateUnit(id: string, request: UpdateCTUnitRequest): Observable<CTUnit> {
    return this.http.put<CTUnit>(`${this.baseUrl}/${id}`, request);
  }

  deleteUnit(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  assignReel(unitId: string, request: AssignReelRequest): Observable<CTUnit> {
    return this.http.post<CTUnit>(`${this.baseUrl}/${unitId}/assign-reel`, request);
  }

  detachReel(unitId: string): Observable<CTUnit> {
    return this.http.post<CTUnit>(`${this.baseUrl}/${unitId}/detach-reel`, {});
  }

  getUnitsByStatus(tenantId: string, status: string): Observable<CTUnit[]> {
    return this.http.get<CTUnit[]>(`${this.baseUrl}/tenant/${tenantId}/status/${status}`);
  }
}
