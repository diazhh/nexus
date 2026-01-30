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
import org.thingsboard.server.common.data.nexus.DataMappingRule;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * DAO interface for DataMappingRule operations.
 */
public interface DataMappingRuleDao extends Dao<DataMappingRule> {

    /**
     * Save or update a mapping rule
     */
    DataMappingRule save(TenantId tenantId, DataMappingRule rule);

    /**
     * Find rule by ID
     */
    DataMappingRule findById(UUID ruleId);

    /**
     * Find all rules for a data source configuration
     */
    List<DataMappingRule> findByDataSourceConfigId(UUID dataSourceConfigId);

    /**
     * Find active rules for a data source configuration (ordered by priority)
     */
    List<DataMappingRule> findActiveByDataSourceConfigId(UUID dataSourceConfigId);

    /**
     * Find rules by source key
     */
    List<DataMappingRule> findBySourceKey(UUID dataSourceConfigId, String sourceKey);

    /**
     * Delete rule by ID
     */
    boolean removeById(UUID ruleId);

    /**
     * Delete all rules for a data source configuration
     */
    void deleteByDataSourceConfigId(UUID dataSourceConfigId);
}
