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
import org.thingsboard.server.common.data.nexus.DataAggregation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DataAggregationEntity;
import org.thingsboard.server.dao.nexus.DataAggregationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of DataAggregationDao.
 */
@Component
@SqlDao
public class JpaDataAggregationDao extends JpaAbstractDao<DataAggregationEntity, DataAggregation> implements DataAggregationDao {

    @Autowired
    private DataAggregationRepository repository;

    @Override
    protected Class<DataAggregationEntity> getEntityClass() {
        return DataAggregationEntity.class;
    }

    @Override
    protected JpaRepository<DataAggregationEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public DataAggregation save(TenantId tenantId, DataAggregation aggregation) {
        return super.save(tenantId, aggregation);
    }

    @Override
    public DataAggregation findById(UUID aggregationId) {
        return DaoUtil.getData(repository.findById(aggregationId));
    }

    @Override
    public PageData<DataAggregation> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<DataAggregation> findByModuleKey(UUID tenantId, String moduleKey) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndModuleKey(tenantId, moduleKey));
    }

    @Override
    public List<DataAggregation> findActiveByModuleKey(UUID tenantId, String moduleKey) {
        return DaoUtil.convertDataList(repository.findActiveByTenantIdAndModuleKey(tenantId, moduleKey));
    }

    @Override
    public List<DataAggregation> findBySourceAssetType(UUID tenantId, String sourceAssetType) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndSourceAssetType(tenantId, sourceAssetType));
    }

    @Override
    public List<DataAggregation> findActiveBySourceAssetType(UUID tenantId, String sourceAssetType) {
        return DaoUtil.convertDataList(repository.findActiveByTenantIdAndSourceAssetType(tenantId, sourceAssetType));
    }

    @Override
    public boolean existsByName(UUID tenantId, String name) {
        return repository.existsByTenantIdAndAggregationName(tenantId, name);
    }

    @Override
    public boolean removeById(UUID aggregationId) {
        if (repository.existsById(aggregationId)) {
            repository.deleteById(aggregationId);
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
        return EntityType.DATA_AGGREGATION;
    }
}
