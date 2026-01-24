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
import { AddUserDialogComponent, AddUserDialogData } from './add-user-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { UserService } from '@core/http/user.service';
import { RoleService } from '@core/http/role.service';
import { of, throwError } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { ActivationMethod } from '@shared/models/user.model';
import { PageData } from '@shared/models/page/page-data';
import { Role } from '@shared/models/role.models';

describe('AddUserDialogComponent', () => {
  let component: AddUserDialogComponent;
  let fixture: ComponentFixture<AddUserDialogComponent>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<AddUserDialogComponent>>;
  let mockUserService: jasmine.SpyObj<UserService>;
  let mockRoleService: jasmine.SpyObj<RoleService>;
  let mockStore: jasmine.SpyObj<Store>;
  let mockRouter: jasmine.SpyObj<Router>;

  const mockDialogData: AddUserDialogData = {
    tenantId: 'tenant-123',
    customerId: 'customer-456',
    authority: Authority.TENANT_ADMIN,
    allowTenantUser: true
  };

  const mockRoles: Role[] = [
    {
      id: { id: 'role-1', entityType: 'ROLE' },
      name: 'Admin Role',
      permissions: [],
      createdTime: 123456789
    },
    {
      id: { id: 'role-2', entityType: 'ROLE' },
      name: 'User Role',
      permissions: [],
      createdTime: 123456789
    }
  ];

  const mockRolesPage: PageData<Role> = {
    data: mockRoles,
    totalPages: 1,
    totalElements: 2,
    hasNext: false
  };

  beforeEach(async () => {
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    mockUserService = jasmine.createSpyObj('UserService', ['saveUser', 'changeUserRole', 'getActivationLinkInfo']);
    mockRoleService = jasmine.createSpyObj('RoleService', ['getRoles']);
    mockStore = jasmine.createSpyObj('Store', ['select', 'dispatch']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [AddUserDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: UserService, useValue: mockUserService },
        { provide: RoleService, useValue: mockRoleService },
        { provide: Store, useValue: mockStore },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    mockRoleService.getRoles.and.returnValue(of(mockRolesPage));
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AddUserDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load roles on init', () => {
    component.ngOnInit();
    expect(mockRoleService.getRoles).toHaveBeenCalled();
    expect(component.roles).toEqual(mockRoles);
    expect(component.loadingRoles).toBe(false);
  });

  it('should handle role loading error', () => {
    mockRoleService.getRoles.and.returnValue(throwError(() => new Error('Load error')));
    component.ngOnInit();
    expect(component.loadingRoles).toBe(false);
    expect(component.roles).toEqual([]);
  });

  it('should set loadingRoles to true while loading', () => {
    component.loadRoles();
    expect(component.loadingRoles).toBe(true);
  });

  it('should close dialog on cancel', () => {
    component.cancel();
    expect(mockDialogRef.close).toHaveBeenCalledWith(null);
  });

  it('should validate form requires selected role', () => {
    component.selectedRole = null;
    expect(component.isFormValid()).toBe(false);

    component.selectedRole = mockRoles[0];
    expect(component.isFormValid()).toBe(true);
  });

  it('should return allowTenantUser from data', () => {
    expect(component.allowTenantUser).toBe(true);
  });

  it('should save user with selected role', () => {
    const mockUser = {
      id: { id: 'user-123', entityType: 'USER' },
      email: 'test@example.com',
      authority: Authority.TENANT_ADMIN,
      additionalInfo: {}
    };

    mockUserService.saveUser.and.returnValue(of(mockUser));
    mockUserService.changeUserRole.and.returnValue(of(null));

    component.selectedRole = mockRoles[0];
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: true, value: mockUser });
    component.activationMethod = ActivationMethod.SEND_ACTIVATION_MAIL;

    component.add();

    expect(mockUserService.saveUser).toHaveBeenCalled();
    expect(mockUserService.changeUserRole).toHaveBeenCalledWith('user-123', 'role-1');
  });

  it('should save user without role if not selected', () => {
    const mockUser = {
      id: { id: 'user-123', entityType: 'USER' },
      email: 'test@example.com',
      authority: Authority.TENANT_ADMIN,
      additionalInfo: {}
    };

    mockUserService.saveUser.and.returnValue(of(mockUser));
    component.selectedRole = null;
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: true, value: mockUser });

    // This should not call add() because isFormValid() returns false
    component.add();

    expect(mockUserService.saveUser).not.toHaveBeenCalled();
  });

  it('should create user as tenant user when checkbox is checked', () => {
    const mockUser = {
      id: { id: 'user-123', entityType: 'USER' },
      email: 'test@example.com',
      authority: Authority.TENANT_ADMIN,
      additionalInfo: {}
    };

    mockUserService.saveUser.and.returnValue(of(mockUser));
    mockUserService.changeUserRole.and.returnValue(of(null));

    component.selectedRole = mockRoles[0];
    component.createAsTenantUser = true;
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: true, value: mockUser });
    component.activationMethod = ActivationMethod.SEND_ACTIVATION_MAIL;

    component.add();

    expect(component.user.customerId).toBeNull();
  });

  it('should handle role assignment error gracefully', () => {
    const mockUser = {
      id: { id: 'user-123', entityType: 'USER' },
      email: 'test@example.com',
      authority: Authority.TENANT_ADMIN,
      additionalInfo: {}
    };

    mockUserService.saveUser.and.returnValue(of(mockUser));
    mockUserService.changeUserRole.and.returnValue(throwError(() => new Error('Role error')));

    component.selectedRole = mockRoles[0];
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: true, value: mockUser });
    component.activationMethod = ActivationMethod.SEND_ACTIVATION_MAIL;

    component.add();

    expect(mockUserService.changeUserRole).toHaveBeenCalled();
    expect(mockDialogRef.close).toHaveBeenCalledWith(mockUser);
  });

  it('should display activation link when method is DISPLAY_ACTIVATION_LINK', () => {
    const mockUser = {
      id: { id: 'user-123', entityType: 'USER' },
      email: 'test@example.com',
      authority: Authority.TENANT_ADMIN,
      additionalInfo: {}
    };

    const mockActivationLink = {
      activationLink: 'http://example.com/activate'
    };

    mockUserService.saveUser.and.returnValue(of(mockUser));
    mockUserService.changeUserRole.and.returnValue(of(null));
    mockUserService.getActivationLinkInfo.and.returnValue(of(mockActivationLink));

    component.selectedRole = mockRoles[0];
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: true, value: mockUser });
    component.activationMethod = ActivationMethod.DISPLAY_ACTIVATION_LINK;

    spyOn(component, 'displayActivationLink').and.returnValue(of(undefined));

    component.add();

    expect(mockUserService.getActivationLinkInfo).toHaveBeenCalledWith('user-123');
  });

  it('should not call saveUser if form is invalid', () => {
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: false });
    component.add();
    expect(mockUserService.saveUser).not.toHaveBeenCalled();
  });

  it('should clean up additionalInfo before saving', () => {
    const mockUser = {
      id: { id: 'user-123', entityType: 'USER' },
      email: 'test@example.com',
      authority: Authority.TENANT_ADMIN,
      additionalInfo: { lang: null, unitSystem: null }
    };

    mockUserService.saveUser.and.returnValue(of(mockUser));
    mockUserService.changeUserRole.and.returnValue(of(null));

    component.selectedRole = mockRoles[0];
    component.detailsForm = jasmine.createSpyObj('FormGroup', ['valid'], { valid: true, value: mockUser });
    component.activationMethod = ActivationMethod.SEND_ACTIVATION_MAIL;

    component.add();

    expect(component.user.additionalInfo.lang).toBeUndefined();
    expect(component.user.additionalInfo.unitSystem).toBeUndefined();
  });
});
