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
package org.thingsboard.nexus.rv.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.rv.dto.RvIprModelDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for managing IPR (Inflow Performance Relationship) Models.
 * Implements Vogel, Fetkovich, and composite IPR calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvIprModelService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;
    private final RvCalculationService calculationService;

    /**
     * Creates a new IPR Model.
     */
    public RvIprModelDto createIprModel(UUID tenantId, RvIprModelDto dto) {
        log.info("Creating IPR model: {} for well: {}", dto.getName(), dto.getWellAssetId());

        if (dto.getWellAssetId() != null) {
            if (!rvAssetService.existsById(dto.getWellAssetId())) {
                throw new RvEntityNotFoundException("Well", dto.getWellAssetId());
            }
        }

        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvIprModelDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        saveIprAttributes(dto);

        // Link to well if specified
        if (dto.getWellAssetId() != null) {
            rvHierarchyService.createRelation(tenantId, dto.getWellAssetId(), dto.getAssetId(), "HasIPR");
        }

        // Link to completion if specified
        if (dto.getCompletionAssetId() != null) {
            rvHierarchyService.createRelation(tenantId, dto.getCompletionAssetId(), dto.getAssetId(), "HasIPR");
        }

        log.info("IPR model created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets an IPR Model by ID.
     */
    public Optional<RvIprModelDto> getIprModelById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvIprModelDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvIprModelDto dto = new RvIprModelDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadIprAttributes(dto);

                return dto;
            });
    }

    /**
     * Gets all IPR Models for a tenant.
     */
    public Page<RvIprModelDto> getAllIprModels(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvIprModelDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets IPR Models by Well.
     */
    public List<RvIprModelDto> getIprModelsByWell(UUID tenantId, UUID wellAssetId) {
        List<UUID> iprIds = rvHierarchyService.getRelatedAssets(tenantId, wellAssetId, "HasIPR");
        List<RvIprModelDto> models = new ArrayList<>();

        for (UUID iprId : iprIds) {
            getIprModelById(iprId).ifPresent(models::add);
        }

        // Sort by analysis date (most recent first)
        models.sort((a, b) -> {
            Long dateA = a.getAnalysisDate() != null ? a.getAnalysisDate() : 0L;
            Long dateB = b.getAnalysisDate() != null ? b.getAnalysisDate() : 0L;
            return dateB.compareTo(dateA);
        });

        return models;
    }

    /**
     * Calculates Vogel IPR from test data.
     * For undersaturated reservoirs (Pr > Pb), uses straight line above Pb and Vogel below.
     */
    public RvIprModelDto calculateVogelIpr(UUID assetId, BigDecimal reservoirPressure,
                                            BigDecimal bubblePointPressure, BigDecimal testRate,
                                            BigDecimal testPwf) {
        log.info("Calculating Vogel IPR for model {}: Pr={}, Pb={}, qtest={}, Pwf_test={}",
            assetId, reservoirPressure, bubblePointPressure, testRate, testPwf);

        RvIprModelDto dto = getIprModelById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("IPR Model", assetId));

        dto.setReservoirPressurePsi(reservoirPressure);
        dto.setBubblePointPressurePsi(bubblePointPressure);
        dto.setTestRateBopd(testRate);
        dto.setTestPwfPsi(testPwf);
        dto.setIprMethod("VOGEL");
        dto.setAnalysisDate(System.currentTimeMillis());

        // Determine if reservoir is saturated or undersaturated
        boolean isSaturated = reservoirPressure.compareTo(bubblePointPressure) <= 0;
        dto.setIsBelowBubblePoint(isSaturated);

        BigDecimal qmax;
        BigDecimal productivityIndex;

        if (isSaturated) {
            // Reservoir at or below bubble point - pure Vogel
            // qo = qmax * [1 - 0.2*(Pwf/Pr) - 0.8*(Pwf/Pr)²]
            // Solve for qmax from test point
            BigDecimal pwfRatio = testPwf.divide(reservoirPressure, 6, RoundingMode.HALF_UP);
            BigDecimal vogelFactor = BigDecimal.ONE
                .subtract(BigDecimal.valueOf(0.2).multiply(pwfRatio))
                .subtract(BigDecimal.valueOf(0.8).multiply(pwfRatio.pow(2)));

            qmax = testRate.divide(vogelFactor, 2, RoundingMode.HALF_UP);

            // Calculate productivity index at Pr (slope at Pwf=Pr)
            productivityIndex = qmax.multiply(BigDecimal.valueOf(1.8))
                .divide(reservoirPressure, 4, RoundingMode.HALF_UP);

        } else {
            // Reservoir above bubble point - composite IPR
            if (testPwf.compareTo(bubblePointPressure) >= 0) {
                // Test was above bubble point - straight line
                productivityIndex = testRate.divide(
                    reservoirPressure.subtract(testPwf), 4, RoundingMode.HALF_UP);

                // Flow rate at bubble point
                BigDecimal qb = productivityIndex.multiply(reservoirPressure.subtract(bubblePointPressure));

                // qmax from Vogel below Pb
                qmax = qb.add(productivityIndex.multiply(bubblePointPressure).divide(BigDecimal.valueOf(1.8), 2, RoundingMode.HALF_UP));

                dto.setProductivityIndexAbovePb(productivityIndex);

            } else {
                // Test was below bubble point - need to solve composite
                // This is more complex, use iterative approach
                productivityIndex = estimateProductivityIndex(reservoirPressure, bubblePointPressure, testRate, testPwf);

                BigDecimal qb = productivityIndex.multiply(reservoirPressure.subtract(bubblePointPressure));
                qmax = qb.add(productivityIndex.multiply(bubblePointPressure).divide(BigDecimal.valueOf(1.8), 2, RoundingMode.HALF_UP));

                dto.setProductivityIndexAbovePb(productivityIndex);
            }
        }

        dto.setQmaxBopd(qmax);
        dto.setProductivityIndexBpdPsi(productivityIndex);
        dto.setVogelCoefficient(BigDecimal.valueOf(0.8));

        // Calculate ideal qmax without skin
        if (dto.getSkinFactor() != null && dto.getSkinFactor().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal flowEfficiency = calculateFlowEfficiency(dto.getSkinFactor(), reservoirPressure, testPwf);
            dto.setFlowEfficiency(flowEfficiency);
            dto.setIdealQmaxBopd(qmax.divide(flowEfficiency, 2, RoundingMode.HALF_UP));
        }

        dto.setUpdatedTime(System.currentTimeMillis());
        saveIprAttributes(dto);

        log.info("Vogel IPR calculated: qmax={} bopd, J={} bpd/psi", qmax, productivityIndex);
        return dto;
    }

    /**
     * Generates IPR curve data points for visualization.
     */
    public List<Map<String, Object>> generateIprCurve(UUID assetId, int numPoints) {
        RvIprModelDto dto = getIprModelById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("IPR Model", assetId));

        if (dto.getQmaxBopd() == null || dto.getReservoirPressurePsi() == null) {
            throw new IllegalStateException("IPR parameters not set. Run calculateVogelIpr first.");
        }

        List<Map<String, Object>> curve = new ArrayList<>();
        BigDecimal qmax = dto.getQmaxBopd();
        BigDecimal pr = dto.getReservoirPressurePsi();
        BigDecimal pb = dto.getBubblePointPressurePsi();
        BigDecimal productivityIndex = dto.getProductivityIndexBpdPsi();
        boolean isSaturated = Boolean.TRUE.equals(dto.getIsBelowBubblePoint());

        BigDecimal pressureStep = pr.divide(BigDecimal.valueOf(numPoints), 2, RoundingMode.HALF_UP);

        for (int i = 0; i <= numPoints; i++) {
            BigDecimal pwf = pr.subtract(pressureStep.multiply(BigDecimal.valueOf(i)));
            if (pwf.compareTo(BigDecimal.ZERO) < 0) {
                pwf = BigDecimal.ZERO;
            }

            BigDecimal rate;

            if (isSaturated) {
                // Pure Vogel
                rate = calculationService.calculateIprVogel(qmax, pr, pwf);
            } else {
                // Composite
                if (pwf.compareTo(pb) >= 0) {
                    // Above bubble point - straight line
                    rate = productivityIndex.multiply(pr.subtract(pwf));
                } else {
                    // Below bubble point - Vogel
                    BigDecimal qb = productivityIndex.multiply(pr.subtract(pb));
                    BigDecimal qmaxBelow = qb.add(productivityIndex.multiply(pb).divide(BigDecimal.valueOf(1.8), 2, RoundingMode.HALF_UP));
                    rate = qb.add(qmaxBelow.subtract(qb).multiply(
                        BigDecimal.ONE
                            .subtract(BigDecimal.valueOf(0.2).multiply(pwf.divide(pb, 6, RoundingMode.HALF_UP)))
                            .subtract(BigDecimal.valueOf(0.8).multiply(pwf.divide(pb, 6, RoundingMode.HALF_UP).pow(2)))
                    ));
                }
            }

            Map<String, Object> point = new HashMap<>();
            point.put("pwfPsi", pwf);
            point.put("rateBopd", rate.max(BigDecimal.ZERO));
            curve.add(point);
        }

        return curve;
    }

    /**
     * Calculates operating point efficiency.
     */
    public Map<String, Object> calculateOperatingPoint(UUID assetId, BigDecimal currentPwf) {
        RvIprModelDto dto = getIprModelById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("IPR Model", assetId));

        if (dto.getQmaxBopd() == null) {
            throw new IllegalStateException("IPR parameters not set.");
        }

        BigDecimal currentRate = calculationService.calculateIprVogel(
            dto.getQmaxBopd(), dto.getReservoirPressurePsi(), currentPwf);

        BigDecimal efficiency = currentRate.divide(dto.getQmaxBopd(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        // Update model with current operating point
        dto.setCurrentPwfPsi(currentPwf);
        dto.setCurrentRateBopd(currentRate);
        dto.setOperatingEfficiencyPercent(efficiency);
        saveIprAttributes(dto);

        Map<String, Object> result = new HashMap<>();
        result.put("currentPwfPsi", currentPwf);
        result.put("currentRateBopd", currentRate);
        result.put("qmaxBopd", dto.getQmaxBopd());
        result.put("efficiencyPercent", efficiency);
        result.put("drawdownPsi", dto.getReservoirPressurePsi().subtract(currentPwf));

        return result;
    }

    /**
     * Updates an IPR Model.
     */
    public RvIprModelDto updateIprModel(RvIprModelDto dto) {
        log.info("Updating IPR model: {}", dto.getAssetId());

        rvAssetService.getAssetById(dto.getAssetId()).ifPresent(asset -> {
            boolean needsUpdate = false;
            if (!asset.getName().equals(dto.getName())) {
                asset.setName(dto.getName());
                needsUpdate = true;
            }
            if (!Objects.equals(asset.getLabel(), dto.getLabel())) {
                asset.setLabel(dto.getLabel());
                needsUpdate = true;
            }
            if (needsUpdate) {
                rvAssetService.updateAsset(asset);
            }
        });

        dto.setUpdatedTime(System.currentTimeMillis());
        saveIprAttributes(dto);

        return dto;
    }

    /**
     * Deletes an IPR Model.
     */
    public void deleteIprModel(UUID tenantId, UUID assetId) {
        log.warn("Deleting IPR model: {}", assetId);
        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    private BigDecimal estimateProductivityIndex(BigDecimal pr, BigDecimal pb, BigDecimal testRate, BigDecimal testPwf) {
        // Iterative solution for composite IPR
        // Start with linear assumption
        BigDecimal j = testRate.divide(pr.subtract(testPwf), 4, RoundingMode.HALF_UP);

        // Iterate to refine
        for (int iter = 0; iter < 10; iter++) {
            BigDecimal qb = j.multiply(pr.subtract(pb));
            BigDecimal qmax = qb.add(j.multiply(pb).divide(BigDecimal.valueOf(1.8), 4, RoundingMode.HALF_UP));

            BigDecimal pwfRatio = testPwf.divide(pb, 6, RoundingMode.HALF_UP);
            BigDecimal vogelPart = qmax.subtract(qb).multiply(
                BigDecimal.ONE
                    .subtract(BigDecimal.valueOf(0.2).multiply(pwfRatio))
                    .subtract(BigDecimal.valueOf(0.8).multiply(pwfRatio.pow(2)))
            );
            BigDecimal calculatedRate = qb.add(vogelPart);

            BigDecimal error = testRate.subtract(calculatedRate);
            if (error.abs().compareTo(BigDecimal.valueOf(0.1)) < 0) {
                break;
            }

            // Adjust J
            j = j.multiply(testRate.divide(calculatedRate, 4, RoundingMode.HALF_UP));
        }

        return j;
    }

    private BigDecimal calculateFlowEfficiency(BigDecimal skin, BigDecimal pr, BigDecimal pwf) {
        // FE = (Pr - Pwf - ΔPskin) / (Pr - Pwf)
        // ΔPskin ≈ skin * (Pr - Pwf) / 7 (approximation)
        BigDecimal drawdown = pr.subtract(pwf);
        BigDecimal deltaPskin = skin.multiply(drawdown).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);
        return drawdown.subtract(deltaPskin).divide(drawdown, 4, RoundingMode.HALF_UP);
    }

    private void saveIprAttributes(RvIprModelDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getModelCode() != null) attrs.put(RvIprModelDto.ATTR_MODEL_CODE, dto.getModelCode());
        if (dto.getAnalysisDate() != null) attrs.put(RvIprModelDto.ATTR_ANALYSIS_DATE, dto.getAnalysisDate());
        if (dto.getIprMethod() != null) attrs.put(RvIprModelDto.ATTR_IPR_METHOD, dto.getIprMethod());
        if (dto.getReservoirPressurePsi() != null) attrs.put(RvIprModelDto.ATTR_RESERVOIR_PRESSURE_PSI, dto.getReservoirPressurePsi());
        if (dto.getBubblePointPressurePsi() != null) attrs.put("bubble_point_pressure_psi", dto.getBubblePointPressurePsi());
        if (dto.getReservoirTemperatureF() != null) attrs.put("reservoir_temperature_f", dto.getReservoirTemperatureF());
        if (dto.getIsBelowBubblePoint() != null) attrs.put("is_below_bubble_point", dto.getIsBelowBubblePoint());
        if (dto.getTestRateBopd() != null) attrs.put("test_rate_bopd", dto.getTestRateBopd());
        if (dto.getTestPwfPsi() != null) attrs.put("test_pwf_psi", dto.getTestPwfPsi());
        if (dto.getTestDate() != null) attrs.put("test_date", dto.getTestDate());
        if (dto.getTestType() != null) attrs.put("test_type", dto.getTestType());
        if (dto.getProductivityIndexBpdPsi() != null) attrs.put(RvIprModelDto.ATTR_PRODUCTIVITY_INDEX, dto.getProductivityIndexBpdPsi());
        if (dto.getProductivityIndexAbovePb() != null) attrs.put("productivity_index_above_pb", dto.getProductivityIndexAbovePb());
        if (dto.getQmaxBopd() != null) attrs.put(RvIprModelDto.ATTR_QMAX_BOPD, dto.getQmaxBopd());
        if (dto.getVogelCoefficient() != null) attrs.put("vogel_coefficient", dto.getVogelCoefficient());
        if (dto.getFetkovichC() != null) attrs.put("fetkovich_c", dto.getFetkovichC());
        if (dto.getFetkovichN() != null) attrs.put("fetkovich_n", dto.getFetkovichN());
        if (dto.getCurrentPwfPsi() != null) attrs.put("current_pwf_psi", dto.getCurrentPwfPsi());
        if (dto.getCurrentRateBopd() != null) attrs.put("current_rate_bopd", dto.getCurrentRateBopd());
        if (dto.getOperatingEfficiencyPercent() != null) attrs.put("operating_efficiency_percent", dto.getOperatingEfficiencyPercent());
        if (dto.getSkinFactor() != null) attrs.put(RvIprModelDto.ATTR_SKIN_FACTOR, dto.getSkinFactor());
        if (dto.getFlowEfficiency() != null) attrs.put(RvIprModelDto.ATTR_FLOW_EFFICIENCY, dto.getFlowEfficiency());
        if (dto.getIdealQmaxBopd() != null) attrs.put("ideal_qmax_bopd", dto.getIdealQmaxBopd());
        if (dto.getModelQuality() != null) attrs.put("model_quality", dto.getModelQuality());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadIprAttributes(RvIprModelDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvIprModelDto.ATTR_MODEL_CODE -> dto.setModelCode(entry.getValueAsString());
                case RvIprModelDto.ATTR_ANALYSIS_DATE -> entry.getLongValue().ifPresent(dto::setAnalysisDate);
                case RvIprModelDto.ATTR_IPR_METHOD -> dto.setIprMethod(entry.getValueAsString());
                case RvIprModelDto.ATTR_RESERVOIR_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setReservoirPressurePsi(BigDecimal.valueOf(v)));
                case "bubble_point_pressure_psi" -> entry.getDoubleValue().ifPresent(v -> dto.setBubblePointPressurePsi(BigDecimal.valueOf(v)));
                case "reservoir_temperature_f" -> entry.getDoubleValue().ifPresent(v -> dto.setReservoirTemperatureF(BigDecimal.valueOf(v)));
                case "is_below_bubble_point" -> entry.getBooleanValue().ifPresent(dto::setIsBelowBubblePoint);
                case "test_rate_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setTestRateBopd(BigDecimal.valueOf(v)));
                case "test_pwf_psi" -> entry.getDoubleValue().ifPresent(v -> dto.setTestPwfPsi(BigDecimal.valueOf(v)));
                case "test_date" -> entry.getLongValue().ifPresent(dto::setTestDate);
                case "test_type" -> dto.setTestType(entry.getValueAsString());
                case RvIprModelDto.ATTR_PRODUCTIVITY_INDEX -> entry.getDoubleValue().ifPresent(v -> dto.setProductivityIndexBpdPsi(BigDecimal.valueOf(v)));
                case "productivity_index_above_pb" -> entry.getDoubleValue().ifPresent(v -> dto.setProductivityIndexAbovePb(BigDecimal.valueOf(v)));
                case RvIprModelDto.ATTR_QMAX_BOPD -> entry.getDoubleValue().ifPresent(v -> dto.setQmaxBopd(BigDecimal.valueOf(v)));
                case "vogel_coefficient" -> entry.getDoubleValue().ifPresent(v -> dto.setVogelCoefficient(BigDecimal.valueOf(v)));
                case "fetkovich_c" -> entry.getDoubleValue().ifPresent(v -> dto.setFetkovichC(BigDecimal.valueOf(v)));
                case "fetkovich_n" -> entry.getDoubleValue().ifPresent(v -> dto.setFetkovichN(BigDecimal.valueOf(v)));
                case "current_pwf_psi" -> entry.getDoubleValue().ifPresent(v -> dto.setCurrentPwfPsi(BigDecimal.valueOf(v)));
                case "current_rate_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setCurrentRateBopd(BigDecimal.valueOf(v)));
                case "operating_efficiency_percent" -> entry.getDoubleValue().ifPresent(v -> dto.setOperatingEfficiencyPercent(BigDecimal.valueOf(v)));
                case RvIprModelDto.ATTR_SKIN_FACTOR -> entry.getDoubleValue().ifPresent(v -> dto.setSkinFactor(BigDecimal.valueOf(v)));
                case RvIprModelDto.ATTR_FLOW_EFFICIENCY -> entry.getDoubleValue().ifPresent(v -> dto.setFlowEfficiency(BigDecimal.valueOf(v)));
                case "ideal_qmax_bopd" -> entry.getDoubleValue().ifPresent(v -> dto.setIdealQmaxBopd(BigDecimal.valueOf(v)));
                case "model_quality" -> dto.setModelQuality(entry.getValueAsString());
            }
        }
    }

    private RvIprModelDto mapAssetToDto(Asset asset) {
        RvIprModelDto dto = new RvIprModelDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadIprAttributes(dto);
        return dto;
    }
}
