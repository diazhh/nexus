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
import org.thingsboard.nexus.rv.dto.RvSeismicSurveyDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Seismic Survey entities.
 * Handles 2D/3D seismic acquisition and interpretation data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvSeismicSurveyService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    private static final String ASSET_TYPE = RvAssetService.TYPE_SEISMIC_SURVEY;

    // Attribute keys
    private static final String ATTR_SURVEY_TYPE = "survey_type";
    private static final String ATTR_ACQUISITION_DATE = "acquisition_date";
    private static final String ATTR_PROCESSING_DATE = "processing_date";
    private static final String ATTR_CONTRACTOR = "contractor";
    private static final String ATTR_AREA_KM2 = "area_km2";
    private static final String ATTR_TOTAL_LINE_KM = "total_line_km";
    private static final String ATTR_FOLD_COVERAGE = "fold_coverage";
    private static final String ATTR_QUALITY_RATING = "quality_rating";
    private static final String ATTR_INTERPRETED = "interpreted";
    private static final String ATTR_HORIZONS_INTERPRETED = "horizons_interpreted";
    private static final String ATTR_FAULTS_INTERPRETED = "faults_interpreted";
    private static final String ATTR_SEGY_FILE_PATH = "segy_file_path";
    private static final String ATTR_FAJA_REGION = "faja_region";

    /**
     * Creates a new Seismic Survey.
     */
    public RvSeismicSurveyDto createSeismicSurvey(UUID tenantId, RvSeismicSurveyDto dto) {
        log.info("Creating seismic survey: {} of type {}", dto.getName(), dto.getSurveyType());

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

        // Create hierarchy relation to field
        if (dto.getFieldId() != null) {
            rvHierarchyService.createCharacterizedByRelation(tenantId, dto.getFieldId(), dto.getId());
        }

        log.info("Seismic survey created with ID: {}", dto.getId());
        return dto;
    }

    /**
     * Gets a Seismic Survey by ID.
     */
    public Optional<RvSeismicSurveyDto> getSeismicSurveyById(UUID id) {
        return rvAssetService.getAssetById(id)
            .filter(a -> ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvSeismicSurveyDto dto = new RvSeismicSurveyDto();
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
     * Gets all Seismic Surveys for a tenant.
     */
    public Page<RvSeismicSurveyDto> getAllSeismicSurveys(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Seismic Surveys by Field.
     */
    public List<RvSeismicSurveyDto> getSeismicSurveysByField(UUID tenantId, UUID fieldId) {
        List<UUID> surveyIds = rvHierarchyService.getRelatedEntities(tenantId, fieldId, "CharacterizedBy");
        List<RvSeismicSurveyDto> surveys = new ArrayList<>();

        for (UUID surveyId : surveyIds) {
            getSeismicSurveyById(surveyId).ifPresent(surveys::add);
        }

        return surveys;
    }

    /**
     * Gets Seismic Surveys by type (2D, 3D, 4D, VSP).
     */
    public List<RvSeismicSurveyDto> getSeismicSurveysByType(UUID tenantId, String surveyType, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(s -> surveyType.equals(s.getSurveyType()))
            .toList();
    }

    /**
     * Updates a Seismic Survey.
     */
    public RvSeismicSurveyDto updateSeismicSurvey(RvSeismicSurveyDto dto) {
        log.info("Updating seismic survey: {}", dto.getId());

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
     * Deletes a Seismic Survey.
     */
    public void deleteSeismicSurvey(UUID tenantId, UUID id) {
        log.warn("Deleting seismic survey: {}", id);
        rvHierarchyService.deleteAllRelations(tenantId, id);
        rvAssetService.deleteAsset(tenantId, id);
    }

    /**
     * Marks survey as interpreted.
     */
    public void markAsInterpreted(UUID id, String interpreter, int horizons, int faults) {
        log.info("Marking seismic survey {} as interpreted by {}", id, interpreter);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ATTR_INTERPRETED, true);
        attrs.put("interpreter_name", interpreter);
        attrs.put("interpretation_date", System.currentTimeMillis());
        attrs.put(ATTR_HORIZONS_INTERPRETED, horizons);
        attrs.put(ATTR_FAULTS_INTERPRETED, faults);

        rvAttributeService.saveServerAttributes(id, attrs);
    }

    private void saveAttributes(RvSeismicSurveyDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getSurveyType() != null) attrs.put(ATTR_SURVEY_TYPE, dto.getSurveyType());
        if (dto.getAcquisitionDate() != null) attrs.put(ATTR_ACQUISITION_DATE, dto.getAcquisitionDate());
        if (dto.getProcessingDate() != null) attrs.put(ATTR_PROCESSING_DATE, dto.getProcessingDate());
        if (dto.getContractor() != null) attrs.put(ATTR_CONTRACTOR, dto.getContractor());
        if (dto.getAreaKm2() != null) attrs.put(ATTR_AREA_KM2, dto.getAreaKm2());
        if (dto.getTotalLineKm() != null) attrs.put(ATTR_TOTAL_LINE_KM, dto.getTotalLineKm());
        if (dto.getFoldCoverage() != null) attrs.put(ATTR_FOLD_COVERAGE, dto.getFoldCoverage());
        if (dto.getQualityRating() != null) attrs.put(ATTR_QUALITY_RATING, dto.getQualityRating());
        if (dto.getInterpreted() != null) attrs.put(ATTR_INTERPRETED, dto.getInterpreted());
        if (dto.getHorizonsInterpreted() != null) attrs.put(ATTR_HORIZONS_INTERPRETED, dto.getHorizonsInterpreted());
        if (dto.getFaultsInterpreted() != null) attrs.put(ATTR_FAULTS_INTERPRETED, dto.getFaultsInterpreted());
        if (dto.getSegyFilePath() != null) attrs.put(ATTR_SEGY_FILE_PATH, dto.getSegyFilePath());
        if (dto.getFajaRegion() != null) attrs.put(ATTR_FAJA_REGION, dto.getFajaRegion());
        if (dto.getFieldId() != null) attrs.put("field_id", dto.getFieldId().toString());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getId(), attrs);
        }
    }

    private void loadAttributes(RvSeismicSurveyDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case ATTR_SURVEY_TYPE -> dto.setSurveyType(entry.getValueAsString());
                case ATTR_ACQUISITION_DATE -> dto.setAcquisitionDate(entry.getValueAsString());
                case ATTR_PROCESSING_DATE -> dto.setProcessingDate(entry.getValueAsString());
                case ATTR_CONTRACTOR -> dto.setContractor(entry.getValueAsString());
                case ATTR_AREA_KM2 -> entry.getDoubleValue().ifPresent(v -> dto.setAreaKm2(BigDecimal.valueOf(v)));
                case ATTR_TOTAL_LINE_KM -> entry.getDoubleValue().ifPresent(v -> dto.setTotalLineKm(BigDecimal.valueOf(v)));
                case ATTR_FOLD_COVERAGE -> entry.getLongValue().ifPresent(v -> dto.setFoldCoverage(v.intValue()));
                case ATTR_QUALITY_RATING -> dto.setQualityRating(entry.getValueAsString());
                case ATTR_INTERPRETED -> entry.getBooleanValue().ifPresent(dto::setInterpreted);
                case ATTR_HORIZONS_INTERPRETED -> entry.getLongValue().ifPresent(v -> dto.setHorizonsInterpreted(v.intValue()));
                case ATTR_FAULTS_INTERPRETED -> entry.getLongValue().ifPresent(v -> dto.setFaultsInterpreted(v.intValue()));
                case ATTR_SEGY_FILE_PATH -> dto.setSegyFilePath(entry.getValueAsString());
                case ATTR_FAJA_REGION -> dto.setFajaRegion(entry.getValueAsString());
                case "field_id" -> dto.setFieldId(UUID.fromString(entry.getValueAsString()));
            }
        }
    }

    private RvSeismicSurveyDto mapAssetToDto(Asset asset) {
        RvSeismicSurveyDto dto = new RvSeismicSurveyDto();
        dto.setId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setDescription(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadAttributes(dto);
        return dto;
    }
}
