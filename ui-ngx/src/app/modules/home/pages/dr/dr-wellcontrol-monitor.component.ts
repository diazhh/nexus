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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { DRCalculationService, DRKpiService } from '@core/http/dr';
import {
  KickToleranceCalculationRequest,
  KickToleranceCalculationResult,
  SwabSurgeCalculationRequest,
  SwabSurgeCalculationResult,
  EcdCalculationRequest,
  EcdCalculationResult
} from '@shared/models/dr';

export enum WellControlStatus {
  NORMAL = 'NORMAL',
  CAUTION = 'CAUTION',
  WARNING = 'WARNING',
  CRITICAL = 'CRITICAL'
}

export interface WellControlIndicator {
  name: string;
  value: number;
  unit: string;
  threshold: number;
  status: WellControlStatus;
  trend: 'up' | 'down' | 'stable';
}

@Component({
  selector: 'tb-dr-wellcontrol-monitor',
  templateUrl: './dr-wellcontrol-monitor.component.html',
  styleUrls: ['./dr-wellcontrol-monitor.component.scss']
})
export class DrWellcontrolMonitorComponent implements OnInit, OnDestroy {

  runId: string;
  wellId: string;
  isLoading = false;

  // Well Control Status
  overallStatus: WellControlStatus = WellControlStatus.NORMAL;

  // Current Parameters (simulated real-time)
  currentDepthFt = 0;
  currentMudWeightPpg = 0;
  currentEcdPpg = 0;
  formationPressurePpg = 0;
  fracturePressurePpg = 0;

  // Kick Tolerance
  kickTolerance: KickToleranceCalculationResult;
  maxKickVolumesBbl = 0;
  maxInfluxHeightFt = 0;

  // Swab/Surge
  swabSurgeResult: SwabSurgeCalculationResult;
  swabPressurePsi = 0;
  surgePressurePsi = 0;

  // ECD
  ecdResult: EcdCalculationResult;

  // Indicators
  indicators: WellControlIndicator[] = [];

  // Alerts
  activeAlerts: string[] = [];

  // Refresh interval (5 seconds)
  private refreshInterval = 5000;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private calculationService: DRCalculationService,
    private kpiService: DRKpiService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.runId = params['runId'];
      this.wellId = params['wellId'];
      this.loadInitialData();
      this.startRealTimeMonitoring();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadInitialData(): void {
    this.isLoading = true;
    // Load initial well control parameters
    this.calculateKickTolerance();
    this.calculateSwabSurge();
    this.calculateEcd();
    this.isLoading = false;
  }

  private startRealTimeMonitoring(): void {
    interval(this.refreshInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshData();
      });
  }

  refreshData(): void {
    this.calculateKickTolerance();
    this.calculateSwabSurge();
    this.calculateEcd();
    this.updateIndicators();
    this.checkAlerts();
  }

  private calculateKickTolerance(): void {
    if (!this.runId) return;

    const request: KickToleranceCalculationRequest = {
      mudWeightPpg: this.currentMudWeightPpg,
      tvdFt: this.currentDepthFt * 0.95, // Approximate TVD
      porePressurePpg: this.formationPressurePpg,
      fracGradientPpg: this.fracturePressurePpg,
      casingShoeDepthFt: this.currentDepthFt * 0.7,
      annularCapacityBblFt: 0.0459
    };

    this.calculationService.calculateKickTolerance(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.kickTolerance = result;
          this.maxKickVolumesBbl = result.kickToleranceBbl || 0;
          this.maxInfluxHeightFt = result.maxInfluxHeightFt || 0;
        },
        error: () => {}
      });
  }

  private calculateSwabSurge(): void {
    if (!this.runId) return;

    const request: SwabSurgeCalculationRequest = {
      pipeOdIn: 5,
      holeIdIn: 8.5,
      pipeLengthFt: this.currentDepthFt,
      tripSpeedFtMin: 90,
      mudWeightPpg: this.currentMudWeightPpg,
      plasticViscosityCp: 25,
      yieldPointLbf100sqft: 15
    };

    this.calculationService.calculateSwabSurge(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.swabSurgeResult = result;
          this.swabPressurePsi = result.swabPressurePsi || 0;
          this.surgePressurePsi = result.surgePressurePsi || 0;
        },
        error: () => {}
      });
  }

  private calculateEcd(): void {
    if (!this.runId) return;

    const request: EcdCalculationRequest = {
      mudWeightPpg: this.currentMudWeightPpg,
      tvdFt: this.currentDepthFt * 0.95,
      annularPressureLossPsi: 150
    };

    this.calculationService.calculateEcd(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.ecdResult = result;
          this.currentEcdPpg = result.ecdPpg || 0;
        },
        error: () => {}
      });
  }

  private updateIndicators(): void {
    // Convert PSI to PPG equivalent for display (approximate)
    const swabPpgEquivalent = this.swabSurgeResult?.swabEcdPpg || 0;
    const surgePpgEquivalent = this.swabSurgeResult?.surgeEcdPpg || 0;

    this.indicators = [
      {
        name: 'Mud Weight',
        value: this.currentMudWeightPpg,
        unit: 'ppg',
        threshold: this.formationPressurePpg,
        status: this.getMudWeightStatus(),
        trend: 'stable'
      },
      {
        name: 'ECD',
        value: this.currentEcdPpg,
        unit: 'ppg',
        threshold: this.fracturePressurePpg,
        status: this.getEcdStatus(),
        trend: 'stable'
      },
      {
        name: 'Kick Tolerance',
        value: this.maxKickVolumesBbl,
        unit: 'bbl',
        threshold: 20,
        status: this.getKickToleranceStatus(),
        trend: 'stable'
      },
      {
        name: 'Swab ECD',
        value: swabPpgEquivalent,
        unit: 'ppg',
        threshold: this.formationPressurePpg,
        status: this.getSwabMarginStatus(),
        trend: 'stable'
      },
      {
        name: 'Surge ECD',
        value: surgePpgEquivalent,
        unit: 'ppg',
        threshold: this.fracturePressurePpg,
        status: this.getSurgeMarginStatus(),
        trend: 'stable'
      }
    ];

    this.updateOverallStatus();
  }

  private getMudWeightStatus(): WellControlStatus {
    const margin = this.currentMudWeightPpg - this.formationPressurePpg;
    if (margin < 0) return WellControlStatus.CRITICAL;
    if (margin < 0.2) return WellControlStatus.WARNING;
    if (margin < 0.5) return WellControlStatus.CAUTION;
    return WellControlStatus.NORMAL;
  }

  private getEcdStatus(): WellControlStatus {
    const margin = this.fracturePressurePpg - this.currentEcdPpg;
    if (margin < 0) return WellControlStatus.CRITICAL;
    if (margin < 0.3) return WellControlStatus.WARNING;
    if (margin < 0.5) return WellControlStatus.CAUTION;
    return WellControlStatus.NORMAL;
  }

  private getKickToleranceStatus(): WellControlStatus {
    if (this.maxKickVolumesBbl < 10) return WellControlStatus.CRITICAL;
    if (this.maxKickVolumesBbl < 20) return WellControlStatus.WARNING;
    if (this.maxKickVolumesBbl < 30) return WellControlStatus.CAUTION;
    return WellControlStatus.NORMAL;
  }

  private getSwabMarginStatus(): WellControlStatus {
    const swabEcd = this.swabSurgeResult?.swabEcdPpg || this.currentMudWeightPpg;
    const margin = swabEcd - this.formationPressurePpg;
    if (margin < 0) return WellControlStatus.CRITICAL;
    if (margin < 0.2) return WellControlStatus.WARNING;
    if (margin < 0.3) return WellControlStatus.CAUTION;
    return WellControlStatus.NORMAL;
  }

  private getSurgeMarginStatus(): WellControlStatus {
    const surgeEcd = this.swabSurgeResult?.surgeEcdPpg || this.currentMudWeightPpg;
    const margin = this.fracturePressurePpg - surgeEcd;
    if (margin < 0) return WellControlStatus.CRITICAL;
    if (margin < 0.2) return WellControlStatus.WARNING;
    if (margin < 0.3) return WellControlStatus.CAUTION;
    return WellControlStatus.NORMAL;
  }

  private updateOverallStatus(): void {
    const statuses = this.indicators.map(i => i.status);
    if (statuses.includes(WellControlStatus.CRITICAL)) {
      this.overallStatus = WellControlStatus.CRITICAL;
    } else if (statuses.includes(WellControlStatus.WARNING)) {
      this.overallStatus = WellControlStatus.WARNING;
    } else if (statuses.includes(WellControlStatus.CAUTION)) {
      this.overallStatus = WellControlStatus.CAUTION;
    } else {
      this.overallStatus = WellControlStatus.NORMAL;
    }
  }

  private checkAlerts(): void {
    this.activeAlerts = [];

    if (this.currentMudWeightPpg < this.formationPressurePpg) {
      this.activeAlerts.push('UNDERBALANCED: Mud weight below formation pressure!');
    }

    if (this.currentEcdPpg > this.fracturePressurePpg) {
      this.activeAlerts.push('LOST CIRCULATION RISK: ECD exceeds fracture pressure!');
    }

    if (this.maxKickVolumesBbl < 10) {
      this.activeAlerts.push('LOW KICK TOLERANCE: Consider increasing mud weight or setting casing');
    }

    const swabEcd = this.swabSurgeResult?.swabEcdPpg || this.currentMudWeightPpg;
    const swabMargin = swabEcd - this.formationPressurePpg;
    if (swabMargin < 0.2) {
      this.activeAlerts.push('SWAB RISK: Reduce trip speed to prevent kick');
    }

    const surgeEcd = this.swabSurgeResult?.surgeEcdPpg || this.currentMudWeightPpg;
    const surgeMargin = this.fracturePressurePpg - surgeEcd;
    if (surgeMargin < 0.2) {
      this.activeAlerts.push('SURGE RISK: Reduce trip speed to prevent lost circulation');
    }
  }

  getStatusColor(status: WellControlStatus): string {
    const colors: { [key: string]: string } = {
      'NORMAL': '#4caf50',
      'CAUTION': '#ff9800',
      'WARNING': '#f44336',
      'CRITICAL': '#b71c1c'
    };
    return colors[status] || '#757575';
  }

  getStatusIcon(status: WellControlStatus): string {
    const icons: { [key: string]: string } = {
      'NORMAL': 'check_circle',
      'CAUTION': 'warning',
      'WARNING': 'error',
      'CRITICAL': 'dangerous'
    };
    return icons[status] || 'help';
  }

  formatNumber(value: number, decimals: number = 2): string {
    if (value === undefined || value === null) return 'N/A';
    return value.toFixed(decimals);
  }

  goBack(): void {
    if (this.runId) {
      this.router.navigate(['/dr/runs', this.runId]);
    } else {
      this.router.navigate(['/dr/dashboards/realtime']);
    }
  }

  // Simulation methods for demo purposes
  simulateKick(): void {
    this.currentMudWeightPpg = this.formationPressurePpg - 0.5;
    this.refreshData();
  }

  simulateLostCirculation(): void {
    this.currentEcdPpg = this.fracturePressurePpg + 0.3;
    this.refreshData();
  }

  resetSimulation(): void {
    this.currentMudWeightPpg = 12.5;
    this.currentEcdPpg = 13.0;
    this.formationPressurePpg = 11.5;
    this.fracturePressurePpg = 15.0;
    this.currentDepthFt = 10000;
    this.refreshData();
  }
}
