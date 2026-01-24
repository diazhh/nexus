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

import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Role } from '@shared/models/role.models';
import { RoleService } from '@core/http/role.service';
import { RoleComponent } from '@modules/home/pages/role/role.component';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RoleDialogComponent, RoleDialogData } from '@modules/home/pages/role/role-dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { RoleTabsComponent } from '@home/pages/role/role-tabs.component';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { PermissionMatrixComponent, PermissionMatrixDialogData } from '@modules/home/pages/role/permission-matrix.component';

@Injectable()
export class RolesTableConfigResolver {

  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(private store: Store<AppState>,
              private roleService: RoleService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.ROLE;
    this.config.entityComponent = RoleComponent;
    this.config.entityTabsComponent = RoleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);

    this.config.columns.push(
      new DateEntityTableColumn<Role>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Role>('name', 'role.name', '30%'),
      new EntityTableColumn<Role>('description', 'role.description', '40%'),
      new EntityTableColumn<Role>('isSystem', 'role.is-system', '120px',
        (entity) => entity.isSystem ? this.translate.instant('common.yes') : this.translate.instant('common.no'))
    );

    this.config.deleteEnabled = role => role && !role.isSystem;
    this.config.deleteEntityTitle = role => this.translate.instant('role.delete-role-title', { roleName: role.name });
    this.config.deleteEntityContent = () => this.translate.instant('role.delete-role-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('role.delete-roles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('role.delete-roles-text');

    this.config.loadEntity = id => this.roleService.getRole(id.id);
    this.config.saveEntity = role => this.roleService.saveRole(role);
    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);

    this.config.entitiesFetchFunction = pageLink => this.fetchRoles(pageLink);

    this.config.addEnabled = true;
    this.config.addActionDescriptors = [
      {
        name: this.translate.instant('role.add-role'),
        icon: 'add',
        isEnabled: () => true,
        onAction: ($event) => this.addRole($event)
      }
    ];

    this.config.cellActionDescriptors = [
      {
        name: this.translate.instant('role.manage-permissions'),
        icon: 'security',
        isEnabled: () => true,
        onAction: ($event, entity) => this.managePermissions($event, entity)
      }
    ];

    this.config.entityTitle = (role) => role ? role.name : '';
    this.config.entitySelectionEnabled = (role) => !role.isSystem;
  }

  resolve(): EntityTableConfig<Role> {
    this.config.tableTitle = this.translate.instant('role.roles');
    return this.config;
  }

  private fetchRoles(pageLink: PageLink): Observable<PageData<Role>> {
    return this.roleService.getRoles(pageLink);
  }

  private addRole($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<RoleDialogComponent, RoleDialogData, Role>(RoleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        role: null
      }
    }).afterClosed().subscribe(
      (role) => {
        if (role) {
          this.config.updateData();
        }
      }
    );
  }

  private managePermissions($event: Event, role: Role): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<PermissionMatrixComponent, PermissionMatrixDialogData>(PermissionMatrixComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        role: role
      }
    }).afterClosed().subscribe(
      (permissions) => {
        if (permissions) {
          this.config.updateData();
        }
      }
    );
  }
}
