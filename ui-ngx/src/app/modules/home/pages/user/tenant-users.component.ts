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

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { User } from '@shared/models/user.model';
import { Role } from '@shared/models/role.models';
import { UserService } from '@core/http/user.service';
import { RoleService } from '@core/http/role.service';
import { PageLink } from '@shared/models/page/page-link';
import { AddUserDialogComponent, AddUserDialogData } from './add-user-dialog.component';
import { ChangeUserRoleDialogComponent, ChangeUserRoleDialogData } from './change-user-role-dialog.component';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';

@Component({
  selector: 'tb-tenant-users',
  templateUrl: './tenant-users.component.html',
  styleUrls: ['./tenant-users.component.scss']
})
export class TenantUsersComponent implements OnInit {

  displayedColumns: string[] = ['email', 'name', 'role', 'customer', 'createdTime', 'actions'];
  dataSource: MatTableDataSource<User>;
  roles: Role[] = [];
  selectedRoleFilter: string = null;
  loading = false;
  tenantId: string;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private store: Store<AppState>,
    private userService: UserService,
    private roleService: RoleService,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private translate: TranslateService,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<User>([]);
  }

  ngOnInit(): void {
    const authUser = getCurrentAuthUser(this.store);
    if (authUser && authUser.tenantId) {
      this.tenantId = authUser.tenantId;
      this.loadRoles();
      this.loadUsers();
    }

    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadRoles(): void {
    this.roleService.getRoles(new PageLink(100)).subscribe(
      rolesPage => {
        this.roles = rolesPage.data;
      }
    );
  }

  loadUsers(): void {
    this.loading = true;
    const pageLink = new PageLink(100);
    
    if (this.selectedRoleFilter) {
      this.userService.getUsersByRole(this.selectedRoleFilter, pageLink).subscribe(
        usersPage => {
          this.dataSource.data = usersPage.data;
          this.loading = false;
        },
        () => {
          this.loading = false;
        }
      );
    } else {
      // Usar endpoint /api/users que sí permite TENANT_ADMIN
      this.userService.getUsers(pageLink).subscribe(
        usersPage => {
          this.dataSource.data = usersPage.data;
          this.loading = false;
        },
        () => {
          this.loading = false;
        }
      );
    }
  }

  onRoleFilterChange(): void {
    this.loadUsers();
  }

  addUser(): void {
    this.dialog.open<AddUserDialogComponent, AddUserDialogData, User>(
      AddUserDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          tenantId: this.tenantId,
          customerId: null,
          authority: Authority.TENANT_ADMIN,
          allowTenantUser: true
        }
      }
    ).afterClosed().subscribe(
      (user) => {
        if (user) {
          this.loadUsers();
        }
      }
    );
  }

  editUser(user: User): void {
    this.router.navigate(['/users', user.id.id]);
  }

  changeRole(user: User): void {
    const currentRoleId = user.additionalInfo?.roleId;
    
    this.dialog.open<ChangeUserRoleDialogComponent, ChangeUserRoleDialogData, User>(
      ChangeUserRoleDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          user,
          currentRoleId
        }
      }
    ).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.loadUsers();
        }
      }
    );
  }

  deleteUser(user: User): void {
    this.dialogService.confirm(
      this.translate.instant('user.delete-user-title', {userEmail: user.email}),
      this.translate.instant('user.delete-user-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe(
      (result) => {
        if (result) {
          this.userService.deleteUser(user.id.id).subscribe(
            () => {
              this.loadUsers();
            }
          );
        }
      }
    );
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}
