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
import { RvBasinDialogComponent } from '../rv-basin-list/rv-basin-dialog.component';

@Component({
  selector: 'tb-rv-basin-details',
  templateUrl: './rv-basin-details.component.html',
  styleUrls: ['./rv-basin-details.component.scss']
})
export class RvBasinDetailsComponent implements OnInit {

  tenantId: string;
  basinId: string;
  basin: any;
  fields: any[] = [];
  statistics: any = {};

  isLoading = true;

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
    this.basinId = this.route.snapshot.paramMap.get('basinId');

    if (this.basinId) {
      this.loadBasinData();
    }
  }

  loadBasinData(): void {
    this.isLoading = true;

    this.rvService.getBasin(this.basinId).subscribe({
      next: (basin) => {
        this.basin = basin;
        this.loadRelatedData();
      },
      error: () => {
        this.isLoading = false;
        this.router.navigate(['/rv/basins']);
      }
    });
  }

  loadRelatedData(): void {
    // Load fields
    this.rvService.getFieldsByBasin(this.tenantId, this.basinId).subscribe({
      next: (fields) => {
        this.fields = fields;
        this.loadStatistics();
      },
      error: (err) => {
        console.error('Error loading fields:', err);
        this.isLoading = false;
      }
    });
  }

  loadStatistics(): void {
    // Calculate statistics from loaded fields
    // For now, we'll show placeholder statistics
    this.statistics = {
      totalReservoirs: 0,
      totalWells: 0,
      producingWells: 0
    };
    this.isLoading = false;
  }

  goBack(): void {
    this.router.navigate(['/rv/basins']);
  }

  editBasin(): void {
    const dialogRef = this.dialog.open(RvBasinDialogComponent, {
      width: '90vw',
      maxWidth: '700px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, basin: this.basin }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadBasinData();
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
}
