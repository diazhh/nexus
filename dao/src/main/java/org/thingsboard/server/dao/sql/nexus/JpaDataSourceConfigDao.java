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
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DataSourceConfigEntity;
import org.thingsboard.server.dao.nexus.DataSourceConfigDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of DataSourceConfigDao.
 */
@Component
@SqlDao
public class JpaDataSourceConfigDao extends JpaAbstractDao<DataSourceConfigEntity, DataSourceConfig> implements DataSourceConfigDao {

    @Autowired
    private DataSourceConfigRepository repository;

    @Override
    protected Class<DataSourceConfigEntity> getEntityClass() {
        return DataSourceConfigEntity.class;
    }

    @Override
    protected JpaRepository<DataSourceConfigEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public DataSourceConfig save(TenantId tenantId, DataSourceConfig config) {
        return save(tenantId, config);
    }

    @Override
    public DataSourceConfig findById(UUID configId) {
        return DaoUtil.getData(repository.findById(configId));
    }

    @Override
    public Optional<DataSourceConfig> findByDeviceId(UUID tenantId, UUID deviceId) {
        return repository.findByTenantIdAndDeviceId(tenantId, deviceId)
                .map(DataSourceConfigEntity::toData);
    }

    @Override
    public PageData<DataSourceConfig> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<DataSourceConfig> findByModuleKey(UUID tenantId, String moduleKey) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndModuleKey(tenantId, moduleKey));
    }

    @Override
    public List<DataSourceConfig> findActiveByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(repository.findActiveByTenantId(tenantId));
    }

    @Override
    public List<DataSourceConfig> findActiveByModuleKey(UUID tenantId, String moduleKey) {
        return DaoUtil.convertDataList(repository.findActiveByTenantIdAndModuleKey(tenantId, moduleKey));
    }

    @Override
    public boolean existsByDeviceId(UUID tenantId, UUID deviceId) {
        return repository.existsByTenantIdAndDeviceId(tenantId, deviceId);
    }

    @Override
    public boolean removeById(UUID configId) {
        if (repository.existsById(configId)) {
            repository.deleteById(configId);
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
        return EntityType.DATA_SOURCE_CONFIG;
    }
}
