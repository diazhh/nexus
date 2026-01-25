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
import { CTJob, CreateCTJobRequest, UpdateCTJobRequest } from '@shared/models/ct/ct-job.model';

@Injectable({
  providedIn: 'root'
})
export class CTJobService {

  private baseUrl = '/api/nexus/ct/jobs';

  constructor(private http: HttpClient) {}

  getJobs(pageLink: PageLink, tenantId: string): Observable<PageData<CTJob>> {
    return this.http.get<PageData<CTJob>>(`${this.baseUrl}/tenant/${tenantId}`, {
      params: {
        page: pageLink.page.toString(),
        pageSize: pageLink.pageSize.toString(),
        ...(pageLink.textSearch && { textSearch: pageLink.textSearch }),
        ...(pageLink.sortOrder && { sortProperty: pageLink.sortOrder.property, sortOrder: pageLink.sortOrder.direction })
      }
    });
  }

  getJob(id: string): Observable<CTJob> {
    return this.http.get<CTJob>(`${this.baseUrl}/${id}`);
  }

  createJob(request: CreateCTJobRequest): Observable<CTJob> {
    return this.http.post<CTJob>(this.baseUrl, request);
  }

  updateJob(id: string, request: UpdateCTJobRequest): Observable<CTJob> {
    return this.http.put<CTJob>(`${this.baseUrl}/${id}`, request);
  }

  deleteJob(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  startJob(id: string): Observable<CTJob> {
    return this.http.post<CTJob>(`${this.baseUrl}/${id}/start`, {});
  }

  completeJob(id: string): Observable<CTJob> {
    return this.http.post<CTJob>(`${this.baseUrl}/${id}/complete`, {});
  }

  getJobsByUnit(unitId: string): Observable<CTJob[]> {
    return this.http.get<CTJob[]>(`${this.baseUrl}/unit/${unitId}`);
  }

  getJobsByReel(reelId: string): Observable<CTJob[]> {
    return this.http.get<CTJob[]>(`${this.baseUrl}/reel/${reelId}`);
  }

  getJobsByStatus(tenantId: string, status: string): Observable<CTJob[]> {
    return this.http.get<CTJob[]>(`${this.baseUrl}/tenant/${tenantId}/status/${status}`);
  }
}
