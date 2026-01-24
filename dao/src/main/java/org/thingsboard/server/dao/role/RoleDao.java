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

import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleDao extends Dao<Role>, TenantEntityDao<Role> {

    Role save(TenantId tenantId, Role role);

    PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink);

    Optional<Role> findRoleByTenantIdAndName(UUID tenantId, String name);

    List<Role> findSystemRoles();

    List<Role> findRolesByTenantIdAndIds(UUID tenantId, List<UUID> roleIds);

    long countUsersByRoleId(UUID roleId);
}
