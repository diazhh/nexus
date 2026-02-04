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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.GasLiftAllocationDto;
import org.thingsboard.nexus.po.dto.GasLiftAllocationDto.WellAllocation;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.server.common.data.asset.Asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gas Lift Allocation Optimizer.
 *
 * Optimizes gas injection rates across multiple wells to maximize
 * total field production under total gas availability constraints.
 *
 * Uses marginal analysis approach:
 * - Calculate marginal oil rate (dQ/dG) for each well
 * - Allocate gas to wells with highest marginal returns
 * - Respect min/max constraints per well
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoGasLiftOptimizer {

    private final PoAssetService assetService;
    private final PoAttributeService attributeService;
    private final PoModuleConfiguration config;

    /**
     * Optimizes gas lift allocation for a field/wellpad.
     *
     * @param tenantId Tenant ID
     * @param fieldAssetId Field or wellpad asset ID
     * @param totalAvailableGas Total available gas (MSCF/day), null to use current total
     * @return Optimization result with recommended allocations
     */
    public GasLiftAllocationDto optimizeAllocation(UUID tenantId, UUID fieldAssetId, BigDecimal totalAvailableGas) {
        log.info("Optimizing gas lift allocation for field: {}", fieldAssetId);

        // Get field info
        Optional<Asset> fieldOpt = assetService.getAssetById(tenantId, fieldAssetId);
        if (fieldOpt.isEmpty()) {
            log.warn("Field not found: {}", fieldAssetId);
            return null;
        }

        String fieldName = fieldOpt.get().getName();

        // Get all gas lift wells in this field
        List<WellData> wellDataList = getGasLiftWellsData(tenantId, fieldAssetId);
        if (wellDataList.isEmpty()) {
            log.warn("No gas lift wells found for field: {}", fieldAssetId);
            return null;
        }

        // Calculate current totals
        BigDecimal currentTotalGas = wellDataList.stream()
                .map(w -> w.currentGasRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentTotalProduction = wellDataList.stream()
                .map(w -> w.currentProduction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Use provided gas or current total
        BigDecimal availableGas = totalAvailableGas != null ? totalAvailableGas : currentTotalGas;

        // Apply constraint from config
        PoModuleConfiguration.GasLiftAllocatorConfig glConfig = config.getGasLift();
        availableGas = availableGas.min(BigDecimal.valueOf(glConfig.getMaxTotalGasRate()));

        // Run optimization
        List<WellAllocation> allocations = runMarginalOptimization(wellDataList, availableGas, glConfig);

        // Calculate optimized totals
        BigDecimal optimizedTotalGas = allocations.stream()
                .map(WellAllocation::getRecommendedGasRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedTotalProduction = allocations.stream()
                .map(WellAllocation::getExpectedProduction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal productionIncrease = expectedTotalProduction.subtract(currentTotalProduction);
        BigDecimal productionIncreasePercent = currentTotalProduction.compareTo(BigDecimal.ZERO) > 0
                ? productionIncrease.divide(currentTotalProduction, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate efficiency improvement
        BigDecimal currentEfficiency = currentTotalProduction.compareTo(BigDecimal.ZERO) > 0 && currentTotalGas.compareTo(BigDecimal.ZERO) > 0
                ? currentTotalProduction.divide(currentTotalGas, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal expectedEfficiency = expectedTotalProduction.compareTo(BigDecimal.ZERO) > 0 && optimizedTotalGas.compareTo(BigDecimal.ZERO) > 0
                ? expectedTotalProduction.divide(optimizedTotalGas, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal efficiencyImprovement = currentEfficiency.compareTo(BigDecimal.ZERO) > 0
                ? expectedEfficiency.subtract(currentEfficiency).divide(currentEfficiency, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate confidence based on data quality
        double confidence = calculateConfidence(wellDataList);

        GasLiftAllocationDto result = GasLiftAllocationDto.builder()
                .optimizationId(UUID.randomUUID())
                .fieldAssetId(fieldAssetId)
                .fieldName(fieldName)
                .totalAvailableGas(availableGas)
                .currentTotalGasRate(currentTotalGas)
                .optimizedTotalGasRate(optimizedTotalGas)
                .currentTotalProduction(currentTotalProduction)
                .expectedTotalProduction(expectedTotalProduction)
                .expectedProductionIncrease(productionIncrease)
                .expectedProductionIncreasePercent(productionIncreasePercent)
                .efficiencyImprovement(efficiencyImprovement)
                .wellAllocations(allocations)
                .timestamp(System.currentTimeMillis())
                .confidence(confidence)
                .build();

        log.info("Gas lift optimization completed for {}: {} wells, expected production increase: {} BPD (+{}%)",
                fieldName, allocations.size(), productionIncrease, productionIncreasePercent);

        return result;
    }

    /**
     * Creates recommendations from optimization result.
     */
    public List<RecommendationDto> createRecommendations(UUID tenantId, GasLiftAllocationDto optimization) {
        List<RecommendationDto> recommendations = new ArrayList<>();

        for (WellAllocation alloc : optimization.getWellAllocations()) {
            // Only create recommendation if change is significant (>5% change)
            BigDecimal changePercent = alloc.getCurrentGasRate().compareTo(BigDecimal.ZERO) > 0
                    ? alloc.getGasRateChange().abs().divide(alloc.getCurrentGasRate(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.valueOf(100);

            if (changePercent.compareTo(BigDecimal.valueOf(5)) >= 0) {
                String direction = alloc.getGasRateChange().compareTo(BigDecimal.ZERO) > 0 ? "Increase" : "Decrease";

                RecommendationDto rec = RecommendationDto.builder()
                        .tenantId(tenantId)
                        .assetId(alloc.getWellAssetId())
                        .assetType(PoAssetService.ASSET_TYPE_WELL)
                        .assetName(alloc.getWellName())
                        .type(OptimizationType.GAS_LIFT_ALLOCATION)
                        .priority(calculatePriority(alloc))
                        .title(String.format("%s gas injection from %.0f to %.0f MSCF/day",
                                direction, alloc.getCurrentGasRate(), alloc.getRecommendedGasRate()))
                        .description(buildDescription(optimization, alloc))
                        .currentValue(alloc.getCurrentGasRate())
                        .recommendedValue(alloc.getRecommendedGasRate())
                        .unit("MSCF/day")
                        .expectedProductionIncrease(alloc.getExpectedProductionIncrease())
                        .expectedProductionIncreasePercent(
                                alloc.getCurrentProduction().compareTo(BigDecimal.ZERO) > 0
                                        ? alloc.getExpectedProductionIncrease()
                                            .divide(alloc.getCurrentProduction(), 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO)
                        .confidence(optimization.getConfidence())
                        .build();

                recommendations.add(rec);
            }
        }

        return recommendations;
    }

    // Private methods

    private List<WellData> getGasLiftWellsData(UUID tenantId, UUID fieldAssetId) {
        List<WellData> wellDataList = new ArrayList<>();

        // Get wells related to this field
        Page<Asset> wells = assetService.getWells(tenantId, 0, 1000);

        for (Asset well : wells.getContent()) {
            Map<String, Object> attrs = attributeService.getAttributesAsMap(well.getId().getId());

            // Check if this is a gas lift well
            String liftType = (String) attrs.getOrDefault("lift_type", "");
            if (!"GAS_LIFT".equalsIgnoreCase(liftType) && !"GL".equalsIgnoreCase(liftType)) {
                continue;
            }

            // Check if related to field (simplified - in real impl use relations)
            UUID wellFieldId = getUuidValue(attrs, "field_id");
            if (wellFieldId != null && !wellFieldId.equals(fieldAssetId)) {
                continue;
            }

            WellData data = new WellData();
            data.wellAssetId = well.getId().getId();
            data.wellName = well.getName();
            data.currentGasRate = getDecimalValue(attrs, "gas_injection_rate", BigDecimal.valueOf(500));
            data.currentProduction = getDecimalValue(attrs, "current_production_bpd", BigDecimal.valueOf(200));
            data.gasOilRatio = getDecimalValue(attrs, "gor", BigDecimal.valueOf(1000));

            // Get or estimate performance curve parameters
            data.minGasRate = getDecimalValue(attrs, "min_gas_rate",
                    BigDecimal.valueOf(config.getGasLift().getMinGasPerWell()));
            data.maxGasRate = getDecimalValue(attrs, "max_gas_rate",
                    BigDecimal.valueOf(config.getGasLift().getMaxGasPerWell()));

            // Estimate marginal oil rate from historical data or use empirical model
            data.marginalOilRate = estimateMarginalOilRate(data);

            wellDataList.add(data);
        }

        return wellDataList;
    }

    private BigDecimal estimateMarginalOilRate(WellData data) {
        // Simplified marginal analysis model
        // In reality, this would be derived from production performance curves
        // dQ/dG = a * exp(-b * G) where G is current gas rate

        // Higher base production = higher marginal potential
        // Higher current gas = lower marginal rate (diminishing returns)
        BigDecimal baseMarginal = data.currentProduction
                .divide(data.currentGasRate.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(0.5));

        // Apply diminishing returns factor
        BigDecimal gasUtilization = data.currentGasRate.divide(data.maxGasRate, 4, RoundingMode.HALF_UP);
        BigDecimal diminishingFactor = BigDecimal.ONE.subtract(gasUtilization.multiply(BigDecimal.valueOf(0.3)));

        return baseMarginal.multiply(diminishingFactor).setScale(4, RoundingMode.HALF_UP);
    }

    private List<WellAllocation> runMarginalOptimization(
            List<WellData> wells,
            BigDecimal totalAvailableGas,
            PoModuleConfiguration.GasLiftAllocatorConfig config) {

        // Sort wells by marginal oil rate (descending)
        wells.sort((a, b) -> b.marginalOilRate.compareTo(a.marginalOilRate));

        // Assign priority ranks
        for (int i = 0; i < wells.size(); i++) {
            wells.get(i).priorityRank = i + 1;
        }

        // Phase 1: Allocate minimum gas to all wells
        BigDecimal minGasPerWell = BigDecimal.valueOf(config.getMinGasPerWell());
        BigDecimal maxGasPerWell = BigDecimal.valueOf(config.getMaxGasPerWell());

        BigDecimal remainingGas = totalAvailableGas;
        Map<UUID, BigDecimal> allocations = new HashMap<>();

        for (WellData well : wells) {
            BigDecimal minAlloc = well.minGasRate.max(minGasPerWell);
            if (remainingGas.compareTo(minAlloc) >= 0) {
                allocations.put(well.wellAssetId, minAlloc);
                remainingGas = remainingGas.subtract(minAlloc);
            } else {
                // Not enough gas for minimum - allocate what's left or nothing
                allocations.put(well.wellAssetId, remainingGas.max(BigDecimal.ZERO));
                remainingGas = BigDecimal.ZERO;
            }
        }

        // Phase 2: Allocate remaining gas to highest marginal wells
        BigDecimal incrementSize = BigDecimal.valueOf(50); // Allocate in 50 MSCF/day increments

        while (remainingGas.compareTo(incrementSize) >= 0) {
            boolean allocated = false;

            for (WellData well : wells) {
                BigDecimal currentAlloc = allocations.get(well.wellAssetId);
                BigDecimal wellMax = well.maxGasRate.min(maxGasPerWell);

                if (currentAlloc.add(incrementSize).compareTo(wellMax) <= 0) {
                    allocations.put(well.wellAssetId, currentAlloc.add(incrementSize));
                    remainingGas = remainingGas.subtract(incrementSize);
                    allocated = true;
                    break;
                }
            }

            if (!allocated) {
                break; // All wells at max
            }
        }

        // Build result allocations
        List<WellAllocation> result = new ArrayList<>();
        for (WellData well : wells) {
            BigDecimal recommendedGas = allocations.get(well.wellAssetId);
            BigDecimal gasChange = recommendedGas.subtract(well.currentGasRate);

            // Estimate production at new gas rate
            BigDecimal productionChange = gasChange.multiply(well.marginalOilRate);
            BigDecimal expectedProduction = well.currentProduction.add(productionChange).max(BigDecimal.ZERO);

            WellAllocation alloc = WellAllocation.builder()
                    .wellAssetId(well.wellAssetId)
                    .wellName(well.wellName)
                    .currentGasRate(well.currentGasRate.setScale(0, RoundingMode.HALF_UP))
                    .recommendedGasRate(recommendedGas.setScale(0, RoundingMode.HALF_UP))
                    .gasRateChange(gasChange.setScale(0, RoundingMode.HALF_UP))
                    .currentProduction(well.currentProduction.setScale(0, RoundingMode.HALF_UP))
                    .expectedProduction(expectedProduction.setScale(0, RoundingMode.HALF_UP))
                    .expectedProductionIncrease(productionChange.setScale(0, RoundingMode.HALF_UP))
                    .marginalOilRate(well.marginalOilRate)
                    .gasOilRatio(well.gasOilRatio)
                    .priorityRank(well.priorityRank)
                    .atMinimum(recommendedGas.compareTo(well.minGasRate) <= 0)
                    .atMaximum(recommendedGas.compareTo(well.maxGasRate) >= 0)
                    .build();

            result.add(alloc);
        }

        return result;
    }

    private double calculateConfidence(List<WellData> wells) {
        if (wells.isEmpty()) return 0.5;

        // Base confidence
        double confidence = 0.8;

        // Reduce confidence if too few wells (less statistical validity)
        if (wells.size() < 5) {
            confidence -= 0.1;
        }

        // Reduce if high variability in marginal rates
        List<BigDecimal> marginals = wells.stream()
                .map(w -> w.marginalOilRate)
                .collect(Collectors.toList());
        BigDecimal maxMarginal = marginals.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        BigDecimal minMarginal = marginals.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        if (maxMarginal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal spread = maxMarginal.subtract(minMarginal).divide(maxMarginal, 4, RoundingMode.HALF_UP);
            if (spread.compareTo(BigDecimal.valueOf(0.5)) > 0) {
                confidence -= 0.05;
            }
        }

        return Math.max(confidence, 0.5);
    }

    private Integer calculatePriority(WellAllocation alloc) {
        // Priority 1-5, lower is higher priority
        BigDecimal increase = alloc.getExpectedProductionIncrease().abs();
        if (increase.compareTo(BigDecimal.valueOf(50)) >= 0) return 1;
        if (increase.compareTo(BigDecimal.valueOf(25)) >= 0) return 2;
        if (increase.compareTo(BigDecimal.valueOf(10)) >= 0) return 3;
        return 4;
    }

    private String buildDescription(GasLiftAllocationDto optimization, WellAllocation alloc) {
        StringBuilder sb = new StringBuilder();

        String direction = alloc.getGasRateChange().compareTo(BigDecimal.ZERO) > 0 ? "Increase" : "Decrease";
        sb.append(String.format("%s gas injection rate from %.0f to %.0f MSCF/day.\n\n",
                direction, alloc.getCurrentGasRate(), alloc.getRecommendedGasRate()));

        sb.append("Expected Benefits:\n");
        sb.append(String.format("- Production change: %+.0f BPD\n", alloc.getExpectedProductionIncrease()));
        sb.append(String.format("- Marginal oil rate: %.3f BPD per MSCF/day\n", alloc.getMarginalOilRate()));

        sb.append("\nField Context:\n");
        sb.append(String.format("- Field: %s\n", optimization.getFieldName()));
        sb.append(String.format("- Total available gas: %.0f MSCF/day\n", optimization.getTotalAvailableGas()));
        sb.append(String.format("- Field production increase: +%.0f BPD (+%.1f%%)\n",
                optimization.getExpectedProductionIncrease(),
                optimization.getExpectedProductionIncreasePercent()));

        sb.append(String.format("\nWell priority rank: %d of %d\n",
                alloc.getPriorityRank(), optimization.getWellAllocations().size()));

        if (alloc.getAtMinimum()) {
            sb.append("⚠️ Well at minimum gas rate constraint\n");
        }
        if (alloc.getAtMaximum()) {
            sb.append("⚠️ Well at maximum gas rate constraint\n");
        }

        sb.append(String.format("\nConfidence: %.0f%%", optimization.getConfidence() * 100));

        return sb.toString();
    }

    private BigDecimal getDecimalValue(Map<String, Object> attrs, String key, BigDecimal defaultValue) {
        Object value = attrs.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return defaultValue;
    }

    private UUID getUuidValue(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    // Inner class for well data during optimization
    private static class WellData {
        UUID wellAssetId;
        String wellName;
        BigDecimal currentGasRate;
        BigDecimal currentProduction;
        BigDecimal gasOilRatio;
        BigDecimal minGasRate;
        BigDecimal maxGasRate;
        BigDecimal marginalOilRate;
        Integer priorityRank;
    }
}
