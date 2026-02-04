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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from '../http-utils';
import { PageData } from '@shared/models/page/page-data';
import { OptimizationType } from '@shared/models/po/po-recommendation.model';

export interface OptimizationResult {
  id: string;
  wellId: string;
  wellName?: string;
  optimizationType: OptimizationType;
  currentValue: number;
  recommendedValue: number;
  unit: string;
  productionGainBpd: number;
  costSavingsUsd: number;
  confidence: number;
  hasActionRequired: boolean;
  risks?: string[];
  simulationDetails?: SimulationDetails;
  calculatedAt: number;
}

export interface SimulationDetails {
  currentProduction: number;
  estimatedProduction: number;
  currentEfficiency: number;
  estimatedEfficiency: number;
  currentPowerKw?: number;
  estimatedPowerKw?: number;
  nodalAnalysis?: {
    iprCurve: { pwf: number; qo: number }[];
    vlpCurve: { pwf: number; qo: number }[];
    operatingPoint: { pwf: number; qo: number };
  };
}

export interface EspOptimizationRequest {
  wellId: string;
  targetProduction?: number;
  maxFrequencyHz?: number;
  minFrequencyHz?: number;
  considerWearFactor?: boolean;
}

export interface GasLiftAllocationRequest {
  wellpadId?: string;
  wellIds: string[];
  totalGasAvailableMscfd: number;
  optimizationObjective: 'MAX_PRODUCTION' | 'MAX_PROFIT' | 'MIN_GAS';
}

export interface GasLiftAllocationResult {
  allocation: { wellId: string; wellName: string; gasRateMscfd: number; productionBpd: number }[];
  totalGasUsed: number;
  totalProduction: number;
  improvementBpd: number;
  improvementPercent: number;
}

export interface ProductionKpi {
  wellId: string;
  wellName?: string;
  date: string;
  oilProductionBpd: number;
  gasProductionMcfd: number;
  waterProductionBpd: number;
  potential: number;
  efficiency: number;
  uptime: number;
  deferment: number;
  bsw: number;
  gor: number;
}

@Injectable({
  providedIn: 'root'
})
export class PoOptimizationService {

  private readonly BASE_URL = '/api/nexus/po';

  constructor(private http: HttpClient) {}

  // ============ ESP OPTIMIZATION ============

  /**
   * Run ESP frequency optimization for a well
   */
  optimizeEspFrequency(request: EspOptimizationRequest, config?: RequestConfig): Observable<OptimizationResult> {
    return this.http.post<OptimizationResult>(
      `${this.BASE_URL}/optimize/esp-frequency`,
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get ESP optimization result for a well
   */
  getEspOptimization(wellId: string, config?: RequestConfig): Observable<OptimizationResult> {
    return this.http.get<OptimizationResult>(
      `${this.BASE_URL}/wells/${wellId}/optimization/esp`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ PCP OPTIMIZATION ============

  /**
   * Run PCP speed optimization for a well
   */
  optimizePcpSpeed(wellId: string, config?: RequestConfig): Observable<OptimizationResult> {
    return this.http.post<OptimizationResult>(
      `${this.BASE_URL}/optimize/pcp-speed`,
      { wellId },
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ GAS LIFT OPTIMIZATION ============

  /**
   * Run gas lift allocation optimization
   */
  optimizeGasLiftAllocation(request: GasLiftAllocationRequest, config?: RequestConfig): Observable<GasLiftAllocationResult> {
    return this.http.post<GasLiftAllocationResult>(
      `${this.BASE_URL}/optimize/gas-lift-allocation`,
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get current gas lift allocation for a wellpad
   */
  getCurrentGasLiftAllocation(wellpadId: string, config?: RequestConfig): Observable<GasLiftAllocationResult> {
    return this.http.get<GasLiftAllocationResult>(
      `${this.BASE_URL}/wellpads/${wellpadId}/gas-lift-allocation`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ ROD PUMP OPTIMIZATION ============

  /**
   * Run rod pump optimization for a well
   */
  optimizeRodPump(wellId: string, config?: RequestConfig): Observable<OptimizationResult> {
    return this.http.post<OptimizationResult>(
      `${this.BASE_URL}/optimize/rod-pump`,
      { wellId },
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ============ OPTIMIZATION HISTORY ============

  /**
   * Get optimization history for a well
   */
  getOptimizationHistory(
    wellId: string,
    startTs: number,
    endTs: number,
    config?: RequestConfig
  ): Observable<OptimizationResult[]> {
    const params = new HttpParams()
      .set('startTs', startTs.toString())
      .set('endTs', endTs.toString());

    return this.http.get<OptimizationResult[]>(
      `${this.BASE_URL}/wells/${wellId}/optimization/history`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  // ============ KPIs ============

  /**
   * Get production KPIs for a well
   */
  getProductionKpis(
    wellId: string,
    startDate: string,
    endDate: string,
    config?: RequestConfig
  ): Observable<ProductionKpi[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<ProductionKpi[]>(
      `${this.BASE_URL}/wells/${wellId}/kpis`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get field-level KPIs
   */
  getFieldKpis(
    startDate: string,
    endDate: string,
    config?: RequestConfig
  ): Observable<{
    totalProduction: number;
    avgEfficiency: number;
    totalDeferment: number;
    optimizationGain: number;
    byLiftType: Record<string, { production: number; efficiency: number; wellCount: number }>;
  }> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<any>(
      `${this.BASE_URL}/kpis/field`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }

  /**
   * Get ROI metrics
   */
  getRoiMetrics(
    startDate: string,
    endDate: string,
    config?: RequestConfig
  ): Observable<{
    productionGainBpd: number;
    revenueGainUsd: number;
    costSavingsUsd: number;
    failuresAvoided: number;
    failureCostAvoided: number;
    totalRoi: number;
  }> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<any>(
      `${this.BASE_URL}/kpis/roi`,
      { ...defaultHttpOptionsFromConfig(config), params }
    );
  }
}
