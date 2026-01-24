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

import { TestBed } from '@angular/core/testing';
import { RolesTableConfigResolver } from './roles-table-config.resolver';
import { Store } from '@ngrx/store';
import { RoleService } from '@core/http/role.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { Role } from '@shared/models/role.models';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityType } from '@shared/models/entity-type.models';

describe('RolesTableConfigResolver', () => {
  let resolver: RolesTableConfigResolver;
  let mockStore: jasmine.SpyObj<Store>;
  let mockRoleService: jasmine.SpyObj<RoleService>;
  let mockTranslate: jasmine.SpyObj<TranslateService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let datePipe: DatePipe;

  const testRole: Role = {
    id: { id: '123' } as RoleId,
    tenantId: { id: 'tenant1' } as TenantId,
    name: 'Test Role',
    description: 'Test Description',
    isSystem: false,
    version: 1,
    createdTime: Date.now()
  };

  beforeEach(() => {
    mockStore = jasmine.createSpyObj('Store', ['dispatch', 'select']);
    mockRoleService = jasmine.createSpyObj('RoleService', ['getRoles', 'getRole', 'saveRole', 'deleteRole']);
    mockTranslate = jasmine.createSpyObj('TranslateService', ['instant']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    datePipe = new DatePipe('en-US');

    mockTranslate.instant.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      providers: [
        RolesTableConfigResolver,
        { provide: Store, useValue: mockStore },
        { provide: RoleService, useValue: mockRoleService },
        { provide: TranslateService, useValue: mockTranslate },
        { provide: DatePipe, useValue: datePipe },
        { provide: Router, useValue: mockRouter },
        { provide: MatDialog, useValue: mockDialog }
      ]
    });

    resolver = TestBed.inject(RolesTableConfigResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });

  it('should resolve entity table config', () => {
    const config = resolver.resolve();
    
    expect(config).toBeDefined();
    expect(config.entityType).toBe(EntityType.ROLE);
    expect(config.columns.length).toBeGreaterThan(0);
  });

  it('should configure table columns', () => {
    const config = resolver.resolve();
    
    const columnNames = config.columns.map(col => col.key);
    expect(columnNames).toContain('createdTime');
    expect(columnNames).toContain('name');
    expect(columnNames).toContain('description');
    expect(columnNames).toContain('isSystem');
  });

  it('should enable delete for non-system roles', () => {
    const config = resolver.resolve();
    
    const nonSystemRole: Role = { ...testRole, isSystem: false };
    const systemRole: Role = { ...testRole, isSystem: true };
    
    expect(config.deleteEnabled(nonSystemRole)).toBeTruthy();
    expect(config.deleteEnabled(systemRole)).toBeFalsy();
  });

  it('should configure load entity function', () => {
    const config = resolver.resolve();
    mockRoleService.getRole.and.returnValue(of(testRole));
    
    config.loadEntity(testRole.id);
    
    expect(mockRoleService.getRole).toHaveBeenCalledWith(testRole.id.id);
  });

  it('should configure save entity function', () => {
    const config = resolver.resolve();
    mockRoleService.saveRole.and.returnValue(of(testRole));
    
    config.saveEntity(testRole);
    
    expect(mockRoleService.saveRole).toHaveBeenCalledWith(testRole);
  });

  it('should configure delete entity function', () => {
    const config = resolver.resolve();
    mockRoleService.deleteRole.and.returnValue(of(undefined));
    
    config.deleteEntity(testRole.id);
    
    expect(mockRoleService.deleteRole).toHaveBeenCalledWith(testRole.id.id);
  });

  it('should configure entities fetch function', () => {
    const config = resolver.resolve();
    const pageLink = new PageLink(10, 0);
    const mockPageData: PageData<Role> = {
      data: [testRole],
      totalPages: 1,
      totalElements: 1,
      hasNext: false
    };
    
    mockRoleService.getRoles.and.returnValue(of(mockPageData));
    
    config.entitiesFetchFunction(pageLink).subscribe(result => {
      expect(result).toEqual(mockPageData);
    });
    
    expect(mockRoleService.getRoles).toHaveBeenCalledWith(pageLink);
  });

  it('should enable add action', () => {
    const config = resolver.resolve();
    
    expect(config.addEnabled).toBeTruthy();
    expect(config.addActionDescriptors.length).toBeGreaterThan(0);
  });

  it('should configure cell actions for manage permissions', () => {
    const config = resolver.resolve();
    
    expect(config.cellActionDescriptors.length).toBeGreaterThan(0);
    const managePermissionsAction = config.cellActionDescriptors.find(
      action => action.name === 'role.manage-permissions'
    );
    expect(managePermissionsAction).toBeDefined();
  });

  it('should generate entity title from role name', () => {
    const config = resolver.resolve();
    
    const title = config.entityTitle(testRole);
    expect(title).toBe('Test Role');
    
    const emptyTitle = config.entityTitle(null);
    expect(emptyTitle).toBe('');
  });

  it('should enable entity selection for non-system roles', () => {
    const config = resolver.resolve();
    
    const nonSystemRole: Role = { ...testRole, isSystem: false };
    const systemRole: Role = { ...testRole, isSystem: true };
    
    expect(config.entitySelectionEnabled(nonSystemRole)).toBeTruthy();
    expect(config.entitySelectionEnabled(systemRole)).toBeFalsy();
  });

  it('should configure delete entity title', () => {
    const config = resolver.resolve();
    
    const title = config.deleteEntityTitle(testRole);
    expect(mockTranslate.instant).toHaveBeenCalledWith('role.delete-role-title', { roleName: testRole.name });
  });

  it('should configure delete entities title', () => {
    const config = resolver.resolve();
    
    const title = config.deleteEntitiesTitle(5);
    expect(mockTranslate.instant).toHaveBeenCalledWith('role.delete-roles-title', { count: 5 });
  });
});
