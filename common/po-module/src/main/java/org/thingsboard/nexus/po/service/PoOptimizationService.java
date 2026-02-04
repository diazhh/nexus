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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.EspOptimizationDto;
import org.thingsboard.nexus.po.dto.GasLiftAllocationDto;
import org.thingsboard.nexus.po.dto.PcpOptimizationDto;
import org.thingsboard.nexus.po.dto.RodPumpOptimizationDto;
import org.thingsboard.nexus.po.dto.OptimizationResultDto;
import org.thingsboard.nexus.po.dto.OptimizationResultDto.OptimizationRunStatus;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.nexus.po.exception.PoOptimizationException;
import org.thingsboard.nexus.po.model.PoOptimizationResult;
import org.thingsboard.nexus.po.repository.PoOptimizationResultRepository;
import org.thingsboard.server.common.data.asset.Asset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service for production optimization.
 * Orchestrates different optimizer implementations and manages optimization results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoOptimizationService {

    private final PoOptimizationResultRepository resultRepository;
    private final PoRecommendationService recommendationService;
    private final PoAssetService assetService;
    private final PoEspFrequencyOptimizer espOptimizer;
    private final PoGasLiftOptimizer gasLiftOptimizer;
    private final PoPcpSpeedOptimizer pcpOptimizer;
    private final PoRodPumpOptimizer rodPumpOptimizer;
    private final PoModuleConfiguration config;
    private final ObjectMapper objectMapper;

    /**
     * Runs ESP frequency optimization for a well.
     */
    @Transactional
    public OptimizationResultDto optimizeEspFrequency(UUID tenantId, UUID wellAssetId, UUID triggeredBy) {
        log.info("Starting ESP frequency optimization for well: {}", wellAssetId);

        long startTime = System.currentTimeMillis();

        // Create result entity
        PoOptimizationResult result = PoOptimizationResult.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .assetId(wellAssetId)
                .assetType(PoAssetService.ASSET_TYPE_WELL)
                .optimizationType(OptimizationType.ESP_FREQUENCY)
                .runStatus(OptimizationRunStatus.RUNNING)
                .algorithm("ESP_FREQ_OPT_V1")
                .algorithmVersion("1.0.0")
                .triggeredBy(triggeredBy)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            // Run optimization
            EspOptimizationDto optimizationResult = espOptimizer.optimizeFrequency(tenantId, wellAssetId, null);

            if (optimizationResult == null) {
                result.setRunStatus(OptimizationRunStatus.FAILED);
                result.setErrorMessage("Well not found or no ESP data available");
                resultRepository.save(result);
                return mapToDto(result);
            }

            // Store results
            result.setOptimalValue(optimizationResult.getRecommendedFrequency());
            result.setOptimalValueUnit("Hz");
            result.setOutputResults(buildOutputResults(optimizationResult));
            result.setConverged(true);
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            result.setRunStatus(OptimizationRunStatus.COMPLETED);

            PoOptimizationResult saved = resultRepository.save(result);

            // Create recommendation if significant
            if (optimizationResult.getIsSignificant()) {
                RecommendationDto recommendation = espOptimizer.createRecommendation(tenantId, optimizationResult);
                if (recommendation != null) {
                    recommendation.setOptimizationResultId(saved.getId());
                    recommendationService.createRecommendation(recommendation);
                }
            }

            log.info("ESP optimization completed for well {}: optimal frequency = {} Hz",
                    wellAssetId, optimizationResult.getRecommendedFrequency());

            return mapToDto(saved);

        } catch (Exception e) {
            log.error("ESP optimization failed for well {}: {}", wellAssetId, e.getMessage(), e);
            result.setRunStatus(OptimizationRunStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            resultRepository.save(result);

            throw new PoOptimizationException("ESP optimization failed: " + e.getMessage(),
                    OptimizationType.ESP_FREQUENCY, wellAssetId, e);
        }
    }

    /**
     * Runs optimization for all ESP wells in a tenant.
     */
    @Transactional
    public List<OptimizationResultDto> optimizeAllEspWells(UUID tenantId, UUID triggeredBy) {
        log.info("Starting batch ESP optimization for tenant: {}", tenantId);

        List<OptimizationResultDto> results = new ArrayList<>();
        Page<Asset> espSystems = assetService.getEspSystems(tenantId, 0, 1000);

        for (Asset esp : espSystems.getContent()) {
            try {
                // Get the parent well for this ESP
                // In a real implementation, you'd use relations to find the well
                UUID wellAssetId = esp.getId().getId(); // Simplified

                OptimizationResultDto result = optimizeEspFrequency(tenantId, wellAssetId, triggeredBy);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to optimize ESP {}: {}", esp.getName(), e.getMessage());
            }
        }

        log.info("Batch ESP optimization completed: {} wells processed", results.size());
        return results;
    }

    // ==================== GAS LIFT OPTIMIZATION ====================

    /**
     * Runs gas lift allocation optimization for a field.
     */
    @Transactional
    public OptimizationResultDto optimizeGasLiftAllocation(UUID tenantId, UUID fieldAssetId, BigDecimal totalAvailableGas, UUID triggeredBy) {
        log.info("Starting gas lift allocation optimization for field: {}", fieldAssetId);

        long startTime = System.currentTimeMillis();

        // Create result entity
        PoOptimizationResult result = PoOptimizationResult.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .assetId(fieldAssetId)
                .assetType(PoAssetService.ASSET_TYPE_FIELD)
                .optimizationType(OptimizationType.GAS_LIFT_ALLOCATION)
                .runStatus(OptimizationRunStatus.RUNNING)
                .algorithm("GAS_LIFT_MARGINAL_OPT_V1")
                .algorithmVersion("1.0.0")
                .triggeredBy(triggeredBy)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            // Run optimization
            GasLiftAllocationDto optimizationResult = gasLiftOptimizer.optimizeAllocation(tenantId, fieldAssetId, totalAvailableGas);

            if (optimizationResult == null) {
                result.setRunStatus(OptimizationRunStatus.FAILED);
                result.setErrorMessage("Field not found or no gas lift wells available");
                resultRepository.save(result);
                return mapToDto(result);
            }

            // Store results
            result.setOptimalValue(optimizationResult.getExpectedTotalProduction());
            result.setOptimalValueUnit("BPD");
            result.setOutputResults(buildGasLiftOutputResults(optimizationResult));
            result.setConverged(true);
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            result.setRunStatus(OptimizationRunStatus.COMPLETED);

            PoOptimizationResult saved = resultRepository.save(result);

            // Create recommendations for significant changes
            List<RecommendationDto> recommendations = gasLiftOptimizer.createRecommendations(tenantId, optimizationResult);
            for (RecommendationDto rec : recommendations) {
                rec.setOptimizationResultId(saved.getId());
                recommendationService.createRecommendation(rec);
            }

            log.info("Gas lift optimization completed for field {}: {} recommendations created, expected production: {} BPD",
                    fieldAssetId, recommendations.size(), optimizationResult.getExpectedTotalProduction());

            return mapToDto(saved);

        } catch (Exception e) {
            log.error("Gas lift optimization failed for field {}: {}", fieldAssetId, e.getMessage(), e);
            result.setRunStatus(OptimizationRunStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            resultRepository.save(result);

            throw new PoOptimizationException("Gas lift optimization failed: " + e.getMessage(),
                    OptimizationType.GAS_LIFT_ALLOCATION, fieldAssetId, e);
        }
    }

    /**
     * Gets gas lift allocation result for a field.
     */
    public GasLiftAllocationDto getGasLiftAllocation(UUID tenantId, UUID fieldAssetId, BigDecimal totalAvailableGas) {
        return gasLiftOptimizer.optimizeAllocation(tenantId, fieldAssetId, totalAvailableGas);
    }

    // ==================== PCP OPTIMIZATION ====================

    /**
     * Runs PCP speed optimization for a well.
     */
    @Transactional
    public OptimizationResultDto optimizePcpSpeed(UUID tenantId, UUID wellAssetId, UUID triggeredBy) {
        log.info("Starting PCP speed optimization for well: {}", wellAssetId);

        long startTime = System.currentTimeMillis();

        // Create result entity
        PoOptimizationResult result = PoOptimizationResult.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .assetId(wellAssetId)
                .assetType(PoAssetService.ASSET_TYPE_WELL)
                .optimizationType(OptimizationType.PCP_SPEED)
                .runStatus(OptimizationRunStatus.RUNNING)
                .algorithm("PCP_SPEED_OPT_V1")
                .algorithmVersion("1.0.0")
                .triggeredBy(triggeredBy)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            // Run optimization
            PcpOptimizationDto optimizationResult = pcpOptimizer.optimizeSpeed(tenantId, wellAssetId, null);

            if (optimizationResult == null) {
                result.setRunStatus(OptimizationRunStatus.FAILED);
                result.setErrorMessage("Well not found or no PCP data available");
                resultRepository.save(result);
                return mapToDto(result);
            }

            // Store results
            result.setOptimalValue(optimizationResult.getRecommendedRpm());
            result.setOptimalValueUnit("RPM");
            result.setOutputResults(buildPcpOutputResults(optimizationResult));
            result.setConverged(true);
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            result.setRunStatus(OptimizationRunStatus.COMPLETED);

            PoOptimizationResult saved = resultRepository.save(result);

            // Create recommendation if significant
            if (optimizationResult.getIsSignificant()) {
                RecommendationDto recommendation = pcpOptimizer.createRecommendation(tenantId, optimizationResult);
                if (recommendation != null) {
                    recommendation.setOptimizationResultId(saved.getId());
                    recommendationService.createRecommendation(recommendation);
                }
            }

            log.info("PCP optimization completed for well {}: optimal RPM = {}",
                    wellAssetId, optimizationResult.getRecommendedRpm());

            return mapToDto(saved);

        } catch (Exception e) {
            log.error("PCP optimization failed for well {}: {}", wellAssetId, e.getMessage(), e);
            result.setRunStatus(OptimizationRunStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            resultRepository.save(result);

            throw new PoOptimizationException("PCP optimization failed: " + e.getMessage(),
                    OptimizationType.PCP_SPEED, wellAssetId, e);
        }
    }

    /**
     * Runs optimization for all PCP wells in a tenant.
     */
    @Transactional
    public List<OptimizationResultDto> optimizeAllPcpWells(UUID tenantId, UUID triggeredBy) {
        log.info("Starting batch PCP optimization for tenant: {}", tenantId);

        List<OptimizationResultDto> results = new ArrayList<>();
        Page<Asset> pcpSystems = assetService.getPcpSystems(tenantId, 0, 1000);

        for (Asset pcp : pcpSystems.getContent()) {
            try {
                UUID wellAssetId = pcp.getId().getId(); // Simplified
                OptimizationResultDto result = optimizePcpSpeed(tenantId, wellAssetId, triggeredBy);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to optimize PCP {}: {}", pcp.getName(), e.getMessage());
            }
        }

        log.info("Batch PCP optimization completed: {} wells processed", results.size());
        return results;
    }

    // ==================== ROD PUMP OPTIMIZATION ====================

    /**
     * Runs rod pump optimization for a well.
     */
    @Transactional
    public OptimizationResultDto optimizeRodPump(UUID tenantId, UUID wellAssetId, UUID triggeredBy) {
        log.info("Starting rod pump optimization for well: {}", wellAssetId);

        long startTime = System.currentTimeMillis();

        // Create result entity
        PoOptimizationResult result = PoOptimizationResult.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .assetId(wellAssetId)
                .assetType(PoAssetService.ASSET_TYPE_WELL)
                .optimizationType(OptimizationType.ROD_PUMP_SPEED)
                .runStatus(OptimizationRunStatus.RUNNING)
                .algorithm("ROD_PUMP_OPT_V1")
                .algorithmVersion("1.0.0")
                .triggeredBy(triggeredBy)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            // Run optimization
            RodPumpOptimizationDto optimizationResult = rodPumpOptimizer.optimize(tenantId, wellAssetId, null);

            if (optimizationResult == null) {
                result.setRunStatus(OptimizationRunStatus.FAILED);
                result.setErrorMessage("Well not found or no rod pump data available");
                resultRepository.save(result);
                return mapToDto(result);
            }

            // Store results
            result.setOptimalValue(optimizationResult.getRecommendedSpm());
            result.setOptimalValueUnit("SPM");
            result.setOutputResults(buildRodPumpOutputResults(optimizationResult));
            result.setConverged(true);
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            result.setRunStatus(OptimizationRunStatus.COMPLETED);

            PoOptimizationResult saved = resultRepository.save(result);

            // Create recommendation if significant
            if (optimizationResult.getIsSignificant()) {
                RecommendationDto recommendation = rodPumpOptimizer.createRecommendation(tenantId, optimizationResult);
                if (recommendation != null) {
                    recommendation.setOptimizationResultId(saved.getId());
                    recommendationService.createRecommendation(recommendation);
                }
            }

            log.info("Rod pump optimization completed for well {}: optimal SPM = {}",
                    wellAssetId, optimizationResult.getRecommendedSpm());

            return mapToDto(saved);

        } catch (Exception e) {
            log.error("Rod pump optimization failed for well {}: {}", wellAssetId, e.getMessage(), e);
            result.setRunStatus(OptimizationRunStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            result.setComputationTimeMs(System.currentTimeMillis() - startTime);
            resultRepository.save(result);

            throw new PoOptimizationException("Rod pump optimization failed: " + e.getMessage(),
                    OptimizationType.ROD_PUMP_SPEED, wellAssetId, e);
        }
    }

    /**
     * Runs optimization for all rod pump wells in a tenant.
     */
    @Transactional
    public List<OptimizationResultDto> optimizeAllRodPumpWells(UUID tenantId, UUID triggeredBy) {
        log.info("Starting batch rod pump optimization for tenant: {}", tenantId);

        List<OptimizationResultDto> results = new ArrayList<>();
        Page<Asset> rodPumpSystems = assetService.getRodPumpSystems(tenantId, 0, 1000);

        for (Asset rodPump : rodPumpSystems.getContent()) {
            try {
                UUID wellAssetId = rodPump.getId().getId(); // Simplified
                OptimizationResultDto result = optimizeRodPump(tenantId, wellAssetId, triggeredBy);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to optimize rod pump {}: {}", rodPump.getName(), e.getMessage());
            }
        }

        log.info("Batch rod pump optimization completed: {} wells processed", results.size());
        return results;
    }

    // ==================== RESULT QUERIES ====================

    /**
     * Gets optimization result by ID.
     */
    public Optional<OptimizationResultDto> getResult(UUID id) {
        return resultRepository.findById(id).map(this::mapToDto);
    }

    /**
     * Gets optimization results for a tenant.
     */
    public Page<OptimizationResultDto> getResults(UUID tenantId, int page, int size) {
        Page<PoOptimizationResult> results = resultRepository.findByTenantIdOrderByTimestampDesc(
                tenantId, PageRequest.of(page, size));
        return mapToPage(results, page, size);
    }

    /**
     * Gets optimization results for an asset.
     */
    public Page<OptimizationResultDto> getResultsForAsset(UUID assetId, int page, int size) {
        Page<PoOptimizationResult> results = resultRepository.findByAssetIdOrderByTimestampDesc(
                assetId, PageRequest.of(page, size));
        return mapToPage(results, page, size);
    }

    /**
     * Gets latest optimization result for an asset.
     */
    public Optional<OptimizationResultDto> getLatestResult(UUID assetId) {
        return resultRepository.findFirstByAssetIdOrderByTimestampDesc(assetId).map(this::mapToDto);
    }

    /**
     * Gets latest optimization result for an asset and type.
     */
    public Optional<OptimizationResultDto> getLatestResult(UUID assetId, OptimizationType type) {
        return resultRepository.findFirstByAssetIdAndOptimizationTypeOrderByTimestampDesc(assetId, type)
                .map(this::mapToDto);
    }

    /**
     * Gets results by optimization type.
     */
    public Page<OptimizationResultDto> getResultsByType(UUID tenantId, OptimizationType type, int page, int size) {
        Page<PoOptimizationResult> results = resultRepository.findByTenantIdAndOptimizationTypeOrderByTimestampDesc(
                tenantId, type, PageRequest.of(page, size));
        return mapToPage(results, page, size);
    }

    /**
     * Gets results within a time range.
     */
    public List<OptimizationResultDto> getResultsInTimeRange(UUID tenantId, Long fromTs, Long toTs) {
        return resultRepository.findByTenantIdAndTimestampBetween(tenantId, fromTs, toTs)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Counts results by status.
     */
    public long countByStatus(UUID tenantId, OptimizationRunStatus status) {
        return resultRepository.countByTenantIdAndRunStatus(tenantId, status);
    }

    /**
     * Counts results by type.
     */
    public long countByType(UUID tenantId, OptimizationType type) {
        return resultRepository.countByTenantIdAndOptimizationType(tenantId, type);
    }

    // Helper methods

    private ObjectNode buildOutputResults(EspOptimizationDto opt) {
        ObjectNode results = objectMapper.createObjectNode();
        results.put("currentFrequency", opt.getCurrentFrequency().doubleValue());
        results.put("recommendedFrequency", opt.getRecommendedFrequency().doubleValue());
        results.put("frequencyChange", opt.getFrequencyChange().doubleValue());
        results.put("expectedProductionIncrease", opt.getExpectedProductionIncrease().doubleValue());
        results.put("expectedProductionIncreasePercent", opt.getExpectedProductionIncreasePercent().doubleValue());
        results.put("expectedEfficiencyImprovement", opt.getExpectedEfficiencyImprovement().doubleValue());
        results.put("confidence", opt.getConfidence());
        results.put("isSignificant", opt.getIsSignificant());
        if (opt.getLimitingConstraint() != null) {
            results.put("limitingConstraint", opt.getLimitingConstraint());
        }
        return results;
    }

    private ObjectNode buildGasLiftOutputResults(GasLiftAllocationDto opt) {
        ObjectNode results = objectMapper.createObjectNode();
        results.put("fieldName", opt.getFieldName());
        results.put("totalAvailableGas", opt.getTotalAvailableGas().doubleValue());
        results.put("currentTotalGasRate", opt.getCurrentTotalGasRate().doubleValue());
        results.put("optimizedTotalGasRate", opt.getOptimizedTotalGasRate().doubleValue());
        results.put("currentTotalProduction", opt.getCurrentTotalProduction().doubleValue());
        results.put("expectedTotalProduction", opt.getExpectedTotalProduction().doubleValue());
        results.put("expectedProductionIncrease", opt.getExpectedProductionIncrease().doubleValue());
        results.put("expectedProductionIncreasePercent", opt.getExpectedProductionIncreasePercent().doubleValue());
        results.put("efficiencyImprovement", opt.getEfficiencyImprovement().doubleValue());
        results.put("wellCount", opt.getWellAllocations().size());
        results.put("confidence", opt.getConfidence());
        return results;
    }

    private ObjectNode buildPcpOutputResults(PcpOptimizationDto opt) {
        ObjectNode results = objectMapper.createObjectNode();
        results.put("currentRpm", opt.getCurrentRpm().doubleValue());
        results.put("recommendedRpm", opt.getRecommendedRpm().doubleValue());
        results.put("rpmChange", opt.getRpmChange().doubleValue());
        results.put("expectedProductionIncrease", opt.getExpectedProductionIncrease().doubleValue());
        results.put("expectedProductionIncreasePercent", opt.getExpectedProductionIncreasePercent().doubleValue());
        results.put("expectedEfficiencyImprovement", opt.getExpectedEfficiencyImprovement().doubleValue());
        results.put("currentTorque", opt.getCurrentTorque().doubleValue());
        results.put("expectedTorque", opt.getExpectedTorque().doubleValue());
        results.put("rodWearFactor", opt.getRodWearFactor().doubleValue());
        results.put("statorWearFactor", opt.getStatorWearFactor().doubleValue());
        results.put("confidence", opt.getConfidence());
        results.put("isSignificant", opt.getIsSignificant());
        if (opt.getLimitingConstraint() != null) {
            results.put("limitingConstraint", opt.getLimitingConstraint());
        }
        return results;
    }

    private ObjectNode buildRodPumpOutputResults(RodPumpOptimizationDto opt) {
        ObjectNode results = objectMapper.createObjectNode();
        results.put("currentSpm", opt.getCurrentSpm().doubleValue());
        results.put("recommendedSpm", opt.getRecommendedSpm().doubleValue());
        results.put("spmChange", opt.getSpmChange().doubleValue());
        results.put("currentStrokeLength", opt.getCurrentStrokeLength().doubleValue());
        results.put("recommendedStrokeLength", opt.getRecommendedStrokeLength().doubleValue());
        results.put("currentFillage", opt.getCurrentFillage().doubleValue());
        results.put("expectedFillage", opt.getExpectedFillage().doubleValue());
        results.put("expectedProductionIncrease", opt.getExpectedProductionIncrease().doubleValue());
        results.put("expectedProductionIncreasePercent", opt.getExpectedProductionIncreasePercent().doubleValue());
        results.put("expectedEfficiencyImprovement", opt.getExpectedEfficiencyImprovement().doubleValue());
        results.put("currentPeakLoad", opt.getCurrentPeakLoad().doubleValue());
        results.put("expectedPeakLoad", opt.getExpectedPeakLoad().doubleValue());
        results.put("currentRodStress", opt.getCurrentRodStress().doubleValue());
        results.put("expectedRodStress", opt.getExpectedRodStress().doubleValue());
        results.put("confidence", opt.getConfidence());
        results.put("isSignificant", opt.getIsSignificant());
        results.put("optimizationType", opt.getOptimizationType());
        if (opt.getLimitingConstraint() != null) {
            results.put("limitingConstraint", opt.getLimitingConstraint());
        }
        if (opt.getCounterbalanceRecommendation() != null) {
            results.put("counterbalanceRecommendation", opt.getCounterbalanceRecommendation());
        }
        if (opt.getDynacardAnalysis() != null) {
            results.put("dynacardAnalysis", opt.getDynacardAnalysis());
        }
        return results;
    }

    private OptimizationResultDto mapToDto(PoOptimizationResult entity) {
        return OptimizationResultDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .assetId(entity.getAssetId())
                .assetType(entity.getAssetType())
                .assetName(assetService.getAssetName(entity.getAssetId()))
                .type(entity.getOptimizationType())
                .runStatus(entity.getRunStatus())
                .algorithm(entity.getAlgorithm())
                .algorithmVersion(entity.getAlgorithmVersion())
                .inputParameters(entity.getInputParameters())
                .outputResults(entity.getOutputResults())
                .optimalValue(entity.getOptimalValue())
                .optimalValueUnit(entity.getOptimalValueUnit())
                .objectiveValue(entity.getObjectiveValue())
                .iterations(entity.getIterations())
                .converged(entity.getConverged())
                .computationTimeMs(entity.getComputationTimeMs())
                .dataQualityScore(entity.getDataQualityScore())
                .dataWindowStart(entity.getDataWindowStart())
                .dataWindowEnd(entity.getDataWindowEnd())
                .errorMessage(entity.getErrorMessage())
                .triggeredBy(entity.getTriggeredBy())
                .timestamp(entity.getTimestamp())
                .build();
    }

    private Page<OptimizationResultDto> mapToPage(Page<PoOptimizationResult> entities, int page, int size) {
        List<OptimizationResultDto> dtos = entities.getContent().stream()
                .map(this::mapToDto)
                .toList();
        return new PageImpl<>(dtos, PageRequest.of(page, size), entities.getTotalElements());
    }
}
