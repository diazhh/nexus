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
import { RoleDialogComponent, RoleDialogData } from './role-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { RoleService } from '@core/http/role.service';
import { of, throwError } from 'rxjs';
import { Role } from '@shared/models/role.models';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('RoleDialogComponent', () => {
  let component: RoleDialogComponent;
  let fixture: ComponentFixture<RoleDialogComponent>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<RoleDialogComponent>>;
  let mockRoleService: jasmine.SpyObj<RoleService>;
  let mockStore: jasmine.SpyObj<Store>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    mockRoleService = jasmine.createSpyObj('RoleService', ['saveRole']);
    mockStore = jasmine.createSpyObj('Store', ['dispatch', 'select']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [RoleDialogComponent],
      imports: [ReactiveFormsModule],
      providers: [
        UntypedFormBuilder,
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: RoleService, useValue: mockRoleService },
        { provide: Store, useValue: mockStore },
        { provide: Router, useValue: mockRouter },
        { provide: MAT_DIALOG_DATA, useValue: { role: null } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should initialize as add mode when no role provided', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    expect(component.isAdd).toBeTruthy();
  });

  it('should initialize as edit mode when role provided', () => {
    const testRole: Role = {
      id: { id: '123' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'Existing Role',
      description: 'Existing Description',
      isSystem: false,
      version: 1,
      createdTime: Date.now()
    };

    TestBed.overrideProvider(MAT_DIALOG_DATA, { useValue: { role: testRole } });
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    expect(component.isAdd).toBeFalsy();
    expect(component.role).toEqual(testRole);
  });

  it('should initialize form with empty values in add mode', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    expect(component.roleForm.get('name').value).toBe('');
    expect(component.roleForm.get('description').value).toBe('');
  });

  it('should initialize form with role values in edit mode', () => {
    const testRole: Role = {
      id: { id: '123' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'Test Role',
      description: 'Test Description',
      isSystem: false,
      version: 1,
      createdTime: Date.now()
    };

    TestBed.overrideProvider(MAT_DIALOG_DATA, { useValue: { role: testRole } });
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    expect(component.roleForm.get('name').value).toBe('Test Role');
    expect(component.roleForm.get('description').value).toBe('Test Description');
  });

  it('should validate name as required', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    const nameControl = component.roleForm.get('name');
    nameControl.setValue('');
    
    expect(nameControl.invalid).toBeTruthy();
    expect(nameControl.errors.required).toBeTruthy();
  });

  it('should validate name max length', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    const nameControl = component.roleForm.get('name');
    const longName = 'a'.repeat(256);
    nameControl.setValue(longName);
    
    expect(nameControl.invalid).toBeTruthy();
    expect(nameControl.errors.maxlength).toBeTruthy();
  });

  it('should validate description max length', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    const descControl = component.roleForm.get('description');
    const longDesc = 'a'.repeat(1025);
    descControl.setValue(longDesc);
    
    expect(descControl.invalid).toBeTruthy();
    expect(descControl.errors.maxlength).toBeTruthy();
  });

  it('should close dialog with null on cancel', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    component.cancel();
    
    expect(mockDialogRef.close).toHaveBeenCalledWith(null);
  });

  it('should not save when form is invalid', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    component.roleForm.get('name').setValue('');
    component.save();
    
    expect(mockRoleService.saveRole).not.toHaveBeenCalled();
  });

  it('should save role when form is valid', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    const savedRole: Role = {
      id: { id: '123' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'New Role',
      description: 'New Description',
      isSystem: false,
      version: 1,
      createdTime: Date.now()
    };

    mockRoleService.saveRole.and.returnValue(of(savedRole));
    
    component.roleForm.patchValue({
      name: 'New Role',
      description: 'New Description'
    });
    
    component.save();
    
    expect(mockRoleService.saveRole).toHaveBeenCalled();
    expect(mockDialogRef.close).toHaveBeenCalledWith(savedRole);
  });

  it('should merge existing role data with form values on save', () => {
    const existingRole: Role = {
      id: { id: '123' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'Old Name',
      description: 'Old Description',
      isSystem: false,
      version: 1,
      createdTime: 123456789
    };

    TestBed.overrideProvider(MAT_DIALOG_DATA, { useValue: { role: existingRole } });
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const updatedRole: Role = {
      ...existingRole,
      name: 'Updated Name',
      description: 'Updated Description'
    };

    mockRoleService.saveRole.and.returnValue(of(updatedRole));
    
    component.roleForm.patchValue({
      name: 'Updated Name',
      description: 'Updated Description'
    });
    
    component.save();
    
    const savedRoleArg = mockRoleService.saveRole.calls.mostRecent().args[0];
    expect(savedRoleArg.id).toEqual(existingRole.id);
    expect(savedRoleArg.name).toBe('Updated Name');
    expect(savedRoleArg.description).toBe('Updated Description');
  });

  it('should handle save errors gracefully', () => {
    fixture = TestBed.createComponent(RoleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    
    mockRoleService.saveRole.and.returnValue(throwError({ status: 500 }));
    
    component.roleForm.patchValue({
      name: 'Test Role',
      description: 'Test Description'
    });
    
    component.save();
    
    expect(mockRoleService.saveRole).toHaveBeenCalled();
    expect(mockDialogRef.close).not.toHaveBeenCalled();
  });
});
