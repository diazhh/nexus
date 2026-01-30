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
package org.thingsboard.rule.engine.nexus;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.List;

/**
 * Configuration for NEXUS Data Aggregation Node.
 *
 * This node aggregates telemetry data from child assets to parent assets
 * according to predefined aggregation rules (SUM, AVG, MIN, MAX, COUNT).
 */
@Data
public class TbNexusDataAggregationNodeConfiguration implements NodeConfiguration<TbNexusDataAggregationNodeConfiguration> {

    /**
     * Module key to filter aggregation configurations.
     * Use "ALL" to process all available aggregations.
     */
    private String moduleKey;

    /**
     * Trigger mode:
     * - ON_DATA: Execute aggregation when telemetry arrives
     * - ON_SCHEDULE: Execute based on scheduled interval
     * - BOTH: Execute on both data arrival and schedule
     */
    private String triggerMode;

    /**
     * Source asset type to aggregate from (e.g., "CT_UNIT", "RV_WELL", "DR_SYSTEM").
     * Leave empty to use configurations from the database.
     */
    private String sourceAssetType;

    /**
     * Target asset type to aggregate to (e.g., "CT_LOCATION", "RV_CLIENT", "DR_RIG").
     * Leave empty to use configurations from the database.
     */
    private String targetAssetType;

    /**
     * Aggregation types to apply.
     * If empty, uses configurations from the database.
     */
    private List<String> aggregationTypes;

    /**
     * Whether to use cached values for performance.
     */
    private boolean useCache;

    /**
     * Cache TTL in milliseconds.
     */
    private long cacheTtlMs;

    /**
     * Whether to log aggregation operations for auditing.
     */
    private boolean enableAuditLog;

    /**
     * Whether to save aggregation results as telemetry.
     */
    private boolean saveAsTelemetry;

    /**
     * Whether to save aggregation results as attributes.
     */
    private boolean saveAsAttribute;

    /**
     * Attribute scope for saving (SERVER_SCOPE, SHARED_SCOPE).
     */
    private String attributeScope;

    @Override
    public TbNexusDataAggregationNodeConfiguration defaultConfiguration() {
        TbNexusDataAggregationNodeConfiguration config = new TbNexusDataAggregationNodeConfiguration();
        config.setModuleKey("ALL");
        config.setTriggerMode("ON_DATA");
        config.setSourceAssetType("");
        config.setTargetAssetType("");
        config.setAggregationTypes(List.of("SUM", "AVG", "MIN", "MAX", "COUNT"));
        config.setUseCache(true);
        config.setCacheTtlMs(60000);
        config.setEnableAuditLog(true);
        config.setSaveAsTelemetry(true);
        config.setSaveAsAttribute(false);
        config.setAttributeScope("SERVER_SCOPE");
        return config;
    }
}
