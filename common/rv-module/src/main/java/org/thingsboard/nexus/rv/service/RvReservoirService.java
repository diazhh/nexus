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
import org.thingsboard.nexus.rv.dto.RvReservoirDto;
import org.thingsboard.nexus.rv.exception.RvBusinessException;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Reservoir (Yacimiento) entities.
 * Provides CRUD operations, OOIP calculations, and PVT integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvReservoirService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;
    private final RvCalculationService calculationService;

    /**
     * Creates a new Reservoir under a Field.
     */
    public RvReservoirDto createReservoir(UUID tenantId, RvReservoirDto dto) {
        log.info("Creating reservoir: {} under field: {}", dto.getName(), dto.getFieldAssetId());

        // Validate field exists
        if (dto.getFieldAssetId() != null) {
            if (!rvAssetService.existsById(dto.getFieldAssetId())) {
                throw new RvEntityNotFoundException("Field", dto.getFieldAssetId());
            }
        }

        // Create ThingsBoard Asset
        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvReservoirDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveReservoirAttributes(dto);

        // Create hierarchy relation
        if (dto.getFieldAssetId() != null) {
            rvHierarchyService.setParentChild(tenantId, dto.getFieldAssetId(), dto.getAssetId());
        }

        log.info("Reservoir created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Reservoir by ID.
     */
    public Optional<RvReservoirDto> getReservoirById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvReservoirDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvReservoirDto dto = new RvReservoirDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadReservoirAttributes(dto);

                // Get parent field
                UUID fieldId = rvHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                dto.setFieldAssetId(fieldId);

                return dto;
            });
    }

    /**
     * Gets all Reservoirs for a tenant.
     */
    public Page<RvReservoirDto> getAllReservoirs(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvReservoirDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Reservoirs by Field.
     */
    public List<RvReservoirDto> getReservoirsByField(UUID tenantId, UUID fieldAssetId) {
        List<UUID> reservoirIds = rvHierarchyService.getChildren(tenantId, fieldAssetId);
        List<RvReservoirDto> reservoirs = new ArrayList<>();

        for (UUID reservoirId : reservoirIds) {
            getReservoirById(reservoirId).ifPresent(reservoirs::add);
        }

        return reservoirs;
    }

    /**
     * Updates a Reservoir.
     */
    public RvReservoirDto updateReservoir(RvReservoirDto dto) {
        log.info("Updating reservoir: {}", dto.getAssetId());

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
        saveReservoirAttributes(dto);

        return dto;
    }

    /**
     * Deletes a Reservoir.
     */
    public void deleteReservoir(UUID tenantId, UUID assetId) {
        log.warn("Deleting reservoir: {}", assetId);

        List<UUID> children = rvHierarchyService.getChildren(tenantId, assetId);
        if (!children.isEmpty()) {
            throw new RvBusinessException(RvBusinessException.INVALID_HIERARCHY,
                "Cannot delete reservoir with " + children.size() + " associated zones/wells");
        }

        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Calculates OOIP for a reservoir using volumetric method.
     */
    public BigDecimal calculateOOIP(UUID assetId) {
        RvReservoirDto reservoir = getReservoirById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("Reservoir", assetId));

        // Validate required data
        if (reservoir.getAreaKm2() == null || reservoir.getNetPayThicknessM() == null ||
            reservoir.getAveragePorosityFrac() == null || reservoir.getAverageSwFrac() == null ||
            reservoir.getFormationVolFactorBo() == null) {
            throw new RvBusinessException(RvBusinessException.INSUFFICIENT_DATA,
                "Missing required data for OOIP calculation");
        }

        // Convert km² to acres (1 km² = 247.105 acres)
        BigDecimal areaAcres = reservoir.getAreaKm2().multiply(new BigDecimal("247.105"));

        BigDecimal ooip = calculationService.calculateOOIP(
            areaAcres,
            reservoir.getNetPayThicknessM(),
            reservoir.getAveragePorosityFrac(),
            reservoir.getAverageSwFrac(),
            reservoir.getFormationVolFactorBo()
        );

        // Update reservoir with calculated OOIP
        reservoir.setOoipStb(ooip);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(RvReservoirDto.ATTR_OOIP_STB, ooip);
        rvAttributeService.saveServerAttributes(assetId, attrs);

        log.info("OOIP calculated for reservoir {}: {} STB", assetId, ooip);
        return ooip;
    }

    /**
     * Calculates recoverable reserves based on OOIP and recovery factor.
     */
    public BigDecimal calculateRecoverableReserves(UUID assetId, BigDecimal recoveryFactor) {
        RvReservoirDto reservoir = getReservoirById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("Reservoir", assetId));

        BigDecimal ooip = reservoir.getOoipStb();
        if (ooip == null) {
            ooip = calculateOOIP(assetId);
        }

        BigDecimal reserves = ooip.multiply(recoveryFactor);

        // Update reservoir
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(RvReservoirDto.ATTR_RECOVERY_FACTOR, recoveryFactor);
        attrs.put("recoverable_reserves_stb", reserves);
        rvAttributeService.saveServerAttributes(assetId, attrs);

        log.info("Recoverable reserves for reservoir {}: {} STB (RF={})", assetId, reserves, recoveryFactor);
        return reserves;
    }

    /**
     * Associates a PVT Study with a Reservoir.
     */
    public void associatePvtStudy(UUID tenantId, UUID reservoirAssetId, UUID pvtStudyAssetId) {
        log.info("Associating PVT study {} with reservoir {}", pvtStudyAssetId, reservoirAssetId);
        rvHierarchyService.createCharacterizedByRelation(tenantId, reservoirAssetId, pvtStudyAssetId);
    }

    private void saveReservoirAttributes(RvReservoirDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCode() != null) attrs.put(RvReservoirDto.ATTR_CODE, dto.getCode());
        if (dto.getFormationName() != null) attrs.put(RvReservoirDto.ATTR_FORMATION_NAME, dto.getFormationName());
        if (dto.getGeologicAge() != null) attrs.put(RvReservoirDto.ATTR_GEOLOGIC_AGE, dto.getGeologicAge());
        if (dto.getAveragePorosityFrac() != null) attrs.put(RvReservoirDto.ATTR_AVG_POROSITY_FRAC, dto.getAveragePorosityFrac());
        if (dto.getAveragePermeabilityMd() != null) attrs.put(RvReservoirDto.ATTR_AVG_PERMEABILITY_MD, dto.getAveragePermeabilityMd());
        if (dto.getNetPayThicknessM() != null) attrs.put(RvReservoirDto.ATTR_NET_PAY_THICKNESS_M, dto.getNetPayThicknessM());
        if (dto.getInitialPressurePsi() != null) attrs.put(RvReservoirDto.ATTR_INITIAL_PRESSURE_PSI, dto.getInitialPressurePsi());
        if (dto.getCurrentPressurePsi() != null) attrs.put(RvReservoirDto.ATTR_CURRENT_PRESSURE_PSI, dto.getCurrentPressurePsi());
        if (dto.getBubblePointPressurePsi() != null) attrs.put(RvReservoirDto.ATTR_BUBBLE_POINT_PSI, dto.getBubblePointPressurePsi());
        if (dto.getTemperatureF() != null) attrs.put(RvReservoirDto.ATTR_TEMPERATURE_F, dto.getTemperatureF());
        if (dto.getFluidType() != null) attrs.put(RvReservoirDto.ATTR_FLUID_TYPE, dto.getFluidType());
        if (dto.getApiGravity() != null) attrs.put(RvReservoirDto.ATTR_API_GRAVITY, dto.getApiGravity());
        if (dto.getOoipStb() != null) attrs.put(RvReservoirDto.ATTR_OOIP_STB, dto.getOoipStb());
        if (dto.getRecoveryFactorFrac() != null) attrs.put(RvReservoirDto.ATTR_RECOVERY_FACTOR, dto.getRecoveryFactorFrac());
        if (dto.getPrimaryDriveMechanism() != null) attrs.put(RvReservoirDto.ATTR_PRIMARY_DRIVE, dto.getPrimaryDriveMechanism());
        if (dto.getIsFoamyOil() != null) attrs.put(RvReservoirDto.ATTR_IS_FOAMY_OIL, dto.getIsFoamyOil());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadReservoirAttributes(RvReservoirDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvReservoirDto.ATTR_CODE -> dto.setCode(entry.getValueAsString());
                case RvReservoirDto.ATTR_FORMATION_NAME -> dto.setFormationName(entry.getValueAsString());
                case RvReservoirDto.ATTR_GEOLOGIC_AGE -> dto.setGeologicAge(entry.getValueAsString());
                case RvReservoirDto.ATTR_AVG_POROSITY_FRAC -> entry.getDoubleValue().ifPresent(v -> dto.setAveragePorosityFrac(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_AVG_PERMEABILITY_MD -> entry.getDoubleValue().ifPresent(v -> dto.setAveragePermeabilityMd(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_NET_PAY_THICKNESS_M -> entry.getDoubleValue().ifPresent(v -> dto.setNetPayThicknessM(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_INITIAL_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setInitialPressurePsi(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_CURRENT_PRESSURE_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setCurrentPressurePsi(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_BUBBLE_POINT_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setBubblePointPressurePsi(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_TEMPERATURE_F -> entry.getDoubleValue().ifPresent(v -> dto.setTemperatureF(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_FLUID_TYPE -> dto.setFluidType(entry.getValueAsString());
                case RvReservoirDto.ATTR_API_GRAVITY -> entry.getDoubleValue().ifPresent(v -> dto.setApiGravity(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_OOIP_STB -> entry.getDoubleValue().ifPresent(v -> dto.setOoipStb(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_RECOVERY_FACTOR -> entry.getDoubleValue().ifPresent(v -> dto.setRecoveryFactorFrac(BigDecimal.valueOf(v)));
                case RvReservoirDto.ATTR_PRIMARY_DRIVE -> dto.setPrimaryDriveMechanism(entry.getValueAsString());
                case RvReservoirDto.ATTR_IS_FOAMY_OIL -> entry.getBooleanValue().ifPresent(dto::setIsFoamyOil);
            }
        }
    }

    private RvReservoirDto mapAssetToDto(Asset asset) {
        RvReservoirDto dto = new RvReservoirDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadReservoirAttributes(dto);
        return dto;
    }
}
