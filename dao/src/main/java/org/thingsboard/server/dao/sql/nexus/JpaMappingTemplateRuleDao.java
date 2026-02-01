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
import org.thingsboard.server.common.data.nexus.MappingTemplateRule;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.MappingTemplateRuleEntity;
import org.thingsboard.server.dao.nexus.MappingTemplateRuleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of MappingTemplateRuleDao.
 */
@Component
@SqlDao
public class JpaMappingTemplateRuleDao extends JpaAbstractDao<MappingTemplateRuleEntity, MappingTemplateRule> implements MappingTemplateRuleDao {

    @Autowired
    private MappingTemplateRuleRepository repository;

    @Override
    protected Class<MappingTemplateRuleEntity> getEntityClass() {
        return MappingTemplateRuleEntity.class;
    }

    @Override
    protected JpaRepository<MappingTemplateRuleEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public MappingTemplateRule save(TenantId tenantId, MappingTemplateRule rule) {
        return super.save(tenantId, rule);
    }

    @Override
    public MappingTemplateRule findById(UUID ruleId) {
        return DaoUtil.getData(repository.findById(ruleId));
    }

    @Override
    public List<MappingTemplateRule> findByTemplateId(UUID templateId) {
        return DaoUtil.convertDataList(repository.findByTemplateId(templateId));
    }

    @Override
    public List<MappingTemplateRule> findActiveByTemplateId(UUID templateId) {
        return DaoUtil.convertDataList(repository.findActiveByTemplateId(templateId));
    }

    @Override
    public List<MappingTemplateRule> findBySourceKey(UUID templateId, String sourceKey) {
        return DaoUtil.convertDataList(repository.findByTemplateIdAndSourceKey(templateId, sourceKey));
    }

    @Override
    public boolean existsBySourceKey(UUID templateId, String sourceKey) {
        return repository.existsByTemplateIdAndSourceKey(templateId, sourceKey);
    }

    @Override
    public boolean removeById(UUID ruleId) {
        if (repository.existsById(ruleId)) {
            repository.deleteById(ruleId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteByTemplateId(UUID templateId) {
        repository.deleteByTemplateId(templateId);
    }

    @Override
    public long countByTemplateId(UUID templateId) {
        return repository.countByTemplateId(templateId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MAPPING_TEMPLATE_RULE;
    }
}
