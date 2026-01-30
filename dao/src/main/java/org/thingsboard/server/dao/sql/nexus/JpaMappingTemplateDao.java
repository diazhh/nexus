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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.MappingTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.MappingTemplateEntity;
import org.thingsboard.server.dao.nexus.MappingTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of MappingTemplateDao.
 */
@Component
@SqlDao
public class JpaMappingTemplateDao extends JpaAbstractDao<MappingTemplateEntity, MappingTemplate> implements MappingTemplateDao {

    @Autowired
    private MappingTemplateRepository repository;

    @Override
    protected Class<MappingTemplateEntity> getEntityClass() {
        return MappingTemplateEntity.class;
    }

    @Override
    protected JpaRepository<MappingTemplateEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public MappingTemplate save(TenantId tenantId, MappingTemplate template) {
        return save(tenantId, template);
    }

    @Override
    public MappingTemplate findById(UUID templateId) {
        return DaoUtil.getData(repository.findById(templateId));
    }

    @Override
    public PageData<MappingTemplate> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<MappingTemplate> findByModuleKey(UUID tenantId, String moduleKey) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndModuleKey(tenantId, moduleKey));
    }

    @Override
    public PageData<MappingTemplate> findByModuleKey(UUID tenantId, String moduleKey, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantIdAndModuleKey(
                tenantId,
                moduleKey,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<MappingTemplate> findDefaultByModuleKey(UUID tenantId, String moduleKey) {
        return repository.findDefaultByTenantIdAndModuleKey(tenantId, moduleKey)
                .map(MappingTemplateEntity::toData);
    }

    @Override
    public List<MappingTemplate> findActiveByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(repository.findActiveByTenantId(tenantId));
    }

    @Override
    public List<MappingTemplate> findActiveByModuleKey(UUID tenantId, String moduleKey) {
        return DaoUtil.convertDataList(repository.findActiveByTenantIdAndModuleKey(tenantId, moduleKey));
    }

    @Override
    public Optional<MappingTemplate> findByName(UUID tenantId, String moduleKey, String name) {
        return repository.findByTenantIdAndModuleKeyAndName(tenantId, moduleKey, name)
                .map(MappingTemplateEntity::toData);
    }

    @Override
    public boolean existsByName(UUID tenantId, String moduleKey, String name) {
        return repository.existsByTenantIdAndModuleKeyAndName(tenantId, moduleKey, name);
    }

    @Override
    public boolean removeById(UUID templateId) {
        if (repository.existsById(templateId)) {
            repository.deleteById(templateId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteByTenantId(UUID tenantId) {
        repository.deleteByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MAPPING_TEMPLATE;
    }
}
