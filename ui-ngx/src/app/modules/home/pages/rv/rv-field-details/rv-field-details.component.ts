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
import { RvFieldDialogComponent } from '../rv-field-list/rv-field-dialog.component';

@Component({
  selector: 'tb-rv-field-details',
  templateUrl: './rv-field-details.component.html',
  styleUrls: ['./rv-field-details.component.scss']
})
export class RvFieldDetailsComponent implements OnInit {

  tenantId: string;
  fieldId: string;
  field: any;
  reservoirs: any[] = [];
  wells: any[] = [];

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
    this.fieldId = this.route.snapshot.paramMap.get('fieldId');

    if (this.fieldId) {
      this.loadFieldData();
    }
  }

  loadFieldData(): void {
    this.isLoading = true;

    this.rvService.getField(this.fieldId).subscribe({
      next: (field) => {
        this.field = field;
        this.loadRelatedData();
      },
      error: () => {
        this.isLoading = false;
        this.router.navigate(['/rv/fields']);
      }
    });
  }

  loadRelatedData(): void {
    // Load reservoirs
    this.rvService.getReservoirsByField(this.tenantId, this.fieldId).subscribe({
      next: (reservoirs) => this.reservoirs = reservoirs,
      error: (err) => console.error('Error loading reservoirs:', err)
    });

    // Load wells - using getWells with pagination and filtering by field
    this.rvService.getWells(this.tenantId, new PageLink(1000, 0)).subscribe({
      next: (pageData) => {
        // Filter wells that belong to reservoirs in this field
        const fieldReservoirIds = this.reservoirs.map(r => r.assetId);
        this.wells = pageData.data.filter((w: any) => fieldReservoirIds.includes(w.reservoirAssetId));
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading wells:', err);
        this.isLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/rv/fields']);
  }

  editField(): void {
    const dialogRef = this.dialog.open(RvFieldDialogComponent, {
      width: '90vw',
      maxWidth: '700px',
      maxHeight: '90vh',
      data: { tenantId: this.tenantId, field: this.field }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadFieldData();
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

  getTotalOOIP(): number {
    return this.reservoirs.reduce((sum, r) => sum + (r.ooipEstimateMmbbl || 0), 0);
  }

  getTotalWells(): number {
    return this.wells.length;
  }

  getProducingWells(): number {
    return this.wells.filter(w => w.status === 'PRODUCING').length;
  }
}
