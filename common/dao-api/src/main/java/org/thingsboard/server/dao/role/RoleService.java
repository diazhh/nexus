/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Set;

public interface RoleService extends EntityDaoService {

    Role findRoleById(TenantId tenantId, RoleId roleId);

    ListenableFuture<Role> findRoleByIdAsync(TenantId tenantId, RoleId roleId);

    Role findRoleByTenantIdAndName(TenantId tenantId, String name);

    Role saveRole(Role role);

    Role saveRole(Role role, boolean doValidate);

    void deleteRole(TenantId tenantId, RoleId roleId);

    PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink);

    List<Role> findSystemRoles();

    List<Role> findRolesByTenantIdAndIds(TenantId tenantId, List<RoleId> roleIds);

    Set<RolePermission> getRolePermissions(RoleId roleId);

    void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions);

    void addRolePermissions(RoleId roleId, Set<RolePermission> permissions);

    void removeRolePermissions(RoleId roleId, Set<RolePermission> permissions);

    void createDefaultTenantRoles(TenantId tenantId);

    long countUsersByRoleId(RoleId roleId);

    void deleteRolesByTenantId(TenantId tenantId);
}
