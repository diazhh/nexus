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
import { CTReel, ReelStatus } from '@shared/models/ct/ct-reel.model';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityTableColumn } from '@home/models/entity/entities-table-config.models';

export class CTReelsTableConfig extends EntityTableConfig<CTReel> {

  constructor(
    private translate: TranslateService,
    private datePipe: DatePipe
  ) {
    super();
    this.entityType = 'CT_REEL' as any;
    this.entityTranslations = {
      list: 'ct.reels',
      single: 'ct.reel',
      noEntities: 'ct.no-reels'
    };
    this.entityResources = {
      helpLinkId: 'ct-reels'
    };

    this.columns.push(
      new EntityTableColumn<CTReel>('reelCode', 'ct.reel-code', '12%'),
      new EntityTableColumn<CTReel>('reelName', 'ct.reel-name', '18%'),
      new EntityTableColumn<CTReel>('status', 'ct.status', '12%',
        entity => this.getStatusLabel(entity.status)),
      new EntityTableColumn<CTReel>('materialGrade', 'ct.material-grade', '10%'),
      new EntityTableColumn<CTReel>('totalLengthFt', 'ct.length-ft', '10%',
        entity => entity.totalLengthFt?.toFixed(0) || '0'),
      new EntityTableColumn<CTReel>('accumulatedFatiguePercent', 'ct.fatigue', '12%',
        entity => this.getFatigueDisplay(entity.accumulatedFatiguePercent)),
      new EntityTableColumn<CTReel>('totalCycles', 'ct.cycles', '10%'),
      new EntityTableColumn<CTReel>('currentLocation', 'ct.location', '16%')
    );

    this.cellActionDescriptors = this.configureCellActions();
    this.groupActionDescriptors = this.configureGroupActions();
    this.addActionDescriptors = this.configureAddActions();
  }

  private getStatusLabel(status: ReelStatus): string {
    const statusMap = {
      [ReelStatus.AVAILABLE]: this.translate.instant('ct.status.available'),
      [ReelStatus.IN_USE]: this.translate.instant('ct.status.in-use'),
      [ReelStatus.MAINTENANCE]: this.translate.instant('ct.status.maintenance'),
      [ReelStatus.RETIRED]: this.translate.instant('ct.status.retired')
    };
    return statusMap[status] || status;
  }

  private getFatigueDisplay(fatigue?: number): string {
    if (!fatigue) return '0.0%';
    const value = fatigue.toFixed(1);
    const color = fatigue >= 95 ? 'red' : fatigue >= 80 ? 'orange' : 'green';
    return `<span style="color: ${color}; font-weight: bold;">${value}%</span>`;
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
        name: this.translate.instant('ct.view-fatigue-history'),
        icon: 'timeline',
        isEnabled: () => true,
        onAction: ($event, entity) => this.viewFatigueHistory($event, entity)
      },
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
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
        name: this.translate.instant('ct.add-reel'),
        icon: 'add',
        isEnabled: () => true,
        onAction: ($event) => this.addEntity($event)
      }
    ];
  }

  private viewEntity($event: Event, entity: CTReel) {
    // Navigate to reel details
  }

  private viewFatigueHistory($event: Event, entity: CTReel) {
    // Open fatigue history dialog
  }

  private editEntity($event: Event, entity: CTReel) {
    // Open edit dialog
  }

  private deleteEntity($event: Event, entity: CTReel) {
    // Delete confirmation and action
  }

  private deleteEntities($event: Event, entities: CTReel[]) {
    // Bulk delete confirmation and action
  }

  private addEntity($event: Event) {
    // Open create dialog
  }
}
