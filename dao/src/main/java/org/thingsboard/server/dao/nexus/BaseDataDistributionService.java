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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * Implementation of DataDistributionService.
 */
@Service("DataDistributionService")
@Slf4j
public class BaseDataDistributionService implements DataDistributionService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CONFIG_ID = "Incorrect dataSourceConfigId ";
    public static final String INCORRECT_RULE_ID = "Incorrect dataMappingRuleId ";
    public static final String INCORRECT_AGGREGATION_ID = "Incorrect dataAggregationId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String CONFIG_NOT_FOUND = "Data source configuration not found: ";
    public static final String RULE_NOT_FOUND = "Data mapping rule not found: ";
    public static final String AGGREGATION_NOT_FOUND = "Data aggregation not found: ";
    public static final String DEVICE_ALREADY_HAS_CONFIG = "Device already has a data source configuration";
    public static final String MODULE_KEY_REQUIRED = "Module key is required";
    public static final String AGGREGATION_NAME_REQUIRED = "Aggregation name is required";
    public static final String AGGREGATION_NAME_EXISTS = "Aggregation with name '%s' already exists";

    @Autowired
    private DataSourceConfigDao dataSourceConfigDao;

    @Autowired
    private DataMappingRuleDao dataMappingRuleDao;

    @Autowired
    private DataAggregationDao dataAggregationDao;

    @Autowired
    private DistributionLogDao distributionLogDao;

    // ========================
    // Data Source Configuration
    // ========================

    @Override
    @Transactional
    public DataSourceConfig saveDataSourceConfig(TenantId tenantId, DataSourceConfig config) {
        log.trace("Executing saveDataSourceConfig [{}]", config);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateDataSourceConfig(config);

        if (config.getId() == null) {
            // Creating new config
            if (dataSourceConfigDao.existsByDeviceId(tenantId.getId(), config.getDeviceId().getId())) {
                throw new IncorrectParameterException(DEVICE_ALREADY_HAS_CONFIG);
            }
            config.setTenantId(tenantId);
        } else {
            // Updating existing config
            DataSourceConfig existing = dataSourceConfigDao.findById(config.getId().getId());
            if (existing == null) {
                throw new IncorrectParameterException(CONFIG_NOT_FOUND + config.getId());
            }
            // Check if device is being changed to one that already has a config
            if (!existing.getDeviceId().equals(config.getDeviceId()) &&
                    dataSourceConfigDao.existsByDeviceId(tenantId.getId(), config.getDeviceId().getId())) {
                throw new IncorrectParameterException(DEVICE_ALREADY_HAS_CONFIG);
            }
        }

        return dataSourceConfigDao.save(tenantId, config);
    }

    @Override
    public DataSourceConfig findDataSourceConfigById(DataSourceConfigId configId) {
        log.trace("Executing findDataSourceConfigById [{}]", configId);
        validateId(configId, id -> INCORRECT_CONFIG_ID + id);
        return dataSourceConfigDao.findById(configId.getId());
    }

    @Override
    public Optional<DataSourceConfig> findDataSourceConfigByDeviceId(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDataSourceConfigByDeviceId [{}] [{}]", tenantId, deviceId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        return dataSourceConfigDao.findByDeviceId(tenantId.getId(), deviceId.getId());
    }

    @Override
    public PageData<DataSourceConfig> findDataSourceConfigsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDataSourceConfigsByTenantId [{}] [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return dataSourceConfigDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<DataSourceConfig> findDataSourceConfigsByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findDataSourceConfigsByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return dataSourceConfigDao.findByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    public List<DataSourceConfig> findActiveDataSourceConfigs(TenantId tenantId) {
        log.trace("Executing findActiveDataSourceConfigs [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return dataSourceConfigDao.findActiveByTenantId(tenantId.getId());
    }

    @Override
    public List<DataSourceConfig> findActiveDataSourceConfigsByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findActiveDataSourceConfigsByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return dataSourceConfigDao.findActiveByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    @Transactional
    public void deleteDataSourceConfig(DataSourceConfigId configId) {
        log.trace("Executing deleteDataSourceConfig [{}]", configId);
        validateId(configId, id -> INCORRECT_CONFIG_ID + id);

        // First delete all mapping rules for this config
        dataMappingRuleDao.deleteByDataSourceConfigId(configId.getId());

        // Then delete the config
        if (!dataSourceConfigDao.removeById(configId.getId())) {
            throw new IncorrectParameterException(CONFIG_NOT_FOUND + configId);
        }
    }

    @Override
    public boolean existsDataSourceConfigForDevice(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing existsDataSourceConfigForDevice [{}] [{}]", tenantId, deviceId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        return dataSourceConfigDao.existsByDeviceId(tenantId.getId(), deviceId.getId());
    }

    // ========================
    // Data Mapping Rules
    // ========================

    @Override
    @Transactional
    public DataMappingRule saveDataMappingRule(TenantId tenantId, DataMappingRule rule) {
        log.trace("Executing saveDataMappingRule [{}]", rule);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateDataMappingRule(rule);

        // Verify the data source config exists
        DataSourceConfig config = dataSourceConfigDao.findById(rule.getDataSourceConfigId().getId());
        if (config == null) {
            throw new IncorrectParameterException(CONFIG_NOT_FOUND + rule.getDataSourceConfigId());
        }

        return dataMappingRuleDao.save(tenantId, rule);
    }

    @Override
    public DataMappingRule findDataMappingRuleById(DataMappingRuleId ruleId) {
        log.trace("Executing findDataMappingRuleById [{}]", ruleId);
        validateId(ruleId, id -> INCORRECT_RULE_ID + id);
        return dataMappingRuleDao.findById(ruleId.getId());
    }

    @Override
    public List<DataMappingRule> findMappingRulesByDataSourceConfigId(DataSourceConfigId configId) {
        log.trace("Executing findMappingRulesByDataSourceConfigId [{}]", configId);
        validateId(configId, id -> INCORRECT_CONFIG_ID + id);
        return dataMappingRuleDao.findByDataSourceConfigId(configId.getId());
    }

    @Override
    public List<DataMappingRule> findActiveMappingRulesByDataSourceConfigId(DataSourceConfigId configId) {
        log.trace("Executing findActiveMappingRulesByDataSourceConfigId [{}]", configId);
        validateId(configId, id -> INCORRECT_CONFIG_ID + id);
        return dataMappingRuleDao.findActiveByDataSourceConfigId(configId.getId());
    }

    @Override
    public List<DataMappingRule> findMappingRulesBySourceKey(DataSourceConfigId configId, String sourceKey) {
        log.trace("Executing findMappingRulesBySourceKey [{}] [{}]", configId, sourceKey);
        validateId(configId, id -> INCORRECT_CONFIG_ID + id);
        return dataMappingRuleDao.findBySourceKey(configId.getId(), sourceKey);
    }

    @Override
    @Transactional
    public void deleteDataMappingRule(DataMappingRuleId ruleId) {
        log.trace("Executing deleteDataMappingRule [{}]", ruleId);
        validateId(ruleId, id -> INCORRECT_RULE_ID + id);
        if (!dataMappingRuleDao.removeById(ruleId.getId())) {
            throw new IncorrectParameterException(RULE_NOT_FOUND + ruleId);
        }
    }

    @Override
    @Transactional
    public void deleteMappingRulesByDataSourceConfigId(DataSourceConfigId configId) {
        log.trace("Executing deleteMappingRulesByDataSourceConfigId [{}]", configId);
        validateId(configId, id -> INCORRECT_CONFIG_ID + id);
        dataMappingRuleDao.deleteByDataSourceConfigId(configId.getId());
    }

    // ========================
    // Data Aggregation
    // ========================

    @Override
    @Transactional
    public DataAggregation saveDataAggregation(TenantId tenantId, DataAggregation aggregation) {
        log.trace("Executing saveDataAggregation [{}]", aggregation);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateDataAggregation(aggregation);

        if (aggregation.getId() == null) {
            // Creating new aggregation
            if (dataAggregationDao.existsByName(tenantId.getId(), aggregation.getName())) {
                throw new IncorrectParameterException(String.format(AGGREGATION_NAME_EXISTS, aggregation.getName()));
            }
            aggregation.setTenantId(tenantId);
        } else {
            // Updating existing aggregation
            DataAggregation existing = dataAggregationDao.findById(aggregation.getId().getId());
            if (existing == null) {
                throw new IncorrectParameterException(AGGREGATION_NOT_FOUND + aggregation.getId());
            }
            // Check if name is being changed to one that already exists
            if (!existing.getName().equals(aggregation.getName()) &&
                    dataAggregationDao.existsByName(tenantId.getId(), aggregation.getName())) {
                throw new IncorrectParameterException(String.format(AGGREGATION_NAME_EXISTS, aggregation.getName()));
            }
        }

        return dataAggregationDao.save(tenantId, aggregation);
    }

    @Override
    public DataAggregation findDataAggregationById(DataAggregationId aggregationId) {
        log.trace("Executing findDataAggregationById [{}]", aggregationId);
        validateId(aggregationId, id -> INCORRECT_AGGREGATION_ID + id);
        return dataAggregationDao.findById(aggregationId.getId());
    }

    @Override
    public PageData<DataAggregation> findDataAggregationsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDataAggregationsByTenantId [{}] [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return dataAggregationDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<DataAggregation> findDataAggregationsByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findDataAggregationsByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return dataAggregationDao.findByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    public List<DataAggregation> findActiveDataAggregationsByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findActiveDataAggregationsByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return dataAggregationDao.findActiveByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    public List<DataAggregation> findDataAggregationsBySourceAssetType(TenantId tenantId, String sourceAssetType) {
        log.trace("Executing findDataAggregationsBySourceAssetType [{}] [{}]", tenantId, sourceAssetType);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return dataAggregationDao.findBySourceAssetType(tenantId.getId(), sourceAssetType);
    }

    @Override
    public List<DataAggregation> findActiveDataAggregationsBySourceAssetType(TenantId tenantId, String sourceAssetType) {
        log.trace("Executing findActiveDataAggregationsBySourceAssetType [{}] [{}]", tenantId, sourceAssetType);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return dataAggregationDao.findActiveBySourceAssetType(tenantId.getId(), sourceAssetType);
    }

    @Override
    @Transactional
    public void deleteDataAggregation(DataAggregationId aggregationId) {
        log.trace("Executing deleteDataAggregation [{}]", aggregationId);
        validateId(aggregationId, id -> INCORRECT_AGGREGATION_ID + id);
        if (!dataAggregationDao.removeById(aggregationId.getId())) {
            throw new IncorrectParameterException(AGGREGATION_NOT_FOUND + aggregationId);
        }
    }

    // ========================
    // Distribution Logs
    // ========================

    @Override
    public DistributionLog saveDistributionLog(TenantId tenantId, DistributionLog log) {
        log.setTenantId(tenantId);
        return distributionLogDao.save(tenantId, log);
    }

    @Override
    public DistributionLog findDistributionLogById(TenantId tenantId, String logId) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return distributionLogDao.findById(UUID.fromString(logId));
    }

    @Override
    public PageData<DistributionLog> findDistributionLogsByTenantId(TenantId tenantId, PageLink pageLink) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return distributionLogDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DistributionLog> findDistributionLogsByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        validatePageLink(pageLink);
        return distributionLogDao.findByDeviceId(tenantId.getId(), deviceId.getId(), pageLink);
    }

    @Override
    public List<DistributionLog> findDistributionLogsByStatus(TenantId tenantId, DistributionStatus status) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return distributionLogDao.findByStatus(tenantId.getId(), status);
    }

    @Override
    public PageData<DistributionLog> findDistributionLogsByTimeRange(TenantId tenantId, long startTime, long endTime, PageLink pageLink) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return distributionLogDao.findByTimeRange(tenantId.getId(), startTime, endTime, pageLink);
    }

    @Override
    public long countDistributionLogsByStatus(TenantId tenantId, DistributionStatus status) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return distributionLogDao.countByStatus(tenantId.getId(), status);
    }

    @Override
    @Transactional
    public void deleteDistributionLogsOlderThan(TenantId tenantId, long timestamp) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        distributionLogDao.deleteOlderThan(tenantId.getId(), timestamp);
    }

    // ========================
    // Cleanup
    // ========================

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);

        // Delete logs first
        distributionLogDao.deleteByTenantId(tenantId.getId());

        // Delete aggregations
        dataAggregationDao.deleteByTenantId(tenantId.getId());

        // Get all data source configs to delete their rules
        List<DataSourceConfig> configs = dataSourceConfigDao.findActiveByTenantId(tenantId.getId());
        for (DataSourceConfig config : configs) {
            dataMappingRuleDao.deleteByDataSourceConfigId(config.getId().getId());
        }

        // Delete data source configs
        dataSourceConfigDao.deleteByTenantId(tenantId.getId());
    }

    // ========================
    // Private Methods
    // ========================

    private void validateDataSourceConfig(DataSourceConfig config) {
        if (config == null) {
            throw new IncorrectParameterException("Data source configuration cannot be null");
        }
        validateId(config.getDeviceId(), id -> INCORRECT_DEVICE_ID + id);
        validateString(config.getModuleKey(), k -> MODULE_KEY_REQUIRED);
    }

    private void validateDataMappingRule(DataMappingRule rule) {
        if (rule == null) {
            throw new IncorrectParameterException("Data mapping rule cannot be null");
        }
        validateId(rule.getDataSourceConfigId(), id -> INCORRECT_CONFIG_ID + id);
        if (rule.getSourceKey() == null || rule.getSourceKey().isEmpty()) {
            throw new IncorrectParameterException("Source key is required");
        }
        if (rule.getTargetKey() == null || rule.getTargetKey().isEmpty()) {
            throw new IncorrectParameterException("Target key is required");
        }
    }

    private void validateDataAggregation(DataAggregation aggregation) {
        if (aggregation == null) {
            throw new IncorrectParameterException("Data aggregation cannot be null");
        }
        validateString(aggregation.getModuleKey(), k -> MODULE_KEY_REQUIRED);
        validateString(aggregation.getName(), n -> AGGREGATION_NAME_REQUIRED);
        if (aggregation.getSourceAssetType() == null || aggregation.getSourceAssetType().isEmpty()) {
            throw new IncorrectParameterException("Source asset type is required");
        }
        if (aggregation.getTargetAssetType() == null || aggregation.getTargetAssetType().isEmpty()) {
            throw new IncorrectParameterException("Target asset type is required");
        }
        if (aggregation.getSourceKey() == null || aggregation.getSourceKey().isEmpty()) {
            throw new IncorrectParameterException("Source key is required");
        }
        if (aggregation.getTargetKey() == null || aggregation.getTargetKey().isEmpty()) {
            throw new IncorrectParameterException("Target key is required");
        }
        if (aggregation.getAggregationType() == null) {
            throw new IncorrectParameterException("Aggregation type is required");
        }
    }
}
