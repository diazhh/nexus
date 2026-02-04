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
import org.thingsboard.nexus.pf.dto.PfRodPumpSystemDto;
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
 * Service for managing Rod Pump (Sucker Rod Pump / Beam Pump) System entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfRodPumpSystemService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;
    private final PfAlarmService alarmService;

    /**
     * Creates a new Rod Pump System for a well.
     */
    public PfRodPumpSystemDto createRodPumpSystem(UUID tenantId, PfRodPumpSystemDto dto) {
        log.info("Creating Rod Pump system for well: {}", dto.getWellId());

        // Validate well exists
        if (!pfAssetService.existsById(dto.getWellId())) {
            throw new PfEntityNotFoundException("Well", dto.getWellId());
        }

        String assetName = String.format("RP-%s-%s",
                dto.getPumpingUnitType() != null ? dto.getPumpingUnitType().name().substring(0, 4) : "CONV",
                dto.getPumpSerialNumber() != null ? dto.getPumpSerialNumber() : dto.getWellId().toString().substring(0, 8));

        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfRodPumpSystemDto.ASSET_TYPE,
                assetName,
                "Rod Pump System"
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Calculate run life if installation date is set
        if (dto.getInstallationDate() != null) {
            int runLife = (int) ChronoUnit.DAYS.between(dto.getInstallationDate(), LocalDate.now());
            dto.setRunLifeDays(runLife);
        }

        saveRodPumpAttributes(dto);

        // Create relation to well
        pfHierarchyService.createInstalledInRelation(tenantId, dto.getAssetId(), dto.getWellId());

        // Create default operational limits
        createDefaultOperationalLimits(dto);

        log.info("Rod Pump system created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Rod Pump System by ID.
     */
    public Optional<PfRodPumpSystemDto> getRodPumpSystemById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfRodPumpSystemDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfRodPumpSystemDto dto = new PfRodPumpSystemDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadRodPumpAttributes(dto);

                    // Update run life
                    if (dto.getInstallationDate() != null) {
                        int runLife = (int) ChronoUnit.DAYS.between(dto.getInstallationDate(), LocalDate.now());
                        dto.setRunLifeDays(runLife);
                    }

                    return dto;
                });
    }

    /**
     * Gets Rod Pump System by Well ID.
     */
    public Optional<PfRodPumpSystemDto> getRodPumpSystemByWell(UUID tenantId, UUID wellId) {
        List<UUID> equipmentIds = pfHierarchyService.getEquipmentInWell(tenantId, wellId);

        for (UUID equipmentId : equipmentIds) {
            Optional<PfRodPumpSystemDto> rodPump = getRodPumpSystemById(equipmentId);
            if (rodPump.isPresent()) {
                return rodPump;
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all Rod Pump Systems for a tenant.
     */
    public Page<PfRodPumpSystemDto> getAllRodPumpSystems(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfRodPumpSystemDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Updates a Rod Pump System.
     */
    public PfRodPumpSystemDto updateRodPumpSystem(PfRodPumpSystemDto dto) {
        log.info("Updating Rod Pump system: {}", dto.getAssetId());

        dto.setUpdatedTime(System.currentTimeMillis());
        saveRodPumpAttributes(dto);

        return dto;
    }

    /**
     * Records a dynamometer survey date.
     */
    public void recordDynamometerSurvey(UUID assetId, LocalDate surveyDate) {
        log.info("Recording Dynamometer survey for {}", assetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfRodPumpSystemDto.ATTR_LAST_DYNAMOMETER_DATE, surveyDate.toString());
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Calculates pump fillage from dynamometer card analysis.
     * This is a simplified estimation - real analysis requires full dyno card data.
     */
    public PumpFillageResult estimatePumpFillage(UUID assetId, BigDecimal peakLoad, BigDecimal minLoad,
                                                  BigDecimal theoreticalFluidLoad) {
        return getRodPumpSystemById(assetId)
                .map(rp -> {
                    // Simplified pump fillage calculation
                    // Actual fillage = (Peak Load - Min Load) / Theoretical Fluid Load * 100
                    BigDecimal loadDiff = peakLoad.subtract(minLoad);
                    BigDecimal fillage = loadDiff.divide(theoreticalFluidLoad, 2, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    String status;
                    if (fillage.compareTo(BigDecimal.valueOf(85)) >= 0) {
                        status = "PUMP_OFF"; // Pump-off condition
                    } else if (fillage.compareTo(BigDecimal.valueOf(70)) >= 0) {
                        status = "GOOD";
                    } else if (fillage.compareTo(BigDecimal.valueOf(50)) >= 0) {
                        status = "FAIR";
                    } else {
                        status = "GAS_INTERFERENCE"; // Likely gas interference
                    }

                    return new PumpFillageResult(assetId, fillage, status);
                })
                .orElse(new PumpFillageResult(assetId, BigDecimal.ZERO, "UNKNOWN"));
    }

    /**
     * Gets Rod Pump health status based on operational parameters.
     */
    public RodPumpHealthStatus getHealthStatus(UUID assetId) {
        return getRodPumpSystemById(assetId)
                .map(rp -> {
                    int score = 100;
                    String status = "GOOD";

                    // Check run life
                    if (rp.getRunLifeDays() != null) {
                        if (rp.getRunLifeDays() > 730) { // > 2 years
                            score -= 15;
                            status = "ATTENTION";
                        } else if (rp.getRunLifeDays() > 365) { // > 1 year
                            score -= 5;
                        }
                    }

                    // Check days since last dynamometer
                    if (rp.getLastDynamometerDate() != null) {
                        long daysSinceDyno = ChronoUnit.DAYS.between(rp.getLastDynamometerDate(), LocalDate.now());
                        if (daysSinceDyno > 180) { // > 6 months
                            score -= 10;
                        }
                        if (daysSinceDyno > 365) { // > 1 year
                            score -= 10;
                            status = "NEEDS_SURVEY";
                        }
                    }

                    return new RodPumpHealthStatus(assetId, score, status, rp.getRunLifeDays());
                })
                .orElse(new RodPumpHealthStatus(assetId, 0, "UNKNOWN", null));
    }

    /**
     * Deletes a Rod Pump System.
     */
    public void deleteRodPumpSystem(UUID tenantId, UUID assetId) {
        log.warn("Deleting Rod Pump system: {}", assetId);
        pfHierarchyService.removeAllRelations(tenantId, assetId);
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    // Helper methods

    private void createDefaultOperationalLimits(PfRodPumpSystemDto dto) {
        // Create operational limits for rod pump
        if (dto.getMaxSpm() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfRodPumpSystemDto.ASSET_TYPE)
                    .variableKey("spm")
                    .highHighLimit(dto.getMaxSpm())
                    .highLimit(dto.getMaxSpm().multiply(BigDecimal.valueOf(0.95)))
                    .lowLimit(dto.getMinSpm())
                    .lowLowLimit(dto.getMinSpm() != null ?
                            dto.getMinSpm().multiply(BigDecimal.valueOf(0.8)) : null)
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxPeakLoadLb() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfRodPumpSystemDto.ASSET_TYPE)
                    .variableKey("peak_load_lb")
                    .highHighLimit(dto.getMaxPeakLoadLb())
                    .highLimit(dto.getMaxPeakLoadLb().multiply(BigDecimal.valueOf(0.9)))
                    .enabled(true)
                    .build());
        }

        if (dto.getMinLoadLb() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfRodPumpSystemDto.ASSET_TYPE)
                    .variableKey("min_load_lb")
                    .lowLimit(dto.getMinLoadLb().multiply(BigDecimal.valueOf(1.1)))
                    .lowLowLimit(dto.getMinLoadLb())
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxTorqueInLb() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfRodPumpSystemDto.ASSET_TYPE)
                    .variableKey("torque_in_lb")
                    .highHighLimit(dto.getMaxTorqueInLb())
                    .highLimit(dto.getMaxTorqueInLb().multiply(BigDecimal.valueOf(0.85)))
                    .enabled(true)
                    .build());
        }

        if (dto.getMaxMotorAmps() != null) {
            alarmService.saveOperationalLimit(OperationalLimitDto.builder()
                    .entityId(dto.getAssetId())
                    .entityType(PfRodPumpSystemDto.ASSET_TYPE)
                    .variableKey("motor_amps")
                    .highHighLimit(dto.getMaxMotorAmps())
                    .highLimit(dto.getMaxMotorAmps().multiply(BigDecimal.valueOf(0.9)))
                    .enabled(true)
                    .build());
        }
    }

    private void saveRodPumpAttributes(PfRodPumpSystemDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getWellId() != null) attrs.put("well_id", dto.getWellId().toString());
        if (dto.getPumpingUnitType() != null) attrs.put(PfRodPumpSystemDto.ATTR_PUMPING_UNIT_TYPE, dto.getPumpingUnitType().name());
        if (dto.getPumpingUnitSize() != null) attrs.put(PfRodPumpSystemDto.ATTR_PUMPING_UNIT_SIZE, dto.getPumpingUnitSize());
        if (dto.getPumpType() != null) attrs.put(PfRodPumpSystemDto.ATTR_PUMP_TYPE, dto.getPumpType());
        if (dto.getPumpBoreIn() != null) attrs.put(PfRodPumpSystemDto.ATTR_PUMP_BORE_IN, dto.getPumpBoreIn());
        if (dto.getPumpSerialNumber() != null) attrs.put(PfRodPumpSystemDto.ATTR_PUMP_SERIAL_NUMBER, dto.getPumpSerialNumber());
        if (dto.getStrokeLengthIn() != null) attrs.put(PfRodPumpSystemDto.ATTR_STROKE_LENGTH_IN, dto.getStrokeLengthIn());
        if (dto.getSpm() != null) attrs.put(PfRodPumpSystemDto.ATTR_SPM, dto.getSpm());
        if (dto.getMotorHp() != null) attrs.put(PfRodPumpSystemDto.ATTR_MOTOR_HP, dto.getMotorHp());
        if (dto.getGearboxRating() != null) attrs.put(PfRodPumpSystemDto.ATTR_GEARBOX_RATING, dto.getGearboxRating());
        if (dto.getRodStringDesign() != null) attrs.put(PfRodPumpSystemDto.ATTR_ROD_STRING_DESIGN, dto.getRodStringDesign());
        if (dto.getSettingDepthFt() != null) attrs.put(PfRodPumpSystemDto.ATTR_SETTING_DEPTH_FT, dto.getSettingDepthFt());
        if (dto.getTubingSizeIn() != null) attrs.put(PfRodPumpSystemDto.ATTR_TUBING_SIZE_IN, dto.getTubingSizeIn());
        if (dto.getTubingAnchor() != null) attrs.put(PfRodPumpSystemDto.ATTR_TUBING_ANCHOR, dto.getTubingAnchor());
        if (dto.getCounterbalanceType() != null) attrs.put(PfRodPumpSystemDto.ATTR_COUNTERBALANCE_TYPE, dto.getCounterbalanceType());
        if (dto.getInstallationDate() != null) attrs.put(PfRodPumpSystemDto.ATTR_INSTALLATION_DATE, dto.getInstallationDate().toString());
        if (dto.getLastDynamometerDate() != null) attrs.put(PfRodPumpSystemDto.ATTR_LAST_DYNAMOMETER_DATE, dto.getLastDynamometerDate().toString());
        if (dto.getRunLifeDays() != null) attrs.put(PfRodPumpSystemDto.ATTR_RUN_LIFE_DAYS, dto.getRunLifeDays());

        // Operational limits
        if (dto.getMinSpm() != null) attrs.put(PfRodPumpSystemDto.ATTR_MIN_SPM, dto.getMinSpm());
        if (dto.getMaxSpm() != null) attrs.put(PfRodPumpSystemDto.ATTR_MAX_SPM, dto.getMaxSpm());
        if (dto.getMaxPeakLoadLb() != null) attrs.put(PfRodPumpSystemDto.ATTR_MAX_PEAK_LOAD_LB, dto.getMaxPeakLoadLb());
        if (dto.getMinLoadLb() != null) attrs.put(PfRodPumpSystemDto.ATTR_MIN_LOAD_LB, dto.getMinLoadLb());
        if (dto.getMaxTorqueInLb() != null) attrs.put(PfRodPumpSystemDto.ATTR_MAX_TORQUE_IN_LB, dto.getMaxTorqueInLb());
        if (dto.getMaxMotorAmps() != null) attrs.put(PfRodPumpSystemDto.ATTR_MAX_MOTOR_AMPS, dto.getMaxMotorAmps());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadRodPumpAttributes(PfRodPumpSystemDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case "well_id" -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setWellId(UUID.fromString(strValue));
                    }
                }
                case PfRodPumpSystemDto.ATTR_PUMPING_UNIT_TYPE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setPumpingUnitType(PfRodPumpSystemDto.PumpingUnitType.valueOf(strValue));
                    }
                }
                case PfRodPumpSystemDto.ATTR_PUMPING_UNIT_SIZE -> dto.setPumpingUnitSize(strValue);
                case PfRodPumpSystemDto.ATTR_PUMP_TYPE -> dto.setPumpType(strValue);
                case PfRodPumpSystemDto.ATTR_PUMP_BORE_IN -> entry.getDoubleValue().ifPresent(v -> dto.setPumpBoreIn(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_PUMP_SERIAL_NUMBER -> dto.setPumpSerialNumber(strValue);
                case PfRodPumpSystemDto.ATTR_STROKE_LENGTH_IN -> entry.getDoubleValue().ifPresent(v -> dto.setStrokeLengthIn(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_SPM -> entry.getDoubleValue().ifPresent(v -> dto.setSpm(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_MOTOR_HP -> entry.getDoubleValue().ifPresent(v -> dto.setMotorHp(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_GEARBOX_RATING -> entry.getDoubleValue().ifPresent(v -> dto.setGearboxRating(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_ROD_STRING_DESIGN -> dto.setRodStringDesign(strValue);
                case PfRodPumpSystemDto.ATTR_SETTING_DEPTH_FT -> entry.getDoubleValue().ifPresent(v -> dto.setSettingDepthFt(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_TUBING_SIZE_IN -> entry.getDoubleValue().ifPresent(v -> dto.setTubingSizeIn(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_TUBING_ANCHOR -> entry.getBooleanValue().ifPresent(dto::setTubingAnchor);
                case PfRodPumpSystemDto.ATTR_COUNTERBALANCE_TYPE -> dto.setCounterbalanceType(strValue);
                case PfRodPumpSystemDto.ATTR_INSTALLATION_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setInstallationDate(LocalDate.parse(strValue));
                    }
                }
                case PfRodPumpSystemDto.ATTR_LAST_DYNAMOMETER_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setLastDynamometerDate(LocalDate.parse(strValue));
                    }
                }
                case PfRodPumpSystemDto.ATTR_RUN_LIFE_DAYS -> entry.getLongValue().ifPresent(v -> dto.setRunLifeDays(v.intValue()));
                case PfRodPumpSystemDto.ATTR_MIN_SPM -> entry.getDoubleValue().ifPresent(v -> dto.setMinSpm(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_MAX_SPM -> entry.getDoubleValue().ifPresent(v -> dto.setMaxSpm(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_MAX_PEAK_LOAD_LB -> entry.getDoubleValue().ifPresent(v -> dto.setMaxPeakLoadLb(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_MIN_LOAD_LB -> entry.getDoubleValue().ifPresent(v -> dto.setMinLoadLb(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_MAX_TORQUE_IN_LB -> entry.getDoubleValue().ifPresent(v -> dto.setMaxTorqueInLb(BigDecimal.valueOf(v)));
                case PfRodPumpSystemDto.ATTR_MAX_MOTOR_AMPS -> entry.getDoubleValue().ifPresent(v -> dto.setMaxMotorAmps(BigDecimal.valueOf(v)));
            }
        }
    }

    private PfRodPumpSystemDto mapAssetToDto(Asset asset) {
        PfRodPumpSystemDto dto = new PfRodPumpSystemDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setCreatedTime(asset.getCreatedTime());
        loadRodPumpAttributes(dto);
        return dto;
    }

    /**
     * Record for Rod Pump health status.
     */
    public record RodPumpHealthStatus(UUID assetId, int healthScore, String status, Integer runLifeDays) {}

    /**
     * Record for Pump Fillage result.
     */
    public record PumpFillageResult(UUID assetId, BigDecimal fillagePercent, String status) {}
}
