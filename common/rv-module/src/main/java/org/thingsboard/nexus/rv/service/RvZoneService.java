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
import org.thingsboard.nexus.rv.dto.RvZoneDto;
import org.thingsboard.nexus.rv.exception.RvBusinessException;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Zone entities.
 * Zones are vertical subdivisions within a Reservoir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvZoneService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    /**
     * Creates a new Zone under a Reservoir.
     */
    public RvZoneDto createZone(UUID tenantId, RvZoneDto dto) {
        log.info("Creating zone: {} in reservoir: {}", dto.getName(), dto.getReservoirAssetId());

        if (dto.getReservoirAssetId() != null) {
            if (!rvAssetService.existsById(dto.getReservoirAssetId())) {
                throw new RvEntityNotFoundException("Reservoir", dto.getReservoirAssetId());
            }
        }

        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvZoneDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        saveZoneAttributes(dto);

        if (dto.getReservoirAssetId() != null) {
            rvHierarchyService.setParentChild(tenantId, dto.getReservoirAssetId(), dto.getAssetId());
        }

        log.info("Zone created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Zone by ID.
     */
    public Optional<RvZoneDto> getZoneById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvZoneDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvZoneDto dto = new RvZoneDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadZoneAttributes(dto);

                UUID reservoirId = rvHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                dto.setReservoirAssetId(reservoirId);

                return dto;
            });
    }

    /**
     * Gets all Zones for a tenant.
     */
    public Page<RvZoneDto> getAllZones(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvZoneDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Zones by Reservoir.
     */
    public List<RvZoneDto> getZonesByReservoir(UUID tenantId, UUID reservoirAssetId) {
        List<UUID> zoneIds = rvHierarchyService.getChildren(tenantId, reservoirAssetId);
        List<RvZoneDto> zones = new ArrayList<>();

        for (UUID zoneId : zoneIds) {
            getZoneById(zoneId).ifPresent(zones::add);
        }

        // Sort by zone number
        zones.sort(Comparator.comparing(z -> z.getZoneNumber() != null ? z.getZoneNumber() : 0));

        return zones;
    }

    /**
     * Updates a Zone.
     */
    public RvZoneDto updateZone(RvZoneDto dto) {
        log.info("Updating zone: {}", dto.getAssetId());

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
        saveZoneAttributes(dto);

        return dto;
    }

    /**
     * Deletes a Zone.
     */
    public void deleteZone(UUID tenantId, UUID assetId) {
        log.warn("Deleting zone: {}", assetId);
        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Calculates net-to-gross ratio.
     */
    public BigDecimal calculateNetToGross(UUID assetId) {
        RvZoneDto zone = getZoneById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("Zone", assetId));

        if (zone.getGrossThicknessM() == null || zone.getNetPayThicknessM() == null) {
            throw new RvBusinessException(RvBusinessException.INSUFFICIENT_DATA,
                "Gross and net thickness required");
        }

        BigDecimal ntg = zone.getNetPayThicknessM()
            .divide(zone.getGrossThicknessM(), 4, java.math.RoundingMode.HALF_UP);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("net_to_gross_ratio", ntg);
        rvAttributeService.saveServerAttributes(assetId, attrs);

        return ntg;
    }

    /**
     * Creates adjacent zone relation.
     */
    public void setAdjacentZones(UUID tenantId, UUID zone1Id, UUID zone2Id) {
        log.info("Setting zones {} and {} as adjacent", zone1Id, zone2Id);
        rvHierarchyService.createAdjacentToRelation(tenantId, zone1Id, zone2Id);
    }

    /**
     * Updates net-to-gross ratio for a zone.
     */
    public void updateNetToGross(UUID assetId, BigDecimal ntgRatio) {
        log.info("Updating net-to-gross for zone {}: {}", assetId, ntgRatio);

        if (!rvAssetService.existsById(assetId)) {
            throw new RvEntityNotFoundException("Zone", assetId);
        }

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("net_to_gross_ratio", ntgRatio);
        rvAttributeService.saveServerAttributes(assetId, attrs);
    }

    private void saveZoneAttributes(RvZoneDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCode() != null) attrs.put(RvZoneDto.ATTR_CODE, dto.getCode());
        if (dto.getZoneNumber() != null) attrs.put(RvZoneDto.ATTR_ZONE_NUMBER, dto.getZoneNumber());
        if (dto.getTopDepthMdM() != null) attrs.put(RvZoneDto.ATTR_TOP_DEPTH_MD_M, dto.getTopDepthMdM());
        if (dto.getBottomDepthMdM() != null) attrs.put(RvZoneDto.ATTR_BOTTOM_DEPTH_MD_M, dto.getBottomDepthMdM());
        if (dto.getGrossThicknessM() != null) attrs.put(RvZoneDto.ATTR_GROSS_THICKNESS_M, dto.getGrossThicknessM());
        if (dto.getNetPayThicknessM() != null) attrs.put(RvZoneDto.ATTR_NET_PAY_THICKNESS_M, dto.getNetPayThicknessM());
        if (dto.getPorosityFrac() != null) attrs.put(RvZoneDto.ATTR_POROSITY_FRAC, dto.getPorosityFrac());
        if (dto.getPermeabilityMd() != null) attrs.put(RvZoneDto.ATTR_PERMEABILITY_MD, dto.getPermeabilityMd());
        if (dto.getWaterSaturationFrac() != null) attrs.put(RvZoneDto.ATTR_WATER_SATURATION_FRAC, dto.getWaterSaturationFrac());
        if (dto.getLithology() != null) attrs.put(RvZoneDto.ATTR_LITHOLOGY, dto.getLithology());
        if (dto.getZoneStatus() != null) attrs.put(RvZoneDto.ATTR_ZONE_STATUS, dto.getZoneStatus());
        if (dto.getIsPerforated() != null) attrs.put(RvZoneDto.ATTR_IS_PERFORATED, dto.getIsPerforated());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadZoneAttributes(RvZoneDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvZoneDto.ATTR_CODE -> dto.setCode(entry.getValueAsString());
                case RvZoneDto.ATTR_ZONE_NUMBER -> entry.getLongValue().ifPresent(v -> dto.setZoneNumber(v.intValue()));
                case RvZoneDto.ATTR_TOP_DEPTH_MD_M -> entry.getDoubleValue().ifPresent(v -> dto.setTopDepthMdM(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_BOTTOM_DEPTH_MD_M -> entry.getDoubleValue().ifPresent(v -> dto.setBottomDepthMdM(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_GROSS_THICKNESS_M -> entry.getDoubleValue().ifPresent(v -> dto.setGrossThicknessM(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_NET_PAY_THICKNESS_M -> entry.getDoubleValue().ifPresent(v -> dto.setNetPayThicknessM(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_POROSITY_FRAC -> entry.getDoubleValue().ifPresent(v -> dto.setPorosityFrac(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_PERMEABILITY_MD -> entry.getDoubleValue().ifPresent(v -> dto.setPermeabilityMd(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_WATER_SATURATION_FRAC -> entry.getDoubleValue().ifPresent(v -> dto.setWaterSaturationFrac(BigDecimal.valueOf(v)));
                case RvZoneDto.ATTR_LITHOLOGY -> dto.setLithology(entry.getValueAsString());
                case RvZoneDto.ATTR_ZONE_STATUS -> dto.setZoneStatus(entry.getValueAsString());
                case RvZoneDto.ATTR_IS_PERFORATED -> entry.getBooleanValue().ifPresent(dto::setIsPerforated);
            }
        }
    }

    private RvZoneDto mapAssetToDto(Asset asset) {
        RvZoneDto dto = new RvZoneDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadZoneAttributes(dto);
        return dto;
    }
}
