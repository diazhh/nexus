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

import { Component, OnInit } from '@angular/core';
import { CTReportService, ReportType, ReportFormat } from '@core/http/ct-report.service';

interface ReportOption {
  type: ReportType;
  name: string;
  description: string;
  icon: string;
}

@Component({
  selector: 'tb-ct-reports',
  templateUrl: './ct-reports.component.html',
  styleUrls: ['./ct-reports.component.scss']
})
export class CTReportsComponent implements OnInit {

  reports: ReportOption[] = [
    {
      type: ReportType.JOB_SUMMARY,
      name: 'Job Summary Report',
      description: 'Comprehensive summary of all jobs including status, duration, and well information',
      icon: 'work'
    },
    {
      type: ReportType.REEL_LIFECYCLE,
      name: 'Reel Lifecycle Report',
      description: 'Detailed lifecycle information for all reels including fatigue levels and specifications',
      icon: 'album'
    },
    {
      type: ReportType.FLEET_UTILIZATION,
      name: 'Fleet Utilization Report',
      description: 'Fleet performance metrics including operational hours and utilization rates',
      icon: 'precision_manufacturing'
    },
    {
      type: ReportType.FATIGUE_ANALYSIS,
      name: 'Fatigue Analysis Report',
      description: 'Critical fatigue analysis for reels requiring attention or retirement',
      icon: 'warning'
    },
    {
      type: ReportType.MAINTENANCE_SCHEDULE,
      name: 'Maintenance Schedule Report',
      description: 'Upcoming maintenance requirements based on operational hours',
      icon: 'build'
    }
  ];

  selectedFormat: ReportFormat = ReportFormat.CSV;
  generating = false;
  tenantId: string;

  ReportFormat = ReportFormat;

  constructor(private reportService: CTReportService) {}

  ngOnInit() {
    this.tenantId = this.getCurrentTenantId();
  }

  generateReport(reportType: ReportType) {
    this.generating = true;

    let observable;
    let fileName = '';

    switch (reportType) {
      case ReportType.JOB_SUMMARY:
        observable = this.reportService.generateJobSummary(this.tenantId, this.selectedFormat);
        fileName = 'job_summary';
        break;
      case ReportType.REEL_LIFECYCLE:
        observable = this.reportService.generateReelLifecycle(this.tenantId, this.selectedFormat);
        fileName = 'reel_lifecycle';
        break;
      case ReportType.FLEET_UTILIZATION:
        observable = this.reportService.generateFleetUtilization(this.tenantId, this.selectedFormat);
        fileName = 'fleet_utilization';
        break;
      case ReportType.FATIGUE_ANALYSIS:
        observable = this.reportService.generateFatigueAnalysis(this.tenantId, this.selectedFormat);
        fileName = 'fatigue_analysis';
        break;
      case ReportType.MAINTENANCE_SCHEDULE:
        observable = this.reportService.generateMaintenanceSchedule(this.tenantId, this.selectedFormat);
        fileName = 'maintenance_schedule';
        break;
    }

    const extension = this.selectedFormat === ReportFormat.CSV ? '.csv' : '.txt';
    fileName += `_${Date.now()}${extension}`;

    observable.subscribe({
      next: (blob) => {
        this.reportService.downloadFile(blob, fileName);
        this.generating = false;
      },
      error: (error) => {
        console.error('Error generating report:', error);
        this.generating = false;
      }
    });
  }

  private getCurrentTenantId(): string {
    return 'current-tenant-id';
  }
}
