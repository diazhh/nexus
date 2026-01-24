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
package org.thingsboard.server.dao.sql.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
public class JpaRoleDao extends JpaAbstractDao<RoleEntity, Role> implements RoleDao {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected Class<RoleEntity> getEntityClass() {
        return RoleEntity.class;
    }

    @Override
    protected JpaRepository<RoleEntity, UUID> getRepository() {
        return roleRepository;
    }

    @Override
    public PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(roleRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Role> findRoleByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(DaoUtil.getData(roleRepository.findByTenantIdAndName(tenantId, name)));
    }

    @Override
    public List<Role> findSystemRoles() {
        return DaoUtil.convertDataList(roleRepository.findSystemRoles());
    }

    @Override
    public List<Role> findRolesByTenantIdAndIds(UUID tenantId, List<UUID> roleIds) {
        return DaoUtil.convertDataList(roleRepository.findByTenantIdAndIdIn(tenantId, roleIds));
    }

    @Override
    public long countUsersByRoleId(UUID roleId) {
        return roleRepository.countUsersByRoleId(roleId);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return roleRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }
}
