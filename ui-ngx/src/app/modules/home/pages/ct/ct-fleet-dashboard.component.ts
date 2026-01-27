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
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { CTUnit, UnitStatus } from '@shared/models/ct/ct-unit.model';
import { CTReel } from '@shared/models/ct/ct-reel.model';
import { PageLink } from '@shared/models/page/page-link';

interface FleetMetrics {
  totalUnits: number;
  operationalUnits: number;
  maintenanceUnits: number;
  utilizationRate: number;
}

@Component({
  selector: 'tb-ct-fleet-dashboard',
  templateUrl: './ct-fleet-dashboard.component.html',
  styleUrls: ['./ct-fleet-dashboard.component.scss']
})
export class CTFleetDashboardComponent implements OnInit {

  metrics: FleetMetrics = {
    totalUnits: 0,
    operationalUnits: 0,
    maintenanceUnits: 0,
    utilizationRate: 0
  };

  units: CTUnit[] = [];
  reels: CTReel[] = [];
  loading = true;

  displayedColumns = ['unitCode', 'unitName', 'status', 'location', 'hours', 'reel', 'actions'];

  constructor(
    private unitService: CTUnitService,
    private reelService: CTReelService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadFleetData();
  }

  loadFleetData() {
    this.loading = true;
    const pageLink = new PageLink(50, 0);
    const tenantId = this.getCurrentTenantId();

    this.unitService.getUnits(pageLink, tenantId).subscribe({
      next: (response) => {
        this.units = response.data;
        this.metrics.totalUnits = response.totalElements;
        this.calculateMetrics();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading fleet data:', error);
        this.loading = false;
      }
    });

    this.reelService.getReels(pageLink, tenantId).subscribe({
      next: (response) => {
        this.reels = response.data;
      }
    });
  }

  private calculateMetrics() {
    this.metrics.operationalUnits = this.units.filter(u => 
      u.operationalStatus === UnitStatus.ACTIVE
    ).length;
    
    this.metrics.maintenanceUnits = this.units.filter(u => 
      u.operationalStatus === UnitStatus.MAINTENANCE
    ).length;

    this.metrics.utilizationRate = this.metrics.totalUnits > 0
      ? (this.metrics.operationalUnits / this.metrics.totalUnits) * 100
      : 0;
  }

  private getCurrentTenantId(): string {
    return 'current-tenant';
  }

  viewUnitDetails(unit: CTUnit) {
    this.router.navigate(['/ct/units', unit.id.id]);
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'ACTIVE': 'primary',
      'MAINTENANCE': 'warn',
      'OUT_OF_SERVICE': 'basic',
      'STANDBY': 'accent'
    };
    return colors[status] || 'basic';
  }

  getUtilizationColor(rate: number): string {
    if (rate >= 80) return 'primary';
    if (rate >= 50) return 'accent';
    return 'warn';
  }
}
