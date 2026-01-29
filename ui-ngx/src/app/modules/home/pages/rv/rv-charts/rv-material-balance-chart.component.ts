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

interface HavlenaOdehPoint {
  xAxis: number;
  yAxis: number;
  pressure: number;
  date?: string;
}

interface DriveIndexes {
  primaryMechanism: string;
  depletionDriveIndex: number;
  gasCapDriveIndex: number;
  waterDriveIndex: number;
  compactionDriveIndex: number;
}

@Component({
  selector: 'tb-rv-material-balance-chart',
  template: `
    <mat-card class="mb-chart-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>analytics</mat-icon>
          Análisis Havlena-Odeh
        </mat-card-title>
        <mat-card-subtitle *ngIf="plotType">{{ getPlotTypeLabel(plotType) }}</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <!-- Loading State -->
        <div *ngIf="isLoading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Cargando datos...</p>
        </div>

        <!-- No Data State -->
        <div *ngIf="!isLoading && dataPoints.length === 0" class="no-data">
          <mat-icon>show_chart</mat-icon>
          <p>No hay datos de balance de materiales disponibles</p>
        </div>

        <!-- Chart Area -->
        <div *ngIf="!isLoading && dataPoints.length > 0" class="chart-area">
          <!-- Havlena-Odeh Plot -->
          <div class="plot-container">
            <h4>Gráfico {{ getPlotTypeLabel(plotType) }}</h4>
            <div class="axis-labels">
              <span class="y-label">{{ getYAxisLabel() }}</span>
              <span class="x-label">{{ getXAxisLabel() }}</span>
            </div>
            <div class="plot-area">
              <svg viewBox="0 0 400 300" class="plot-svg">
                <!-- Grid Lines -->
                <g class="grid">
                  <line *ngFor="let i of [0,1,2,3,4]"
                        [attr.x1]="50" [attr.x2]="380"
                        [attr.y1]="50 + i*50" [attr.y2]="50 + i*50"
                        stroke="#e0e0e0" stroke-width="1"/>
                  <line *ngFor="let i of [0,1,2,3,4,5,6]"
                        [attr.x1]="50 + i*55" [attr.x2]="50 + i*55"
                        [attr.y1]="50" [attr.y2]="250"
                        stroke="#e0e0e0" stroke-width="1"/>
                </g>

                <!-- Axes -->
                <line x1="50" y1="250" x2="380" y2="250" stroke="#333" stroke-width="2"/>
                <line x1="50" y1="50" x2="50" y2="250" stroke="#333" stroke-width="2"/>

                <!-- Data Points -->
                <g class="data-points">
                  <circle *ngFor="let point of normalizedPoints; let i = index"
                          [attr.cx]="point.x" [attr.cy]="point.y"
                          r="6" fill="#1976d2" stroke="#fff" stroke-width="2"
                          [matTooltip]="'P=' + dataPoints[i]?.pressure + ' psi'"/>
                </g>

                <!-- Regression Line -->
                <line *ngIf="regressionLine"
                      [attr.x1]="regressionLine.x1" [attr.y1]="regressionLine.y1"
                      [attr.x2]="regressionLine.x2" [attr.y2]="regressionLine.y2"
                      stroke="#f44336" stroke-width="2" stroke-dasharray="5,5"/>
              </svg>
            </div>
          </div>

          <!-- Results Panel -->
          <div class="results-panel">
            <h4>Resultados del Análisis</h4>

            <div class="result-item highlight">
              <span class="label">OOIP Calculado:</span>
              <span class="value">{{ calculatedOOIP | number:'1.2-2' }} MMSTB</span>
            </div>

            <div class="result-item">
              <span class="label">Coeficiente R²:</span>
              <span class="value" [ngClass]="{'good': r2 >= 0.9, 'fair': r2 >= 0.7 && r2 < 0.9, 'poor': r2 < 0.7}">
                {{ r2 | number:'1.4-4' }}
              </span>
            </div>

            <div class="result-item">
              <span class="label">Pendiente:</span>
              <span class="value">{{ regressionSlope | number:'1.4-4' }}</span>
            </div>

            <div class="result-item">
              <span class="label">Intercepto:</span>
              <span class="value">{{ regressionIntercept | number:'1.4-4' }}</span>
            </div>
          </div>
        </div>

        <!-- Drive Mechanisms -->
        <div *ngIf="!isLoading && driveIndexes" class="drive-mechanisms">
          <h4>
            <mat-icon>speed</mat-icon>
            Mecanismos de Empuje
          </h4>

          <div class="primary-drive">
            <mat-chip color="primary" selected>
              {{ getDriveMechanismLabel(driveIndexes.primaryMechanism) }}
            </mat-chip>
          </div>

          <div class="drive-bars">
            <div class="drive-bar">
              <span class="drive-label">DDI (Depleción)</span>
              <mat-progress-bar mode="determinate" [value]="driveIndexes.depletionDriveIndex * 100" color="primary"></mat-progress-bar>
              <span class="drive-value">{{ driveIndexes.depletionDriveIndex * 100 | number:'1.1-1' }}%</span>
            </div>

            <div class="drive-bar">
              <span class="drive-label">SDI (Capa de Gas)</span>
              <mat-progress-bar mode="determinate" [value]="driveIndexes.gasCapDriveIndex * 100" color="accent"></mat-progress-bar>
              <span class="drive-value">{{ driveIndexes.gasCapDriveIndex * 100 | number:'1.1-1' }}%</span>
            </div>

            <div class="drive-bar">
              <span class="drive-label">WDI (Agua)</span>
              <mat-progress-bar mode="determinate" [value]="driveIndexes.waterDriveIndex * 100" color="primary"></mat-progress-bar>
              <span class="drive-value">{{ driveIndexes.waterDriveIndex * 100 | number:'1.1-1' }}%</span>
            </div>

            <div class="drive-bar">
              <span class="drive-label">CDI (Compactación)</span>
              <mat-progress-bar mode="determinate" [value]="driveIndexes.compactionDriveIndex * 100" color="warn"></mat-progress-bar>
              <span class="drive-value">{{ driveIndexes.compactionDriveIndex * 100 | number:'1.1-1' }}%</span>
            </div>
          </div>
        </div>

        <!-- Data Table -->
        <mat-expansion-panel *ngIf="dataPoints.length > 0" class="data-table-panel">
          <mat-expansion-panel-header>
            <mat-panel-title>
              <mat-icon>table_chart</mat-icon>
              Datos de Entrada ({{ dataPoints.length }} puntos)
            </mat-panel-title>
          </mat-expansion-panel-header>

          <table class="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>P (psi)</th>
                <th>{{ getXAxisLabel() }}</th>
                <th>{{ getYAxisLabel() }}</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let point of dataPoints">
                <td>{{ point.date || '-' }}</td>
                <td>{{ point.pressure | number:'1.0-0' }}</td>
                <td>{{ point.xAxis | number:'1.6-6' }}</td>
                <td>{{ point.yAxis | number:'1.2-2' }}</td>
              </tr>
            </tbody>
          </table>
        </mat-expansion-panel>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .mb-chart-card {
      margin: 16px 0;
    }

    mat-card-header mat-icon {
      margin-right: 8px;
      vertical-align: middle;
    }

    .loading-container, .no-data {
      text-align: center;
      padding: 48px;
      color: #666;
    }

    .no-data mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #ccc;
    }

    .chart-area {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 24px;
      margin-top: 16px;
    }

    .plot-container {
      background: #fafafa;
      border-radius: 8px;
      padding: 16px;
    }

    .plot-container h4 {
      margin: 0 0 16px 0;
      color: #333;
    }

    .plot-area {
      position: relative;
    }

    .plot-svg {
      width: 100%;
      height: auto;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
    }

    .axis-labels {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #666;
      margin-bottom: 8px;
    }

    .results-panel {
      background: #f5f5f5;
      border-radius: 8px;
      padding: 16px;
    }

    .results-panel h4 {
      margin: 0 0 16px 0;
      color: #333;
    }

    .result-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #e0e0e0;
    }

    .result-item:last-child {
      border-bottom: none;
    }

    .result-item.highlight {
      background: #e3f2fd;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 8px;
    }

    .result-item .label {
      font-weight: 500;
      color: #666;
    }

    .result-item .value {
      font-weight: bold;
      color: #333;
    }

    .result-item .value.good { color: #4caf50; }
    .result-item .value.fair { color: #ff9800; }
    .result-item .value.poor { color: #f44336; }

    .drive-mechanisms {
      margin-top: 24px;
      padding: 16px;
      background: #fafafa;
      border-radius: 8px;
    }

    .drive-mechanisms h4 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 16px 0;
    }

    .primary-drive {
      margin-bottom: 16px;
    }

    .drive-bars {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .drive-bar {
      display: grid;
      grid-template-columns: 140px 1fr 60px;
      align-items: center;
      gap: 12px;
    }

    .drive-label {
      font-size: 13px;
      color: #666;
    }

    .drive-value {
      font-weight: bold;
      text-align: right;
    }

    .data-table-panel {
      margin-top: 16px;
    }

    .data-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
    }

    .data-table th, .data-table td {
      padding: 8px 12px;
      text-align: right;
      border-bottom: 1px solid #e0e0e0;
    }

    .data-table th {
      background: #f5f5f5;
      font-weight: 600;
      text-align: center;
    }

    .data-table td:first-child, .data-table th:first-child {
      text-align: left;
    }

    @media (max-width: 768px) {
      .chart-area {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class RvMaterialBalanceChartComponent implements OnInit, OnChanges {

  @Input() studyId: string;

  isLoading = false;

  // Plot data
  plotType: string = '';
  dataPoints: HavlenaOdehPoint[] = [];
  normalizedPoints: { x: number; y: number }[] = [];

  // Regression results
  calculatedOOIP: number = 0;
  r2: number = 0;
  regressionSlope: number = 0;
  regressionIntercept: number = 0;
  regressionLine: { x1: number; y1: number; x2: number; y2: number } | null = null;

  // Drive mechanisms
  driveIndexes: DriveIndexes | null = null;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.studyId && !changes.studyId.firstChange) {
      this.loadData();
    }
  }

  loadData(): void {
    if (!this.studyId) return;

    this.isLoading = true;

    // Load Havlena-Odeh plot data
    this.rvService.getHavlenaOdehPlotData(this.studyId).subscribe({
      next: (data) => {
        this.plotType = data.plotType || 'F_vs_Eo';
        this.dataPoints = data.dataPoints || [];
        this.calculatedOOIP = data.calculatedOOIP || 0;
        this.r2 = data.r2 || 0;
        this.regressionSlope = data.regressionSlope || 0;
        this.regressionIntercept = data.regressionIntercept || 0;

        this.normalizePoints();
        this.calculateRegressionLine();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });

    // Load drive mechanisms
    this.rvService.getDriveMechanisms(this.studyId).subscribe({
      next: (data) => {
        this.driveIndexes = data;
      }
    });
  }

  normalizePoints(): void {
    if (this.dataPoints.length === 0) {
      this.normalizedPoints = [];
      return;
    }

    const xValues = this.dataPoints.map(p => p.xAxis);
    const yValues = this.dataPoints.map(p => p.yAxis);

    const xMin = Math.min(...xValues);
    const xMax = Math.max(...xValues);
    const yMin = Math.min(...yValues);
    const yMax = Math.max(...yValues);

    const xRange = xMax - xMin || 1;
    const yRange = yMax - yMin || 1;

    // SVG plot area: x from 50 to 380, y from 250 to 50 (inverted)
    this.normalizedPoints = this.dataPoints.map(point => ({
      x: 50 + ((point.xAxis - xMin) / xRange) * 330,
      y: 250 - ((point.yAxis - yMin) / yRange) * 200
    }));
  }

  calculateRegressionLine(): void {
    if (this.dataPoints.length < 2) {
      this.regressionLine = null;
      return;
    }

    const xValues = this.dataPoints.map(p => p.xAxis);
    const xMin = Math.min(...xValues);
    const xMax = Math.max(...xValues);

    const yMin = this.regressionSlope * xMin + this.regressionIntercept;
    const yMax = this.regressionSlope * xMax + this.regressionIntercept;

    const yValues = this.dataPoints.map(p => p.yAxis);
    const dataYMin = Math.min(...yValues);
    const dataYMax = Math.max(...yValues);
    const xRange = xMax - xMin || 1;
    const yRange = dataYMax - dataYMin || 1;

    this.regressionLine = {
      x1: 50,
      y1: 250 - ((yMin - dataYMin) / yRange) * 200,
      x2: 380,
      y2: 250 - ((yMax - dataYMin) / yRange) * 200
    };
  }

  getPlotTypeLabel(plotType: string): string {
    switch (plotType) {
      case 'F_vs_Eo': return 'F vs Eo';
      case 'F_vs_EoEg': return 'F vs (Eo + mEg)';
      case 'F_Eo_vs_EwEf': return 'F/Eo vs Efw/Eo';
      default: return plotType;
    }
  }

  getXAxisLabel(): string {
    switch (this.plotType) {
      case 'F_vs_Eo': return 'Eo (bbl/STB)';
      case 'F_vs_EoEg': return 'Eo + mEg (bbl/STB)';
      case 'F_Eo_vs_EwEf': return 'Efw/Eo';
      default: return 'X';
    }
  }

  getYAxisLabel(): string {
    switch (this.plotType) {
      case 'F_vs_Eo': return 'F (MMbbl)';
      case 'F_vs_EoEg': return 'F (MMbbl)';
      case 'F_Eo_vs_EwEf': return 'F/Eo (MMSTB)';
      default: return 'Y';
    }
  }

  getDriveMechanismLabel(mechanism: string): string {
    switch (mechanism) {
      case 'SOLUTION_GAS': return 'Gas en Solución';
      case 'GAS_CAP': return 'Capa de Gas';
      case 'WATER_DRIVE': return 'Empuje de Agua';
      case 'COMPACTION': return 'Compactación';
      case 'GRAVITY': return 'Gravedad';
      case 'COMBINATION': return 'Combinado';
      default: return mechanism;
    }
  }
}
