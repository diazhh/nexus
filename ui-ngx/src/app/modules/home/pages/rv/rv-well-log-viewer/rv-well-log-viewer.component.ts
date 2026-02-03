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

import { Component, Input, OnInit, OnChanges, ElementRef, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';

/**
 * Interface for log curve data
 */
export interface LogCurve {
  name: string;
  unit: string;
  data: LogDataPoint[];
  color?: string;
  minValue?: number;
  maxValue?: number;
  logarithmic?: boolean;
}

export interface LogDataPoint {
  depth: number;
  value: number | null;
}

export interface LogTrack {
  name: string;
  curves: LogCurve[];
  width?: number;
  gridLines?: number;
  logarithmic?: boolean;
}

/**
 * Well Log Viewer Component
 *
 * Displays well log curves in a multi-track visualization similar to
 * industry-standard log viewers.
 *
 * Usage:
 * <tb-rv-well-log-viewer
 *   [tracks]="logTracks"
 *   [topDepth]="2000"
 *   [bottomDepth]="2500"
 *   [depthUnit]="'m'"
 * ></tb-rv-well-log-viewer>
 */
@Component({
  selector: 'tb-rv-well-log-viewer',
  templateUrl: './rv-well-log-viewer.component.html',
  styleUrls: ['./rv-well-log-viewer.component.scss']
})
export class RvWellLogViewerComponent implements OnInit, OnChanges, AfterViewInit {

  @ViewChild('logContainer') logContainer: ElementRef<HTMLDivElement>;

  @Input() tracks: LogTrack[] = [];
  @Input() topDepth: number = 0;
  @Input() bottomDepth: number = 100;
  @Input() depthUnit: string = 'm';
  @Input() height: number = 600;

  // Internal state
  containerWidth: number = 800;
  depthTrackWidth: number = 60;
  defaultTrackWidth: number = 150;

  // Default curve colors
  private defaultColors = ['#1E88E5', '#D32F2F', '#388E3C', '#F57C00', '#7B1FA2', '#00796B'];
  private colorIndex = 0;

  ngOnInit(): void {
    this.assignCurveColors();
  }

  ngAfterViewInit(): void {
    if (this.logContainer) {
      this.containerWidth = this.logContainer.nativeElement.offsetWidth;
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tracks']) {
      this.assignCurveColors();
    }
  }

  private assignCurveColors(): void {
    this.colorIndex = 0;
    this.tracks.forEach(track => {
      track.curves.forEach(curve => {
        if (!curve.color) {
          curve.color = this.defaultColors[this.colorIndex % this.defaultColors.length];
          this.colorIndex++;
        }
        // Calculate min/max if not provided
        if (curve.minValue === undefined || curve.maxValue === undefined) {
          const values = curve.data.filter(d => d.value !== null).map(d => d.value as number);
          if (values.length > 0) {
            curve.minValue = curve.minValue ?? Math.min(...values);
            curve.maxValue = curve.maxValue ?? Math.max(...values);
          }
        }
      });
    });
  }

  getTrackWidth(track: LogTrack): number {
    return track.width || this.defaultTrackWidth;
  }

  getTotalWidth(): number {
    return this.depthTrackWidth + this.tracks.reduce((sum, t) => sum + this.getTrackWidth(t), 0);
  }

  // Generate depth grid lines
  getDepthGridLines(): number[] {
    const range = this.bottomDepth - this.topDepth;
    const step = this.calculateGridStep(range);
    const lines: number[] = [];

    let depth = Math.ceil(this.topDepth / step) * step;
    while (depth <= this.bottomDepth) {
      lines.push(depth);
      depth += step;
    }
    return lines;
  }

  private calculateGridStep(range: number): number {
    const idealSteps = 10;
    const rawStep = range / idealSteps;

    // Round to nice numbers
    const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    const normalized = rawStep / magnitude;

    if (normalized <= 1) return magnitude;
    if (normalized <= 2) return 2 * magnitude;
    if (normalized <= 5) return 5 * magnitude;
    return 10 * magnitude;
  }

  depthToY(depth: number): number {
    const range = this.bottomDepth - this.topDepth;
    return ((depth - this.topDepth) / range) * this.height;
  }

  // Generate SVG path for a curve
  getCurvePath(curve: LogCurve, trackWidth: number, logarithmic: boolean = false): string {
    if (!curve.data || curve.data.length === 0) return '';

    const minVal = curve.minValue ?? 0;
    const maxVal = curve.maxValue ?? 1;

    const valueToX = (value: number): number => {
      if (logarithmic && minVal > 0) {
        const logMin = Math.log10(minVal);
        const logMax = Math.log10(maxVal);
        const logVal = Math.log10(Math.max(value, minVal));
        return ((logVal - logMin) / (logMax - logMin)) * (trackWidth - 10) + 5;
      }
      return ((value - minVal) / (maxVal - minVal)) * (trackWidth - 10) + 5;
    };

    let path = '';
    let lastValidPoint: { x: number; y: number } | null = null;

    curve.data.forEach((point, index) => {
      if (point.depth < this.topDepth || point.depth > this.bottomDepth) return;

      const y = this.depthToY(point.depth);

      if (point.value === null) {
        lastValidPoint = null;
        return;
      }

      const x = valueToX(point.value);

      if (lastValidPoint === null) {
        path += `M ${x} ${y} `;
      } else {
        path += `L ${x} ${y} `;
      }

      lastValidPoint = { x, y };
    });

    return path;
  }

  // Get grid lines for a track (value axis)
  getTrackGridValues(track: LogTrack): number[] {
    if (track.curves.length === 0) return [];

    const firstCurve = track.curves[0];
    const min = firstCurve.minValue ?? 0;
    const max = firstCurve.maxValue ?? 1;
    const steps = track.gridLines ?? 5;

    const values: number[] = [];
    if (track.logarithmic && min > 0) {
      // Logarithmic scale - use powers of 10
      const logMin = Math.floor(Math.log10(min));
      const logMax = Math.ceil(Math.log10(max));
      for (let i = logMin; i <= logMax; i++) {
        values.push(Math.pow(10, i));
      }
    } else {
      // Linear scale
      const step = (max - min) / steps;
      for (let i = 0; i <= steps; i++) {
        values.push(min + step * i);
      }
    }
    return values;
  }

  valueToX(value: number, track: LogTrack): number {
    if (track.curves.length === 0) return 0;

    const trackWidth = this.getTrackWidth(track);
    const firstCurve = track.curves[0];
    const min = firstCurve.minValue ?? 0;
    const max = firstCurve.maxValue ?? 1;

    if (track.logarithmic && min > 0) {
      const logMin = Math.log10(min);
      const logMax = Math.log10(max);
      const logVal = Math.log10(Math.max(value, min));
      return ((logVal - logMin) / (logMax - logMin)) * (trackWidth - 10) + 5;
    }
    return ((value - min) / (max - min)) * (trackWidth - 10) + 5;
  }

  formatValue(value: number): string {
    if (Math.abs(value) >= 1000 || (Math.abs(value) < 0.01 && value !== 0)) {
      return value.toExponential(1);
    }
    return value.toFixed(Math.abs(value) < 1 ? 2 : 0);
  }

  // Zoom controls
  zoomIn(): void {
    const range = this.bottomDepth - this.topDepth;
    const center = (this.topDepth + this.bottomDepth) / 2;
    const newRange = range * 0.8;
    this.topDepth = center - newRange / 2;
    this.bottomDepth = center + newRange / 2;
  }

  zoomOut(): void {
    const range = this.bottomDepth - this.topDepth;
    const center = (this.topDepth + this.bottomDepth) / 2;
    const newRange = range * 1.25;
    this.topDepth = center - newRange / 2;
    this.bottomDepth = center + newRange / 2;
  }

  scrollUp(): void {
    const range = this.bottomDepth - this.topDepth;
    const step = range * 0.25;
    this.topDepth -= step;
    this.bottomDepth -= step;
  }

  scrollDown(): void {
    const range = this.bottomDepth - this.topDepth;
    const step = range * 0.25;
    this.topDepth += step;
    this.bottomDepth += step;
  }

  // Helper for template - calculates cumulative track offset
  calcTrackOffset = (sum: number, track: LogTrack): number => {
    return sum + (track.width || this.defaultTrackWidth);
  }
}
