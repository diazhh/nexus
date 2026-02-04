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
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.nexus.po.dto.RodPumpOptimizationDto;
import org.thingsboard.server.common.data.asset.Asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Rod Pump (Sucker Rod Pump / Beam Pump) Optimizer.
 *
 * Calculates optimal operating parameters based on:
 * - Pump fillage analysis
 * - Rod stress constraints
 * - Peak and minimum load analysis
 * - Counterbalance optimization
 * - Production targets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoRodPumpOptimizer {

    private final PoAssetService assetService;
    private final PoAttributeService attributeService;
    private final PoModuleConfiguration config;

    /**
     * Optimizes rod pump parameters for a well.
     *
     * @param tenantId Tenant ID
     * @param wellAssetId Well asset ID
     * @param rodPumpAssetId Rod pump system asset ID (optional)
     * @return Optimization result with recommended parameters
     */
    public RodPumpOptimizationDto optimize(UUID tenantId, UUID wellAssetId, UUID rodPumpAssetId) {
        log.info("Optimizing rod pump for well: {}", wellAssetId);

        // Get well info
        Optional<Asset> wellOpt = assetService.getAssetById(tenantId, wellAssetId);
        if (wellOpt.isEmpty()) {
            log.warn("Well not found: {}", wellAssetId);
            return null;
        }

        String wellName = wellOpt.get().getName();

        // Get current operating conditions
        Map<String, Object> wellAttrs = attributeService.getAttributesAsMap(wellAssetId);
        Map<String, Object> pumpAttrs = rodPumpAssetId != null ?
                attributeService.getAttributesAsMap(rodPumpAssetId) : wellAttrs;

        // Extract current values
        BigDecimal currentSpm = getDecimalValue(pumpAttrs, "spm", BigDecimal.valueOf(8));
        BigDecimal currentStrokeLength = getDecimalValue(pumpAttrs, "stroke_length", BigDecimal.valueOf(86));
        BigDecimal currentFillage = getDecimalValue(pumpAttrs, "fillage", BigDecimal.valueOf(75));
        BigDecimal currentPeakLoad = getDecimalValue(pumpAttrs, "peak_load", BigDecimal.valueOf(15000));
        BigDecimal currentMinLoad = getDecimalValue(pumpAttrs, "min_load", BigDecimal.valueOf(3000));
        BigDecimal currentCounterbalance = getDecimalValue(pumpAttrs, "counterbalance", BigDecimal.valueOf(50));
        BigDecimal currentProduction = getDecimalValue(wellAttrs, "current_production_bpd", BigDecimal.valueOf(100));
        BigDecimal currentPower = getDecimalValue(pumpAttrs, "power_kw", BigDecimal.valueOf(20));
        BigDecimal currentEfficiency = getDecimalValue(pumpAttrs, "pump_efficiency", BigDecimal.valueOf(70));
        BigDecimal currentRodStress = getDecimalValue(pumpAttrs, "rod_stress", BigDecimal.valueOf(20000));

        // Pump geometry
        BigDecimal pumpDiameter = getDecimalValue(pumpAttrs, "pump_diameter", BigDecimal.valueOf(2.25));

        // Get constraints from config
        PoModuleConfiguration.RodPumpOptimizerConfig rpConfig = config.getRodPump();
        BigDecimal minSpm = BigDecimal.valueOf(rpConfig.getMinSpm());
        BigDecimal maxSpm = BigDecimal.valueOf(rpConfig.getMaxSpm());
        BigDecimal minStroke = BigDecimal.valueOf(rpConfig.getMinStrokeLength());
        BigDecimal maxStroke = BigDecimal.valueOf(rpConfig.getMaxStrokeLength());
        BigDecimal targetFillage = BigDecimal.valueOf(rpConfig.getTargetFillage());
        BigDecimal minFillage = BigDecimal.valueOf(rpConfig.getMinFillage());
        BigDecimal maxPeakLoad = BigDecimal.valueOf(rpConfig.getMaxPeakLoad());
        BigDecimal maxRodStress = BigDecimal.valueOf(rpConfig.getMaxRodStress());

        // Calculate pump displacement
        BigDecimal pumpDisplacement = calculatePumpDisplacement(pumpDiameter, currentStrokeLength);

        // Calculate optimal parameters
        OptimizationResult result = calculateOptimalParameters(
                currentSpm, currentStrokeLength, currentFillage, currentPeakLoad, currentMinLoad,
                currentCounterbalance, currentProduction, currentPower, currentEfficiency, currentRodStress,
                pumpDisplacement,
                minSpm, maxSpm, minStroke, maxStroke, targetFillage, minFillage, maxPeakLoad, maxRodStress,
                rpConfig
        );

        // Build DTO
        RodPumpOptimizationDto dto = RodPumpOptimizationDto.builder()
                .wellAssetId(wellAssetId)
                .rodPumpAssetId(rodPumpAssetId)
                .wellName(wellName)
                // Current conditions
                .currentSpm(currentSpm)
                .currentStrokeLength(currentStrokeLength)
                .currentFillage(currentFillage)
                .currentPeakLoad(currentPeakLoad)
                .currentMinLoad(currentMinLoad)
                .currentCounterbalance(currentCounterbalance)
                .currentProductionBpd(currentProduction)
                .currentPowerKw(currentPower)
                .currentPumpEfficiency(currentEfficiency)
                .currentRodStress(currentRodStress)
                // Recommended values
                .recommendedSpm(result.optimalSpm)
                .spmChange(result.optimalSpm.subtract(currentSpm))
                .recommendedStrokeLength(result.optimalStrokeLength)
                .strokeLengthChange(result.optimalStrokeLength.subtract(currentStrokeLength))
                .expectedFillage(result.expectedFillage)
                .expectedPeakLoad(result.expectedPeakLoad)
                .expectedProductionBpd(result.expectedProduction)
                .expectedProductionIncrease(result.expectedProduction.subtract(currentProduction))
                .expectedProductionIncreasePercent(calculatePercentChange(currentProduction, result.expectedProduction))
                .expectedPowerKw(result.expectedPower)
                .expectedPumpEfficiency(result.expectedEfficiency)
                .expectedEfficiencyImprovement(result.expectedEfficiency.subtract(currentEfficiency))
                .expectedRodStress(result.expectedRodStress)
                // Constraints
                .minSpm(minSpm)
                .maxSpm(maxSpm)
                .minStrokeLength(minStroke)
                .maxStrokeLength(maxStroke)
                .maxPeakLoad(maxPeakLoad)
                .maxRodStress(maxRodStress)
                .minFillage(minFillage)
                // Analysis
                .optimalFillageSpm(result.optimalFillageSpm)
                .pumpDisplacement(pumpDisplacement)
                .theoreticalCapacity(result.theoreticalCapacity)
                .volumetricEfficiency(result.volumetricEfficiency)
                .counterbalanceRecommendation(result.counterbalanceRecommendation)
                .optimizationType(result.optimizationType)
                .limitingConstraint(result.limitingConstraint)
                .confidence(result.confidence)
                .isSignificant(result.isSignificant)
                .notSignificantReason(result.notSignificantReason)
                .dynacardAnalysis(result.dynacardAnalysis)
                .build();

        log.info("Rod pump optimization complete for {}: SPM {}→{}, Stroke {}→{}",
                wellName, currentSpm, result.optimalSpm, currentStrokeLength, result.optimalStrokeLength);

        return dto;
    }

    /**
     * Creates a recommendation from optimization result.
     */
    public RecommendationDto createRecommendation(UUID tenantId, RodPumpOptimizationDto optimization) {
        if (!optimization.getIsSignificant()) {
            return null;
        }

        String title;
        BigDecimal currentValue;
        BigDecimal recommendedValue;
        String unit;

        // Determine primary recommendation based on optimization type
        if ("STROKE".equals(optimization.getOptimizationType())) {
            title = String.format("Adjust stroke length from %.0f to %.0f inches",
                    optimization.getCurrentStrokeLength(), optimization.getRecommendedStrokeLength());
            currentValue = optimization.getCurrentStrokeLength();
            recommendedValue = optimization.getRecommendedStrokeLength();
            unit = "inches";
        } else {
            title = String.format("Adjust SPM from %.1f to %.1f",
                    optimization.getCurrentSpm(), optimization.getRecommendedSpm());
            currentValue = optimization.getCurrentSpm();
            recommendedValue = optimization.getRecommendedSpm();
            unit = "SPM";
        }

        return RecommendationDto.builder()
                .tenantId(tenantId)
                .assetId(optimization.getWellAssetId())
                .assetType(PoAssetService.ASSET_TYPE_WELL)
                .assetName(optimization.getWellName())
                .type(OptimizationType.ROD_PUMP_SPEED)
                .priority(calculatePriority(optimization))
                .title(title)
                .description(buildDescription(optimization))
                .currentValue(currentValue)
                .recommendedValue(recommendedValue)
                .unit(unit)
                .expectedProductionIncrease(optimization.getExpectedProductionIncrease())
                .expectedProductionIncreasePercent(optimization.getExpectedProductionIncreasePercent())
                .expectedEfficiencyImprovement(optimization.getExpectedEfficiencyImprovement())
                .confidence(optimization.getConfidence())
                .build();
    }

    // Private methods

    private BigDecimal calculatePumpDisplacement(BigDecimal diameter, BigDecimal strokeLength) {
        // Displacement (bbl/stroke) = π * D² * S / 4 / 9702
        // Where D is diameter in inches, S is stroke in inches
        // 9702 is conversion factor to bbl
        BigDecimal pi = BigDecimal.valueOf(Math.PI);
        return pi.multiply(diameter.pow(2))
                .multiply(strokeLength)
                .divide(BigDecimal.valueOf(4 * 9702), 6, RoundingMode.HALF_UP);
    }

    private OptimizationResult calculateOptimalParameters(
            BigDecimal currentSpm, BigDecimal currentStroke, BigDecimal currentFillage,
            BigDecimal currentPeakLoad, BigDecimal currentMinLoad, BigDecimal currentCounterbalance,
            BigDecimal currentProd, BigDecimal currentPower, BigDecimal currentEfficiency,
            BigDecimal currentRodStress, BigDecimal pumpDisplacement,
            BigDecimal minSpm, BigDecimal maxSpm, BigDecimal minStroke, BigDecimal maxStroke,
            BigDecimal targetFillage, BigDecimal minFillage, BigDecimal maxPeakLoad, BigDecimal maxRodStress,
            PoModuleConfiguration.RodPumpOptimizerConfig rpConfig) {

        OptimizationResult result = new OptimizationResult();

        // Analyze current fillage to determine optimization strategy
        String optimizationType = "SPEED";
        String limitingConstraint = null;
        String dynacardAnalysis;

        BigDecimal optimalSpm = currentSpm;
        BigDecimal optimalStroke = currentStroke;

        // Calculate theoretical capacity at current settings
        result.theoreticalCapacity = pumpDisplacement.multiply(currentSpm)
                .multiply(BigDecimal.valueOf(1440)) // Minutes per day
                .setScale(2, RoundingMode.HALF_UP);

        result.volumetricEfficiency = currentProd.compareTo(BigDecimal.ZERO) > 0
                ? currentProd.divide(result.theoreticalCapacity.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Fillage-based optimization
        if (currentFillage.compareTo(targetFillage) < 0) {
            // Underfilled - pump is running too fast for inflow
            // Option 1: Reduce SPM
            BigDecimal spmReduction = currentSpm.multiply(
                    BigDecimal.ONE.subtract(currentFillage.divide(targetFillage, 4, RoundingMode.HALF_UP))
            ).multiply(BigDecimal.valueOf(0.5)); // Conservative reduction

            optimalSpm = currentSpm.subtract(spmReduction).max(minSpm);
            dynacardAnalysis = String.format("Pump underfilled (%.0f%%). Recommend reducing SPM to improve fillage.",
                    currentFillage);

            if (optimalSpm.compareTo(minSpm) <= 0) {
                limitingConstraint = "MIN_SPM";
            }
        } else if (currentFillage.compareTo(BigDecimal.valueOf(95)) > 0) {
            // Overfilled - pump may be able to handle more
            // Check if we have headroom in peak load and rod stress
            if (currentPeakLoad.compareTo(maxPeakLoad.multiply(BigDecimal.valueOf(0.85))) < 0 &&
                currentRodStress.compareTo(maxRodStress.multiply(BigDecimal.valueOf(0.85))) < 0) {
                // Room to increase SPM
                BigDecimal spmIncrease = BigDecimal.ONE;
                optimalSpm = currentSpm.add(spmIncrease).min(maxSpm);
                dynacardAnalysis = String.format("Pump fully filled (%.0f%%) with load headroom. Recommend increasing SPM.",
                        currentFillage);

                if (optimalSpm.compareTo(maxSpm) >= 0) {
                    limitingConstraint = "MAX_SPM";
                }
            } else {
                dynacardAnalysis = String.format("Pump fully filled (%.0f%%) but near load limits. Maintain current settings.",
                        currentFillage);
            }
        } else {
            // Fillage is good (75-95%)
            dynacardAnalysis = String.format("Pump fillage (%.0f%%) is within optimal range.", currentFillage);
        }

        // Check rod stress constraint
        if (currentRodStress.compareTo(maxRodStress.multiply(BigDecimal.valueOf(0.9))) > 0) {
            optimalSpm = optimalSpm.subtract(BigDecimal.ONE).max(minSpm);
            limitingConstraint = "ROD_STRESS";
            dynacardAnalysis += " High rod stress detected - reducing speed.";
        }

        // Check peak load constraint
        if (currentPeakLoad.compareTo(maxPeakLoad.multiply(BigDecimal.valueOf(0.9))) > 0) {
            optimalSpm = optimalSpm.subtract(BigDecimal.valueOf(0.5)).max(minSpm);
            limitingConstraint = "PEAK_LOAD";
            dynacardAnalysis += " High peak load detected.";
        }

        // Counterbalance analysis
        String counterbalanceRec = null;
        BigDecimal cbLow = BigDecimal.valueOf(rpConfig.getCounterbalanceLow());
        BigDecimal cbHigh = BigDecimal.valueOf(rpConfig.getCounterbalanceHigh());

        if (currentCounterbalance.compareTo(cbLow) < 0) {
            counterbalanceRec = String.format("Increase counterbalance from %.0f%% to %.0f%% for optimal load distribution",
                    currentCounterbalance, cbLow.add(cbHigh).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP));
        } else if (currentCounterbalance.compareTo(cbHigh) > 0) {
            counterbalanceRec = String.format("Decrease counterbalance from %.0f%% to %.0f%% for optimal load distribution",
                    currentCounterbalance, cbLow.add(cbHigh).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP));
        }

        // Calculate expected values at optimal settings
        BigDecimal spmRatio = optimalSpm.divide(currentSpm.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP);

        // Production scales with SPM (adjusted for fillage)
        BigDecimal expectedFillage;
        if (optimalSpm.compareTo(currentSpm) < 0) {
            // Slower speed = higher fillage
            expectedFillage = currentFillage.divide(spmRatio, 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));
        } else {
            // Faster speed = lower fillage (but bounded)
            // Use division instead of pow(-1) since BigDecimal doesn't support negative exponents
            expectedFillage = currentFillage.divide(spmRatio, 2, RoundingMode.HALF_UP).max(minFillage);
        }

        result.expectedFillage = expectedFillage.setScale(1, RoundingMode.HALF_UP);

        // Production based on new SPM and fillage
        BigDecimal newTheoretical = pumpDisplacement.multiply(optimalSpm)
                .multiply(BigDecimal.valueOf(1440));
        result.expectedProduction = newTheoretical.multiply(result.expectedFillage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Peak load scales roughly with SPM (dynamic effects)
        result.expectedPeakLoad = currentPeakLoad.multiply(spmRatio.pow(2))
                .setScale(0, RoundingMode.HALF_UP);

        // Power scales with SPM
        result.expectedPower = currentPower.multiply(spmRatio).setScale(2, RoundingMode.HALF_UP);

        // Rod stress scales with load
        result.expectedRodStress = currentRodStress.multiply(spmRatio.pow(2))
                .setScale(0, RoundingMode.HALF_UP);

        // Efficiency improves with better fillage
        BigDecimal fillageImprovement = result.expectedFillage.subtract(currentFillage)
                .divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        result.expectedEfficiency = currentEfficiency.add(fillageImprovement).min(BigDecimal.valueOf(95));

        // Optimal fillage SPM (speed that would give target fillage)
        result.optimalFillageSpm = currentSpm.multiply(currentFillage)
                .divide(targetFillage, 2, RoundingMode.HALF_UP);

        result.optimalSpm = optimalSpm.setScale(1, RoundingMode.HALF_UP);
        result.optimalStrokeLength = optimalStroke.setScale(0, RoundingMode.HALF_UP);
        result.optimizationType = optimizationType;
        result.limitingConstraint = limitingConstraint;
        result.counterbalanceRecommendation = counterbalanceRec;
        result.dynacardAnalysis = dynacardAnalysis;

        // Determine if change is significant
        BigDecimal spmChange = optimalSpm.subtract(currentSpm).abs();
        double minChange = config.getRodPump().getMinSpmChange();
        result.isSignificant = spmChange.compareTo(BigDecimal.valueOf(minChange)) >= 0 || counterbalanceRec != null;

        if (!result.isSignificant) {
            result.notSignificantReason = String.format("SPM change (%.1f) is below minimum threshold (%.1f)",
                    spmChange.doubleValue(), minChange);
        }

        // Confidence
        result.confidence = calculateConfidence(currentFillage, currentPeakLoad, maxPeakLoad,
                currentRodStress, maxRodStress, limitingConstraint);

        return result;
    }

    private double calculateConfidence(BigDecimal fillage, BigDecimal peakLoad, BigDecimal maxPeakLoad,
                                       BigDecimal rodStress, BigDecimal maxRodStress, String constraint) {
        double confidence = 0.85;

        // Reduce confidence if fillage is very low (unreliable data)
        if (fillage.compareTo(BigDecimal.valueOf(30)) < 0) {
            confidence -= 0.15;
        }

        // Reduce if near structural limits
        if (peakLoad.compareTo(maxPeakLoad.multiply(BigDecimal.valueOf(0.85))) > 0) {
            confidence -= 0.1;
        }
        if (rodStress.compareTo(maxRodStress.multiply(BigDecimal.valueOf(0.85))) > 0) {
            confidence -= 0.1;
        }

        return Math.max(confidence, 0.5);
    }

    private Integer calculatePriority(RodPumpOptimizationDto opt) {
        BigDecimal increase = opt.getExpectedProductionIncreasePercent().abs();
        if (increase.compareTo(BigDecimal.valueOf(10)) >= 0) return 1;
        if (increase.compareTo(BigDecimal.valueOf(5)) >= 0) return 2;
        if (increase.compareTo(BigDecimal.valueOf(2)) >= 0) return 3;
        return 4;
    }

    private String buildDescription(RodPumpOptimizationDto opt) {
        StringBuilder sb = new StringBuilder();

        if ("STROKE".equals(opt.getOptimizationType())) {
            sb.append(String.format("Adjust stroke length from %.0f to %.0f inches.\n\n",
                    opt.getCurrentStrokeLength(), opt.getRecommendedStrokeLength()));
        } else {
            sb.append(String.format("Adjust pump speed from %.1f to %.1f SPM.\n\n",
                    opt.getCurrentSpm(), opt.getRecommendedSpm()));
        }

        sb.append("Expected Benefits:\n");
        sb.append(String.format("- Production change: %+.0f BPD (%+.1f%%)\n",
                opt.getExpectedProductionIncrease(), opt.getExpectedProductionIncreasePercent()));
        sb.append(String.format("- Fillage: %.0f%% → %.0f%%\n",
                opt.getCurrentFillage(), opt.getExpectedFillage()));
        sb.append(String.format("- Efficiency: %.1f%% → %.1f%% (%+.1f%%)\n",
                opt.getCurrentPumpEfficiency(), opt.getExpectedPumpEfficiency(), opt.getExpectedEfficiencyImprovement()));

        sb.append("\nCurrent Conditions:\n");
        sb.append(String.format("- SPM: %.1f\n", opt.getCurrentSpm()));
        sb.append(String.format("- Stroke: %.0f inches\n", opt.getCurrentStrokeLength()));
        sb.append(String.format("- Fillage: %.0f%%\n", opt.getCurrentFillage()));
        sb.append(String.format("- Peak load: %.0f lbs\n", opt.getCurrentPeakLoad()));
        sb.append(String.format("- Rod stress: %.0f psi\n", opt.getCurrentRodStress()));

        if (opt.getCounterbalanceRecommendation() != null) {
            sb.append("\nCounterbalance: ").append(opt.getCounterbalanceRecommendation()).append("\n");
        }

        if (opt.getDynacardAnalysis() != null) {
            sb.append("\nAnalysis: ").append(opt.getDynacardAnalysis()).append("\n");
        }

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
        BigDecimal optimalSpm;
        BigDecimal optimalStrokeLength;
        BigDecimal expectedFillage;
        BigDecimal expectedPeakLoad;
        BigDecimal expectedProduction;
        BigDecimal expectedPower;
        BigDecimal expectedEfficiency;
        BigDecimal expectedRodStress;
        BigDecimal optimalFillageSpm;
        BigDecimal theoreticalCapacity;
        BigDecimal volumetricEfficiency;
        String counterbalanceRecommendation;
        String optimizationType;
        String limitingConstraint;
        String dynacardAnalysis;
        double confidence;
        boolean isSignificant;
        String notSignificantReason;
    }
}
