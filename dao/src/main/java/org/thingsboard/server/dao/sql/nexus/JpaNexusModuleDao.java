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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NexusModuleEntity;
import org.thingsboard.server.dao.nexus.NexusModuleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of NexusModuleDao.
 */
@Component
@SqlDao
public class JpaNexusModuleDao extends JpaAbstractDao<NexusModuleEntity, NexusModule> implements NexusModuleDao {

    @Autowired
    private NexusModuleRepository moduleRepository;

    @Override
    protected Class<NexusModuleEntity> getEntityClass() {
        return NexusModuleEntity.class;
    }

    @Override
    protected JpaRepository<NexusModuleEntity, UUID> getRepository() {
        return moduleRepository;
    }

    @Override
    public NexusModule save(NexusModule module) {
        return save(TenantId.SYS_TENANT_ID, module);
    }

    @Override
    public NexusModule findById(UUID moduleId) {
        return DaoUtil.getData(moduleRepository.findById(moduleId));
    }

    @Override
    public Optional<NexusModule> findByModuleKey(String moduleKey) {
        return moduleRepository.findByModuleKey(moduleKey)
                .map(NexusModuleEntity::toData);
    }

    @Override
    public List<NexusModule> findAvailableModules() {
        return DaoUtil.convertDataList(moduleRepository.findAvailableModules());
    }

    @Override
    public List<NexusModule> findSystemModules() {
        return DaoUtil.convertDataList(moduleRepository.findSystemModules());
    }

    @Override
    public List<NexusModule> findByCategory(String category) {
        return DaoUtil.convertDataList(moduleRepository.findByCategory(category));
    }

    @Override
    public PageData<NexusModule> findAllModules(PageLink pageLink) {
        return DaoUtil.toPageData(moduleRepository.findAllModules(
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<NexusModule> findByIds(List<UUID> moduleIds) {
        return DaoUtil.convertDataList(moduleRepository.findByIdIn(moduleIds));
    }

    @Override
    public long countAvailableModules() {
        return moduleRepository.countAvailableModules();
    }

    @Override
    public boolean existsByModuleKey(String moduleKey) {
        return moduleRepository.existsByModuleKey(moduleKey);
    }

    @Override
    public boolean removeById(UUID moduleId) {
        if (moduleRepository.existsById(moduleId)) {
            moduleRepository.deleteById(moduleId);
            return true;
        }
        return false;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NEXUS_MODULE;
    }
}
