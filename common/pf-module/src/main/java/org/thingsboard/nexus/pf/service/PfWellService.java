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
import org.thingsboard.nexus.pf.dto.LiftSystemType;
import org.thingsboard.nexus.pf.dto.PfWellDto;
import org.thingsboard.nexus.pf.dto.WellStatus;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Production Well entities.
 * Provides CRUD operations and production monitoring capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfWellService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;

    /**
     * Creates a new Production Well.
     */
    public PfWellDto createWell(UUID tenantId, PfWellDto dto) {
        log.info("Creating well: {} in wellpad: {}", dto.getName(), dto.getWellpadId());

        // Validate wellpad exists if specified
        if (dto.getWellpadId() != null) {
            if (!pfAssetService.existsById(dto.getWellpadId())) {
                throw new PfEntityNotFoundException("Wellpad", dto.getWellpadId());
            }
        }

        // Create ThingsBoard Asset
        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfWellDto.ASSET_TYPE,
                dto.getName(),
                dto.getName()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveWellAttributes(dto);

        // Create hierarchy relations
        if (dto.getWellpadId() != null) {
            pfHierarchyService.setParentChild(tenantId, dto.getWellpadId(), dto.getAssetId());
        }

        log.info("Well created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Well by ID.
     */
    public Optional<PfWellDto> getWellById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfWellDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfWellDto dto = new PfWellDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setName(asset.getName());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadWellAttributes(dto);

                    // Get parent wellpad
                    UUID wellpadId = pfHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                    dto.setWellpadId(wellpadId);

                    return dto;
                });
    }

    /**
     * Gets all Wells for a tenant.
     */
    public Page<PfWellDto> getAllWells(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfWellDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Wells by Wellpad.
     */
    public List<PfWellDto> getWellsByWellpad(UUID tenantId, UUID wellpadId) {
        List<UUID> wellIds = pfHierarchyService.getChildren(tenantId, wellpadId);
        List<PfWellDto> wells = new ArrayList<>();

        for (UUID wellId : wellIds) {
            getWellById(wellId).ifPresent(wells::add);
        }

        return wells;
    }

    /**
     * Gets Wells by status.
     */
    public List<PfWellDto> getWellsByStatus(UUID tenantId, WellStatus status, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfWellDto.ASSET_TYPE, page, size);

        return assets.getContent().stream()
                .map(this::mapAssetToDto)
                .filter(w -> status.equals(w.getStatus()))
                .toList();
    }

    /**
     * Updates a Well.
     */
    public PfWellDto updateWell(PfWellDto dto) {
        log.info("Updating well: {}", dto.getAssetId());

        pfAssetService.getAssetById(dto.getAssetId()).ifPresent(asset -> {
            boolean needsUpdate = false;
            if (!asset.getName().equals(dto.getName())) {
                asset.setName(dto.getName());
                needsUpdate = true;
            }
            if (needsUpdate) {
                pfAssetService.updateAsset(asset);
            }
        });

        dto.setUpdatedTime(System.currentTimeMillis());
        saveWellAttributes(dto);

        return dto;
    }

    /**
     * Updates well status.
     */
    public void updateWellStatus(UUID assetId, WellStatus newStatus) {
        log.info("Updating well {} status to {}", assetId, newStatus);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfWellDto.ATTR_STATUS, newStatus.name());
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Updates current production rate.
     */
    public void updateProductionRate(UUID assetId, BigDecimal productionBpd) {
        log.debug("Updating well {} production to {} BPD", assetId, productionBpd);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfWellDto.ATTR_CURRENT_PRODUCTION_BPD, productionBpd);
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Deletes a Well.
     */
    public void deleteWell(UUID tenantId, UUID assetId) {
        log.warn("Deleting well: {}", assetId);

        // Remove relations first
        pfHierarchyService.removeAllRelations(tenantId, assetId);

        // Delete the asset
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Counts wells by status.
     */
    public Map<WellStatus, Long> countWellsByStatus(UUID tenantId) {
        Map<WellStatus, Long> counts = new HashMap<>();
        for (WellStatus status : WellStatus.values()) {
            counts.put(status, 0L);
        }

        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfWellDto.ASSET_TYPE, 0, 10000);
        for (Asset asset : assets.getContent()) {
            PfWellDto dto = mapAssetToDto(asset);
            if (dto.getStatus() != null) {
                counts.merge(dto.getStatus(), 1L, Long::sum);
            }
        }

        return counts;
    }

    // Helper methods

    private void saveWellAttributes(PfWellDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getApiNumber() != null) attrs.put(PfWellDto.ATTR_API_NUMBER, dto.getApiNumber());
        if (dto.getStatus() != null) attrs.put(PfWellDto.ATTR_STATUS, dto.getStatus().name());
        if (dto.getLiftSystemType() != null) attrs.put(PfWellDto.ATTR_LIFT_SYSTEM_TYPE, dto.getLiftSystemType().name());
        if (dto.getLatitude() != null) attrs.put(PfWellDto.ATTR_LATITUDE, dto.getLatitude());
        if (dto.getLongitude() != null) attrs.put(PfWellDto.ATTR_LONGITUDE, dto.getLongitude());
        if (dto.getMeasuredDepthFt() != null) attrs.put(PfWellDto.ATTR_MEASURED_DEPTH_FT, dto.getMeasuredDepthFt());
        if (dto.getTrueVerticalDepthFt() != null) attrs.put(PfWellDto.ATTR_TRUE_VERTICAL_DEPTH_FT, dto.getTrueVerticalDepthFt());
        if (dto.getSpudDate() != null) attrs.put(PfWellDto.ATTR_SPUD_DATE, dto.getSpudDate().toString());
        if (dto.getFirstProductionDate() != null) attrs.put(PfWellDto.ATTR_FIRST_PRODUCTION_DATE, dto.getFirstProductionDate().toString());
        if (dto.getCurrentProductionBpd() != null) attrs.put(PfWellDto.ATTR_CURRENT_PRODUCTION_BPD, dto.getCurrentProductionBpd());
        if (dto.getRvWellId() != null) attrs.put(PfWellDto.ATTR_RV_WELL_ID, dto.getRvWellId().toString());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadWellAttributes(PfWellDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case PfWellDto.ATTR_API_NUMBER -> dto.setApiNumber(strValue);
                case PfWellDto.ATTR_STATUS -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setStatus(WellStatus.valueOf(strValue));
                    }
                }
                case PfWellDto.ATTR_LIFT_SYSTEM_TYPE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setLiftSystemType(LiftSystemType.valueOf(strValue));
                    }
                }
                case PfWellDto.ATTR_LATITUDE -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setLatitude(BigDecimal.valueOf(v)));
                case PfWellDto.ATTR_LONGITUDE -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setLongitude(BigDecimal.valueOf(v)));
                case PfWellDto.ATTR_MEASURED_DEPTH_FT -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setMeasuredDepthFt(BigDecimal.valueOf(v)));
                case PfWellDto.ATTR_TRUE_VERTICAL_DEPTH_FT -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setTrueVerticalDepthFt(BigDecimal.valueOf(v)));
                case PfWellDto.ATTR_SPUD_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setSpudDate(LocalDate.parse(strValue));
                    }
                }
                case PfWellDto.ATTR_FIRST_PRODUCTION_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setFirstProductionDate(LocalDate.parse(strValue));
                    }
                }
                case PfWellDto.ATTR_CURRENT_PRODUCTION_BPD -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setCurrentProductionBpd(BigDecimal.valueOf(v)));
                case PfWellDto.ATTR_RV_WELL_ID -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setRvWellId(UUID.fromString(strValue));
                    }
                }
            }
        }
    }

    private PfWellDto mapAssetToDto(Asset asset) {
        PfWellDto dto = new PfWellDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setCreatedTime(asset.getCreatedTime());
        loadWellAttributes(dto);
        return dto;
    }
}
