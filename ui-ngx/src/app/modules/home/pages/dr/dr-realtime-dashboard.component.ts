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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DRRigService } from '@core/http/dr/dr-rig.service';
import { DRRig, RigStatus } from '@shared/models/dr/dr-rig.model';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-dr-realtime-dashboard',
  templateUrl: './dr-realtime-dashboard.component.html',
  styleUrls: ['./dr-realtime-dashboard.component.scss']
})
export class DrRealtimeDashboardComponent implements OnInit, OnDestroy {

  activeRigs: DRRig[] = [];
  isLoading = true;
  selectedRig: DRRig | null = null;

  RigStatus = RigStatus;

  private destroy$ = new Subject<void>();

  constructor(
    private store: Store<AppState>,
    private rigService: DRRigService
  ) {}

  ngOnInit() {
    this.loadActiveRigs();

    // Refresh every 30 seconds
    interval(30000).pipe(
      takeUntil(this.destroy$),
      switchMap(() => this.rigService.getRigsByStatus(this.getCurrentTenantId(), RigStatus.DRILLING))
    ).subscribe(rigs => {
      this.activeRigs = rigs;
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadActiveRigs() {
    this.isLoading = true;
    this.rigService.getRigsByStatus(this.getCurrentTenantId(), RigStatus.DRILLING)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rigs) => {
          this.activeRigs = rigs;
          if (rigs.length > 0 && !this.selectedRig) {
            this.selectedRig = rigs[0];
          }
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading active rigs:', error);
          this.isLoading = false;
        }
      });
  }

  selectRig(rig: DRRig) {
    this.selectedRig = rig;
  }

  getStatusColor(status: RigStatus): string {
    switch (status) {
      case RigStatus.DRILLING: return '#4caf50';
      case RigStatus.CIRCULATING: return '#ff9800';
      case RigStatus.TRIPPING: return '#2196f3';
      default: return '#9e9e9e';
    }
  }

  private getCurrentTenantId(): string {
    const authUser = getCurrentAuthUser(this.store);
    return authUser?.tenantId || '';
  }
}
