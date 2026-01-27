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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CTReelService } from '@core/http/ct/ct-reel.service';

export interface CTFatigueHistoryDialogData {
  reelId: string;
  reelCode: string;
}

export interface FatigueLogEntry {
  timestamp: number;
  fatiguePercent: number;
  cyclesAdded: number;
  stressType: string;
  maxStressPsi: number;
  calculationMethod: string;
}

@Component({
  selector: 'tb-ct-fatigue-history-dialog',
  templateUrl: './ct-fatigue-history-dialog.component.html',
  styleUrls: ['./ct-fatigue-history-dialog.component.scss']
})
export class CTFatigueHistoryDialogComponent implements OnInit {

  fatigueHistory: FatigueLogEntry[] = [];
  isLoading = false;
  error: string | null = null;

  displayedColumns: string[] = [
    'timestamp',
    'fatiguePercent',
    'cyclesAdded',
    'stressType',
    'maxStressPsi',
    'calculationMethod'
  ];

  // Chart data (placeholder for future chart implementation)
  chartData: any = null;

  constructor(
    public dialogRef: MatDialogRef<CTFatigueHistoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CTFatigueHistoryDialogData,
    private reelService: CTReelService
  ) {}

  ngOnInit() {
    this.loadFatigueHistory();
  }

  loadFatigueHistory() {
    this.isLoading = true;
    this.error = null;

    // TODO: Implement actual API call when backend endpoint is ready
    // For now, using mock data
    setTimeout(() => {
      this.fatigueHistory = this.generateMockData();
      this.prepareChartData();
      this.isLoading = false;
    }, 500);

    /*
    // Actual implementation when API is ready:
    this.reelService.getFatigueHistory(this.data.reelId).subscribe({
      next: (history) => {
        this.fatigueHistory = history;
        this.prepareChartData();
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load fatigue history';
        this.isLoading = false;
      }
    });
    */
  }

  prepareChartData() {
    // Prepare data for chart visualization
    this.chartData = {
      labels: this.fatigueHistory.map(entry => new Date(entry.timestamp)),
      datasets: [{
        label: 'Fatigue %',
        data: this.fatigueHistory.map(entry => entry.fatiguePercent)
      }]
    };
  }

  generateMockData(): FatigueLogEntry[] {
    const now = Date.now();
    const mockData: FatigueLogEntry[] = [];
    
    for (let i = 0; i < 20; i++) {
      mockData.push({
        timestamp: now - (i * 86400000), // Days ago
        fatiguePercent: 45 + (i * 2.5),
        cyclesAdded: Math.floor(Math.random() * 10) + 1,
        stressType: i % 3 === 0 ? 'Tension' : i % 3 === 1 ? 'Compression' : 'Bending',
        maxStressPsi: 25000 + Math.floor(Math.random() * 10000),
        calculationMethod: 'Palmgren-Miner'
      });
    }
    
    return mockData.reverse();
  }

  getFatigueColor(fatigue: number): string {
    if (fatigue >= 95) return '#f44336';
    if (fatigue >= 80) return '#ff9800';
    if (fatigue >= 60) return '#ffc107';
    return '#4caf50';
  }

  get averageCyclesPerEntry(): number {
    if (this.fatigueHistory.length === 0) return 0;
    const total = this.fatigueHistory.reduce((sum, e) => sum + e.cyclesAdded, 0);
    return total / this.fatigueHistory.length;
  }

  close() {
    this.dialogRef.close();
  }
}
