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
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { PoRecommendationService } from '@core/http/po/po-recommendation.service';
import {
  PoRecommendation,
  PoRecommendationQuery,
  PoRecommendationStats,
  RecommendationStatus,
  RecommendationPriority,
  OptimizationType,
  RecommendationStatusColors,
  RecommendationStatusLabels,
  RecommendationPriorityColors,
  OptimizationTypeLabels,
  OptimizationTypeUnits
} from '@shared/models/po/po-recommendation.model';
import { PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-po-recommendations-list',
  templateUrl: './po-recommendations-list.component.html',
  styleUrls: ['./po-recommendations-list.component.scss']
})
export class PoRecommendationsListComponent implements OnInit, OnDestroy {

  @ViewChild(MatPaginator) paginator: MatPaginator;

  displayedColumns: string[] = [
    'priority',
    'wellName',
    'type',
    'change',
    'benefit',
    'confidence',
    'status',
    'createdTime',
    'actions'
  ];

  dataSource: MatTableDataSource<PoRecommendation>;
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  // Filters
  wellIdFilter: string | null = null;
  statusFilter: RecommendationStatus[] = [RecommendationStatus.PENDING];
  priorityFilter: RecommendationPriority[] = [];
  typeFilter: OptimizationType[] = [];

  // Stats
  stats: PoRecommendationStats;

  // Enums for template
  RecommendationStatus = RecommendationStatus;
  RecommendationPriority = RecommendationPriority;
  OptimizationType = OptimizationType;
  RecommendationStatusColors = RecommendationStatusColors;
  RecommendationStatusLabels = RecommendationStatusLabels;
  RecommendationPriorityColors = RecommendationPriorityColors;
  OptimizationTypeLabels = OptimizationTypeLabels;
  OptimizationTypeUnits = OptimizationTypeUnits;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private recommendationService: PoRecommendationService
  ) {
    this.dataSource = new MatTableDataSource<PoRecommendation>([]);
  }

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.wellIdFilter = params['wellId'] || null;
      this.loadRecommendations();
    });

    this.loadStats();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadRecommendations(): void {
    this.isLoading = true;

    const query: PoRecommendationQuery = {
      pageSize: this.pageSize,
      page: this.pageIndex,
      sortProperty: 'expectedBenefitBpd',
      sortOrder: 'DESC'
    };

    if (this.wellIdFilter) {
      query.wellId = this.wellIdFilter;
    }
    if (this.statusFilter.length > 0) {
      query.status = this.statusFilter;
    }
    if (this.priorityFilter.length > 0) {
      query.priority = this.priorityFilter;
    }
    if (this.typeFilter.length > 0) {
      query.types = this.typeFilter;
    }

    this.recommendationService.getRecommendations(query).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (pageData: PageData<PoRecommendation>) => {
        this.dataSource.data = pageData.data;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading recommendations:', error);
        this.isLoading = false;
      }
    });
  }

  loadStats(): void {
    this.recommendationService.getRecommendationStats().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (stats) => {
        this.stats = stats;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRecommendations();
  }

  onStatusFilterChange(status: RecommendationStatus[]): void {
    this.statusFilter = status;
    this.pageIndex = 0;
    this.loadRecommendations();
  }

  onPriorityFilterChange(priority: RecommendationPriority[]): void {
    this.priorityFilter = priority;
    this.pageIndex = 0;
    this.loadRecommendations();
  }

  onTypeFilterChange(types: OptimizationType[]): void {
    this.typeFilter = types;
    this.pageIndex = 0;
    this.loadRecommendations();
  }

  clearFilters(): void {
    this.statusFilter = [RecommendationStatus.PENDING];
    this.priorityFilter = [];
    this.typeFilter = [];
    this.pageIndex = 0;
    this.loadRecommendations();
  }

  approveRecommendation(rec: PoRecommendation): void {
    this.recommendationService.approveRecommendation({
      recommendationId: rec.id.id
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.loadRecommendations();
        this.loadStats();
      }
    });
  }

  rejectRecommendation(rec: PoRecommendation): void {
    this.recommendationService.rejectRecommendation({
      recommendationId: rec.id.id,
      reason: 'Rejected by user'
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.loadRecommendations();
        this.loadStats();
      }
    });
  }

  executeRecommendation(rec: PoRecommendation): void {
    this.recommendationService.executeRecommendation(rec.id.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadRecommendations();
        this.loadStats();
      }
    });
  }

  viewWellDetails(rec: PoRecommendation): void {
    this.router.navigate(['/pf/wells', rec.wellId]);
  }

  generateRecommendations(): void {
    this.recommendationService.generateAllRecommendations().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        console.log('Generated recommendations:', result);
        this.loadRecommendations();
        this.loadStats();
      }
    });
  }

  getPriorityColor(priority: RecommendationPriority): string {
    return RecommendationPriorityColors[priority] || '#9e9e9e';
  }

  getStatusColor(status: RecommendationStatus): string {
    return RecommendationStatusColors[status] || '#9e9e9e';
  }

  getTypeLabel(type: OptimizationType): string {
    return OptimizationTypeLabels[type] || type;
  }

  getUnit(type: OptimizationType): string {
    return OptimizationTypeUnits[type] || '';
  }

  formatValue(value: number | undefined, decimals: number = 1): string {
    if (value === undefined || value === null) return '-';
    return value.toLocaleString('en-US', { maximumFractionDigits: decimals });
  }

  trackByRec(index: number, rec: PoRecommendation): string {
    return rec.id.id;
  }
}
