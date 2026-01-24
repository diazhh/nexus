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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Role } from '@shared/models/role.models';
import { RoleService } from '@core/http/role.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface RoleDialogData {
  role: Role;
}

@Component({
  selector: 'tb-role-dialog',
  templateUrl: './role-dialog.component.html',
  styleUrls: ['./role-dialog.component.scss']
})
export class RoleDialogComponent extends DialogComponent<RoleDialogComponent, Role> implements OnInit {

  roleForm: UntypedFormGroup;
  role: Role;
  isAdd: boolean;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: RoleDialogData,
              public dialogRef: MatDialogRef<RoleDialogComponent, Role>,
              private fb: UntypedFormBuilder,
              private roleService: RoleService) {
    super(store, router, dialogRef);
    this.role = data.role;
    this.isAdd = !this.role || !this.role.id;
  }

  ngOnInit(): void {
    this.roleForm = this.fb.group({
      name: [this.role ? this.role.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [this.role ? this.role.description : '', [Validators.maxLength(1024)]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    if (this.roleForm.valid) {
      const role: Role = {
        ...this.role,
        ...this.roleForm.value
      };
      this.roleService.saveRole(role).subscribe(
        (savedRole) => {
          this.dialogRef.close(savedRole);
        }
      );
    }
  }
}
