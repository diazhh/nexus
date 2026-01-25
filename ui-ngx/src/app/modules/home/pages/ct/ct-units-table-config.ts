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
import { CTUnit, UnitStatus } from '@shared/models/ct/ct-unit.model';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityTableColumn } from '@home/models/entity/entities-table-config.models';

export class CTUnitsTableConfig extends EntityTableConfig<CTUnit> {

  constructor(
    private translate: TranslateService,
    private datePipe: DatePipe
  ) {
    super();
    this.entityType = 'CT_UNIT' as any;
    this.entityTranslations = {
      list: 'ct.units',
      single: 'ct.unit',
      noEntities: 'ct.no-units'
    };
    this.entityResources = {
      helpLinkId: 'ct-units'
    };

    this.columns.push(
      new EntityTableColumn<CTUnit>('unitCode', 'ct.unit-code', '15%'),
      new EntityTableColumn<CTUnit>('unitName', 'ct.unit-name', '20%'),
      new EntityTableColumn<CTUnit>('operationalStatus', 'ct.status', '15%', 
        entity => this.getStatusLabel(entity.operationalStatus)),
      new EntityTableColumn<CTUnit>('currentLocation', 'ct.location', '15%'),
      new EntityTableColumn<CTUnit>('totalOperationalHours', 'ct.operational-hours', '15%',
        entity => entity.totalOperationalHours?.toFixed(1) || '0.0'),
      new EntityTableColumn<CTUnit>('totalJobsCompleted', 'ct.jobs-completed', '10%'),
      new EntityTableColumn<CTUnit>('createdTime', 'common.created-time', '10%',
        entity => this.datePipe.transform(entity.createdTime, 'short'))
    );

    this.cellActionDescriptors = this.configureCellActions();
    this.groupActionDescriptors = this.configureGroupActions();
    this.addActionDescriptors = this.configureAddActions();
  }

  private getStatusLabel(status: UnitStatus): string {
    const statusMap = {
      [UnitStatus.STANDBY]: this.translate.instant('ct.status.standby'),
      [UnitStatus.ACTIVE]: this.translate.instant('ct.status.active'),
      [UnitStatus.MAINTENANCE]: this.translate.instant('ct.status.maintenance'),
      [UnitStatus.OUT_OF_SERVICE]: this.translate.instant('ct.status.out-of-service')
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
        name: this.translate.instant('ct.add-unit'),
        icon: 'add',
        isEnabled: () => true,
        onAction: ($event) => this.addEntity($event)
      }
    ];
  }

  private viewEntity($event: Event, entity: CTUnit) {
    // Navigate to unit details
  }

  private editEntity($event: Event, entity: CTUnit) {
    // Open edit dialog
  }

  private deleteEntity($event: Event, entity: CTUnit) {
    // Delete confirmation and action
  }

  private deleteEntities($event: Event, entities: CTUnit[]) {
    // Bulk delete confirmation and action
  }

  private addEntity($event: Event) {
    // Open create dialog
  }
}
