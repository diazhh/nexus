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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RvWellLog } from '@shared/models/rv/rv.models';
import { LogTrack, LogCurve, LogDataPoint } from './rv-well-log-viewer.component';

export interface RvWellLogViewerDialogData {
  wellLog: RvWellLog;
  wellName: string;
}

@Component({
  selector: 'tb-rv-well-log-viewer-dialog',
  template: `
    <h2 mat-dialog-title>
      <mat-icon>analytics</mat-icon>
      Visualizador de Curvas - {{ data.wellLog.name }}
    </h2>
    <mat-dialog-content>
      <div class="well-info" *ngIf="data.wellName">
        <span class="info-label">Pozo:</span>
        <span class="info-value">{{ data.wellName }}</span>
        <span class="info-label">Profundidad:</span>
        <span class="info-value">{{ data.wellLog.topDepthMdM?.toFixed(1) }} - {{ data.wellLog.bottomDepthMdM?.toFixed(1) }} m</span>
      </div>

      <div *ngIf="tracks.length > 0; else noCurves">
        <tb-rv-well-log-viewer
          [tracks]="tracks"
          [topDepth]="data.wellLog.topDepthMdM || 0"
          [bottomDepth]="data.wellLog.bottomDepthMdM || 100"
          [depthUnit]="'m'"
          [height]="500">
        </tb-rv-well-log-viewer>
      </div>

      <ng-template #noCurves>
        <div class="no-curves">
          <mat-icon>show_chart</mat-icon>
          <p>No hay curvas disponibles para visualizar</p>
          <p class="hint">Importe un archivo LAS para agregar curvas a este registro</p>
        </div>
      </ng-template>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onClose()">Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      mat-icon {
        color: #1976d2;
      }
    }
    mat-dialog-content {
      min-width: 600px;
      max-width: 90vw;
    }
    .well-info {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 4px;
      .info-label {
        font-weight: 500;
        color: #666;
      }
      .info-value {
        color: #333;
      }
    }
    .no-curves {
      text-align: center;
      padding: 48px;
      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #ccc;
      }
      p {
        margin: 16px 0 0;
        color: #666;
      }
      .hint {
        font-size: 12px;
        color: #999;
      }
    }
  `]
})
export class RvWellLogViewerDialogComponent implements OnInit {

  tracks: LogTrack[] = [];

  constructor(
    public dialogRef: MatDialogRef<RvWellLogViewerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RvWellLogViewerDialogData
  ) {}

  ngOnInit(): void {
    this.buildTracks();
  }

  buildTracks(): void {
    const wellLog = this.data.wellLog;

    // Check if we have curve data in additionalInfo
    if (wellLog.additionalInfo?.curves && Array.isArray(wellLog.additionalInfo.curves)) {
      const curves = wellLog.additionalInfo.curves;

      // Group curves by type for multi-track display
      const grCurves: LogCurve[] = [];
      const resistivityCurves: LogCurve[] = [];
      const porosityCurves: LogCurve[] = [];
      const otherCurves: LogCurve[] = [];

      curves.forEach((curveData: any) => {
        const curve: LogCurve = {
          name: curveData.curveName,
          unit: curveData.unit || '',
          data: (curveData.data || []).map((d: any) => ({
            depth: d.depth,
            value: isNaN(d.value) ? null : d.value
          }))
        };

        const name = curveData.curveName.toUpperCase();
        if (name.includes('GR') || name === 'SGR' || name === 'CGR') {
          grCurves.push(curve);
        } else if (name.includes('RES') || name.includes('ILD') || name.includes('ILM') || name.includes('LLD') || name.includes('MSFL')) {
          curve.logarithmic = true;
          resistivityCurves.push(curve);
        } else if (name.includes('NPHI') || name.includes('RHOB') || name.includes('DEN') || name.includes('PHI')) {
          porosityCurves.push(curve);
        } else if (!name.includes('DEPT') && name !== 'MD') {
          otherCurves.push(curve);
        }
      });

      // Create tracks
      if (grCurves.length > 0) {
        this.tracks.push({ name: 'GR', curves: grCurves, width: 150 });
      }
      if (resistivityCurves.length > 0) {
        this.tracks.push({ name: 'Resistividad', curves: resistivityCurves, width: 150, logarithmic: true });
      }
      if (porosityCurves.length > 0) {
        this.tracks.push({ name: 'Porosidad', curves: porosityCurves, width: 150 });
      }
      if (otherCurves.length > 0) {
        this.tracks.push({ name: 'Otras', curves: otherCurves, width: 150 });
      }
    }
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
