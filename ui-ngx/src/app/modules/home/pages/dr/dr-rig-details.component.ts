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
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DRRigService } from '@core/http/dr/dr-rig.service';
import { DRRunService } from '@core/http/dr/dr-run.service';
import { DRKpiService } from '@core/http/dr/dr-kpi.service';
import { DRRig, RigStatus, RigType } from '@shared/models/dr/dr-rig.model';
import { DRRun } from '@shared/models/dr/dr-run.model';
import { RigKpi } from '@shared/models/dr/dr-kpi.model';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-dr-rig-details',
  templateUrl: './dr-rig-details.component.html',
  styleUrls: ['./dr-rig-details.component.scss']
})
export class DrRigDetailsComponent implements OnInit, OnDestroy {

  rig: DRRig;
  rigKpis: RigKpi;
  recentRuns: DRRun[] = [];
  isLoading = true;
  selectedTab = 0;

  RigStatus = RigStatus;
  RigType = RigType;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private rigService: DRRigService,
    private runService: DRRunService,
    private kpiService: DRKpiService
  ) {}

  ngOnInit() {
    const rigId = this.route.snapshot.params.id;
    if (rigId) {
      this.loadRigDetails(rigId);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadRigDetails(rigId: string) {
    this.isLoading = true;

    this.rigService.getRig(rigId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rig) => {
          this.rig = rig;
          this.loadRigKpis(rigId);
          this.loadRecentRuns(rigId);
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading rig:', error);
          this.isLoading = false;
        }
      });
  }

  loadRigKpis(rigId: string) {
    this.kpiService.getRigKpis(rigId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (kpis) => {
          this.rigKpis = kpis;
        },
        error: (error) => {
          console.error('Error loading rig KPIs:', error);
        }
      });
  }

  loadRecentRuns(rigId: string) {
    const pageLink = new PageLink(5, 0);
    this.runService.getRunsByRig(rigId, pageLink)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageData) => {
          this.recentRuns = pageData.data;
        },
        error: (error) => {
          console.error('Error loading recent runs:', error);
        }
      });
  }

  goBack() {
    this.router.navigate(['/dr/rigs']);
  }

  editRig() {
    // TODO: Open edit dialog
    console.log('Edit rig:', this.rig);
  }

  assignWell() {
    // TODO: Open assign well dialog
    console.log('Assign well to rig:', this.rig);
  }

  releaseWell() {
    if (confirm(`Release rig ${this.rig.rigCode} from current well?`)) {
      this.rigService.releaseWell(this.rig.id.id).subscribe({
        next: () => {
          this.loadRigDetails(this.rig.id.id);
        },
        error: (error) => {
          console.error('Error releasing well:', error);
        }
      });
    }
  }

  updateStatus(status: RigStatus) {
    this.rigService.updateStatus(this.rig.id.id, status).subscribe({
      next: () => {
        this.loadRigDetails(this.rig.id.id);
      },
      error: (error) => {
        console.error('Error updating status:', error);
      }
    });
  }

  recordBopTest() {
    const testDate = Date.now();
    const testPressure = 5000; // TODO: Get from dialog
    this.rigService.recordBopTest(this.rig.id.id, testDate, testPressure).subscribe({
      next: () => {
        this.loadRigDetails(this.rig.id.id);
      },
      error: (error) => {
        console.error('Error recording BOP test:', error);
      }
    });
  }

  recordInspection() {
    const inspectionDate = Date.now();
    this.rigService.recordInspection(this.rig.id.id, inspectionDate).subscribe({
      next: () => {
        this.loadRigDetails(this.rig.id.id);
      },
      error: (error) => {
        console.error('Error recording inspection:', error);
      }
    });
  }

  viewRun(run: DRRun) {
    this.router.navigate(['/dr/runs', run.id.id]);
  }

  getStatusColor(status: RigStatus): string {
    switch (status) {
      case RigStatus.DRILLING:
        return '#4caf50';
      case RigStatus.STANDBY:
        return '#2196f3';
      case RigStatus.TRIPPING:
      case RigStatus.CIRCULATING:
        return '#ff9800';
      case RigStatus.MAINTENANCE:
        return '#ff5722';
      case RigStatus.RIGGING_UP:
      case RigStatus.RIGGING_DOWN:
        return '#9c27b0';
      case RigStatus.OUT_OF_SERVICE:
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  getStatusLabel(status: RigStatus): string {
    return status.replace(/_/g, ' ');
  }

  getRigTypeLabel(type: RigType): string {
    return type.replace(/_/g, ' ');
  }
}
