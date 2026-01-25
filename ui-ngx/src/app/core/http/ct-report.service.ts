import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum ReportType {
  JOB_SUMMARY = 'JOB_SUMMARY',
  REEL_LIFECYCLE = 'REEL_LIFECYCLE',
  FLEET_UTILIZATION = 'FLEET_UTILIZATION',
  FATIGUE_ANALYSIS = 'FATIGUE_ANALYSIS',
  MAINTENANCE_SCHEDULE = 'MAINTENANCE_SCHEDULE'
}

export enum ReportFormat {
  CSV = 'CSV',
  PDF = 'PDF',
  EXCEL = 'EXCEL'
}

export interface ReportRequest {
  reportType: ReportType;
  format: ReportFormat;
  startDate?: number;
  endDate?: number;
  entityId?: string;
  tenantId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CTReportService {

  private baseUrl = '/api/nexus/ct/reports';

  constructor(private http: HttpClient) {}

  generateReport(request: ReportRequest): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/generate`, request, {
      responseType: 'blob'
    });
  }

  generateJobSummary(tenantId: string, format: ReportFormat = ReportFormat.CSV): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/job-summary/${tenantId}`, {
      params,
      responseType: 'blob'
    });
  }

  generateReelLifecycle(tenantId: string, format: ReportFormat = ReportFormat.CSV): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/reel-lifecycle/${tenantId}`, {
      params,
      responseType: 'blob'
    });
  }

  generateFleetUtilization(tenantId: string, format: ReportFormat = ReportFormat.CSV): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/fleet-utilization/${tenantId}`, {
      params,
      responseType: 'blob'
    });
  }

  generateFatigueAnalysis(tenantId: string, format: ReportFormat = ReportFormat.CSV): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/fatigue-analysis/${tenantId}`, {
      params,
      responseType: 'blob'
    });
  }

  generateMaintenanceSchedule(tenantId: string, format: ReportFormat = ReportFormat.CSV): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/maintenance-schedule/${tenantId}`, {
      params,
      responseType: 'blob'
    });
  }

  downloadFile(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
