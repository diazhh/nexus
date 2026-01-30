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

import org.thingsboard.server.common.data.id.DataAggregationId;
import org.thingsboard.server.common.data.id.DataMappingRuleId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.DataAggregation;
import org.thingsboard.server.common.data.nexus.DataMappingRule;
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.nexus.DistributionLog;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for NEXUS Data Distribution System.
 * Handles data source configuration, mapping rules, aggregations, and distribution logs.
 */
public interface DataDistributionService {

    // ========================
    // Data Source Configuration
    // ========================

    /**
     * Save or update a data source configuration
     */
    DataSourceConfig saveDataSourceConfig(TenantId tenantId, DataSourceConfig config);

    /**
     * Find data source configuration by ID
     */
    DataSourceConfig findDataSourceConfigById(DataSourceConfigId configId);

    /**
     * Find data source configuration by device ID
     */
    Optional<DataSourceConfig> findDataSourceConfigByDeviceId(TenantId tenantId, DeviceId deviceId);

    /**
     * Find all data source configurations for a tenant
     */
    PageData<DataSourceConfig> findDataSourceConfigsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Find data source configurations by module key
     */
    List<DataSourceConfig> findDataSourceConfigsByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Find active data source configurations for a tenant
     */
    List<DataSourceConfig> findActiveDataSourceConfigs(TenantId tenantId);

    /**
     * Find active data source configurations by module key
     */
    List<DataSourceConfig> findActiveDataSourceConfigsByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Delete a data source configuration
     */
    void deleteDataSourceConfig(DataSourceConfigId configId);

    /**
     * Check if a device already has a configuration
     */
    boolean existsDataSourceConfigForDevice(TenantId tenantId, DeviceId deviceId);

    // ========================
    // Data Mapping Rules
    // ========================

    /**
     * Save or update a mapping rule
     */
    DataMappingRule saveDataMappingRule(TenantId tenantId, DataMappingRule rule);

    /**
     * Find mapping rule by ID
     */
    DataMappingRule findDataMappingRuleById(DataMappingRuleId ruleId);

    /**
     * Find all mapping rules for a data source configuration
     */
    List<DataMappingRule> findMappingRulesByDataSourceConfigId(DataSourceConfigId configId);

    /**
     * Find active mapping rules for a data source configuration (ordered by priority)
     */
    List<DataMappingRule> findActiveMappingRulesByDataSourceConfigId(DataSourceConfigId configId);

    /**
     * Find mapping rules by source key
     */
    List<DataMappingRule> findMappingRulesBySourceKey(DataSourceConfigId configId, String sourceKey);

    /**
     * Delete a mapping rule
     */
    void deleteDataMappingRule(DataMappingRuleId ruleId);

    /**
     * Delete all mapping rules for a data source configuration
     */
    void deleteMappingRulesByDataSourceConfigId(DataSourceConfigId configId);

    // ========================
    // Data Aggregation
    // ========================

    /**
     * Save or update an aggregation definition
     */
    DataAggregation saveDataAggregation(TenantId tenantId, DataAggregation aggregation);

    /**
     * Find aggregation by ID
     */
    DataAggregation findDataAggregationById(DataAggregationId aggregationId);

    /**
     * Find all aggregations for a tenant
     */
    PageData<DataAggregation> findDataAggregationsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Find aggregations by module key
     */
    List<DataAggregation> findDataAggregationsByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Find active aggregations by module key
     */
    List<DataAggregation> findActiveDataAggregationsByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Find aggregations by source asset type
     */
    List<DataAggregation> findDataAggregationsBySourceAssetType(TenantId tenantId, String sourceAssetType);

    /**
     * Find active aggregations by source asset type
     */
    List<DataAggregation> findActiveDataAggregationsBySourceAssetType(TenantId tenantId, String sourceAssetType);

    /**
     * Delete an aggregation
     */
    void deleteDataAggregation(DataAggregationId aggregationId);

    // ========================
    // Distribution Logs
    // ========================

    /**
     * Save a distribution log entry
     */
    DistributionLog saveDistributionLog(TenantId tenantId, DistributionLog log);

    /**
     * Find distribution log by ID
     */
    DistributionLog findDistributionLogById(TenantId tenantId, String logId);

    /**
     * Find distribution logs for a tenant
     */
    PageData<DistributionLog> findDistributionLogsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Find distribution logs for a device
     */
    PageData<DistributionLog> findDistributionLogsByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink);

    /**
     * Find distribution logs by status
     */
    List<DistributionLog> findDistributionLogsByStatus(TenantId tenantId, DistributionStatus status);

    /**
     * Find distribution logs within a time range
     */
    PageData<DistributionLog> findDistributionLogsByTimeRange(TenantId tenantId, long startTime, long endTime, PageLink pageLink);

    /**
     * Count distribution logs by status
     */
    long countDistributionLogsByStatus(TenantId tenantId, DistributionStatus status);

    /**
     * Delete distribution logs older than a given timestamp
     */
    void deleteDistributionLogsOlderThan(TenantId tenantId, long timestamp);

    // ========================
    // Cleanup
    // ========================

    /**
     * Delete all data distribution resources for a tenant
     */
    void deleteByTenantId(TenantId tenantId);
}
