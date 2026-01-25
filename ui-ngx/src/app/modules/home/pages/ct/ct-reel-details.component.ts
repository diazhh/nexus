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
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { CTReel, ReelStatus } from '@shared/models/ct/ct-reel.model';
import { MatDialog } from '@angular/material/dialog';
import { CTFatigueHistoryDialogComponent } from './ct-fatigue-history-dialog.component';

@Component({
  selector: 'tb-ct-reel-details',
  templateUrl: './ct-reel-details.component.html',
  styleUrls: ['./ct-reel-details.component.scss']
})
export class CTReelDetailsComponent implements OnInit {

  reel: CTReel | null = null;
  isLoading = false;
  error: string | null = null;

  ReelStatus = ReelStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reelService: CTReelService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    const reelId = this.route.snapshot.paramMap.get('id');
    if (reelId) {
      this.loadReelDetails(reelId);
    }
  }

  loadReelDetails(reelId: string) {
    this.isLoading = true;
    this.error = null;

    this.reelService.getReel(reelId).subscribe({
      next: (reel) => {
        this.reel = reel;
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load reel details';
        this.isLoading = false;
        console.error('Error loading reel:', error);
      }
    });
  }

  editReel() {
    console.log('Edit reel:', this.reel);
  }

  viewFatigueHistory() {
    if (this.reel) {
      this.dialog.open(CTFatigueHistoryDialogComponent, {
        width: '1000px',
        maxHeight: '90vh',
        data: {
          reelId: this.reel.id.id,
          reelCode: this.reel.reelCode
        }
      });
    }
  }

  retireReel() {
    if (this.reel && confirm(`Retire reel ${this.reel.reelCode}?`)) {
      const updatedReel = { ...this.reel, status: ReelStatus.RETIRED };
      this.reelService.updateReel(this.reel.id.id, updatedReel).subscribe({
        next: () => {
          this.loadReelDetails(this.reel!.id.id);
        },
        error: (error) => {
          console.error('Error retiring reel:', error);
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/ct/reels']);
  }

  getStatusColor(status: ReelStatus): string {
    switch (status) {
      case ReelStatus.AVAILABLE:
        return '#4caf50';
      case ReelStatus.IN_USE:
        return '#2196f3';
      case ReelStatus.MAINTENANCE:
        return '#ff9800';
      case ReelStatus.RETIRED:
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  getStatusLabel(status: ReelStatus): string {
    return status.replace(/_/g, ' ');
  }

  getFatigueColor(fatigue: number): string {
    if (fatigue >= 95) return '#f44336';
    if (fatigue >= 80) return '#ff9800';
    if (fatigue >= 60) return '#ffc107';
    return '#4caf50';
  }

  getFatigueLevel(fatigue: number): string {
    if (fatigue >= 95) return 'CRITICAL';
    if (fatigue >= 80) return 'HIGH';
    if (fatigue >= 60) return 'MEDIUM';
    return 'LOW';
  }

  getRemainingLife(fatigue: number): number {
    return Math.max(0, 100 - fatigue);
  }
}
