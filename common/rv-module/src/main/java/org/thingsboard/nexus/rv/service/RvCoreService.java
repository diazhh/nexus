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
import org.thingsboard.nexus.rv.dto.RvCoreDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing Core sample entities.
 * Handles conventional and sidewall core data with RCA and SCAL analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvCoreService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;

    private static final String ASSET_TYPE = RvAssetService.TYPE_CORE;

    // Attribute keys
    private static final String ATTR_CORE_NUMBER = "core_number";
    private static final String ATTR_CORE_DATE = "core_date";
    private static final String ATTR_CORE_TYPE = "core_type";
    private static final String ATTR_TOP_DEPTH_MD = "top_depth_md";
    private static final String ATTR_BOTTOM_DEPTH_MD = "bottom_depth_md";
    private static final String ATTR_CUT_LENGTH = "cut_length";
    private static final String ATTR_RECOVERED_LENGTH = "recovered_length";
    private static final String ATTR_RECOVERY = "recovery";
    private static final String ATTR_FORMATION_NAME = "formation_name";
    private static final String ATTR_LITHOLOGY_PRIMARY = "lithology_primary";
    private static final String ATTR_OIL_SHOW = "oil_show";
    private static final String ATTR_RCA_COMPLETED = "rca_completed";
    private static final String ATTR_AVG_POROSITY = "avg_porosity";
    private static final String ATTR_AVG_PERM_H = "avg_permeability_h";
    private static final String ATTR_AVG_PERM_V = "avg_permeability_v";
    private static final String ATTR_SCAL_COMPLETED = "scal_completed";
    private static final String ATTR_SWIRR = "irreducible_water_sat";
    private static final String ATTR_SOR = "residual_oil_sat";
    private static final String ATTR_WETTABILITY = "wettability";
    private static final String ATTR_HEAVY_OIL_CORE = "heavy_oil_core";

    /**
     * Creates a new Core.
     */
    public RvCoreDto createCore(UUID tenantId, RvCoreDto dto) {
        log.info("Creating core #{} for well {}", dto.getCoreNumber(), dto.getWellId());

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

        // Calculate recovery if not provided
        if (dto.getRecovery() == null && dto.getCutLength() != null && dto.getRecoveredLength() != null) {
            if (dto.getCutLength().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal recovery = dto.getRecoveredLength()
                    .divide(dto.getCutLength(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                dto.setRecovery(recovery);
            }
        }

        // Save attributes
        saveAttributes(dto);

        // Create hierarchy relation to well
        if (dto.getWellId() != null) {
            rvHierarchyService.setParentChild(tenantId, dto.getWellId(), dto.getId());
        }

        log.info("Core created with ID: {}", dto.getId());
        return dto;
    }

    /**
     * Gets a Core by ID.
     */
    public Optional<RvCoreDto> getCoreById(UUID id) {
        return rvAssetService.getAssetById(id)
            .filter(a -> ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvCoreDto dto = new RvCoreDto();
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
     * Gets all Cores for a tenant.
     */
    public Page<RvCoreDto> getAllCores(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets Cores by Well.
     */
    public List<RvCoreDto> getCoresByWell(UUID tenantId, UUID wellId) {
        List<UUID> coreIds = rvHierarchyService.getChildren(tenantId, wellId);
        List<RvCoreDto> cores = new ArrayList<>();

        for (UUID coreId : coreIds) {
            getCoreById(coreId).ifPresent(cores::add);
        }

        // Sort by core number
        cores.sort(Comparator.comparingInt(dto -> dto.getCoreNumber() != null ? dto.getCoreNumber() : 0));

        return cores;
    }

    /**
     * Gets Cores with RCA completed.
     */
    public List<RvCoreDto> getCoresWithRCA(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(c -> Boolean.TRUE.equals(c.getRcaCompleted()))
            .toList();
    }

    /**
     * Gets Cores with SCAL completed.
     */
    public List<RvCoreDto> getCoresWithSCAL(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, ASSET_TYPE, page, size);

        return assets.getContent().stream()
            .map(this::mapAssetToDto)
            .filter(c -> Boolean.TRUE.equals(c.getScalCompleted()))
            .toList();
    }

    /**
     * Updates a Core.
     */
    public RvCoreDto updateCore(RvCoreDto dto) {
        log.info("Updating core: {}", dto.getId());

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
     * Updates RCA (Routine Core Analysis) results.
     */
    public void updateRCAResults(UUID id, String laboratory, BigDecimal avgPorosity,
                                  BigDecimal avgPermH, BigDecimal avgPermV,
                                  BigDecimal grainDensity, BigDecimal avgWaterSat) {
        log.info("Updating RCA results for core {}", id);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ATTR_RCA_COMPLETED, true);
        attrs.put("rca_laboratory", laboratory);
        attrs.put("rca_date", System.currentTimeMillis());
        if (avgPorosity != null) attrs.put(ATTR_AVG_POROSITY, avgPorosity);
        if (avgPermH != null) attrs.put(ATTR_AVG_PERM_H, avgPermH);
        if (avgPermV != null) attrs.put(ATTR_AVG_PERM_V, avgPermV);
        if (grainDensity != null) attrs.put("avg_grain_density", grainDensity);
        if (avgWaterSat != null) attrs.put("avg_water_sat", avgWaterSat);

        rvAttributeService.saveServerAttributes(id, attrs);
    }

    /**
     * Updates SCAL (Special Core Analysis) results.
     */
    public void updateSCALResults(UUID id, String laboratory, BigDecimal swirr,
                                   BigDecimal sor, BigDecimal krwEndpoint, BigDecimal kroEndpoint,
                                   BigDecimal wettabilityIndex, String wettabilityClass) {
        log.info("Updating SCAL results for core {}", id);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ATTR_SCAL_COMPLETED, true);
        attrs.put("scal_laboratory", laboratory);
        attrs.put("scal_date", System.currentTimeMillis());
        if (swirr != null) attrs.put(ATTR_SWIRR, swirr);
        if (sor != null) attrs.put(ATTR_SOR, sor);
        if (krwEndpoint != null) attrs.put("rel_perm_endpoint_water", krwEndpoint);
        if (kroEndpoint != null) attrs.put("rel_perm_endpoint_oil", kroEndpoint);
        if (wettabilityIndex != null) attrs.put(ATTR_WETTABILITY, wettabilityIndex);
        if (wettabilityClass != null) attrs.put("wettability_class", wettabilityClass);

        rvAttributeService.saveServerAttributes(id, attrs);
    }

    /**
     * Deletes a Core.
     */
    public void deleteCore(UUID tenantId, UUID id) {
        log.warn("Deleting core: {}", id);
        rvHierarchyService.deleteAllRelations(tenantId, id);
        rvAssetService.deleteAsset(tenantId, id);
    }

    private void saveAttributes(RvCoreDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCoreNumber() != null) attrs.put(ATTR_CORE_NUMBER, dto.getCoreNumber());
        if (dto.getCoreDate() != null) attrs.put(ATTR_CORE_DATE, dto.getCoreDate());
        if (dto.getCoreType() != null) attrs.put(ATTR_CORE_TYPE, dto.getCoreType());
        if (dto.getTopDepthMd() != null) attrs.put(ATTR_TOP_DEPTH_MD, dto.getTopDepthMd());
        if (dto.getBottomDepthMd() != null) attrs.put(ATTR_BOTTOM_DEPTH_MD, dto.getBottomDepthMd());
        if (dto.getCutLength() != null) attrs.put(ATTR_CUT_LENGTH, dto.getCutLength());
        if (dto.getRecoveredLength() != null) attrs.put(ATTR_RECOVERED_LENGTH, dto.getRecoveredLength());
        if (dto.getRecovery() != null) attrs.put(ATTR_RECOVERY, dto.getRecovery());
        if (dto.getFormationName() != null) attrs.put(ATTR_FORMATION_NAME, dto.getFormationName());
        if (dto.getLithologyPrimary() != null) attrs.put(ATTR_LITHOLOGY_PRIMARY, dto.getLithologyPrimary());
        if (dto.getOilShow() != null) attrs.put(ATTR_OIL_SHOW, dto.getOilShow());
        if (dto.getRcaCompleted() != null) attrs.put(ATTR_RCA_COMPLETED, dto.getRcaCompleted());
        if (dto.getAvgPorosity() != null) attrs.put(ATTR_AVG_POROSITY, dto.getAvgPorosity());
        if (dto.getAvgPermeabilityH() != null) attrs.put(ATTR_AVG_PERM_H, dto.getAvgPermeabilityH());
        if (dto.getAvgPermeabilityV() != null) attrs.put(ATTR_AVG_PERM_V, dto.getAvgPermeabilityV());
        if (dto.getScalCompleted() != null) attrs.put(ATTR_SCAL_COMPLETED, dto.getScalCompleted());
        if (dto.getIrreducibleWaterSat() != null) attrs.put(ATTR_SWIRR, dto.getIrreducibleWaterSat());
        if (dto.getResidualOilSat() != null) attrs.put(ATTR_SOR, dto.getResidualOilSat());
        if (dto.getWettability() != null) attrs.put(ATTR_WETTABILITY, dto.getWettability());
        if (dto.getHeavyOilCore() != null) attrs.put(ATTR_HEAVY_OIL_CORE, dto.getHeavyOilCore());
        if (dto.getWellId() != null) attrs.put("well_id", dto.getWellId().toString());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getId(), attrs);
        }
    }

    private void loadAttributes(RvCoreDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case ATTR_CORE_NUMBER -> entry.getLongValue().ifPresent(v -> dto.setCoreNumber(v.intValue()));
                case ATTR_CORE_DATE -> dto.setCoreDate(entry.getValueAsString());
                case ATTR_CORE_TYPE -> dto.setCoreType(entry.getValueAsString());
                case ATTR_TOP_DEPTH_MD -> entry.getDoubleValue().ifPresent(v -> dto.setTopDepthMd(BigDecimal.valueOf(v)));
                case ATTR_BOTTOM_DEPTH_MD -> entry.getDoubleValue().ifPresent(v -> dto.setBottomDepthMd(BigDecimal.valueOf(v)));
                case ATTR_CUT_LENGTH -> entry.getDoubleValue().ifPresent(v -> dto.setCutLength(BigDecimal.valueOf(v)));
                case ATTR_RECOVERED_LENGTH -> entry.getDoubleValue().ifPresent(v -> dto.setRecoveredLength(BigDecimal.valueOf(v)));
                case ATTR_RECOVERY -> entry.getDoubleValue().ifPresent(v -> dto.setRecovery(BigDecimal.valueOf(v)));
                case ATTR_FORMATION_NAME -> dto.setFormationName(entry.getValueAsString());
                case ATTR_LITHOLOGY_PRIMARY -> dto.setLithologyPrimary(entry.getValueAsString());
                case ATTR_OIL_SHOW -> dto.setOilShow(entry.getValueAsString());
                case ATTR_RCA_COMPLETED -> entry.getBooleanValue().ifPresent(dto::setRcaCompleted);
                case ATTR_AVG_POROSITY -> entry.getDoubleValue().ifPresent(v -> dto.setAvgPorosity(BigDecimal.valueOf(v)));
                case ATTR_AVG_PERM_H -> entry.getDoubleValue().ifPresent(v -> dto.setAvgPermeabilityH(BigDecimal.valueOf(v)));
                case ATTR_AVG_PERM_V -> entry.getDoubleValue().ifPresent(v -> dto.setAvgPermeabilityV(BigDecimal.valueOf(v)));
                case ATTR_SCAL_COMPLETED -> entry.getBooleanValue().ifPresent(dto::setScalCompleted);
                case ATTR_SWIRR -> entry.getDoubleValue().ifPresent(v -> dto.setIrreducibleWaterSat(BigDecimal.valueOf(v)));
                case ATTR_SOR -> entry.getDoubleValue().ifPresent(v -> dto.setResidualOilSat(BigDecimal.valueOf(v)));
                case ATTR_WETTABILITY -> entry.getDoubleValue().ifPresent(v -> dto.setWettability(BigDecimal.valueOf(v)));
                case ATTR_HEAVY_OIL_CORE -> entry.getBooleanValue().ifPresent(dto::setHeavyOilCore);
                case "well_id" -> dto.setWellId(UUID.fromString(entry.getValueAsString()));
            }
        }
    }

    private RvCoreDto mapAssetToDto(Asset asset) {
        RvCoreDto dto = new RvCoreDto();
        dto.setId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setDescription(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadAttributes(dto);
        return dto;
    }
}
