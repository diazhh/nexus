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
import org.thingsboard.server.common.data.nexus.DataMappingRule;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DataMappingRuleEntity;
import org.thingsboard.server.dao.nexus.DataMappingRuleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of DataMappingRuleDao.
 */
@Component
@SqlDao
public class JpaDataMappingRuleDao extends JpaAbstractDao<DataMappingRuleEntity, DataMappingRule> implements DataMappingRuleDao {

    @Autowired
    private DataMappingRuleRepository repository;

    @Override
    protected Class<DataMappingRuleEntity> getEntityClass() {
        return DataMappingRuleEntity.class;
    }

    @Override
    protected JpaRepository<DataMappingRuleEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public DataMappingRule save(TenantId tenantId, DataMappingRule rule) {
        return super.save(tenantId, rule);
    }

    @Override
    public DataMappingRule findById(UUID ruleId) {
        return DaoUtil.getData(repository.findById(ruleId));
    }

    @Override
    public List<DataMappingRule> findByDataSourceConfigId(UUID dataSourceConfigId) {
        return DaoUtil.convertDataList(repository.findByDataSourceConfigId(dataSourceConfigId));
    }

    @Override
    public List<DataMappingRule> findActiveByDataSourceConfigId(UUID dataSourceConfigId) {
        return DaoUtil.convertDataList(repository.findActiveByDataSourceConfigId(dataSourceConfigId));
    }

    @Override
    public List<DataMappingRule> findBySourceKey(UUID dataSourceConfigId, String sourceKey) {
        return DaoUtil.convertDataList(repository.findByDataSourceConfigIdAndSourceKey(dataSourceConfigId, sourceKey));
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
    public void deleteByDataSourceConfigId(UUID dataSourceConfigId) {
        repository.deleteByDataSourceConfigId(dataSourceConfigId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DATA_MAPPING_RULE;
    }
}
