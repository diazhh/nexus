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
package org.thingsboard.nexus.po.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.PcpOptimizationDto;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.server.common.data.asset.Asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PCP (Progressive Cavity Pump) Speed Optimizer.
 *
 * Calculates optimal PCP RPM based on:
 * - Current operating conditions (torque, load, production)
 * - Pump efficiency curves
 * - Rod and stator wear considerations
 * - Fluid viscosity effects
 * - Power consumption optimization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoPcpSpeedOptimizer {

    private final PoAssetService assetService;
    private final PoAttributeService attributeService;
    private final PoModuleConfiguration config;

    /**
     * Optimizes PCP speed for a well.
     *
     * @param tenantId Tenant ID
     * @param wellAssetId Well asset ID
     * @param pcpAssetId PCP system asset ID (optional, will find if not provided)
     * @return Optimization result with recommended RPM
     */
    public PcpOptimizationDto optimizeSpeed(UUID tenantId, UUID wellAssetId, UUID pcpAssetId) {
        log.info("Optimizing PCP speed for well: {}", wellAssetId);

        // Get well info
        Optional<Asset> wellOpt = assetService.getAssetById(tenantId, wellAssetId);
        if (wellOpt.isEmpty()) {
            log.warn("Well not found: {}", wellAssetId);
            return null;
        }

        String wellName = wellOpt.get().getName();

        // Get current operating conditions from attributes/telemetry
        Map<String, Object> wellAttrs = attributeService.getAttributesAsMap(wellAssetId);
        Map<String, Object> pcpAttrs = pcpAssetId != null ?
                attributeService.getAttributesAsMap(pcpAssetId) : wellAttrs;

        // Extract current values
        BigDecimal currentRpm = getDecimalValue(pcpAttrs, "rpm", BigDecimal.valueOf(200));
        BigDecimal currentTorque = getDecimalValue(pcpAttrs, "torque", BigDecimal.valueOf(60));
        BigDecimal currentDriveLoad = getDecimalValue(pcpAttrs, "drive_load", BigDecimal.valueOf(65));
        BigDecimal currentRodLoad = getDecimalValue(pcpAttrs, "rod_load", BigDecimal.valueOf(8000));
        BigDecimal currentPip = getDecimalValue(pcpAttrs, "pip", BigDecimal.valueOf(150));
        BigDecimal currentProduction = getDecimalValue(wellAttrs, "current_production_bpd", BigDecimal.valueOf(300));
        BigDecimal currentPower = getDecimalValue(pcpAttrs, "power_kw", BigDecimal.valueOf(50));
        BigDecimal currentViscosity = getDecimalValue(wellAttrs, "fluid_viscosity", BigDecimal.valueOf(100));
        BigDecimal currentEfficiency = getDecimalValue(pcpAttrs, "pump_efficiency", BigDecimal.valueOf(75));

        // Get constraints from config
        PoModuleConfiguration.PcpOptimizerConfig pcpConfig = config.getPcp();
        BigDecimal minRpm = BigDecimal.valueOf(pcpConfig.getMinRpm());
        BigDecimal maxRpm = BigDecimal.valueOf(pcpConfig.getMaxRpm());
        BigDecimal targetTorque = BigDecimal.valueOf(pcpConfig.getTargetTorque());
        BigDecimal maxTorque = BigDecimal.valueOf(pcpConfig.getMaxTorque());
        BigDecimal maxRodLoad = BigDecimal.valueOf(pcpConfig.getMaxRodLoad());

        // Calculate optimal RPM
        OptimizationResult result = calculateOptimalRpm(
                currentRpm, currentTorque, currentDriveLoad, currentRodLoad,
                currentPip, currentProduction, currentPower, currentViscosity, currentEfficiency,
                minRpm, maxRpm, targetTorque, maxTorque, maxRodLoad
        );

        // Build DTO
        PcpOptimizationDto dto = PcpOptimizationDto.builder()
                .wellAssetId(wellAssetId)
                .pcpAssetId(pcpAssetId)
                .wellName(wellName)
                // Current conditions
                .currentRpm(currentRpm)
                .currentTorque(currentTorque)
                .currentDriveLoad(currentDriveLoad)
                .currentRodLoad(currentRodLoad)
                .currentPip(currentPip)
                .currentProductionBpd(currentProduction)
                .currentPowerKw(currentPower)
                .currentViscosity(currentViscosity)
                .currentPumpEfficiency(currentEfficiency)
                // Recommended values
                .recommendedRpm(result.optimalRpm)
                .rpmChange(result.optimalRpm.subtract(currentRpm))
                .expectedTorque(result.expectedTorque)
                .expectedDriveLoad(result.expectedDriveLoad)
                .expectedProductionBpd(result.expectedProduction)
                .expectedProductionIncrease(result.expectedProduction.subtract(currentProduction))
                .expectedProductionIncreasePercent(calculatePercentChange(currentProduction, result.expectedProduction))
                .expectedPowerKw(result.expectedPower)
                .expectedPumpEfficiency(result.expectedEfficiency)
                .expectedEfficiencyImprovement(result.expectedEfficiency.subtract(currentEfficiency))
                // Constraints
                .minRpm(minRpm)
                .maxRpm(maxRpm)
                .maxTorque(maxTorque)
                .maxRodLoad(maxRodLoad)
                .minPip(BigDecimal.valueOf(50))
                // Analysis
                .oepRpm(result.oepRpm)
                .distanceFromOep(result.distanceFromOep)
                .rodWearFactor(result.rodWearFactor)
                .statorWearFactor(result.statorWearFactor)
                .limitingConstraint(result.limitingConstraint)
                .confidence(result.confidence)
                .isSignificant(result.isSignificant)
                .notSignificantReason(result.notSignificantReason)
                .build();

        log.info("PCP optimization complete for {}: current={}RPM, recommended={}RPM, change={}RPM",
                wellName, currentRpm, result.optimalRpm,
                result.optimalRpm.subtract(currentRpm));

        return dto;
    }

    /**
     * Creates a recommendation from optimization result.
     */
    public RecommendationDto createRecommendation(UUID tenantId, PcpOptimizationDto optimization) {
        if (!optimization.getIsSignificant()) {
            return null;
        }

        return RecommendationDto.builder()
                .tenantId(tenantId)
                .assetId(optimization.getWellAssetId())
                .assetType(PoAssetService.ASSET_TYPE_WELL)
                .assetName(optimization.getWellName())
                .type(OptimizationType.PCP_SPEED)
                .priority(calculatePriority(optimization))
                .title(String.format("Adjust PCP speed from %.0f to %.0f RPM",
                        optimization.getCurrentRpm(), optimization.getRecommendedRpm()))
                .description(buildDescription(optimization))
                .currentValue(optimization.getCurrentRpm())
                .recommendedValue(optimization.getRecommendedRpm())
                .unit("RPM")
                .expectedProductionIncrease(optimization.getExpectedProductionIncrease())
                .expectedProductionIncreasePercent(optimization.getExpectedProductionIncreasePercent())
                .expectedEfficiencyImprovement(optimization.getExpectedEfficiencyImprovement())
                .confidence(optimization.getConfidence())
                .build();
    }

    // Private optimization methods

    private OptimizationResult calculateOptimalRpm(
            BigDecimal currentRpm, BigDecimal currentTorque, BigDecimal currentDriveLoad,
            BigDecimal currentRodLoad, BigDecimal currentPip, BigDecimal currentProd,
            BigDecimal currentPower, BigDecimal viscosity, BigDecimal currentEfficiency,
            BigDecimal minRpm, BigDecimal maxRpm, BigDecimal targetTorque,
            BigDecimal maxTorque, BigDecimal maxRodLoad) {

        OptimizationResult result = new OptimizationResult();

        // Calculate optimal efficiency point (OEP) - depends on viscosity
        // Higher viscosity = lower optimal RPM
        BigDecimal viscosityFactor = BigDecimal.ONE.subtract(
                viscosity.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP).min(BigDecimal.valueOf(0.3))
        );
        result.oepRpm = BigDecimal.valueOf(250).multiply(viscosityFactor).setScale(0, RoundingMode.HALF_UP);

        BigDecimal optimalRpm = currentRpm;
        String limitingConstraint = null;

        // Optimization logic:
        // 1. If torque is low and rod load is within limits, increase RPM
        // 2. If torque or rod load is high, decrease RPM
        // 3. Try to get closer to OEP while respecting constraints

        if (currentTorque.compareTo(targetTorque) < 0 &&
            currentRodLoad.compareTo(maxRodLoad.multiply(BigDecimal.valueOf(0.85))) < 0) {
            // Room to increase
            BigDecimal torqueMargin = targetTorque.subtract(currentTorque);
            BigDecimal rpmIncrease = torqueMargin.multiply(BigDecimal.valueOf(1.5)); // 1.5 RPM per % torque
            optimalRpm = currentRpm.add(rpmIncrease).min(maxRpm);

            // Also consider moving toward OEP
            if (optimalRpm.compareTo(result.oepRpm) < 0 && currentRpm.compareTo(result.oepRpm) < 0) {
                // Move toward OEP
                optimalRpm = optimalRpm.min(result.oepRpm);
            }

            if (optimalRpm.compareTo(maxRpm) >= 0) {
                limitingConstraint = "MAX_RPM";
            }
        } else if (currentTorque.compareTo(maxTorque.multiply(BigDecimal.valueOf(0.9))) > 0) {
            // Torque too high - reduce RPM
            BigDecimal rpmDecrease = BigDecimal.valueOf(20);
            optimalRpm = currentRpm.subtract(rpmDecrease).max(minRpm);
            limitingConstraint = "TORQUE";
        } else if (currentRodLoad.compareTo(maxRodLoad.multiply(BigDecimal.valueOf(0.9))) > 0) {
            // Rod load too high - reduce RPM
            BigDecimal rpmDecrease = BigDecimal.valueOf(15);
            optimalRpm = currentRpm.subtract(rpmDecrease).max(minRpm);
            limitingConstraint = "ROD_LOAD";
        }

        if (optimalRpm.compareTo(minRpm) <= 0) {
            limitingConstraint = "MIN_RPM";
        }

        // Estimate expected values at optimal RPM
        BigDecimal rpmRatio = optimalRpm.divide(currentRpm, 4, RoundingMode.HALF_UP);

        // Production scales roughly linearly with RPM for PCPs
        result.expectedProduction = currentProd.multiply(rpmRatio).setScale(2, RoundingMode.HALF_UP);

        // Torque scales with RPM (higher RPM = higher torque due to fluid friction)
        result.expectedTorque = currentTorque.multiply(rpmRatio.pow(1)).setScale(2, RoundingMode.HALF_UP);

        // Drive load follows torque
        result.expectedDriveLoad = currentDriveLoad.multiply(rpmRatio).setScale(2, RoundingMode.HALF_UP);

        // Power scales with RPM * torque (roughly RPM^2)
        result.expectedPower = currentPower.multiply(rpmRatio.pow(2)).setScale(2, RoundingMode.HALF_UP);

        // Calculate efficiency at new operating point
        result.expectedEfficiency = calculateEfficiency(optimalRpm, result.oepRpm, viscosity, result.expectedTorque);

        // Calculate distance from OEP
        result.distanceFromOep = optimalRpm.subtract(result.oepRpm).abs()
                .divide(result.oepRpm.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Calculate wear factors
        result.rodWearFactor = calculateRodWearFactor(optimalRpm, maxRpm);
        result.statorWearFactor = calculateStatorWearFactor(optimalRpm, maxRpm, viscosity);

        result.optimalRpm = optimalRpm.setScale(0, RoundingMode.HALF_UP);
        result.limitingConstraint = limitingConstraint;

        // Determine if change is significant
        BigDecimal absChange = optimalRpm.subtract(currentRpm).abs();
        double minChange = config.getPcp().getMinRpmChange();
        result.isSignificant = absChange.compareTo(BigDecimal.valueOf(minChange)) >= 0;

        if (!result.isSignificant) {
            result.notSignificantReason = String.format("Change (%.0f RPM) is below minimum threshold (%.0f RPM)",
                    absChange.doubleValue(), minChange);
        }

        // Confidence based on data quality and constraint headroom
        result.confidence = calculateConfidence(currentTorque, maxTorque, currentRodLoad, maxRodLoad, limitingConstraint);

        return result;
    }

    private BigDecimal calculateEfficiency(BigDecimal rpm, BigDecimal oepRpm, BigDecimal viscosity, BigDecimal torque) {
        // Efficiency is highest at OEP and decreases as we move away
        BigDecimal distanceFromOep = rpm.subtract(oepRpm).abs()
                .divide(oepRpm.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP);

        // Base efficiency around 80% at OEP
        BigDecimal baseEfficiency = BigDecimal.valueOf(80);

        // Reduce efficiency based on distance from OEP (up to 15% reduction)
        BigDecimal distancePenalty = distanceFromOep.multiply(BigDecimal.valueOf(15)).min(BigDecimal.valueOf(15));

        // Higher viscosity reduces efficiency
        BigDecimal viscosityPenalty = viscosity.divide(BigDecimal.valueOf(500), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(5));

        return baseEfficiency.subtract(distancePenalty).subtract(viscosityPenalty).max(BigDecimal.valueOf(50));
    }

    private BigDecimal calculateRodWearFactor(BigDecimal rpm, BigDecimal maxRpm) {
        // Rod wear increases exponentially with RPM
        BigDecimal rpmRatio = rpm.divide(maxRpm, 4, RoundingMode.HALF_UP);
        return rpmRatio.pow(2).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStatorWearFactor(BigDecimal rpm, BigDecimal maxRpm, BigDecimal viscosity) {
        // Stator wear depends on RPM and viscosity (lower viscosity = more wear)
        BigDecimal rpmRatio = rpm.divide(maxRpm, 4, RoundingMode.HALF_UP);
        BigDecimal viscosityFactor = BigDecimal.valueOf(200).divide(viscosity.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
        return rpmRatio.multiply(viscosityFactor).setScale(3, RoundingMode.HALF_UP);
    }

    private double calculateConfidence(BigDecimal torque, BigDecimal maxTorque,
                                       BigDecimal rodLoad, BigDecimal maxRodLoad, String constraint) {
        double confidence = 0.85; // Base confidence

        // Reduce confidence if near limits
        if (torque.compareTo(maxTorque.multiply(BigDecimal.valueOf(0.85))) > 0) {
            confidence -= 0.1;
        }
        if (rodLoad.compareTo(maxRodLoad.multiply(BigDecimal.valueOf(0.85))) > 0) {
            confidence -= 0.1;
        }
        if (constraint != null && (constraint.contains("TORQUE") || constraint.contains("ROD_LOAD"))) {
            confidence -= 0.05;
        }

        return Math.max(confidence, 0.5);
    }

    private Integer calculatePriority(PcpOptimizationDto opt) {
        // Priority 1-5, lower is higher priority
        BigDecimal increase = opt.getExpectedProductionIncreasePercent().abs();
        if (increase.compareTo(BigDecimal.valueOf(10)) >= 0) return 1;
        if (increase.compareTo(BigDecimal.valueOf(5)) >= 0) return 2;
        if (increase.compareTo(BigDecimal.valueOf(2)) >= 0) return 3;
        return 4;
    }

    private String buildDescription(PcpOptimizationDto opt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Optimize PCP speed from %.0f RPM to %.0f RPM.\n\n",
                opt.getCurrentRpm(), opt.getRecommendedRpm()));

        sb.append("Expected Benefits:\n");
        sb.append(String.format("- Production change: %+.0f BPD (%+.1f%%)\n",
                opt.getExpectedProductionIncrease(), opt.getExpectedProductionIncreasePercent()));
        sb.append(String.format("- Efficiency: %.1f%% → %.1f%% (%+.1f%%)\n",
                opt.getCurrentPumpEfficiency(), opt.getExpectedPumpEfficiency(), opt.getExpectedEfficiencyImprovement()));

        sb.append("\nCurrent Conditions:\n");
        sb.append(String.format("- Torque: %.1f%%\n", opt.getCurrentTorque()));
        sb.append(String.format("- Drive load: %.1f%%\n", opt.getCurrentDriveLoad()));
        sb.append(String.format("- Rod load: %.0f lbs\n", opt.getCurrentRodLoad()));
        sb.append(String.format("- Production: %.0f BPD\n", opt.getCurrentProductionBpd()));

        sb.append("\nWear Analysis:\n");
        sb.append(String.format("- Rod wear factor: %.2f\n", opt.getRodWearFactor()));
        sb.append(String.format("- Stator wear factor: %.2f\n", opt.getStatorWearFactor()));

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
        BigDecimal optimalRpm;
        BigDecimal expectedTorque;
        BigDecimal expectedDriveLoad;
        BigDecimal expectedProduction;
        BigDecimal expectedPower;
        BigDecimal expectedEfficiency;
        BigDecimal oepRpm;
        BigDecimal distanceFromOep;
        BigDecimal rodWearFactor;
        BigDecimal statorWearFactor;
        String limitingConstraint;
        double confidence;
        boolean isSignificant;
        String notSignificantReason;
    }
}
