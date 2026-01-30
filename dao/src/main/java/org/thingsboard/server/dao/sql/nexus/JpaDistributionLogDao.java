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
import org.thingsboard.server.common.data.nexus.DistributionLog;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DistributionLogEntity;
import org.thingsboard.server.dao.nexus.DistributionLogDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of DistributionLogDao.
 */
@Component
@SqlDao
public class JpaDistributionLogDao extends JpaAbstractDao<DistributionLogEntity, DistributionLog> implements DistributionLogDao {

    @Autowired
    private DistributionLogRepository repository;

    @Override
    protected Class<DistributionLogEntity> getEntityClass() {
        return DistributionLogEntity.class;
    }

    @Override
    protected JpaRepository<DistributionLogEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public DistributionLog save(TenantId tenantId, DistributionLog log) {
        return save(tenantId, log);
    }

    @Override
    public DistributionLog findById(UUID logId) {
        return DaoUtil.getData(repository.findById(logId));
    }

    @Override
    public PageData<DistributionLog> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DistributionLog> findByDeviceId(UUID tenantId, UUID deviceId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantIdAndDeviceId(tenantId, deviceId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<DistributionLog> findByStatus(UUID tenantId, DistributionStatus status) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndStatus(tenantId, status));
    }

    @Override
    public PageData<DistributionLog> findByTimeRange(UUID tenantId, long startTime, long endTime, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantIdAndTimeRange(
                tenantId, startTime, endTime, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public long countByStatus(UUID tenantId, DistributionStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }

    @Override
    @Transactional
    public void deleteOlderThan(UUID tenantId, long timestamp) {
        repository.deleteByTenantIdAndCreatedTimeBefore(tenantId, timestamp);
    }

    @Override
    @Transactional
    public void deleteByTenantId(UUID tenantId) {
        repository.deleteByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DISTRIBUTION_LOG;
    }
}
