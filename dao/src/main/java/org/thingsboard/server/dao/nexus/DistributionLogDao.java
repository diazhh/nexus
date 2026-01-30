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
package org.thingsboard.server.dao.nexus;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.DistributionLog;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * DAO interface for DistributionLog operations.
 */
public interface DistributionLogDao extends Dao<DistributionLog> {

    /**
     * Save a distribution log entry
     */
    DistributionLog save(TenantId tenantId, DistributionLog log);

    /**
     * Find log entry by ID
     */
    DistributionLog findById(UUID logId);

    /**
     * Find log entries for a tenant with pagination
     */
    PageData<DistributionLog> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find log entries for a device
     */
    PageData<DistributionLog> findByDeviceId(UUID tenantId, UUID deviceId, PageLink pageLink);

    /**
     * Find log entries by status
     */
    List<DistributionLog> findByStatus(UUID tenantId, DistributionStatus status);

    /**
     * Find log entries within a time range
     */
    PageData<DistributionLog> findByTimeRange(UUID tenantId, long startTime, long endTime, PageLink pageLink);

    /**
     * Count logs by status for a tenant
     */
    long countByStatus(UUID tenantId, DistributionStatus status);

    /**
     * Delete logs older than a given timestamp
     */
    void deleteOlderThan(UUID tenantId, long timestamp);

    /**
     * Delete all logs for a tenant
     */
    void deleteByTenantId(UUID tenantId);
}
