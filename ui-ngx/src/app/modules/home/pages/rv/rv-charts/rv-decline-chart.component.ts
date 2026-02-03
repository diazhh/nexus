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
  selector: 'tb-rv-decline-chart',
  template: `
    <div #chartContainer class="chart-container" *ngIf="forecastData.length > 0"></div>
    <div *ngIf="isLoading" class="chart-loading">
      <mat-spinner diameter="24"></mat-spinner>
    </div>
    <div *ngIf="!isLoading && forecastData.length === 0" class="chart-empty">
      <mat-icon>trending_down</mat-icon>
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
export class RvDeclineChartComponent implements OnInit, OnChanges, AfterViewInit {

  @Input() declineAnalysisId: string;
  @ViewChild('chartContainer', { static: false }) chartContainer: ElementRef;

  forecastData: any[] = [];
  isLoading = false;
  private chart: echarts.ECharts;

  constructor(private rvService: RvService) {}

  ngOnInit(): void {
    this.loadForecast();
  }

  ngAfterViewInit(): void {
    if (this.chartContainer && this.forecastData.length > 0) {
      this.initChart();
    }
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
        if (this.chartContainer) {
          setTimeout(() => this.initChart(), 0);
        }
      },
      error: () => this.isLoading = false
    });
  }

  private initChart(): void {
    if (!this.chartContainer || this.forecastData.length === 0) return;

    // Dispose existing chart
    if (this.chart) {
      this.chart.dispose();
    }

    // Initialize chart
    this.chart = echarts.init(this.chartContainer.nativeElement);

    const months = this.forecastData.map(d => d.month);
    const rates = this.forecastData.map(d => d.rateBopd);
    const cumulative = this.forecastData.map(d => d.cumulativeBbl);

    const option: echarts.EChartsCoreOption = {
      title: {
        text: 'Pronóstico de Declinación',
        left: 'center',
        textStyle: {
          fontSize: 16,
          fontWeight: 500
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      legend: {
        data: ['Tasa de Producción', 'Producción Acumulada'],
        top: 30
      },
      grid: {
        left: '10%',
        right: '10%',
        bottom: '15%',
        top: '20%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: months,
        name: 'Mes',
        nameLocation: 'middle',
        nameGap: 30,
        nameTextStyle: {
          fontSize: 12,
          fontWeight: 'bold'
        }
      },
      yAxis: [
        {
          type: 'value',
          name: 'Tasa (bopd)',
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold',
            color: '#f57c00'
          },
          axisLabel: {
            color: '#f57c00'
          }
        },
        {
          type: 'value',
          name: 'Acumulado (bbl)',
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold',
            color: '#1976d2'
          },
          axisLabel: {
            color: '#1976d2',
            formatter: (value: number) => {
              if (value >= 1000) {
                return (value / 1000).toFixed(0) + 'K';
              }
              return value.toString();
            }
          }
        }
      ],
      series: [
        {
          name: 'Tasa de Producción',
          type: 'line',
          data: rates,
          yAxisIndex: 0,
          smooth: true,
          lineStyle: {
            width: 3,
            color: '#f57c00'
          },
          itemStyle: {
            color: '#f57c00'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(245, 124, 0, 0.3)'
              }, {
                offset: 1, color: 'rgba(245, 124, 0, 0.05)'
              }]
            }
          }
        },
        {
          name: 'Producción Acumulada',
          type: 'line',
          data: cumulative,
          yAxisIndex: 1,
          smooth: true,
          lineStyle: {
            width: 2,
            color: '#1976d2',
            type: 'dashed'
          },
          itemStyle: {
            color: '#1976d2'
          }
        }
      ]
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
