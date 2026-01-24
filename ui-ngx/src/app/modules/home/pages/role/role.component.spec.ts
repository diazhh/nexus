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
import { RoleComponent } from './role.component';
import { UntypedFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import { of } from 'rxjs';
import { Role } from '@shared/models/role.models';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { ChangeDetectorRef } from '@angular/core';

describe('RoleComponent', () => {
  let component: RoleComponent;
  let fixture: ComponentFixture<RoleComponent>;
  let mockStore: jasmine.SpyObj<Store>;
  let mockCd: jasmine.SpyObj<ChangeDetectorRef>;

  beforeEach(async () => {
    mockStore = jasmine.createSpyObj('Store', ['dispatch', 'select']);
    mockCd = jasmine.createSpyObj('ChangeDetectorRef', ['detectChanges', 'markForCheck']);

    await TestBed.configureTestingModule({
      declarations: [RoleComponent],
      imports: [ReactiveFormsModule],
      providers: [
        UntypedFormBuilder,
        { provide: Store, useValue: mockStore },
        { provide: ChangeDetectorRef, useValue: mockCd },
        { provide: 'entity', useValue: null },
        { provide: 'entitiesTableConfig', useValue: null }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RoleComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should build form with empty values when no entity provided', () => {
    const form = component.buildForm(null);
    
    expect(form).toBeDefined();
    expect(form.get('name').value).toBe('');
    expect(form.get('description').value).toBe('');
    expect(form.get('isSystem').value).toBe(false);
  });

  it('should build form with entity values when entity provided', () => {
    const testRole: Role = {
      id: { id: '123' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'Test Role',
      description: 'Test Description',
      isSystem: false,
      version: 1,
      createdTime: Date.now()
    };

    const form = component.buildForm(testRole);
    
    expect(form.get('name').value).toBe('Test Role');
    expect(form.get('description').value).toBe('Test Description');
    expect(form.get('isSystem').value).toBe(false);
  });

  it('should validate name as required', () => {
    const form = component.buildForm(null);
    const nameControl = form.get('name');
    
    nameControl.setValue('');
    expect(nameControl.invalid).toBeTruthy();
    expect(nameControl.errors.required).toBeTruthy();
    
    nameControl.setValue('Valid Name');
    expect(nameControl.valid).toBeTruthy();
  });

  it('should validate name max length', () => {
    const form = component.buildForm(null);
    const nameControl = form.get('name');
    
    const longName = 'a'.repeat(256);
    nameControl.setValue(longName);
    expect(nameControl.invalid).toBeTruthy();
    expect(nameControl.errors.maxlength).toBeTruthy();
    
    const validName = 'a'.repeat(255);
    nameControl.setValue(validName);
    expect(nameControl.valid).toBeTruthy();
  });

  it('should validate description max length', () => {
    const form = component.buildForm(null);
    const descControl = form.get('description');
    
    const longDesc = 'a'.repeat(1025);
    descControl.setValue(longDesc);
    expect(descControl.invalid).toBeTruthy();
    expect(descControl.errors.maxlength).toBeTruthy();
    
    const validDesc = 'a'.repeat(1024);
    descControl.setValue(validDesc);
    expect(descControl.valid).toBeTruthy();
  });

  it('should have isSystem field disabled', () => {
    const form = component.buildForm(null);
    const isSystemControl = form.get('isSystem');
    
    expect(isSystemControl.disabled).toBeTruthy();
  });

  it('should update form with entity data', () => {
    const testRole: Role = {
      id: { id: '456' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'Updated Role',
      description: 'Updated Description',
      isSystem: true,
      version: 1,
      createdTime: Date.now()
    };

    component.entityForm = component.buildForm(null);
    component.updateForm(testRole);
    
    expect(component.entityForm.get('name').value).toBe('Updated Role');
    expect(component.entityForm.get('description').value).toBe('Updated Description');
    expect(component.entityForm.get('isSystem').value).toBe(true);
  });

  it('should allow empty description', () => {
    const form = component.buildForm(null);
    const descControl = form.get('description');
    
    descControl.setValue('');
    expect(descControl.valid).toBeTruthy();
  });

  it('should preserve isSystem value when building form', () => {
    const systemRole: Role = {
      id: { id: '789' } as RoleId,
      tenantId: { id: 'tenant1' } as TenantId,
      name: 'System Role',
      description: 'System Description',
      isSystem: true,
      version: 1,
      createdTime: Date.now()
    };

    const form = component.buildForm(systemRole);
    expect(form.get('isSystem').value).toBe(true);
  });
});
