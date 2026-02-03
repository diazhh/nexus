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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.dr.dto.DrBhaDto;
import org.thingsboard.nexus.dr.exception.DrBusinessException;
import org.thingsboard.nexus.dr.exception.DrEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;
import org.thingsboard.server.common.data.template.TemplateInstanceResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Bottom Hole Assemblies (BHAs).
 * Uses ThingsBoard Assets as the underlying storage mechanism.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrBhaService {

    private final DrAssetService assetService;
    private final DrAttributeService attributeService;
    private final DrTemplateService templateService;

    // --- Query Operations ---

    /**
     * Get a BHA by its asset ID
     */
    public DrBhaDto getById(UUID assetId) {
        log.debug("Getting BHA by asset id: {}", assetId);
        Asset asset = assetService.getAssetById(assetId)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", assetId.toString()));

        if (!DrBhaDto.ASSET_TYPE.equals(asset.getType())) {
            throw new DrEntityNotFoundException("BHA", assetId.toString());
        }

        List<AttributeKvEntry> attributes = attributeService.getServerAttributes(assetId);
        return DrBhaDto.fromAssetAndAttributes(asset, attributes);
    }

    /**
     * Get a BHA by its number (searches in attributes)
     */
    public DrBhaDto getByNumber(UUID tenantId, String bhaNumber) {
        log.debug("Getting BHA by number: {}", bhaNumber);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, DrBhaDto.ASSET_TYPE, 0, 1000);

        for (Asset asset : assets.getContent()) {
            List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
            for (AttributeKvEntry attr : attributes) {
                if (DrBhaDto.ATTR_BHA_NUMBER.equals(attr.getKey()) &&
                    bhaNumber.equals(attr.getStrValue().orElse(null))) {
                    return DrBhaDto.fromAssetAndAttributes(asset, attributes);
                }
            }
        }

        throw new DrEntityNotFoundException("BHA", bhaNumber);
    }

    /**
     * Get all BHAs for a tenant with pagination
     */
    public Page<DrBhaDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting BHAs for tenant: {}", tenantId);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, DrBhaDto.ASSET_TYPE,
                pageable.getPageNumber(), pageable.getPageSize());

        List<DrBhaDto> dtos = assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return DrBhaDto.fromAssetAndAttributes(asset, attributes);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, assets.getTotalElements());
    }

    /**
     * Get all BHAs for a tenant (no pagination)
     */
    public List<DrBhaDto> getAllByTenant(UUID tenantId) {
        log.debug("Getting all BHAs for tenant: {}", tenantId);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, DrBhaDto.ASSET_TYPE, 0, 10000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return DrBhaDto.fromAssetAndAttributes(asset, attributes);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get available BHAs (status = AVAILABLE)
     */
    public List<DrBhaDto> getAvailableBhas(UUID tenantId) {
        log.debug("Getting available BHAs for tenant: {}", tenantId);
        return getAllByTenant(tenantId).stream()
                .filter(bha -> "AVAILABLE".equals(bha.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get available directional BHAs
     */
    public List<DrBhaDto> getAvailableDirectionalBhas(UUID tenantId) {
        log.debug("Getting available directional BHAs for tenant: {}", tenantId);
        return getAllByTenant(tenantId).stream()
                .filter(bha -> "AVAILABLE".equals(bha.getStatus()) && Boolean.TRUE.equals(bha.getIsDirectional()))
                .collect(Collectors.toList());
    }

    /**
     * Get BHAs by status
     */
    public List<DrBhaDto> getByStatus(UUID tenantId, String status) {
        log.debug("Getting BHAs by status - tenant: {}, status: {}", tenantId, status);
        return getAllByTenant(tenantId).stream()
                .filter(bha -> status.equals(bha.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get BHAs by type
     */
    public List<DrBhaDto> getByType(UUID tenantId, String bhaType) {
        log.debug("Getting BHAs by type - tenant: {}, type: {}", tenantId, bhaType);
        return getAllByTenant(tenantId).stream()
                .filter(bha -> bhaType.equals(bha.getBhaType()))
                .collect(Collectors.toList());
    }

    /**
     * Get BHAs by bit size
     */
    public List<DrBhaDto> getByBitSize(UUID tenantId, BigDecimal bitSizeIn) {
        log.debug("Getting BHAs by bit size - tenant: {}, size: {}", tenantId, bitSizeIn);
        return getAllByTenant(tenantId).stream()
                .filter(bha -> bitSizeIn.equals(bha.getBitSizeIn()))
                .collect(Collectors.toList());
    }

    /**
     * Search BHAs by name
     */
    public Page<DrBhaDto> searchByName(UUID tenantId, String searchText, Pageable pageable) {
        log.debug("Searching BHAs by name: {}", searchText);
        Page<Asset> assets = assetService.searchAssetsByName(tenantId, DrBhaDto.ASSET_TYPE, searchText,
                pageable.getPageNumber(), pageable.getPageSize());

        List<DrBhaDto> dtos = assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return DrBhaDto.fromAssetAndAttributes(asset, attributes);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, assets.getTotalElements());
    }

    // --- Create Operations ---

    /**
     * Create a BHA from a template
     */
    public DrBhaDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        return createFromTemplate(tenantId, request.getTemplateId(), request.getVariables(), tenantId);
    }

    /**
     * Create a BHA from a template with variables
     */
    public DrBhaDto createFromTemplate(UUID tenantId, UUID templateId, Map<String, Object> variables, UUID createdBy) {
        log.info("Creating BHA from template {} for tenant {}", templateId, tenantId);

        String bhaNumber = (String) variables.get("bhaNumber");
        if (bhaNumber == null || bhaNumber.isEmpty()) {
            throw new DrBusinessException("BHA number is required");
        }

        // Check if BHA number already exists
        try {
            getByNumber(tenantId, bhaNumber);
            throw new DrBusinessException("BHA number already exists: " + bhaNumber);
        } catch (DrEntityNotFoundException e) {
            // Good, number doesn't exist
        }

        // Instantiate template to create digital twin asset
        TemplateInstanceResult instanceResult = templateService.instantiateTemplate(
                new TenantId(tenantId),
                templateId,
                variables,
                createdBy
        );

        UUID assetId = instanceResult.getRootAssetId();

        // Build DTO from variables
        DrBhaDto dto = new DrBhaDto();
        dto.setAssetId(assetId);
        dto.setTenantId(tenantId);
        dto.setBhaNumber(bhaNumber);
        dto.setBhaType((String) variables.get("bhaType"));
        dto.setStatus("AVAILABLE");

        if (variables.get("isDirectional") != null) {
            dto.setIsDirectional(Boolean.parseBoolean(variables.get("isDirectional").toString()));
        }

        // Bit information
        dto.setBitSerial((String) variables.get("bitSerial"));
        dto.setBitType((String) variables.get("bitType"));
        dto.setBitIadcCode((String) variables.get("bitIadcCode"));
        dto.setBitManufacturer((String) variables.get("bitManufacturer"));
        dto.setBitModel((String) variables.get("bitModel"));
        dto.setBitNozzles((String) variables.get("bitNozzles"));

        if (variables.get("bitSizeIn") != null) {
            dto.setBitSizeIn(new BigDecimal(variables.get("bitSizeIn").toString()));
        }
        if (variables.get("bitTfaSqIn") != null) {
            dto.setBitTfaSqIn(new BigDecimal(variables.get("bitTfaSqIn").toString()));
        }

        // Motor information
        dto.setMotorManufacturer((String) variables.get("motorManufacturer"));
        dto.setMotorModel((String) variables.get("motorModel"));
        dto.setMotorLobeConfiguration((String) variables.get("motorLobeConfiguration"));

        if (variables.get("motorOdIn") != null) {
            dto.setMotorOdIn(new BigDecimal(variables.get("motorOdIn").toString()));
        }
        if (variables.get("motorBendAngleDeg") != null) {
            dto.setMotorBendAngleDeg(new BigDecimal(variables.get("motorBendAngleDeg").toString()));
        }

        // Initialize statistics
        dto.setTotalFootageDrilled(BigDecimal.ZERO);
        dto.setTotalHoursOnBottom(BigDecimal.ZERO);
        dto.setTotalRuns(0);

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("BHA created from template: {} with asset ID: {}", bhaNumber, assetId);

        return getById(assetId);
    }

    /**
     * Create a BHA directly (without template)
     */
    public DrBhaDto create(UUID tenantId, DrBhaDto dto) {
        log.info("Creating new BHA: {}", dto.getBhaNumber());

        if (dto.getBhaNumber() == null || dto.getBhaNumber().isEmpty()) {
            throw new DrBusinessException("BHA number is required");
        }

        // Check if BHA number already exists
        try {
            getByNumber(tenantId, dto.getBhaNumber());
            throw new DrBusinessException("BHA number already exists: " + dto.getBhaNumber());
        } catch (DrEntityNotFoundException e) {
            // Good, number doesn't exist
        }

        // Create the asset
        String assetName = dto.getBhaNumber();
        Asset asset = assetService.createAsset(tenantId, DrBhaDto.ASSET_TYPE, assetName, dto.getBhaNumber());

        UUID assetId = asset.getId().getId();
        dto.setAssetId(assetId);
        dto.setTenantId(tenantId);

        // Set defaults
        if (dto.getStatus() == null) {
            dto.setStatus("AVAILABLE");
        }
        if (dto.getTotalFootageDrilled() == null) {
            dto.setTotalFootageDrilled(BigDecimal.ZERO);
        }
        if (dto.getTotalHoursOnBottom() == null) {
            dto.setTotalHoursOnBottom(BigDecimal.ZERO);
        }
        if (dto.getTotalRuns() == null) {
            dto.setTotalRuns(0);
        }

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("BHA created: {}", assetId);

        return getById(assetId);
    }

    // --- Update Operations ---

    /**
     * Update a BHA
     */
    public DrBhaDto update(UUID assetId, DrBhaDto updatedDto) {
        log.info("Updating BHA: {}", assetId);

        // Verify it exists
        DrBhaDto existingBha = getById(assetId);

        // Check for duplicate BHA number
        if (updatedDto.getBhaNumber() != null &&
            !existingBha.getBhaNumber().equals(updatedDto.getBhaNumber())) {
            try {
                getByNumber(existingBha.getTenantId(), updatedDto.getBhaNumber());
                throw new DrBusinessException("BHA number already exists: " + updatedDto.getBhaNumber());
            } catch (DrEntityNotFoundException e) {
                // Good, number doesn't exist
            }
        }

        // Update asset name if changed
        if (updatedDto.getBhaNumber() != null && !updatedDto.getBhaNumber().equals(existingBha.getBhaNumber())) {
            Asset asset = assetService.getAssetById(assetId).orElseThrow();
            asset.setName(updatedDto.getBhaNumber());
            assetService.updateAsset(asset);
        }

        // Save updated attributes
        attributeService.saveServerAttributes(assetId, updatedDto.toAttributeMap());

        log.info("BHA updated: {}", assetId);

        return getById(assetId);
    }

    /**
     * Update BHA status
     */
    public DrBhaDto updateStatus(UUID assetId, String newStatus) {
        log.info("Updating BHA {} status to {}", assetId, newStatus);

        // Verify it exists
        getById(assetId);

        // Update status attribute
        attributeService.saveServerAttribute(assetId, DrBhaDto.ATTR_STATUS, newStatus);

        return getById(assetId);
    }

    /**
     * Record dull grade for a BHA
     */
    public DrBhaDto recordDullGrade(UUID assetId, String dullInner, String dullOuter, String dullChar,
                                    String dullLocation, String bearingCondition, String gaugeCondition,
                                    String reasonPulled) {
        log.info("Recording dull grade for BHA: {}", assetId);

        // Verify it exists
        getById(assetId);

        Map<String, Object> attrs = new HashMap<>();
        if (dullInner != null) attrs.put(DrBhaDto.ATTR_BIT_DULL_INNER, dullInner);
        if (dullOuter != null) attrs.put(DrBhaDto.ATTR_BIT_DULL_OUTER, dullOuter);
        if (dullChar != null) attrs.put(DrBhaDto.ATTR_BIT_DULL_CHAR, dullChar);
        if (dullLocation != null) attrs.put(DrBhaDto.ATTR_BIT_DULL_LOCATION, dullLocation);
        if (bearingCondition != null) attrs.put(DrBhaDto.ATTR_BIT_BEARING_CONDITION, bearingCondition);
        if (gaugeCondition != null) attrs.put(DrBhaDto.ATTR_BIT_GAUGE_CONDITION, gaugeCondition);
        if (reasonPulled != null) attrs.put(DrBhaDto.ATTR_BIT_REASON_PULLED, reasonPulled);

        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Dull grade recorded for BHA: {}", assetId);
        return getById(assetId);
    }

    /**
     * Update BHA statistics (add footage and hours)
     */
    public DrBhaDto updateStatistics(UUID assetId, BigDecimal footageDrilled, BigDecimal hoursOnBottom) {
        log.info("Updating statistics for BHA: {}", assetId);

        DrBhaDto bha = getById(assetId);

        Map<String, Object> attrs = new HashMap<>();

        if (footageDrilled != null) {
            BigDecimal currentFootage = bha.getTotalFootageDrilled() != null ? bha.getTotalFootageDrilled() : BigDecimal.ZERO;
            attrs.put(DrBhaDto.ATTR_TOTAL_FOOTAGE_DRILLED, currentFootage.add(footageDrilled));
        }

        if (hoursOnBottom != null) {
            BigDecimal currentHours = bha.getTotalHoursOnBottom() != null ? bha.getTotalHoursOnBottom() : BigDecimal.ZERO;
            attrs.put(DrBhaDto.ATTR_TOTAL_HOURS_ON_BOTTOM, currentHours.add(hoursOnBottom));
        }

        attributeService.saveServerAttributes(assetId, attrs);

        return getById(assetId);
    }

    /**
     * Increment run count for a BHA
     */
    public DrBhaDto incrementRunCount(UUID assetId) {
        log.info("Incrementing run count for BHA: {}", assetId);

        DrBhaDto bha = getById(assetId);

        int currentRuns = bha.getTotalRuns() != null ? bha.getTotalRuns() : 0;
        attributeService.saveServerAttribute(assetId, DrBhaDto.ATTR_TOTAL_RUNS, currentRuns + 1);

        return getById(assetId);
    }

    // --- Delete Operations ---

    /**
     * Delete a BHA
     */
    public void delete(UUID tenantId, UUID assetId) {
        log.info("Deleting BHA: {}", assetId);

        DrBhaDto bha = getById(assetId);

        if ("IN_USE".equals(bha.getStatus())) {
            throw new DrBusinessException("Cannot delete BHA that is currently in use");
        }

        assetService.deleteAsset(tenantId, assetId);
        log.info("BHA deleted: {}", assetId);
    }

    // --- Statistics ---

    /**
     * Count BHAs by status
     */
    public long countByStatus(UUID tenantId, String status) {
        return getByStatus(tenantId, status).size();
    }

    /**
     * Count BHAs by type
     */
    public long countByType(UUID tenantId, String bhaType) {
        return getByType(tenantId, bhaType).size();
    }

    /**
     * Count all BHAs for a tenant
     */
    public long countByTenant(UUID tenantId) {
        return assetService.countByType(tenantId, DrBhaDto.ASSET_TYPE);
    }
}
