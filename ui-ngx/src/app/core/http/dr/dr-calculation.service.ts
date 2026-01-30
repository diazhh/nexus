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
import {
  MseCalculationRequest,
  MseCalculationResult,
  EcdCalculationRequest,
  EcdCalculationResult,
  SwabSurgeCalculationRequest,
  SwabSurgeCalculationResult,
  KickToleranceCalculationRequest,
  KickToleranceCalculationResult,
  TorqueDragCalculationRequest,
  TorqueDragCalculationResult,
  DlsCalculationRequest,
  DlsCalculationResult,
  BitHydraulicsCalculationRequest,
  BitHydraulicsCalculationResult
} from '@shared/models/dr/dr-calculation.model';

@Injectable({
  providedIn: 'root'
})
export class DRCalculationService {

  private baseUrl = '/api/nexus/dr/calculations';

  constructor(private http: HttpClient) {}

  calculateMse(request: MseCalculationRequest): Observable<MseCalculationResult> {
    return this.http.post<MseCalculationResult>(`${this.baseUrl}/mse`, request);
  }

  calculateEcd(request: EcdCalculationRequest): Observable<EcdCalculationResult> {
    return this.http.post<EcdCalculationResult>(`${this.baseUrl}/ecd`, request);
  }

  calculateSwabSurge(request: SwabSurgeCalculationRequest): Observable<SwabSurgeCalculationResult> {
    return this.http.post<SwabSurgeCalculationResult>(`${this.baseUrl}/swab-surge`, request);
  }

  calculateKickTolerance(request: KickToleranceCalculationRequest): Observable<KickToleranceCalculationResult> {
    return this.http.post<KickToleranceCalculationResult>(`${this.baseUrl}/kick-tolerance`, request);
  }

  calculateTorqueDrag(request: TorqueDragCalculationRequest): Observable<TorqueDragCalculationResult> {
    return this.http.post<TorqueDragCalculationResult>(`${this.baseUrl}/torque-drag`, request);
  }

  calculateDls(request: DlsCalculationRequest): Observable<DlsCalculationResult> {
    return this.http.post<DlsCalculationResult>(`${this.baseUrl}/dls`, request);
  }

  calculateBitHydraulics(request: BitHydraulicsCalculationRequest): Observable<BitHydraulicsCalculationResult> {
    return this.http.post<BitHydraulicsCalculationResult>(`${this.baseUrl}/bit-hydraulics`, request);
  }
}
