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

import { Component, Input, OnInit, OnChanges, SimpleChanges, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { RvService } from '@core/http/rv/rv.service';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  LineChart,
  CanvasRenderer
]);

@Component({
  selector: 'tb-rv-ipr-chart',
  template: `
    <div #chartContainer class="chart-container" *ngIf="curveData.length > 0"></div>
    <div *ngIf="isLoading" class="chart-loading">
      <mat-spinner diameter="24"></mat-spinner>
    </div>
    <div *ngIf="!isLoading && curveData.length === 0" class="chart-empty">
      <mat-icon>show_chart</mat-icon>
      <p>No hay datos para mostrar</p>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      height: 100%;
    }
    .chart-container {
      width: 100%;
      height: 400px;
      min-height: 300px;
    }
    .chart-loading {
      text-align: center;
      padding: 48px;
    }
    .chart-empty {
      text-align: center;
      padding: 48px;
      color: #999;
    }
    .chart-empty mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #ccc;
      margin-bottom: 16px;
    }
  `]
})
export class RvIprChartComponent implements OnInit, OnChanges, AfterViewInit {

  @Input() iprModelId: string;
  @ViewChild('chartContainer', { static: false }) chartContainer: ElementRef;

  curveData: { pwfPsi: number; rateBopd: number }[] = [];
  isLoading = false;
  private chart: echarts.ECharts;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadCurve();
  }

  ngAfterViewInit(): void {
    if (this.chartContainer && this.curveData.length > 0) {
      this.initChart();
    }
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
        if (this.chartContainer) {
          setTimeout(() => this.initChart(), 0);
        }
      },
      error: () => this.isLoading = false
    });
  }

  private initChart(): void {
    if (!this.chartContainer || this.curveData.length === 0) return;

    // Dispose existing chart
    if (this.chart) {
      this.chart.dispose();
    }

    // Initialize chart
    this.chart = echarts.init(this.chartContainer.nativeElement);

    const option: echarts.EChartsCoreOption = {
      title: {
        text: 'Curva IPR (Inflow Performance Relationship)',
        left: 'center',
        textStyle: {
          fontSize: 16,
          fontWeight: 500
        }
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const data = params[0];
          return `Pwf: ${data.value[0].toFixed(0)} psi<br/>Rate: ${data.value[1].toFixed(0)} bopd`;
        }
      },
      grid: {
        left: '10%',
        right: '10%',
        bottom: '15%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'value',
        name: 'Presión de Fondo Fluyente (psi)',
        nameLocation: 'middle',
        nameGap: 30,
        nameTextStyle: {
          fontSize: 12,
          fontWeight: 'bold'
        }
      },
      yAxis: {
        type: 'value',
        name: 'Tasa de Producción (bopd)',
        nameLocation: 'middle',
        nameGap: 50,
        nameTextStyle: {
          fontSize: 12,
          fontWeight: 'bold'
        }
      },
      series: [{
        name: 'IPR Curve',
        type: 'line',
        data: this.curveData.map(d => [d.pwfPsi, d.rateBopd]),
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#1976d2'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: 'rgba(25, 118, 210, 0.3)'
            }, {
              offset: 1, color: 'rgba(25, 118, 210, 0.05)'
            }]
          }
        }
      }]
    };

    this.chart.setOption(option);

    // Resize on window resize
    window.addEventListener('resize', () => {
      if (this.chart) {
        this.chart.resize();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.dispose();
    }
  }
}
