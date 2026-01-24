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
import { TenantUsersComponent } from './tenant-users.component';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { UserService } from '@core/http/user.service';
import { RoleService } from '@core/http/role.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { User } from '@shared/models/user.model';
import { Role } from '@shared/models/role.models';
import { Authority } from '@shared/models/authority.enum';

describe('TenantUsersComponent', () => {
  let component: TenantUsersComponent;
  let fixture: ComponentFixture<TenantUsersComponent>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockStore: jasmine.SpyObj<Store>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockUserService: jasmine.SpyObj<UserService>;
  let mockRoleService: jasmine.SpyObj<RoleService>;
  let mockDialogService: jasmine.SpyObj<DialogService>;
  let mockTranslateService: jasmine.SpyObj<TranslateService>;

  const mockAuthUser = {
    tenantId: 'tenant-123',
    authority: Authority.TENANT_ADMIN
  };

  const mockUsers: User[] = [
    {
      id: { id: 'user-1', entityType: 'USER' },
      email: 'user1@example.com',
      firstName: 'John',
      lastName: 'Doe',
      authority: Authority.TENANT_ADMIN,
      createdTime: 123456789,
      additionalInfo: { roleId: 'role-1', roleName: 'Admin Role' }
    },
    {
      id: { id: 'user-2', entityType: 'USER' },
      email: 'user2@example.com',
      firstName: 'Jane',
      lastName: 'Smith',
      authority: Authority.TENANT_ADMIN,
      createdTime: 123456790,
      additionalInfo: { roleId: 'role-2', roleName: 'User Role' }
    }
  ];

  const mockUsersPage: PageData<User> = {
    data: mockUsers,
    totalPages: 1,
    totalElements: 2,
    hasNext: false
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
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockStore = jasmine.createSpyObj('Store', ['select', 'dispatch']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockUserService = jasmine.createSpyObj('UserService', ['getTenantAdmins', 'getUsersByRole', 'deleteUser']);
    mockRoleService = jasmine.createSpyObj('RoleService', ['getRoles']);
    mockDialogService = jasmine.createSpyObj('DialogService', ['confirm']);
    mockTranslateService = jasmine.createSpyObj('TranslateService', ['instant']);

    await TestBed.configureTestingModule({
      declarations: [TenantUsersComponent],
      providers: [
        { provide: MatDialog, useValue: mockDialog },
        { provide: Store, useValue: mockStore },
        { provide: Router, useValue: mockRouter },
        { provide: UserService, useValue: mockUserService },
        { provide: RoleService, useValue: mockRoleService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: TranslateService, useValue: mockTranslateService }
      ]
    }).compileComponents();

    mockUserService.getTenantAdmins.and.returnValue(of(mockUsersPage));
    mockRoleService.getRoles.and.returnValue(of(mockRolesPage));
    mockTranslateService.instant.and.returnValue('Translated text');
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TenantUsersComponent);
    component = fixture.componentInstance;
    component.tenantId = 'tenant-123';
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load roles on init', () => {
    component.ngOnInit();
    expect(mockRoleService.getRoles).toHaveBeenCalled();
    expect(component.roles).toEqual(mockRoles);
  });

  it('should load users on init', () => {
    component.ngOnInit();
    expect(mockUserService.getTenantAdmins).toHaveBeenCalledWith('tenant-123', jasmine.any(Object));
    expect(component.dataSource.data).toEqual(mockUsers);
    expect(component.loading).toBe(false);
  });

  it('should handle user loading error', () => {
    mockUserService.getTenantAdmins.and.returnValue(throwError(() => new Error('Load error')));
    component.ngOnInit();
    expect(component.loading).toBe(false);
  });

  it('should filter users by role when role filter is selected', () => {
    mockUserService.getUsersByRole.and.returnValue(of(mockUsersPage));
    component.selectedRoleFilter = 'role-1';
    component.loadUsers();
    
    expect(mockUserService.getUsersByRole).toHaveBeenCalledWith('role-1', jasmine.any(Object));
    expect(component.dataSource.data).toEqual(mockUsers);
  });

  it('should reload users when role filter changes', () => {
    spyOn(component, 'loadUsers');
    component.onRoleFilterChange();
    expect(component.loadUsers).toHaveBeenCalled();
  });

  it('should open add user dialog', () => {
    const mockDialogRef = {
      afterClosed: () => of(mockUsers[0])
    };
    mockDialog.open.and.returnValue(mockDialogRef as any);
    spyOn(component, 'loadUsers');

    component.addUser();

    expect(mockDialog.open).toHaveBeenCalled();
    expect(component.loadUsers).toHaveBeenCalled();
  });

  it('should not reload users if add user dialog is cancelled', () => {
    const mockDialogRef = {
      afterClosed: () => of(null)
    };
    mockDialog.open.and.returnValue(mockDialogRef as any);
    spyOn(component, 'loadUsers');

    component.addUser();

    expect(mockDialog.open).toHaveBeenCalled();
    expect(component.loadUsers).not.toHaveBeenCalled();
  });

  it('should navigate to user details on edit', () => {
    component.editUser(mockUsers[0]);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/users', 'user-1']);
  });

  it('should open change role dialog', () => {
    const mockDialogRef = {
      afterClosed: () => of(mockUsers[0])
    };
    mockDialog.open.and.returnValue(mockDialogRef as any);
    spyOn(component, 'loadUsers');

    component.changeRole(mockUsers[0]);

    expect(mockDialog.open).toHaveBeenCalled();
    expect(component.loadUsers).toHaveBeenCalled();
  });

  it('should not reload users if change role dialog is cancelled', () => {
    const mockDialogRef = {
      afterClosed: () => of(null)
    };
    mockDialog.open.and.returnValue(mockDialogRef as any);
    spyOn(component, 'loadUsers');

    component.changeRole(mockUsers[0]);

    expect(mockDialog.open).toHaveBeenCalled();
    expect(component.loadUsers).not.toHaveBeenCalled();
  });

  it('should delete user after confirmation', () => {
    mockDialogService.confirm.and.returnValue(of(true));
    mockUserService.deleteUser.and.returnValue(of(null));
    spyOn(component, 'loadUsers');

    component.deleteUser(mockUsers[0]);

    expect(mockDialogService.confirm).toHaveBeenCalled();
    expect(mockUserService.deleteUser).toHaveBeenCalledWith('user-1');
    expect(component.loadUsers).toHaveBeenCalled();
  });

  it('should not delete user if confirmation is cancelled', () => {
    mockDialogService.confirm.and.returnValue(of(false));
    spyOn(component, 'loadUsers');

    component.deleteUser(mockUsers[0]);

    expect(mockDialogService.confirm).toHaveBeenCalled();
    expect(mockUserService.deleteUser).not.toHaveBeenCalled();
    expect(component.loadUsers).not.toHaveBeenCalled();
  });

  it('should apply filter to data source', () => {
    component.dataSource.data = mockUsers;
    const event = { target: { value: 'john' } } as any;
    
    component.applyFilter(event);
    
    expect(component.dataSource.filter).toBe('john');
  });

  it('should reset paginator on filter', () => {
    component.dataSource.data = mockUsers;
    component.dataSource.paginator = jasmine.createSpyObj('MatPaginator', ['firstPage']);
    const event = { target: { value: 'test' } } as any;
    
    component.applyFilter(event);
    
    expect(component.dataSource.paginator.firstPage).toHaveBeenCalled();
  });

  it('should set loading to true while loading users', () => {
    component.loadUsers();
    expect(component.loading).toBe(true);
  });

  it('should display columns correctly', () => {
    expect(component.displayedColumns).toEqual(['email', 'name', 'role', 'customer', 'createdTime', 'actions']);
  });
});
