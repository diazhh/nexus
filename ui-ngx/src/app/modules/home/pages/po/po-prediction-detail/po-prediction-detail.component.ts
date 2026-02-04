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
import { MatSnackBar } from '@angular/material/snack-bar';

import { PoMlPredictionService } from '@core/http/po/po-ml-prediction.service';
import { PfWellService } from '@core/http/pf/pf-well.service';
import {
  WellPredictionDetail,
  PoMlPrediction,
  HealthLevel,
  HealthTrend,
  HealthLevelColors,
  HealthLevelLabels,
  HealthTrendIcons,
  HealthTrendColors,
  FactorImpact,
  FactorImpactColors,
  FactorTrendIcons,
  ContributingFactor,
  FeatureLabels,
  FeatureUnits,
  getRiskColor
} from '@shared/models/po/po-ml-prediction.model';
import { PfWell } from '@shared/models/pf/pf-well.model';

@Component({
  selector: 'tb-po-prediction-detail',
  templateUrl: './po-prediction-detail.component.html',
  styleUrls: ['./po-prediction-detail.component.scss']
})
export class PoPredictionDetailComponent implements OnInit, OnDestroy {

  wellId: string;
  well: PfWell;
  predictionDetail: WellPredictionDetail;

  isLoading = true;
  error: string;

  // Enums for template
  HealthLevel = HealthLevel;
  HealthTrend = HealthTrend;
  HealthLevelColors = HealthLevelColors;
  HealthLevelLabels = HealthLevelLabels;
  HealthTrendIcons = HealthTrendIcons;
  HealthTrendColors = HealthTrendColors;
  FactorImpact = FactorImpact;
  FactorImpactColors = FactorImpactColors;
  FactorTrendIcons = FactorTrendIcons;
  FeatureLabels = FeatureLabels;
  FeatureUnits = FeatureUnits;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private predictionService: PoMlPredictionService,
    private wellService: PfWellService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.wellId = this.route.snapshot.params['wellId'];
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadData(): void {
    this.isLoading = true;

    // Load well info
    this.wellService.getWell(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (well) => {
        this.well = well;
      },
      error: (err) => {
        console.error('Error loading well:', err);
      }
    });

    // Load prediction detail
    this.predictionService.getWellPredictionDetail(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (detail) => {
        this.predictionDetail = detail;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading predictions:', err);
        this.error = 'Error loading prediction data';
        this.isLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/po/health']);
  }

  getFeatureLabel(key: string): string {
    return FeatureLabels[key] || key;
  }

  getFeatureUnit(key: string): string {
    return FeatureUnits[key] || '';
  }

  getRiskColor(probability: number): string {
    return getRiskColor(probability);
  }

  getRiskLevel(probability: number): string {
    if (probability >= 80) return 'Critical';
    if (probability >= 60) return 'High';
    if (probability >= 40) return 'Medium';
    return 'Low';
  }

  getHealthLevelColor(level: HealthLevel): string {
    return HealthLevelColors[level] || '#9e9e9e';
  }

  getTrendIcon(trend: HealthTrend): string {
    return HealthTrendIcons[trend] || 'trending_flat';
  }

  getTrendColor(trend: HealthTrend): string {
    return HealthTrendColors[trend] || '#9e9e9e';
  }

  getFactorImpactColor(impact: FactorImpact): string {
    return FactorImpactColors[impact] || '#9e9e9e';
  }

  getFactorTrendIcon(trend: string): string {
    return FactorTrendIcons[trend as keyof typeof FactorTrendIcons] || 'remove';
  }

  formatProbability(probability: number): string {
    return probability ? `${Math.round(probability * 100)}%` : '-';
  }

  formatDate(timestamp: number): string {
    return timestamp ? new Date(timestamp).toLocaleString() : '-';
  }

  refreshPrediction(): void {
    this.predictionService.runPredictionForWell(this.wellId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.snackBar.open('Prediction updated', 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        console.error('Error running prediction:', err);
        this.snackBar.open('Error updating prediction', 'Close', { duration: 3000 });
      }
    });
  }

  createWorkOrder(): void {
    if (this.predictionDetail?.failurePrediction) {
      this.predictionService.createWorkOrderFromPrediction(
        this.predictionDetail.failurePrediction.id.id
      ).pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: (workOrder) => {
          this.snackBar.open('Work order created', 'Close', { duration: 3000 });
        },
        error: (err) => {
          console.error('Error creating work order:', err);
          this.snackBar.open('Error creating work order', 'Close', { duration: 3000 });
        }
      });
    }
  }

  dismissPrediction(): void {
    if (this.predictionDetail?.failurePrediction) {
      const reason = prompt('Please enter a reason for dismissing this prediction:');
      if (reason) {
        this.predictionService.dismissPrediction(
          this.predictionDetail.failurePrediction.id.id,
          reason
        ).pipe(
          takeUntil(this.destroy$)
        ).subscribe({
          next: () => {
            this.snackBar.open('Prediction dismissed', 'Close', { duration: 3000 });
            this.loadData();
          },
          error: (err) => {
            console.error('Error dismissing prediction:', err);
            this.snackBar.open('Error dismissing prediction', 'Close', { duration: 3000 });
          }
        });
      }
    }
  }

  viewRecommendations(): void {
    this.router.navigate(['/po/recommendations'], { queryParams: { wellId: this.wellId } });
  }
}
