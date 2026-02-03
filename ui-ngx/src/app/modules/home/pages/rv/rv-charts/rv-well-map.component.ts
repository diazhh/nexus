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

import { Component, Input, OnInit, OnChanges, SimpleChanges, Output, EventEmitter, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { RvService } from '@core/http/rv/rv.service';
import { RvWell } from '@shared/models/rv/rv.models';
import { PageLink } from '@shared/models/page/page-link';
import * as L from 'leaflet';
import 'leaflet.markercluster';

interface WellMarker {
  id: string;
  name: string;
  status: string;
  type: string;
  rate?: number;
  latitude?: number;
  longitude?: number;
  marker?: L.Marker;
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
          <mat-button-toggle-group [(value)]="viewMode" (change)="updateMarkerStyles()" aria-label="View mode">
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
          <p>Cargando pozos...</p>
        </div>

        <!-- Leaflet Map -->
        <div #mapContainer class="map-container" *ngIf="!isLoading"></div>

        <!-- No coordinates warning -->
        <mat-card *ngIf="!isLoading && wellsWithoutCoords > 0" class="warning-card">
          <mat-icon color="warn">warning</mat-icon>
          {{ wellsWithoutCoords }} pozos sin coordenadas geográficas no se muestran en el mapa
        </mat-card>

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

    .loading-container p {
      margin-top: 16px;
      color: #666;
    }

    .map-container {
      width: 100%;
      height: 500px;
      min-height: 400px;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .warning-card {
      margin-top: 16px;
      background: #fff3e0;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
    }

    .warning-card mat-icon {
      flex-shrink: 0;
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
      .map-stats {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    /* Leaflet popup custom styles */
    :host ::ng-deep .leaflet-popup-content-wrapper {
      border-radius: 8px;
      padding: 0;
    }

    :host ::ng-deep .leaflet-popup-content {
      margin: 12px;
      line-height: 1.4;
    }

    :host ::ng-deep .well-popup-title {
      font-weight: bold;
      font-size: 14px;
      margin-bottom: 8px;
      color: #1976d2;
    }

    :host ::ng-deep .well-popup-info {
      font-size: 12px;
      color: #666;
    }
  `]
})
export class RvWellMapComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  @Input() tenantId: string;
  @Input() fieldId: string;

  @Output() wellSelected = new EventEmitter<string>();

  @ViewChild('mapContainer', { static: false }) mapContainer: ElementRef;

  isLoading = false;
  wells: WellMarker[] = [];
  filteredWells: WellMarker[] = [];
  selectedWell: WellMarker | null = null;
  wellsWithoutCoords = 0;

  viewMode: 'status' | 'type' | 'rate' = 'status';
  statusFilter = 'ALL';

  private map: L.Map;
  private markerClusterGroup: L.MarkerClusterGroup;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadWells();
  }

  ngAfterViewInit(): void {
    if (this.wells.length > 0 && !this.map) {
      setTimeout(() => this.initMap(), 0);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes.tenantId || changes.fieldId) && !changes.tenantId?.firstChange) {
      this.loadWells();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  loadWells(): void {
    if (!this.tenantId) return;

    this.isLoading = true;
    const pageLink = new PageLink(500, 0);

    this.rvService.getWells(this.tenantId, pageLink).subscribe({
      next: (data) => {
        this.wells = data.data
          .filter(well => well.surfaceLatitude && well.surfaceLongitude)
          .map(well => this.mapWellToMarker(well));

        this.wellsWithoutCoords = data.data.length - this.wells.length;
        this.applyFilter();
        this.isLoading = false;

        if (this.mapContainer && !this.map) {
          setTimeout(() => this.initMap(), 0);
        } else if (this.map) {
          this.updateMapMarkers();
        }
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  mapWellToMarker(well: RvWell): WellMarker {
    return {
      id: well.id,
      name: well.name,
      status: well.wellStatus || 'UNKNOWN',
      type: well.wellType || 'PRODUCER',
      rate: well.currentRateBopd || 0,
      latitude: well.surfaceLatitude,
      longitude: well.surfaceLongitude
    };
  }

  private initMap(): void {
    if (!this.mapContainer || this.map) return;

    // Initialize map
    this.map = L.map(this.mapContainer.nativeElement, {
      center: this.getMapCenter(),
      zoom: 10,
      zoomControl: true
    });

    // Add OpenStreetMap tiles
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 18
    }).addTo(this.map);

    // Initialize marker cluster group
    this.markerClusterGroup = L.markerClusterGroup({
      showCoverageOnHover: false,
      zoomToBoundsOnClick: true,
      spiderfyOnMaxZoom: true,
      removeOutsideVisibleBounds: true,
      iconCreateFunction: (cluster) => {
        const count = cluster.getChildCount();
        let className = 'marker-cluster-';
        if (count < 10) {
          className += 'small';
        } else if (count < 50) {
          className += 'medium';
        } else {
          className += 'large';
        }
        return L.divIcon({
          html: `<div><span>${count}</span></div>`,
          className: 'marker-cluster ' + className,
          iconSize: L.point(40, 40)
        });
      }
    });

    this.map.addLayer(this.markerClusterGroup);

    // Add markers
    this.updateMapMarkers();

    // Fit bounds if we have wells
    if (this.filteredWells.length > 0) {
      const bounds = L.latLngBounds(
        this.filteredWells.map(w => [w.latitude, w.longitude] as L.LatLngTuple)
      );
      this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
    }
  }

  private getMapCenter(): L.LatLngExpression {
    if (this.filteredWells.length === 0) {
      return [0, 0]; // Default center
    }

    const avgLat = this.filteredWells.reduce((sum, w) => sum + w.latitude, 0) / this.filteredWells.length;
    const avgLng = this.filteredWells.reduce((sum, w) => sum + w.longitude, 0) / this.filteredWells.length;
    return [avgLat, avgLng];
  }

  private updateMapMarkers(): void {
    if (!this.map || !this.markerClusterGroup) return;

    // Clear existing markers
    this.markerClusterGroup.clearLayers();

    // Add markers for filtered wells
    this.filteredWells.forEach(well => {
      const icon = this.createWellIcon(well);
      const marker = L.marker([well.latitude, well.longitude], { icon });

      // Popup content
      const popupContent = `
        <div class="well-popup-title">${well.name}</div>
        <div class="well-popup-info">
          <div><strong>Estado:</strong> ${well.status}</div>
          <div><strong>Tipo:</strong> ${well.type}</div>
          ${well.rate ? `<div><strong>Tasa:</strong> ${well.rate.toFixed(0)} bopd</div>` : ''}
          <div><strong>Coords:</strong> ${well.latitude.toFixed(6)}, ${well.longitude.toFixed(6)}</div>
        </div>
      `;

      marker.bindPopup(popupContent);

      // Click event
      marker.on('click', () => {
        this.selectWell(well);
      });

      well.marker = marker;
      this.markerClusterGroup.addLayer(marker);
    });

    // Update bounds if needed
    if (this.filteredWells.length > 0) {
      const bounds = L.latLngBounds(
        this.filteredWells.map(w => [w.latitude, w.longitude] as L.LatLngTuple)
      );
      this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
    }
  }

  private createWellIcon(well: WellMarker): L.DivIcon {
    const color = this.getWellColor(well);
    const size = this.viewMode === 'rate' ? this.getRateIconSize(well.rate) : 12;

    const svg = `
      <svg width="${size * 2}" height="${size * 2}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" fill="${color}" stroke="#fff" stroke-width="2" />
        ${well.type?.includes('INJECTOR') ? '<line x1="7" y1="12" x2="17" y2="12" stroke="#fff" stroke-width="2"/><line x1="12" y1="7" x2="12" y2="17" stroke="#fff" stroke-width="2"/>' : ''}
      </svg>
    `;

    return L.divIcon({
      html: svg,
      className: 'well-marker-icon',
      iconSize: [size * 2, size * 2],
      iconAnchor: [size, size],
      popupAnchor: [0, -size]
    });
  }

  private getRateIconSize(rate: number): number {
    if (!rate || rate === 0) return 8;
    if (rate < 100) return 10;
    if (rate < 500) return 12;
    if (rate < 1000) return 14;
    return 16;
  }

  applyFilter(): void {
    if (this.statusFilter === 'ALL') {
      this.filteredWells = [...this.wells];
    } else if (this.statusFilter === 'INJECTOR') {
      this.filteredWells = this.wells.filter(w => w.type === 'INJECTOR' || w.type === 'WATER_INJECTOR' || w.type === 'GAS_INJECTOR');
    } else {
      this.filteredWells = this.wells.filter(w => w.status === this.statusFilter);
    }

    if (this.map) {
      this.updateMapMarkers();
    }
  }

  updateMarkerStyles(): void {
    if (this.map) {
      this.updateMapMarkers();
    }
  }

  selectWell(well: WellMarker): void {
    this.selectedWell = well;
    this.wellSelected.emit(well.id);

    // Pan to well location
    if (this.map && well.latitude && well.longitude) {
      this.map.setView([well.latitude, well.longitude], Math.max(this.map.getZoom(), 12), {
        animate: true
      });
    }
  }

  openWellDetails(): void {
    if (this.selectedWell) {
      this.wellSelected.emit(this.selectedWell.id);
    }
  }

  getWellColor(well: WellMarker): string {
    if (this.viewMode === 'type') {
      if (well.type?.includes('INJECTOR')) return '#2196f3';
      return '#4caf50';
    }

    if (this.viewMode === 'rate') {
      if (!well.rate || well.rate === 0) return '#999';
      if (well.rate < 100) return '#ffc107';
      if (well.rate < 500) return '#4caf50';
      return '#1976d2';
    }

    // By status (default)
    switch (well.status) {
      case 'PRODUCING': return '#4caf50';
      case 'SHUT_IN': return '#999';
      case 'DRILLING': return '#ff9800';
      case 'COMPLETING': return '#9c27b0';
      case 'ABANDONED': return '#f44336';
      default: return '#ccc';
    }
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
