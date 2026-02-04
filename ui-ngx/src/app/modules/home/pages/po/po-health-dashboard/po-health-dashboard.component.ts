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

import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { PoHealthScoreService } from '@core/http/po/po-health-score.service';
import {
  PoHealthScore,
  HealthScoreSummary,
  HealthLevel,
  HealthTrend,
  HealthLevelColors,
  HealthLevelLabels,
  HealthTrendColors,
  HealthTrendIcons,
  isWellAtRisk,
  getHealthLevelFromScore
} from '@shared/models/po/po-health-score.model';
import { PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-po-health-dashboard',
  templateUrl: './po-health-dashboard.component.html',
  styleUrls: ['./po-health-dashboard.component.scss']
})
export class PoHealthDashboardComponent implements OnInit, OnDestroy {

  @ViewChild(MatPaginator) paginator: MatPaginator;

  displayedColumns: string[] = [
    'wellName',
    'score',
    'level',
    'trend',
    'failureProbability',
    'estimatedDays',
    'actions'
  ];

  dataSource: MatTableDataSource<PoHealthScore>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  // Filters
  levelFilter: HealthLevel[] = [];
  trendFilter: HealthTrend[] = [];
  showOnlyAtRisk = false;

  // Summary
  summary: HealthScoreSummary;

  // Wells at risk
  wellsAtRisk: PoHealthScore[] = [];

  // Enums for template
  HealthLevel = HealthLevel;
  HealthTrend = HealthTrend;
  HealthLevelColors = HealthLevelColors;
  HealthLevelLabels = HealthLevelLabels;
  HealthTrendColors = HealthTrendColors;
  HealthTrendIcons = HealthTrendIcons;

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private healthScoreService: PoHealthScoreService
  ) {
    this.dataSource = new MatTableDataSource<PoHealthScore>([]);
  }

  ngOnInit(): void {
    this.loadHealthScores();
    this.loadSummary();
    this.loadWellsAtRisk();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadHealthScores(): void {
    this.isLoading = true;

    const query: any = {
      pageSize: this.pageSize,
      page: this.pageIndex,
      sortProperty: 'score',
      sortOrder: 'ASC'
    };

    if (this.levelFilter.length > 0) {
      query.levels = this.levelFilter;
    }
    if (this.trendFilter.length > 0) {
      query.trends = this.trendFilter;
    }

    this.healthScoreService.getHealthScores(query).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (pageData: PageData<PoHealthScore>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading health scores:', error);
        this.isLoading = false;
      }
    });
  }

  loadSummary(): void {
    this.healthScoreService.getHealthScoreSummary().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (summary) => {
        this.summary = summary;
      }
    });
  }

  loadWellsAtRisk(): void {
    this.healthScoreService.getWellsAtRisk().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (wells) => {
        this.wellsAtRisk = wells.slice(0, 5);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadHealthScores();
  }

  onLevelFilterChange(levels: HealthLevel[]): void {
    this.levelFilter = levels;
    this.pageIndex = 0;
    this.loadHealthScores();
  }

  onTrendFilterChange(trends: HealthTrend[]): void {
    this.trendFilter = trends;
    this.pageIndex = 0;
    this.loadHealthScores();
  }

  toggleAtRiskFilter(): void {
    this.showOnlyAtRisk = !this.showOnlyAtRisk;
    if (this.showOnlyAtRisk) {
      this.levelFilter = [HealthLevel.CRITICAL, HealthLevel.POOR];
    } else {
      this.levelFilter = [];
    }
    this.pageIndex = 0;
    this.loadHealthScores();
  }

  clearFilters(): void {
    this.levelFilter = [];
    this.trendFilter = [];
    this.showOnlyAtRisk = false;
    this.pageIndex = 0;
    this.loadHealthScores();
  }

  recalculateAll(): void {
    this.healthScoreService.recalculateAllHealthScores().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        console.log('Recalculated:', result);
        this.loadHealthScores();
        this.loadSummary();
        this.loadWellsAtRisk();
      }
    });
  }

  viewWellDetails(score: PoHealthScore): void {
    this.router.navigate(['/pf/wells', score.wellId]);
  }

  recalculateWell(score: PoHealthScore): void {
    this.healthScoreService.recalculateHealthScore(score.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadHealthScores();
        this.loadSummary();
      }
    });
  }

  getLevelColor(level: HealthLevel): string {
    return HealthLevelColors[level] || '#9e9e9e';
  }

  getTrendColor(trend: HealthTrend): string {
    return HealthTrendColors[trend] || '#9e9e9e';
  }

  getTrendIcon(trend: HealthTrend): string {
    return HealthTrendIcons[trend] || 'remove';
  }

  getScoreGradient(score: number): string {
    const level = getHealthLevelFromScore(score);
    const color = HealthLevelColors[level];
    return `linear-gradient(90deg, ${color} ${score}%, #e0e0e0 ${score}%)`;
  }

  isAtRisk(score: PoHealthScore): boolean {
    return isWellAtRisk(score);
  }

  formatPercent(value: number | undefined): string {
    if (value === undefined || value === null) return '-';
    return (value * 100).toFixed(0) + '%';
  }

  trackByScore(index: number, score: PoHealthScore): string {
    return score.wellId;
  }
}
