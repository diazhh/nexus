/*
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
package org.thingsboard.nexus.pf.service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.config.PfModuleConfiguration;
import org.thingsboard.nexus.pf.dto.DataQualityResultDto;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for managing Production Facility Telemetry using ThingsBoard ts_kv.
 * This is a wrapper over TB's native TimeseriesService following the CT/RV module pattern.
 *
 * Key changes from old architecture:
 * - Uses TB ts_kv tables instead of custom pf.telemetry table
 * - No TimescaleDB hypertables - uses TB native time-series storage
 * - Leverages TB's built-in data partitioning and retention
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfTelemetryService {

    private final TimeseriesService tbTimeseriesService;
    private final PfAlarmService alarmService;
    private final PfDataQualityService dataQualityService;
    private final PfModuleConfiguration config;

    /**
     * Processes incoming telemetry data with advanced data quality validation.
     * @return DataQualityResultDto with detailed validation results
     */
    public DataQualityResultDto processTelemetry(UUID tenantId, TelemetryDataDto data) {
        log.debug("Processing telemetry for entity: {}", data.getEntityId());

        // Validate data quality using advanced service
        DataQualityResultDto qualityResult = dataQualityService.validateTelemetry(data);
        double qualityScore = qualityResult.getOverallScore();

        if (qualityResult.isAccepted()) {
            // Store telemetry using TB ts_kv
            saveTelemetry(tenantId, data, qualityScore);

            // Evaluate alarms if enabled
            if (config.isAlarmEvaluationEnabled()) {
                evaluateAlarms(tenantId, data);
            }

            log.debug("Telemetry processed: entity={}, keys={}, quality={}, level={}",
                    data.getEntityId(), data.getValues().keySet(), qualityScore, qualityResult.getQualityLevel());
        } else {
            log.warn("Low quality telemetry rejected: entity={}, quality={}, level={}, issues={}",
                    data.getEntityId(), qualityScore, qualityResult.getQualityLevel(),
                    qualityResult.getIssues().size());
        }

        return qualityResult;
    }

    /**
     * Processes incoming telemetry data (legacy method for backward compatibility).
     * @return true if data was accepted
     */
    public boolean processTelemetrySimple(UUID tenantId, TelemetryDataDto data) {
        DataQualityResultDto result = processTelemetry(tenantId, data);
        return result.isAccepted();
    }

    /**
     * Batch processes telemetry data with advanced validation.
     * @return List of DataQualityResultDto with validation results for each entry
     */
    @Async
    public CompletableFuture<List<DataQualityResultDto>> processTelemetryBatch(UUID tenantId, List<TelemetryDataDto> batch) {
        log.info("Processing telemetry batch of {} items", batch.size());

        List<DataQualityResultDto> results = new ArrayList<>();
        int accepted = 0;

        for (TelemetryDataDto data : batch) {
            DataQualityResultDto qualityResult = dataQualityService.validateTelemetry(data);
            results.add(qualityResult);

            if (qualityResult.isAccepted()) {
                saveTelemetry(tenantId, data, qualityResult.getOverallScore());
                if (config.isAlarmEvaluationEnabled()) {
                    evaluateAlarms(tenantId, data);
                }
                accepted++;
            }
        }

        log.info("Batch processed: total={}, accepted={}", batch.size(), accepted);
        return CompletableFuture.completedFuture(results);
    }

    /**
     * Gets the latest telemetry for an entity using TB Telemetry API.
     */
    public TelemetryDataDto getLatestTelemetry(UUID tenantId, UUID entityId, String entityType) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(entityId, entityType);

        try {
            // Get all latest values for the entity
            ListenableFuture<List<TsKvEntry>> latestFuture = tbTimeseriesService.findAllLatest(tbTenantId, tbEntityId);
            List<TsKvEntry> latest = latestFuture.get();

            if (latest.isEmpty()) {
                return null;
            }

            Map<String, Object> values = new HashMap<>();
            Long timestamp = null;
            double avgQuality = 1.0; // Default quality since ts_kv doesn't store quality

            for (TsKvEntry entry : latest) {
                Object value = entry.getValue();
                if (value != null) {
                    values.put(entry.getKey(), value);
                }
                if (timestamp == null || entry.getTs() > timestamp) {
                    timestamp = entry.getTs();
                }
            }

            return TelemetryDataDto.builder()
                    .entityId(entityId)
                    .entityType(entityType)
                    .timestamp(timestamp)
                    .values(values)
                    .quality(avgQuality)
                    .build();

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting latest telemetry for entity {}: {}", entityId, e.getMessage());
            return null;
        }
    }

    /**
     * Gets historical telemetry for an entity within a time range using TB Telemetry API.
     */
    public List<TelemetryDataDto> getHistoricalTelemetry(UUID tenantId, UUID entityId, String entityType,
                                                         List<String> keys, Long fromTs, Long toTs) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(entityId, entityType);

        try {
            // If no keys provided, get all available keys
            if (keys == null || keys.isEmpty()) {
                ListenableFuture<List<TsKvEntry>> latestFuture = tbTimeseriesService.findAllLatest(tbTenantId, tbEntityId);
                List<TsKvEntry> latest = latestFuture.get();
                keys = latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList());
            }

            if (keys.isEmpty()) {
                return new ArrayList<>();
            }

            // Build queries for each key
            List<ReadTsKvQuery> queries = keys.stream()
                    .map(key -> new BaseReadTsKvQuery(key, fromTs, toTs, 0, 10000, Aggregation.NONE))
                    .collect(Collectors.toList());

            // Execute query
            ListenableFuture<List<TsKvEntry>> historyFuture = tbTimeseriesService.findAll(tbTenantId, tbEntityId, queries);
            List<TsKvEntry> history = historyFuture.get();

            // Group by timestamp
            Map<Long, TelemetryDataDto> groupedByTime = new HashMap<>();
            for (TsKvEntry entry : history) {
                long ts = entry.getTs();
                TelemetryDataDto dto = groupedByTime.computeIfAbsent(ts, k ->
                        TelemetryDataDto.builder()
                                .entityId(entityId)
                                .entityType(entityType)
                                .timestamp(ts)
                                .values(new HashMap<>())
                                .quality(1.0)
                                .build());

                Object value = entry.getValue();
                if (value != null) {
                    dto.getValues().put(entry.getKey(), value);
                }
            }

            return groupedByTime.values().stream()
                    .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting historical telemetry for entity {}: {}", entityId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets aggregated telemetry for an entity.
     */
    public List<TelemetryDataDto> getAggregatedTelemetry(UUID tenantId, UUID entityId, String entityType,
                                                          List<String> keys, Long fromTs, Long toTs,
                                                          Aggregation aggregation, long interval) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(entityId, entityType);

        try {
            // Build queries with aggregation
            List<ReadTsKvQuery> queries = keys.stream()
                    .map(key -> new BaseReadTsKvQuery(key, fromTs, toTs, interval, 10000, aggregation))
                    .collect(Collectors.toList());

            // Execute query
            ListenableFuture<List<TsKvEntry>> historyFuture = tbTimeseriesService.findAll(tbTenantId, tbEntityId, queries);
            List<TsKvEntry> history = historyFuture.get();

            // Group by timestamp
            Map<Long, TelemetryDataDto> groupedByTime = new HashMap<>();
            for (TsKvEntry entry : history) {
                long ts = entry.getTs();
                TelemetryDataDto dto = groupedByTime.computeIfAbsent(ts, k ->
                        TelemetryDataDto.builder()
                                .entityId(entityId)
                                .entityType(entityType)
                                .timestamp(ts)
                                .values(new HashMap<>())
                                .quality(1.0)
                                .build());

                Object value = entry.getValue();
                if (value != null) {
                    dto.getValues().put(entry.getKey(), value);
                }
            }

            return groupedByTime.values().stream()
                    .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting aggregated telemetry for entity {}: {}", entityId, e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper methods

    /**
     * Saves telemetry to TB ts_kv.
     */
    private void saveTelemetry(UUID tenantId, TelemetryDataDto data, double qualityScore) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(data.getEntityId(), data.getEntityType());

        long timestamp = data.getTimestamp() != null ? data.getTimestamp() : System.currentTimeMillis();

        List<TsKvEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.getValues().entrySet()) {
            TsKvEntry tsKvEntry = createTsKvEntry(entry.getKey(), entry.getValue(), timestamp);
            if (tsKvEntry != null) {
                entries.add(tsKvEntry);
            }
        }

        // Also store quality score as telemetry
        entries.add(new BasicTsKvEntry(timestamp, new DoubleDataEntry("_quality_score", qualityScore)));

        if (!entries.isEmpty()) {
            ListenableFuture<?> saveFuture = tbTimeseriesService.save(tbTenantId, tbEntityId, entries, 0);

            Futures.addCallback(saveFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    log.trace("Telemetry saved successfully for entity {}", data.getEntityId());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failed to save telemetry for entity {}: {}", data.getEntityId(), t.getMessage());
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private TsKvEntry createTsKvEntry(String key, Object value, long timestamp) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return new BasicTsKvEntry(timestamp, new DoubleDataEntry(key, ((Number) value).doubleValue()));
        } else if (value instanceof Boolean) {
            return new BasicTsKvEntry(timestamp, new BooleanDataEntry(key, (Boolean) value));
        } else {
            return new BasicTsKvEntry(timestamp, new StringDataEntry(key, value.toString()));
        }
    }

    private EntityId createEntityId(UUID id, String type) {
        if (type == null) {
            return new AssetId(id);
        }

        return switch (type.toLowerCase()) {
            case "pf_well", "well" -> new AssetId(id);
            case "pf_wellpad", "wellpad" -> new AssetId(id);
            case "pf_esp_system", "esp" -> new AssetId(id);
            case "pf_pcp_system", "pcp" -> new AssetId(id);
            case "pf_gas_lift_system", "gas_lift" -> new AssetId(id);
            case "pf_rod_pump_system", "rod_pump" -> new AssetId(id);
            case "device" -> new DeviceId(id);
            default -> new AssetId(id);
        };
    }

    private void evaluateAlarms(UUID tenantId, TelemetryDataDto data) {
        for (Map.Entry<String, Object> entry : data.getValues().entrySet()) {
            if (entry.getValue() instanceof Number) {
                // Note: Alarm evaluation is now primarily handled by Asset Profile Alarm Rules
                // This is for backward compatibility with custom alarm logic
                log.trace("Telemetry {} = {} for entity {}, alarm evaluation delegated to Asset Profile rules",
                        entry.getKey(), entry.getValue(), data.getEntityId());
            }
        }
    }
}
