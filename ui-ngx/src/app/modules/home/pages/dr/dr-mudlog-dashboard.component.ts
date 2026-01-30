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
import { DRMudLogService } from '@core/http/dr';
import { DRMudLog, LithologyType } from '@shared/models/dr';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-dr-mudlog-dashboard',
  templateUrl: './dr-mudlog-dashboard.component.html',
  styleUrls: ['./dr-mudlog-dashboard.component.scss']
})
export class DrMudlogDashboardComponent implements OnInit, OnDestroy {

  runId: string;
  wellId: string;
  isLoading = false;

  mudLogs: DRMudLog[] = [];
  selectedMudLog: DRMudLog;

  // Stats
  totalEntries = 0;
  maxGasReading = 0;
  avgRop = 0;
  currentDepth = 0;

  // Filters
  depthFromFt: number;
  depthToFt: number;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private mudLogService: DRMudLogService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.runId = params['runId'];
      this.wellId = params['wellId'];
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
      this.loadRunMudLogs();
    }
  }

  private loadRunMudLogs(): void {
    // If depth filters are provided, use depth range endpoint
    if (this.depthFromFt !== undefined && this.depthToFt !== undefined) {
      this.mudLogService.getMudLogsByDepthRange(this.runId, this.depthFromFt, this.depthToFt)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (logs) => {
            this.mudLogs = logs;
            this.calculateStats();
            this.isLoading = false;
          },
          error: () => {
            this.isLoading = false;
          }
        });
    } else {
      // Otherwise use paged endpoint
      const pageLink = new PageLink(1000, 0);
      this.mudLogService.getMudLogsByRun(this.runId, pageLink)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (pageData) => {
            this.mudLogs = pageData.data;
            this.calculateStats();
            this.isLoading = false;
          },
          error: () => {
            this.isLoading = false;
          }
        });
    }
  }

  private calculateStats(): void {
    if (this.mudLogs.length === 0) return;

    this.totalEntries = this.mudLogs.length;
    this.maxGasReading = Math.max(...this.mudLogs.map(l => l.totalGasUnits || 0));

    const rops = this.mudLogs.filter(l => l.ropFtHr > 0).map(l => l.ropFtHr);
    this.avgRop = rops.length > 0 ? rops.reduce((a, b) => a + b, 0) / rops.length : 0;

    this.currentDepth = Math.max(...this.mudLogs.map(l => l.depthFt || 0));
  }

  selectMudLog(log: DRMudLog): void {
    this.selectedMudLog = log;
  }

  applyFilter(): void {
    this.loadData();
  }

  clearFilter(): void {
    this.depthFromFt = undefined;
    this.depthToFt = undefined;
    this.loadData();
  }

  getLithologyColor(type: LithologyType): string {
    const colors: { [key: string]: string } = {
      'SANDSTONE': '#FFD700',
      'SHALE': '#808080',
      'LIMESTONE': '#4169E1',
      'DOLOMITE': '#9370DB',
      'SILTSTONE': '#DEB887',
      'CLAY': '#8B4513',
      'MARL': '#D2B48C',
      'ANHYDRITE': '#E6E6FA',
      'SALT': '#FFFFFF',
      'COAL': '#000000',
      'GRANITE': '#FF6347',
      'BASITE': '#2F4F4F',
      'OTHER': '#CCCCCC'
    };
    return colors[type] || '#CCCCCC';
  }

  getGasLevelClass(gasUnits: number): string {
    if (gasUnits > 500) return 'gas-high';
    if (gasUnits > 100) return 'gas-medium';
    return 'gas-low';
  }

  formatNumber(value: number, decimals: number = 2): string {
    if (value === undefined || value === null) return 'N/A';
    return value.toFixed(decimals);
  }

  formatTimestamp(ts: number): string {
    if (!ts) return 'N/A';
    return new Date(ts).toLocaleString();
  }

  goBack(): void {
    if (this.runId) {
      this.router.navigate(['/dr/runs', this.runId]);
    } else {
      this.router.navigate(['/dr/runs']);
    }
  }
}
