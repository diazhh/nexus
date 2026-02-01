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

import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { DataMappingService } from '@core/http/data-mapping.service';
import { DataSourceConfig, DataSourceConfigInfo } from '@shared/models/data-mapping.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatDialog } from '@angular/material/dialog';
import { ApplyTemplateDialogComponent, ApplyTemplateDialogData } from './apply-template-dialog.component';

@Component({
  selector: 'tb-data-mapping-sources',
  templateUrl: './data-mapping-sources.component.html',
  styleUrls: ['./data-mapping-sources.component.scss']
})
export class DataMappingSourcesComponent extends PageComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['deviceName', 'assetName', 'moduleKey', 'distributionMode', 'active', 'createdTime', 'actions'];
  dataSource = new MatTableDataSource<DataSourceConfigInfo>();
  sources: DataSourceConfigInfo[] = [];

  isLoading = true;
  selectedModule: string = '';
  moduleOptions: string[] = ['', 'CT', 'DR', 'RV'];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  private readonly destroy$ = new Subject<void>();

  constructor(
    protected store: Store<AppState>,
    private dataMappingService: DataMappingService,
    private translate: TranslateService,
    private dialogService: DialogService,
    private dialog: MatDialog
  ) {
    super(store);
  }

  ngOnInit(): void {
    this.loadSources();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadSources(): void {
    this.isLoading = true;
    const pageLink = new PageLink(1000); // Load all for now

    const request$ = this.selectedModule
      ? this.dataMappingService.getDataSourcesByModule(this.selectedModule as any, pageLink)
      : this.dataMappingService.getDataSources(pageLink);

    request$.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageData) => {
          this.sources = pageData.data as DataSourceConfigInfo[];
          this.dataSource.data = this.sources;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading data sources:', error);
          this.showNotification('data-mapping.error-loading-sources', 'error');
          this.isLoading = false;
          this.sources = [];
          this.dataSource.data = [];
        }
      });
  }

  onModuleFilterChange(): void {
    this.loadSources();
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  createDataSource(): void {
    const dialogRef = this.dialog.open<ApplyTemplateDialogComponent, ApplyTemplateDialogData, DataSourceConfig>(
      ApplyTemplateDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          moduleKey: this.selectedModule || undefined
        }
      }
    );

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
      if (result) {
        this.showNotification('data-mapping.template-applied');
        this.loadSources();
      }
    });
  }

  deleteDataSource(source: DataSourceConfigInfo): void {
    this.dialogService.confirm(
      this.translate.instant('data-mapping.delete-data-source'),
      this.translate.instant('data-mapping.confirm-delete-data-source'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.dataMappingService.deleteDataSource(source.id.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.showNotification('data-mapping.data-source-deleted');
              this.loadSources();
            },
            error: (error) => {
              console.error('Error deleting data source:', error);
              this.showNotification('data-mapping.error-deleting-source', 'error');
            }
          });
      }
    });
  }

  getModuleLabel(moduleKey: string): string {
    const moduleLabels: { [key: string]: string } = {
      'CT': 'Coiled Tubing',
      'DR': 'Drilling',
      'RV': 'Reservoir'
    };
    return moduleLabels[moduleKey] || moduleKey;
  }

  formatDate(timestamp: number): string {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleString();
  }

  private showNotification(messageKey: string, type: 'success' | 'warn' | 'error' = 'success'): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageKey),
      type,
      duration: 2000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }
}
