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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DRSurveyService } from '@core/http/dr';
import { DRDirectionalSurvey, WellTrajectory, TrajectoryPoint } from '@shared/models/dr';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-dr-directional-dashboard',
  templateUrl: './dr-directional-dashboard.component.html',
  styleUrls: ['./dr-directional-dashboard.component.scss']
})
export class DrDirectionalDashboardComponent implements OnInit, OnDestroy {

  wellId: string;
  runId: string;
  isLoading = false;

  surveys: DRDirectionalSurvey[] = [];
  trajectory: WellTrajectory;

  // Stats
  totalMd = 0;
  totalTvd = 0;
  maxInclination = 0;
  totalDisplacement = 0;
  maxDls = 0;

  // Selected survey for details
  selectedSurvey: DRDirectionalSurvey;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private surveyService: DRSurveyService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.wellId = params['wellId'];
      this.runId = params['runId'];
      this.loadData();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadData(): void {
    this.isLoading = true;

    if (this.runId) {
      this.loadRunSurveys();
    } else if (this.wellId) {
      this.loadWellTrajectory();
    }
  }

  private loadRunSurveys(): void {
    const pageLink = new PageLink(1000, 0); // Get all surveys for trajectory calculation
    this.surveyService.getSurveysByRun(this.runId, pageLink).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        this.surveys = pageData.data;
        this.calculateStats();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  private loadWellTrajectory(): void {
    this.surveyService.getWellTrajectory(this.wellId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (trajectory) => {
        this.trajectory = trajectory;
        this.totalMd = trajectory.totalMd;
        this.totalTvd = trajectory.totalTvd;
        this.maxInclination = trajectory.maxInclination;
        this.totalDisplacement = trajectory.totalDisplacement;
        this.calculateMaxDls(trajectory.points);
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  private calculateStats(): void {
    if (this.surveys.length === 0) return;

    this.totalMd = Math.max(...this.surveys.map(s => s.mdFt || 0));
    this.totalTvd = Math.max(...this.surveys.map(s => s.tvdFt || 0));
    this.maxInclination = Math.max(...this.surveys.map(s => s.inclinationDeg || 0));
    this.maxDls = Math.max(...this.surveys.map(s => s.dlsDegPer100ft || 0));

    // Calculate total displacement from last survey
    const lastSurvey = this.surveys[this.surveys.length - 1];
    if (lastSurvey?.northFt !== undefined && lastSurvey?.eastFt !== undefined) {
      this.totalDisplacement = Math.sqrt(
        Math.pow(lastSurvey.northFt, 2) + Math.pow(lastSurvey.eastFt, 2)
      );
    }
  }

  private calculateMaxDls(points: TrajectoryPoint[]): void {
    if (points && points.length > 0) {
      this.maxDls = Math.max(...points.map(p => p.dlsDegPer100ft || 0));
    }
  }

  selectSurvey(survey: DRDirectionalSurvey): void {
    this.selectedSurvey = survey;
  }

  getSurveyTypeColor(type: string): string {
    const colors: { [key: string]: string } = {
      'MWD': '#1976d2',
      'GYRO': '#9c27b0',
      'SINGLE_SHOT': '#ff9800',
      'MULTI_SHOT': '#4caf50'
    };
    return colors[type] || '#757575';
  }

  getQualityColor(quality: string): string {
    const colors: { [key: string]: string } = {
      'GOOD': '#4caf50',
      'POOR': '#ff9800',
      'REJECTED': '#f44336'
    };
    return colors[quality] || '#757575';
  }

  formatNumber(value: number, decimals: number = 2): string {
    if (value === undefined || value === null) return 'N/A';
    return value.toFixed(decimals);
  }

  goBack(): void {
    if (this.runId) {
      this.router.navigate(['/dr/runs', this.runId]);
    } else if (this.wellId) {
      this.router.navigate(['/dr/wells', this.wellId]);
    } else {
      this.router.navigate(['/dr/runs']);
    }
  }
}
