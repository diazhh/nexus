import { Component, OnInit } from '@angular/core';
import { CTJobService } from '@core/http/ct/ct-job.service';
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { CTJob, JobStatus } from '@shared/models/ct/ct-job.model';
import { PageLink } from '@shared/models/page/page-link';

interface AnalyticsMetrics {
  totalJobs: number;
  completedJobs: number;
  averageJobDuration: number;
  totalDepthDrilled: number;
  successRate: number;
}

interface JobTypeStats {
  type: string;
  count: number;
  percentage: number;
}

@Component({
  selector: 'tb-ct-analytics-dashboard',
  templateUrl: './ct-analytics-dashboard.component.html',
  styleUrls: ['./ct-analytics-dashboard.component.scss']
})
export class CTAnalyticsDashboardComponent implements OnInit {

  metrics: AnalyticsMetrics = {
    totalJobs: 0,
    completedJobs: 0,
    averageJobDuration: 0,
    totalDepthDrilled: 0,
    successRate: 0
  };

  jobTypeStats: JobTypeStats[] = [];
  recentJobs: CTJob[] = [];
  loading = true;

  displayedColumns = ['jobNumber', 'jobType', 'wellName', 'status', 'duration', 'depth'];

  constructor(
    private jobService: CTJobService,
    private unitService: CTUnitService,
    private reelService: CTReelService
  ) {}

  ngOnInit() {
    this.loadAnalyticsData();
  }

  loadAnalyticsData() {
    this.loading = true;
    const pageLink = new PageLink(100, 0);
    const tenantId = this.getCurrentTenantId();

    this.jobService.getJobs(pageLink, tenantId).subscribe({
      next: (response) => {
        this.recentJobs = response.data.slice(0, 10);
        this.metrics.totalJobs = response.totalElements;
        this.calculateMetrics(response.data);
        this.calculateJobTypeStats(response.data);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading analytics data:', error);
        this.loading = false;
      }
    });
  }

  private calculateMetrics(jobs: CTJob[]) {
    this.metrics.completedJobs = jobs.filter(j => j.status === JobStatus.COMPLETED).length;
    
    const completedJobs = jobs.filter(j => j.status === JobStatus.COMPLETED && j.actualDurationHours);
    this.metrics.averageJobDuration = completedJobs.length > 0
      ? completedJobs.reduce((sum, j) => sum + (j.actualDurationHours || 0), 0) / completedJobs.length
      : 0;

    this.metrics.totalDepthDrilled = jobs.reduce((sum, j) => sum + (j.maxDepthReachedFt || 0), 0);

    this.metrics.successRate = this.metrics.totalJobs > 0
      ? (this.metrics.completedJobs / this.metrics.totalJobs) * 100
      : 0;
  }

  private calculateJobTypeStats(jobs: CTJob[]) {
    const typeCounts = new Map<string, number>();
    
    jobs.forEach(job => {
      const type = job.jobType || 'Unknown';
      typeCounts.set(type, (typeCounts.get(type) || 0) + 1);
    });

    this.jobTypeStats = Array.from(typeCounts.entries()).map(([type, count]) => ({
      type,
      count,
      percentage: (count / jobs.length) * 100
    })).sort((a, b) => b.count - a.count);
  }

  private getCurrentTenantId(): string {
    return 'current-tenant';
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'IN_PROGRESS': 'primary',
      'PLANNED': 'accent',
      'COMPLETED': 'success',
      'CANCELLED': 'warn',
      'ON_HOLD': 'basic'
    };
    return colors[status] || 'basic';
  }

  formatDuration(hours: number | undefined): string {
    if (!hours) return 'N/A';
    if (hours < 1) return `${Math.round(hours * 60)} min`;
    return `${hours.toFixed(1)} hrs`;
  }
}
