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
import { defaultHttpOptionsFromConfig, RequestConfig } from '../http-utils';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  PfWell,
  PfWellpad,
  PfFlowStation,
  PfEspSystem,
  PfPcpSystem,
  PfGasLiftSystem,
  PfRodPumpSystem,
  WellStatus,
  LiftSystemType
} from '@shared/models/pf/pf-well.model';

@Injectable({
  providedIn: 'root'
})
export class PfWellService {

  private readonly BASE_URL = '/api/nexus/pf';

  constructor(private http: HttpClient) {}

  // ============ WELLS ============

  getWells(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfWell>> {
    return this.http.get<PageData<PfWell>>(
      `${this.BASE_URL}/wells${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellsByWellpad(wellpadId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfWell>> {
    return this.http.get<PageData<PfWell>>(
      `${this.BASE_URL}/wells${pageLink.toQuery()}&wellpadId=${wellpadId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellsByStatus(status: WellStatus, pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfWell>> {
    return this.http.get<PageData<PfWell>>(
      `${this.BASE_URL}/wells${pageLink.toQuery()}&status=${status}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellsByLiftSystemType(type: LiftSystemType, pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfWell>> {
    return this.http.get<PageData<PfWell>>(
      `${this.BASE_URL}/wells${pageLink.toQuery()}&liftSystemType=${type}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWell(wellId: string, config?: RequestConfig): Observable<PfWell> {
    return this.http.get<PfWell>(
      `${this.BASE_URL}/wells/${wellId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  createWell(well: Partial<PfWell>, config?: RequestConfig): Observable<PfWell> {
    return this.http.post<PfWell>(
      `${this.BASE_URL}/wells`,
      well,
      defaultHttpOptionsFromConfig(config)
    );
  }

  updateWell(wellId: string, well: Partial<PfWell>, config?: RequestConfig): Observable<PfWell> {
    return this.http.put<PfWell>(
      `${this.BASE_URL}/wells/${wellId}`,
      well,
      defaultHttpOptionsFromConfig(config)
    );
  }

  updateWellStatus(wellId: string, status: WellStatus, config?: RequestConfig): Observable<void> {
    return this.http.patch<void>(
      `${this.BASE_URL}/wells/${wellId}/status`,
      { status },
      defaultHttpOptionsFromConfig(config)
    );
  }

  deleteWell(wellId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/wells/${wellId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ WELLPADS ============

  getWellpads(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfWellpad>> {
    return this.http.get<PageData<PfWellpad>>(
      `${this.BASE_URL}/wellpads${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getWellpad(wellpadId: string, config?: RequestConfig): Observable<PfWellpad> {
    return this.http.get<PfWellpad>(
      `${this.BASE_URL}/wellpads/${wellpadId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  createWellpad(wellpad: Partial<PfWellpad>, config?: RequestConfig): Observable<PfWellpad> {
    return this.http.post<PfWellpad>(
      `${this.BASE_URL}/wellpads`,
      wellpad,
      defaultHttpOptionsFromConfig(config)
    );
  }

  updateWellpad(wellpadId: string, wellpad: Partial<PfWellpad>, config?: RequestConfig): Observable<PfWellpad> {
    return this.http.put<PfWellpad>(
      `${this.BASE_URL}/wellpads/${wellpadId}`,
      wellpad,
      defaultHttpOptionsFromConfig(config)
    );
  }

  deleteWellpad(wellpadId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/wellpads/${wellpadId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ FLOW STATIONS ============

  getFlowStations(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfFlowStation>> {
    return this.http.get<PageData<PfFlowStation>>(
      `${this.BASE_URL}/flow-stations${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getFlowStation(flowStationId: string, config?: RequestConfig): Observable<PfFlowStation> {
    return this.http.get<PfFlowStation>(
      `${this.BASE_URL}/flow-stations/${flowStationId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ ESP SYSTEMS ============

  getEspSystems(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfEspSystem>> {
    return this.http.get<PageData<PfEspSystem>>(
      `${this.BASE_URL}/esp-systems${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getEspSystemByWell(wellId: string, config?: RequestConfig): Observable<PfEspSystem> {
    return this.http.get<PfEspSystem>(
      `${this.BASE_URL}/wells/${wellId}/esp-system`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getEspSystem(espSystemId: string, config?: RequestConfig): Observable<PfEspSystem> {
    return this.http.get<PfEspSystem>(
      `${this.BASE_URL}/esp-systems/${espSystemId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  createEspSystem(espSystem: Partial<PfEspSystem>, config?: RequestConfig): Observable<PfEspSystem> {
    return this.http.post<PfEspSystem>(
      `${this.BASE_URL}/esp-systems`,
      espSystem,
      defaultHttpOptionsFromConfig(config)
    );
  }

  updateEspSystem(espSystemId: string, espSystem: Partial<PfEspSystem>, config?: RequestConfig): Observable<PfEspSystem> {
    return this.http.put<PfEspSystem>(
      `${this.BASE_URL}/esp-systems/${espSystemId}`,
      espSystem,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ PCP SYSTEMS ============

  getPcpSystems(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfPcpSystem>> {
    return this.http.get<PageData<PfPcpSystem>>(
      `${this.BASE_URL}/pcp-systems${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getPcpSystemByWell(wellId: string, config?: RequestConfig): Observable<PfPcpSystem> {
    return this.http.get<PfPcpSystem>(
      `${this.BASE_URL}/wells/${wellId}/pcp-system`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ GAS LIFT SYSTEMS ============

  getGasLiftSystems(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfGasLiftSystem>> {
    return this.http.get<PageData<PfGasLiftSystem>>(
      `${this.BASE_URL}/gas-lift-systems${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getGasLiftSystemByWell(wellId: string, config?: RequestConfig): Observable<PfGasLiftSystem> {
    return this.http.get<PfGasLiftSystem>(
      `${this.BASE_URL}/wells/${wellId}/gas-lift-system`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ ROD PUMP SYSTEMS ============

  getRodPumpSystems(pageLink: PageLink, config?: RequestConfig): Observable<PageData<PfRodPumpSystem>> {
    return this.http.get<PageData<PfRodPumpSystem>>(
      `${this.BASE_URL}/rod-pump-systems${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getRodPumpSystemByWell(wellId: string, config?: RequestConfig): Observable<PfRodPumpSystem> {
    return this.http.get<PfRodPumpSystem>(
      `${this.BASE_URL}/wells/${wellId}/rod-pump-system`,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
