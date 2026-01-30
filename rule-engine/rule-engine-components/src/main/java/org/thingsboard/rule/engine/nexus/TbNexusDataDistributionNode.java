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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.nexus.DataMappingRule;
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.nexus.DistributionLog;
import org.thingsboard.server.common.data.nexus.DistributionMode;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.common.data.nexus.TransformationType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.nexus.DataDistributionService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rule Engine node for distributing telemetry data from devices to Digital Twin assets.
 *
 * This node is the core of the NEXUS Data Distribution System, responsible for:
 * 1. Receiving telemetry from devices (RTUs, PLCs, sensors)
 * 2. Looking up data source configuration for the device
 * 3. Applying mapping rules to transform data
 * 4. Distributing transformed data to Digital Twin assets
 *
 * Output routes:
 * - "Success": Data was successfully distributed
 * - "No Config": Device has no data source configuration
 * - "Partial": Some keys were distributed, some failed
 * - "Failure": Distribution failed completely
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "nexus data distribution",
        configClazz = TbNexusDataDistributionNodeConfiguration.class,
        nodeDescription = "Distribute telemetry data from devices to Digital Twin assets",
        nodeDetails = "Receives telemetry from devices and distributes it to corresponding Digital Twin assets " +
                "based on configured mapping rules.<br/><br/>" +
                "The node supports three distribution modes:<br/>" +
                "- <b>DIRECT:</b> Send data directly to the target asset without transformation<br/>" +
                "- <b>MAPPED:</b> Transform data according to mapping rules before distribution<br/>" +
                "- <b>HIERARCHICAL:</b> Distribute to asset hierarchy (target + related assets)<br/><br/>" +
                "Each device must have a Data Source Configuration to enable distribution.",
        configDirective = "tbActionNodeNexusDataDistributionConfig",
        icon = "share",
        docUrl = "https://nexus.thingsboard.io/docs/data-distribution",
        relationTypes = {"Success", "No Config", "Partial", "Failure"}
)
public class TbNexusDataDistributionNode implements TbNode {

    private static final String RELATION_SUCCESS = "Success";
    private static final String RELATION_NO_CONFIG = "No Config";
    private static final String RELATION_PARTIAL = "Partial";
    private static final String RELATION_FAILURE = "Failure";

    private TbNexusDataDistributionNodeConfiguration config;

    // Cache for data source configurations (device ID -> config)
    private final Map<DeviceId, Optional<DataSourceConfig>> configCache = new ConcurrentHashMap<>();

    // Cache for mapping rules (config ID -> rules)
    private final Map<String, List<DataMappingRule>> rulesCache = new ConcurrentHashMap<>();

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbNexusDataDistributionNodeConfiguration.class);
        log.info("TbNexusDataDistributionNode initialized with config: moduleKey={}, mode={}, errorHandling={}",
                config.getModuleKey(), config.getDistributionMode(), config.getErrorHandling());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellNext(msg, RELATION_NO_CONFIG);
            return;
        }

        DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
        TenantId tenantId = ctx.getTenantId();
        DataDistributionService distributionService = ctx.getDataDistributionService();

        long startTime = System.currentTimeMillis();
        int keysProcessed = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Get data source configuration
            Optional<DataSourceConfig> configOpt = getDataSourceConfig(distributionService, tenantId, deviceId);

            if (configOpt.isEmpty()) {
                log.debug("No data source configuration found for device: {}", deviceId);
                ctx.tellNext(msg, RELATION_NO_CONFIG);
                return;
            }

            DataSourceConfig dataSourceConfig = configOpt.get();

            // Check if module filter applies
            if (!isModuleMatch(dataSourceConfig)) {
                log.debug("Module filter mismatch for device: {}, config module: {}, filter: {}",
                        deviceId, dataSourceConfig.getModuleKey(), config.getModuleKey());
                ctx.tellNext(msg, RELATION_NO_CONFIG);
                return;
            }

            // Check if config is active
            if (!dataSourceConfig.isActive()) {
                log.debug("Data source configuration is inactive for device: {}", deviceId);
                ctx.tellNext(msg, RELATION_NO_CONFIG);
                return;
            }

            // Parse message data
            JsonNode dataNode = JacksonUtil.toJsonNode(msg.getData());
            if (dataNode == null || !dataNode.isObject()) {
                ctx.tellFailure(msg, new RuntimeException("Message data is not a valid JSON object"));
                return;
            }

            // Get timestamp
            long ts = config.isUseServerTs() ? System.currentTimeMillis() : msg.getMetaDataTs();

            // Get distribution mode
            DistributionMode mode = getDistributionMode(dataSourceConfig);

            // Process based on mode
            if (mode == DistributionMode.DIRECT) {
                keysProcessed = distributeDirectly(ctx, msg, dataSourceConfig, dataNode, ts, errors);
            } else {
                // MAPPED or HIERARCHICAL
                keysProcessed = distributeWithMapping(ctx, msg, distributionService, dataSourceConfig, dataNode, ts, errors);
            }

            // Determine result
            long processingTime = System.currentTimeMillis() - startTime;
            DistributionStatus status = determineStatus(keysProcessed, errors);

            // Log if enabled
            if (config.isEnableAuditLog()) {
                logDistribution(distributionService, tenantId, deviceId, dataSourceConfig, status,
                        keysProcessed, errors, processingTime);
            }

            // Route message
            switch (status) {
                case SUCCESS:
                    ctx.tellNext(msg, RELATION_SUCCESS);
                    break;
                case PARTIAL:
                    ctx.tellNext(msg, RELATION_PARTIAL);
                    break;
                case FAILED:
                    ctx.tellFailure(msg, new RuntimeException("Distribution failed: " + String.join("; ", errors)));
                    break;
                default:
                    ctx.tellNext(msg, RELATION_SUCCESS);
            }

        } catch (Exception e) {
            log.error("Error in data distribution for device: {}", deviceId, e);
            ctx.tellFailure(msg, e);
        }
    }

    private Optional<DataSourceConfig> getDataSourceConfig(DataDistributionService service,
                                                            TenantId tenantId, DeviceId deviceId) {
        return configCache.computeIfAbsent(deviceId,
                id -> service.findDataSourceConfigByDeviceId(tenantId, deviceId));
    }

    private boolean isModuleMatch(DataSourceConfig dataSourceConfig) {
        if ("AUTO".equalsIgnoreCase(config.getModuleKey())) {
            return true;
        }
        return config.getModuleKey().equalsIgnoreCase(dataSourceConfig.getModuleKey());
    }

    private DistributionMode getDistributionMode(DataSourceConfig dataSourceConfig) {
        if ("DIRECT".equalsIgnoreCase(config.getDistributionMode())) {
            return DistributionMode.DIRECT;
        } else if ("HIERARCHICAL".equalsIgnoreCase(config.getDistributionMode())) {
            return DistributionMode.HIERARCHICAL;
        } else {
            // Use config from data source or default to MAPPED
            return dataSourceConfig.getDistributionMode() != null ?
                    dataSourceConfig.getDistributionMode() : DistributionMode.MAPPED;
        }
    }

    private int distributeDirectly(TbContext ctx, TbMsg msg, DataSourceConfig dataSourceConfig,
                                    JsonNode dataNode, long ts, List<String> errors) {
        if (dataSourceConfig.getTargetAssetId() == null) {
            errors.add("No target asset configured for direct distribution");
            return 0;
        }

        AssetId targetAssetId = dataSourceConfig.getTargetAssetId();
        int keysProcessed = 0;

        try {
            List<TsKvEntry> entries = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = dataNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                TsKvEntry entry = createTsKvEntry(ts, field.getKey(), field.getValue());
                if (entry != null) {
                    entries.add(entry);
                    keysProcessed++;
                }
            }

            if (!entries.isEmpty()) {
                saveTelemetry(ctx, targetAssetId, entries);
            }
        } catch (Exception e) {
            errors.add("Direct distribution error: " + e.getMessage());
            log.error("Error in direct distribution to asset: {}", targetAssetId, e);
        }

        return keysProcessed;
    }

    private int distributeWithMapping(TbContext ctx, TbMsg msg, DataDistributionService service,
                                       DataSourceConfig dataSourceConfig, JsonNode dataNode,
                                       long ts, List<String> errors) {
        // Get mapping rules
        List<DataMappingRule> rules = getMappingRules(service, dataSourceConfig);

        if (rules.isEmpty()) {
            // No rules, fall back to direct distribution
            return distributeDirectly(ctx, msg, dataSourceConfig, dataNode, ts, errors);
        }

        int keysProcessed = 0;
        AssetId defaultTarget = dataSourceConfig.getTargetAssetId();

        // Group rules by target asset
        Map<AssetId, List<TsKvEntry>> entriesByAsset = new ConcurrentHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = dataNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String sourceKey = field.getKey();
            JsonNode value = field.getValue();

            // Find matching rules for this key
            List<DataMappingRule> matchingRules = rules.stream()
                    .filter(r -> r.getSourceKey().equals(sourceKey) && r.isActive())
                    .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                    .toList();

            if (matchingRules.isEmpty()) {
                // No rule for this key, send to default target if available
                if (defaultTarget != null) {
                    TsKvEntry entry = createTsKvEntry(ts, sourceKey, value);
                    if (entry != null) {
                        entriesByAsset.computeIfAbsent(defaultTarget, k -> new ArrayList<>()).add(entry);
                        keysProcessed++;
                    }
                }
            } else {
                // Apply rules
                for (DataMappingRule rule : matchingRules) {
                    try {
                        TsKvEntry entry = applyMappingRule(ts, rule, value);
                        if (entry != null) {
                            // Use the default target from config or resolve based on rule's relation/type
                            AssetId targetAsset = defaultTarget;
                            if (targetAsset != null) {
                                entriesByAsset.computeIfAbsent(targetAsset, k -> new ArrayList<>()).add(entry);
                                keysProcessed++;
                            }
                        }
                    } catch (Exception e) {
                        String error = String.format("Rule %s failed for key %s: %s",
                                rule.getId(), sourceKey, e.getMessage());
                        errors.add(error);
                        if ("FAIL".equalsIgnoreCase(config.getErrorHandling())) {
                            throw new RuntimeException(error, e);
                        }
                    }
                }
            }
        }

        // Save telemetry to each target asset
        for (Map.Entry<AssetId, List<TsKvEntry>> entry : entriesByAsset.entrySet()) {
            try {
                saveTelemetry(ctx, entry.getKey(), entry.getValue());
            } catch (Exception e) {
                errors.add("Failed to save to asset " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return keysProcessed;
    }

    private List<DataMappingRule> getMappingRules(DataDistributionService service, DataSourceConfig config) {
        String cacheKey = config.getId().toString();
        return rulesCache.computeIfAbsent(cacheKey,
                k -> service.findActiveMappingRulesByDataSourceConfigId(config.getId()));
    }

    private TsKvEntry createTsKvEntry(long ts, String key, JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isNumber()) {
            if (value.isIntegralNumber()) {
                return new BasicTsKvEntry(ts, new LongDataEntry(key, value.longValue()));
            } else {
                return new BasicTsKvEntry(ts, new DoubleDataEntry(key, value.doubleValue()));
            }
        } else if (value.isTextual()) {
            return new BasicTsKvEntry(ts, new StringDataEntry(key, value.textValue()));
        } else if (value.isBoolean()) {
            return new BasicTsKvEntry(ts, new StringDataEntry(key, String.valueOf(value.booleanValue())));
        }

        return null;
    }

    private TsKvEntry applyMappingRule(long ts, DataMappingRule rule, JsonNode value) {
        String targetKey = rule.getTargetKey();
        TransformationType transformationType = rule.getTransformationType();

        if (transformationType == null || transformationType == TransformationType.DIRECT) {
            return createTsKvEntry(ts, targetKey, value);
        }

        // Handle transformations
        if (transformationType == TransformationType.SCALE && value.isNumber()) {
            JsonNode configNode = rule.getTransformationConfig();
            double factor = configNode != null && configNode.has("factor") ?
                    configNode.get("factor").doubleValue() : 1.0;
            double offset = configNode != null && configNode.has("offset") ?
                    configNode.get("offset").doubleValue() : 0.0;

            double transformed = value.doubleValue() * factor + offset;
            return new BasicTsKvEntry(ts, new DoubleDataEntry(targetKey, transformed));
        }

        // For unsupported transformations, just use direct
        return createTsKvEntry(ts, targetKey, value);
    }

    private void saveTelemetry(TbContext ctx, AssetId assetId, List<TsKvEntry> entries) {
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(assetId)
                .entries(entries)
                .ttl(config.getDefaultTtlSeconds())
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .callback(new TelemetryCallback(assetId))
                .build());
    }

    private DistributionStatus determineStatus(int keysProcessed, List<String> errors) {
        if (errors.isEmpty()) {
            return DistributionStatus.SUCCESS;
        } else if (keysProcessed > 0) {
            return DistributionStatus.PARTIAL;
        } else {
            return DistributionStatus.FAILED;
        }
    }

    private void logDistribution(DataDistributionService service, TenantId tenantId, DeviceId deviceId,
                                  DataSourceConfig dataSourceConfig, DistributionStatus status,
                                  int keysProcessed, List<String> errors, long processingTime) {
        DistributionLog logEntry = new DistributionLog();
        logEntry.setTenantId(tenantId);
        logEntry.setDeviceId(deviceId);
        logEntry.setTargetAssetId(dataSourceConfig.getTargetAssetId());
        logEntry.setModuleKey(dataSourceConfig.getModuleKey());
        logEntry.setOperationType("DISTRIBUTE");
        logEntry.setStatus(status);
        logEntry.setKeysProcessed(keysProcessed);
        logEntry.setProcessingTimeMs(processingTime);

        if (!errors.isEmpty()) {
            logEntry.setErrorMessage(String.join("; ", errors));
        }

        service.saveDistributionLog(tenantId, logEntry);
    }

    @Override
    public void destroy() {
        configCache.clear();
        rulesCache.clear();
    }

    /**
     * Callback for telemetry save operations.
     */
    private static class TelemetryCallback implements com.google.common.util.concurrent.FutureCallback<Void> {
        private final AssetId assetId;

        TelemetryCallback(AssetId assetId) {
            this.assetId = assetId;
        }

        @Override
        public void onSuccess(Void result) {
            log.debug("Telemetry saved successfully to asset: {}", assetId);
        }

        @Override
        public void onFailure(Throwable t) {
            log.error("Failed to save telemetry to asset: {}", assetId, t);
        }
    }
}
