///
/// Copyright © 2016-2026 The Thingsboard Authors
///

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { RvService } from '@core/http/rv/rv.service';
import { RvWell, RvCompletion, RvIprModel, RvDeclineAnalysis, formatOilRate, formatVolume } from '@shared/models/rv/rv.models';

@Component({
  selector: 'tb-rv-well-details',
  templateUrl: './rv-well-details.component.html',
  styleUrls: ['./rv-well-details.component.scss']
})
export class RvWellDetailsComponent implements OnInit {

  tenantId: string;
  wellId: string;
  well: RvWell;
  completions: RvCompletion[] = [];
  iprModels: RvIprModel[] = [];
  declineAnalyses: RvDeclineAnalysis[] = [];

  isLoading = true;
  selectedTabIndex = 0;

  formatOilRate = formatOilRate;
  formatVolume = formatVolume;

  constructor(
    private store: Store<AppState>,
    private rvService: RvService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser?.tenantId || '';
    this.wellId = this.route.snapshot.paramMap.get('wellId');

    if (this.wellId) {
      this.loadWellData();
    }
  }

  loadWellData(): void {
    this.isLoading = true;

    this.rvService.getWell(this.wellId).subscribe({
      next: (well) => {
        this.well = well;
        this.loadRelatedData();
      },
      error: () => {
        this.isLoading = false;
        this.router.navigate(['/rv/wells']);
      }
    });
  }

  loadRelatedData(): void {
    // Load completions
    this.rvService.getCompletionsByWell(this.tenantId, this.wellId).subscribe({
      next: (completions) => this.completions = completions
    });

    // Load IPR models
    this.rvService.getIprModelsByWell(this.tenantId, this.wellId).subscribe({
      next: (models) => this.iprModels = models
    });

    // Load decline analyses
    this.rvService.getDeclineAnalysesByWell(this.tenantId, this.wellId).subscribe({
      next: (analyses) => {
        this.declineAnalyses = analyses;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  goBack(): void {
    this.router.navigate(['/rv/wells']);
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PRODUCING': case 'ACTIVE': return 'primary';
      case 'DRILLING': case 'COMPLETING': return 'accent';
      case 'SHUT_IN': case 'INACTIVE': return 'warn';
      default: return '';
    }
  }
}
