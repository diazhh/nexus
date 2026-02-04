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
import org.thingsboard.nexus.pf.dto.PfEspSystemDto;
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
 * Service for managing ESP (Electric Submersible Pump) System entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfEspSystemService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;
    private final PfAlarmService alarmService;

    /**
     * Creates a new ESP System for a well.
     */
    public PfEspSystemDto createEspSystem(UUID tenantId, PfEspSystemDto dto) {
        log.info("Creating ESP system for well: {}", dto.getWellId());

        // Validate well exists
        if (!pfAssetService.existsById(dto.getWellId())) {
            throw new PfEntityNotFoundException("Well", dto.getWellId());
        }

        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfEspSystemDto.ASSET_TYPE,
                "ESP-" + dto.getPumpSerialNumber(),
                "ESP System"
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Calculate run life if installation date is set
        if (dto.getInstallationDate() != null) {
            int runLife = (int) ChronoUnit.DAYS.between(dto.getInstallationDate(), LocalDate.now());
            dto.setRunLifeDays(runLife);
        }

        saveEspAttributes(dto);

        // Create relation to well
        pfHierarchyService.createInstalledInRelation(tenantId, dto.getAssetId(), dto.getWellId());

        // Create default operational limits
        createDefaultOperationalLimits(dto);

        log.info("ESP system created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets an ESP System by ID.
     */
    public Optional<PfEspSystemDto> getEspSystemById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfEspSystemDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfEspSystemDto dto = new PfEspSystemDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadEspAttributes(dto);

                    // Update run life
                    if (dto.getInstallationDate() != null) {
                        int runLife = (int) ChronoUnit.DAYS.between(dto.getInstallationDate(), LocalDate.now());
                        dto.setRunLifeDays(runLife);
                    }

                    return dto;
                });
    }

    /**
     * Gets ESP System by Well ID.
     */
    public Optional<PfEspSystemDto> getEspSystemByWell(UUID tenantId, UUID wellId) {
        List<UUID> equipmentIds = pfHierarchyService.getEquipmentInWell(tenantId, wellId);

        for (UUID equipmentId : equipmentIds) {
            Optional<PfEspSystemDto> esp = getEspSystemById(equipmentId);
            if (esp.isPresent()) {
                return esp;
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all ESP Systems for a tenant.
     */
    public Page<PfEspSystemDto> getAllEspSystems(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfEspSystemDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Updates an ESP System.
     */
    public PfEspSystemDto updateEspSystem(PfEspSystemDto dto) {
        log.info("Updating ESP system: {}", dto.getAssetId());

        dto.setUpdatedTime(System.currentTimeMillis());
        saveEspAttributes(dto);

        return dto;
    }

    /**
     * Records a pulling event (when ESP is pulled from well for maintenance).
     */
    public void recordPulling(UUID assetId, LocalDate pullingDate) {
        log.info("Recording ESP pulling for {}", assetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfEspSystemDto.ATTR_LAST_PULLING_DATE, pullingDate.toString());
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Gets ESP health status based on operational parameters.
     */
    public EspHealthStatus getHealthStatus(UUID assetId) {
        return getEspSystemById(assetId)
                .map(esp -> {
                    int score = 100;
                    String status = "GOOD";

                    // Check run life
                    if (esp.getRunLifeDays() != null && esp.getRunLifeDays() > 365) {
                        score -= 10;
                    }
                    if (esp.getRunLifeDays() != null && esp.getRunLifeDays() > 730) {
                        score -= 20;
                        status = "ATTENTION";
                    }

                    return new EspHealthStatus(assetId, score, status, esp.getRunLifeDays());
                })
                .orElse(new EspHealthStatus(assetId, 0, "UNKNOWN", null));
    }

    /**
     * Deletes an ESP System.
     */
    public void deleteEspSystem(UUID tenantId, UUID assetId) {
        log.warn("Deleting ESP system: {}", assetId);
        pfHierarchyService.removeAllRelations(tenantId, assetId);
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    // Helper methods

    private void createDefaultOperationalLimits(PfEspSystemDto dto) {
        // Create operational limits based on ESP specs
        if (dto.getMaxFrequencyHz() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfEspSystemDto.ASSET_TYPE)
                    .variableKey("frequency_hz")
                    .highHighLimit(dto.getMaxFrequencyHz())
                    .highLimit(dto.getMaxFrequencyHz().multiply(BigDecimal.valueOf(0.95)))
                    .lowLimit(dto.getMinFrequencyHz())
                    .lowLowLimit(dto.getMinFrequencyHz() != null ?
                            dto.getMinFrequencyHz().multiply(BigDecimal.valueOf(0.9)) : null)
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxCurrentAmps() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfEspSystemDto.ASSET_TYPE)
                    .variableKey("current_amps")
                    .highHighLimit(dto.getMaxCurrentAmps())
                    .highLimit(dto.getMaxCurrentAmps().multiply(BigDecimal.valueOf(0.9)))
                    .lowLimit(dto.getMinCurrentAmps())
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxMotorTempF() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfEspSystemDto.ASSET_TYPE)
                    .variableKey("temperature_motor_f")
                    .highHighLimit(dto.getMaxMotorTempF())
                    .highLimit(dto.getMaxMotorTempF().multiply(BigDecimal.valueOf(0.9)))
                    .enabled(true)
                    .build());
        }

        if (dto.getMinPipPsi() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfEspSystemDto.ASSET_TYPE)
                    .variableKey("pip_psi")
                    .lowLimit(dto.getMinPipPsi().multiply(BigDecimal.valueOf(1.1)))
                    .lowLowLimit(dto.getMinPipPsi())
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxVibrationG() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfEspSystemDto.ASSET_TYPE)
                    .variableKey("vibration_g")
                    .highLimit(dto.getMaxVibrationG().multiply(BigDecimal.valueOf(0.8)))
                    .highHighLimit(dto.getMaxVibrationG())
                    .enabled(true)
                    .build());
        }
    }

    private void saveEspAttributes(PfEspSystemDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getWellId() != null) attrs.put("well_id", dto.getWellId().toString());
        if (dto.getPumpModel() != null) attrs.put(PfEspSystemDto.ATTR_PUMP_MODEL, dto.getPumpModel());
        if (dto.getPumpSerialNumber() != null) attrs.put(PfEspSystemDto.ATTR_PUMP_SERIAL_NUMBER, dto.getPumpSerialNumber());
        if (dto.getStages() != null) attrs.put(PfEspSystemDto.ATTR_STAGES, dto.getStages());
        if (dto.getRatedHeadFt() != null) attrs.put(PfEspSystemDto.ATTR_RATED_HEAD_FT, dto.getRatedHeadFt());
        if (dto.getRatedFlowBpd() != null) attrs.put(PfEspSystemDto.ATTR_RATED_FLOW_BPD, dto.getRatedFlowBpd());
        if (dto.getMotorHp() != null) attrs.put(PfEspSystemDto.ATTR_MOTOR_HP, dto.getMotorHp());
        if (dto.getMotorVoltage() != null) attrs.put(PfEspSystemDto.ATTR_MOTOR_VOLTAGE, dto.getMotorVoltage());
        if (dto.getFrequencyHz() != null) attrs.put(PfEspSystemDto.ATTR_FREQUENCY_HZ, dto.getFrequencyHz());
        if (dto.getSettingDepthFt() != null) attrs.put(PfEspSystemDto.ATTR_SETTING_DEPTH_FT, dto.getSettingDepthFt());
        if (dto.getInstallationDate() != null) attrs.put(PfEspSystemDto.ATTR_INSTALLATION_DATE, dto.getInstallationDate().toString());
        if (dto.getLastPullingDate() != null) attrs.put(PfEspSystemDto.ATTR_LAST_PULLING_DATE, dto.getLastPullingDate().toString());
        if (dto.getRunLifeDays() != null) attrs.put(PfEspSystemDto.ATTR_RUN_LIFE_DAYS, dto.getRunLifeDays());

        // Operational limits
        if (dto.getMinFrequencyHz() != null) attrs.put(PfEspSystemDto.ATTR_MIN_FREQUENCY_HZ, dto.getMinFrequencyHz());
        if (dto.getMaxFrequencyHz() != null) attrs.put(PfEspSystemDto.ATTR_MAX_FREQUENCY_HZ, dto.getMaxFrequencyHz());
        if (dto.getMinCurrentAmps() != null) attrs.put(PfEspSystemDto.ATTR_MIN_CURRENT_AMPS, dto.getMinCurrentAmps());
        if (dto.getMaxCurrentAmps() != null) attrs.put(PfEspSystemDto.ATTR_MAX_CURRENT_AMPS, dto.getMaxCurrentAmps());
        if (dto.getMaxMotorTempF() != null) attrs.put(PfEspSystemDto.ATTR_MAX_MOTOR_TEMP_F, dto.getMaxMotorTempF());
        if (dto.getMinPipPsi() != null) attrs.put(PfEspSystemDto.ATTR_MIN_PIP_PSI, dto.getMinPipPsi());
        if (dto.getMaxVibrationG() != null) attrs.put(PfEspSystemDto.ATTR_MAX_VIBRATION_G, dto.getMaxVibrationG());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadEspAttributes(PfEspSystemDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case "well_id" -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setWellId(UUID.fromString(strValue));
                    }
                }
                case PfEspSystemDto.ATTR_PUMP_MODEL -> dto.setPumpModel(strValue);
                case PfEspSystemDto.ATTR_PUMP_SERIAL_NUMBER -> dto.setPumpSerialNumber(strValue);
                case PfEspSystemDto.ATTR_STAGES -> entry.getLongValue().ifPresent(v -> dto.setStages(v.intValue()));
                case PfEspSystemDto.ATTR_RATED_HEAD_FT -> entry.getDoubleValue().ifPresent(v -> dto.setRatedHeadFt(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_RATED_FLOW_BPD -> entry.getDoubleValue().ifPresent(v -> dto.setRatedFlowBpd(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MOTOR_HP -> entry.getDoubleValue().ifPresent(v -> dto.setMotorHp(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MOTOR_VOLTAGE -> entry.getLongValue().ifPresent(v -> dto.setMotorVoltage(v.intValue()));
                case PfEspSystemDto.ATTR_FREQUENCY_HZ -> entry.getDoubleValue().ifPresent(v -> dto.setFrequencyHz(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_SETTING_DEPTH_FT -> entry.getDoubleValue().ifPresent(v -> dto.setSettingDepthFt(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_INSTALLATION_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setInstallationDate(LocalDate.parse(strValue));
                    }
                }
                case PfEspSystemDto.ATTR_LAST_PULLING_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setLastPullingDate(LocalDate.parse(strValue));
                    }
                }
                case PfEspSystemDto.ATTR_RUN_LIFE_DAYS -> entry.getLongValue().ifPresent(v -> dto.setRunLifeDays(v.intValue()));
                case PfEspSystemDto.ATTR_MIN_FREQUENCY_HZ -> entry.getDoubleValue().ifPresent(v -> dto.setMinFrequencyHz(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MAX_FREQUENCY_HZ -> entry.getDoubleValue().ifPresent(v -> dto.setMaxFrequencyHz(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MIN_CURRENT_AMPS -> entry.getDoubleValue().ifPresent(v -> dto.setMinCurrentAmps(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MAX_CURRENT_AMPS -> entry.getDoubleValue().ifPresent(v -> dto.setMaxCurrentAmps(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MAX_MOTOR_TEMP_F -> entry.getDoubleValue().ifPresent(v -> dto.setMaxMotorTempF(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MIN_PIP_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setMinPipPsi(BigDecimal.valueOf(v)));
                case PfEspSystemDto.ATTR_MAX_VIBRATION_G -> entry.getDoubleValue().ifPresent(v -> dto.setMaxVibrationG(BigDecimal.valueOf(v)));
            }
        }
    }

    private PfEspSystemDto mapAssetToDto(Asset asset) {
        PfEspSystemDto dto = new PfEspSystemDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setCreatedTime(asset.getCreatedTime());
        loadEspAttributes(dto);
        return dto;
    }

    /**
     * Record for ESP health status.
     */
    public record EspHealthStatus(UUID assetId, int healthScore, String status, Integer runLifeDays) {}
}
