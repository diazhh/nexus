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
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { MatDialog } from '@angular/material/dialog';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { RvService } from '@core/http/rv/rv.service';
import { PageLink } from '@shared/models/page/page-link';
import { RvReservoirDialogComponent } from '../rv-reservoir-list/rv-reservoir-dialog.component';

@Component({
  selector: 'tb-rv-reservoir-details',
  templateUrl: './rv-reservoir-details.component.html',
  styleUrls: ['./rv-reservoir-details.component.scss']
})
export class RvReservoirDetailsComponent implements OnInit {

  tenantId: string;
  reservoirId: string;
  reservoir: any;
  zones: any[] = [];
  wells: any[] = [];
  pvtStudies: any[] = [];
  materialBalance: any[] = [];

  isLoading = true;
  selectedTabIndex = 0;

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.reservoirId = this.route.snapshot.paramMap.get('reservoirId');

    if (this.reservoirId) {
      this.loadReservoirData();
    }
  }

  loadReservoirData(): void {
    this.isLoading = true;

    this.rvService.getReservoir(this.reservoirId).subscribe({
      next: (reservoir) => {
        this.reservoir = reservoir;
        this.loadRelatedData();
      },
      error: () => {
        this.isLoading = false;
        this.router.navigate(['/rv/reservoirs']);
      }
    });
  }

  loadRelatedData(): void {
    // Load zones
    this.rvService.getZonesByReservoir(this.tenantId, this.reservoirId).subscribe({
      next: (zones) => this.zones = zones,
      error: (err) => console.error('Error loading zones:', err)
    });

    // Load wells
    this.rvService.getWellsByReservoir(this.tenantId, this.reservoirId).subscribe({
      next: (wells) => this.wells = wells,
      error: (err) => console.error('Error loading wells:', err)
    });

    // Load PVT studies
    this.rvService.getPvtStudies(this.tenantId, new PageLink(100, 0)).subscribe({
      next: (pageData) => {
        this.pvtStudies = pageData.data.filter((pvt: any) => pvt.reservoirAssetId === this.reservoirId);
      },
      error: (err) => console.error('Error loading PVT studies:', err)
    });

    // Load material balance
    this.rvService.getMaterialBalanceStudiesByReservoir(this.tenantId, this.reservoirId).subscribe({
      next: (studies) => {
        this.materialBalance = studies;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading material balance:', err);
        this.isLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/rv/reservoirs']);
  }

  editReservoir(): void {
    const dialogRef = this.dialog.open(RvReservoirDialogComponent, {
      width: '90vw',
      maxWidth: '800px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, reservoir: this.reservoir }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadReservoirData();
      }
    });
  }

  formatNumber(value: number, decimals: number = 2): string {
    if (!value && value !== 0) return 'N/A';
    return value.toLocaleString('en-US', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    });
  }

  formatPercentage(value: number): string {
    if (!value && value !== 0) return 'N/A';
    return (value * 100).toFixed(2) + '%';
  }
}
