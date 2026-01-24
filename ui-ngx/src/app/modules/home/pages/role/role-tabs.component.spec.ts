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
import { RoleTabsComponent } from './role-tabs.component';
import { Store } from '@ngrx/store';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('RoleTabsComponent', () => {
  let component: RoleTabsComponent;
  let fixture: ComponentFixture<RoleTabsComponent>;
  let mockStore: jasmine.SpyObj<Store>;

  beforeEach(async () => {
    mockStore = jasmine.createSpyObj('Store', ['dispatch', 'select']);

    await TestBed.configureTestingModule({
      declarations: [RoleTabsComponent],
      providers: [
        { provide: Store, useValue: mockStore }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(RoleTabsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call ngOnInit', () => {
    spyOn(component, 'ngOnInit').and.callThrough();
    component.ngOnInit();
    expect(component.ngOnInit).toHaveBeenCalled();
  });
});
