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
import org.thingsboard.nexus.rv.dto.RvWellDto;
import org.thingsboard.nexus.rv.exception.RvBusinessException;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Well (Pozo) entities.
 * Provides CRUD operations, IPR analysis, and production tracking.
 *
 * Integration point with Drilling Module (Taladros) and Production Module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvWellService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;
    private final RvCalculationService calculationService;

    /**
     * Creates a new Well.
     */
    public RvWellDto createWell(UUID tenantId, RvWellDto dto) {
        log.info("Creating well: {} in reservoir: {}", dto.getName(), dto.getReservoirAssetId());

        // Validate reservoir exists
        if (dto.getReservoirAssetId() != null) {
            if (!rvAssetService.existsById(dto.getReservoirAssetId())) {
                throw new RvEntityNotFoundException("Reservoir", dto.getReservoirAssetId());
            }
        }

        // Create ThingsBoard Asset
        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvWellDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveWellAttributes(dto);

        // Create hierarchy relations
        if (dto.getReservoirAssetId() != null) {
            rvHierarchyService.setParentChild(tenantId, dto.getReservoirAssetId(), dto.getAssetId());
            rvHierarchyService.createProducesFromRelation(tenantId, dto.getAssetId(), dto.getReservoirAssetId());
        }

        log.info("Well created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Well by ID.
     */
    public Optional<RvWellDto> getWellById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvWellDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvWellDto dto = new RvWellDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadWellAttributes(dto);

                // Get parent reservoir
                UUID reservoirId = rvHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                dto.setReservoirAssetId(reservoirId);

                return dto;
            });
    }

    /**
     * Gets all Wells for a tenant.
     */
    public Page<RvWellDto> getAllWells(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvWellDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Wells by Reservoir.
     */
    public List<RvWellDto> getWellsByReservoir(UUID tenantId, UUID reservoirAssetId) {
        List<UUID> wellIds = rvHierarchyService.getChildren(tenantId, reservoirAssetId);
        List<RvWellDto> wells = new ArrayList<>();

        for (UUID wellId : wellIds) {
            getWellById(wellId).ifPresent(wells::add);
        }

        return wells;
    }

    /**
     * Gets Wells by Field (through reservoirs).
     */
    public List<RvWellDto> getWellsByField(UUID tenantId, UUID fieldAssetId) {
        List<RvWellDto> wells = new ArrayList<>();

        // Get all reservoirs in field
        List<UUID> reservoirIds = rvHierarchyService.getChildren(tenantId, fieldAssetId);

        for (UUID reservoirId : reservoirIds) {
            wells.addAll(getWellsByReservoir(tenantId, reservoirId));
        }

        return wells;
    }

    /**
     * Gets Wells by status.
     */
    public List<RvWellDto> getWellsByStatus(UUID tenantId, String status, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvWellDto.ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(w -> status.equals(w.getWellStatus()))
            .toList();
    }

    /**
     * Updates a Well.
     */
    public RvWellDto updateWell(RvWellDto dto) {
        log.info("Updating well: {}", dto.getAssetId());

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
        saveWellAttributes(dto);

        return dto;
    }

    /**
     * Updates well status.
     */
    public void updateWellStatus(UUID assetId, String newStatus) {
        log.info("Updating well {} status to {}", assetId, newStatus);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(RvWellDto.ATTR_WELL_STATUS, newStatus);
        rvAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Deletes a Well.
     */
    public void deleteWell(UUID tenantId, UUID assetId) {
        log.warn("Deleting well: {}", assetId);

        List<UUID> children = rvHierarchyService.getChildren(tenantId, assetId);
        if (!children.isEmpty()) {
            throw new RvBusinessException(RvBusinessException.INVALID_HIERARCHY,
                "Cannot delete well with " + children.size() + " associated completions");
        }

        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Calculates Productivity Index from test data.
     */
    public BigDecimal calculateProductivityIndex(UUID assetId, BigDecimal testRate,
                                                  BigDecimal reservoirPressure, BigDecimal flowingPressure) {
        log.info("Calculating PI for well {}", assetId);

        BigDecimal pi = calculationService.calculateProductivityIndex(testRate, reservoirPressure, flowingPressure);

        // Update well with calculated PI
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(RvWellDto.ATTR_PRODUCTIVITY_INDEX, pi);
        rvAttributeService.saveServerAttributes(assetId, attrs);

        return pi;
    }

    /**
     * Calculates expected rate using Vogel IPR.
     */
    public BigDecimal calculateExpectedRate(UUID assetId, BigDecimal flowingPressure) {
        RvWellDto well = getWellById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("Well", assetId));

        if (well.getProductivityIndexBpdPsi() == null) {
            throw new RvBusinessException(RvBusinessException.INSUFFICIENT_DATA,
                "Productivity Index not available for well");
        }

        // Need reservoir pressure - get from parent reservoir
        // For now, use a simple calculation
        // In production, this would integrate with reservoir service

        return calculationService.calculateIprVogel(
            BigDecimal.valueOf(1000), // qmax - would be calculated
            BigDecimal.valueOf(3000), // Pr - would come from reservoir
            flowingPressure
        );
    }

    /**
     * Links well to drilling job (integration with Drilling Module).
     */
    public void linkToDrillingJob(UUID tenantId, UUID wellAssetId, UUID drillingJobAssetId) {
        log.info("Linking well {} to drilling job {}", wellAssetId, drillingJobAssetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("drilling_job_asset_id", drillingJobAssetId.toString());
        rvAttributeService.saveServerAttributes(wellAssetId, attrs);

        // Create relation
        rvHierarchyService.createCharacterizedByRelation(tenantId, wellAssetId, drillingJobAssetId);
    }

    /**
     * Links well to production unit (integration with Production Module).
     */
    public void linkToProductionUnit(UUID tenantId, UUID wellAssetId, UUID productionUnitAssetId) {
        log.info("Linking well {} to production unit {}", wellAssetId, productionUnitAssetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("production_unit_asset_id", productionUnitAssetId.toString());
        rvAttributeService.saveServerAttributes(wellAssetId, attrs);
    }

    /**
     * Gets well count by type for a field.
     */
    public Map<String, Long> getWellCountByType(UUID tenantId, UUID fieldAssetId) {
        List<RvWellDto> wells = getWellsByField(tenantId, fieldAssetId);

        Map<String, Long> counts = new HashMap<>();
        counts.put("PRODUCER", wells.stream().filter(w -> "PRODUCER".equals(w.getWellType())).count());
        counts.put("INJECTOR", wells.stream().filter(w -> "INJECTOR".equals(w.getWellType())).count());
        counts.put("OBSERVATION", wells.stream().filter(w -> "OBSERVATION".equals(w.getWellType())).count());
        counts.put("TOTAL", (long) wells.size());

        return counts;
    }

    private void saveWellAttributes(RvWellDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getWellCode() != null) attrs.put(RvWellDto.ATTR_WELL_CODE, dto.getWellCode());
        if (dto.getWellType() != null) attrs.put(RvWellDto.ATTR_WELL_TYPE, dto.getWellType());
        if (dto.getWellStatus() != null) attrs.put(RvWellDto.ATTR_WELL_STATUS, dto.getWellStatus());
        if (dto.getWellPurpose() != null) attrs.put(RvWellDto.ATTR_WELL_PURPOSE, dto.getWellPurpose());
        if (dto.getWellConfiguration() != null) attrs.put(RvWellDto.ATTR_WELL_CONFIGURATION, dto.getWellConfiguration());
        if (dto.getLiftMethod() != null) attrs.put(RvWellDto.ATTR_LIFT_METHOD, dto.getLiftMethod());
        if (dto.getSurfaceLatitude() != null) attrs.put(RvWellDto.ATTR_SURFACE_LATITUDE, dto.getSurfaceLatitude());
        if (dto.getSurfaceLongitude() != null) attrs.put(RvWellDto.ATTR_SURFACE_LONGITUDE, dto.getSurfaceLongitude());
        if (dto.getTotalDepthMdM() != null) attrs.put(RvWellDto.ATTR_TOTAL_DEPTH_MD_M, dto.getTotalDepthMdM());
        if (dto.getTotalDepthTvdM() != null) attrs.put(RvWellDto.ATTR_TOTAL_DEPTH_TVD_M, dto.getTotalDepthTvdM());
        if (dto.getSpudDate() != null) attrs.put(RvWellDto.ATTR_SPUD_DATE, dto.getSpudDate());
        if (dto.getCompletionDate() != null) attrs.put(RvWellDto.ATTR_COMPLETION_DATE, dto.getCompletionDate());
        if (dto.getFirstProductionDate() != null) attrs.put(RvWellDto.ATTR_FIRST_PRODUCTION_DATE, dto.getFirstProductionDate());
        if (dto.getProductivityIndexBpdPsi() != null) attrs.put(RvWellDto.ATTR_PRODUCTIVITY_INDEX, dto.getProductivityIndexBpdPsi());
        if (dto.getIsColdProduction() != null) attrs.put(RvWellDto.ATTR_IS_COLD_PRODUCTION, dto.getIsColdProduction());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadWellAttributes(RvWellDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvWellDto.ATTR_WELL_CODE -> dto.setWellCode(entry.getValueAsString());
                case RvWellDto.ATTR_WELL_TYPE -> dto.setWellType(entry.getValueAsString());
                case RvWellDto.ATTR_WELL_STATUS -> dto.setWellStatus(entry.getValueAsString());
                case RvWellDto.ATTR_WELL_PURPOSE -> dto.setWellPurpose(entry.getValueAsString());
                case RvWellDto.ATTR_WELL_CONFIGURATION -> dto.setWellConfiguration(entry.getValueAsString());
                case RvWellDto.ATTR_LIFT_METHOD -> dto.setLiftMethod(entry.getValueAsString());
                case RvWellDto.ATTR_SURFACE_LATITUDE -> entry.getDoubleValue().ifPresent(v -> dto.setSurfaceLatitude(BigDecimal.valueOf(v)));
                case RvWellDto.ATTR_SURFACE_LONGITUDE -> entry.getDoubleValue().ifPresent(v -> dto.setSurfaceLongitude(BigDecimal.valueOf(v)));
                case RvWellDto.ATTR_TOTAL_DEPTH_MD_M -> entry.getDoubleValue().ifPresent(v -> dto.setTotalDepthMdM(BigDecimal.valueOf(v)));
                case RvWellDto.ATTR_TOTAL_DEPTH_TVD_M -> entry.getDoubleValue().ifPresent(v -> dto.setTotalDepthTvdM(BigDecimal.valueOf(v)));
                case RvWellDto.ATTR_SPUD_DATE -> entry.getLongValue().ifPresent(dto::setSpudDate);
                case RvWellDto.ATTR_COMPLETION_DATE -> entry.getLongValue().ifPresent(dto::setCompletionDate);
                case RvWellDto.ATTR_FIRST_PRODUCTION_DATE -> entry.getLongValue().ifPresent(dto::setFirstProductionDate);
                case RvWellDto.ATTR_PRODUCTIVITY_INDEX -> entry.getDoubleValue().ifPresent(v -> dto.setProductivityIndexBpdPsi(BigDecimal.valueOf(v)));
                case RvWellDto.ATTR_IS_COLD_PRODUCTION -> entry.getBooleanValue().ifPresent(dto::setIsColdProduction);
            }
        }
    }

    private RvWellDto mapAssetToDto(Asset asset) {
        RvWellDto dto = new RvWellDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadWellAttributes(dto);
        return dto;
    }
}
