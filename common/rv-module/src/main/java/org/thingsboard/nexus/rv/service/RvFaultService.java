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
import org.thingsboard.nexus.rv.dto.RvFaultDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Fault entities.
 * Handles geological fault interpretation and sealing analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvFaultService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    private static final String ASSET_TYPE = RvAssetService.TYPE_FAULT;

    // Attribute keys
    private static final String ATTR_FAULT_TYPE = "fault_type";
    private static final String ATTR_FAULT_SYSTEM = "fault_system";
    private static final String ATTR_STRIKE_AZIMUTH = "strike_azimuth";
    private static final String ATTR_DIP_ANGLE = "dip_angle";
    private static final String ATTR_DIP_DIRECTION = "dip_direction";
    private static final String ATTR_LENGTH_KM = "length_km";
    private static final String ATTR_THROW_M = "throw_m";
    private static final String ATTR_HEAVE_M = "heave_m";
    private static final String ATTR_SEALING_POTENTIAL = "sealing_potential";
    private static final String ATTR_SHALE_SMEAR_FACTOR = "shale_smear_factor";
    private static final String ATTR_COMPARTMENTALIZING = "compartmentalizing";
    private static final String ATTR_INTERPRETATION_CONFIDENCE = "interpretation_confidence";

    /**
     * Creates a new Fault.
     */
    public RvFaultDto createFault(UUID tenantId, RvFaultDto dto) {
        log.info("Creating fault: {} of type {}", dto.getName(), dto.getFaultType());

        // Validate field exists if specified
        if (dto.getFieldId() != null && !rvAssetService.existsById(dto.getFieldId())) {
            throw new RvEntityNotFoundException("Field", dto.getFieldId());
        }

        // Create ThingsBoard Asset
        Asset asset = rvAssetService.createAsset(
            tenantId,
            ASSET_TYPE,
            dto.getName(),
            dto.getDescription()
        );

        dto.setId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // Save attributes
        saveAttributes(dto);

        // Create hierarchy relations
        if (dto.getFieldId() != null) {
            rvHierarchyService.createCharacterizedByRelation(tenantId, dto.getFieldId(), dto.getId());
        }
        if (dto.getSeismicSurveyId() != null) {
            rvHierarchyService.createCharacterizedByRelation(tenantId, dto.getId(), dto.getSeismicSurveyId());
        }

        log.info("Fault created with ID: {}", dto.getId());
        return dto;
    }

    /**
     * Gets a Fault by ID.
     */
    public Optional<RvFaultDto> getFaultById(UUID id) {
        return rvAssetService.getAssetById(id)
            .filter(a -> ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvFaultDto dto = new RvFaultDto();
                dto.setId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setDescription(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());
                loadAttributes(dto);
                return dto;
            });
    }

    /**
     * Gets all Faults for a tenant.
     */
    public Page<RvFaultDto> getAllFaults(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Faults by Field.
     */
    public List<RvFaultDto> getFaultsByField(UUID tenantId, UUID fieldId) {
        List<UUID> faultIds = rvHierarchyService.getRelatedEntities(tenantId, fieldId, "CharacterizedBy");
        List<RvFaultDto> faults = new ArrayList<>();

        for (UUID faultId : faultIds) {
            getFaultById(faultId).ifPresent(faults::add);
        }

        return faults;
    }

    /**
     * Gets Faults by Seismic Survey.
     */
    public List<RvFaultDto> getFaultsBySeismicSurvey(UUID tenantId, UUID surveyId) {
        Page<Asset> allFaults = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, 0, 1000);

        return allFaults.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(f -> surveyId.equals(f.getSeismicSurveyId()))
            .toList();
    }

    /**
     * Gets sealing faults only.
     */
    public List<RvFaultDto> getSealingFaults(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(f -> "SEALING".equals(f.getSealingPotential()))
            .toList();
    }

    /**
     * Gets compartmentalizing faults.
     */
    public List<RvFaultDto> getCompartmentalizingFaults(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(f -> Boolean.TRUE.equals(f.getCompartmentalizing()))
            .toList();
    }

    /**
     * Updates a Fault.
     */
    public RvFaultDto updateFault(RvFaultDto dto) {
        log.info("Updating fault: {}", dto.getId());

        rvAssetService.getAssetById(dto.getId()).ifPresent(asset -> {
            boolean needsUpdate = false;
            if (!asset.getName().equals(dto.getName())) {
                asset.setName(dto.getName());
                needsUpdate = true;
            }
            if (!Objects.equals(asset.getLabel(), dto.getDescription())) {
                asset.setLabel(dto.getDescription());
                needsUpdate = true;
            }
            if (needsUpdate) {
                rvAssetService.updateAsset(asset);
            }
        });

        dto.setUpdatedTime(System.currentTimeMillis());
        saveAttributes(dto);

        return dto;
    }

    /**
     * Updates sealing analysis results.
     */
    public void updateSealingAnalysis(UUID id, String sealingPotential, BigDecimal sgr,
                                       BigDecimal csp, Boolean compartmentalizing) {
        log.info("Updating sealing analysis for fault {}", id);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ATTR_SEALING_POTENTIAL, sealingPotential);
        if (sgr != null) attrs.put(ATTR_SHALE_SMEAR_FACTOR, sgr);
        if (csp != null) attrs.put("clay_smear_potential", csp);
        if (compartmentalizing != null) attrs.put(ATTR_COMPARTMENTALIZING, compartmentalizing);

        rvAttributeService.saveServerAttributes(id, attrs);
    }

    /**
     * Deletes a Fault.
     */
    public void deleteFault(UUID tenantId, UUID id) {
        log.warn("Deleting fault: {}", id);
        rvHierarchyService.deleteAllRelations(tenantId, id);
        rvAssetService.deleteAsset(tenantId, id);
    }

    private void saveAttributes(RvFaultDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getFaultType() != null) attrs.put(ATTR_FAULT_TYPE, dto.getFaultType());
        if (dto.getFaultSystem() != null) attrs.put(ATTR_FAULT_SYSTEM, dto.getFaultSystem());
        if (dto.getStrikeAzimuth() != null) attrs.put(ATTR_STRIKE_AZIMUTH, dto.getStrikeAzimuth());
        if (dto.getDipAngle() != null) attrs.put(ATTR_DIP_ANGLE, dto.getDipAngle());
        if (dto.getDipDirection() != null) attrs.put(ATTR_DIP_DIRECTION, dto.getDipDirection());
        if (dto.getLengthKm() != null) attrs.put(ATTR_LENGTH_KM, dto.getLengthKm());
        if (dto.getThrowM() != null) attrs.put(ATTR_THROW_M, dto.getThrowM());
        if (dto.getHeaveM() != null) attrs.put(ATTR_HEAVE_M, dto.getHeaveM());
        if (dto.getSealingPotential() != null) attrs.put(ATTR_SEALING_POTENTIAL, dto.getSealingPotential());
        if (dto.getShaleSmeaerFactor() != null) attrs.put(ATTR_SHALE_SMEAR_FACTOR, dto.getShaleSmeaerFactor());
        if (dto.getCompartmentalizing() != null) attrs.put(ATTR_COMPARTMENTALIZING, dto.getCompartmentalizing());
        if (dto.getInterpretationConfidence() != null) attrs.put(ATTR_INTERPRETATION_CONFIDENCE, dto.getInterpretationConfidence());
        if (dto.getFieldId() != null) attrs.put("field_id", dto.getFieldId().toString());
        if (dto.getSeismicSurveyId() != null) attrs.put("seismic_survey_id", dto.getSeismicSurveyId().toString());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getId(), attrs);
        }
    }

    private void loadAttributes(RvFaultDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case ATTR_FAULT_TYPE -> dto.setFaultType(entry.getValueAsString());
                case ATTR_FAULT_SYSTEM -> dto.setFaultSystem(entry.getValueAsString());
                case ATTR_STRIKE_AZIMUTH -> entry.getDoubleValue().ifPresent(v -> dto.setStrikeAzimuth(BigDecimal.valueOf(v)));
                case ATTR_DIP_ANGLE -> entry.getDoubleValue().ifPresent(v -> dto.setDipAngle(BigDecimal.valueOf(v)));
                case ATTR_DIP_DIRECTION -> dto.setDipDirection(entry.getValueAsString());
                case ATTR_LENGTH_KM -> entry.getDoubleValue().ifPresent(v -> dto.setLengthKm(BigDecimal.valueOf(v)));
                case ATTR_THROW_M -> entry.getDoubleValue().ifPresent(v -> dto.setThrowM(BigDecimal.valueOf(v)));
                case ATTR_HEAVE_M -> entry.getDoubleValue().ifPresent(v -> dto.setHeaveM(BigDecimal.valueOf(v)));
                case ATTR_SEALING_POTENTIAL -> dto.setSealingPotential(entry.getValueAsString());
                case ATTR_SHALE_SMEAR_FACTOR -> entry.getDoubleValue().ifPresent(v -> dto.setShaleSmeaerFactor(BigDecimal.valueOf(v)));
                case ATTR_COMPARTMENTALIZING -> entry.getBooleanValue().ifPresent(dto::setCompartmentalizing);
                case ATTR_INTERPRETATION_CONFIDENCE -> dto.setInterpretationConfidence(entry.getValueAsString());
                case "field_id" -> dto.setFieldId(UUID.fromString(entry.getValueAsString()));
                case "seismic_survey_id" -> dto.setSeismicSurveyId(UUID.fromString(entry.getValueAsString()));
            }
        }
    }

    private RvFaultDto mapAssetToDto(Asset asset) {
        RvFaultDto dto = new RvFaultDto();
        dto.setId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setDescription(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadAttributes(dto);
        return dto;
    }
}
