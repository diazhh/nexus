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
  selector: 'tb-rv-ipr-chart',
  template: `
    <div class="chart-container" *ngIf="curveData.length > 0">
      <div class="chart-placeholder">
        <mat-icon>show_chart</mat-icon>
        <p>Curva IPR - {{ curveData.length }} puntos</p>
        <table class="mini-table">
          <tr><th>Pwf (psi)</th><th>Rate (bopd)</th></tr>
          <tr *ngFor="let point of curveData.slice(0, 5)">
            <td>{{ point.pwfPsi | number:'1.0-0' }}</td>
            <td>{{ point.rateBopd | number:'1.0-0' }}</td>
          </tr>
          <tr *ngIf="curveData.length > 5"><td colspan="2">...</td></tr>
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
    .chart-placeholder mat-icon { font-size: 48px; width: 48px; height: 48px; color: #1976d2; }
    .mini-table { margin: 16px auto; border-collapse: collapse; font-size: 12px; }
    .mini-table th, .mini-table td { padding: 4px 12px; border: 1px solid #ddd; }
    .mini-table th { background: #e0e0e0; }
    .chart-loading { text-align: center; padding: 24px; }
  `]
})
export class RvIprChartComponent implements OnInit, OnChanges {

  @Input() iprModelId: string;

  curveData: { pwfPsi: number; rateBopd: number }[] = [];
  isLoading = false;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadCurve();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.iprModelId && !changes.iprModelId.firstChange) {
      this.loadCurve();
    }
  }

  loadCurve(): void {
    if (!this.iprModelId) return;

    this.isLoading = true;
    this.rvService.getIprCurve(this.iprModelId, 20).subscribe({
      next: (data) => {
        this.curveData = data;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }
}
