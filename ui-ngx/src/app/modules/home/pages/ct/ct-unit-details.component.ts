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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { CTJobService } from '@core/http/ct/ct-job.service';
import { CTUnit, UnitStatus } from '@shared/models/ct/ct-unit.model';
import { CTJob } from '@shared/models/ct/ct-job.model';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-ct-unit-details',
  templateUrl: './ct-unit-details.component.html',
  styleUrls: ['./ct-unit-details.component.scss']
})
export class CTUnitDetailsComponent implements OnInit {

  unit: CTUnit | null = null;
  recentJobs: CTJob[] = [];
  isLoading = false;
  error: string | null = null;

  UnitStatus = UnitStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private unitService: CTUnitService,
    private jobService: CTJobService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    const unitId = this.route.snapshot.paramMap.get('id');
    if (unitId) {
      this.loadUnitDetails(unitId);
      this.loadRecentJobs(unitId);
    }
  }

  loadUnitDetails(unitId: string) {
    this.isLoading = true;
    this.error = null;

    this.unitService.getUnit(unitId).subscribe({
      next: (unit) => {
        this.unit = unit;
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load unit details';
        this.isLoading = false;
        console.error('Error loading unit:', error);
      }
    });
  }

  loadRecentJobs(unitId: string) {
    this.jobService.getJobsByUnit(unitId).subscribe({
      next: (jobs) => {
        this.recentJobs = jobs.slice(0, 5);
      },
      error: (error) => {
        console.error('Error loading recent jobs:', error);
      }
    });
  }

  editUnit() {
    console.log('Edit unit:', this.unit);
  }

  assignReel() {
    console.log('Assign reel to unit:', this.unit);
  }

  detachReel() {
    if (this.unit && confirm(`Detach reel from unit ${this.unit.unitCode}?`)) {
      this.unitService.detachReel(this.unit.id.id).subscribe({
        next: () => {
          this.loadUnitDetails(this.unit!.id.id);
        },
        error: (error) => {
          console.error('Error detaching reel:', error);
        }
      });
    }
  }

  viewJob(job: CTJob) {
    this.router.navigate(['/ct/jobs', job.id.id]);
  }

  goBack() {
    this.router.navigate(['/ct/units']);
  }

  getStatusColor(status: UnitStatus): string {
    switch (status) {
      case UnitStatus.ACTIVE:
        return '#4caf50';
      case UnitStatus.STANDBY:
        return '#2196f3';
      case UnitStatus.MAINTENANCE:
        return '#ff9800';
      case UnitStatus.OUT_OF_SERVICE:
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  getStatusLabel(status: UnitStatus): string {
    return status.replace(/_/g, ' ');
  }
}
