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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PermissionMatrixComponent, PermissionMatrixDialogData } from './permission-matrix.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { RoleService } from '@core/http/role.service';
import { TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { Role, RolePermission, Resource, Operation } from '@shared/models/role.models';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('PermissionMatrixComponent', () => {
  let component: PermissionMatrixComponent;
  let fixture: ComponentFixture<PermissionMatrixComponent>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<PermissionMatrixComponent>>;
  let mockRoleService: jasmine.SpyObj<RoleService>;
  let mockStore: jasmine.SpyObj<Store>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockTranslate: jasmine.SpyObj<TranslateService>;

  const testRole: Role = {
    id: { id: '123' } as RoleId,
    tenantId: { id: 'tenant1' } as TenantId,
    name: 'Test Role',
    description: 'Test Description',
    isSystem: false,
    version: 1,
    createdTime: Date.now()
  };

  beforeEach(async () => {
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    mockRoleService = jasmine.createSpyObj('RoleService', ['getPermissions', 'updatePermissions']);
    mockStore = jasmine.createSpyObj('Store', ['dispatch', 'select']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockTranslate = jasmine.createSpyObj('TranslateService', ['instant']);

    mockTranslate.instant.and.callFake((key: string) => key);

    await TestBed.configureTestingModule({
      declarations: [PermissionMatrixComponent],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: RoleService, useValue: mockRoleService },
        { provide: Store, useValue: mockStore },
        { provide: Router, useValue: mockRouter },
        { provide: TranslateService, useValue: mockTranslate },
        { provide: MAT_DIALOG_DATA, useValue: { role: testRole } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  beforeEach(() => {
    mockRoleService.getPermissions.and.returnValue(of([]));
    fixture = TestBed.createComponent(PermissionMatrixComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should initialize with role from dialog data', () => {
    fixture.detectChanges();
    expect(component.role).toEqual(testRole);
  });

  it('should load available resources and operations on init', () => {
    fixture.detectChanges();
    
    expect(component.resources.length).toBeGreaterThan(0);
    expect(component.operations.length).toBeGreaterThan(0);
    expect(component.resources).toContain(Resource.DEVICE);
    expect(component.operations).toContain(Operation.READ);
  });

  it('should initialize permission matrix with all false values', () => {
    fixture.detectChanges();
    
    component.resources.forEach(resource => {
      component.operations.forEach(operation => {
        expect(component.isPermissionEnabled(resource, operation)).toBeFalsy();
      });
    });
  });

  it('should load current permissions from service', () => {
    const mockPermissions: RolePermission[] = [
      { roleId: testRole.id, resource: Resource.DEVICE, operation: Operation.READ },
      { roleId: testRole.id, resource: Resource.DEVICE, operation: Operation.WRITE },
      { roleId: testRole.id, resource: Resource.ASSET, operation: Operation.READ }
    ];

    mockRoleService.getPermissions.and.returnValue(of(mockPermissions));
    fixture.detectChanges();

    expect(component.isPermissionEnabled(Resource.DEVICE, Operation.READ)).toBeTruthy();
    expect(component.isPermissionEnabled(Resource.DEVICE, Operation.WRITE)).toBeTruthy();
    expect(component.isPermissionEnabled(Resource.ASSET, Operation.READ)).toBeTruthy();
    expect(component.isPermissionEnabled(Resource.ASSET, Operation.WRITE)).toBeFalsy();
  });

  it('should toggle permission on/off', () => {
    fixture.detectChanges();
    
    expect(component.isPermissionEnabled(Resource.DEVICE, Operation.READ)).toBeFalsy();
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    expect(component.isPermissionEnabled(Resource.DEVICE, Operation.READ)).toBeTruthy();
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    expect(component.isPermissionEnabled(Resource.DEVICE, Operation.READ)).toBeFalsy();
  });

  it('should toggle all operations for a resource', () => {
    fixture.detectChanges();
    
    component.toggleAllOperationsForResource(Resource.DEVICE);
    
    component.operations.forEach(operation => {
      expect(component.isPermissionEnabled(Resource.DEVICE, operation)).toBeTruthy();
    });
    
    component.toggleAllOperationsForResource(Resource.DEVICE);
    
    component.operations.forEach(operation => {
      expect(component.isPermissionEnabled(Resource.DEVICE, operation)).toBeFalsy();
    });
  });

  it('should toggle all resources for an operation', () => {
    fixture.detectChanges();
    
    component.toggleAllResourcesForOperation(Operation.READ);
    
    component.resources.forEach(resource => {
      expect(component.isPermissionEnabled(resource, Operation.READ)).toBeTruthy();
    });
    
    component.toggleAllResourcesForOperation(Operation.READ);
    
    component.resources.forEach(resource => {
      expect(component.isPermissionEnabled(resource, Operation.READ)).toBeFalsy();
    });
  });

  it('should detect when all operations are enabled for a resource', () => {
    fixture.detectChanges();
    
    expect(component.areAllOperationsEnabledForResource(Resource.DEVICE)).toBeFalsy();
    
    component.operations.forEach(operation => {
      component.togglePermission(Resource.DEVICE, operation);
    });
    
    expect(component.areAllOperationsEnabledForResource(Resource.DEVICE)).toBeTruthy();
  });

  it('should detect when all resources are enabled for an operation', () => {
    fixture.detectChanges();
    
    expect(component.areAllResourcesEnabledForOperation(Operation.READ)).toBeFalsy();
    
    component.resources.forEach(resource => {
      component.togglePermission(resource, Operation.READ);
    });
    
    expect(component.areAllResourcesEnabledForOperation(Operation.READ)).toBeTruthy();
  });

  it('should get resource translation', () => {
    fixture.detectChanges();
    
    const translation = component.getResourceTranslation(Resource.DEVICE);
    expect(mockTranslate.instant).toHaveBeenCalled();
    expect(translation).toBeDefined();
  });

  it('should get operation translation', () => {
    fixture.detectChanges();
    
    const translation = component.getOperationTranslation(Operation.READ);
    expect(mockTranslate.instant).toHaveBeenCalled();
    expect(translation).toBeDefined();
  });

  it('should count enabled permissions', () => {
    fixture.detectChanges();
    
    expect(component.getEnabledPermissionsCount()).toBe(0);
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    component.togglePermission(Resource.DEVICE, Operation.WRITE);
    component.togglePermission(Resource.ASSET, Operation.READ);
    
    expect(component.getEnabledPermissionsCount()).toBe(3);
  });

  it('should close dialog with null on cancel', () => {
    fixture.detectChanges();
    
    component.cancel();
    
    expect(mockDialogRef.close).toHaveBeenCalledWith(null);
  });

  it('should save permissions and close dialog', () => {
    mockRoleService.updatePermissions.and.returnValue(of(undefined));
    fixture.detectChanges();
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    component.togglePermission(Resource.ASSET, Operation.WRITE);
    
    component.save();
    
    expect(mockRoleService.updatePermissions).toHaveBeenCalled();
    const savedPermissions = mockRoleService.updatePermissions.calls.mostRecent().args[1];
    expect(savedPermissions.length).toBe(2);
    expect(mockDialogRef.close).toHaveBeenCalledWith(savedPermissions);
  });

  it('should only save enabled permissions', () => {
    mockRoleService.updatePermissions.and.returnValue(of(undefined));
    fixture.detectChanges();
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    component.togglePermission(Resource.DEVICE, Operation.WRITE);
    component.togglePermission(Resource.DEVICE, Operation.WRITE);
    
    component.save();
    
    const savedPermissions = mockRoleService.updatePermissions.calls.mostRecent().args[1];
    expect(savedPermissions.length).toBe(1);
    expect(savedPermissions[0].resource).toBe(Resource.DEVICE);
    expect(savedPermissions[0].operation).toBe(Operation.READ);
  });

  it('should set loading state during save', () => {
    mockRoleService.updatePermissions.and.returnValue(of(undefined));
    fixture.detectChanges();
    
    expect(component.loading).toBeFalsy();
    
    component.save();
    
    expect(component.loading).toBeFalsy();
  });

  it('should handle save errors gracefully', () => {
    mockRoleService.updatePermissions.and.returnValue(throwError({ status: 500 }));
    fixture.detectChanges();
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    component.save();
    
    expect(mockRoleService.updatePermissions).toHaveBeenCalled();
    expect(mockDialogRef.close).not.toHaveBeenCalled();
  });

  it('should handle permission loading errors', () => {
    mockRoleService.getPermissions.and.returnValue(throwError({ status: 500 }));
    
    fixture.detectChanges();
    
    expect(component.loading).toBeFalsy();
  });
});
