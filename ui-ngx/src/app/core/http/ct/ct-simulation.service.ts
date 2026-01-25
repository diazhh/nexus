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
import { JobParameters, SimulationResult } from '@shared/models/ct/ct-simulation.model';

@Injectable({
  providedIn: 'root'
})
export class CTSimulationService {

  private baseUrl = '/api/nexus/ct/simulation';

  constructor(private http: HttpClient) {}

  simulateJob(jobId: string): Observable<SimulationResult> {
    return this.http.post<SimulationResult>(`${this.baseUrl}/job/${jobId}`, {});
  }

  simulateCustomJob(params: JobParameters): Observable<SimulationResult> {
    return this.http.post<SimulationResult>(`${this.baseUrl}/custom`, params);
  }
}
