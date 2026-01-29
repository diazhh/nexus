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
import org.thingsboard.nexus.rv.dto.RvBasinDto;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Basin (Cuenca) entities.
 * Provides CRUD operations and business logic for rv_basin Assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvBasinService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    /**
     * Creates a new Basin.
     */
    public RvBasinDto createBasin(UUID tenantId, RvBasinDto basinDto) {
        log.info("Creating basin: {}", basinDto.getName());

        // Create ThingsBoard Asset
        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvBasinDto.ASSET_TYPE,
            basinDto.getName(),
            basinDto.getLabel()
        );

        basinDto.setAssetId(asset.getId().getId());
        basinDto.setTenantId(tenantId);
        basinDto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveBasinAttributes(basinDto);

        log.info("Basin created with ID: {}", basinDto.getAssetId());
        return basinDto;
    }

    /**
     * Gets a Basin by ID.
     */
    public Optional<RvBasinDto> getBasinById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvBasinDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvBasinDto dto = new RvBasinDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                // Load attributes
                loadBasinAttributes(dto);

                return dto;
            });
    }

    /**
     * Gets all Basins for a tenant.
     */
    public Page<RvBasinDto> getAllBasins(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvBasinDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToBasinDto);
    }

    /**
     * Searches Basins by name.
     */
    public Page<RvBasinDto> searchBasins(UUID tenantId, String searchText, int page, int size) {
        Page<Asset> assets = rvAssetService.searchAssetsByName(tenantId, RvBasinDto.ASSET_TYPE, searchText, page, size);
        return assets.map(this::mapAssetToBasinDto);
    }

    /**
     * Updates a Basin.
     */
    public RvBasinDto updateBasin(RvBasinDto basinDto) {
        log.info("Updating basin: {}", basinDto.getAssetId());

        // Update Asset name/label if changed
        rvAssetService.getAssetById(basinDto.getAssetId()).ifPresent(asset -> {
            boolean needsUpdate = false;
            if (!asset.getName().equals(basinDto.getName())) {
                asset.setName(basinDto.getName());
                needsUpdate = true;
            }
            if (!Objects.equals(asset.getLabel(), basinDto.getLabel())) {
                asset.setLabel(basinDto.getLabel());
                needsUpdate = true;
            }
            if (needsUpdate) {
                rvAssetService.updateAsset(asset);
            }
        });

        // Update attributes
        basinDto.setUpdatedTime(System.currentTimeMillis());
        saveBasinAttributes(basinDto);

        return basinDto;
    }

    /**
     * Deletes a Basin.
     */
    public void deleteBasin(UUID tenantId, UUID assetId) {
        log.warn("Deleting basin: {}", assetId);

        // Check if has children (Fields)
        List<UUID> children = rvHierarchyService.getChildren(tenantId, assetId);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete basin with " + children.size() + " associated fields");
        }

        // Delete relations first
        rvHierarchyService.deleteAllRelations(tenantId, assetId);

        // Delete Asset
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Gets statistics for a Basin.
     */
    public Map<String, Object> getBasinStatistics(UUID tenantId, UUID basinAssetId) {
        Map<String, Object> stats = new HashMap<>();

        // Count Fields
        List<UUID> fieldIds = rvHierarchyService.getChildren(tenantId, basinAssetId);
        stats.put("fieldCount", fieldIds.size());

        // Count Wells (through fields and reservoirs)
        int wellCount = 0;
        for (UUID fieldId : fieldIds) {
            List<UUID> reservoirIds = rvHierarchyService.getChildren(tenantId, fieldId);
            for (UUID reservoirId : reservoirIds) {
                List<UUID> wellIds = rvHierarchyService.getChildren(tenantId, reservoirId);
                wellCount += wellIds.size();
            }
        }
        stats.put("wellCount", wellCount);

        return stats;
    }

    private void saveBasinAttributes(RvBasinDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCode() != null) attrs.put(RvBasinDto.ATTR_CODE, dto.getCode());
        if (dto.getBasinType() != null) attrs.put(RvBasinDto.ATTR_BASIN_TYPE, dto.getBasinType());
        if (dto.getCountry() != null) attrs.put(RvBasinDto.ATTR_COUNTRY, dto.getCountry());
        if (dto.getRegion() != null) attrs.put(RvBasinDto.ATTR_REGION, dto.getRegion());
        if (dto.getTotalAreaKm2() != null) attrs.put(RvBasinDto.ATTR_TOTAL_AREA_KM2, dto.getTotalAreaKm2());
        if (dto.getEstimatedReservesMmbbl() != null) attrs.put(RvBasinDto.ATTR_ESTIMATED_RESERVES_MMBBL, dto.getEstimatedReservesMmbbl());
        if (dto.getGeojsonBoundary() != null) attrs.put(RvBasinDto.ATTR_GEOJSON_BOUNDARY, dto.getGeojsonBoundary());
        if (dto.getCenterLatitude() != null) attrs.put(RvBasinDto.ATTR_CENTER_LATITUDE, dto.getCenterLatitude());
        if (dto.getCenterLongitude() != null) attrs.put(RvBasinDto.ATTR_CENTER_LONGITUDE, dto.getCenterLongitude());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadBasinAttributes(RvBasinDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvBasinDto.ATTR_CODE -> dto.setCode(entry.getValueAsString());
                case RvBasinDto.ATTR_BASIN_TYPE -> dto.setBasinType(entry.getValueAsString());
                case RvBasinDto.ATTR_COUNTRY -> dto.setCountry(entry.getValueAsString());
                case RvBasinDto.ATTR_REGION -> dto.setRegion(entry.getValueAsString());
                case RvBasinDto.ATTR_TOTAL_AREA_KM2 -> entry.getDoubleValue().ifPresent(v -> dto.setTotalAreaKm2(BigDecimal.valueOf(v)));
                case RvBasinDto.ATTR_ESTIMATED_RESERVES_MMBBL -> entry.getDoubleValue().ifPresent(v -> dto.setEstimatedReservesMmbbl(BigDecimal.valueOf(v)));
                case RvBasinDto.ATTR_CENTER_LATITUDE -> entry.getDoubleValue().ifPresent(v -> dto.setCenterLatitude(BigDecimal.valueOf(v)));
                case RvBasinDto.ATTR_CENTER_LONGITUDE -> entry.getDoubleValue().ifPresent(v -> dto.setCenterLongitude(BigDecimal.valueOf(v)));
            }
        }
    }

    private RvBasinDto mapAssetToBasinDto(Asset asset) {
        RvBasinDto dto = new RvBasinDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadBasinAttributes(dto);
        return dto;
    }
}
