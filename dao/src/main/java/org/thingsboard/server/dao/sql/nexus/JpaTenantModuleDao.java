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
package org.thingsboard.server.dao.sql.nexus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.TenantModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantModuleEntity;
import org.thingsboard.server.dao.nexus.TenantModuleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of TenantModuleDao.
 */
@Component
@SqlDao
public class JpaTenantModuleDao extends JpaAbstractDao<TenantModuleEntity, TenantModule> implements TenantModuleDao {

    @Autowired
    private TenantModuleRepository tenantModuleRepository;

    @Override
    protected Class<TenantModuleEntity> getEntityClass() {
        return TenantModuleEntity.class;
    }

    @Override
    protected JpaRepository<TenantModuleEntity, UUID> getRepository() {
        return tenantModuleRepository;
    }

    @Override
    public TenantModule save(TenantModule tenantModule) {
        return save(tenantModule.getTenantId(), tenantModule);
    }

    @Override
    public TenantModule findById(UUID id) {
        return DaoUtil.getData(tenantModuleRepository.findById(id));
    }

    @Override
    public List<TenantModule> findByTenantId(TenantId tenantId) {
        return DaoUtil.convertDataList(tenantModuleRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public List<TenantModule> findActiveByTenantId(TenantId tenantId) {
        return DaoUtil.convertDataList(tenantModuleRepository.findActiveByTenantId(tenantId.getId()));
    }

    @Override
    public Optional<TenantModule> findByTenantIdAndModuleId(TenantId tenantId, NexusModuleId moduleId) {
        return tenantModuleRepository.findByTenantIdAndModuleId(tenantId.getId(), moduleId.getId())
                .map(TenantModuleEntity::toData);
    }

    @Override
    public boolean isTenantModuleActive(TenantId tenantId, NexusModuleId moduleId) {
        return tenantModuleRepository.isTenantModuleActive(tenantId.getId(), moduleId.getId());
    }

    @Override
    public List<TenantModule> findTenantsByModuleId(NexusModuleId moduleId) {
        return DaoUtil.convertDataList(tenantModuleRepository.findTenantsByModuleId(moduleId.getId()));
    }

    @Override
    public long countActiveModulesByTenantId(TenantId tenantId) {
        return tenantModuleRepository.countActiveModulesByTenantId(tenantId.getId());
    }

    @Override
    public long countTenantsByModuleId(NexusModuleId moduleId) {
        return tenantModuleRepository.countTenantsByModuleId(moduleId.getId());
    }

    @Override
    public PageData<TenantModule> findByTenantIdPaged(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantModuleRepository.findByTenantIdPaged(
                tenantId.getId(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    @Transactional
    public int deactivateAllModulesForTenant(TenantId tenantId) {
        return tenantModuleRepository.deactivateAllModulesForTenant(
                tenantId.getId(),
                System.currentTimeMillis());
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        tenantModuleRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public boolean existsByTenantIdAndModuleId(TenantId tenantId, NexusModuleId moduleId) {
        return tenantModuleRepository.existsByTenantIdAndModuleId(tenantId.getId(), moduleId.getId());
    }

    @Override
    public List<String> findActiveModuleKeysByTenantId(TenantId tenantId) {
        return tenantModuleRepository.findActiveModuleKeysByTenantId(tenantId.getId());
    }

    @Override
    public boolean removeById(UUID id) {
        if (tenantModuleRepository.existsById(id)) {
            tenantModuleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT_MODULE;
    }
}
