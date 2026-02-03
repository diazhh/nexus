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
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.ReelStatus;
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
 * Service for managing CT Reels as ThingsBoard Assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CTReelService {

    private final CTAssetService assetService;
    private final CTAttributeService attributeService;
    private final CTTemplateService templateService;

    public CTReelDto getById(UUID assetId) {
        log.debug("Getting CT Reel by asset id: {}", assetId);
        Asset asset = assetService.getAssetById(assetId)
                .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", assetId.toString()));
        List<AttributeKvEntry> attributes = attributeService.getServerAttributes(assetId);
        return CTReelDto.fromAssetAndAttributes(asset, attributes);
    }

    public CTReelDto getByCode(UUID tenantId, String reelCode) {
        log.debug("Getting CT Reel by code: {}", reelCode);
        Page<Asset> assets = assetService.searchAssetsByName(tenantId, CTReelDto.ASSET_TYPE, reelCode, 0, 100);

        for (Asset asset : assets.getContent()) {
            List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
            for (AttributeKvEntry attr : attributes) {
                if (CTReelDto.ATTR_REEL_CODE.equals(attr.getKey()) &&
                    attr.getStrValue().orElse("").equals(reelCode)) {
                    return CTReelDto.fromAssetAndAttributes(asset, attributes);
                }
            }
        }
        throw new CTEntityNotFoundException("CT Reel", reelCode);
    }

    public Page<CTReelDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting CT Reels for tenant: {}", tenantId);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTReelDto.ASSET_TYPE,
                pageable.getPageNumber(), pageable.getPageSize());
        return assets.map(asset -> {
            List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
            return CTReelDto.fromAssetAndAttributes(asset, attributes);
        });
    }

    public List<CTReelDto> getByStatus(UUID tenantId, ReelStatus status) {
        log.debug("Getting CT Reels by status - tenant: {}, status: {}", tenantId, status);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTReelDto.ASSET_TYPE, 0, 1000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return CTReelDto.fromAssetAndAttributes(asset, attributes);
                })
                .filter(dto -> dto.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<CTReelDto> getAvailableReelsBySize(UUID tenantId, BigDecimal odInch, BigDecimal maxFatigue) {
        log.debug("Getting available reels - tenant: {}, OD: {}, maxFatigue: {}", tenantId, odInch, maxFatigue);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTReelDto.ASSET_TYPE, 0, 1000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return CTReelDto.fromAssetAndAttributes(asset, attributes);
                })
                .filter(dto -> dto.getStatus() == ReelStatus.AVAILABLE)
                .filter(dto -> odInch == null || (dto.getTubingOdInch() != null && dto.getTubingOdInch().compareTo(odInch) == 0))
                .filter(dto -> maxFatigue == null || (dto.getAccumulatedFatiguePercent() != null && dto.getAccumulatedFatiguePercent().compareTo(maxFatigue) <= 0))
                .collect(Collectors.toList());
    }

    public List<CTReelDto> getReelsAboveFatigueThreshold(UUID tenantId, BigDecimal threshold) {
        log.debug("Getting reels above fatigue threshold - tenant: {}, threshold: {}", tenantId, threshold);
        Page<Asset> assets = assetService.getAssetsByType(tenantId, CTReelDto.ASSET_TYPE, 0, 1000);

        return assets.getContent().stream()
                .map(asset -> {
                    List<AttributeKvEntry> attributes = attributeService.getServerAttributes(asset.getId().getId());
                    return CTReelDto.fromAssetAndAttributes(asset, attributes);
                })
                .filter(dto -> dto.getAccumulatedFatiguePercent() != null &&
                        dto.getAccumulatedFatiguePercent().compareTo(threshold) >= 0)
                .collect(Collectors.toList());
    }

    public CTReelDto create(UUID tenantId, CTReelDto dto) {
        log.info("Creating new CT Reel: {}", dto.getReelCode());

        // Check for duplicate reel code
        try {
            getByCode(tenantId, dto.getReelCode());
            throw new CTBusinessException("Reel code already exists: " + dto.getReelCode());
        } catch (CTEntityNotFoundException e) {
            // Expected - reel code doesn't exist
        }

        // Create asset
        String assetName = "CT-REEL-" + dto.getReelCode();
        Asset asset = assetService.createAsset(tenantId, CTReelDto.ASSET_TYPE, assetName, dto.getReelCode());
        UUID assetId = asset.getId().getId();

        // Set defaults if not provided
        if (dto.getStatus() == null) {
            dto.setStatus(ReelStatus.AVAILABLE);
        }
        if (dto.getAccumulatedFatiguePercent() == null) {
            dto.setAccumulatedFatiguePercent(BigDecimal.ZERO);
        }

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("CT Reel created successfully: {}", assetId);
        return getById(assetId);
    }

    public CTReelDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        log.info("Creating CT Reel from template {} for tenant {}", request.getTemplateId(), tenantId);

        Map<String, Object> variables = request.getVariables();

        String reelCode = (String) variables.get("reelCode");
        if (reelCode == null || reelCode.isEmpty()) {
            throw new CTBusinessException("Reel code is required");
        }

        // Check for duplicate
        try {
            getByCode(tenantId, reelCode);
            throw new CTBusinessException("Reel code already exists: " + reelCode);
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
        CTReelDto dto = new CTReelDto();
        dto.setReelCode(reelCode);
        dto.setReelName((String) variables.get("reelName"));

        Object tubingOD = variables.get("tubingOD");
        if (tubingOD != null) {
            dto.setTubingOdInch(tubingOD instanceof Double ?
                    BigDecimal.valueOf((Double) tubingOD) :
                    BigDecimal.valueOf(Double.parseDouble(tubingOD.toString())));
        }

        Object tubingID = variables.get("tubingID");
        if (tubingID != null) {
            dto.setTubingIdInch(tubingID instanceof Double ?
                    BigDecimal.valueOf((Double) tubingID) :
                    BigDecimal.valueOf(Double.parseDouble(tubingID.toString())));
        }

        Object wallThickness = variables.get("wallThickness");
        if (wallThickness != null) {
            dto.setWallThicknessInch(wallThickness instanceof Double ?
                    BigDecimal.valueOf((Double) wallThickness) :
                    BigDecimal.valueOf(Double.parseDouble(wallThickness.toString())));
        }

        Object totalLength = variables.get("totalLength");
        if (totalLength != null) {
            dto.setTotalLengthFt(totalLength instanceof Double ?
                    BigDecimal.valueOf((Double) totalLength) :
                    BigDecimal.valueOf(Double.parseDouble(totalLength.toString())));
        }

        String materialGrade = (String) variables.get("material");
        if (materialGrade == null) {
            materialGrade = (String) variables.get("grade");
        }
        dto.setMaterialGrade(materialGrade);
        dto.setStatus(ReelStatus.AVAILABLE);
        dto.setAccumulatedFatiguePercent(BigDecimal.ZERO);

        // Save attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("CT Reel created from template successfully: {} with asset ID: {}", reelCode, assetId);
        return getById(assetId);
    }

    public CTReelDto update(UUID assetId, CTReelDto dto) {
        log.info("Updating CT Reel: {}", assetId);

        CTReelDto existing = getById(assetId);

        // Check for duplicate reel code if changing
        if (!existing.getReelCode().equals(dto.getReelCode())) {
            try {
                getByCode(existing.getTenantId(), dto.getReelCode());
                throw new CTBusinessException("Reel code already exists: " + dto.getReelCode());
            } catch (CTEntityNotFoundException e) {
                // Expected
            }
        }

        // Update attributes
        attributeService.saveServerAttributes(assetId, dto.toAttributeMap());

        log.info("CT Reel updated successfully: {}", assetId);
        return getById(assetId);
    }

    public void delete(UUID tenantId, UUID assetId) {
        log.info("Deleting CT Reel: {}", assetId);

        CTReelDto reel = getById(assetId);

        if (reel.getStatus() == ReelStatus.IN_USE) {
            throw new CTBusinessException("Cannot delete reel that is currently in use");
        }

        assetService.deleteAsset(tenantId, assetId);
        log.info("CT Reel deleted successfully: {}", assetId);
    }

    public CTReelDto updateStatus(UUID assetId, ReelStatus newStatus) {
        log.info("Updating reel {} status to {}", assetId, newStatus);

        getById(assetId); // Verify exists

        attributeService.saveServerAttribute(assetId, CTReelDto.ATTR_STATUS, newStatus.name());

        log.info("Reel status updated successfully");
        return getById(assetId);
    }

    public CTReelDto updateFatigue(UUID assetId, BigDecimal fatiguePercent, Integer cycles) {
        log.info("Updating reel {} fatigue to {}%", assetId, fatiguePercent);

        getById(assetId); // Verify exists

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put(CTReelDto.ATTR_ACCUMULATED_FATIGUE_PERCENT, fatiguePercent);
        if (cycles != null) {
            attrs.put(CTReelDto.ATTR_TOTAL_CYCLES, cycles);
        }

        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Reel fatigue updated successfully");
        return getById(assetId);
    }

    public CTReelDto recordInspection(UUID assetId, Long inspectionDate, String inspectionType,
                                      String inspectionResult, String notes) {
        log.info("Recording inspection for reel {}", assetId);

        CTReelDto reel = getById(assetId);

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put(CTReelDto.ATTR_LAST_INSPECTION_DATE, inspectionDate);
        attrs.put(CTReelDto.ATTR_LAST_INSPECTION_TYPE, inspectionType);
        attrs.put(CTReelDto.ATTR_LAST_INSPECTION_RESULT, inspectionResult);

        if (notes != null) {
            String existingNotes = reel.getNotes() != null ? reel.getNotes() : "";
            String inspectionNote = String.format("\n[%d] Inspection: %s - Result: %s - %s",
                    inspectionDate, inspectionType, inspectionResult, notes);
            attrs.put(CTReelDto.ATTR_NOTES, existingNotes + inspectionNote);
        }

        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Inspection recorded successfully");
        return getById(assetId);
    }

    public CTReelDto retireReel(UUID assetId, String reason) {
        log.info("Retiring reel {} - Reason: {}", assetId, reason);

        CTReelDto reel = getById(assetId);

        if (reel.getStatus() == ReelStatus.IN_USE) {
            throw new CTBusinessException("Cannot retire reel that is currently in use");
        }

        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put(CTReelDto.ATTR_STATUS, ReelStatus.RETIRED.name());
        attrs.put(CTReelDto.ATTR_RETIREMENT_DATE, System.currentTimeMillis());

        String existingNotes = reel.getNotes() != null ? reel.getNotes() : "";
        attrs.put(CTReelDto.ATTR_NOTES, existingNotes + "\n[RETIRED] Reason: " + reason);

        attributeService.saveServerAttributes(assetId, attrs);

        log.info("Reel retired successfully");
        return getById(assetId);
    }

    public long countByStatus(UUID tenantId, ReelStatus status) {
        return getByStatus(tenantId, status).size();
    }

    public long countByTenant(UUID tenantId) {
        return assetService.countAssetsByType(tenantId, CTReelDto.ASSET_TYPE);
    }
}
