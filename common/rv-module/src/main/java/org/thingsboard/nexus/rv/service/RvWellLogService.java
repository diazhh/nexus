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
import org.thingsboard.nexus.rv.dto.RvWellLogRunDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Well Log Run entities.
 * Handles wireline and LWD logging data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvWellLogService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    private static final String ASSET_TYPE = RvAssetService.TYPE_WELL_LOG_RUN;

    // Attribute keys
    private static final String ATTR_RUN_NUMBER = "run_number";
    private static final String ATTR_RUN_DATE = "run_date";
    private static final String ATTR_LOGGING_TYPE = "logging_type";
    private static final String ATTR_LOGGING_COMPANY = "logging_company";
    private static final String ATTR_TOOL_STRING = "tool_string";
    private static final String ATTR_TOP_DEPTH_MD = "top_depth_md";
    private static final String ATTR_BOTTOM_DEPTH_MD = "bottom_depth_md";
    private static final String ATTR_INTERVAL_LOGGED = "interval_logged";
    private static final String ATTR_MUD_TYPE = "mud_type";
    private static final String ATTR_MUD_WEIGHT = "mud_weight";
    private static final String ATTR_OVERALL_QUALITY = "overall_quality";
    private static final String ATTR_INTERPRETED = "interpreted";
    private static final String ATTR_AVG_POROSITY = "avg_porosity";
    private static final String ATTR_AVG_WATER_SAT = "avg_water_sat";
    private static final String ATTR_AVG_VSHALE = "avg_vshale";
    private static final String ATTR_NET_PAY = "net_pay_thickness";
    private static final String ATTR_LAS_FILE_PATH = "las_file_path";

    /**
     * Creates a new Well Log Run.
     */
    public RvWellLogRunDto createWellLogRun(UUID tenantId, RvWellLogRunDto dto) {
        log.info("Creating well log run #{} for well {}", dto.getRunNumber(), dto.getWellId());

        // Validate well exists
        if (dto.getWellId() != null && !rvAssetService.existsById(dto.getWellId())) {
            throw new RvEntityNotFoundException("Well", dto.getWellId());
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

        // Create hierarchy relation to well
        if (dto.getWellId() != null) {
            rvHierarchyService.setParentChild(tenantId, dto.getWellId(), dto.getId());
        }

        log.info("Well log run created with ID: {}", dto.getId());
        return dto;
    }

    /**
     * Gets a Well Log Run by ID.
     */
    public Optional<RvWellLogRunDto> getWellLogRunById(UUID id) {
        return rvAssetService.getAssetById(id)
            .filter(a -> ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvWellLogRunDto dto = new RvWellLogRunDto();
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
     * Gets all Well Log Runs for a tenant.
     */
    public Page<RvWellLogRunDto> getAllWellLogRuns(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Well Log Runs by Well.
     */
    public List<RvWellLogRunDto> getWellLogRunsByWell(UUID tenantId, UUID wellId) {
        List<UUID> logRunIds = rvHierarchyService.getChildren(tenantId, wellId);
        List<RvWellLogRunDto> logRuns = new ArrayList<>();

        for (UUID logRunId : logRunIds) {
            getWellLogRunById(logRunId).ifPresent(logRuns::add);
        }

        // Sort by run number
        logRuns.sort(Comparator.comparingInt(dto -> dto.getRunNumber() != null ? dto.getRunNumber() : 0));

        return logRuns;
    }

    /**
     * Gets Well Log Runs by logging type (WIRELINE, LWD, etc.).
     */
    public List<RvWellLogRunDto> getWellLogRunsByType(UUID tenantId, String loggingType, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(l -> loggingType.equals(l.getLoggingType()))
            .toList();
    }

    /**
     * Updates a Well Log Run.
     */
    public RvWellLogRunDto updateWellLogRun(RvWellLogRunDto dto) {
        log.info("Updating well log run: {}", dto.getId());

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
     * Updates interpretation results.
     */
    public void updateInterpretationResults(UUID id, String interpreter, BigDecimal avgPorosity,
                                             BigDecimal avgWaterSat, BigDecimal avgVshale,
                                             BigDecimal netPay, BigDecimal grossThickness) {
        log.info("Updating interpretation results for log run {}", id);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ATTR_INTERPRETED, true);
        attrs.put("interpreter", interpreter);
        attrs.put("interpretation_date", System.currentTimeMillis());
        if (avgPorosity != null) attrs.put(ATTR_AVG_POROSITY, avgPorosity);
        if (avgWaterSat != null) attrs.put(ATTR_AVG_WATER_SAT, avgWaterSat);
        if (avgVshale != null) attrs.put(ATTR_AVG_VSHALE, avgVshale);
        if (netPay != null) attrs.put(ATTR_NET_PAY, netPay);
        if (grossThickness != null) {
            attrs.put("gross_thickness", grossThickness);
            if (netPay != null && grossThickness.compareTo(BigDecimal.ZERO) > 0) {
                attrs.put("net_to_gross", netPay.divide(grossThickness, 4, java.math.RoundingMode.HALF_UP));
            }
        }

        rvAttributeService.saveServerAttributes(id, attrs);
    }

    /**
     * Deletes a Well Log Run.
     */
    public void deleteWellLogRun(UUID tenantId, UUID id) {
        log.warn("Deleting well log run: {}", id);
        rvHierarchyService.deleteAllRelations(tenantId, id);
        rvAssetService.deleteAsset(tenantId, id);
    }

    private void saveAttributes(RvWellLogRunDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getRunNumber() != null) attrs.put(ATTR_RUN_NUMBER, dto.getRunNumber());
        if (dto.getRunDate() != null) attrs.put(ATTR_RUN_DATE, dto.getRunDate());
        if (dto.getLoggingType() != null) attrs.put(ATTR_LOGGING_TYPE, dto.getLoggingType());
        if (dto.getLoggingCompany() != null) attrs.put(ATTR_LOGGING_COMPANY, dto.getLoggingCompany());
        if (dto.getToolString() != null) attrs.put(ATTR_TOOL_STRING, dto.getToolString());
        if (dto.getTopDepthMd() != null) attrs.put(ATTR_TOP_DEPTH_MD, dto.getTopDepthMd());
        if (dto.getBottomDepthMd() != null) attrs.put(ATTR_BOTTOM_DEPTH_MD, dto.getBottomDepthMd());
        if (dto.getIntervalLogged() != null) attrs.put(ATTR_INTERVAL_LOGGED, dto.getIntervalLogged());
        if (dto.getMudType() != null) attrs.put(ATTR_MUD_TYPE, dto.getMudType());
        if (dto.getMudWeight() != null) attrs.put(ATTR_MUD_WEIGHT, dto.getMudWeight());
        if (dto.getOverallQuality() != null) attrs.put(ATTR_OVERALL_QUALITY, dto.getOverallQuality());
        if (dto.getInterpreted() != null) attrs.put(ATTR_INTERPRETED, dto.getInterpreted());
        if (dto.getAvgPorosity() != null) attrs.put(ATTR_AVG_POROSITY, dto.getAvgPorosity());
        if (dto.getAvgWaterSat() != null) attrs.put(ATTR_AVG_WATER_SAT, dto.getAvgWaterSat());
        if (dto.getAvgVshale() != null) attrs.put(ATTR_AVG_VSHALE, dto.getAvgVshale());
        if (dto.getNetPayThickness() != null) attrs.put(ATTR_NET_PAY, dto.getNetPayThickness());
        if (dto.getLasFilePath() != null) attrs.put(ATTR_LAS_FILE_PATH, dto.getLasFilePath());
        if (dto.getWellId() != null) attrs.put("well_id", dto.getWellId().toString());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getId(), attrs);
        }
    }

    private void loadAttributes(RvWellLogRunDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case ATTR_RUN_NUMBER -> entry.getLongValue().ifPresent(v -> dto.setRunNumber(v.intValue()));
                case ATTR_RUN_DATE -> dto.setRunDate(entry.getValueAsString());
                case ATTR_LOGGING_TYPE -> dto.setLoggingType(entry.getValueAsString());
                case ATTR_LOGGING_COMPANY -> dto.setLoggingCompany(entry.getValueAsString());
                case ATTR_TOOL_STRING -> dto.setToolString(entry.getValueAsString());
                case ATTR_TOP_DEPTH_MD -> entry.getDoubleValue().ifPresent(v -> dto.setTopDepthMd(BigDecimal.valueOf(v)));
                case ATTR_BOTTOM_DEPTH_MD -> entry.getDoubleValue().ifPresent(v -> dto.setBottomDepthMd(BigDecimal.valueOf(v)));
                case ATTR_INTERVAL_LOGGED -> entry.getDoubleValue().ifPresent(v -> dto.setIntervalLogged(BigDecimal.valueOf(v)));
                case ATTR_MUD_TYPE -> dto.setMudType(entry.getValueAsString());
                case ATTR_MUD_WEIGHT -> entry.getDoubleValue().ifPresent(v -> dto.setMudWeight(BigDecimal.valueOf(v)));
                case ATTR_OVERALL_QUALITY -> dto.setOverallQuality(entry.getValueAsString());
                case ATTR_INTERPRETED -> entry.getBooleanValue().ifPresent(dto::setInterpreted);
                case ATTR_AVG_POROSITY -> entry.getDoubleValue().ifPresent(v -> dto.setAvgPorosity(BigDecimal.valueOf(v)));
                case ATTR_AVG_WATER_SAT -> entry.getDoubleValue().ifPresent(v -> dto.setAvgWaterSat(BigDecimal.valueOf(v)));
                case ATTR_AVG_VSHALE -> entry.getDoubleValue().ifPresent(v -> dto.setAvgVshale(BigDecimal.valueOf(v)));
                case ATTR_NET_PAY -> entry.getDoubleValue().ifPresent(v -> dto.setNetPayThickness(BigDecimal.valueOf(v)));
                case ATTR_LAS_FILE_PATH -> dto.setLasFilePath(entry.getValueAsString());
                case "well_id" -> dto.setWellId(UUID.fromString(entry.getValueAsString()));
            }
        }
    }

    private RvWellLogRunDto mapAssetToDto(Asset asset) {
        RvWellLogRunDto dto = new RvWellLogRunDto();
        dto.setId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setDescription(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadAttributes(dto);
        return dto;
    }
}
