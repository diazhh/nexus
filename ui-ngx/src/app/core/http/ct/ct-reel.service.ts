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
import { CTReel, CreateCTReelRequest, UpdateCTReelRequest } from '@shared/models/ct/ct-reel.model';
import { CreateFromTemplateRequest } from '@core/http/ct-template.service';

@Injectable({
  providedIn: 'root'
})
export class CTReelService {

  private baseUrl = '/api/nexus/ct/reels';

  constructor(private http: HttpClient) {}

  getReels(pageLink: PageLink, tenantId: string): Observable<PageData<CTReel>> {
    return this.http.get<PageData<CTReel>>(`${this.baseUrl}/tenant/${tenantId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.textSearch && { textSearch: pageLink.textSearch }),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getReel(id: string): Observable<CTReel> {
    return this.http.get<CTReel>(`${this.baseUrl}/${id}`);
  }

  createReel(request: CreateCTReelRequest): Observable<CTReel> {
    return this.http.post<CTReel>(this.baseUrl, request);
  }

  updateReel(id: string, request: UpdateCTReelRequest): Observable<CTReel> {
    return this.http.put<CTReel>(`${this.baseUrl}/${id}`, request);
  }

  deleteReel(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getReelsByStatus(tenantId: string, status: string): Observable<CTReel[]> {
    return this.http.get<CTReel[]>(`${this.baseUrl}/tenant/${tenantId}/status/${status}`);
  }

  getHighFatigueReels(tenantId: string, threshold: number = 80): Observable<CTReel[]> {
    return this.http.get<CTReel[]>(`${this.baseUrl}/tenant/${tenantId}/high-fatigue`, {
      params: { threshold: threshold.toString() }
    });
  }

  createFromTemplate(tenantId: string, request: CreateFromTemplateRequest): Observable<CTReel> {
    return this.http.post<CTReel>(`${this.baseUrl}/from-template`, request, {
      params: { tenantId }
    });
  }
}
