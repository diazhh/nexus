import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CTJobService } from '@core/http/ct/ct-job.service';
import { CTUnitService } from '@core/http/ct/ct-unit.service';
import { CTReelService } from '@core/http/ct/ct-reel.service';
import { CTJob, JobStatus } from '@shared/models/ct/ct-job.model';
import { CTUnit } from '@shared/models/ct/ct-unit.model';
import { CTReel } from '@shared/models/ct/ct-reel.model';
import { PageLink } from '@shared/models/page/page-link';

interface RealtimeMetrics {
  activeJobs: number;
  activeUnits: number;
  totalDepth: number;
  averageSpeed: number;
  criticalAlarms: number;
}

interface JobProgress {
  jobId: string;
  jobNumber: string;
  wellName: string;
  currentDepth: number;
  targetDepth: number;
  progress: number;
  status: string;
  startTime: Date;
  estimatedCompletion: Date;
}

@Component({
  selector: 'tb-ct-realtime-dashboard',
  templateUrl: './ct-realtime-dashboard.component.html',
  styleUrls: ['./ct-realtime-dashboard.component.scss']
})
export class CTRealtimeDashboardComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();
  
  metrics: RealtimeMetrics = {
    activeJobs: 0,
    activeUnits: 0,
    totalDepth: 0,
    averageSpeed: 0,
    criticalAlarms: 0
  };

  activeJobs: JobProgress[] = [];
  recentAlarms: any[] = [];
  
  loading = true;
  lastUpdate: Date = new Date();

  displayedColumns = ['jobNumber', 'wellName', 'currentDepth', 'targetDepth', 'progress', 'status', 'actions'];

  constructor(
    private jobService: CTJobService,
    private unitService: CTUnitService,
    private reelService: CTReelService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadDashboardData();
    
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshData();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadDashboardData() {
    this.loading = true;
    
    const pageLink = new PageLink(20, 0);
    const tenantId = this.getCurrentTenantId();
    
    this.jobService.getJobs(pageLink, tenantId).subscribe({
      next: (response) => {
        const inProgressJobs = response.data.filter(job => job.status === JobStatus.IN_PROGRESS);
        this.activeJobs = inProgressJobs.map(job => this.mapJobToProgress(job));
        this.metrics.activeJobs = inProgressJobs.length;
        this.calculateMetrics();
        this.loading = false;
        this.lastUpdate = new Date();
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.loading = false;
      }
    });

    this.unitService.getUnits(pageLink, tenantId).subscribe({
      next: (response) => {
        this.metrics.activeUnits = response.totalElements;
      }
    });
  }

  refreshData() {
    const pageLink = new PageLink(20, 0);
    const tenantId = this.getCurrentTenantId();
    
    this.jobService.getJobs(pageLink, tenantId).subscribe({
      next: (response) => {
        const inProgressJobs = response.data.filter(job => job.status === JobStatus.IN_PROGRESS);
        this.activeJobs = inProgressJobs.map(job => this.mapJobToProgress(job));
        this.metrics.activeJobs = inProgressJobs.length;
        this.calculateMetrics();
        this.lastUpdate = new Date();
      }
    });
  }

  private getCurrentTenantId(): string {
    return 'current-tenant';
  }

  private mapJobToProgress(job: CTJob): JobProgress {
    const currentDepth = job.maxDepthReachedFt || 0;
    const targetDepth = job.targetDepthToFt || 1;
    const progress = (currentDepth / targetDepth) * 100;

    return {
      jobId: job.id.id,
      jobNumber: job.jobNumber,
      wellName: job.wellName,
      currentDepth,
      targetDepth,
      progress: Math.min(progress, 100),
      status: job.status,
      startTime: new Date(job.actualStartDate || job.plannedStartDate || Date.now()),
      estimatedCompletion: this.calculateEstimatedCompletion(job)
    };
  }

  private calculateEstimatedCompletion(job: CTJob): Date {
    const now = new Date();
    const estimatedHours = 4;
    return new Date(now.getTime() + estimatedHours * 60 * 60 * 1000);
  }

  private calculateMetrics() {
    this.metrics.totalDepth = this.activeJobs.reduce((sum, job) => sum + job.currentDepth, 0);
    this.metrics.averageSpeed = this.activeJobs.length > 0 
      ? this.metrics.totalDepth / this.activeJobs.length 
      : 0;
  }

  viewJobDetails(jobId: string) {
    this.router.navigate(['/ct/jobs', jobId]);
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'IN_PROGRESS': 'primary',
      'PLANNED': 'accent',
      'COMPLETED': 'success',
      'CANCELLED': 'warn'
    };
    return colors[status] || 'basic';
  }

  getProgressColor(progress: number): string {
    if (progress < 30) return 'warn';
    if (progress < 70) return 'accent';
    return 'primary';
  }
}
