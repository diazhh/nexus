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
import org.thingsboard.nexus.rv.dto.RvFieldDto;
import org.thingsboard.nexus.rv.exception.RvBusinessException;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Field (Campo) entities.
 * Provides CRUD operations and business logic for rv_field Assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvFieldService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    /**
     * Creates a new Field under a Basin.
     */
    public RvFieldDto createField(UUID tenantId, RvFieldDto fieldDto) {
        log.info("Creating field: {} under basin: {}", fieldDto.getName(), fieldDto.getBasinAssetId());

        // Validate basin exists
        if (fieldDto.getBasinAssetId() != null) {
            if (!rvAssetService.existsById(fieldDto.getBasinAssetId())) {
                throw new RvEntityNotFoundException("Basin", fieldDto.getBasinAssetId());
            }
        }

        // Create ThingsBoard Asset
        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvFieldDto.ASSET_TYPE,
            fieldDto.getName(),
            fieldDto.getLabel()
        );

        fieldDto.setAssetId(asset.getId().getId());
        fieldDto.setTenantId(tenantId);
        fieldDto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveFieldAttributes(fieldDto);

        // Create hierarchy relation
        if (fieldDto.getBasinAssetId() != null) {
            rvHierarchyService.setParentChild(tenantId, fieldDto.getBasinAssetId(), fieldDto.getAssetId());
        }

        log.info("Field created with ID: {}", fieldDto.getAssetId());
        return fieldDto;
    }

    /**
     * Gets a Field by ID.
     */
    public Optional<RvFieldDto> getFieldById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvFieldDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvFieldDto dto = new RvFieldDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                // Load attributes
                loadFieldAttributes(dto);

                // Get parent basin
                UUID basinId = rvHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                dto.setBasinAssetId(basinId);

                return dto;
            });
    }

    /**
     * Gets all Fields for a tenant.
     */
    public Page<RvFieldDto> getAllFields(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvFieldDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToFieldDto);
    }

    /**
     * Gets Fields by Basin.
     */
    public List<RvFieldDto> getFieldsByBasin(UUID tenantId, UUID basinAssetId) {
        List<UUID> fieldIds = rvHierarchyService.getChildren(tenantId, basinAssetId);
        List<RvFieldDto> fields = new ArrayList<>();

        for (UUID fieldId : fieldIds) {
            getFieldById(fieldId).ifPresent(fields::add);
        }

        return fields;
    }

    /**
     * Updates a Field.
     */
    public RvFieldDto updateField(RvFieldDto fieldDto) {
        log.info("Updating field: {}", fieldDto.getAssetId());

        rvAssetService.getAssetById(fieldDto.getAssetId()).ifPresent(asset -> {
            boolean needsUpdate = false;
            if (!asset.getName().equals(fieldDto.getName())) {
                asset.setName(fieldDto.getName());
                needsUpdate = true;
            }
            if (!Objects.equals(asset.getLabel(), fieldDto.getLabel())) {
                asset.setLabel(fieldDto.getLabel());
                needsUpdate = true;
            }
            if (needsUpdate) {
                rvAssetService.updateAsset(asset);
            }
        });

        fieldDto.setUpdatedTime(System.currentTimeMillis());
        saveFieldAttributes(fieldDto);

        return fieldDto;
    }

    /**
     * Deletes a Field.
     */
    public void deleteField(UUID tenantId, UUID assetId) {
        log.warn("Deleting field: {}", assetId);

        List<UUID> children = rvHierarchyService.getChildren(tenantId, assetId);
        if (!children.isEmpty()) {
            throw new RvBusinessException(RvBusinessException.INVALID_HIERARCHY,
                "Cannot delete field with " + children.size() + " associated reservoirs");
        }

        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    private void saveFieldAttributes(RvFieldDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCode() != null) attrs.put(RvFieldDto.ATTR_CODE, dto.getCode());
        if (dto.getOperationalStatus() != null) attrs.put(RvFieldDto.ATTR_OPERATIONAL_STATUS, dto.getOperationalStatus());
        if (dto.getOperatorCompany() != null) attrs.put(RvFieldDto.ATTR_OPERATOR_COMPANY, dto.getOperatorCompany());
        if (dto.getFieldType() != null) attrs.put(RvFieldDto.ATTR_FIELD_TYPE, dto.getFieldType());
        if (dto.getDiscoveryYear() != null) attrs.put(RvFieldDto.ATTR_DISCOVERY_YEAR, dto.getDiscoveryYear());
        if (dto.getProductionStartYear() != null) attrs.put(RvFieldDto.ATTR_PRODUCTION_START_YEAR, dto.getProductionStartYear());
        if (dto.getTotalAreaKm2() != null) attrs.put(RvFieldDto.ATTR_TOTAL_AREA_KM2, dto.getTotalAreaKm2());
        if (dto.getPredominantFluidType() != null) attrs.put(RvFieldDto.ATTR_PREDOMINANT_FLUID_TYPE, dto.getPredominantFluidType());
        if (dto.getAverageApiGravity() != null) attrs.put(RvFieldDto.ATTR_AVERAGE_API_GRAVITY, dto.getAverageApiGravity());
        if (dto.getOilInPlaceMmbbl() != null) attrs.put(RvFieldDto.ATTR_OIL_IN_PLACE_MMBBL, dto.getOilInPlaceMmbbl());
        if (dto.getFajaSector() != null) attrs.put(RvFieldDto.ATTR_FAJA_SECTOR, dto.getFajaSector());
        if (dto.getRequiresDiluent() != null) attrs.put(RvFieldDto.ATTR_REQUIRES_DILUENT, dto.getRequiresDiluent());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadFieldAttributes(RvFieldDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvFieldDto.ATTR_CODE -> dto.setCode(entry.getValueAsString());
                case RvFieldDto.ATTR_OPERATIONAL_STATUS -> dto.setOperationalStatus(entry.getValueAsString());
                case RvFieldDto.ATTR_OPERATOR_COMPANY -> dto.setOperatorCompany(entry.getValueAsString());
                case RvFieldDto.ATTR_FIELD_TYPE -> dto.setFieldType(entry.getValueAsString());
                case RvFieldDto.ATTR_DISCOVERY_YEAR -> entry.getLongValue().ifPresent(v -> dto.setDiscoveryYear(v.intValue()));
                case RvFieldDto.ATTR_PRODUCTION_START_YEAR -> entry.getLongValue().ifPresent(v -> dto.setProductionStartYear(v.intValue()));
                case RvFieldDto.ATTR_TOTAL_AREA_KM2 -> entry.getDoubleValue().ifPresent(v -> dto.setTotalAreaKm2(BigDecimal.valueOf(v)));
                case RvFieldDto.ATTR_PREDOMINANT_FLUID_TYPE -> dto.setPredominantFluidType(entry.getValueAsString());
                case RvFieldDto.ATTR_AVERAGE_API_GRAVITY -> entry.getDoubleValue().ifPresent(v -> dto.setAverageApiGravity(BigDecimal.valueOf(v)));
                case RvFieldDto.ATTR_OIL_IN_PLACE_MMBBL -> entry.getDoubleValue().ifPresent(v -> dto.setOilInPlaceMmbbl(BigDecimal.valueOf(v)));
                case RvFieldDto.ATTR_FAJA_SECTOR -> dto.setFajaSector(entry.getValueAsString());
                case RvFieldDto.ATTR_REQUIRES_DILUENT -> entry.getBooleanValue().ifPresent(dto::setRequiresDiluent);
            }
        }
    }

    private RvFieldDto mapAssetToFieldDto(Asset asset) {
        RvFieldDto dto = new RvFieldDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadFieldAttributes(dto);
        return dto;
    }
}
