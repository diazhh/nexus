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
package org.thingsboard.nexus.dr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.dr.dto.DrRigDto;
import org.thingsboard.nexus.dr.exception.DrBusinessException;
import org.thingsboard.nexus.dr.exception.DrEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;
import org.thingsboard.server.common.data.template.TemplateInstanceResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Drilling Rigs.
 * Uses ThingsBoard Assets as the underlying storage mechanism.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrRigService {

    private final DrAssetService assetService;
    private final DrAttributeService attributeService;
    private final DrTemplateService templateService;

    // --- Query Operations ---

    /**
     * Get a rig by its asset ID
     */
    public DrRigDto getById(UUID assetId) {
        log.debug("Getting drilling rig by asset id: {}", assetId);
        Asset asset = assetService.getAssetById(assetId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Rig", assetId.toString()));

        if (!DrRigDto.ASSET_TYPE.equals(asset.getType())) {
            throw new DrEntityNotFoundException("Drilling Rig", assetId.toString());
        }

        List<AttributeKvEntry> attributes = attributeService.getServerAttributes(assetId);
        return DrRigDto.fromAssetAndAttributes(asset, attributes);
    }

    /**
     * Get a rig by its code (searches in attributes)
     */
    public DrRigDto getByCode(UUID tenantId, String rigCode) {
        log.debug("Getting drilling rig by code: {}", rigCode);
        // Search through all rigs to find by code
        Page<Asset> assets = assetService.getAssetsByType(tenantId, DrRigDto.ASSET_TYPE, 0, 1000);

        for (Asset asset : assets.getContent()) {
            List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
            for (AttributeKvEntry attr : attributes) {
                if (DrRigDto.ATTR_RIG_CODE.equals(attr.getKey()) &&
                    rigCode.equals(attr.getStrValue().orElse(null))) {
                    return DrRigDto.fromAssetAndAttributes(asset, attributes);
                }
            }
        }

        throw new DrEntityNotFoundException("Drilling Rig", rigCode);
    }

    /**
     * Get all rigs for a tenant with pagination
     */
    public Page<DrRigDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting drilling rigs for tenant: {}", tenantId);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, DrRigDto.ASSET_TYPE,
                pageable.getPageNumber(), pageable.getPageSize());

        List<DrRigDto> dtos = assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return DrRigDto.fromAssetAndAttributes(asset, attributes);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, assets.getTotalElements());
    }

    /**
     * Get all rigs for a tenant (no pagination)
     */
    public List<DrRigDto> getAllByTenant(UUID tenantId) {
        log.debug("Getting all drilling rigs for tenant: {}", tenantId);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, DrRigDto.ASSET_TYPE, 0, 10000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return DrRigDto.fromAssetAndAttributes(asset, attributes);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get rigs by status
     */
    public List<DrRigDto> getByStatus(UUID tenantId, String status) {
        log.debug("Getting drilling rigs by status - tenant: {}, status: {}", tenantId, status);
        return getAllByTenant(tenantId).stream()
                .filter(rig -> status.equals(rig.getOperationalStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get rigs by type
     */
    public List<DrRigDto> getByType(UUID tenantId, String rigType) {
        log.debug("Getting drilling rigs by type - tenant: {}, type: {}", tenantId, rigType);
        return getAllByTenant(tenantId).stream()
                .filter(rig -> rigType.equals(rig.getRigType()))
                .collect(Collectors.toList());
    }

    /**
     * Get available rigs (status = STANDBY or RIG_DOWN)
     */
    public List<DrRigDto> getAvailableRigs(UUID tenantId) {
        log.debug("Getting available drilling rigs for tenant: {}", tenantId);
        return getAllByTenant(tenantId).stream()
                .filter(rig -> "STANDBY".equals(rig.getOperationalStatus()) ||
                               "RIG_DOWN".equals(rig.getOperationalStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Search rigs by name
     */
    public Page<DrRigDto> searchByName(UUID tenantId, String searchText, Pageable pageable) {
        log.debug("Searching rigs by name: {}", searchText);
        Page<Asset> assets = assetService.searchAssetsByName(tenantId, DrRigDto.ASSET_TYPE, searchText,
                pageable.getPageNumber(), pageable.getPageSize());

        List<DrRigDto> dtos = assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return DrRigDto.fromAssetAndAttributes(asset, attributes);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, assets.getTotalElements());
    }

    // --- Create Operations ---

    /**
     * Create a rig from a template
     */
    public DrRigDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        return createFromTemplate(tenantId, request.getTemplateId(), request.getVariables(), tenantId);
    }

    /**
     * Create a rig from a template with variables
     */
    public DrRigDto createFromTemplate(UUID tenantId, UUID templateId, Map<String, Object> variables, UUID createdBy) {
        log.info("Creating drilling rig from template {} for tenant {}", templateId, tenantId);

        String rigCode = (String) variables.get("rigCode");
        if (rigCode == null || rigCode.isEmpty()) {
            throw new DrBusinessException("Rig code is required");
        }

        // Check if rig code already exists
        try {
            getByCode(tenantId, rigCode);
            throw new DrBusinessException("Rig code already exists: " + rigCode);
        } catch (DrEntityNotFoundException e) {
            // Good, code doesn't exist
        }

        // Instantiate template to create digital twin assets
        TemplateInstanceResult instanceResult = templateService.instantiateTemplate(
                new TenantId(tenantId),
                templateId,
                variables,
                createdBy
        );

        UUID assetId = instanceResult.getRootAssetId();

        // Build attributes from variables
        DrRigDto dto = new DrRigDto();
        dto.setAssetId(assetId);
        dto.setTenantId(tenantId);
        dto.setRigCode(rigCode);
        dto.setRigName((String) variables.get("rigName"));
        dto.setRigType((String) variables.get("rigType"));
        dto.setContractor((String) variables.get("contractor"));
        dto.setManufacturer((String) variables.get("manufacturer"));
        dto.setModel((String) variables.get("model"));
        dto.setCurrentLocation((String) variables.get("location"));
        dto.setOperationalStatus("STANDBY");

        // Numeric values
        if (variables.get("yearBuilt") != null) {
            dto.setYearBuilt(Integer.parseInt(variables.get("yearBuilt").toString()));
        }
        if (variables.get("maxHookloadLbs") != null) {
            dto.setMaxHookloadLbs(Integer.parseInt(variables.get("maxHookloadLbs").toString()));
        }
        if (variables.get("maxRotaryTorqueFtLbs") != null) {
            dto.setMaxRotaryTorqueFtLbs(Integer.parseInt(variables.get("maxRotaryTorqueFtLbs").toString()));
        }
        if (variables.get("maxDepthCapabilityFt") != null) {
            dto.setMaxDepthCapabilityFt(new BigDecimal(variables.get("maxDepthCapabilityFt").toString()));
        }

        // Initialize statistics
        dto.setTotalWellsDrilled(0);
        dto.setTotalFootageDrilledFt(BigDecimal.ZERO);
        dto.setTotalNptHours(BigDecimal.ZERO);
        dto.setTotalOperationalHours(BigDecimal.ZERO);

        // Map child asset IDs from template instantiation
        Map<String, UUID> nodeMap = instanceResult.getNodeKeyToAssetIdMap();
        dto.setDrawworksAssetId(nodeMap.get("drawworks"));
        dto.setTopDriveAssetId(nodeMap.get("top_drive"));
        dto.setMudPump1AssetId(nodeMap.get("mud_pump_1"));
        dto.setMudPump2AssetId(nodeMap.get("mud_pump_2"));
        dto.setMudPump3AssetId(nodeMap.get("mud_pump_3"));
        dto.setMudSystemAssetId(nodeMap.get("mud_system"));
        dto.setBopStackAssetId(nodeMap.get("bop_stack"));
        dto.setGasDetectorAssetId(nodeMap.get("gas_detector"));

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("Drilling rig created from template: {} with asset ID: {}", rigCode, assetId);

        return getById(assetId);
    }

    /**
     * Create a rig directly (without template)
     */
    public DrRigDto create(UUID tenantId, DrRigDto dto) {
        log.info("Creating new drilling rig: {}", dto.getRigCode());

        if (dto.getRigCode() == null || dto.getRigCode().isEmpty()) {
            throw new DrBusinessException("Rig code is required");
        }

        // Check if rig code already exists
        try {
            getByCode(tenantId, dto.getRigCode());
            throw new DrBusinessException("Rig code already exists: " + dto.getRigCode());
        } catch (DrEntityNotFoundException e) {
            // Good, code doesn't exist
        }

        // Create the asset
        String assetName = dto.getRigName() != null ? dto.getRigName() : dto.getRigCode();
        Asset asset = assetService.createAsset(tenantId, DrRigDto.ASSET_TYPE, assetName, dto.getRigCode());

        UUID assetId = asset.getId().getId();
        dto.setAssetId(assetId);
        dto.setTenantId(tenantId);

        // Set defaults
        if (dto.getOperationalStatus() == null) {
            dto.setOperationalStatus("STANDBY");
        }
        if (dto.getTotalWellsDrilled() == null) {
            dto.setTotalWellsDrilled(0);
        }
        if (dto.getTotalFootageDrilledFt() == null) {
            dto.setTotalFootageDrilledFt(BigDecimal.ZERO);
        }
        if (dto.getTotalNptHours() == null) {
            dto.setTotalNptHours(BigDecimal.ZERO);
        }
        if (dto.getTotalOperationalHours() == null) {
            dto.setTotalOperationalHours(BigDecimal.ZERO);
        }

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("Drilling rig created: {}", assetId);

        return getById(assetId);
    }

    // --- Update Operations ---

    /**
     * Update a rig
     */
    public DrRigDto update(UUID assetId, DrRigDto updatedDto) {
        log.info("Updating drilling rig: {}", assetId);

        // Verify it exists
        DrRigDto existingRig = getById(assetId);

        // Check for duplicate rig code
        if (updatedDto.getRigCode() != null &&
            !existingRig.getRigCode().equals(updatedDto.getRigCode())) {
            try {
                getByCode(existingRig.getTenantId(), updatedDto.getRigCode());
                throw new DrBusinessException("Rig code already exists: " + updatedDto.getRigCode());
            } catch (DrEntityNotFoundException e) {
                // Good, code doesn't exist
            }
        }

        // Update asset name if rigName changed
        if (updatedDto.getRigName() != null && !updatedDto.getRigName().equals(existingRig.getRigName())) {
            Asset asset = assetService.getAssetById(assetId).orElseThrow();
            asset.setName(updatedDto.getRigName());
            assetService.updateAsset(asset);
        }

        // Save updated attributes
        attributeService.saveServerAttributes(assetId, updatedDto.toAttributeMap());

        log.info("Drilling rig updated: {}", assetId);

        return getById(assetId);
    }

    /**
     * Update rig status
     */
    public DrRigDto updateStatus(UUID assetId, String newStatus) {
        log.info("Updating rig {} status to {}", assetId, newStatus);

        // Verify it exists
        getById(assetId);

        // Update status attribute
        attributeService.saveServerAttribute(assetId, DrRigDto.ATTR_OPERATIONAL_STATUS, newStatus);

        return getById(assetId);
    }

    /**
     * Update rig location
     */
    public DrRigDto updateLocation(UUID assetId, String location, BigDecimal latitude, BigDecimal longitude) {
        log.info("Updating rig {} location to {}", assetId, location);

        // Verify it exists
        getById(assetId);

        // Update location attributes
        Map<String, Object> attrs = Map.of(
            DrRigDto.ATTR_CURRENT_LOCATION, location,
            DrRigDto.ATTR_LATITUDE, latitude,
            DrRigDto.ATTR_LONGITUDE, longitude
        );
        attributeService.saveServerAttributes(assetId, attrs);

        return getById(assetId);
    }

    // --- Well Assignment Operations ---

    /**
     * Assign rig to a well
     */
    public DrRigDto assignToWell(UUID assetId, UUID wellId) {
        log.info("Assigning rig {} to well {}", assetId, wellId);

        DrRigDto rig = getById(assetId);

        if (rig.getCurrentWellId() != null) {
            throw new DrBusinessException("Rig is already assigned to a well. Release it first.");
        }

        // TODO: Check if another rig is assigned to this well

        Map<String, Object> attrs = Map.of(
            DrRigDto.ATTR_CURRENT_WELL_ID, wellId.toString(),
            DrRigDto.ATTR_OPERATIONAL_STATUS, "RIG_UP"
        );
        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Rig assigned to well successfully");
        return getById(assetId);
    }

    /**
     * Release rig from well
     */
    public DrRigDto releaseFromWell(UUID assetId) {
        log.info("Releasing rig {} from well", assetId);

        DrRigDto rig = getById(assetId);

        if (rig.getCurrentWellId() == null) {
            throw new DrBusinessException("Rig is not assigned to any well");
        }

        // Update statistics
        int newWellCount = (rig.getTotalWellsDrilled() != null ? rig.getTotalWellsDrilled() : 0) + 1;

        Map<String, Object> attrs = Map.of(
            DrRigDto.ATTR_CURRENT_WELL_ID, "",
            DrRigDto.ATTR_CURRENT_RUN_ID, "",
            DrRigDto.ATTR_OPERATIONAL_STATUS, "RIG_DOWN",
            DrRigDto.ATTR_TOTAL_WELLS_DRILLED, newWellCount
        );
        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Rig released from well successfully");
        return getById(assetId);
    }

    // --- BOP Test Operations ---

    /**
     * Record a BOP test
     */
    public DrRigDto recordBopTest(UUID assetId, Long testDate, String notes) {
        log.info("Recording BOP test for rig {}", assetId);

        // Verify it exists
        DrRigDto rig = getById(assetId);

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put(DrRigDto.ATTR_BOP_TEST_DATE, testDate);

        if (notes != null) {
            String existingNotes = rig.getNotes() != null ? rig.getNotes() : "";
            String newNotes = existingNotes + "\n[" + System.currentTimeMillis() + "] BOP Test: " + notes;
            attrs.put(DrRigDto.ATTR_NOTES, newNotes);
        }

        attributeService.saveServerAttributes(assetId, attrs);

        log.info("BOP test recorded successfully");
        return getById(assetId);
    }

    // --- Delete Operations ---

    /**
     * Delete a rig
     */
    public void delete(UUID tenantId, UUID assetId) {
        log.info("Deleting drilling rig: {}", assetId);

        DrRigDto rig = getById(assetId);

        if (rig.getCurrentWellId() != null) {
            throw new DrBusinessException("Cannot delete rig assigned to a well. Release it first.");
        }

        assetService.deleteAsset(tenantId, assetId);
        log.info("Drilling rig deleted: {}", assetId);
    }

    // --- Statistics ---

    /**
     * Count rigs by status
     */
    public long countByStatus(UUID tenantId, String status) {
        return getByStatus(tenantId, status).size();
    }

    /**
     * Count rigs by type
     */
    public long countByType(UUID tenantId, String rigType) {
        return getByType(tenantId, rigType).size();
    }

    /**
     * Count all rigs for a tenant
     */
    public long countByTenant(UUID tenantId) {
        return assetService.countByType(tenantId, DrRigDto.ASSET_TYPE);
    }
}
