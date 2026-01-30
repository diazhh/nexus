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
import { DRRunService } from '@core/http/dr/dr-run.service';
import { DRSurveyService } from '@core/http/dr/dr-survey.service';
import { DRMudLogService } from '@core/http/dr/dr-mudlog.service';
import { DRRun, RunStatus, HoleSection } from '@shared/models/dr/dr-run.model';
import { RunKpi } from '@shared/models/dr/dr-kpi.model';
import { DRDirectionalSurvey } from '@shared/models/dr/dr-survey.model';
import { DRMudLog } from '@shared/models/dr/dr-mudlog.model';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-dr-run-details',
  templateUrl: './dr-run-details.component.html',
  styleUrls: ['./dr-run-details.component.scss']
})
export class DrRunDetailsComponent implements OnInit, OnDestroy {

  run: DRRun;
  runKpis: RunKpi;
  recentSurveys: DRDirectionalSurvey[] = [];
  recentMudLogs: DRMudLog[] = [];
  isLoading = true;
  selectedTab = 0;

  RunStatus = RunStatus;
  HoleSection = HoleSection;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private runService: DRRunService,
    private surveyService: DRSurveyService,
    private mudLogService: DRMudLogService
  ) {}

  ngOnInit() {
    const runId = this.route.snapshot.params.id;
    if (runId) {
      this.loadRunDetails(runId);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadRunDetails(runId: string) {
    this.isLoading = true;
    this.runService.getRun(runId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (run) => {
        this.run = run;
        this.loadRunKpis(runId);
        this.loadRecentSurveys(runId);
        this.loadRecentMudLogs(runId);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading run:', error);
        this.isLoading = false;
      }
    });
  }

  loadRunKpis(runId: string) {
    this.runService.getRunKpis(runId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (kpis) => this.runKpis = kpis,
      error: (error) => console.error('Error loading run KPIs:', error)
    });
  }

  loadRecentSurveys(runId: string) {
    const pageLink = new PageLink(10, 0);
    this.surveyService.getSurveysByRun(runId, pageLink).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => this.recentSurveys = pageData.data,
      error: (error) => console.error('Error loading surveys:', error)
    });
  }

  loadRecentMudLogs(runId: string) {
    const pageLink = new PageLink(10, 0);
    this.mudLogService.getMudLogsByRun(runId, pageLink).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => this.recentMudLogs = pageData.data,
      error: (error) => console.error('Error loading mud logs:', error)
    });
  }

  goBack() {
    this.router.navigate(['/dr/runs']);
  }

  getStatusColor(status: RunStatus): string {
    switch (status) {
      case RunStatus.IN_PROGRESS: return '#4caf50';
      case RunStatus.PLANNED: return '#2196f3';
      case RunStatus.COMPLETED: return '#9e9e9e';
      case RunStatus.SUSPENDED: return '#ff9800';
      case RunStatus.CANCELLED: return '#f44336';
      default: return '#9e9e9e';
    }
  }

  getStatusLabel(status: RunStatus): string {
    return status.replace(/_/g, ' ');
  }
}
