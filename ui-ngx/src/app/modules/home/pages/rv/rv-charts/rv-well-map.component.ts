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

import { Component, Input, OnInit, OnChanges, SimpleChanges, Output, EventEmitter } from '@angular/core';
import { RvService } from '@core/http/rv/rv.service';
import { RvWell } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';

interface WellMarker {
  id: string;
  name: string;
  x: number;
  y: number;
  status: string;
  type: string;
  rate?: number;
  latitude?: number;
  longitude?: number;
}

@Component({
  selector: 'tb-rv-well-map',
  template: `
    <mat-card class="well-map-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>map</mat-icon>
          Mapa de Pozos
        </mat-card-title>
        <mat-card-subtitle>{{ wells.length }} pozos en el campo</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <!-- Toolbar -->
        <div class="map-toolbar">
          <mat-button-toggle-group [(value)]="viewMode" aria-label="View mode">
            <mat-button-toggle value="status">Por Estado</mat-button-toggle>
            <mat-button-toggle value="type">Por Tipo</mat-button-toggle>
            <mat-button-toggle value="rate">Por Producción</mat-button-toggle>
          </mat-button-toggle-group>

          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Filtrar</mat-label>
            <mat-select [(value)]="statusFilter" (selectionChange)="applyFilter()">
              <mat-option value="ALL">Todos</mat-option>
              <mat-option value="PRODUCING">Productores</mat-option>
              <mat-option value="INJECTOR">Inyectores</mat-option>
              <mat-option value="SHUT_IN">Cerrados</mat-option>
              <mat-option value="DRILLING">En Perforación</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>

        <!-- Map Area -->
        <div *ngIf="!isLoading" class="map-container">
          <!-- SVG Map -->
          <svg viewBox="0 0 600 400" class="well-map-svg" (click)="onMapClick($event)">
            <!-- Background Grid -->
            <defs>
              <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
                <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#e0e0e0" stroke-width="0.5"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)"/>

            <!-- Field Boundary (example polygon) -->
            <polygon *ngIf="showBoundary"
                     points="50,50 550,50 550,350 50,350"
                     fill="none" stroke="#1976d2" stroke-width="2" stroke-dasharray="5,5"/>

            <!-- Well Markers -->
            <g *ngFor="let well of filteredWells" class="well-marker"
               [attr.transform]="'translate(' + well.x + ',' + well.y + ')'"
               (click)="selectWell(well, $event)"
               [matTooltip]="well.name + ' - ' + well.status">

              <!-- Well Symbol based on type/status -->
              <ng-container [ngSwitch]="getWellSymbol(well)">
                <!-- Producer -->
                <circle *ngSwitchCase="'producer'" r="10"
                        [attr.fill]="getWellColor(well)"
                        stroke="#333" stroke-width="1.5"/>

                <!-- Injector -->
                <g *ngSwitchCase="'injector'">
                  <circle r="10" fill="none" [attr.stroke]="getWellColor(well)" stroke-width="2"/>
                  <line x1="-5" y1="0" x2="5" y2="0" [attr.stroke]="getWellColor(well)" stroke-width="2"/>
                  <line x1="0" y1="-5" x2="0" y2="5" [attr.stroke]="getWellColor(well)" stroke-width="2"/>
                </g>

                <!-- Shut-in -->
                <g *ngSwitchCase="'shutin'">
                  <circle r="10" fill="#fff" stroke="#999" stroke-width="2"/>
                  <line x1="-5" y1="-5" x2="5" y2="5" stroke="#999" stroke-width="2"/>
                </g>

                <!-- Drilling -->
                <g *ngSwitchCase="'drilling'">
                  <polygon points="0,-12 8,8 -8,8" fill="#ff9800" stroke="#333" stroke-width="1"/>
                </g>

                <!-- Default -->
                <circle *ngSwitchDefault r="8" fill="#ccc" stroke="#666" stroke-width="1"/>
              </ng-container>

              <!-- Rate bubble (for production view) -->
              <g *ngIf="viewMode === 'rate' && well.rate > 0">
                <circle [attr.r]="getRateBubbleSize(well.rate)" fill="rgba(76, 175, 80, 0.3)"
                        stroke="#4caf50" stroke-width="1"/>
              </g>
            </g>

            <!-- Selected well highlight -->
            <circle *ngIf="selectedWell"
                    [attr.cx]="selectedWell.x" [attr.cy]="selectedWell.y"
                    r="18" fill="none" stroke="#f44336" stroke-width="2"
                    class="selection-ring"/>

            <!-- Scale bar -->
            <g transform="translate(500, 380)">
              <line x1="0" y1="0" x2="80" y2="0" stroke="#333" stroke-width="2"/>
              <line x1="0" y1="-5" x2="0" y2="5" stroke="#333" stroke-width="2"/>
              <line x1="80" y1="-5" x2="80" y2="5" stroke="#333" stroke-width="2"/>
              <text x="40" y="-8" text-anchor="middle" font-size="10">1 km</text>
            </g>

            <!-- North arrow -->
            <g transform="translate(30, 30)">
              <polygon points="0,-15 5,10 0,5 -5,10" fill="#333"/>
              <text x="0" y="-20" text-anchor="middle" font-size="10" font-weight="bold">N</text>
            </g>
          </svg>

          <!-- Legend -->
          <div class="map-legend">
            <h4>Leyenda</h4>
            <div class="legend-items">
              <div class="legend-item">
                <svg width="20" height="20"><circle cx="10" cy="10" r="8" fill="#4caf50"/></svg>
                <span>Productor Activo</span>
              </div>
              <div class="legend-item">
                <svg width="20" height="20"><circle cx="10" cy="10" r="8" fill="#2196f3"/></svg>
                <span>Inyector</span>
              </div>
              <div class="legend-item">
                <svg width="20" height="20">
                  <circle cx="10" cy="10" r="8" fill="#fff" stroke="#999" stroke-width="2"/>
                  <line x1="5" y1="5" x2="15" y2="15" stroke="#999" stroke-width="2"/>
                </svg>
                <span>Cerrado</span>
              </div>
              <div class="legend-item">
                <svg width="20" height="20"><polygon points="10,2 18,18 2,18" fill="#ff9800"/></svg>
                <span>En Perforación</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Selected Well Info Panel -->
        <mat-expansion-panel *ngIf="selectedWell" class="well-info-panel" [expanded]="true">
          <mat-expansion-panel-header>
            <mat-panel-title>
              <mat-icon>oil_barrel</mat-icon>
              {{ selectedWell.name }}
            </mat-panel-title>
            <mat-panel-description>
              <mat-chip [color]="getStatusChipColor(selectedWell.status)" selected>
                {{ selectedWell.status }}
              </mat-chip>
            </mat-panel-description>
          </mat-expansion-panel-header>

          <div class="well-info-content">
            <div class="info-row">
              <span class="label">Tipo:</span>
              <span class="value">{{ selectedWell.type }}</span>
            </div>
            <div class="info-row" *ngIf="selectedWell.rate">
              <span class="label">Tasa Actual:</span>
              <span class="value">{{ selectedWell.rate | number:'1.0-0' }} bopd</span>
            </div>
            <div class="info-row" *ngIf="selectedWell.latitude">
              <span class="label">Coordenadas:</span>
              <span class="value">{{ selectedWell.latitude | number:'1.6-6' }}, {{ selectedWell.longitude | number:'1.6-6' }}</span>
            </div>
            <button mat-stroked-button color="primary" (click)="openWellDetails()">
              <mat-icon>open_in_new</mat-icon>
              Ver Detalles
            </button>
          </div>
        </mat-expansion-panel>

        <!-- Statistics Summary -->
        <div class="map-stats">
          <div class="stat-item">
            <span class="stat-value">{{ getWellCountByStatus('PRODUCING') }}</span>
            <span class="stat-label">Productores</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ getWellCountByStatus('INJECTOR') }}</span>
            <span class="stat-label">Inyectores</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ getWellCountByStatus('SHUT_IN') }}</span>
            <span class="stat-label">Cerrados</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ getTotalProduction() | number:'1.0-0' }}</span>
            <span class="stat-label">BOPD Total</span>
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .well-map-card {
      margin: 16px 0;
    }

    mat-card-header mat-icon {
      margin-right: 8px;
      vertical-align: middle;
    }

    .map-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      flex-wrap: wrap;
      gap: 16px;
    }

    .filter-field {
      width: 200px;
    }

    .loading-container {
      text-align: center;
      padding: 48px;
    }

    .map-container {
      display: grid;
      grid-template-columns: 1fr 200px;
      gap: 16px;
    }

    .well-map-svg {
      width: 100%;
      height: auto;
      min-height: 400px;
      background: #fafafa;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      cursor: crosshair;
    }

    .well-marker {
      cursor: pointer;
      transition: transform 0.2s;
    }

    .well-marker:hover {
      transform: scale(1.2);
    }

    .selection-ring {
      animation: pulse 1.5s infinite;
    }

    @keyframes pulse {
      0% { opacity: 1; }
      50% { opacity: 0.5; }
      100% { opacity: 1; }
    }

    .map-legend {
      background: #f5f5f5;
      border-radius: 8px;
      padding: 16px;
    }

    .map-legend h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
    }

    .legend-items {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
    }

    .well-info-panel {
      margin-top: 16px;
    }

    .well-info-panel mat-panel-title mat-icon {
      margin-right: 8px;
    }

    .well-info-content {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 8px;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
    }

    .info-row .label {
      color: #666;
    }

    .info-row .value {
      font-weight: bold;
    }

    .map-stats {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;
    }

    .stat-item {
      text-align: center;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .stat-value {
      display: block;
      font-size: 24px;
      font-weight: bold;
      color: #1976d2;
    }

    .stat-label {
      font-size: 12px;
      color: #666;
    }

    @media (max-width: 768px) {
      .map-container {
        grid-template-columns: 1fr;
      }

      .map-stats {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `]
})
export class RvWellMapComponent implements OnInit, OnChanges {

  @Input() tenantId: string;
  @Input() fieldId: string;
  @Input() showBoundary = true;

  @Output() wellSelected = new EventEmitter<string>();

  isLoading = false;
  wells: WellMarker[] = [];
  filteredWells: WellMarker[] = [];
  selectedWell: WellMarker | null = null;

  viewMode: 'status' | 'type' | 'rate' = 'status';
  statusFilter = 'ALL';

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadWells();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes.tenantId || changes.fieldId) && !changes.tenantId?.firstChange) {
      this.loadWells();
    }
  }

  loadWells(): void {
    if (!this.tenantId) return;

    this.isLoading = true;
    const pageLink = new PageLink(500, 0);

    this.rvService.getWells(this.tenantId, pageLink).subscribe({
      next: (data) => {
        this.wells = data.data.map((well, index) => this.mapWellToMarker(well, index));
        this.applyFilter();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  mapWellToMarker(well: RvWell, index: number): WellMarker {
    // If we have coordinates, use them; otherwise generate pseudo-random positions
    let x: number, y: number;

    if (well.surfaceLatitude && well.surfaceLongitude) {
      // Normalize coordinates to fit in SVG viewport
      // This is simplified - in production you'd use proper projection
      x = 50 + ((well.surfaceLongitude + 180) % 360) * 1.5;
      y = 50 + ((90 - well.surfaceLatitude) % 180) * 2;
    } else {
      // Generate positions in a grid-like pattern with some randomness
      const row = Math.floor(index / 8);
      const col = index % 8;
      x = 80 + col * 60 + (Math.random() * 20 - 10);
      y = 80 + row * 50 + (Math.random() * 20 - 10);
    }

    return {
      id: well.id,
      name: well.name,
      x: Math.min(Math.max(x, 50), 550),
      y: Math.min(Math.max(y, 50), 350),
      status: well.wellStatus || 'UNKNOWN',
      type: well.wellType || 'PRODUCER',
      rate: well.currentRateBopd || 0,
      latitude: well.surfaceLatitude,
      longitude: well.surfaceLongitude
    };
  }

  applyFilter(): void {
    if (this.statusFilter === 'ALL') {
      this.filteredWells = [...this.wells];
    } else if (this.statusFilter === 'INJECTOR') {
      this.filteredWells = this.wells.filter(w => w.type === 'INJECTOR' || w.type === 'WATER_INJECTOR' || w.type === 'GAS_INJECTOR');
    } else {
      this.filteredWells = this.wells.filter(w => w.status === this.statusFilter);
    }
  }

  selectWell(well: WellMarker, event: Event): void {
    event.stopPropagation();
    this.selectedWell = well;
    this.wellSelected.emit(well.id);
  }

  onMapClick(event: Event): void {
    // Deselect if clicking on empty area
    if ((event.target as Element).tagName === 'svg' || (event.target as Element).tagName === 'rect') {
      this.selectedWell = null;
    }
  }

  openWellDetails(): void {
    if (this.selectedWell) {
      this.wellSelected.emit(this.selectedWell.id);
    }
  }

  getWellSymbol(well: WellMarker): string {
    if (well.type?.includes('INJECTOR')) return 'injector';
    if (well.status === 'SHUT_IN') return 'shutin';
    if (well.status === 'DRILLING') return 'drilling';
    return 'producer';
  }

  getWellColor(well: WellMarker): string {
    if (well.type?.includes('INJECTOR')) return '#2196f3';

    switch (well.status) {
      case 'PRODUCING': return '#4caf50';
      case 'SHUT_IN': return '#999';
      case 'DRILLING': return '#ff9800';
      case 'COMPLETING': return '#9c27b0';
      case 'ABANDONED': return '#f44336';
      default: return '#ccc';
    }
  }

  getRateBubbleSize(rate: number): number {
    // Scale bubble size based on production rate
    const minSize = 15;
    const maxSize = 40;
    const maxRate = Math.max(...this.wells.map(w => w.rate || 0), 1000);
    return minSize + (rate / maxRate) * (maxSize - minSize);
  }

  getStatusChipColor(status: string): string {
    switch (status) {
      case 'PRODUCING': return 'primary';
      case 'DRILLING': return 'accent';
      case 'SHUT_IN': return 'warn';
      default: return '';
    }
  }

  getWellCountByStatus(status: string): number {
    if (status === 'INJECTOR') {
      return this.wells.filter(w => w.type?.includes('INJECTOR')).length;
    }
    return this.wells.filter(w => w.status === status).length;
  }

  getTotalProduction(): number {
    return this.wells
      .filter(w => w.status === 'PRODUCING')
      .reduce((sum, w) => sum + (w.rate || 0), 0);
  }
}
