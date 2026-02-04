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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.EspOptimizationDto;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.server.common.data.asset.Asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ESP Frequency Optimizer.
 *
 * Calculates optimal ESP frequency based on:
 * - Current operating conditions
 * - Motor constraints (temperature, load)
 * - Pump performance curves
 * - Production targets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoEspFrequencyOptimizer {

    private final PoAssetService assetService;
    private final PoAttributeService attributeService;
    private final PoModuleConfiguration config;

    /**
     * Optimizes ESP frequency for a well.
     *
     * @param tenantId Tenant ID
     * @param wellAssetId Well asset ID
     * @param espAssetId ESP system asset ID (optional, will find if not provided)
     * @return Optimization result with recommended frequency
     */
    public EspOptimizationDto optimizeFrequency(UUID tenantId, UUID wellAssetId, UUID espAssetId) {
        log.info("Optimizing ESP frequency for well: {}", wellAssetId);

        // Get well info
        Optional<Asset> wellOpt = assetService.getAssetById(tenantId, wellAssetId);
        if (wellOpt.isEmpty()) {
            log.warn("Well not found: {}", wellAssetId);
            return null;
        }

        String wellName = wellOpt.get().getName();

        // Get current operating conditions from attributes/telemetry
        Map<String, Object> wellAttrs = attributeService.getAttributesAsMap(wellAssetId);
        Map<String, Object> espAttrs = espAssetId != null ?
                attributeService.getAttributesAsMap(espAssetId) : wellAttrs;

        // Extract current values
        BigDecimal currentFrequency = getDecimalValue(espAttrs, "frequency", BigDecimal.valueOf(50));
        BigDecimal currentMotorLoad = getDecimalValue(espAttrs, "motor_load", BigDecimal.valueOf(70));
        BigDecimal currentMotorTemp = getDecimalValue(espAttrs, "motor_temperature", BigDecimal.valueOf(250));
        BigDecimal currentPip = getDecimalValue(espAttrs, "pip", BigDecimal.valueOf(200));
        BigDecimal currentDischargePressure = getDecimalValue(espAttrs, "discharge_pressure", BigDecimal.valueOf(1500));
        BigDecimal currentProduction = getDecimalValue(wellAttrs, "current_production_bpd", BigDecimal.valueOf(500));
        BigDecimal currentPower = getDecimalValue(espAttrs, "power_kw", BigDecimal.valueOf(100));

        // Get constraints from config
        PoModuleConfiguration.EspOptimizerConfig espConfig = config.getEsp();
        BigDecimal minFreq = BigDecimal.valueOf(espConfig.getMinFrequency());
        BigDecimal maxFreq = BigDecimal.valueOf(espConfig.getMaxFrequency());
        BigDecimal targetMotorLoad = BigDecimal.valueOf(espConfig.getTargetMotorLoad());
        BigDecimal maxMotorTemp = BigDecimal.valueOf(espConfig.getMaxMotorTemperature());

        // Calculate optimal frequency
        OptimizationResult result = calculateOptimalFrequency(
                currentFrequency, currentMotorLoad, currentMotorTemp, currentPip,
                currentProduction, currentPower,
                minFreq, maxFreq, targetMotorLoad, maxMotorTemp
        );

        // Build DTO
        EspOptimizationDto dto = EspOptimizationDto.builder()
                .wellAssetId(wellAssetId)
                .espAssetId(espAssetId)
                .wellName(wellName)
                // Current conditions
                .currentFrequency(currentFrequency)
                .currentMotorLoad(currentMotorLoad)
                .currentMotorTemperature(currentMotorTemp)
                .currentPip(currentPip)
                .currentDischargePressure(currentDischargePressure)
                .currentProductionBpd(currentProduction)
                .currentPowerKw(currentPower)
                // Recommended values
                .recommendedFrequency(result.optimalFrequency)
                .frequencyChange(result.optimalFrequency.subtract(currentFrequency))
                .expectedMotorLoad(result.expectedMotorLoad)
                .expectedMotorTemperature(result.expectedMotorTemp)
                .expectedProductionBpd(result.expectedProduction)
                .expectedProductionIncrease(result.expectedProduction.subtract(currentProduction))
                .expectedProductionIncreasePercent(calculatePercentChange(currentProduction, result.expectedProduction))
                .expectedPowerKw(result.expectedPower)
                .expectedEfficiencyImprovement(result.efficiencyImprovement)
                // Constraints
                .minFrequency(minFreq)
                .maxFrequency(maxFreq)
                .maxMotorTemperature(maxMotorTemp)
                .maxMotorLoad(BigDecimal.valueOf(90))
                .minPip(BigDecimal.valueOf(100))
                // Analysis
                .operatingEfficiency(result.currentEfficiency)
                .bepFrequency(result.bepFrequency)
                .distanceFromBep(result.distanceFromBep)
                .limitingConstraint(result.limitingConstraint)
                .confidence(result.confidence)
                .isSignificant(result.isSignificant)
                .notSignificantReason(result.notSignificantReason)
                .build();

        log.info("ESP optimization complete for {}: current={}Hz, recommended={}Hz, change={}Hz",
                wellName, currentFrequency, result.optimalFrequency,
                result.optimalFrequency.subtract(currentFrequency));

        return dto;
    }

    /**
     * Creates a recommendation from optimization result.
     */
    public RecommendationDto createRecommendation(UUID tenantId, EspOptimizationDto optimization) {
        if (!optimization.getIsSignificant()) {
            return null;
        }

        return RecommendationDto.builder()
                .tenantId(tenantId)
                .assetId(optimization.getWellAssetId())
                .assetType(PoAssetService.ASSET_TYPE_WELL)
                .assetName(optimization.getWellName())
                .type(OptimizationType.ESP_FREQUENCY)
                .priority(calculatePriority(optimization))
                .title(String.format("Adjust ESP frequency from %.1f to %.1f Hz",
                        optimization.getCurrentFrequency(), optimization.getRecommendedFrequency()))
                .description(buildDescription(optimization))
                .currentValue(optimization.getCurrentFrequency())
                .recommendedValue(optimization.getRecommendedFrequency())
                .unit("Hz")
                .expectedProductionIncrease(optimization.getExpectedProductionIncrease())
                .expectedProductionIncreasePercent(optimization.getExpectedProductionIncreasePercent())
                .expectedEfficiencyImprovement(optimization.getExpectedEfficiencyImprovement())
                .confidence(optimization.getConfidence())
                .build();
    }

    // Private optimization methods

    private OptimizationResult calculateOptimalFrequency(
            BigDecimal currentFreq, BigDecimal currentLoad, BigDecimal currentTemp, BigDecimal currentPip,
            BigDecimal currentProd, BigDecimal currentPower,
            BigDecimal minFreq, BigDecimal maxFreq, BigDecimal targetLoad, BigDecimal maxTemp) {

        OptimizationResult result = new OptimizationResult();

        // Calculate best efficiency point (BEP) - simplified model
        result.bepFrequency = BigDecimal.valueOf(55); // Typical BEP for ESP

        // Calculate current efficiency
        result.currentEfficiency = calculateEfficiency(currentLoad, currentTemp, currentProd, currentPower);

        // Optimization logic:
        // 1. If motor load is low, increase frequency to increase production
        // 2. If motor load is high, decrease frequency to prevent damage
        // 3. If temperature is high, decrease frequency
        // 4. Try to get closer to BEP

        BigDecimal optimalFreq = currentFreq;
        String limitingConstraint = null;

        // Check if we can increase frequency
        if (currentLoad.compareTo(targetLoad) < 0 && currentTemp.compareTo(maxTemp.multiply(BigDecimal.valueOf(0.9))) < 0) {
            // Room to increase
            BigDecimal loadMargin = targetLoad.subtract(currentLoad);
            BigDecimal freqIncrease = loadMargin.multiply(BigDecimal.valueOf(0.1)); // 0.1 Hz per % load
            optimalFreq = currentFreq.add(freqIncrease).min(maxFreq);

            if (optimalFreq.compareTo(maxFreq) >= 0) {
                limitingConstraint = "MAX_FREQUENCY";
            }
        } else if (currentLoad.compareTo(BigDecimal.valueOf(85)) > 0 || currentTemp.compareTo(maxTemp.multiply(BigDecimal.valueOf(0.95))) > 0) {
            // Need to reduce
            BigDecimal freqDecrease;
            if (currentTemp.compareTo(maxTemp.multiply(BigDecimal.valueOf(0.95))) > 0) {
                freqDecrease = BigDecimal.valueOf(2); // Reduce by 2 Hz for temp
                limitingConstraint = "MOTOR_TEMPERATURE";
            } else {
                freqDecrease = BigDecimal.valueOf(1); // Reduce by 1 Hz for load
                limitingConstraint = "MOTOR_LOAD";
            }
            optimalFreq = currentFreq.subtract(freqDecrease).max(minFreq);

            if (optimalFreq.compareTo(minFreq) <= 0) {
                limitingConstraint = "MIN_FREQUENCY";
            }
        }

        // Estimate expected values at optimal frequency
        BigDecimal freqRatio = optimalFreq.divide(currentFreq, 4, RoundingMode.HALF_UP);

        // Production scales roughly with frequency (simplified affinity law)
        result.expectedProduction = currentProd.multiply(freqRatio).setScale(2, RoundingMode.HALF_UP);

        // Motor load scales with frequency squared (for centrifugal pumps)
        result.expectedMotorLoad = currentLoad.multiply(freqRatio.pow(2)).setScale(2, RoundingMode.HALF_UP);

        // Temperature increase with load
        BigDecimal tempIncrease = result.expectedMotorLoad.subtract(currentLoad).multiply(BigDecimal.valueOf(0.5));
        result.expectedMotorTemp = currentTemp.add(tempIncrease).setScale(1, RoundingMode.HALF_UP);

        // Power scales with frequency cubed (affinity law)
        result.expectedPower = currentPower.multiply(freqRatio.pow(3)).setScale(2, RoundingMode.HALF_UP);

        // Calculate new efficiency
        BigDecimal newEfficiency = calculateEfficiency(result.expectedMotorLoad, result.expectedMotorTemp,
                result.expectedProduction, result.expectedPower);
        result.efficiencyImprovement = newEfficiency.subtract(result.currentEfficiency);

        // Calculate distance from BEP
        result.distanceFromBep = optimalFreq.subtract(result.bepFrequency).abs()
                .divide(result.bepFrequency, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        result.optimalFrequency = optimalFreq.setScale(1, RoundingMode.HALF_UP);
        result.limitingConstraint = limitingConstraint;

        // Determine if change is significant
        BigDecimal absChange = optimalFreq.subtract(currentFreq).abs();
        double minChange = config.getEsp().getMinFrequencyChange();
        result.isSignificant = absChange.compareTo(BigDecimal.valueOf(minChange)) >= 0;

        if (!result.isSignificant) {
            result.notSignificantReason = String.format("Change (%.1f Hz) is below minimum threshold (%.1f Hz)",
                    absChange.doubleValue(), minChange);
        }

        // Confidence based on data quality and constraint headroom
        result.confidence = calculateConfidence(currentLoad, currentTemp, maxTemp, limitingConstraint);

        return result;
    }

    private BigDecimal calculateEfficiency(BigDecimal load, BigDecimal temp, BigDecimal prod, BigDecimal power) {
        // Simplified efficiency = production per unit power, adjusted for conditions
        if (power.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal baseEfficiency = prod.divide(power, 4, RoundingMode.HALF_UP);

        // Penalize for high temperature
        BigDecimal tempPenalty = BigDecimal.ONE;
        if (temp.compareTo(BigDecimal.valueOf(270)) > 0) {
            tempPenalty = BigDecimal.valueOf(0.9);
        }

        // Penalize for very low or very high load
        BigDecimal loadPenalty = BigDecimal.ONE;
        if (load.compareTo(BigDecimal.valueOf(50)) < 0 || load.compareTo(BigDecimal.valueOf(85)) > 0) {
            loadPenalty = BigDecimal.valueOf(0.95);
        }

        return baseEfficiency.multiply(tempPenalty).multiply(loadPenalty);
    }

    private double calculateConfidence(BigDecimal load, BigDecimal temp, BigDecimal maxTemp, String constraint) {
        double confidence = 0.85; // Base confidence

        // Reduce confidence if near limits
        if (temp.compareTo(maxTemp.multiply(BigDecimal.valueOf(0.9))) > 0) {
            confidence -= 0.1;
        }
        if (load.compareTo(BigDecimal.valueOf(85)) > 0) {
            confidence -= 0.05;
        }
        if (constraint != null && constraint.contains("TEMPERATURE")) {
            confidence -= 0.1;
        }

        return Math.max(confidence, 0.5);
    }

    private Integer calculatePriority(EspOptimizationDto opt) {
        // Priority 1-5, lower is higher priority
        BigDecimal increase = opt.getExpectedProductionIncreasePercent();
        if (increase.compareTo(BigDecimal.valueOf(10)) >= 0) return 1;
        if (increase.compareTo(BigDecimal.valueOf(5)) >= 0) return 2;
        if (increase.compareTo(BigDecimal.valueOf(2)) >= 0) return 3;
        return 4;
    }

    private String buildDescription(EspOptimizationDto opt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Optimize ESP frequency from %.1f Hz to %.1f Hz.\n\n",
                opt.getCurrentFrequency(), opt.getRecommendedFrequency()));

        sb.append("Expected Benefits:\n");
        sb.append(String.format("- Production increase: +%.0f BPD (+%.1f%%)\n",
                opt.getExpectedProductionIncrease(), opt.getExpectedProductionIncreasePercent()));
        sb.append(String.format("- Efficiency improvement: +%.1f%%\n", opt.getExpectedEfficiencyImprovement()));

        sb.append("\nCurrent Conditions:\n");
        sb.append(String.format("- Motor load: %.1f%%\n", opt.getCurrentMotorLoad()));
        sb.append(String.format("- Motor temperature: %.0f°F\n", opt.getCurrentMotorTemperature()));
        sb.append(String.format("- Production: %.0f BPD\n", opt.getCurrentProductionBpd()));

        if (opt.getLimitingConstraint() != null) {
            sb.append(String.format("\nLimiting factor: %s\n", opt.getLimitingConstraint()));
        }

        sb.append(String.format("\nConfidence: %.0f%%", opt.getConfidence() * 100));

        return sb.toString();
    }

    private BigDecimal getDecimalValue(Map<String, Object> attrs, String key, BigDecimal defaultValue) {
        Object value = attrs.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return defaultValue;
    }

    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal expected) {
        if (current.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return expected.subtract(current)
                .divide(current, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Inner class for optimization result
    private static class OptimizationResult {
        BigDecimal optimalFrequency;
        BigDecimal expectedMotorLoad;
        BigDecimal expectedMotorTemp;
        BigDecimal expectedProduction;
        BigDecimal expectedPower;
        BigDecimal currentEfficiency;
        BigDecimal efficiencyImprovement;
        BigDecimal bepFrequency;
        BigDecimal distanceFromBep;
        String limitingConstraint;
        double confidence;
        boolean isSignificant;
        String notSignificantReason;
    }
}
