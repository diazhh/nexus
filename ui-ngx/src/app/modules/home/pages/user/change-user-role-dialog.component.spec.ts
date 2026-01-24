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
import { ChangeUserRoleDialogComponent, ChangeUserRoleDialogData } from './change-user-role-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { UserService } from '@core/http/user.service';
import { RoleService } from '@core/http/role.service';
import { of, throwError } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { PageData } from '@shared/models/page/page-data';
import { User } from '@shared/models/user.model';
import { Role } from '@shared/models/role.models';

describe('ChangeUserRoleDialogComponent', () => {
  let component: ChangeUserRoleDialogComponent;
  let fixture: ComponentFixture<ChangeUserRoleDialogComponent>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<ChangeUserRoleDialogComponent>>;
  let mockUserService: jasmine.SpyObj<UserService>;
  let mockRoleService: jasmine.SpyObj<RoleService>;
  let mockStore: jasmine.SpyObj<Store>;
  let mockRouter: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: { id: 'user-123', entityType: 'USER' },
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    authority: Authority.TENANT_ADMIN,
    createdTime: 123456789,
    additionalInfo: { roleId: 'role-1', roleName: 'Admin Role' }
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

  const mockDialogData: ChangeUserRoleDialogData = {
    user: mockUser,
    currentRoleId: 'role-1'
  };

  beforeEach(async () => {
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    mockUserService = jasmine.createSpyObj('UserService', ['changeUserRole']);
    mockRoleService = jasmine.createSpyObj('RoleService', ['getRoles']);
    mockStore = jasmine.createSpyObj('Store', ['select', 'dispatch']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [ChangeUserRoleDialogComponent],
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
    fixture = TestBed.createComponent(ChangeUserRoleDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with user and currentRoleId from dialog data', () => {
    expect(component.user).toEqual(mockUser);
    expect(component.currentRoleId).toBe('role-1');
  });

  it('should load roles on init', () => {
    component.ngOnInit();
    expect(mockRoleService.getRoles).toHaveBeenCalled();
    expect(component.roles).toEqual(mockRoles);
    expect(component.loadingRoles).toBe(false);
  });

  it('should preselect current role if currentRoleId is provided', () => {
    component.ngOnInit();
    expect(component.selectedRole).toEqual(mockRoles[0]);
  });

  it('should not preselect role if currentRoleId is not provided', () => {
    component.currentRoleId = null;
    component.ngOnInit();
    expect(component.selectedRole).toBeNull();
  });

  it('should handle role loading error', () => {
    mockRoleService.getRoles.and.returnValue(throwError(() => new Error('Load error')));
    component.ngOnInit();
    expect(component.loadingRoles).toBe(false);
  });

  it('should set loadingRoles to true while loading', () => {
    component.loadRoles();
    expect(component.loadingRoles).toBe(true);
  });

  it('should close dialog on cancel', () => {
    component.cancel();
    expect(mockDialogRef.close).toHaveBeenCalledWith(null);
  });

  it('should detect role is different when new role is selected', () => {
    component.currentRoleId = 'role-1';
    component.selectedRole = mockRoles[1];
    expect(component.isRoleDifferent()).toBe(true);
  });

  it('should detect role is same when same role is selected', () => {
    component.currentRoleId = 'role-1';
    component.selectedRole = mockRoles[0];
    expect(component.isRoleDifferent()).toBe(false);
  });

  it('should detect role is different when no current role exists', () => {
    component.currentRoleId = null;
    component.selectedRole = mockRoles[0];
    expect(component.isRoleDifferent()).toBe(true);
  });

  it('should validate form requires selected role and different role', () => {
    component.selectedRole = null;
    expect(component.isFormValid()).toBe(false);

    component.selectedRole = mockRoles[0];
    component.currentRoleId = 'role-1';
    expect(component.isFormValid()).toBe(false);

    component.selectedRole = mockRoles[1];
    expect(component.isFormValid()).toBe(true);
  });

  it('should change user role successfully', () => {
    mockUserService.changeUserRole.and.returnValue(of(null));
    component.selectedRole = mockRoles[1];
    component.currentRoleId = 'role-1';

    component.changeRole();

    expect(mockUserService.changeUserRole).toHaveBeenCalledWith('user-123', 'role-2');
    expect(mockDialogRef.close).toHaveBeenCalledWith(mockUser);
  });

  it('should not change role if selected role is same as current', () => {
    component.selectedRole = mockRoles[0];
    component.currentRoleId = 'role-1';

    component.changeRole();

    expect(mockUserService.changeUserRole).not.toHaveBeenCalled();
    expect(mockDialogRef.close).not.toHaveBeenCalled();
  });

  it('should not change role if no role is selected', () => {
    component.selectedRole = null;
    component.currentRoleId = 'role-1';

    component.changeRole();

    expect(mockUserService.changeUserRole).not.toHaveBeenCalled();
    expect(mockDialogRef.close).not.toHaveBeenCalled();
  });

  it('should handle role change error gracefully', () => {
    mockUserService.changeUserRole.and.returnValue(throwError(() => new Error('Change error')));
    component.selectedRole = mockRoles[1];
    component.currentRoleId = 'role-1';

    component.changeRole();

    expect(mockUserService.changeUserRole).toHaveBeenCalled();
  });
});
