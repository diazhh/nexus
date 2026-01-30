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
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DRRigService } from '@core/http/dr/dr-rig.service';
import { DRKpiService } from '@core/http/dr/dr-kpi.service';
import { DRRig, RigStatus } from '@shared/models/dr/dr-rig.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-dr-fleet-dashboard',
  templateUrl: './dr-fleet-dashboard.component.html',
  styleUrls: ['./dr-fleet-dashboard.component.scss']
})
export class DrFleetDashboardComponent implements OnInit, OnDestroy {

  rigs: DRRig[] = [];
  isLoading = true;

  // Fleet statistics
  totalRigs = 0;
  drillingRigs = 0;
  standbyRigs = 0;
  maintenanceRigs = 0;
  totalFootage = 0;
  avgUtilization = 0;

  RigStatus = RigStatus;

  private destroy$ = new Subject<void>();

  constructor(
    private store: Store<AppState>,
    private rigService: DRRigService,
    private kpiService: DRKpiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadFleetData();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadFleetData() {
    this.isLoading = true;
    const tenantId = this.getCurrentTenantId();
    const pageLink = new PageLink(100, 0);

    this.rigService.getRigs(pageLink, tenantId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        this.rigs = pageData.data;
        this.calculateStatistics();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading fleet data:', error);
        this.isLoading = false;
      }
    });
  }

  calculateStatistics() {
    this.totalRigs = this.rigs.length;
    this.drillingRigs = this.rigs.filter(r => r.operationalStatus === RigStatus.DRILLING).length;
    this.standbyRigs = this.rigs.filter(r => r.operationalStatus === RigStatus.STANDBY).length;
    this.maintenanceRigs = this.rigs.filter(r => r.operationalStatus === RigStatus.MAINTENANCE).length;
    this.totalFootage = this.rigs.reduce((sum, r) => sum + (r.totalFootageDrilledFt || 0), 0);
    this.avgUtilization = this.totalRigs > 0 ? (this.drillingRigs / this.totalRigs) * 100 : 0;
  }

  viewRig(rig: DRRig) {
    this.router.navigate(['/dr/rigs', rig.id.id]);
  }

  getStatusColor(status: RigStatus): string {
    switch (status) {
      case RigStatus.DRILLING: return '#4caf50';
      case RigStatus.STANDBY: return '#2196f3';
      case RigStatus.MAINTENANCE: return '#ff9800';
      case RigStatus.OUT_OF_SERVICE: return '#f44336';
      default: return '#9e9e9e';
    }
  }

  private getCurrentTenantId(): string {
    const authUser = getCurrentAuthUser(this.store);
    return authUser?.tenantId || '';
  }
}
