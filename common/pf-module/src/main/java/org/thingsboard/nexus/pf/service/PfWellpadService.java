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
import org.thingsboard.nexus.pf.dto.OperationalStatus;
import org.thingsboard.nexus.pf.dto.PfWellpadDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Wellpad (Macolla/Cluster) entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfWellpadService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;

    /**
     * Creates a new Wellpad.
     */
    public PfWellpadDto createWellpad(UUID tenantId, PfWellpadDto dto) {
        log.info("Creating wellpad: {}", dto.getName());

        // Validate flow station exists if specified
        if (dto.getFlowStationId() != null) {
            if (!pfAssetService.existsById(dto.getFlowStationId())) {
                throw new PfEntityNotFoundException("FlowStation", dto.getFlowStationId());
            }
        }

        // Create ThingsBoard Asset
        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfWellpadDto.ASSET_TYPE,
                dto.getName(),
                dto.getName()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveWellpadAttributes(dto);

        // Create hierarchy relations
        if (dto.getFlowStationId() != null) {
            pfHierarchyService.setParentChild(tenantId, dto.getFlowStationId(), dto.getAssetId());
        }

        log.info("Wellpad created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Wellpad by ID.
     */
    public Optional<PfWellpadDto> getWellpadById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfWellpadDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfWellpadDto dto = new PfWellpadDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setName(asset.getName());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadWellpadAttributes(dto);

                    // Get parent flow station
                    UUID flowStationId = pfHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                    dto.setFlowStationId(flowStationId);

                    // Count wells
                    List<UUID> wellIds = pfHierarchyService.getChildren(dto.getTenantId(), dto.getAssetId());
                    dto.setCurrentWellCount(wellIds.size());

                    return dto;
                });
    }

    /**
     * Gets all Wellpads for a tenant.
     */
    public Page<PfWellpadDto> getAllWellpads(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfWellpadDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Wellpads by Flow Station.
     */
    public List<PfWellpadDto> getWellpadsByFlowStation(UUID tenantId, UUID flowStationId) {
        List<UUID> wellpadIds = pfHierarchyService.getChildren(tenantId, flowStationId);
        List<PfWellpadDto> wellpads = new ArrayList<>();

        for (UUID wellpadId : wellpadIds) {
            getWellpadById(wellpadId).ifPresent(wellpads::add);
        }

        return wellpads;
    }

    /**
     * Updates a Wellpad.
     */
    public PfWellpadDto updateWellpad(PfWellpadDto dto) {
        log.info("Updating wellpad: {}", dto.getAssetId());

        pfAssetService.getAssetById(dto.getAssetId()).ifPresent(asset -> {
            if (!asset.getName().equals(dto.getName())) {
                asset.setName(dto.getName());
                pfAssetService.updateAsset(asset);
            }
        });

        dto.setUpdatedTime(System.currentTimeMillis());
        saveWellpadAttributes(dto);

        return dto;
    }

    /**
     * Updates total production for a wellpad.
     */
    public void updateTotalProduction(UUID assetId, BigDecimal totalProductionBpd) {
        log.debug("Updating wellpad {} total production to {} BPD", assetId, totalProductionBpd);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfWellpadDto.ATTR_TOTAL_PRODUCTION_BPD, totalProductionBpd);
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Updates operational status.
     */
    public void updateOperationalStatus(UUID assetId, OperationalStatus status) {
        log.info("Updating wellpad {} status to {}", assetId, status);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfWellpadDto.ATTR_OPERATIONAL_STATUS, status.name());
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Deletes a Wellpad.
     */
    public void deleteWellpad(UUID tenantId, UUID assetId) {
        log.warn("Deleting wellpad: {}", assetId);

        // Check if wellpad has wells
        List<UUID> wellIds = pfHierarchyService.getChildren(tenantId, assetId);
        if (!wellIds.isEmpty()) {
            throw new IllegalStateException("Cannot delete wellpad with " + wellIds.size() + " wells. Remove wells first.");
        }

        pfHierarchyService.removeAllRelations(tenantId, assetId);
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Gets dashboard summary for all wellpads.
     */
    public WellpadSummary getWellpadSummary(UUID tenantId) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfWellpadDto.ASSET_TYPE, 0, 10000);

        int totalWellpads = 0;
        int operationalCount = 0;
        int maintenanceCount = 0;
        BigDecimal totalProduction = BigDecimal.ZERO;

        for (Asset asset : assets.getContent()) {
            totalWellpads++;
            PfWellpadDto dto = mapAssetToDto(asset);

            if (dto.getOperationalStatus() == OperationalStatus.OPERATIONAL) {
                operationalCount++;
            } else if (dto.getOperationalStatus() == OperationalStatus.UNDER_MAINTENANCE) {
                maintenanceCount++;
            }

            if (dto.getTotalProductionBpd() != null) {
                totalProduction = totalProduction.add(dto.getTotalProductionBpd());
            }
        }

        return new WellpadSummary(totalWellpads, operationalCount, maintenanceCount, totalProduction);
    }

    // Helper methods

    private void saveWellpadAttributes(PfWellpadDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCode() != null) attrs.put(PfWellpadDto.ATTR_CODE, dto.getCode());
        if (dto.getLatitude() != null) attrs.put(PfWellpadDto.ATTR_LATITUDE, dto.getLatitude());
        if (dto.getLongitude() != null) attrs.put(PfWellpadDto.ATTR_LONGITUDE, dto.getLongitude());
        if (dto.getCapacityWells() != null) attrs.put(PfWellpadDto.ATTR_CAPACITY_WELLS, dto.getCapacityWells());
        if (dto.getTotalProductionBpd() != null) attrs.put(PfWellpadDto.ATTR_TOTAL_PRODUCTION_BPD, dto.getTotalProductionBpd());
        if (dto.getCommissioningDate() != null) attrs.put(PfWellpadDto.ATTR_COMMISSIONING_DATE, dto.getCommissioningDate().toString());
        if (dto.getOperationalStatus() != null) attrs.put(PfWellpadDto.ATTR_OPERATIONAL_STATUS, dto.getOperationalStatus().name());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadWellpadAttributes(PfWellpadDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case PfWellpadDto.ATTR_CODE -> dto.setCode(strValue);
                case PfWellpadDto.ATTR_LATITUDE -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setLatitude(BigDecimal.valueOf(v)));
                case PfWellpadDto.ATTR_LONGITUDE -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setLongitude(BigDecimal.valueOf(v)));
                case PfWellpadDto.ATTR_CAPACITY_WELLS -> entry.getLongValue()
                        .ifPresent(v -> dto.setCapacityWells(v.intValue()));
                case PfWellpadDto.ATTR_TOTAL_PRODUCTION_BPD -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setTotalProductionBpd(BigDecimal.valueOf(v)));
                case PfWellpadDto.ATTR_COMMISSIONING_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setCommissioningDate(LocalDate.parse(strValue));
                    }
                }
                case PfWellpadDto.ATTR_OPERATIONAL_STATUS -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setOperationalStatus(OperationalStatus.valueOf(strValue));
                    }
                }
            }
        }
    }

    private PfWellpadDto mapAssetToDto(Asset asset) {
        PfWellpadDto dto = new PfWellpadDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setCreatedTime(asset.getCreatedTime());
        loadWellpadAttributes(dto);
        return dto;
    }

    /**
     * Summary record for dashboard.
     */
    public record WellpadSummary(
            int totalWellpads,
            int operationalCount,
            int maintenanceCount,
            BigDecimal totalProductionBpd
    ) {}
}
