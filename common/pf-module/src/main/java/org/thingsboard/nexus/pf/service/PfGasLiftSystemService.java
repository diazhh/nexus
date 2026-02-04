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
package org.thingsboard.nexus.pf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.OperationalLimitDto;
import org.thingsboard.nexus.pf.dto.PfGasLiftSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Gas Lift System entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfGasLiftSystemService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;
    private final PfAlarmService alarmService;

    /**
     * Creates a new Gas Lift System for a well.
     */
    public PfGasLiftSystemDto createGasLiftSystem(UUID tenantId, PfGasLiftSystemDto dto) {
        log.info("Creating Gas Lift system for well: {}", dto.getWellId());

        // Validate well exists
        if (!pfAssetService.existsById(dto.getWellId())) {
            throw new PfEntityNotFoundException("Well", dto.getWellId());
        }

        String assetName = String.format("GL-%s-%s",
                dto.getGasLiftType().name().substring(0, 4),
                dto.getWellId().toString().substring(0, 8));

        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfGasLiftSystemDto.ASSET_TYPE,
                assetName,
                "Gas Lift System"
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        saveGasLiftAttributes(dto);

        // Create relation to well
        pfHierarchyService.createInstalledInRelation(tenantId, dto.getAssetId(), dto.getWellId());

        // Create default operational limits
        createDefaultOperationalLimits(dto);

        log.info("Gas Lift system created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Gas Lift System by ID.
     */
    public Optional<PfGasLiftSystemDto> getGasLiftSystemById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfGasLiftSystemDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfGasLiftSystemDto dto = new PfGasLiftSystemDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadGasLiftAttributes(dto);

                    return dto;
                });
    }

    /**
     * Gets Gas Lift System by Well ID.
     */
    public Optional<PfGasLiftSystemDto> getGasLiftSystemByWell(UUID tenantId, UUID wellId) {
        List<UUID> equipmentIds = pfHierarchyService.getEquipmentInWell(tenantId, wellId);

        for (UUID equipmentId : equipmentIds) {
            Optional<PfGasLiftSystemDto> gasLift = getGasLiftSystemById(equipmentId);
            if (gasLift.isPresent()) {
                return gasLift;
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all Gas Lift Systems for a tenant.
     */
    public Page<PfGasLiftSystemDto> getAllGasLiftSystems(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfGasLiftSystemDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Updates a Gas Lift System.
     */
    public PfGasLiftSystemDto updateGasLiftSystem(PfGasLiftSystemDto dto) {
        log.info("Updating Gas Lift system: {}", dto.getAssetId());

        dto.setUpdatedTime(System.currentTimeMillis());
        saveGasLiftAttributes(dto);

        return dto;
    }

    /**
     * Records a survey/optimization date.
     */
    public void recordSurvey(UUID assetId, LocalDate surveyDate) {
        log.info("Recording Gas Lift survey for {}", assetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfGasLiftSystemDto.ATTR_LAST_SURVEY_DATE, surveyDate.toString());
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Calculates gas lift efficiency based on injection rate and production.
     */
    public GasLiftEfficiency calculateEfficiency(UUID assetId, BigDecimal injectionRateMscfd,
                                                  BigDecimal oilProductionBpd) {
        return getGasLiftSystemById(assetId)
                .map(gl -> {
                    // Gas Lift Ratio (GLR) = Gas Injected / Oil Produced (MCF/BBL)
                    BigDecimal glr = injectionRateMscfd.divide(oilProductionBpd, 2, java.math.RoundingMode.HALF_UP);

                    // Efficiency is inversely related to GLR (lower GLR = more efficient)
                    // Typical efficient range: 0.3 - 0.8 MCF/BBL
                    String status;
                    int score;

                    double glrValue = glr.doubleValue();
                    if (glrValue < 0.5) {
                        status = "EXCELLENT";
                        score = 95;
                    } else if (glrValue < 0.8) {
                        status = "GOOD";
                        score = 85;
                    } else if (glrValue < 1.2) {
                        status = "FAIR";
                        score = 70;
                    } else {
                        status = "POOR";
                        score = 50;
                    }

                    return new GasLiftEfficiency(assetId, glr, score, status);
                })
                .orElse(new GasLiftEfficiency(assetId, BigDecimal.ZERO, 0, "UNKNOWN"));
    }

    /**
     * Deletes a Gas Lift System.
     */
    public void deleteGasLiftSystem(UUID tenantId, UUID assetId) {
        log.warn("Deleting Gas Lift system: {}", assetId);
        pfHierarchyService.removeAllRelations(tenantId, assetId);
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    // Helper methods

    private void createDefaultOperationalLimits(PfGasLiftSystemDto dto) {
        // Create operational limits for gas lift
        if (dto.getMaxInjectionRateMscfd() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfGasLiftSystemDto.ASSET_TYPE)
                    .variableKey("injection_rate_mscfd")
                    .highHighLimit(dto.getMaxInjectionRateMscfd())
                    .highLimit(dto.getMaxInjectionRateMscfd().multiply(BigDecimal.valueOf(0.95)))
                    .lowLimit(dto.getMinInjectionRateMscfd())
                    .lowLowLimit(dto.getMinInjectionRateMscfd() != null ?
                            dto.getMinInjectionRateMscfd().multiply(BigDecimal.valueOf(0.9)) : null)
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxInjectionPressurePsi() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfGasLiftSystemDto.ASSET_TYPE)
                    .variableKey("injection_pressure_psi")
                    .highHighLimit(dto.getMaxInjectionPressurePsi())
                    .highLimit(dto.getMaxInjectionPressurePsi().multiply(BigDecimal.valueOf(0.9)))
                    .lowLimit(dto.getMinInjectionPressurePsi())
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxCasingPressurePsi() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfGasLiftSystemDto.ASSET_TYPE)
                    .variableKey("casing_pressure_psi")
                    .highHighLimit(dto.getMaxCasingPressurePsi())
                    .highLimit(dto.getMaxCasingPressurePsi().multiply(BigDecimal.valueOf(0.9)))
                    .lowLimit(dto.getMinCasingPressurePsi())
                    .enabled(true)
                    .build());
        }
    }

    private void saveGasLiftAttributes(PfGasLiftSystemDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getWellId() != null) attrs.put("well_id", dto.getWellId().toString());
        if (dto.getGasLiftType() != null) attrs.put(PfGasLiftSystemDto.ATTR_GAS_LIFT_TYPE, dto.getGasLiftType().name());
        if (dto.getNumValves() != null) attrs.put(PfGasLiftSystemDto.ATTR_NUM_VALVES, dto.getNumValves());
        if (dto.getOperatingValveDepthFt() != null) attrs.put(PfGasLiftSystemDto.ATTR_OPERATING_VALVE_DEPTH_FT, dto.getOperatingValveDepthFt());
        if (dto.getInjectionPointDepthFt() != null) attrs.put(PfGasLiftSystemDto.ATTR_INJECTION_POINT_DEPTH_FT, dto.getInjectionPointDepthFt());
        if (dto.getMandrelSize() != null) attrs.put(PfGasLiftSystemDto.ATTR_MANDREL_SIZE, dto.getMandrelSize());
        if (dto.getValveType() != null) attrs.put(PfGasLiftSystemDto.ATTR_VALVE_TYPE, dto.getValveType());
        if (dto.getDesignInjectionRateMscfd() != null) attrs.put(PfGasLiftSystemDto.ATTR_DESIGN_INJECTION_RATE_MSCFD, dto.getDesignInjectionRateMscfd());
        if (dto.getDesignInjectionPressurePsi() != null) attrs.put(PfGasLiftSystemDto.ATTR_DESIGN_INJECTION_PRESSURE_PSI, dto.getDesignInjectionPressurePsi());
        if (dto.getGasSpecificGravity() != null) attrs.put(PfGasLiftSystemDto.ATTR_GAS_SPECIFIC_GRAVITY, dto.getGasSpecificGravity());
        if (dto.getCasingPressurePsi() != null) attrs.put(PfGasLiftSystemDto.ATTR_CASING_PRESSURE_PSI, dto.getCasingPressurePsi());
        if (dto.getTubingSizeIn() != null) attrs.put(PfGasLiftSystemDto.ATTR_TUBING_SIZE_IN, dto.getTubingSizeIn());
        if (dto.getCasingSizeIn() != null) attrs.put(PfGasLiftSystemDto.ATTR_CASING_SIZE_IN, dto.getCasingSizeIn());
        if (dto.getInstallationDate() != null) attrs.put(PfGasLiftSystemDto.ATTR_INSTALLATION_DATE, dto.getInstallationDate().toString());
        if (dto.getLastSurveyDate() != null) attrs.put(PfGasLiftSystemDto.ATTR_LAST_SURVEY_DATE, dto.getLastSurveyDate().toString());

        // Operational limits
        if (dto.getMinInjectionRateMscfd() != null) attrs.put(PfGasLiftSystemDto.ATTR_MIN_INJECTION_RATE_MSCFD, dto.getMinInjectionRateMscfd());
        if (dto.getMaxInjectionRateMscfd() != null) attrs.put(PfGasLiftSystemDto.ATTR_MAX_INJECTION_RATE_MSCFD, dto.getMaxInjectionRateMscfd());
        if (dto.getMinInjectionPressurePsi() != null) attrs.put(PfGasLiftSystemDto.ATTR_MIN_INJECTION_PRESSURE_PSI, dto.getMinInjectionPressurePsi());
        if (dto.getMaxInjectionPressurePsi() != null) attrs.put(PfGasLiftSystemDto.ATTR_MAX_INJECTION_PRESSURE_PSI, dto.getMaxInjectionPressurePsi());
        if (dto.getMinCasingPressurePsi() != null) attrs.put(PfGasLiftSystemDto.ATTR_MIN_CASING_PRESSURE_PSI, dto.getMinCasingPressurePsi());
        if (dto.getMaxCasingPressurePsi() != null) attrs.put(PfGasLiftSystemDto.ATTR_MAX_CASING_PRESSURE_PSI, dto.getMaxCasingPressurePsi());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadGasLiftAttributes(PfGasLiftSystemDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case "well_id" -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setWellId(UUID.fromString(strValue));
                    }
                }
                case PfGasLiftSystemDto.ATTR_GAS_LIFT_TYPE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setGasLiftType(PfGasLiftSystemDto.GasLiftType.valueOf(strValue));
                    }
                }
                case PfGasLiftSystemDto.ATTR_NUM_VALVES -> entry.getLongValue().ifPresent(v -> dto.setNumValves(v.intValue()));
                case PfGasLiftSystemDto.ATTR_OPERATING_VALVE_DEPTH_FT -> entry.getDoubleValue().ifPresent(v -> dto.setOperatingValveDepthFt(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_INJECTION_POINT_DEPTH_FT -> entry.getDoubleValue().ifPresent(v -> dto.setInjectionPointDepthFt(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_MANDREL_SIZE -> dto.setMandrelSize(strValue);
                case PfGasLiftSystemDto.ATTR_VALVE_TYPE -> dto.setValveType(strValue);
                case PfGasLiftSystemDto.ATTR_DESIGN_INJECTION_RATE_MSCFD -> entry.getDoubleValue().ifPresent(v -> dto.setDesignInjectionRateMscfd(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_DESIGN_INJECTION_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setDesignInjectionPressurePsi(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_GAS_SPECIFIC_GRAVITY -> entry.getDoubleValue().ifPresent(v -> dto.setGasSpecificGravity(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_CASING_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setCasingPressurePsi(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_TUBING_SIZE_IN -> entry.getDoubleValue().ifPresent(v -> dto.setTubingSizeIn(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_CASING_SIZE_IN -> entry.getDoubleValue().ifPresent(v -> dto.setCasingSizeIn(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_INSTALLATION_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setInstallationDate(LocalDate.parse(strValue));
                    }
                }
                case PfGasLiftSystemDto.ATTR_LAST_SURVEY_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setLastSurveyDate(LocalDate.parse(strValue));
                    }
                }
                case PfGasLiftSystemDto.ATTR_MIN_INJECTION_RATE_MSCFD -> entry.getDoubleValue().ifPresent(v -> dto.setMinInjectionRateMscfd(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_MAX_INJECTION_RATE_MSCFD -> entry.getDoubleValue().ifPresent(v -> dto.setMaxInjectionRateMscfd(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_MIN_INJECTION_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setMinInjectionPressurePsi(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_MAX_INJECTION_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setMaxInjectionPressurePsi(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_MIN_CASING_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setMinCasingPressurePsi(BigDecimal.valueOf(v)));
                case PfGasLiftSystemDto.ATTR_MAX_CASING_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setMaxCasingPressurePsi(BigDecimal.valueOf(v)));
            }
        }
    }

    private PfGasLiftSystemDto mapAssetToDto(Asset asset) {
        PfGasLiftSystemDto dto = new PfGasLiftSystemDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setCreatedTime(asset.getCreatedTime());
        loadGasLiftAttributes(dto);
        return dto;
    }

    /**
     * Record for Gas Lift efficiency.
     */
    public record GasLiftEfficiency(UUID assetId, BigDecimal gasLiftRatio, int efficiencyScore, String status) {}
}
