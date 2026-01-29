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
import { RvPvtStudy } from '@shared/models/rv/rv.models';

interface PvtDataPoint {
  pressure: number;
  Bo?: number;
  Rs?: number;
  Bg?: number;
  viscosity?: number;
  z?: number;
}

@Component({
  selector: 'tb-rv-pvt-chart',
  template: `
    <mat-card class="pvt-chart-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>science</mat-icon>
          Propiedades PVT
        </mat-card-title>
        <mat-card-subtitle *ngIf="pvtStudy">{{ pvtStudy.name }}</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <!-- Loading State -->
        <div *ngIf="isLoading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>

        <!-- No Data State -->
        <div *ngIf="!isLoading && !pvtStudy" class="no-data">
          <mat-icon>science</mat-icon>
          <p>No hay estudio PVT disponible</p>
        </div>

        <!-- PVT Properties Cards -->
        <div *ngIf="!isLoading && pvtStudy" class="pvt-content">
          <!-- Key Properties -->
          <div class="key-properties">
            <div class="property-card">
              <mat-icon>compress</mat-icon>
              <div class="property-info">
                <span class="property-label">Presión de Burbuja</span>
                <span class="property-value">{{ pvtStudy.bubblePointPressurePsia | number:'1.0-0' }} psia</span>
              </div>
            </div>

            <div class="property-card">
              <mat-icon>thermostat</mat-icon>
              <div class="property-info">
                <span class="property-label">Temperatura</span>
                <span class="property-value">{{ pvtStudy.reservoirTemperatureF | number:'1.0-0' }} °F</span>
              </div>
            </div>

            <div class="property-card">
              <mat-icon>opacity</mat-icon>
              <div class="property-info">
                <span class="property-label">°API</span>
                <span class="property-value">{{ pvtStudy.apiGravity | number:'1.1-1' }}</span>
              </div>
            </div>

            <div class="property-card">
              <mat-icon>air</mat-icon>
              <div class="property-info">
                <span class="property-label">GOR</span>
                <span class="property-value">{{ pvtStudy.solutionGorScfStb | number:'1.0-0' }} scf/stb</span>
              </div>
            </div>
          </div>

          <!-- Property Tabs -->
          <mat-tab-group class="pvt-tabs">
            <!-- Bo Tab -->
            <mat-tab label="Bo (FVF)">
              <div class="tab-content">
                <div class="mini-chart">
                  <h4>Factor de Volumen del Petróleo</h4>
                  <div class="chart-values">
                    <div class="value-row">
                      <span>Bo &#64; Pb:</span>
                      <strong>{{ pvtStudy.boAtPbRbStb | number:'1.4-4' }} rb/stb</strong>
                    </div>
                    <div class="value-row">
                      <span>Bob:</span>
                      <strong>{{ pvtStudy.bobRbStb | number:'1.4-4' }} rb/stb</strong>
                    </div>
                  </div>
                  <div class="simple-chart">
                    <svg viewBox="0 0 300 150" class="chart-svg">
                      <polyline
                        points="20,130 100,80 150,60 200,50 280,45"
                        fill="none" stroke="#1976d2" stroke-width="2"/>
                      <circle cx="150" cy="60" r="5" fill="#f44336"/>
                      <text x="155" y="55" font-size="10" fill="#f44336">Pb</text>
                      <text x="10" y="145" font-size="9" fill="#666">0</text>
                      <text x="280" y="145" font-size="9" fill="#666">P</text>
                      <text x="5" y="20" font-size="9" fill="#666">Bo</text>
                    </svg>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Rs Tab -->
            <mat-tab label="Rs (GOR)">
              <div class="tab-content">
                <div class="mini-chart">
                  <h4>Relación Gas-Petróleo en Solución</h4>
                  <div class="chart-values">
                    <div class="value-row">
                      <span>Rs &#64; Pb:</span>
                      <strong>{{ pvtStudy.solutionGorScfStb | number:'1.0-0' }} scf/stb</strong>
                    </div>
                    <div class="value-row">
                      <span>Gas Gravity:</span>
                      <strong>{{ pvtStudy.gasGravity | number:'1.3-3' }}</strong>
                    </div>
                  </div>
                  <div class="simple-chart">
                    <svg viewBox="0 0 300 150" class="chart-svg">
                      <polyline
                        points="20,130 80,90 120,70 150,55 150,55 280,55"
                        fill="none" stroke="#4caf50" stroke-width="2"/>
                      <circle cx="150" cy="55" r="5" fill="#f44336"/>
                      <text x="155" y="50" font-size="10" fill="#f44336">Pb</text>
                      <text x="10" y="145" font-size="9" fill="#666">0</text>
                      <text x="280" y="145" font-size="9" fill="#666">P</text>
                      <text x="5" y="20" font-size="9" fill="#666">Rs</text>
                    </svg>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- Viscosity Tab -->
            <mat-tab label="μo (Viscosidad)">
              <div class="tab-content">
                <div class="mini-chart">
                  <h4>Viscosidad del Petróleo</h4>
                  <div class="chart-values">
                    <div class="value-row">
                      <span>μod (dead oil):</span>
                      <strong>{{ pvtStudy.deadOilViscosityCp | number:'1.2-2' }} cp</strong>
                    </div>
                    <div class="value-row">
                      <span>μo &#64; Pb:</span>
                      <strong>{{ pvtStudy.oilViscosityAtPbCp | number:'1.2-2' }} cp</strong>
                    </div>
                  </div>
                  <div class="simple-chart">
                    <svg viewBox="0 0 300 150" class="chart-svg">
                      <polyline
                        points="20,40 80,60 120,70 150,75 200,70 280,50"
                        fill="none" stroke="#ff9800" stroke-width="2"/>
                      <circle cx="150" cy="75" r="5" fill="#f44336"/>
                      <text x="155" y="70" font-size="10" fill="#f44336">Pb</text>
                      <text x="10" y="145" font-size="9" fill="#666">0</text>
                      <text x="280" y="145" font-size="9" fill="#666">P</text>
                      <text x="5" y="20" font-size="9" fill="#666">μo</text>
                    </svg>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- All Properties Tab -->
            <mat-tab label="Resumen">
              <div class="tab-content">
                <table class="pvt-table">
                  <thead>
                    <tr>
                      <th>Propiedad</th>
                      <th>Valor</th>
                      <th>Unidad</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Presión Inicial</td>
                      <td>{{ pvtStudy.initialPressurePsia | number:'1.0-0' }}</td>
                      <td>psia</td>
                    </tr>
                    <tr>
                      <td>Presión de Burbuja</td>
                      <td>{{ pvtStudy.bubblePointPressurePsia | number:'1.0-0' }}</td>
                      <td>psia</td>
                    </tr>
                    <tr>
                      <td>Temperatura</td>
                      <td>{{ pvtStudy.reservoirTemperatureF | number:'1.0-0' }}</td>
                      <td>°F</td>
                    </tr>
                    <tr>
                      <td>°API</td>
                      <td>{{ pvtStudy.apiGravity | number:'1.1-1' }}</td>
                      <td>-</td>
                    </tr>
                    <tr>
                      <td>GOR (Rs)</td>
                      <td>{{ pvtStudy.solutionGorScfStb | number:'1.0-0' }}</td>
                      <td>scf/stb</td>
                    </tr>
                    <tr>
                      <td>Bo &#64; Pb</td>
                      <td>{{ pvtStudy.boAtPbRbStb | number:'1.4-4' }}</td>
                      <td>rb/stb</td>
                    </tr>
                    <tr>
                      <td>μo &#64; Pb</td>
                      <td>{{ pvtStudy.oilViscosityAtPbCp | number:'1.2-2' }}</td>
                      <td>cp</td>
                    </tr>
                    <tr>
                      <td>Gas Gravity</td>
                      <td>{{ pvtStudy.gasGravity | number:'1.3-3' }}</td>
                      <td>-</td>
                    </tr>
                    <tr>
                      <td>Compresibilidad</td>
                      <td>{{ pvtStudy.oilCompressibility | number:'1.2e-6' }}</td>
                      <td>1/psi</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </mat-tab>
          </mat-tab-group>

          <!-- Venezuela Specific - Foamy Oil -->
          <div *ngIf="pvtStudy.hasFoamyBehavior" class="foamy-oil-section">
            <h4>
              <mat-icon>bubble_chart</mat-icon>
              Comportamiento Foamy Oil
            </h4>
            <div class="foamy-properties">
              <div class="foamy-item">
                <span class="label">Pseudo Pb:</span>
                <span class="value">{{ pvtStudy.pseudoBubblePoint | number:'1.0-0' }} psia</span>
              </div>
              <div class="foamy-item">
                <span class="label">Factor Foamy:</span>
                <span class="value">{{ pvtStudy.foamyOilFactor | number:'1.2-2' }}</span>
              </div>
            </div>
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .pvt-chart-card {
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

    .pvt-content {
      padding-top: 16px;
    }

    .key-properties {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .property-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
      border-left: 4px solid #1976d2;
    }

    .property-card mat-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #1976d2;
    }

    .property-info {
      display: flex;
      flex-direction: column;
    }

    .property-label {
      font-size: 12px;
      color: #666;
    }

    .property-value {
      font-size: 18px;
      font-weight: bold;
      color: #333;
    }

    .pvt-tabs {
      margin-top: 16px;
    }

    .tab-content {
      padding: 16px;
    }

    .mini-chart {
      background: #fafafa;
      border-radius: 8px;
      padding: 16px;
    }

    .mini-chart h4 {
      margin: 0 0 16px 0;
      color: #333;
    }

    .chart-values {
      display: flex;
      gap: 24px;
      margin-bottom: 16px;
    }

    .value-row {
      display: flex;
      gap: 8px;
    }

    .value-row span {
      color: #666;
    }

    .simple-chart {
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      padding: 8px;
    }

    .chart-svg {
      width: 100%;
      max-width: 400px;
      height: auto;
    }

    .pvt-table {
      width: 100%;
      border-collapse: collapse;
    }

    .pvt-table th, .pvt-table td {
      padding: 12px;
      text-align: left;
      border-bottom: 1px solid #e0e0e0;
    }

    .pvt-table th {
      background: #f5f5f5;
      font-weight: 600;
    }

    .pvt-table td:nth-child(2) {
      font-weight: bold;
      text-align: right;
    }

    .pvt-table td:nth-child(3) {
      color: #666;
      width: 80px;
    }

    .foamy-oil-section {
      margin-top: 24px;
      padding: 16px;
      background: #fff3e0;
      border-radius: 8px;
      border-left: 4px solid #ff9800;
    }

    .foamy-oil-section h4 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 12px 0;
      color: #e65100;
    }

    .foamy-properties {
      display: flex;
      gap: 32px;
    }

    .foamy-item {
      display: flex;
      gap: 8px;
    }

    .foamy-item .label {
      color: #666;
    }

    .foamy-item .value {
      font-weight: bold;
    }
  `]
})
export class RvPvtChartComponent implements OnInit, OnChanges {

  @Input() pvtStudyId: string;

  isLoading = false;
  pvtStudy: RvPvtStudy | null = null;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadPvtStudy();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.pvtStudyId && !changes.pvtStudyId.firstChange) {
      this.loadPvtStudy();
    }
  }

  loadPvtStudy(): void {
    if (!this.pvtStudyId) return;

    this.isLoading = true;
    this.rvService.getPvtStudy(this.pvtStudyId).subscribe({
      next: (data) => {
        this.pvtStudy = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }
}
