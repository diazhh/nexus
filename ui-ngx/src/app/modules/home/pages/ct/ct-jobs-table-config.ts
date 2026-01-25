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

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { CTJob, JobStatus } from '@shared/models/ct/ct-job.model';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityTableColumn } from '@home/models/entity/entities-table-config.models';

export class CTJobsTableConfig extends EntityTableConfig<CTJob> {

  constructor(
    private translate: TranslateService,
    private datePipe: DatePipe
  ) {
    super();
    this.entityType = 'CT_JOB' as any;
    this.entityTranslations = {
      list: 'ct.jobs',
      single: 'ct.job',
      noEntities: 'ct.no-jobs'
    };
    this.entityResources = {
      helpLinkId: 'ct-jobs'
    };

    this.columns.push(
      new EntityTableColumn<CTJob>('jobNumber', 'ct.job-number', '12%'),
      new EntityTableColumn<CTJob>('jobName', 'ct.job-name', '18%'),
      new EntityTableColumn<CTJob>('wellName', 'ct.well-name', '15%'),
      new EntityTableColumn<CTJob>('status', 'ct.status', '12%',
        entity => this.getStatusLabel(entity.status)),
      new EntityTableColumn<CTJob>('jobType', 'ct.job-type', '12%'),
      new EntityTableColumn<CTJob>('plannedStartDate', 'ct.planned-start', '13%',
        entity => entity.plannedStartDate ? this.datePipe.transform(entity.plannedStartDate, 'short') : '-'),
      new EntityTableColumn<CTJob>('actualDurationHours', 'ct.duration-hrs', '10%',
        entity => entity.actualDurationHours?.toFixed(1) || '-'),
      new EntityTableColumn<CTJob>('priority', 'ct.priority', '8%')
    );

    this.cellActionDescriptors = this.configureCellActions();
    this.groupActionDescriptors = this.configureGroupActions();
    this.addActionDescriptors = this.configureAddActions();
  }

  private getStatusLabel(status: JobStatus): string {
    const statusMap = {
      [JobStatus.PLANNED]: this.translate.instant('ct.status.planned'),
      [JobStatus.IN_PROGRESS]: this.translate.instant('ct.status.in-progress'),
      [JobStatus.COMPLETED]: this.translate.instant('ct.status.completed'),
      [JobStatus.CANCELLED]: this.translate.instant('ct.status.cancelled'),
      [JobStatus.ON_HOLD]: this.translate.instant('ct.status.on-hold')
    };
    return statusMap[status] || status;
  }

  private configureCellActions() {
    return [
      {
        name: this.translate.instant('action.view'),
        icon: 'visibility',
        isEnabled: () => true,
        onAction: ($event, entity) => this.viewEntity($event, entity)
      },
      {
        name: this.translate.instant('ct.simulate-job'),
        icon: 'science',
        isEnabled: (entity) => entity.status === JobStatus.PLANNED,
        onAction: ($event, entity) => this.simulateJob($event, entity)
      },
      {
        name: this.translate.instant('ct.start-job'),
        icon: 'play_arrow',
        isEnabled: (entity) => entity.status === JobStatus.PLANNED,
        onAction: ($event, entity) => this.startJob($event, entity)
      },
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: (entity) => entity.status !== JobStatus.COMPLETED,
        onAction: ($event, entity) => this.editEntity($event, entity)
      },
      {
        name: this.translate.instant('action.delete'),
        icon: 'delete',
        isEnabled: () => true,
        onAction: ($event, entity) => this.deleteEntity($event, entity)
      }
    ];
  }

  private configureGroupActions() {
    return [
      {
        name: this.translate.instant('action.delete'),
        icon: 'delete',
        isEnabled: true,
        onAction: ($event, entities) => this.deleteEntities($event, entities)
      }
    ];
  }

  private configureAddActions() {
    return [
      {
        name: this.translate.instant('ct.add-job'),
        icon: 'add',
        isEnabled: () => true,
        onAction: ($event) => this.addEntity($event)
      }
    ];
  }

  private viewEntity($event: Event, entity: CTJob) {
    // Navigate to job details
  }

  private simulateJob($event: Event, entity: CTJob) {
    // Open simulation dialog
  }

  private startJob($event: Event, entity: CTJob) {
    // Start job confirmation and action
  }

  private editEntity($event: Event, entity: CTJob) {
    // Open edit dialog
  }

  private deleteEntity($event: Event, entity: CTJob) {
    // Delete confirmation and action
  }

  private deleteEntities($event: Event, entities: CTJob[]) {
    // Bulk delete confirmation and action
  }

  private addEntity($event: Event) {
    // Open create dialog
  }
}
