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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Role, RolePermission, Resource, Operation, resourceTranslationMap, operationTranslationMap } from '@shared/models/role.models';
import { RoleService } from '@core/http/role.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

export interface PermissionMatrixDialogData {
  role: Role;
}

interface PermissionCell {
  resource: Resource;
  operation: Operation;
  enabled: boolean;
}

@Component({
  selector: 'tb-permission-matrix',
  templateUrl: './permission-matrix.component.html',
  styleUrls: ['./permission-matrix.component.scss']
})
export class PermissionMatrixComponent extends DialogComponent<PermissionMatrixComponent, RolePermission[]> implements OnInit {

  role: Role;
  resources: Resource[] = [];
  operations: Operation[] = [];
  permissionMatrix: Map<string, Map<string, boolean>> = new Map();
  loading = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: PermissionMatrixDialogData,
              public dialogRef: MatDialogRef<PermissionMatrixComponent, RolePermission[]>,
              private roleService: RoleService,
              private translate: TranslateService) {
    super(store, router, dialogRef);
    this.role = data.role;
  }

  ngOnInit(): void {
    this.loadAvailableResourcesAndOperations();
    this.loadCurrentPermissions();
  }

  private loadAvailableResourcesAndOperations(): void {
    this.resources = Object.values(Resource);
    this.operations = Object.values(Operation);
    
    // Initialize matrix
    this.resources.forEach(resource => {
      const operationMap = new Map<string, boolean>();
      this.operations.forEach(operation => {
        operationMap.set(operation, false);
      });
      this.permissionMatrix.set(resource, operationMap);
    });
  }

  private loadCurrentPermissions(): void {
    if (this.role && this.role.id) {
      this.loading = true;
      this.roleService.getPermissions(this.role.id.id).subscribe(
        (permissions) => {
          permissions.forEach(permission => {
            const operationMap = this.permissionMatrix.get(permission.resource);
            if (operationMap) {
              operationMap.set(permission.operation, true);
            }
          });
          this.loading = false;
        },
        () => {
          this.loading = false;
        }
      );
    }
  }

  isPermissionEnabled(resource: Resource, operation: Operation): boolean {
    const operationMap = this.permissionMatrix.get(resource);
    return operationMap ? operationMap.get(operation) || false : false;
  }

  togglePermission(resource: Resource, operation: Operation): void {
    const operationMap = this.permissionMatrix.get(resource);
    if (operationMap) {
      const currentValue = operationMap.get(operation) || false;
      operationMap.set(operation, !currentValue);
    }
  }

  toggleAllOperationsForResource(resource: Resource): void {
    const operationMap = this.permissionMatrix.get(resource);
    if (operationMap) {
      const allEnabled = this.areAllOperationsEnabledForResource(resource);
      this.operations.forEach(operation => {
        operationMap.set(operation, !allEnabled);
      });
    }
  }

  toggleAllResourcesForOperation(operation: Operation): void {
    const allEnabled = this.areAllResourcesEnabledForOperation(operation);
    this.resources.forEach(resource => {
      const operationMap = this.permissionMatrix.get(resource);
      if (operationMap) {
        operationMap.set(operation, !allEnabled);
      }
    });
  }

  areAllOperationsEnabledForResource(resource: Resource): boolean {
    const operationMap = this.permissionMatrix.get(resource);
    if (!operationMap) {
      return false;
    }
    return this.operations.every(operation => operationMap.get(operation) === true);
  }

  areAllResourcesEnabledForOperation(operation: Operation): boolean {
    return this.resources.every(resource => {
      const operationMap = this.permissionMatrix.get(resource);
      return operationMap ? operationMap.get(operation) === true : false;
    });
  }

  getResourceTranslation(resource: Resource): string {
    const key = resourceTranslationMap[resource];
    return key ? this.translate.instant(key) : resource;
  }

  getOperationTranslation(operation: Operation): string {
    const key = operationTranslationMap[operation];
    return key ? this.translate.instant(key) : operation;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const permissions: RolePermission[] = [];
    
    this.permissionMatrix.forEach((operationMap, resource) => {
      operationMap.forEach((enabled, operation) => {
        if (enabled) {
          permissions.push({
            roleId: this.role.id,
            resource: resource as Resource,
            operation: operation as Operation
          });
        }
      });
    });

    this.loading = true;
    this.roleService.updatePermissions(this.role.id.id, permissions).subscribe(
      () => {
        this.loading = false;
        this.dialogRef.close(permissions);
      },
      () => {
        this.loading = false;
      }
    );
  }

  getEnabledPermissionsCount(): number {
    let count = 0;
    this.permissionMatrix.forEach((operationMap) => {
      operationMap.forEach((enabled) => {
        if (enabled) {
          count++;
        }
      });
    });
    return count;
  }
}
