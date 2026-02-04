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
package org.thingsboard.nexus.po.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.HealthLevel;
import org.thingsboard.nexus.po.dto.HealthScoreDto;
import org.thingsboard.server.common.data.asset.Asset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for calculating and managing equipment health scores.
 *
 * Health scores are stored as TB SERVER_SCOPE attributes on assets:
 * - health_score: Overall health score (0.0 - 1.0)
 * - health_level: EXCELLENT, GOOD, FAIR, POOR, CRITICAL
 * - failure_probability: Probability of failure (0.0 - 1.0)
 * - remaining_useful_life_days: Estimated days until failure
 * - health_factors: JSON map of contributing factors
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoHealthScoreService {

    private final PoAssetService assetService;
    private final PoAttributeService attributeService;
    private final PoModuleConfiguration config;
    private final ObjectMapper objectMapper;

    /**
     * Calculates health score for a well or lift system.
     */
    public HealthScoreDto calculateHealthScore(UUID tenantId, UUID assetId) {
        log.info("Calculating health score for asset: {}", assetId);

        Optional<Asset> assetOpt = assetService.getAssetById(tenantId, assetId);
        if (assetOpt.isEmpty()) {
            log.warn("Asset not found: {}", assetId);
            return null;
        }

        Asset asset = assetOpt.get();

        // Get current attributes
        Map<String, Object> currentAttrs = attributeService.getAttributesAsMap(assetId);

        // Calculate health factors based on asset type
        Map<String, Double> factors = calculateHealthFactors(asset.getType(), currentAttrs);

        // Calculate overall score (weighted average of factors)
        double overallScore = calculateOverallScore(factors);

        // Calculate failure probability (inverse of health)
        double failureProbability = calculateFailureProbability(overallScore, factors);

        // Estimate remaining useful life
        Integer remainingLife = estimateRemainingUsefulLife(overallScore, failureProbability);

        // Detect issues
        List<HealthScoreDto.HealthIssue> issues = detectHealthIssues(asset.getType(), currentAttrs, factors);

        // Get previous score for trend
        Double previousScore = attributeService.getDoubleAttribute(assetId, HealthScoreDto.ATTR_HEALTH_SCORE)
                .orElse(null);

        // Determine trend
        HealthScoreDto.HealthTrend trend = calculateTrend(overallScore, previousScore);

        // Build DTO
        HealthScoreDto healthScore = HealthScoreDto.builder()
                .assetId(assetId)
                .tenantId(tenantId)
                .assetType(asset.getType())
                .assetName(asset.getName())
                .score(overallScore)
                .level(HealthLevel.fromScore(overallScore))
                .failureProbability(failureProbability)
                .remainingUsefulLifeDays(remainingLife)
                .factors(factors)
                .issues(issues)
                .calculatedAt(System.currentTimeMillis())
                .trend(trend)
                .previousScore(previousScore)
                .build();

        // Save to TB attributes
        saveHealthScore(assetId, healthScore);

        log.info("Health score calculated for {}: score={}, level={}", asset.getName(), overallScore, healthScore.getLevel());
        return healthScore;
    }

    /**
     * Gets the current health score for an asset (from attributes).
     */
    public Optional<HealthScoreDto> getHealthScore(UUID tenantId, UUID assetId) {
        Optional<Asset> assetOpt = assetService.getAssetById(tenantId, assetId);
        if (assetOpt.isEmpty()) {
            return Optional.empty();
        }

        Asset asset = assetOpt.get();
        Map<String, Object> attrs = attributeService.getAttributesAsMap(assetId,
                List.of(HealthScoreDto.ATTR_HEALTH_SCORE, HealthScoreDto.ATTR_HEALTH_LEVEL,
                        HealthScoreDto.ATTR_FAILURE_PROBABILITY, HealthScoreDto.ATTR_REMAINING_USEFUL_LIFE,
                        HealthScoreDto.ATTR_HEALTH_FACTORS, HealthScoreDto.ATTR_HEALTH_UPDATED_AT));

        if (!attrs.containsKey(HealthScoreDto.ATTR_HEALTH_SCORE)) {
            return Optional.empty();
        }

        Double score = (Double) attrs.get(HealthScoreDto.ATTR_HEALTH_SCORE);
        String levelStr = (String) attrs.get(HealthScoreDto.ATTR_HEALTH_LEVEL);
        Double failureProb = (Double) attrs.get(HealthScoreDto.ATTR_FAILURE_PROBABILITY);
        Number remainingLife = (Number) attrs.get(HealthScoreDto.ATTR_REMAINING_USEFUL_LIFE);
        Number updatedAt = (Number) attrs.get(HealthScoreDto.ATTR_HEALTH_UPDATED_AT);

        return Optional.of(HealthScoreDto.builder()
                .assetId(assetId)
                .tenantId(tenantId)
                .assetType(asset.getType())
                .assetName(asset.getName())
                .score(score)
                .level(levelStr != null ? HealthLevel.valueOf(levelStr) : HealthLevel.fromScore(score))
                .failureProbability(failureProb)
                .remainingUsefulLifeDays(remainingLife != null ? remainingLife.intValue() : null)
                .calculatedAt(updatedAt != null ? updatedAt.longValue() : null)
                .build());
    }

    /**
     * Gets health scores for all wells in a tenant.
     */
    public List<HealthScoreDto> getWellHealthScores(UUID tenantId, int page, int size) {
        Page<Asset> wells = assetService.getWells(tenantId, page, size);
        List<HealthScoreDto> healthScores = new ArrayList<>();

        for (Asset well : wells.getContent()) {
            getHealthScore(tenantId, well.getId().getId())
                    .ifPresent(healthScores::add);
        }

        return healthScores;
    }

    /**
     * Gets wells by health level.
     */
    public List<HealthScoreDto> getWellsByHealthLevel(UUID tenantId, HealthLevel level) {
        List<HealthScoreDto> allHealthScores = getWellHealthScores(tenantId, 0, 1000);

        return allHealthScores.stream()
                .filter(hs -> hs.getLevel() == level)
                .toList();
    }

    /**
     * Gets critical wells (health level = CRITICAL or POOR).
     */
    public List<HealthScoreDto> getCriticalWells(UUID tenantId) {
        List<HealthScoreDto> allHealthScores = getWellHealthScores(tenantId, 0, 1000);

        return allHealthScores.stream()
                .filter(hs -> hs.getLevel() == HealthLevel.CRITICAL || hs.getLevel() == HealthLevel.POOR)
                .toList();
    }

    /**
     * Calculates health summary for a tenant.
     */
    public Map<HealthLevel, Long> getHealthSummary(UUID tenantId) {
        List<HealthScoreDto> allHealthScores = getWellHealthScores(tenantId, 0, 1000);

        Map<HealthLevel, Long> summary = new HashMap<>();
        for (HealthLevel level : HealthLevel.values()) {
            summary.put(level, 0L);
        }

        for (HealthScoreDto hs : allHealthScores) {
            summary.merge(hs.getLevel(), 1L, Long::sum);
        }

        return summary;
    }

    // Private helper methods

    private Map<String, Double> calculateHealthFactors(String assetType, Map<String, Object> attrs) {
        Map<String, Double> factors = new HashMap<>();

        // Common factors for all asset types
        factors.put("data_quality", calculateDataQualityFactor(attrs));
        factors.put("uptime", calculateUptimeFactor(attrs));

        // Asset-specific factors
        switch (assetType) {
            case PoAssetService.ASSET_TYPE_WELL -> {
                factors.put("production_rate", calculateProductionRateFactor(attrs));
                factors.put("water_cut", calculateWaterCutFactor(attrs));
            }
            case PoAssetService.ASSET_TYPE_ESP -> {
                factors.put("motor_temperature", calculateMotorTemperatureFactor(attrs));
                factors.put("motor_load", calculateMotorLoadFactor(attrs));
                factors.put("vibration", calculateVibrationFactor(attrs));
                factors.put("amperage", calculateAmperageFactor(attrs));
            }
            case PoAssetService.ASSET_TYPE_PCP -> {
                factors.put("torque", calculateTorqueFactor(attrs));
                factors.put("rpm_stability", calculateRpmStabilityFactor(attrs));
            }
            case PoAssetService.ASSET_TYPE_GAS_LIFT -> {
                factors.put("gas_efficiency", calculateGasEfficiencyFactor(attrs));
                factors.put("valve_performance", calculateValvePerformanceFactor(attrs));
            }
            case PoAssetService.ASSET_TYPE_ROD_PUMP -> {
                factors.put("pump_fillage", calculatePumpFillageFactor(attrs));
                factors.put("rod_load", calculateRodLoadFactor(attrs));
            }
        }

        return factors;
    }

    private double calculateOverallScore(Map<String, Double> factors) {
        if (factors.isEmpty()) {
            return 0.5; // Default to fair if no factors
        }

        // Weighted average - critical factors have higher weight
        Map<String, Double> weights = Map.of(
                "data_quality", 0.1,
                "uptime", 0.15,
                "motor_temperature", 0.25,
                "motor_load", 0.15,
                "vibration", 0.2,
                "production_rate", 0.15
        );

        double weightedSum = 0;
        double totalWeight = 0;

        for (Map.Entry<String, Double> factor : factors.entrySet()) {
            double weight = weights.getOrDefault(factor.getKey(), 0.1);
            weightedSum += factor.getValue() * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.5;
    }

    private double calculateFailureProbability(double healthScore, Map<String, Double> factors) {
        // Simple inverse relationship with some adjustments
        double baseProbability = 1.0 - healthScore;

        // Increase probability if critical factors are low
        if (factors.containsKey("motor_temperature") && factors.get("motor_temperature") < 0.3) {
            baseProbability *= 1.5;
        }
        if (factors.containsKey("vibration") && factors.get("vibration") < 0.3) {
            baseProbability *= 1.3;
        }

        return Math.min(baseProbability, 1.0);
    }

    private Integer estimateRemainingUsefulLife(double healthScore, double failureProbability) {
        if (healthScore >= 0.9) return 365; // 1 year
        if (healthScore >= 0.75) return 180; // 6 months
        if (healthScore >= 0.5) return 90; // 3 months
        if (healthScore >= 0.25) return 30; // 1 month
        return 7; // 1 week
    }

    private List<HealthScoreDto.HealthIssue> detectHealthIssues(String assetType, Map<String, Object> attrs, Map<String, Double> factors) {
        List<HealthScoreDto.HealthIssue> issues = new ArrayList<>();

        // Check for low factor scores
        for (Map.Entry<String, Double> factor : factors.entrySet()) {
            if (factor.getValue() < 0.5) {
                String severity = factor.getValue() < 0.25 ? "CRITICAL" : "HIGH";
                issues.add(HealthScoreDto.HealthIssue.builder()
                        .code("LOW_" + factor.getKey().toUpperCase())
                        .description("Low " + factor.getKey().replace("_", " ") + " score")
                        .severity(severity)
                        .recommendation("Investigate and address " + factor.getKey().replace("_", " ") + " issues")
                        .impact(1.0 - factor.getValue())
                        .build());
            }
        }

        return issues;
    }

    private HealthScoreDto.HealthTrend calculateTrend(double currentScore, Double previousScore) {
        if (previousScore == null) return HealthScoreDto.HealthTrend.UNKNOWN;

        double diff = currentScore - previousScore;
        if (diff > 0.05) return HealthScoreDto.HealthTrend.IMPROVING;
        if (diff < -0.05) return HealthScoreDto.HealthTrend.DECLINING;
        return HealthScoreDto.HealthTrend.STABLE;
    }

    private void saveHealthScore(UUID assetId, HealthScoreDto healthScore) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(HealthScoreDto.ATTR_HEALTH_SCORE, healthScore.getScore());
        attrs.put(HealthScoreDto.ATTR_HEALTH_LEVEL, healthScore.getLevel().name());
        attrs.put(HealthScoreDto.ATTR_FAILURE_PROBABILITY, healthScore.getFailureProbability());
        attrs.put(HealthScoreDto.ATTR_REMAINING_USEFUL_LIFE, healthScore.getRemainingUsefulLifeDays());
        attrs.put(HealthScoreDto.ATTR_HEALTH_UPDATED_AT, healthScore.getCalculatedAt());

        if (healthScore.getFactors() != null) {
            try {
                attrs.put(HealthScoreDto.ATTR_HEALTH_FACTORS, objectMapper.writeValueAsString(healthScore.getFactors()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize health factors: {}", e.getMessage());
            }
        }

        attributeService.saveServerAttributes(assetId, attrs);
    }

    // Factor calculation methods (simplified implementations)
    private double calculateDataQualityFactor(Map<String, Object> attrs) {
        Object quality = attrs.get("_quality_score");
        return quality instanceof Number ? ((Number) quality).doubleValue() : 0.8;
    }

    private double calculateUptimeFactor(Map<String, Object> attrs) {
        return 0.95; // Placeholder
    }

    private double calculateProductionRateFactor(Map<String, Object> attrs) {
        return 0.85; // Placeholder
    }

    private double calculateWaterCutFactor(Map<String, Object> attrs) {
        return 0.80; // Placeholder
    }

    private double calculateMotorTemperatureFactor(Map<String, Object> attrs) {
        Object temp = attrs.get("motor_temperature");
        if (temp instanceof Number) {
            double tempF = ((Number) temp).doubleValue();
            // Ideal: < 250°F, Critical: > 300°F
            if (tempF < 250) return 1.0;
            if (tempF < 275) return 0.8;
            if (tempF < 300) return 0.5;
            return 0.2;
        }
        return 0.8;
    }

    private double calculateMotorLoadFactor(Map<String, Object> attrs) {
        Object load = attrs.get("motor_load");
        if (load instanceof Number) {
            double loadPct = ((Number) load).doubleValue();
            // Ideal: 60-80%, Problems: < 40% or > 90%
            if (loadPct >= 60 && loadPct <= 80) return 1.0;
            if (loadPct >= 50 && loadPct <= 85) return 0.8;
            if (loadPct >= 40 && loadPct <= 90) return 0.6;
            return 0.3;
        }
        return 0.8;
    }

    private double calculateVibrationFactor(Map<String, Object> attrs) {
        return 0.9; // Placeholder
    }

    private double calculateAmperageFactor(Map<String, Object> attrs) {
        return 0.85; // Placeholder
    }

    private double calculateTorqueFactor(Map<String, Object> attrs) {
        return 0.85; // Placeholder
    }

    private double calculateRpmStabilityFactor(Map<String, Object> attrs) {
        return 0.9; // Placeholder
    }

    private double calculateGasEfficiencyFactor(Map<String, Object> attrs) {
        return 0.85; // Placeholder
    }

    private double calculateValvePerformanceFactor(Map<String, Object> attrs) {
        return 0.9; // Placeholder
    }

    private double calculatePumpFillageFactor(Map<String, Object> attrs) {
        return 0.85; // Placeholder
    }

    private double calculateRodLoadFactor(Map<String, Object> attrs) {
        return 0.9; // Placeholder
    }
}
