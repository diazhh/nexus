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
import { MappingTemplate, MappingTemplateInfo, DistributionMode } from '@shared/models/data-mapping.models';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatDialog } from '@angular/material/dialog';
import { PageLink } from '@shared/models/page/page-link';
import { MappingTemplateDialogComponent, MappingTemplateDialogData } from './mapping-template-dialog.component';
import { MappingTemplateRulesDialogComponent, MappingTemplateRulesDialogData } from './mapping-template-rules-dialog.component';

@Component({
  selector: 'tb-data-mapping-templates',
  templateUrl: './data-mapping-templates.component.html',
  styleUrls: ['./data-mapping-templates.component.scss']
})
export class DataMappingTemplatesComponent extends PageComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['name', 'moduleKey', 'targetAssetType', 'distributionMode', 'rulesCount', 'isDefault', 'active', 'actions'];
  dataSource = new MatTableDataSource<MappingTemplateInfo>();
  templates: MappingTemplateInfo[] = [];

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
    this.loadTemplates();
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

  loadTemplates(): void {
    this.isLoading = true;
    const pageLink = new PageLink(1000); // Load all for now

    const request$ = this.selectedModule
      ? this.dataMappingService.getMappingTemplatesByModule(this.selectedModule, pageLink)
      : this.dataMappingService.getMappingTemplates(pageLink);

    request$.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageData) => {
          const templates = pageData.data as MappingTemplateInfo[];
          // Load rule counts for each template
          if (templates.length > 0) {
            const countRequests = templates.map(t =>
              this.dataMappingService.countMappingTemplateRules(t.id.id)
            );
            forkJoin(countRequests).pipe(takeUntil(this.destroy$))
              .subscribe({
                next: (counts) => {
                  templates.forEach((t, i) => t.ruleCount = counts[i]);
                  this.templates = templates;
                  this.dataSource.data = templates;
                  this.isLoading = false;
                },
                error: () => {
                  this.templates = templates;
                  this.dataSource.data = templates;
                  this.isLoading = false;
                }
              });
          } else {
            this.templates = [];
            this.dataSource.data = [];
            this.isLoading = false;
          }
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  onModuleFilterChange(): void {
    this.loadTemplates();
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  createTemplate(): void {
    const dialogRef = this.dialog.open<MappingTemplateDialogComponent, MappingTemplateDialogData, MappingTemplate>(
      MappingTemplateDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          template: null,
          isEdit: false,
          selectedModule: this.selectedModule || undefined
        }
      }
    );

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
      if (result) {
        this.showNotification('data-mapping.template-created');
        this.loadTemplates();
      }
    });
  }

  editTemplate(template: MappingTemplate): void {
    const dialogRef = this.dialog.open<MappingTemplateDialogComponent, MappingTemplateDialogData, MappingTemplate>(
      MappingTemplateDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          template,
          isEdit: true
        }
      }
    );

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
      if (result) {
        this.showNotification('data-mapping.template-updated');
        this.loadTemplates();
      }
    });
  }

  viewRules(template: MappingTemplate): void {
    const dialogRef = this.dialog.open<MappingTemplateRulesDialogComponent, MappingTemplateRulesDialogData, boolean>(
      MappingTemplateRulesDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          template
        }
      }
    );

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
      if (result) {
        this.loadTemplates();
      }
    });
  }

  duplicateTemplate(template: MappingTemplate): void {
    this.dialogService.confirm(
      this.translate.instant('data-mapping.duplicate-template'),
      this.translate.instant('data-mapping.confirm-duplicate-template'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(confirmed => {
      if (confirmed) {
        const newName = `${template.name} (Copy)`;
        this.dataMappingService.duplicateMappingTemplate(template.id.id, newName)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.showNotification('data-mapping.template-duplicated');
              this.loadTemplates();
            },
            error: () => {
              this.showNotification('data-mapping.error-duplicating', 'warn');
            }
          });
      }
    });
  }

  deleteTemplate(template: MappingTemplate): void {
    this.dialogService.confirm(
      this.translate.instant('data-mapping.delete-template'),
      this.translate.instant('data-mapping.confirm-delete-template'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.dataMappingService.deleteMappingTemplate(template.id.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.showNotification('data-mapping.template-deleted');
              this.loadTemplates();
            },
            error: () => {
              this.showNotification('data-mapping.error-deleting', 'warn');
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

  getDistributionModeLabel(mode: DistributionMode): string {
    return this.translate.instant(`data-mapping.distribution-mode.${mode.toLowerCase()}`);
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
