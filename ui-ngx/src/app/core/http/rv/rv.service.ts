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
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  RvBasin,
  RvField,
  RvReservoir,
  RvZone,
  RvWell,
  RvCompletion,
  RvPvtStudy,
  RvIprModel,
  RvDeclineAnalysis,
  RvWellLog
} from '@shared/models/rv/rv.models';

const BASE_URL = '/api/nexus/rv';

@Injectable({
  providedIn: 'root'
})
export class RvService {

  constructor(private http: HttpClient) {}

  private getHeaders(tenantId: string): HttpHeaders {
    return new HttpHeaders().set('X-Tenant-Id', tenantId);
  }

  // ==============================================
  // BASIN ENDPOINTS
  // ==============================================

  getBasins(tenantId: string, pageLink: PageLink): Observable<PageData<RvBasin>> {
    return this.http.get<PageData<RvBasin>>(`${BASE_URL}/basins`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getBasin(id: string): Observable<RvBasin> {
    return this.http.get<RvBasin>(`${BASE_URL}/basins/${id}`);
  }

  createBasin(tenantId: string, basin: RvBasin): Observable<RvBasin> {
    return this.http.post<RvBasin>(`${BASE_URL}/basins`, basin, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateBasin(id: string, basin: RvBasin): Observable<RvBasin> {
    return this.http.put<RvBasin>(`${BASE_URL}/basins/${id}`, basin);
  }

  deleteBasin(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/basins/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // FIELD ENDPOINTS
  // ==============================================

  getFields(tenantId: string, pageLink: PageLink): Observable<PageData<RvField>> {
    return this.http.get<PageData<RvField>>(`${BASE_URL}/fields`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getFieldsByBasin(tenantId: string, basinId: string): Observable<RvField[]> {
    return this.http.get<RvField[]>(`${BASE_URL}/fields/by-basin/${basinId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getField(id: string): Observable<RvField> {
    return this.http.get<RvField>(`${BASE_URL}/fields/${id}`);
  }

  createField(tenantId: string, field: RvField): Observable<RvField> {
    return this.http.post<RvField>(`${BASE_URL}/fields`, field, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateField(id: string, field: RvField): Observable<RvField> {
    return this.http.put<RvField>(`${BASE_URL}/fields/${id}`, field);
  }

  deleteField(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/fields/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // RESERVOIR ENDPOINTS
  // ==============================================

  getReservoirs(tenantId: string, pageLink: PageLink): Observable<PageData<RvReservoir>> {
    return this.http.get<PageData<RvReservoir>>(`${BASE_URL}/reservoirs`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getReservoirsByField(tenantId: string, fieldId: string): Observable<RvReservoir[]> {
    return this.http.get<RvReservoir[]>(`${BASE_URL}/reservoirs/by-field/${fieldId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getReservoir(id: string): Observable<RvReservoir> {
    return this.http.get<RvReservoir>(`${BASE_URL}/reservoirs/${id}`);
  }

  createReservoir(tenantId: string, reservoir: RvReservoir): Observable<RvReservoir> {
    return this.http.post<RvReservoir>(`${BASE_URL}/reservoirs`, reservoir, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateReservoir(id: string, reservoir: RvReservoir): Observable<RvReservoir> {
    return this.http.put<RvReservoir>(`${BASE_URL}/reservoirs/${id}`, reservoir);
  }

  calculateOOIP(id: string): Observable<{ reservoirId: string; ooip_mmbbl: number; calculatedAt: number }> {
    return this.http.post<any>(`${BASE_URL}/reservoirs/${id}/calculate-ooip`, {});
  }

  deleteReservoir(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/reservoirs/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // ZONE ENDPOINTS
  // ==============================================

  getZones(tenantId: string, pageLink: PageLink): Observable<PageData<RvZone>> {
    return this.http.get<PageData<RvZone>>(`${BASE_URL}/zones`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getZonesByReservoir(tenantId: string, reservoirId: string): Observable<RvZone[]> {
    return this.http.get<RvZone[]>(`${BASE_URL}/zones/by-reservoir/${reservoirId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getZone(id: string): Observable<RvZone> {
    return this.http.get<RvZone>(`${BASE_URL}/zones/${id}`);
  }

  createZone(tenantId: string, zone: RvZone): Observable<RvZone> {
    return this.http.post<RvZone>(`${BASE_URL}/zones`, zone, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateZone(id: string, zone: RvZone): Observable<RvZone> {
    return this.http.put<RvZone>(`${BASE_URL}/zones/${id}`, zone);
  }

  deleteZone(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/zones/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // WELL ENDPOINTS
  // ==============================================

  getWells(tenantId: string, pageLink: PageLink): Observable<PageData<RvWell>> {
    return this.http.get<PageData<RvWell>>(`${BASE_URL}/wells`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getWellsByReservoir(tenantId: string, reservoirId: string): Observable<RvWell[]> {
    return this.http.get<RvWell[]>(`${BASE_URL}/wells/by-reservoir/${reservoirId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getWell(id: string): Observable<RvWell> {
    return this.http.get<RvWell>(`${BASE_URL}/wells/${id}`);
  }

  createWell(tenantId: string, well: RvWell): Observable<RvWell> {
    return this.http.post<RvWell>(`${BASE_URL}/wells`, well, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateWell(id: string, well: RvWell): Observable<RvWell> {
    return this.http.put<RvWell>(`${BASE_URL}/wells/${id}`, well);
  }

  updateWellStatus(id: string, status: string): Observable<void> {
    return this.http.patch<void>(`${BASE_URL}/wells/${id}/status`, { status });
  }

  calculateProductivityIndex(id: string, params: { testRate: number; reservoirPressure: number; flowingPressure: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/wells/${id}/calculate-pi`, params);
  }

  linkWellToDrilling(tenantId: string, wellId: string, drillingJobId: string): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/wells/${wellId}/link-drilling/${drillingJobId}`, {}, {
      headers: this.getHeaders(tenantId)
    });
  }

  linkWellToProduction(tenantId: string, wellId: string, productionUnitId: string): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/wells/${wellId}/link-production/${productionUnitId}`, {}, {
      headers: this.getHeaders(tenantId)
    });
  }

  deleteWell(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/wells/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // COMPLETION ENDPOINTS
  // ==============================================

  getCompletions(tenantId: string, pageLink: PageLink): Observable<PageData<RvCompletion>> {
    return this.http.get<PageData<RvCompletion>>(`${BASE_URL}/completions`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getCompletionsByWell(tenantId: string, wellId: string): Observable<RvCompletion[]> {
    return this.http.get<RvCompletion[]>(`${BASE_URL}/completions/by-well/${wellId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getCompletion(id: string): Observable<RvCompletion> {
    return this.http.get<RvCompletion>(`${BASE_URL}/completions/${id}`);
  }

  createCompletion(tenantId: string, completion: RvCompletion): Observable<RvCompletion> {
    return this.http.post<RvCompletion>(`${BASE_URL}/completions`, completion, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateCompletion(id: string, completion: RvCompletion): Observable<RvCompletion> {
    return this.http.put<RvCompletion>(`${BASE_URL}/completions/${id}`, completion);
  }

  updateCompletionStatus(id: string, status: string): Observable<void> {
    return this.http.patch<void>(`${BASE_URL}/completions/${id}/status`, { status });
  }

  recordStimulation(id: string, stimulationType: string, stimulationDate: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/completions/${id}/stimulation`, { stimulationType, stimulationDate });
  }

  updateArtificialLift(id: string, liftMethod: string, liftParams: any): Observable<void> {
    return this.http.put<void>(`${BASE_URL}/completions/${id}/artificial-lift`, { liftMethod, liftParams });
  }

  deleteCompletion(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/completions/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // PVT STUDY ENDPOINTS
  // ==============================================

  getPvtStudies(tenantId: string, pageLink: PageLink): Observable<PageData<RvPvtStudy>> {
    return this.http.get<PageData<RvPvtStudy>>(`${BASE_URL}/pvt-studies`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getPvtStudiesByReservoir(tenantId: string, reservoirId: string): Observable<RvPvtStudy[]> {
    return this.http.get<RvPvtStudy[]>(`${BASE_URL}/pvt-studies/by-reservoir/${reservoirId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getPvtStudy(id: string): Observable<RvPvtStudy> {
    return this.http.get<RvPvtStudy>(`${BASE_URL}/pvt-studies/${id}`);
  }

  createPvtStudy(tenantId: string, pvtStudy: RvPvtStudy): Observable<RvPvtStudy> {
    return this.http.post<RvPvtStudy>(`${BASE_URL}/pvt-studies`, pvtStudy, {
      headers: this.getHeaders(tenantId)
    });
  }

  updatePvtStudy(id: string, pvtStudy: RvPvtStudy): Observable<RvPvtStudy> {
    return this.http.put<RvPvtStudy>(`${BASE_URL}/pvt-studies/${id}`, pvtStudy);
  }

  calculatePvtCorrelations(id: string, params: { temperature: number; rs: number; gasGravity: number; apiGravity: number }): Observable<RvPvtStudy> {
    return this.http.post<RvPvtStudy>(`${BASE_URL}/pvt-studies/${id}/calculate-correlations`, params);
  }

  validatePvtData(id: string): Observable<{ [key: string]: string }> {
    return this.http.get<{ [key: string]: string }>(`${BASE_URL}/pvt-studies/${id}/validate`);
  }

  deletePvtStudy(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/pvt-studies/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // IPR MODEL ENDPOINTS
  // ==============================================

  getIprModels(tenantId: string, pageLink: PageLink): Observable<PageData<RvIprModel>> {
    return this.http.get<PageData<RvIprModel>>(`${BASE_URL}/ipr-models`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getIprModelsByWell(tenantId: string, wellId: string): Observable<RvIprModel[]> {
    return this.http.get<RvIprModel[]>(`${BASE_URL}/ipr-models/by-well/${wellId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getIprModel(id: string): Observable<RvIprModel> {
    return this.http.get<RvIprModel>(`${BASE_URL}/ipr-models/${id}`);
  }

  createIprModel(tenantId: string, iprModel: RvIprModel): Observable<RvIprModel> {
    return this.http.post<RvIprModel>(`${BASE_URL}/ipr-models`, iprModel, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateIprModel(id: string, iprModel: RvIprModel): Observable<RvIprModel> {
    return this.http.put<RvIprModel>(`${BASE_URL}/ipr-models/${id}`, iprModel);
  }

  calculateVogelIpr(id: string, params: { reservoirPressure: number; bubblePointPressure: number; testRate: number; testPwf: number }): Observable<RvIprModel> {
    return this.http.post<RvIprModel>(`${BASE_URL}/ipr-models/${id}/calculate-vogel`, params);
  }

  getIprCurve(id: string, numPoints: number = 20): Observable<{ pwfPsi: number; rateBopd: number }[]> {
    return this.http.get<{ pwfPsi: number; rateBopd: number }[]>(`${BASE_URL}/ipr-models/${id}/curve`, {
      params: { numPoints: numPoints.toString() }
    });
  }

  calculateOperatingPoint(id: string, currentPwf: number): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/ipr-models/${id}/operating-point`, { currentPwf });
  }

  deleteIprModel(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/ipr-models/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // DECLINE ANALYSIS ENDPOINTS
  // ==============================================

  getDeclineAnalyses(tenantId: string, pageLink: PageLink): Observable<PageData<RvDeclineAnalysis>> {
    return this.http.get<PageData<RvDeclineAnalysis>>(`${BASE_URL}/decline-analyses`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getDeclineAnalysesByWell(tenantId: string, wellId: string): Observable<RvDeclineAnalysis[]> {
    return this.http.get<RvDeclineAnalysis[]>(`${BASE_URL}/decline-analyses/by-well/${wellId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getDeclineAnalysis(id: string): Observable<RvDeclineAnalysis> {
    return this.http.get<RvDeclineAnalysis>(`${BASE_URL}/decline-analyses/${id}`);
  }

  createDeclineAnalysis(tenantId: string, analysis: RvDeclineAnalysis): Observable<RvDeclineAnalysis> {
    return this.http.post<RvDeclineAnalysis>(`${BASE_URL}/decline-analyses`, analysis, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateDeclineAnalysis(id: string, analysis: RvDeclineAnalysis): Observable<RvDeclineAnalysis> {
    return this.http.put<RvDeclineAnalysis>(`${BASE_URL}/decline-analyses/${id}`, analysis);
  }

  performDeclineAnalysis(id: string, params: { qi: number; di: number; b: number; economicLimit?: number; forecastYears?: number }): Observable<RvDeclineAnalysis> {
    return this.http.post<RvDeclineAnalysis>(`${BASE_URL}/decline-analyses/${id}/perform-analysis`, params);
  }

  getDeclineForecast(id: string, forecastYears: number = 20, monthlyIntervals: number = 6): Observable<any[]> {
    return this.http.get<any[]>(`${BASE_URL}/decline-analyses/${id}/forecast`, {
      params: {
        forecastYears: forecastYears.toString(),
        monthlyIntervals: monthlyIntervals.toString()
      }
    });
  }

  deleteDeclineAnalysis(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/decline-analyses/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // CALCULATION ENDPOINTS
  // ==============================================

  calculateOOIPVolumetric(params: { areaAcres: number; thicknessM: number; porosity: number; waterSat: number; boRbStb: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/ooip`, params);
  }

  calculateSwArchie(params: { porosity: number; rw: number; rt: number; a?: number; m?: number; n?: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/sw-archie`, params);
  }

  calculateVshLarionov(params: { grLog: number; grClean: number; grShale: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/vsh-larionov`, params);
  }

  calculatePbStanding(params: { rs: number; gasGravity: number; temperature: number; apiGravity: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/pb-standing`, params);
  }

  calculateBoStanding(params: { rs: number; gasGravity: number; oilGravity: number; temperature: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/bo-standing`, params);
  }

  calculateViscosityBeggsRobinson(params: { apiGravity: number; temperature: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/viscosity-beggs-robinson`, params);
  }

  calculateArpsDecline(params: { qi: number; di: number; b: number; time: number }): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/calculations/arps-decline`, params);
  }

  // ==============================================
  // MATERIAL BALANCE ENDPOINTS
  // ==============================================

  getMaterialBalanceStudies(tenantId: string, pageLink: PageLink): Observable<PageData<any>> {
    return this.http.get<PageData<any>>(`${BASE_URL}/material-balance`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getMaterialBalanceStudiesByReservoir(tenantId: string, reservoirId: string): Observable<any[]> {
    return this.http.get<any[]>(`${BASE_URL}/material-balance/by-reservoir/${reservoirId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getMaterialBalanceStudy(id: string): Observable<any> {
    return this.http.get<any>(`${BASE_URL}/material-balance/${id}`);
  }

  createMaterialBalanceStudy(tenantId: string, study: any): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/material-balance`, study, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateMaterialBalanceStudy(id: string, study: any): Observable<any> {
    return this.http.put<any>(`${BASE_URL}/material-balance/${id}`, study);
  }

  calculateMBETerms(id: string, study: any): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/material-balance/${id}/calculate-mbe-terms`, study);
  }

  performHavlenaOdehAnalysis(id: string, study: any): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/material-balance/${id}/havlena-odeh-analysis`, study);
  }

  getDriveMechanisms(id: string): Observable<any> {
    return this.http.get<any>(`${BASE_URL}/material-balance/${id}/drive-mechanisms`);
  }

  getHavlenaOdehPlotData(id: string): Observable<any> {
    return this.http.get<any>(`${BASE_URL}/material-balance/${id}/havlena-odeh-plot`);
  }

  deleteMaterialBalanceStudy(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/material-balance/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // SEISMIC SURVEY ENDPOINTS
  // ==============================================

  getSeismicSurveys(tenantId: string, pageLink: PageLink): Observable<PageData<any>> {
    return this.http.get<PageData<any>>(`${BASE_URL}/seismic-surveys`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getSeismicSurveysByField(tenantId: string, fieldId: string): Observable<any[]> {
    return this.http.get<any[]>(`${BASE_URL}/seismic-surveys/by-field/${fieldId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  createSeismicSurvey(tenantId: string, survey: any): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/seismic-surveys`, survey, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateSeismicSurvey(id: string, survey: any): Observable<any> {
    return this.http.put<any>(`${BASE_URL}/seismic-surveys/${id}`, survey);
  }

  deleteSeismicSurvey(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/seismic-surveys/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // FAULT ENDPOINTS
  // ==============================================

  getFaults(tenantId: string, pageLink: PageLink): Observable<PageData<any>> {
    return this.http.get<PageData<any>>(`${BASE_URL}/faults`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getFaultsByField(tenantId: string, fieldId: string): Observable<any[]> {
    return this.http.get<any[]>(`${BASE_URL}/faults/by-field/${fieldId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  createFault(tenantId: string, fault: any): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/faults`, fault, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateFault(id: string, fault: any): Observable<any> {
    return this.http.put<any>(`${BASE_URL}/faults/${id}`, fault);
  }

  deleteFault(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/faults/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // CORE ENDPOINTS
  // ==============================================

  getCores(tenantId: string, pageLink: PageLink): Observable<PageData<any>> {
    return this.http.get<PageData<any>>(`${BASE_URL}/cores`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getCoresByWell(tenantId: string, wellId: string): Observable<any[]> {
    return this.http.get<any[]>(`${BASE_URL}/cores/by-well/${wellId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  createCore(tenantId: string, core: any): Observable<any> {
    return this.http.post<any>(`${BASE_URL}/cores`, core, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateCore(id: string, core: any): Observable<any> {
    return this.http.put<any>(`${BASE_URL}/cores/${id}`, core);
  }

  deleteCore(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/cores/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  // ==============================================
  // WELL LOG ENDPOINTS
  // ==============================================

  getWellLogs(tenantId: string, pageLink: PageLink): Observable<PageData<RvWellLog>> {
    return this.http.get<PageData<RvWellLog>>(`${BASE_URL}/well-logs`, {
      headers: this.getHeaders(tenantId),
      params: {
        page: pageLink.page.toString(),
        size: pageLink.pageSize.toString()
      }
    });
  }

  getWellLogsByWell(tenantId: string, wellId: string): Observable<RvWellLog[]> {
    return this.http.get<RvWellLog[]>(`${BASE_URL}/well-logs/by-well/${wellId}`, {
      headers: this.getHeaders(tenantId)
    });
  }

  getWellLog(id: string): Observable<RvWellLog> {
    return this.http.get<RvWellLog>(`${BASE_URL}/well-logs/${id}`);
  }

  createWellLog(tenantId: string, wellLog: RvWellLog): Observable<RvWellLog> {
    return this.http.post<RvWellLog>(`${BASE_URL}/well-logs`, wellLog, {
      headers: this.getHeaders(tenantId)
    });
  }

  updateWellLog(id: string, wellLog: RvWellLog): Observable<RvWellLog> {
    return this.http.put<RvWellLog>(`${BASE_URL}/well-logs/${id}`, wellLog);
  }

  deleteWellLog(tenantId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/well-logs/${id}`, {
      headers: this.getHeaders(tenantId)
    });
  }
}
