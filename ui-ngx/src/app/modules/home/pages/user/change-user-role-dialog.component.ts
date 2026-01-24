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
import { User } from '@shared/models/user.model';
import { Role } from '@shared/models/role.models';
import { UserService } from '@core/http/user.service';
import { RoleService } from '@core/http/role.service';
import { PageLink } from '@shared/models/page/page-link';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface ChangeUserRoleDialogData {
  user: User;
  currentRoleId?: string;
}

@Component({
  selector: 'tb-change-user-role-dialog',
  templateUrl: './change-user-role-dialog.component.html',
  styleUrls: ['./change-user-role-dialog.component.scss']
})
export class ChangeUserRoleDialogComponent extends DialogComponent<ChangeUserRoleDialogComponent, User> implements OnInit {

  roles: Role[] = [];
  selectedRole: Role = null;
  loadingRoles = false;
  user: User;
  currentRoleId: string;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: ChangeUserRoleDialogData,
    public dialogRef: MatDialogRef<ChangeUserRoleDialogComponent, User>,
    private userService: UserService,
    private roleService: RoleService
  ) {
    super(store, router, dialogRef);
    this.user = data.user;
    this.currentRoleId = data.currentRoleId;
  }

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.loadingRoles = true;
    this.roleService.getRoles(new PageLink(100)).subscribe(
      (rolesPage) => {
        this.roles = rolesPage.data;
        if (this.currentRoleId) {
          this.selectedRole = this.roles.find(r => r.id.id === this.currentRoleId);
        }
        this.loadingRoles = false;
      },
      () => {
        this.loadingRoles = false;
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  changeRole(): void {
    if (this.selectedRole && this.isRoleDifferent()) {
      this.userService.changeUserRole(this.user.id.id, this.selectedRole.id.id).subscribe(
        () => {
          this.dialogRef.close(this.user);
        }
      );
    }
  }

  isRoleDifferent(): boolean {
    if (!this.currentRoleId && this.selectedRole) {
      return true;
    }
    return this.selectedRole && this.selectedRole.id.id !== this.currentRoleId;
  }

  isFormValid(): boolean {
    return this.selectedRole !== null && this.isRoleDifferent();
  }
}
