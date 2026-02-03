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
package org.thingsboard.nexus.ct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.ct.dto.CTReelDto;
import org.thingsboard.nexus.ct.dto.CTUnitDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.model.UnitStatus;
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
 * Service for managing CT Units as ThingsBoard Assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CTUnitService {

    private final CTAssetService assetService;
    private final CTAttributeService attributeService;
    private final CTTemplateService templateService;

    public CTUnitDto getById(UUID assetId) {
        log.debug("Getting CT Unit by asset id: {}", assetId);
        Asset asset = assetService.getAssetById(assetId)
                .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", assetId.toString()));
        List<AttributeKvEntry> attributes = attributeService.getServerAttributes(assetId);
        return CTUnitDto.fromAssetAndAttributes(asset, attributes);
    }

    public CTUnitDto getByCode(UUID tenantId, String unitCode) {
        log.debug("Getting CT Unit by code: {}", unitCode);
        Page<Asset> assets = assetService.searchAssetsByName(tenantId, CTUnitDto.ASSET_TYPE, unitCode, 0, 100);

        for (Asset asset : assets.getContent()) {
            List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
            for (AttributeKvEntry attr : attributes) {
                if (CTUnitDto.ATTR_UNIT_CODE.equals(attr.getKey()) &&
                    attr.getStrValue().orElse("").equals(unitCode)) {
                    return CTUnitDto.fromAssetAndAttributes(asset, attributes);
                }
            }
        }
        throw new CTEntityNotFoundException("CT Unit", unitCode);
    }

    public Page<CTUnitDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting CT Units for tenant: {}", tenantId);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTUnitDto.ASSET_TYPE,
                pageable.getPageNumber(), pageable.getPageSize());
        return assets.map(asset -> {
            List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
            return CTUnitDto.fromAssetAndAttributes(asset, attributes);
        });
    }

    public List<CTUnitDto> getByStatus(UUID tenantId, UnitStatus status) {
        log.debug("Getting CT Units by status - tenant: {}, status: {}", tenantId, status);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTUnitDto.ASSET_TYPE, 0, 1000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return CTUnitDto.fromAssetAndAttributes(asset, attributes);
                })
                .filter(dto -> dto.getOperationalStatus() == status)
                .collect(Collectors.toList());
    }

    public CTUnitDto create(UUID tenantId, CTUnitDto dto) {
        log.info("Creating new CT Unit: {}", dto.getUnitCode());

        // Check for duplicate unit code
        try {
            getByCode(tenantId, dto.getUnitCode());
            throw new CTBusinessException("Unit code already exists: " + dto.getUnitCode());
        } catch (CTEntityNotFoundException e) {
            // Expected - unit code doesn't exist
        }

        // Create asset
        String assetName = "CT-UNIT-" + dto.getUnitCode();
        Asset asset = assetService.createAsset(tenantId, CTUnitDto.ASSET_TYPE, assetName, dto.getUnitCode());
        UUID assetId = asset.getId().getId();

        // Set default status if not provided
        if (dto.getOperationalStatus() == null) {
            dto.setOperationalStatus(UnitStatus.OPERATIONAL);
        }

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("CT Unit created successfully: {}", assetId);
        return getById(assetId);
    }

    public CTUnitDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        log.info("Creating CT Unit from template {} for tenant {}", request.getTemplateId(), tenantId);

        Map<String, Object> variables = request.getVariables();

        String unitCode = (String) variables.get("unitCode");
        if (unitCode == null || unitCode.isEmpty()) {
            throw new CTBusinessException("Unit code is required");
        }

        // Check for duplicate
        try {
            getByCode(tenantId, unitCode);
            throw new CTBusinessException("Unit code already exists: " + unitCode);
        } catch (CTEntityNotFoundException e) {
            // Expected
        }

        // Instantiate template
        TemplateInstanceResult instanceResult = templateService.instantiateTemplate(
                new TenantId(tenantId),
                request.getTemplateId(),
                variables,
                tenantId
        );

        UUID assetId = instanceResult.getRootAssetId();

        // Build DTO from template variables
        CTUnitDto dto = new CTUnitDto();
        dto.setUnitCode(unitCode);
        dto.setUnitName((String) variables.get("unitName"));
        dto.setManufacturer((String) variables.get("manufacturer"));
        dto.setModel((String) variables.get("model"));
        dto.setSerialNumber((String) variables.get("serialNumber"));

        Object yearManufactured = variables.get("yearManufactured");
        if (yearManufactured != null) {
            dto.setYearManufactured(yearManufactured instanceof Integer ?
                    (Integer) yearManufactured : Integer.parseInt(yearManufactured.toString()));
        }

        Object maxPressure = variables.get("maxPressurePsi");
        if (maxPressure != null) {
            dto.setMaxPressurePsi(maxPressure instanceof Integer ?
                    (Integer) maxPressure : Integer.parseInt(maxPressure.toString()));
        }

        Object maxTension = variables.get("maxTensionLbf");
        if (maxTension != null) {
            dto.setMaxTensionLbf(maxTension instanceof Integer ?
                    (Integer) maxTension : Integer.parseInt(maxTension.toString()));
        }

        Object maxSpeed = variables.get("maxSpeedFtMin");
        if (maxSpeed != null) {
            dto.setMaxSpeedFtMin(maxSpeed instanceof Integer ?
                    (Integer) maxSpeed : Integer.parseInt(maxSpeed.toString()));
        }

        Object maxTubingOD = variables.get("maxTubingOD");
        if (maxTubingOD != null) {
            dto.setMaxTubingOdInch(maxTubingOD instanceof Double ?
                    BigDecimal.valueOf((Double) maxTubingOD) :
                    BigDecimal.valueOf(Double.parseDouble(maxTubingOD.toString())));
        }

        dto.setOperationalStatus(UnitStatus.OPERATIONAL);
        dto.setCurrentLocation((String) variables.get("location"));

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("CT Unit created from template successfully: {} with asset ID: {}", unitCode, assetId);
        return getById(assetId);
    }

    public CTUnitDto update(UUID assetId, CTUnitDto dto) {
        log.info("Updating CT Unit: {}", assetId);

        CTUnitDto existing = getById(assetId);

        // Check for duplicate unit code if changing
        if (!existing.getUnitCode().equals(dto.getUnitCode())) {
            try {
                getByCode(existing.getTenantId(), dto.getUnitCode());
                throw new CTBusinessException("Unit code already exists: " + dto.getUnitCode());
            } catch (CTEntityNotFoundException e) {
                // Expected
            }
        }

        // Update attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("CT Unit updated successfully: {}", assetId);
        return getById(assetId);
    }

    public void delete(UUID tenantId, UUID assetId) {
        log.info("Deleting CT Unit: {}", assetId);

        CTUnitDto unit = getById(assetId);

        if (unit.getCurrentReelId() != null) {
            throw new CTBusinessException("Cannot delete unit with assigned reel. Detach reel first.");
        }

        assetService.deleteAsset(tenantId, assetId);
        log.info("CT Unit deleted successfully: {}", assetId);
    }

    public CTUnitDto updateStatus(UUID assetId, UnitStatus newStatus) {
        log.info("Updating unit {} status to {}", assetId, newStatus);

        getById(assetId); // Verify exists

        attributeService.saveServerAttribute(assetId, CTUnitDto.ATTR_OPERATIONAL_STATUS, newStatus.name());

        log.info("Unit status updated successfully");
        return getById(assetId);
    }

    public CTUnitDto updateLocation(UUID assetId, String location, Double latitude, Double longitude) {
        log.info("Updating unit {} location to {}", assetId, location);

        getById(assetId); // Verify exists

        Map<String, Object> attrs = new java.util.HashMap<>();
        if (location != null) {
            attrs.put(CTUnitDto.ATTR_CURRENT_LOCATION, location);
        }
        if (latitude != null) {
            attrs.put(CTUnitDto.ATTR_LATITUDE, BigDecimal.valueOf(latitude));
        }
        if (longitude != null) {
            attrs.put(CTUnitDto.ATTR_LONGITUDE, BigDecimal.valueOf(longitude));
        }

        if (!attrs.isEmpty()) {
            attributeService.saveServerAttributes(assetId, attrs);
        }

        log.info("Unit location updated successfully");
        return getById(assetId);
    }

    public CTUnitDto assignReel(UUID unitAssetId, UUID reelAssetId, CTReelDto reelDto) {
        log.info("Assigning reel {} to unit {}", reelAssetId, unitAssetId);

        CTUnitDto unit = getById(unitAssetId);

        if (unit.getCurrentReelId() != null) {
            throw new CTBusinessException("Unit already has a reel assigned. Detach current reel first.");
        }

        if (!reelDto.getStatus().equals(ReelStatus.AVAILABLE)) {
            throw new CTBusinessException("Reel is not available for assignment. Status: " + reelDto.getStatus());
        }

        // Update unit
        Map<String, Object> unitAttrs = new java.util.HashMap<>();
        unitAttrs.put(CTUnitDto.ATTR_CURRENT_REEL_ID, reelAssetId.toString());
        unitAttrs.put(CTUnitDto.ATTR_CURRENT_REEL_CODE, reelDto.getReelCode());
        unitAttrs.put(CTUnitDto.ATTR_REEL_COUPLED_DATE, System.currentTimeMillis());
        attributeService.saveServerAttributes(unitAssetId, unitAttrs);

        // Update reel
        Map<String, Object> reelAttrs = new java.util.HashMap<>();
        reelAttrs.put(CTReelDto.ATTR_STATUS, ReelStatus.IN_USE.name());
        reelAttrs.put(CTReelDto.ATTR_CURRENT_UNIT_ID, unitAssetId.toString());
        reelAttrs.put(CTReelDto.ATTR_CURRENT_UNIT_CODE, unit.getUnitCode());
        attributeService.saveServerAttributes(reelAssetId, reelAttrs);

        log.info("Reel assigned successfully");
        return getById(unitAssetId);
    }

    public CTUnitDto detachReel(UUID unitAssetId) {
        log.info("Detaching reel from unit {}", unitAssetId);

        CTUnitDto unit = getById(unitAssetId);

        if (unit.getCurrentReelId() == null) {
            throw new CTBusinessException("Unit has no reel assigned");
        }

        UUID reelAssetId = unit.getCurrentReelId();

        // Clear unit's reel reference
        Map<String, Object> unitAttrs = new java.util.HashMap<>();
        unitAttrs.put(CTUnitDto.ATTR_CURRENT_REEL_ID, "");
        unitAttrs.put(CTUnitDto.ATTR_CURRENT_REEL_CODE, "");
        unitAttrs.put(CTUnitDto.ATTR_REEL_COUPLED_DATE, 0L);
        attributeService.saveServerAttributes(unitAssetId, unitAttrs);

        // Update reel status
        Map<String, Object> reelAttrs = new java.util.HashMap<>();
        reelAttrs.put(CTReelDto.ATTR_STATUS, ReelStatus.AVAILABLE.name());
        reelAttrs.put(CTReelDto.ATTR_CURRENT_UNIT_ID, "");
        reelAttrs.put(CTReelDto.ATTR_CURRENT_UNIT_CODE, "");
        attributeService.saveServerAttributes(reelAssetId, reelAttrs);

        log.info("Reel detached successfully");
        return getById(unitAssetId);
    }

    public CTUnitDto recordMaintenance(UUID assetId, Long maintenanceDate, String notes) {
        log.info("Recording maintenance for unit {}", assetId);

        CTUnitDto unit = getById(assetId);

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put(CTUnitDto.ATTR_LAST_MAINTENANCE_DATE, maintenanceDate);

        if (notes != null) {
            String existingNotes = unit.getNotes() != null ? unit.getNotes() : "";
            attrs.put(CTUnitDto.ATTR_NOTES, existingNotes + "\n[" + System.currentTimeMillis() + "] Maintenance: " + notes);
        }

        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Maintenance recorded successfully");
        return getById(assetId);
    }

    public List<CTUnitDto> getAvailableUnits(UUID tenantId) {
        log.debug("Getting available CT Units for tenant: {}", tenantId);
        return getByStatus(tenantId, UnitStatus.OPERATIONAL);
    }

    public List<CTUnitDto> getUnitsRequiringMaintenance(UUID tenantId) {
        log.debug("Getting CT Units requiring maintenance for tenant: {}", tenantId);
        Long now = System.currentTimeMillis();
        Long thirtyDaysAgo = now - (30L * 24L * 60L * 60L * 1000L);

        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTUnitDto.ASSET_TYPE, 0, 1000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return CTUnitDto.fromAssetAndAttributes(asset, attributes);
                })
                .filter(unit -> unit.getLastMaintenanceDate() == null ||
                        unit.getLastMaintenanceDate() < thirtyDaysAgo)
                .collect(Collectors.toList());
    }

    public long countByStatus(UUID tenantId, UnitStatus status) {
        return getByStatus(tenantId, status).size();
    }

    public long countByTenant(UUID tenantId) {
        return assetService.countAssetsByType(tenantId, CTUnitDto.ASSET_TYPE);
    }
}
