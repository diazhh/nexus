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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.pf.config.PfModuleConfiguration;
import org.thingsboard.nexus.pf.dto.DataQualityResultDto;
import org.thingsboard.nexus.pf.dto.DataQualityResultDto.*;
import org.thingsboard.nexus.pf.dto.DataQualityRuleDto;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.nexus.pf.model.PfDataQualityRule;
import org.thingsboard.nexus.pf.repository.PfDataQualityRuleRepository;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Advanced Data Quality Validation Service.
 * Uses TB ts_kv for historical data access instead of custom pf.telemetry table.
 *
 * Provides comprehensive validation including:
 * - Physical and expected range validation
 * - Rate of change validation
 * - Statistical outlier detection (3-sigma rule)
 * - Timestamp validation
 * - Completeness checks
 *
 * Note: PfDataQualityRule is kept as a custom table because it's configuration data,
 * not operational data that should flow through TB's native systems.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfDataQualityService {

    private final PfDataQualityRuleRepository ruleRepository;
    private final TimeseriesService tbTimeseriesService;
    private final PfModuleConfiguration config;

    // Cache for recent values (for rate of change calculation)
    private final Map<String, TelemetryCache> recentValuesCache = new ConcurrentHashMap<>();

    // Cache for statistics (mean, stddev)
    private final Map<String, StatisticsCache> statisticsCache = new ConcurrentHashMap<>();

    // Cache TTL
    private static final long CACHE_TTL_MS = 300000; // 5 minutes

    /**
     * Validates telemetry data and returns detailed quality result.
     */
    public DataQualityResultDto validateTelemetry(TelemetryDataDto data) {
        DataQualityResultDto result = DataQualityResultDto.builder()
                .entityId(data.getEntityId())
                .variableResults(new ArrayList<>())
                .issues(new ArrayList<>())
                .build();

        double totalScore = 1.0;
        int validatedCount = 0;

        // 1. Validate timestamp
        TimestampValidation tsValidation = validateTimestamp(data.getTimestamp());
        result.setTimestampValidation(tsValidation);
        if (!tsValidation.isValid()) {
            totalScore *= 0.8;
            result.addIssue(ValidationIssue.builder()
                    .type(data.getTimestamp() == null ?
                            ValidationIssue.IssueType.MISSING_TIMESTAMP :
                            (data.getTimestamp() > System.currentTimeMillis() ?
                                    ValidationIssue.IssueType.FUTURE_TIMESTAMP :
                                    ValidationIssue.IssueType.STALE_TIMESTAMP))
                    .severity(ValidationIssue.IssueSeverity.MEDIUM)
                    .message(tsValidation.getIssue())
                    .actualValue(data.getTimestamp())
                    .scorePenalty(0.2)
                    .build());
        }

        // 2. Check for empty data
        if (data.getValues() == null || data.getValues().isEmpty()) {
            result.setOverallScore(0.0);
            result.setQualityLevel(QualityLevel.POOR);
            result.setAccepted(false);
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.MISSING_REQUIRED_VARIABLE)
                    .severity(ValidationIssue.IssueSeverity.CRITICAL)
                    .message("No telemetry values provided")
                    .scorePenalty(1.0)
                    .build());
            return result;
        }

        // 3. Validate each variable
        for (Map.Entry<String, Object> entry : data.getValues().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            VariableQualityResult varResult = validateVariable(
                    data.getEntityId(),
                    data.getEntityType(),
                    key,
                    value,
                    data.getTimestamp(),
                    result
            );

            result.addVariableResult(varResult);
            totalScore *= varResult.getQualityScore();
            validatedCount++;
        }

        // 4. Calculate completeness (if expected variables are configured)
        result.setCompletenessScore(1.0); // Default to 1.0, can be enhanced with expected variable list

        // 5. Calculate final score
        if (validatedCount > 0) {
            // Geometric mean of all variable scores
            totalScore = Math.pow(totalScore, 1.0 / validatedCount);
        }

        result.setOverallScore(Math.max(0.0, Math.min(1.0, totalScore)));
        result.setQualityLevel(QualityLevel.fromScore(result.getOverallScore()));
        result.setAccepted(result.getOverallScore() >= config.getMinDataQualityScore());

        // Update cache with new values
        updateCache(data);

        return result;
    }

    /**
     * Validates a single variable.
     */
    private VariableQualityResult validateVariable(UUID entityId, String entityType,
                                                    String variableKey, Object value,
                                                    Long timestamp,
                                                    DataQualityResultDto result) {
        VariableQualityResult varResult = VariableQualityResult.builder()
                .variableKey(variableKey)
                .value(value)
                .qualityScore(1.0)
                .rangeValid(true)
                .rateOfChangeValid(true)
                .outlierValid(true)
                .build();

        // Check for invalid values
        if (value == null) {
            varResult.setQualityScore(0.0);
            varResult.setRangeValid(false);
            varResult.setIssueDescription("Null value");
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.INVALID_VALUE)
                    .severity(ValidationIssue.IssueSeverity.HIGH)
                    .variableKey(variableKey)
                    .message("Null value received")
                    .scorePenalty(1.0)
                    .build());
            return varResult;
        }

        // Handle numeric values
        if (value instanceof Number) {
            double numValue = ((Number) value).doubleValue();

            // Check for NaN/Infinite
            if (Double.isNaN(numValue)) {
                varResult.setQualityScore(0.0);
                varResult.setRangeValid(false);
                result.addIssue(ValidationIssue.builder()
                        .type(ValidationIssue.IssueType.NAN_VALUE)
                        .severity(ValidationIssue.IssueSeverity.CRITICAL)
                        .variableKey(variableKey)
                        .message("NaN value received")
                        .scorePenalty(1.0)
                        .build());
                return varResult;
            }

            if (Double.isInfinite(numValue)) {
                varResult.setQualityScore(0.0);
                varResult.setRangeValid(false);
                result.addIssue(ValidationIssue.builder()
                        .type(ValidationIssue.IssueType.INFINITE_VALUE)
                        .severity(ValidationIssue.IssueSeverity.CRITICAL)
                        .variableKey(variableKey)
                        .message("Infinite value received")
                        .actualValue(numValue)
                        .scorePenalty(1.0)
                        .build());
                return varResult;
            }

            // Get applicable rule
            Optional<PfDataQualityRule> ruleOpt = getMostSpecificRule(variableKey, entityType, entityId);

            if (ruleOpt.isPresent()) {
                PfDataQualityRule rule = ruleOpt.get();

                // Range validation
                double rangeScore = validateRange(variableKey, numValue, rule, result);
                if (rangeScore < 1.0) {
                    varResult.setRangeValid(false);
                }
                varResult.setQualityScore(varResult.getQualityScore() * rangeScore);

                // Rate of change validation
                double rocScore = validateRateOfChange(entityId, variableKey, numValue, timestamp, rule, result);
                if (rocScore < 1.0) {
                    varResult.setRateOfChangeValid(false);
                }
                varResult.setQualityScore(varResult.getQualityScore() * rocScore);

                // Outlier detection
                double outlierScore = validateOutlier(entityId, variableKey, numValue, rule, result);
                if (outlierScore < 1.0) {
                    varResult.setOutlierValid(false);
                }
                varResult.setQualityScore(varResult.getQualityScore() * outlierScore);
            }
        }

        return varResult;
    }

    /**
     * Validates value against physical and expected ranges.
     */
    private double validateRange(String variableKey, double value, PfDataQualityRule rule,
                                  DataQualityResultDto result) {
        double score = 1.0;

        // Physical range (hard limits - data is invalid if outside)
        if (rule.getPhysicalMin() != null && value < rule.getPhysicalMin().doubleValue()) {
            score = 0.0;
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.OUT_OF_PHYSICAL_RANGE)
                    .severity(ValidationIssue.IssueSeverity.CRITICAL)
                    .variableKey(variableKey)
                    .message(String.format("%s value %.2f is below physical minimum %.2f",
                            variableKey, value, rule.getPhysicalMin().doubleValue()))
                    .actualValue(value)
                    .expectedValue(rule.getPhysicalMin())
                    .scorePenalty(1.0)
                    .build());
            return score;
        }

        if (rule.getPhysicalMax() != null && value > rule.getPhysicalMax().doubleValue()) {
            score = 0.0;
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.OUT_OF_PHYSICAL_RANGE)
                    .severity(ValidationIssue.IssueSeverity.CRITICAL)
                    .variableKey(variableKey)
                    .message(String.format("%s value %.2f is above physical maximum %.2f",
                            variableKey, value, rule.getPhysicalMax().doubleValue()))
                    .actualValue(value)
                    .expectedValue(rule.getPhysicalMax())
                    .scorePenalty(1.0)
                    .build());
            return score;
        }

        // Expected range (soft limits - data is suspicious if outside)
        if (rule.getExpectedMin() != null && value < rule.getExpectedMin().doubleValue()) {
            score *= 0.7;
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.OUT_OF_EXPECTED_RANGE)
                    .severity(ValidationIssue.IssueSeverity.MEDIUM)
                    .variableKey(variableKey)
                    .message(String.format("%s value %.2f is below expected minimum %.2f",
                            variableKey, value, rule.getExpectedMin().doubleValue()))
                    .actualValue(value)
                    .expectedValue(rule.getExpectedMin())
                    .scorePenalty(0.3)
                    .build());
        }

        if (rule.getExpectedMax() != null && value > rule.getExpectedMax().doubleValue()) {
            score *= 0.7;
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.OUT_OF_EXPECTED_RANGE)
                    .severity(ValidationIssue.IssueSeverity.MEDIUM)
                    .variableKey(variableKey)
                    .message(String.format("%s value %.2f is above expected maximum %.2f",
                            variableKey, value, rule.getExpectedMax().doubleValue()))
                    .actualValue(value)
                    .expectedValue(rule.getExpectedMax())
                    .scorePenalty(0.3)
                    .build());
        }

        return score;
    }

    /**
     * Validates rate of change.
     */
    private double validateRateOfChange(UUID entityId, String variableKey, double value,
                                         Long timestamp, PfDataQualityRule rule,
                                         DataQualityResultDto result) {
        if (rule.getMaxRateOfChangePercentPerMinute() == null &&
                rule.getMaxAbsoluteChangePerMinute() == null) {
            return 1.0;
        }

        String cacheKey = entityId + ":" + variableKey;
        TelemetryCache cached = recentValuesCache.get(cacheKey);

        if (cached == null || timestamp == null) {
            return 1.0; // No previous value to compare
        }

        long timeDiffMs = timestamp - cached.timestamp;
        if (timeDiffMs <= 0) {
            return 1.0; // Same or earlier timestamp
        }

        double timeDiffMinutes = timeDiffMs / 60000.0;
        double absoluteChange = Math.abs(value - cached.value);

        double score = 1.0;

        // Check percentage rate of change
        if (rule.getMaxRateOfChangePercentPerMinute() != null && cached.value != 0) {
            double percentChange = (absoluteChange / Math.abs(cached.value)) * 100;
            double percentChangePerMinute = percentChange / timeDiffMinutes;
            double maxRoc = rule.getMaxRateOfChangePercentPerMinute().doubleValue();

            if (percentChangePerMinute > maxRoc) {
                score *= 0.6;
                result.addIssue(ValidationIssue.builder()
                        .type(ValidationIssue.IssueType.EXCESSIVE_RATE_OF_CHANGE)
                        .severity(ValidationIssue.IssueSeverity.HIGH)
                        .variableKey(variableKey)
                        .message(String.format("%s changed %.1f%%/min (max: %.1f%%/min)",
                                variableKey, percentChangePerMinute, maxRoc))
                        .actualValue(percentChangePerMinute)
                        .expectedValue(maxRoc)
                        .scorePenalty(0.4)
                        .build());
            }
        }

        // Check absolute rate of change
        if (rule.getMaxAbsoluteChangePerMinute() != null) {
            double absoluteChangePerMinute = absoluteChange / timeDiffMinutes;
            double maxAbsRoc = rule.getMaxAbsoluteChangePerMinute().doubleValue();

            if (absoluteChangePerMinute > maxAbsRoc) {
                score *= 0.6;
                result.addIssue(ValidationIssue.builder()
                        .type(ValidationIssue.IssueType.EXCESSIVE_RATE_OF_CHANGE)
                        .severity(ValidationIssue.IssueSeverity.HIGH)
                        .variableKey(variableKey)
                        .message(String.format("%s changed %.2f/min (max: %.2f/min)",
                                variableKey, absoluteChangePerMinute, maxAbsRoc))
                        .actualValue(absoluteChangePerMinute)
                        .expectedValue(maxAbsRoc)
                        .scorePenalty(0.4)
                        .build());
            }
        }

        return score;
    }

    /**
     * Validates against statistical outliers using 3-sigma rule.
     */
    private double validateOutlier(UUID entityId, String variableKey, double value,
                                    PfDataQualityRule rule, DataQualityResultDto result) {
        String statsKey = entityId + ":" + variableKey;
        StatisticsCache stats = statisticsCache.get(statsKey);

        // If no cached stats or too few samples, try to compute from TB ts_kv
        if (stats == null || stats.isExpired() || stats.sampleCount < rule.getMinSamplesForStatistics()) {
            stats = computeStatisticsFromTb(entityId, variableKey, rule.getMinSamplesForStatistics());
            if (stats != null) {
                statisticsCache.put(statsKey, stats);
            }
        }

        if (stats == null || stats.stdDev == 0 || stats.sampleCount < rule.getMinSamplesForStatistics()) {
            return 1.0; // Not enough data for outlier detection
        }

        double sigmaThreshold = rule.getOutlierSigmaThreshold() != null ?
                rule.getOutlierSigmaThreshold().doubleValue() : 3.0;

        double zScore = Math.abs(value - stats.mean) / stats.stdDev;

        if (zScore > sigmaThreshold) {
            double score = 0.5; // Significant penalty for statistical outlier
            result.addIssue(ValidationIssue.builder()
                    .type(ValidationIssue.IssueType.STATISTICAL_OUTLIER)
                    .severity(ValidationIssue.IssueSeverity.HIGH)
                    .variableKey(variableKey)
                    .message(String.format("%s value %.2f is %.1f sigma from mean %.2f (threshold: %.1f sigma)",
                            variableKey, value, zScore, stats.mean, sigmaThreshold))
                    .actualValue(value)
                    .expectedValue(stats.mean)
                    .scorePenalty(0.5)
                    .build());
            return score;
        }

        return 1.0;
    }

    /**
     * Validates timestamp.
     */
    private TimestampValidation validateTimestamp(Long timestamp) {
        if (timestamp == null) {
            return TimestampValidation.builder()
                    .timestamp(null)
                    .valid(false)
                    .ageMillis(0)
                    .issue("Missing timestamp")
                    .build();
        }

        long now = System.currentTimeMillis();
        long age = now - timestamp;

        if (timestamp > now) {
            return TimestampValidation.builder()
                    .timestamp(timestamp)
                    .valid(false)
                    .ageMillis(age)
                    .issue("Timestamp is in the future")
                    .build();
        }

        // Consider data stale if older than 1 hour
        if (age > 3600000) {
            return TimestampValidation.builder()
                    .timestamp(timestamp)
                    .valid(false)
                    .ageMillis(age)
                    .issue(String.format("Timestamp is stale (%.1f hours old)", age / 3600000.0))
                    .build();
        }

        return TimestampValidation.builder()
                .timestamp(timestamp)
                .valid(true)
                .ageMillis(age)
                .build();
    }

    /**
     * Gets the most specific applicable rule for a variable.
     */
    private Optional<PfDataQualityRule> getMostSpecificRule(String variableKey, String entityType, UUID entityId) {
        List<PfDataQualityRule> rules = ruleRepository.findApplicableRules(variableKey, entityType, entityId);
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
    }

    /**
     * Computes statistics from TB ts_kv historical data.
     */
    private StatisticsCache computeStatisticsFromTb(UUID entityId, String variableKey, int minSamples) {
        try {
            TenantId tenantId = TenantId.SYS_TENANT_ID; // Use system tenant or get from context
            EntityId tbEntityId = new AssetId(entityId);

            long now = System.currentTimeMillis();
            long from = now - 24 * 60 * 60 * 1000; // Last 24 hours

            ReadTsKvQuery query = new BaseReadTsKvQuery(variableKey, from, now, 0, 10000, Aggregation.NONE);
            ListenableFuture<List<TsKvEntry>> future = tbTimeseriesService.findAll(tenantId, tbEntityId, List.of(query));

            List<TsKvEntry> data = future.get();

            if (data.size() < minSamples) {
                return null;
            }

            // Calculate mean
            double sum = 0;
            int count = 0;
            for (TsKvEntry entry : data) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    sum += ((Number) value).doubleValue();
                    count++;
                }
            }

            if (count < minSamples) {
                return null;
            }

            double mean = sum / count;

            // Calculate standard deviation
            double sumSquaredDiff = 0;
            for (TsKvEntry entry : data) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    double diff = ((Number) value).doubleValue() - mean;
                    sumSquaredDiff += diff * diff;
                }
            }

            double stdDev = Math.sqrt(sumSquaredDiff / count);

            return new StatisticsCache(mean, stdDev, count, System.currentTimeMillis());

        } catch (InterruptedException | ExecutionException e) {
            log.warn("Error computing statistics for {}:{}: {}", entityId, variableKey, e.getMessage());
            return null;
        }
    }

    /**
     * Updates the cache with new values.
     */
    private void updateCache(TelemetryDataDto data) {
        if (data.getValues() == null) return;

        for (Map.Entry<String, Object> entry : data.getValues().entrySet()) {
            if (entry.getValue() instanceof Number) {
                String cacheKey = data.getEntityId() + ":" + entry.getKey();
                recentValuesCache.put(cacheKey, new TelemetryCache(
                        ((Number) entry.getValue()).doubleValue(),
                        data.getTimestamp() != null ? data.getTimestamp() : System.currentTimeMillis()
                ));
            }
        }
    }

    // Rule Management

    /**
     * Saves a data quality rule.
     */
    @Transactional
    public DataQualityRuleDto saveRule(DataQualityRuleDto dto) {
        PfDataQualityRule rule = dto.getId() != null ?
                ruleRepository.findById(dto.getId()).orElse(new PfDataQualityRule()) :
                new PfDataQualityRule();

        rule.setVariableKey(dto.getVariableKey());
        rule.setEntityType(dto.getEntityType());
        rule.setEntityId(dto.getEntityId());
        rule.setPhysicalMin(dto.getPhysicalMin());
        rule.setPhysicalMax(dto.getPhysicalMax());
        rule.setExpectedMin(dto.getExpectedMin());
        rule.setExpectedMax(dto.getExpectedMax());
        rule.setMaxRateOfChangePercentPerMinute(dto.getMaxRateOfChangePercentPerMinute());
        rule.setMaxAbsoluteChangePerMinute(dto.getMaxAbsoluteChangePerMinute());
        rule.setOutlierSigmaThreshold(dto.getOutlierSigmaThreshold());
        rule.setMinSamplesForStatistics(dto.getMinSamplesForStatistics());
        rule.setUnit(dto.getUnit());
        rule.setDescription(dto.getDescription());
        rule.setEnabled(dto.getEnabled());

        PfDataQualityRule saved = ruleRepository.save(rule);

        dto.setId(saved.getId());
        dto.setCreatedTime(saved.getCreatedTime());
        dto.setUpdatedTime(saved.getUpdatedTime());

        return dto;
    }

    /**
     * Gets all rules.
     */
    public List<DataQualityRuleDto> getAllRules() {
        return ruleRepository.findByEnabledTrue().stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Gets rules for entity type.
     */
    public List<DataQualityRuleDto> getRulesForEntityType(String entityType) {
        return ruleRepository.findByEntityTypeAndEnabledTrue(entityType).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Gets rules for a specific entity.
     */
    public List<DataQualityRuleDto> getRulesForEntity(UUID entityId) {
        return ruleRepository.findByEntityIdAndEnabledTrue(entityId).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Gets global rules (no entity type or ID).
     */
    public List<DataQualityRuleDto> getGlobalRules() {
        return ruleRepository.findByEntityTypeIsNullAndEntityIdIsNullAndEnabledTrue().stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Deletes a rule.
     */
    @Transactional
    public void deleteRule(UUID ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    /**
     * Clears all caches.
     */
    public void clearCaches() {
        recentValuesCache.clear();
        statisticsCache.clear();
        log.info("Data quality caches cleared");
    }

    /**
     * Gets statistics summary.
     */
    public DataQualityStatistics getStatistics() {
        List<PfDataQualityRule> allRules = ruleRepository.findByEnabledTrue();

        int globalRules = 0;
        int entityTypeRules = 0;
        int entitySpecificRules = 0;

        for (PfDataQualityRule rule : allRules) {
            if (rule.getEntityId() != null) {
                entitySpecificRules++;
            } else if (rule.getEntityType() != null) {
                entityTypeRules++;
            } else {
                globalRules++;
            }
        }

        return DataQualityStatistics.builder()
                .totalRules(allRules.size())
                .globalRules(globalRules)
                .entityTypeRules(entityTypeRules)
                .entitySpecificRules(entitySpecificRules)
                .cachedStatisticsEntries(statisticsCache.size())
                .cachedTelemetryEntries(recentValuesCache.size())
                .build();
    }

    /**
     * Statistics summary class.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DataQualityStatistics {
        private int totalRules;
        private int globalRules;
        private int entityTypeRules;
        private int entitySpecificRules;
        private int cachedStatisticsEntries;
        private int cachedTelemetryEntries;
    }

    /**
     * Creates default rules for common ESP variables.
     */
    @Transactional
    public void createDefaultEspRules() {
        createRuleIfNotExists("frequency_hz", "pf_esp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(100),
                BigDecimal.valueOf(30), BigDecimal.valueOf(70),
                BigDecimal.valueOf(5), null, "Hz", "ESP operating frequency");

        createRuleIfNotExists("current_amps", "pf_esp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(200),
                BigDecimal.valueOf(20), BigDecimal.valueOf(100),
                BigDecimal.valueOf(10), null, "A", "ESP motor current");

        createRuleIfNotExists("temperature_motor_f", "pf_esp_system", null,
                BigDecimal.valueOf(32), BigDecimal.valueOf(400),
                BigDecimal.valueOf(150), BigDecimal.valueOf(300),
                BigDecimal.valueOf(5), null, "°F", "ESP motor temperature");

        createRuleIfNotExists("pip_psi", "pf_esp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(50), BigDecimal.valueOf(500),
                BigDecimal.valueOf(10), null, "psi", "Pump intake pressure");

        createRuleIfNotExists("vibration_g", "pf_esp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(10),
                BigDecimal.ZERO, BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(20), null, "g", "ESP vibration");
    }

    /**
     * Creates default rules for common PCP variables.
     */
    @Transactional
    public void createDefaultPcpRules() {
        createRuleIfNotExists("rotor_rpm", "pf_pcp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(600),
                BigDecimal.valueOf(50), BigDecimal.valueOf(400),
                BigDecimal.valueOf(10), null, "RPM", "PCP rotor speed");

        createRuleIfNotExists("torque_ftlb", "pf_pcp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(10000),
                BigDecimal.valueOf(100), BigDecimal.valueOf(3000),
                BigDecimal.valueOf(15), null, "ft-lb", "PCP torque");

        createRuleIfNotExists("temperature_stator_f", "pf_pcp_system", null,
                BigDecimal.valueOf(32), BigDecimal.valueOf(350),
                BigDecimal.valueOf(100), BigDecimal.valueOf(280),
                BigDecimal.valueOf(3), null, "°F", "PCP stator temperature");

        createRuleIfNotExists("drive_current_amps", "pf_pcp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(150),
                BigDecimal.valueOf(10), BigDecimal.valueOf(80),
                BigDecimal.valueOf(10), null, "A", "PCP drive current");

        createRuleIfNotExists("intake_pressure_psi", "pf_pcp_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(3000),
                BigDecimal.valueOf(50), BigDecimal.valueOf(500),
                BigDecimal.valueOf(10), null, "psi", "PCP intake pressure");
    }

    /**
     * Creates default rules for common Gas Lift variables.
     */
    @Transactional
    public void createDefaultGasLiftRules() {
        createRuleIfNotExists("injection_rate_mscfd", "pf_gas_lift_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(100), BigDecimal.valueOf(2000),
                BigDecimal.valueOf(15), null, "Mscf/d", "Gas injection rate");

        createRuleIfNotExists("injection_pressure_psi", "pf_gas_lift_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(500), BigDecimal.valueOf(2500),
                BigDecimal.valueOf(10), null, "psi", "Gas injection pressure");

        createRuleIfNotExists("casing_pressure_psi", "pf_gas_lift_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(200), BigDecimal.valueOf(2000),
                BigDecimal.valueOf(10), null, "psi", "Casing pressure");

        createRuleIfNotExists("tubing_pressure_psi", "pf_gas_lift_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(3000),
                BigDecimal.valueOf(50), BigDecimal.valueOf(500),
                BigDecimal.valueOf(10), null, "psi", "Tubing head pressure");

        createRuleIfNotExists("gas_lift_ratio", "pf_gas_lift_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(10),
                BigDecimal.valueOf(0.2), BigDecimal.valueOf(3),
                BigDecimal.valueOf(20), null, "Mscf/bbl", "Gas lift ratio");
    }

    /**
     * Creates default rules for common Rod Pump variables.
     */
    @Transactional
    public void createDefaultRodPumpRules() {
        createRuleIfNotExists("spm", "pf_rod_pump_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(20),
                BigDecimal.valueOf(4), BigDecimal.valueOf(12),
                BigDecimal.valueOf(10), null, "SPM", "Strokes per minute");

        createRuleIfNotExists("peak_load_lb", "pf_rod_pump_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(50000),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(25000),
                BigDecimal.valueOf(10), null, "lb", "Peak polished rod load");

        createRuleIfNotExists("min_load_lb", "pf_rod_pump_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(30000),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(15000),
                BigDecimal.valueOf(10), null, "lb", "Minimum polished rod load");

        createRuleIfNotExists("motor_current_amps", "pf_rod_pump_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(200),
                BigDecimal.valueOf(20), BigDecimal.valueOf(100),
                BigDecimal.valueOf(10), null, "A", "Prime mover current");

        createRuleIfNotExists("fillage_percent", "pf_rod_pump_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(100),
                BigDecimal.valueOf(60), BigDecimal.valueOf(100),
                BigDecimal.valueOf(5), null, "%", "Pump fillage");

        createRuleIfNotExists("counterbalance_percent", "pf_rod_pump_system", null,
                BigDecimal.ZERO, BigDecimal.valueOf(150),
                BigDecimal.valueOf(80), BigDecimal.valueOf(110),
                BigDecimal.valueOf(5), null, "%", "Counterbalance");
    }

    private void createRuleIfNotExists(String variableKey, String entityType, UUID entityId,
                                        BigDecimal physMin, BigDecimal physMax,
                                        BigDecimal expMin, BigDecimal expMax,
                                        BigDecimal maxRoc, BigDecimal maxAbsRoc,
                                        String unit, String description) {
        if (!ruleRepository.existsByVariableKeyAndEntityTypeAndEntityId(variableKey, entityType, entityId)) {
            PfDataQualityRule rule = PfDataQualityRule.builder()
                    .variableKey(variableKey)
                    .entityType(entityType)
                    .entityId(entityId)
                    .physicalMin(physMin)
                    .physicalMax(physMax)
                    .expectedMin(expMin)
                    .expectedMax(expMax)
                    .maxRateOfChangePercentPerMinute(maxRoc)
                    .maxAbsoluteChangePerMinute(maxAbsRoc)
                    .unit(unit)
                    .description(description)
                    .enabled(true)
                    .build();
            ruleRepository.save(rule);
            log.info("Created default data quality rule for {}", variableKey);
        }
    }

    private DataQualityRuleDto mapToDto(PfDataQualityRule rule) {
        return DataQualityRuleDto.builder()
                .id(rule.getId())
                .variableKey(rule.getVariableKey())
                .entityType(rule.getEntityType())
                .entityId(rule.getEntityId())
                .physicalMin(rule.getPhysicalMin())
                .physicalMax(rule.getPhysicalMax())
                .expectedMin(rule.getExpectedMin())
                .expectedMax(rule.getExpectedMax())
                .maxRateOfChangePercentPerMinute(rule.getMaxRateOfChangePercentPerMinute())
                .maxAbsoluteChangePerMinute(rule.getMaxAbsoluteChangePerMinute())
                .outlierSigmaThreshold(rule.getOutlierSigmaThreshold())
                .minSamplesForStatistics(rule.getMinSamplesForStatistics())
                .unit(rule.getUnit())
                .description(rule.getDescription())
                .enabled(rule.getEnabled())
                .createdTime(rule.getCreatedTime())
                .updatedTime(rule.getUpdatedTime())
                .build();
    }

    // Cache classes

    private static class TelemetryCache {
        final double value;
        final long timestamp;

        TelemetryCache(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    private static class StatisticsCache {
        final double mean;
        final double stdDev;
        final int sampleCount;
        final long computedAt;

        StatisticsCache(double mean, double stdDev, int sampleCount, long computedAt) {
            this.mean = mean;
            this.stdDev = stdDev;
            this.sampleCount = sampleCount;
            this.computedAt = computedAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - computedAt > CACHE_TTL_MS;
        }
    }
}
