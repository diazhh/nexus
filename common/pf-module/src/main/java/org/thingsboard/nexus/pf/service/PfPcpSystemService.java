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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.OperationalLimitDto;
import org.thingsboard.nexus.pf.dto.PfPcpSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing PCP (Progressing Cavity Pump) System entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfPcpSystemService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;
    private final PfAlarmService alarmService;

    /**
     * Creates a new PCP System for a well.
     */
    public PfPcpSystemDto createPcpSystem(UUID tenantId, PfPcpSystemDto dto) {
        log.info("Creating PCP system for well: {}", dto.getWellId());

        // Validate well exists
        if (!pfAssetService.existsById(dto.getWellId())) {
            throw new PfEntityNotFoundException("Well", dto.getWellId());
        }

        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfPcpSystemDto.ASSET_TYPE,
                "PCP-" + dto.getPumpSerialNumber(),
                "PCP System"
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Calculate run life if installation date is set
        if (dto.getInstallationDate() != null) {
            int runLife = (int) ChronoUnit.DAYS.between(dto.getInstallationDate(), LocalDate.now());
            dto.setRunLifeDays(runLife);
        }

        savePcpAttributes(dto);

        // Create relation to well
        pfHierarchyService.createInstalledInRelation(tenantId, dto.getAssetId(), dto.getWellId());

        // Create default operational limits
        createDefaultOperationalLimits(dto);

        log.info("PCP system created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a PCP System by ID.
     */
    public Optional<PfPcpSystemDto> getPcpSystemById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfPcpSystemDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfPcpSystemDto dto = new PfPcpSystemDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadPcpAttributes(dto);

                    // Update run life
                    if (dto.getInstallationDate() != null) {
                        int runLife = (int) ChronoUnit.DAYS.between(dto.getInstallationDate(), LocalDate.now());
                        dto.setRunLifeDays(runLife);
                    }

                    return dto;
                });
    }

    /**
     * Gets PCP System by Well ID.
     */
    public Optional<PfPcpSystemDto> getPcpSystemByWell(UUID tenantId, UUID wellId) {
        List<UUID> equipmentIds = pfHierarchyService.getEquipmentInWell(tenantId, wellId);

        for (UUID equipmentId : equipmentIds) {
            Optional<PfPcpSystemDto> pcp = getPcpSystemById(equipmentId);
            if (pcp.isPresent()) {
                return pcp;
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all PCP Systems for a tenant.
     */
    public Page<PfPcpSystemDto> getAllPcpSystems(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfPcpSystemDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Updates a PCP System.
     */
    public PfPcpSystemDto updatePcpSystem(PfPcpSystemDto dto) {
        log.info("Updating PCP system: {}", dto.getAssetId());

        dto.setUpdatedTime(System.currentTimeMillis());
        savePcpAttributes(dto);

        return dto;
    }

    /**
     * Records a pulling event.
     */
    public void recordPulling(UUID assetId, LocalDate pullingDate) {
        log.info("Recording PCP pulling for {}", assetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfPcpSystemDto.ATTR_LAST_PULLING_DATE, pullingDate.toString());
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Gets PCP health status based on operational parameters.
     */
    public PcpHealthStatus getHealthStatus(UUID assetId) {
        return getPcpSystemById(assetId)
                .map(pcp -> {
                    int score = 100;
                    String status = "GOOD";

                    // Check run life
                    if (pcp.getRunLifeDays() != null && pcp.getRunLifeDays() > 365) {
                        score -= 10;
                    }
                    if (pcp.getRunLifeDays() != null && pcp.getRunLifeDays() > 730) {
                        score -= 20;
                        status = "ATTENTION";
                    }

                    // PCP stators typically have shorter life due to elastomer wear
                    if (pcp.getRunLifeDays() != null && pcp.getRunLifeDays() > 500) {
                        score -= 15;
                        status = "ATTENTION";
                    }

                    return new PcpHealthStatus(assetId, score, status, pcp.getRunLifeDays());
                })
                .orElse(new PcpHealthStatus(assetId, 0, "UNKNOWN", null));
    }

    /**
     * Deletes a PCP System.
     */
    public void deletePcpSystem(UUID tenantId, UUID assetId) {
        log.warn("Deleting PCP system: {}", assetId);
        pfHierarchyService.removeAllRelations(tenantId, assetId);
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    // Helper methods

    private void createDefaultOperationalLimits(PfPcpSystemDto dto) {
        // Create operational limits based on PCP specs
        if (dto.getMaxRpm() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfPcpSystemDto.ASSET_TYPE)
                    .variableKey("rpm")
                    .highHighLimit(dto.getMaxRpm())
                    .highLimit(dto.getMaxRpm().multiply(BigDecimal.valueOf(0.95)))
                    .lowLimit(dto.getMinRpm())
                    .lowLowLimit(dto.getMinRpm() != null ?
                            dto.getMinRpm().multiply(BigDecimal.valueOf(0.9)) : null)
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxTorqueFtLb() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfPcpSystemDto.ASSET_TYPE)
                    .variableKey("torque_ft_lb")
                    .highHighLimit(dto.getMaxTorqueFtLb())
                    .highLimit(dto.getMaxTorqueFtLb().multiply(BigDecimal.valueOf(0.85)))
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxRodLoadLb() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfPcpSystemDto.ASSET_TYPE)
                    .variableKey("rod_load_lb")
                    .highHighLimit(dto.getMaxRodLoadLb())
                    .highLimit(dto.getMaxRodLoadLb().multiply(BigDecimal.valueOf(0.9)))
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxFluidTempF() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfPcpSystemDto.ASSET_TYPE)
                    .variableKey("fluid_temp_f")
                    .highHighLimit(dto.getMaxFluidTempF())
                    .highLimit(dto.getMaxFluidTempF().multiply(BigDecimal.valueOf(0.9)))
                    .enabled(true)
                    .build());
        }
    }

    private void savePcpAttributes(PfPcpSystemDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getWellId() != null) attrs.put("well_id", dto.getWellId().toString());
        if (dto.getPumpModel() != null) attrs.put(PfPcpSystemDto.ATTR_PUMP_MODEL, dto.getPumpModel());
        if (dto.getPumpSerialNumber() != null) attrs.put(PfPcpSystemDto.ATTR_PUMP_SERIAL_NUMBER, dto.getPumpSerialNumber());
        if (dto.getRotorType() != null) attrs.put(PfPcpSystemDto.ATTR_ROTOR_TYPE, dto.getRotorType());
        if (dto.getStatorElastomer() != null) attrs.put(PfPcpSystemDto.ATTR_STATOR_ELASTOMER, dto.getStatorElastomer());
        if (dto.getStages() != null) attrs.put(PfPcpSystemDto.ATTR_STAGES, dto.getStages());
        if (dto.getNominalDisplacementBpdRpm() != null) attrs.put(PfPcpSystemDto.ATTR_NOMINAL_DISPLACEMENT_BPD_RPM, dto.getNominalDisplacementBpdRpm());
        if (dto.getMaxHeadFt() != null) attrs.put(PfPcpSystemDto.ATTR_MAX_HEAD_FT, dto.getMaxHeadFt());
        if (dto.getDriveType() != null) attrs.put(PfPcpSystemDto.ATTR_DRIVE_TYPE, dto.getDriveType().name());
        if (dto.getMotorHp() != null) attrs.put(PfPcpSystemDto.ATTR_MOTOR_HP, dto.getMotorHp());
        if (dto.getGearboxRatio() != null) attrs.put(PfPcpSystemDto.ATTR_GEARBOX_RATIO, dto.getGearboxRatio());
        if (dto.getRodStringType() != null) attrs.put(PfPcpSystemDto.ATTR_ROD_STRING_TYPE, dto.getRodStringType());
        if (dto.getSettingDepthFt() != null) attrs.put(PfPcpSystemDto.ATTR_SETTING_DEPTH_FT, dto.getSettingDepthFt());
        if (dto.getInstallationDate() != null) attrs.put(PfPcpSystemDto.ATTR_INSTALLATION_DATE, dto.getInstallationDate().toString());
        if (dto.getLastPullingDate() != null) attrs.put(PfPcpSystemDto.ATTR_LAST_PULLING_DATE, dto.getLastPullingDate().toString());
        if (dto.getRunLifeDays() != null) attrs.put(PfPcpSystemDto.ATTR_RUN_LIFE_DAYS, dto.getRunLifeDays());

        // Operational limits
        if (dto.getMinRpm() != null) attrs.put(PfPcpSystemDto.ATTR_MIN_RPM, dto.getMinRpm());
        if (dto.getMaxRpm() != null) attrs.put(PfPcpSystemDto.ATTR_MAX_RPM, dto.getMaxRpm());
        if (dto.getMaxTorqueFtLb() != null) attrs.put(PfPcpSystemDto.ATTR_MAX_TORQUE_FT_LB, dto.getMaxTorqueFtLb());
        if (dto.getMaxRodLoadLb() != null) attrs.put(PfPcpSystemDto.ATTR_MAX_ROD_LOAD_LB, dto.getMaxRodLoadLb());
        if (dto.getMaxFluidTempF() != null) attrs.put(PfPcpSystemDto.ATTR_MAX_FLUID_TEMP_F, dto.getMaxFluidTempF());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadPcpAttributes(PfPcpSystemDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case "well_id" -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setWellId(UUID.fromString(strValue));
                    }
                }
                case PfPcpSystemDto.ATTR_PUMP_MODEL -> dto.setPumpModel(strValue);
                case PfPcpSystemDto.ATTR_PUMP_SERIAL_NUMBER -> dto.setPumpSerialNumber(strValue);
                case PfPcpSystemDto.ATTR_ROTOR_TYPE -> dto.setRotorType(strValue);
                case PfPcpSystemDto.ATTR_STATOR_ELASTOMER -> dto.setStatorElastomer(strValue);
                case PfPcpSystemDto.ATTR_STAGES -> entry.getLongValue().ifPresent(v -> dto.setStages(v.intValue()));
                case PfPcpSystemDto.ATTR_NOMINAL_DISPLACEMENT_BPD_RPM -> entry.getDoubleValue().ifPresent(v -> dto.setNominalDisplacementBpdRpm(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_MAX_HEAD_FT -> entry.getDoubleValue().ifPresent(v -> dto.setMaxHeadFt(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_DRIVE_TYPE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setDriveType(PfPcpSystemDto.PcpDriveType.valueOf(strValue));
                    }
                }
                case PfPcpSystemDto.ATTR_MOTOR_HP -> entry.getDoubleValue().ifPresent(v -> dto.setMotorHp(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_GEARBOX_RATIO -> entry.getDoubleValue().ifPresent(v -> dto.setGearboxRatio(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_ROD_STRING_TYPE -> dto.setRodStringType(strValue);
                case PfPcpSystemDto.ATTR_SETTING_DEPTH_FT -> entry.getDoubleValue().ifPresent(v -> dto.setSettingDepthFt(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_INSTALLATION_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setInstallationDate(LocalDate.parse(strValue));
                    }
                }
                case PfPcpSystemDto.ATTR_LAST_PULLING_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setLastPullingDate(LocalDate.parse(strValue));
                    }
                }
                case PfPcpSystemDto.ATTR_RUN_LIFE_DAYS -> entry.getLongValue().ifPresent(v -> dto.setRunLifeDays(v.intValue()));
                case PfPcpSystemDto.ATTR_MIN_RPM -> entry.getDoubleValue().ifPresent(v -> dto.setMinRpm(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_MAX_RPM -> entry.getDoubleValue().ifPresent(v -> dto.setMaxRpm(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_MAX_TORQUE_FT_LB -> entry.getDoubleValue().ifPresent(v -> dto.setMaxTorqueFtLb(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_MAX_ROD_LOAD_LB -> entry.getDoubleValue().ifPresent(v -> dto.setMaxRodLoadLb(BigDecimal.valueOf(v)));
                case PfPcpSystemDto.ATTR_MAX_FLUID_TEMP_F -> entry.getDoubleValue().ifPresent(v -> dto.setMaxFluidTempF(BigDecimal.valueOf(v)));
            }
        }
    }

    private PfPcpSystemDto mapAssetToDto(Asset asset) {
        PfPcpSystemDto dto = new PfPcpSystemDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setCreatedTime(asset.getCreatedTime());
        loadPcpAttributes(dto);
        return dto;
    }

    /**
     * Record for PCP health status.
     */
    public record PcpHealthStatus(UUID assetId, int healthScore, String status, Integer runLifeDays) {}
}
