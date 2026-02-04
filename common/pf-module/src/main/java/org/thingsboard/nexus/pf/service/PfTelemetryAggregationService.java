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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for aggregating telemetry data across different time intervals.
 * Uses ThingsBoard TimeseriesService for efficient aggregated queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfTelemetryAggregationService {

    private final TimeseriesService tbTimeseriesService;

    /**
     * Gets aggregated telemetry for an entity using TB's native aggregation.
     */
    public List<AggregatedTelemetry> getAggregatedTelemetry(
            UUID entityId,
            List<String> keys,
            Instant from,
            Instant to,
            AggregationInterval interval) {

        TenantId tenantId = TenantId.SYS_TENANT_ID;
        EntityId tbEntityId = new AssetId(entityId);

        long intervalMs = getIntervalMillis(interval);
        Aggregation aggregation = Aggregation.AVG;

        try {
            List<AggregatedTelemetry> results = new ArrayList<>();

            for (String key : keys) {
                // Build query with aggregation
                ReadTsKvQuery query = new BaseReadTsKvQuery(
                        key,
                        from.toEpochMilli(),
                        to.toEpochMilli(),
                        intervalMs,
                        10000,
                        aggregation
                );

                List<TsKvEntry> entries = tbTimeseriesService.findAll(tenantId, tbEntityId, List.of(query)).get();

                for (TsKvEntry entry : entries) {
                    if (entry.getValue() != null) {
                        results.add(AggregatedTelemetry.builder()
                                .timestamp(Instant.ofEpochMilli(entry.getTs()))
                                .key(key)
                                .avgValue(entry.getDoubleValue().orElse(0.0))
                                .minValue(0) // TB native aggregation returns single value
                                .maxValue(0) // For min/max, separate queries needed
                                .sampleCount(1)
                                .build());
                    }
                }
            }

            return results.stream()
                    .sorted(Comparator.comparing(AggregatedTelemetry::getTimestamp)
                            .thenComparing(AggregatedTelemetry::getKey))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting aggregated telemetry for entity {}: {}", entityId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets statistics for a variable over a time period using TB Telemetry API.
     */
    public VariableStatistics getVariableStatistics(UUID entityId, String key, Instant from, Instant to) {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        EntityId tbEntityId = new AssetId(entityId);

        try {
            // Get all raw values for the period
            ReadTsKvQuery query = new BaseReadTsKvQuery(
                    key,
                    from.toEpochMilli(),
                    to.toEpochMilli(),
                    0,
                    100000,
                    Aggregation.NONE
            );

            List<TsKvEntry> entries = tbTimeseriesService.findAll(tenantId, tbEntityId, List.of(query)).get();

            if (entries.isEmpty()) {
                return VariableStatistics.builder()
                        .entityId(entityId)
                        .key(key)
                        .from(from)
                        .to(to)
                        .sampleCount(0)
                        .build();
            }

            // Calculate statistics from raw values
            List<Double> values = entries.stream()
                    .map(e -> e.getDoubleValue().orElse(null))
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            if (values.isEmpty()) {
                return VariableStatistics.builder()
                        .entityId(entityId)
                        .key(key)
                        .from(from)
                        .to(to)
                        .sampleCount(0)
                        .build();
            }

            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / values.size();
            double min = values.get(0);
            double max = values.get(values.size() - 1);

            // Calculate standard deviation
            double sumSquaredDiff = values.stream()
                    .mapToDouble(v -> Math.pow(v - avg, 2))
                    .sum();
            double stdDev = Math.sqrt(sumSquaredDiff / values.size());

            // Calculate percentiles
            double median = getPercentile(values, 0.5);
            double percentile25 = getPercentile(values, 0.25);
            double percentile75 = getPercentile(values, 0.75);

            return VariableStatistics.builder()
                    .entityId(entityId)
                    .key(key)
                    .from(from)
                    .to(to)
                    .avgValue(avg)
                    .minValue(min)
                    .maxValue(max)
                    .stdDev(stdDev)
                    .sampleCount(values.size())
                    .median(median)
                    .percentile25(percentile25)
                    .percentile75(percentile75)
                    .build();

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting variable statistics for entity {} key {}: {}", entityId, key, e.getMessage());
            return VariableStatistics.builder()
                    .entityId(entityId)
                    .key(key)
                    .from(from)
                    .to(to)
                    .sampleCount(0)
                    .build();
        }
    }

    /**
     * Gets production summary for a well or wellpad.
     */
    public ProductionSummary getProductionSummary(UUID entityId, Instant from, Instant to) {
        // Get key production variables
        List<String> productionKeys = Arrays.asList(
                "production_bpd", "oil_rate_bpd", "water_rate_bpd", "gas_rate_mscfd"
        );

        Map<String, VariableStatistics> stats = new HashMap<>();
        for (String key : productionKeys) {
            try {
                VariableStatistics varStats = getVariableStatistics(entityId, key, from, to);
                if (varStats.getSampleCount() > 0) {
                    stats.put(key, varStats);
                }
            } catch (Exception e) {
                log.debug("No data for {} on entity {}", key, entityId);
            }
        }

        // Calculate totals and averages
        double avgOilRate = stats.containsKey("oil_rate_bpd") ?
                stats.get("oil_rate_bpd").getAvgValue() : 0;
        double avgWaterRate = stats.containsKey("water_rate_bpd") ?
                stats.get("water_rate_bpd").getAvgValue() : 0;
        double avgGasRate = stats.containsKey("gas_rate_mscfd") ?
                stats.get("gas_rate_mscfd").getAvgValue() : 0;
        double avgTotalLiquid = avgOilRate + avgWaterRate;

        double waterCut = avgTotalLiquid > 0 ? (avgWaterRate / avgTotalLiquid) * 100 : 0;
        double gor = avgOilRate > 0 ? (avgGasRate * 1000) / avgOilRate : 0; // scf/bbl

        long periodDays = ChronoUnit.DAYS.between(from, to);
        double cumulativeOil = avgOilRate * periodDays;
        double cumulativeWater = avgWaterRate * periodDays;
        double cumulativeGas = avgGasRate * periodDays;

        return ProductionSummary.builder()
                .entityId(entityId)
                .from(from)
                .to(to)
                .avgOilRateBpd(avgOilRate)
                .avgWaterRateBpd(avgWaterRate)
                .avgGasRateMscfd(avgGasRate)
                .avgTotalLiquidBpd(avgTotalLiquid)
                .waterCutPercent(waterCut)
                .gorScfBbl(gor)
                .cumulativeOilBbl(cumulativeOil)
                .cumulativeWaterBbl(cumulativeWater)
                .cumulativeGasMscf(cumulativeGas)
                .periodDays(periodDays)
                .variableStats(stats)
                .build();
    }

    /**
     * Gets efficiency metrics for lift systems.
     */
    public LiftSystemMetrics getLiftSystemMetrics(UUID systemId, String systemType,
                                                   Instant from, Instant to) {
        LiftSystemMetrics.LiftSystemMetricsBuilder builder = LiftSystemMetrics.builder()
                .systemId(systemId)
                .systemType(systemType)
                .from(from)
                .to(to);

        switch (systemType.toLowerCase()) {
            case "esp", "pf_esp_system" -> calculateEspMetrics(systemId, from, to, builder);
            case "pcp", "pf_pcp_system" -> calculatePcpMetrics(systemId, from, to, builder);
            case "gas_lift", "pf_gas_lift_system" -> calculateGasLiftMetrics(systemId, from, to, builder);
            case "rod_pump", "pf_rod_pump_system" -> calculateRodPumpMetrics(systemId, from, to, builder);
        }

        return builder.build();
    }

    private void calculateEspMetrics(UUID systemId, Instant from, Instant to,
                                      LiftSystemMetrics.LiftSystemMetricsBuilder builder) {
        VariableStatistics freqStats = getVariableStatistics(systemId, "frequency_hz", from, to);
        VariableStatistics currentStats = getVariableStatistics(systemId, "current_amps", from, to);
        VariableStatistics tempStats = getVariableStatistics(systemId, "temperature_motor_f", from, to);

        builder.avgFrequencyHz(freqStats.getAvgValue());
        builder.avgCurrentAmps(currentStats.getAvgValue());
        builder.avgMotorTempF(tempStats.getAvgValue());

        // Calculate uptime from frequency data
        try {
            TenantId tenantId = TenantId.SYS_TENANT_ID;
            EntityId tbEntityId = new AssetId(systemId);

            ReadTsKvQuery query = new BaseReadTsKvQuery(
                    "frequency_hz",
                    from.toEpochMilli(),
                    to.toEpochMilli(),
                    0,
                    100000,
                    Aggregation.NONE
            );

            List<TsKvEntry> entries = tbTimeseriesService.findAll(tenantId, tbEntityId, List.of(query)).get();

            if (!entries.isEmpty()) {
                long runningCount = entries.stream()
                        .filter(e -> e.getDoubleValue().orElse(0.0) > 0)
                        .count();
                double uptimePercent = (runningCount * 100.0) / entries.size();
                builder.uptimePercent(uptimePercent);
            }
        } catch (Exception e) {
            log.debug("Error calculating ESP uptime: {}", e.getMessage());
            builder.uptimePercent(0.0);
        }
    }

    private void calculatePcpMetrics(UUID systemId, Instant from, Instant to,
                                      LiftSystemMetrics.LiftSystemMetricsBuilder builder) {
        VariableStatistics rpmStats = getVariableStatistics(systemId, "rotor_rpm", from, to);
        VariableStatistics torqueStats = getVariableStatistics(systemId, "torque_ftlb", from, to);

        builder.avgRpm(rpmStats.getAvgValue());
        builder.avgTorqueFtLb(torqueStats.getAvgValue());
    }

    private void calculateGasLiftMetrics(UUID systemId, Instant from, Instant to,
                                          LiftSystemMetrics.LiftSystemMetricsBuilder builder) {
        VariableStatistics injRateStats = getVariableStatistics(systemId, "injection_rate_mscfd", from, to);
        VariableStatistics glrStats = getVariableStatistics(systemId, "gas_lift_ratio", from, to);

        builder.avgInjectionRateMscfd(injRateStats.getAvgValue());
        builder.avgGasLiftRatio(glrStats.getAvgValue());
    }

    private void calculateRodPumpMetrics(UUID systemId, Instant from, Instant to,
                                          LiftSystemMetrics.LiftSystemMetricsBuilder builder) {
        VariableStatistics spmStats = getVariableStatistics(systemId, "spm", from, to);
        VariableStatistics fillageStats = getVariableStatistics(systemId, "fillage_percent", from, to);

        builder.avgSpm(spmStats.getAvgValue());
        builder.avgFillagePercent(fillageStats.getAvgValue());
    }

    /**
     * Gets daily production totals using TB aggregation.
     */
    public List<DailyProduction> getDailyProduction(UUID entityId, Instant from, Instant to) {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        EntityId tbEntityId = new AssetId(entityId);

        long dayMs = 24 * 60 * 60 * 1000L;

        try {
            Map<Long, DailyProduction.DailyProductionBuilder> dailyBuilders = new HashMap<>();

            for (String key : Arrays.asList("oil_rate_bpd", "water_rate_bpd", "gas_rate_mscfd")) {
                ReadTsKvQuery query = new BaseReadTsKvQuery(
                        key,
                        from.toEpochMilli(),
                        to.toEpochMilli(),
                        dayMs,
                        10000,
                        Aggregation.AVG
                );

                List<TsKvEntry> entries = tbTimeseriesService.findAll(tenantId, tbEntityId, List.of(query)).get();

                for (TsKvEntry entry : entries) {
                    long dayTs = (entry.getTs() / dayMs) * dayMs;
                    DailyProduction.DailyProductionBuilder builder = dailyBuilders.computeIfAbsent(
                            dayTs,
                            ts -> DailyProduction.builder().date(Instant.ofEpochMilli(ts))
                    );

                    double value = entry.getDoubleValue().orElse(0.0);
                    switch (key) {
                        case "oil_rate_bpd" -> builder.oilBpd(value);
                        case "water_rate_bpd" -> builder.waterBpd(value);
                        case "gas_rate_mscfd" -> builder.gasMscfd(value);
                    }
                }
            }

            return dailyBuilders.values().stream()
                    .map(b -> {
                        DailyProduction dp = b.build();
                        return DailyProduction.builder()
                                .date(dp.getDate())
                                .oilBpd(dp.getOilBpd())
                                .waterBpd(dp.getWaterBpd())
                                .gasMscfd(dp.getGasMscfd())
                                .totalLiquidBpd(dp.getOilBpd() + dp.getWaterBpd())
                                .build();
                    })
                    .sorted(Comparator.comparing(DailyProduction::getDate))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting daily production for entity {}: {}", entityId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private long getIntervalMillis(AggregationInterval interval) {
        return switch (interval) {
            case RAW -> 0;
            case ONE_MINUTE -> 60 * 1000L;
            case ONE_HOUR -> 60 * 60 * 1000L;
            case ONE_DAY -> 24 * 60 * 60 * 1000L;
        };
    }

    private double getPercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    // Enums and DTOs

    public enum AggregationInterval {
        RAW,
        ONE_MINUTE,
        ONE_HOUR,
        ONE_DAY
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AggregatedTelemetry {
        private Instant timestamp;
        private String key;
        private double avgValue;
        private double minValue;
        private double maxValue;
        private int sampleCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VariableStatistics {
        private UUID entityId;
        private String key;
        private Instant from;
        private Instant to;
        private double avgValue;
        private double minValue;
        private double maxValue;
        private double stdDev;
        private long sampleCount;
        private double median;
        private double percentile25;
        private double percentile75;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductionSummary {
        private UUID entityId;
        private Instant from;
        private Instant to;
        private double avgOilRateBpd;
        private double avgWaterRateBpd;
        private double avgGasRateMscfd;
        private double avgTotalLiquidBpd;
        private double waterCutPercent;
        private double gorScfBbl;
        private double cumulativeOilBbl;
        private double cumulativeWaterBbl;
        private double cumulativeGasMscf;
        private long periodDays;
        private Map<String, VariableStatistics> variableStats;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LiftSystemMetrics {
        private UUID systemId;
        private String systemType;
        private Instant from;
        private Instant to;
        private double uptimePercent;
        // ESP specific
        private Double avgFrequencyHz;
        private Double avgCurrentAmps;
        private Double avgMotorTempF;
        // PCP specific
        private Double avgRpm;
        private Double avgTorqueFtLb;
        // Gas Lift specific
        private Double avgInjectionRateMscfd;
        private Double avgGasLiftRatio;
        // Rod Pump specific
        private Double avgSpm;
        private Double avgFillagePercent;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DailyProduction {
        private Instant date;
        private double oilBpd;
        private double waterBpd;
        private double gasMscfd;
        private double totalLiquidBpd;
    }
}
