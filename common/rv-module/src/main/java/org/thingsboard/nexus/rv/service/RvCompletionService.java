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
import org.thingsboard.nexus.rv.dto.RvCompletionDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Completion entities.
 * Completions represent the mechanical configuration connecting wells to reservoirs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvCompletionService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    /**
     * Creates a new Completion for a Well.
     */
    public RvCompletionDto createCompletion(UUID tenantId, RvCompletionDto dto) {
        log.info("Creating completion: {} for well: {}", dto.getName(), dto.getWellAssetId());

        if (dto.getWellAssetId() != null) {
            if (!rvAssetService.existsById(dto.getWellAssetId())) {
                throw new RvEntityNotFoundException("Well", dto.getWellAssetId());
            }
        }

        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvCompletionDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        saveCompletionAttributes(dto);

        if (dto.getWellAssetId() != null) {
            rvHierarchyService.setParentChild(tenantId, dto.getWellAssetId(), dto.getAssetId());
        }

        // Link to zone if specified
        if (dto.getZoneAssetId() != null) {
            rvHierarchyService.createProducesFromRelation(tenantId, dto.getAssetId(), dto.getZoneAssetId());
        }

        log.info("Completion created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Completion by ID.
     */
    public Optional<RvCompletionDto> getCompletionById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvCompletionDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvCompletionDto dto = new RvCompletionDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadCompletionAttributes(dto);

                UUID wellId = rvHierarchyService.getParent(dto.getTenantId(), dto.getAssetId());
                dto.setWellAssetId(wellId);

                return dto;
            });
    }

    /**
     * Gets all Completions for a tenant.
     */
    public Page<RvCompletionDto> getAllCompletions(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvCompletionDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Completions by Well.
     */
    public List<RvCompletionDto> getCompletionsByWell(UUID tenantId, UUID wellAssetId) {
        List<UUID> completionIds = rvHierarchyService.getChildren(tenantId, wellAssetId);
        List<RvCompletionDto> completions = new ArrayList<>();

        for (UUID completionId : completionIds) {
            getCompletionById(completionId).ifPresent(completions::add);
        }

        // Sort by completion number
        completions.sort(Comparator.comparing(c -> c.getCompletionNumber() != null ? c.getCompletionNumber() : 0));

        return completions;
    }

    /**
     * Updates a Completion.
     */
    public RvCompletionDto updateCompletion(RvCompletionDto dto) {
        log.info("Updating completion: {}", dto.getAssetId());

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
        saveCompletionAttributes(dto);

        return dto;
    }

    /**
     * Updates completion status.
     */
    public void updateCompletionStatus(UUID assetId, String newStatus) {
        log.info("Updating completion {} status to {}", assetId, newStatus);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(RvCompletionDto.ATTR_COMPLETION_STATUS, newStatus);
        rvAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Records a stimulation treatment.
     */
    public void recordStimulation(UUID assetId, String stimulationType, Long stimulationDate) {
        log.info("Recording {} stimulation for completion {}", stimulationType, assetId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("last_stimulation_date", stimulationDate);
        attrs.put("last_stimulation_type", stimulationType);

        if ("ACIDIZING".equals(stimulationType)) {
            attrs.put("has_been_acidized", true);
        } else if ("FRACTURING".equals(stimulationType)) {
            attrs.put("has_been_fractured", true);
        }

        rvAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Updates artificial lift configuration.
     */
    public void updateArtificialLift(UUID assetId, String liftMethod, Map<String, Object> liftParams) {
        log.info("Updating artificial lift for completion {}: {}", assetId, liftMethod);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(RvCompletionDto.ATTR_LIFT_METHOD, liftMethod);
        attrs.putAll(liftParams);

        rvAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Deletes a Completion.
     */
    public void deleteCompletion(UUID tenantId, UUID assetId) {
        log.warn("Deleting completion: {}", assetId);
        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    private void saveCompletionAttributes(RvCompletionDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCompletionCode() != null) attrs.put(RvCompletionDto.ATTR_COMPLETION_CODE, dto.getCompletionCode());
        if (dto.getCompletionNumber() != null) attrs.put(RvCompletionDto.ATTR_COMPLETION_NUMBER, dto.getCompletionNumber());
        if (dto.getCompletionDate() != null) attrs.put(RvCompletionDto.ATTR_COMPLETION_DATE, dto.getCompletionDate());
        if (dto.getCompletionType() != null) attrs.put(RvCompletionDto.ATTR_COMPLETION_TYPE, dto.getCompletionType());
        if (dto.getTopDepthMdM() != null) attrs.put(RvCompletionDto.ATTR_TOP_DEPTH_MD_M, dto.getTopDepthMdM());
        if (dto.getBottomDepthMdM() != null) attrs.put(RvCompletionDto.ATTR_BOTTOM_DEPTH_MD_M, dto.getBottomDepthMdM());
        if (dto.getPerforationShotsPerFoot() != null) attrs.put(RvCompletionDto.ATTR_PERFORATION_SPF, dto.getPerforationShotsPerFoot());
        if (dto.getTubingIdIn() != null) attrs.put(RvCompletionDto.ATTR_TUBING_ID_IN, dto.getTubingIdIn());
        if (dto.getProductivityIndexBpdPsi() != null) attrs.put(RvCompletionDto.ATTR_PRODUCTIVITY_INDEX, dto.getProductivityIndexBpdPsi());
        if (dto.getSkinFactor() != null) attrs.put(RvCompletionDto.ATTR_SKIN_FACTOR, dto.getSkinFactor());
        if (dto.getCompletionStatus() != null) attrs.put(RvCompletionDto.ATTR_COMPLETION_STATUS, dto.getCompletionStatus());
        if (dto.getLiftMethod() != null) attrs.put(RvCompletionDto.ATTR_LIFT_METHOD, dto.getLiftMethod());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadCompletionAttributes(RvCompletionDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvCompletionDto.ATTR_COMPLETION_CODE -> dto.setCompletionCode(entry.getValueAsString());
                case RvCompletionDto.ATTR_COMPLETION_NUMBER -> entry.getLongValue().ifPresent(v -> dto.setCompletionNumber(v.intValue()));
                case RvCompletionDto.ATTR_COMPLETION_DATE -> entry.getLongValue().ifPresent(dto::setCompletionDate);
                case RvCompletionDto.ATTR_COMPLETION_TYPE -> dto.setCompletionType(entry.getValueAsString());
                case RvCompletionDto.ATTR_TOP_DEPTH_MD_M -> entry.getDoubleValue().ifPresent(v -> dto.setTopDepthMdM(BigDecimal.valueOf(v)));
                case RvCompletionDto.ATTR_BOTTOM_DEPTH_MD_M -> entry.getDoubleValue().ifPresent(v -> dto.setBottomDepthMdM(BigDecimal.valueOf(v)));
                case RvCompletionDto.ATTR_PERFORATION_SPF -> entry.getLongValue().ifPresent(v -> dto.setPerforationShotsPerFoot(v.intValue()));
                case RvCompletionDto.ATTR_TUBING_ID_IN -> entry.getDoubleValue().ifPresent(v -> dto.setTubingIdIn(BigDecimal.valueOf(v)));
                case RvCompletionDto.ATTR_PRODUCTIVITY_INDEX -> entry.getDoubleValue().ifPresent(v -> dto.setProductivityIndexBpdPsi(BigDecimal.valueOf(v)));
                case RvCompletionDto.ATTR_SKIN_FACTOR -> entry.getDoubleValue().ifPresent(v -> dto.setSkinFactor(BigDecimal.valueOf(v)));
                case RvCompletionDto.ATTR_COMPLETION_STATUS -> dto.setCompletionStatus(entry.getValueAsString());
                case RvCompletionDto.ATTR_LIFT_METHOD -> dto.setLiftMethod(entry.getValueAsString());
            }
        }
    }

    private RvCompletionDto mapAssetToDto(Asset asset) {
        RvCompletionDto dto = new RvCompletionDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadCompletionAttributes(dto);
        return dto;
    }
}
