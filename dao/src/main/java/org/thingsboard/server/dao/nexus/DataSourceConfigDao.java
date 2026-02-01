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
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO interface for DataSourceConfig operations.
 */
public interface DataSourceConfigDao extends Dao<DataSourceConfig> {

    /**
     * Save or update a data source configuration
     */
    DataSourceConfig save(TenantId tenantId, DataSourceConfig config);

    /**
     * Find configuration by ID
     */
    DataSourceConfig findById(UUID configId);

    /**
     * Find configuration by device ID
     */
    Optional<DataSourceConfig> findByDeviceId(UUID tenantId, UUID deviceId);

    /**
     * Find all configurations for a tenant
     */
    PageData<DataSourceConfig> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find configurations by module key
     */
    List<DataSourceConfig> findByModuleKey(UUID tenantId, String moduleKey);

    /**
     * Find configurations by module key with pagination
     */
    PageData<DataSourceConfig> findByModuleKey(UUID tenantId, String moduleKey, PageLink pageLink);

    /**
     * Find active configurations for a tenant
     */
    List<DataSourceConfig> findActiveByTenantId(UUID tenantId);

    /**
     * Find active configurations by module key
     */
    List<DataSourceConfig> findActiveByModuleKey(UUID tenantId, String moduleKey);

    /**
     * Check if a device already has a configuration
     */
    boolean existsByDeviceId(UUID tenantId, UUID deviceId);

    /**
     * Delete configuration by ID
     */
    boolean removeById(UUID configId);

    /**
     * Delete all configurations for a tenant
     */
    void deleteByTenantId(UUID tenantId);
}
