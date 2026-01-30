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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.nexus.AggregationType;
import org.thingsboard.server.common.data.nexus.DataAggregation;
import org.thingsboard.server.common.data.nexus.DistributionLog;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.nexus.DataDistributionService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rule Engine node for aggregating telemetry data from child assets to parent assets.
 *
 * This node is part of the NEXUS Data Distribution System, responsible for:
 * 1. Receiving triggers (data or schedule)
 * 2. Looking up aggregation configurations
 * 3. Fetching data from child assets
 * 4. Computing aggregations (SUM, AVG, MIN, MAX, COUNT)
 * 5. Saving aggregated data to parent assets
 *
 * Output routes:
 * - "Success": Aggregation completed successfully
 * - "No Config": No aggregation configuration found
 * - "No Children": No child assets found for aggregation
 * - "Failure": Aggregation failed
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "nexus data aggregation",
        configClazz = TbNexusDataAggregationNodeConfiguration.class,
        nodeDescription = "Aggregate telemetry data from child assets to parent assets",
        nodeDetails = "Fetches telemetry from child assets and computes aggregations " +
                "(SUM, AVG, MIN, MAX, COUNT) to be saved in parent assets.<br/><br/>" +
                "The node supports multiple trigger modes:<br/>" +
                "- <b>ON_DATA:</b> Execute when telemetry arrives<br/>" +
                "- <b>ON_SCHEDULE:</b> Execute on scheduled intervals<br/>" +
                "- <b>BOTH:</b> Execute on both triggers<br/><br/>" +
                "Aggregation rules are defined in the NEXUS Data Aggregation configuration.",
        configDirective = "tbActionNodeNexusDataAggregationConfig",
        icon = "functions",
        docUrl = "https://nexus.thingsboard.io/docs/data-aggregation",
        relationTypes = {"Success", "No Config", "No Children", "Failure"}
)
public class TbNexusDataAggregationNode implements TbNode {

    private static final String RELATION_SUCCESS = "Success";
    private static final String RELATION_NO_CONFIG = "No Config";
    private static final String RELATION_NO_CHILDREN = "No Children";
    private static final String RELATION_FAILURE = "Failure";
    private static final String RELATION_TYPE_CONTAINS = "Contains";

    private TbNexusDataAggregationNodeConfiguration config;

    // Cache for aggregation configs
    private final Map<String, List<DataAggregation>> aggregationCache = new ConcurrentHashMap<>();

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbNexusDataAggregationNodeConfiguration.class);
        log.info("TbNexusDataAggregationNode initialized with config: moduleKey={}, triggerMode={}, aggregationTypes={}",
                config.getModuleKey(), config.getTriggerMode(), config.getAggregationTypes());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        TenantId tenantId = ctx.getTenantId();
        DataDistributionService distributionService = ctx.getDataDistributionService();
        RelationService relationService = ctx.getRelationService();

        long startTime = System.currentTimeMillis();
        int aggregationsProcessed = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Get aggregation configurations
            List<DataAggregation> aggregations = getAggregationConfigs(distributionService, tenantId);

            if (aggregations.isEmpty()) {
                log.debug("No aggregation configurations found for tenant: {}", tenantId);
                ctx.tellNext(msg, RELATION_NO_CONFIG);
                return;
            }

            // Determine the source asset from message originator
            EntityId originatorId = msg.getOriginator();
            String sourceAssetType = null;

            if (originatorId.getEntityType() == EntityType.ASSET) {
                Asset sourceAsset = ctx.getAssetService().findAssetById(tenantId, new AssetId(originatorId.getId()));
                if (sourceAsset != null) {
                    sourceAssetType = sourceAsset.getType();
                }
            }

            // Filter and process aggregations
            for (DataAggregation aggregation : aggregations) {
                if (!aggregation.isActive()) {
                    continue;
                }

                // Check if source asset type matches
                if (sourceAssetType != null && !aggregation.getSourceAssetType().equals(sourceAssetType)) {
                    continue;
                }

                try {
                    boolean processed = processAggregation(ctx, tenantId, aggregation, relationService, msg);
                    if (processed) {
                        aggregationsProcessed++;
                    }
                } catch (Exception e) {
                    String error = String.format("Aggregation %s failed: %s", aggregation.getId(), e.getMessage());
                    errors.add(error);
                    log.error("Error processing aggregation: {}", aggregation.getId(), e);
                }
            }

            // Log if enabled
            long processingTime = System.currentTimeMillis() - startTime;
            if (config.isEnableAuditLog() && aggregationsProcessed > 0) {
                logAggregation(distributionService, tenantId, aggregationsProcessed, errors, processingTime);
            }

            // Determine result
            if (aggregationsProcessed == 0 && errors.isEmpty()) {
                ctx.tellNext(msg, RELATION_NO_CONFIG);
            } else if (!errors.isEmpty() && aggregationsProcessed == 0) {
                ctx.tellFailure(msg, new RuntimeException("All aggregations failed: " + String.join("; ", errors)));
            } else {
                ctx.tellNext(msg, RELATION_SUCCESS);
            }

        } catch (Exception e) {
            log.error("Error in data aggregation", e);
            ctx.tellFailure(msg, e);
        }
    }

    private List<DataAggregation> getAggregationConfigs(DataDistributionService service, TenantId tenantId) {
        String cacheKey = tenantId.toString() + "_" + config.getModuleKey();

        if (!config.isUseCache()) {
            return fetchAggregations(service, tenantId);
        }

        return aggregationCache.computeIfAbsent(cacheKey, k -> fetchAggregations(service, tenantId));
    }

    private List<DataAggregation> fetchAggregations(DataDistributionService service, TenantId tenantId) {
        if ("ALL".equalsIgnoreCase(config.getModuleKey())) {
            return service.findDataAggregationsByTenantId(tenantId,
                    new org.thingsboard.server.common.data.page.PageLink(1000)).getData();
        } else {
            return service.findActiveDataAggregationsByModuleKey(tenantId, config.getModuleKey());
        }
    }

    private boolean processAggregation(TbContext ctx, TenantId tenantId, DataAggregation aggregation,
                                        RelationService relationService, TbMsg msg) {
        // Find parent assets of the target type
        List<Asset> parentAssets = findParentAssets(ctx, tenantId, aggregation.getTargetAssetType());

        if (parentAssets.isEmpty()) {
            log.debug("No parent assets found for aggregation: {}", aggregation.getId());
            return false;
        }

        boolean anyProcessed = false;

        for (Asset parentAsset : parentAssets) {
            // Find child assets
            List<Asset> childAssets = findChildAssets(ctx, tenantId, relationService,
                    parentAsset.getId(), aggregation.getSourceAssetType());

            if (childAssets.isEmpty()) {
                continue;
            }

            // Compute aggregation
            Double result = computeAggregation(ctx, tenantId, childAssets, aggregation);

            if (result != null) {
                // Save result to parent
                saveAggregationResult(ctx, parentAsset.getId(), aggregation.getTargetKey(), result);
                anyProcessed = true;
            }
        }

        return anyProcessed;
    }

    private List<Asset> findParentAssets(TbContext ctx, TenantId tenantId, String assetType) {
        List<Asset> assets = new ArrayList<>();
        try {
            // This is a simplified implementation - in production you might want to use pagination
            org.thingsboard.server.common.data.page.PageData<Asset> pageData =
                    ctx.getAssetService().findAssetsByTenantIdAndType(tenantId, assetType,
                            new org.thingsboard.server.common.data.page.PageLink(1000));
            if (pageData != null && pageData.getData() != null) {
                assets.addAll(pageData.getData());
            }
        } catch (Exception e) {
            log.error("Error finding parent assets of type: {}", assetType, e);
        }
        return assets;
    }

    private List<Asset> findChildAssets(TbContext ctx, TenantId tenantId, RelationService relationService,
                                         AssetId parentId, String childAssetType) {
        List<Asset> childAssets = new ArrayList<>();
        try {
            List<EntityRelation> relations = relationService.findByFromAndType(tenantId, parentId,
                    RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON);

            for (EntityRelation relation : relations) {
                if (relation.getTo().getEntityType() == EntityType.ASSET) {
                    Asset childAsset = ctx.getAssetService().findAssetById(tenantId,
                            new AssetId(relation.getTo().getId()));
                    if (childAsset != null && childAsset.getType().equals(childAssetType)) {
                        childAssets.add(childAsset);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error finding child assets for parent: {}", parentId, e);
        }
        return childAssets;
    }

    private Double computeAggregation(TbContext ctx, TenantId tenantId, List<Asset> childAssets,
                                       DataAggregation aggregation) {
        List<Double> values = new ArrayList<>();
        String sourceKey = aggregation.getSourceKey();

        // Get latest values from child assets
        for (Asset childAsset : childAssets) {
            try {
                List<TsKvEntry> entries = ctx.getTimeseriesService().findLatest(tenantId,
                        childAsset.getId(), List.of(sourceKey)).get();

                if (!entries.isEmpty()) {
                    TsKvEntry entry = entries.get(0);
                    if (entry.getValue() != null) {
                        Double value = extractDoubleValue(entry);
                        if (value != null) {
                            values.add(value);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error getting telemetry for asset: {}", childAsset.getId(), e);
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        // Compute aggregation
        AggregationType type = aggregation.getAggregationType();
        return switch (type) {
            case SUM -> values.stream().mapToDouble(Double::doubleValue).sum();
            case AVG -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MIN -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case MAX -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case COUNT -> (double) values.size();
        };
    }

    private Double extractDoubleValue(TsKvEntry entry) {
        if (entry.getDoubleValue().isPresent()) {
            return entry.getDoubleValue().get();
        } else if (entry.getLongValue().isPresent()) {
            return entry.getLongValue().get().doubleValue();
        } else if (entry.getStrValue().isPresent()) {
            try {
                return Double.parseDouble(entry.getStrValue().get());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void saveAggregationResult(TbContext ctx, AssetId assetId, String key, Double value) {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> entries = List.of(new BasicTsKvEntry(ts, new DoubleDataEntry(key, value)));

        if (config.isSaveAsTelemetry()) {
            ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                    .tenantId(ctx.getTenantId())
                    .entityId(assetId)
                    .entries(entries)
                    .ttl(0)
                    .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                    .callback(new AggregationCallback(assetId, key))
                    .build());
        }

        // Attribute saving can be added here if needed
    }

    private void logAggregation(DataDistributionService service, TenantId tenantId,
                                 int aggregationsProcessed, List<String> errors, long processingTime) {
        DistributionLog logEntry = new DistributionLog();
        logEntry.setTenantId(tenantId);
        logEntry.setOperationType("AGGREGATE");
        logEntry.setStatus(errors.isEmpty() ? DistributionStatus.SUCCESS : DistributionStatus.PARTIAL);
        logEntry.setKeysProcessed(aggregationsProcessed);
        logEntry.setProcessingTimeMs(processingTime);

        if (!errors.isEmpty()) {
            logEntry.setErrorMessage(String.join("; ", errors));
        }

        service.saveDistributionLog(tenantId, logEntry);
    }

    @Override
    public void destroy() {
        aggregationCache.clear();
    }

    /**
     * Callback for aggregation save operations.
     */
    private static class AggregationCallback implements com.google.common.util.concurrent.FutureCallback<Void> {
        private final AssetId assetId;
        private final String key;

        AggregationCallback(AssetId assetId, String key) {
            this.assetId = assetId;
            this.key = key;
        }

        @Override
        public void onSuccess(Void result) {
            log.debug("Aggregation result saved successfully: asset={}, key={}", assetId, key);
        }

        @Override
        public void onFailure(Throwable t) {
            log.error("Failed to save aggregation result: asset={}, key={}", assetId, key, t);
        }
    }
}
