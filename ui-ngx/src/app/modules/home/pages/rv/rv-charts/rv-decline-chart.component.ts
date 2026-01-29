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

import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { RvService } from '@core/http/rv/rv.service';

@Component({
  selector: 'tb-rv-decline-chart',
  template: `
    <div class="chart-container" *ngIf="forecastData.length > 0">
      <div class="chart-placeholder">
        <mat-icon>trending_down</mat-icon>
        <p>Pronostico de Declinacion - {{ forecastData.length }} puntos</p>
        <table class="mini-table">
          <tr><th>Mes</th><th>Rate (bopd)</th><th>Np (bbl)</th></tr>
          <tr *ngFor="let point of forecastData.slice(0, 5)">
            <td>{{ point.month }}</td>
            <td>{{ point.rateBopd | number:'1.0-0' }}</td>
            <td>{{ point.cumulativeBbl | number:'1.0-0' }}</td>
          </tr>
          <tr *ngIf="forecastData.length > 5"><td colspan="3">...</td></tr>
        </table>
      </div>
    </div>
    <div *ngIf="isLoading" class="chart-loading">
      <mat-spinner diameter="24"></mat-spinner>
    </div>
  `,
  styles: [`
    .chart-container { padding: 16px; background: #f5f5f5; border-radius: 8px; margin-top: 16px; }
    .chart-placeholder { text-align: center; }
    .chart-placeholder mat-icon { font-size: 48px; width: 48px; height: 48px; color: #f57c00; }
    .mini-table { margin: 16px auto; border-collapse: collapse; font-size: 12px; }
    .mini-table th, .mini-table td { padding: 4px 12px; border: 1px solid #ddd; }
    .mini-table th { background: #e0e0e0; }
    .chart-loading { text-align: center; padding: 24px; }
  `]
})
export class RvDeclineChartComponent implements OnInit, OnChanges {

  @Input() declineAnalysisId: string;

  forecastData: any[] = [];
  isLoading = false;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadForecast();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.declineAnalysisId && !changes.declineAnalysisId.firstChange) {
      this.loadForecast();
    }
  }

  loadForecast(): void {
    if (!this.declineAnalysisId) return;

    this.isLoading = true;
    this.rvService.getDeclineForecast(this.declineAnalysisId, 10, 12).subscribe({
      next: (data) => {
        this.forecastData = data;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }
}
