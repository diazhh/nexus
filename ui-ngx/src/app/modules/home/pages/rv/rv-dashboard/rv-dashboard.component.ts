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

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageLink } from '@shared/models/page/page-link';
import { RvService } from '@core/http/rv/rv.service';
import {
  RvBasin,
  RvField,
  RvReservoir,
  RvWell,
  formatOilRate,
  formatVolume
} from '@shared/models/rv/rv.models';

interface DashboardStats {
  totalBasins: number;
  totalFields: number;
  totalReservoirs: number;
  totalWells: number;
  producingWells: number;
  totalOOIP: number;
  totalProduction: number;
  avgRecoveryFactor: number;
}

interface RecentActivity {
  type: string;
  name: string;
  action: string;
  timestamp: number;
}

@Component({
  selector: 'tb-rv-dashboard',
  templateUrl: './rv-dashboard.component.html',
  styleUrls: ['./rv-dashboard.component.scss']
})
export class RvDashboardComponent implements OnInit {

  tenantId: string;
  isLoading = true;

  stats: DashboardStats = {
    totalBasins: 0,
    totalFields: 0,
    totalReservoirs: 0,
    totalWells: 0,
    producingWells: 0,
    totalOOIP: 0,
    totalProduction: 0,
    avgRecoveryFactor: 0
  };

  recentFields: RvField[] = [];
  recentWells: RvWell[] = [];
  topProducingWells: RvWell[] = [];

  // Helper functions
  formatOilRate = formatOilRate;
  formatVolume = formatVolume;

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    const pageLink = new PageLink(100, 0);

    // Load basins count
    this.rvService.getBasins(this.tenantId, pageLink).subscribe({
      next: (data) => {
        this.stats.totalBasins = data.totalElements;
      }
    });

    // Load fields
    this.rvService.getFields(this.tenantId, pageLink).subscribe({
      next: (data) => {
        this.stats.totalFields = data.totalElements;
        this.recentFields = data.data.slice(0, 5);

        // Calculate totals from fields
        let totalOOIP = 0;
        let totalProduction = 0;
        let recoverySum = 0;
        let recoveryCount = 0;

        data.data.forEach(field => {
          if (field.ooipMmbbl) totalOOIP += field.ooipMmbbl;
          if (field.cumulativeOilProductionMmbbl) totalProduction += field.cumulativeOilProductionMmbbl;
          if (field.recoveryFactorPercent) {
            recoverySum += field.recoveryFactorPercent;
            recoveryCount++;
          }
        });

        this.stats.totalOOIP = totalOOIP;
        this.stats.totalProduction = totalProduction;
        this.stats.avgRecoveryFactor = recoveryCount > 0 ? recoverySum / recoveryCount : 0;
      }
    });

    // Load reservoirs count
    this.rvService.getReservoirs(this.tenantId, pageLink).subscribe({
      next: (data) => {
        this.stats.totalReservoirs = data.totalElements;
      }
    });

    // Load wells
    this.rvService.getWells(this.tenantId, pageLink).subscribe({
      next: (data) => {
        this.stats.totalWells = data.totalElements;
        this.stats.producingWells = data.data.filter(w => w.wellStatus === 'PRODUCING').length;
        this.recentWells = data.data.slice(0, 5);

        // Get top producing wells
        this.topProducingWells = data.data
          .filter(w => w.currentRateBopd && w.currentRateBopd > 0)
          .sort((a, b) => (b.currentRateBopd || 0) - (a.currentRateBopd || 0))
          .slice(0, 5);

        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  navigateTo(route: string): void {
    this.router.navigate(['/rv', route]);
  }

  openWellDetails(wellId: string): void {
    this.router.navigate(['/rv/wells', wellId]);
  }

  getWellStatusColor(status: string): string {
    switch (status) {
      case 'PRODUCING': return 'primary';
      case 'DRILLING': return 'accent';
      case 'COMPLETING': return 'accent';
      case 'SHUT_IN': return 'warn';
      case 'ABANDONED': return '';
      default: return '';
    }
  }

  getFieldStatusColor(status: string): string {
    switch (status) {
      case 'PRODUCING': return 'primary';
      case 'DEVELOPMENT': return 'accent';
      case 'EXPLORATION': return 'warn';
      case 'MATURE': return '';
      case 'ABANDONED': return '';
      default: return '';
    }
  }
}
