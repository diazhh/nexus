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
import { takeUntil, switchMap, startWith } from 'rxjs/operators';

import { PfWellService } from '@core/http/pf/pf-well.service';
import { PfTelemetryService } from '@core/http/pf/pf-telemetry.service';
import { PfAlarmService } from '@core/http/pf/pf-alarm.service';
import { PoHealthScoreService } from '@core/http/po/po-health-score.service';
import { PoRecommendationService } from '@core/http/po/po-recommendation.service';
import {
  PfWell,
  PfEspSystem,
  PfPcpSystem,
  PfGasLiftSystem,
  PfRodPumpSystem,
  WellStatus,
  LiftSystemType,
  WellStatusColors,
  LiftSystemTypeLabels
} from '@shared/models/pf/pf-well.model';
import {
  PfLatestTelemetry,
  EspTelemetryKeys,
  PcpTelemetryKeys,
  GasLiftTelemetryKeys,
  RodPumpTelemetryKeys,
  TelemetryKeyLabels
} from '@shared/models/pf/pf-telemetry.model';
import { PfAlarm, PfAlarmSeverity } from '@shared/models/pf/pf-alarm.model';
import { PoHealthScore, HealthLevelColors, HealthTrendIcons } from '@shared/models/po/po-health-score.model';
import { PoRecommendation } from '@shared/models/po/po-recommendation.model';

@Component({
  selector: 'tb-pf-well-detail',
  templateUrl: './pf-well-detail.component.html',
  styleUrls: ['./pf-well-detail.component.scss']
})
export class PfWellDetailComponent implements OnInit, OnDestroy {

  wellId: string;
  well: PfWell;
  liftSystem: PfEspSystem | PfPcpSystem | PfGasLiftSystem | PfRodPumpSystem;

  latestTelemetry: PfLatestTelemetry;
  activeAlarms: PfAlarm[] = [];
  healthScore: PoHealthScore;
  pendingRecommendations: PoRecommendation[] = [];

  isLoading = true;
  telemetryError = false;

  // Enums for template
  WellStatus = WellStatus;
  LiftSystemType = LiftSystemType;
  WellStatusColors = WellStatusColors;
  LiftSystemTypeLabels = LiftSystemTypeLabels;
  TelemetryKeyLabels = TelemetryKeyLabels;
  HealthLevelColors = HealthLevelColors;
  HealthTrendIcons = HealthTrendIcons;

  // Telemetry keys to display based on lift system type
  telemetryKeys: string[] = [];

  private destroy$ = new Subject<void>();
  private readonly REFRESH_INTERVAL = 30000; // 30 seconds

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private wellService: PfWellService,
    private telemetryService: PfTelemetryService,
    private alarmService: PfAlarmService,
    private healthScoreService: PoHealthScoreService,
    private recommendationService: PoRecommendationService
  ) {}

  ngOnInit(): void {
    this.wellId = this.route.snapshot.params['wellId'];
    this.loadWellData();
    this.startTelemetryPolling();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadWellData(): void {
    this.isLoading = true;

    this.wellService.getWell(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (well) => {
        this.well = well;
        this.setTelemetryKeys(well.liftSystemType);
        this.loadLiftSystem(well.liftSystemType);
        this.loadActiveAlarms();
        this.loadHealthScore();
        this.loadRecommendations();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading well:', error);
        this.isLoading = false;
      }
    });
  }

  private setTelemetryKeys(liftSystemType: LiftSystemType): void {
    switch (liftSystemType) {
      case LiftSystemType.ESP:
        this.telemetryKeys = EspTelemetryKeys;
        break;
      case LiftSystemType.PCP:
        this.telemetryKeys = PcpTelemetryKeys;
        break;
      case LiftSystemType.GAS_LIFT:
        this.telemetryKeys = GasLiftTelemetryKeys;
        break;
      case LiftSystemType.ROD_PUMP:
        this.telemetryKeys = RodPumpTelemetryKeys;
        break;
      default:
        this.telemetryKeys = ['production_bpd', 'water_cut_percent'];
    }
  }

  private loadLiftSystem(liftSystemType: LiftSystemType): void {
    switch (liftSystemType) {
      case LiftSystemType.ESP:
        this.wellService.getEspSystemByWell(this.wellId).pipe(takeUntil(this.destroy$))
          .subscribe(system => this.liftSystem = system);
        break;
      case LiftSystemType.PCP:
        this.wellService.getPcpSystemByWell(this.wellId).pipe(takeUntil(this.destroy$))
          .subscribe(system => this.liftSystem = system);
        break;
      case LiftSystemType.GAS_LIFT:
        this.wellService.getGasLiftSystemByWell(this.wellId).pipe(takeUntil(this.destroy$))
          .subscribe(system => this.liftSystem = system);
        break;
      case LiftSystemType.ROD_PUMP:
        this.wellService.getRodPumpSystemByWell(this.wellId).pipe(takeUntil(this.destroy$))
          .subscribe(system => this.liftSystem = system);
        break;
    }
  }

  private startTelemetryPolling(): void {
    interval(this.REFRESH_INTERVAL).pipe(
      startWith(0),
      takeUntil(this.destroy$),
      switchMap(() => this.telemetryService.getLatestTelemetry(this.wellId))
    ).subscribe({
      next: (telemetry) => {
        this.latestTelemetry = telemetry;
        this.telemetryError = false;
      },
      error: (error) => {
        console.error('Error loading telemetry:', error);
        this.telemetryError = true;
      }
    });
  }

  private loadActiveAlarms(): void {
    this.alarmService.getActiveAlarmsByWell(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alarms) => {
        this.activeAlarms = alarms;
      }
    });
  }

  private loadHealthScore(): void {
    this.healthScoreService.getHealthScore(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (score) => {
        this.healthScore = score;
      }
    });
  }

  private loadRecommendations(): void {
    this.recommendationService.getRecommendationsByWell(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (recommendations) => {
        this.pendingRecommendations = recommendations.filter(r => r.status === 'PENDING' as any);
      }
    });
  }

  getTelemetryValue(key: string): any {
    if (!this.latestTelemetry) return null;
    // Convert snake_case key to camelCase
    const camelKey = key.replace(/_([a-z])/g, (g) => g[1].toUpperCase());
    return (this.latestTelemetry as any)[camelKey] ?? (this.latestTelemetry as any)[key];
  }

  formatTelemetryValue(key: string, value: any): string {
    if (value === null || value === undefined) return '-';
    if (typeof value === 'number') {
      return value.toLocaleString('en-US', { maximumFractionDigits: 2 });
    }
    return String(value);
  }

  getStatusColor(): string {
    return WellStatusColors[this.well?.status] || '#9e9e9e';
  }

  getAlarmSeverityColor(severity: PfAlarmSeverity): string {
    const colors: Record<PfAlarmSeverity, string> = {
      [PfAlarmSeverity.CRITICAL]: '#d32f2f',
      [PfAlarmSeverity.HIGH]: '#f57c00',
      [PfAlarmSeverity.MEDIUM]: '#fbc02d',
      [PfAlarmSeverity.LOW]: '#1976d2',
      [PfAlarmSeverity.INFO]: '#7b1fa2'
    };
    return colors[severity] || '#9e9e9e';
  }

  acknowledgeAlarm(alarm: PfAlarm): void {
    this.alarmService.acknowledgeAlarm(alarm.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => this.loadActiveAlarms()
    });
  }

  viewAlarmHistory(): void {
    this.router.navigate(['/pf/alarms'], { queryParams: { wellId: this.wellId } });
  }

  viewRecommendations(): void {
    this.router.navigate(['/po/recommendations'], { queryParams: { wellId: this.wellId } });
  }

  goBack(): void {
    this.router.navigate(['/pf/wells']);
  }

  formatType(type: string): string {
    return type ? type.replace(/_/g, ' ') : '';
  }
}
