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
import org.thingsboard.nexus.rv.dto.RvPvtStudyDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for managing PVT Study entities.
 * PVT studies contain fluid property data from laboratory analysis or correlations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvPvtStudyService {

    private final RvAssetService rvAssetService;
    private final RvAttributeService rvAttributeService;
    private final RvHierarchyService rvHierarchyService;
    private final RvCalculationService calculationService;

    /**
     * Creates a new PVT Study.
     */
    public RvPvtStudyDto createPvtStudy(UUID tenantId, RvPvtStudyDto dto) {
        log.info("Creating PVT study: {} for reservoir: {}", dto.getName(), dto.getReservoirAssetId());

        Asset asset = rvAssetService.createAsset(
            tenantId,
            RvPvtStudyDto.ASSET_TYPE,
            dto.getName(),
            dto.getLabel()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        savePvtAttributes(dto);

        // Link to reservoir if specified
        if (dto.getReservoirAssetId() != null) {
            rvHierarchyService.createCharacterizedByRelation(tenantId, dto.getReservoirAssetId(), dto.getAssetId());
        }

        // Link to source well if specified
        if (dto.getWellAssetId() != null) {
            rvHierarchyService.createRelation(tenantId, dto.getWellAssetId(), dto.getAssetId(), "SampleFrom");
        }

        log.info("PVT study created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a PVT Study by ID.
     */
    public Optional<RvPvtStudyDto> getPvtStudyById(UUID assetId) {
        return rvAssetService.getAssetById(assetId)
            .filter(a -> RvPvtStudyDto.ASSET_TYPE.equals(a.getType()))
            .map(asset -> {
                RvPvtStudyDto dto = new RvPvtStudyDto();
                dto.setAssetId(asset.getId().getId());
                dto.setTenantId(asset.getTenantId().getId());
                dto.setName(asset.getName());
                dto.setLabel(asset.getLabel());
                dto.setCreatedTime(asset.getCreatedTime());

                loadPvtAttributes(dto);

                return dto;
            });
    }

    /**
     * Gets all PVT Studies for a tenant.
     */
    public Page<RvPvtStudyDto> getAllPvtStudies(UUID tenantId, int page, int size) {
        Page<Asset> assets = rvAssetService.getAssetsByType(tenantId, RvPvtStudyDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Gets PVT Studies by Reservoir.
     */
    public List<RvPvtStudyDto> getPvtStudiesByReservoir(UUID tenantId, UUID reservoirAssetId) {
        List<UUID> pvtIds = rvHierarchyService.getRelatedAssets(tenantId, reservoirAssetId, "CharacterizedBy");
        List<RvPvtStudyDto> pvtStudies = new ArrayList<>();

        for (UUID pvtId : pvtIds) {
            getPvtStudyById(pvtId).ifPresent(pvtStudies::add);
        }

        return pvtStudies;
    }

    /**
     * Updates a PVT Study.
     */
    public RvPvtStudyDto updatePvtStudy(RvPvtStudyDto dto) {
        log.info("Updating PVT study: {}", dto.getAssetId());

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
        savePvtAttributes(dto);

        return dto;
    }

    /**
     * Calculates PVT properties using correlations if lab data not available.
     * Returns a new DTO with calculated values.
     */
    public RvPvtStudyDto calculatePvtFromCorrelations(UUID assetId, BigDecimal temperature,
                                                        BigDecimal rs, BigDecimal gasGravity, BigDecimal apiGravity) {
        log.info("Calculating PVT properties for study {} using correlations", assetId);

        RvPvtStudyDto dto = getPvtStudyById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("PVT Study", assetId));

        // Calculate bubble point using Standing correlation
        BigDecimal pb = calculationService.calculatePbStanding(rs, gasGravity, temperature, apiGravity);
        dto.setBubblePointPressurePsi(pb);

        // Calculate oil FVF using Standing correlation
        BigDecimal oilGravity = BigDecimal.valueOf(141.5).divide(
            apiGravity.add(BigDecimal.valueOf(131.5)), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal bo = calculationService.calculateBoStanding(rs, gasGravity, oilGravity, temperature);
        dto.setOilFvfAtPbRbStb(bo);

        // Calculate oil viscosity using Beggs-Robinson
        BigDecimal viscosity = calculationService.calculateViscosityBeggsRobinson(apiGravity, temperature);
        dto.setStockTankOilViscosityCp(viscosity);

        // Mark as using correlations
        dto.setUsesCorrelations(true);
        dto.setPbCorrelation("STANDING");
        dto.setBoCorrelation("STANDING");
        dto.setViscosityCorrelation("BEGGS_ROBINSON");

        // Update the study with calculated values
        dto.setUpdatedTime(System.currentTimeMillis());
        savePvtAttributes(dto);

        log.info("PVT correlations calculated: Pb={} psi, Bo={} rb/stb", pb, bo);
        return dto;
    }

    /**
     * Validates PVT data consistency.
     */
    public Map<String, String> validatePvtData(UUID assetId) {
        log.info("Validating PVT data for study: {}", assetId);
        Map<String, String> issues = new HashMap<>();

        RvPvtStudyDto dto = getPvtStudyById(assetId)
            .orElseThrow(() -> new RvEntityNotFoundException("PVT Study", assetId));

        // Check API gravity range
        if (dto.getApiGravity() != null) {
            if (dto.getApiGravity().compareTo(BigDecimal.ZERO) < 0 ||
                dto.getApiGravity().compareTo(BigDecimal.valueOf(70)) > 0) {
                issues.put("apiGravity", "API gravity should be between 0 and 70");
            }
        }

        // Check bubble point vs sample pressure
        if (dto.getBubblePointPressurePsi() != null && dto.getSamplePressurePsi() != null) {
            if (dto.getBubblePointPressurePsi().compareTo(dto.getSamplePressurePsi()) > 0) {
                issues.put("bubblePoint", "Bubble point should not exceed sample pressure");
            }
        }

        // Check oil FVF reasonable range
        if (dto.getOilFvfAtPbRbStb() != null) {
            if (dto.getOilFvfAtPbRbStb().compareTo(BigDecimal.valueOf(0.9)) < 0 ||
                dto.getOilFvfAtPbRbStb().compareTo(BigDecimal.valueOf(3.0)) > 0) {
                issues.put("oilFvf", "Oil FVF typically ranges from 0.9 to 3.0 rb/stb");
            }
        }

        // Check gas specific gravity range
        if (dto.getGasSpecificGravity() != null) {
            if (dto.getGasSpecificGravity().compareTo(BigDecimal.valueOf(0.5)) < 0 ||
                dto.getGasSpecificGravity().compareTo(BigDecimal.valueOf(1.5)) > 0) {
                issues.put("gasGravity", "Gas specific gravity typically ranges from 0.5 to 1.5");
            }
        }

        // Check foamy oil consistency
        if (Boolean.TRUE.equals(dto.getHasFoamyBehavior()) && dto.getPseudoBubblePointPsi() == null) {
            issues.put("foamyOil", "Pseudo bubble point required for foamy oil");
        }

        return issues;
    }

    /**
     * Deletes a PVT Study.
     */
    public void deletePvtStudy(UUID tenantId, UUID assetId) {
        log.warn("Deleting PVT study: {}", assetId);
        rvHierarchyService.deleteAllRelations(tenantId, assetId);
        rvAssetService.deleteAsset(tenantId, assetId);
    }

    private void savePvtAttributes(RvPvtStudyDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getStudyCode() != null) attrs.put(RvPvtStudyDto.ATTR_STUDY_CODE, dto.getStudyCode());
        if (dto.getSampleDate() != null) attrs.put(RvPvtStudyDto.ATTR_SAMPLE_DATE, dto.getSampleDate());
        if (dto.getLaboratoryName() != null) attrs.put(RvPvtStudyDto.ATTR_LABORATORY, dto.getLaboratoryName());
        if (dto.getSampleType() != null) attrs.put("sample_type", dto.getSampleType());
        if (dto.getSampleDepthM() != null) attrs.put("sample_depth_m", dto.getSampleDepthM());
        if (dto.getSamplePressurePsi() != null) attrs.put("sample_pressure_psi", dto.getSamplePressurePsi());
        if (dto.getSampleTemperatureF() != null) attrs.put("sample_temperature_f", dto.getSampleTemperatureF());
        if (dto.getApiGravity() != null) attrs.put(RvPvtStudyDto.ATTR_API_GRAVITY, dto.getApiGravity());
        if (dto.getSpecificGravityOil() != null) attrs.put("specific_gravity_oil", dto.getSpecificGravityOil());
        if (dto.getBubblePointPressurePsi() != null) attrs.put(RvPvtStudyDto.ATTR_BUBBLE_POINT_PSI, dto.getBubblePointPressurePsi());
        if (dto.getSolutionGorAtPbScfStb() != null) attrs.put(RvPvtStudyDto.ATTR_SOLUTION_GOR, dto.getSolutionGorAtPbScfStb());
        if (dto.getOilFvfAtPbRbStb() != null) attrs.put(RvPvtStudyDto.ATTR_OIL_FVF_AT_PB, dto.getOilFvfAtPbRbStb());
        if (dto.getOilViscosityAtPbCp() != null) attrs.put("oil_viscosity_at_pb_cp", dto.getOilViscosityAtPbCp());
        if (dto.getStockTankOilViscosityCp() != null) attrs.put("stock_tank_oil_viscosity_cp", dto.getStockTankOilViscosityCp());
        if (dto.getGasSpecificGravity() != null) attrs.put(RvPvtStudyDto.ATTR_GAS_SPECIFIC_GRAVITY, dto.getGasSpecificGravity());
        if (dto.getGasZFactorAtPb() != null) attrs.put("gas_z_factor_at_pb", dto.getGasZFactorAtPb());
        if (dto.getWaterSalinity() != null) attrs.put("water_salinity_ppm", dto.getWaterSalinity());
        if (dto.getWaterFvfAtReservoirRbStb() != null) attrs.put("water_fvf", dto.getWaterFvfAtReservoirRbStb());
        if (dto.getUsesCorrelations() != null) attrs.put(RvPvtStudyDto.ATTR_USES_CORRELATIONS, dto.getUsesCorrelations());
        if (dto.getPbCorrelation() != null) attrs.put("pb_correlation", dto.getPbCorrelation());
        if (dto.getBoCorrelation() != null) attrs.put("bo_correlation", dto.getBoCorrelation());
        if (dto.getViscosityCorrelation() != null) attrs.put("viscosity_correlation", dto.getViscosityCorrelation());
        if (dto.getHasFoamyBehavior() != null) attrs.put(RvPvtStudyDto.ATTR_HAS_FOAMY_BEHAVIOR, dto.getHasFoamyBehavior());
        if (dto.getPseudoBubblePointPsi() != null) attrs.put("pseudo_bubble_point_psi", dto.getPseudoBubblePointPsi());
        if (dto.getFoamCriticalGasSaturation() != null) attrs.put("foam_critical_gas_saturation", dto.getFoamCriticalGasSaturation());

        if (!attrs.isEmpty()) {
            rvAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadPvtAttributes(RvPvtStudyDto dto) {
        List<AttributeKvEntry> entries = rvAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String key = entry.getKey();
            switch (key) {
                case RvPvtStudyDto.ATTR_STUDY_CODE -> dto.setStudyCode(entry.getValueAsString());
                case RvPvtStudyDto.ATTR_SAMPLE_DATE -> entry.getLongValue().ifPresent(dto::setSampleDate);
                case RvPvtStudyDto.ATTR_LABORATORY -> dto.setLaboratoryName(entry.getValueAsString());
                case "sample_type" -> dto.setSampleType(entry.getValueAsString());
                case "sample_depth_m" -> entry.getDoubleValue().ifPresent(v -> dto.setSampleDepthM(BigDecimal.valueOf(v)));
                case "sample_pressure_psi" -> entry.getDoubleValue().ifPresent(v -> dto.setSamplePressurePsi(BigDecimal.valueOf(v)));
                case "sample_temperature_f" -> entry.getDoubleValue().ifPresent(v -> dto.setSampleTemperatureF(BigDecimal.valueOf(v)));
                case RvPvtStudyDto.ATTR_API_GRAVITY -> entry.getDoubleValue().ifPresent(v -> dto.setApiGravity(BigDecimal.valueOf(v)));
                case "specific_gravity_oil" -> entry.getDoubleValue().ifPresent(v -> dto.setSpecificGravityOil(BigDecimal.valueOf(v)));
                case RvPvtStudyDto.ATTR_BUBBLE_POINT_PSI -> entry.getDoubleValue().ifPresent(v -> dto.setBubblePointPressurePsi(BigDecimal.valueOf(v)));
                case RvPvtStudyDto.ATTR_SOLUTION_GOR -> entry.getDoubleValue().ifPresent(v -> dto.setSolutionGorAtPbScfStb(BigDecimal.valueOf(v)));
                case RvPvtStudyDto.ATTR_OIL_FVF_AT_PB -> entry.getDoubleValue().ifPresent(v -> dto.setOilFvfAtPbRbStb(BigDecimal.valueOf(v)));
                case "oil_viscosity_at_pb_cp" -> entry.getDoubleValue().ifPresent(v -> dto.setOilViscosityAtPbCp(BigDecimal.valueOf(v)));
                case "stock_tank_oil_viscosity_cp" -> entry.getDoubleValue().ifPresent(v -> dto.setStockTankOilViscosityCp(BigDecimal.valueOf(v)));
                case RvPvtStudyDto.ATTR_GAS_SPECIFIC_GRAVITY -> entry.getDoubleValue().ifPresent(v -> dto.setGasSpecificGravity(BigDecimal.valueOf(v)));
                case "gas_z_factor_at_pb" -> entry.getDoubleValue().ifPresent(v -> dto.setGasZFactorAtPb(BigDecimal.valueOf(v)));
                case "water_salinity_ppm" -> entry.getDoubleValue().ifPresent(v -> dto.setWaterSalinity(BigDecimal.valueOf(v)));
                case "water_fvf" -> entry.getDoubleValue().ifPresent(v -> dto.setWaterFvfAtReservoirRbStb(BigDecimal.valueOf(v)));
                case RvPvtStudyDto.ATTR_USES_CORRELATIONS -> entry.getBooleanValue().ifPresent(dto::setUsesCorrelations);
                case "pb_correlation" -> dto.setPbCorrelation(entry.getValueAsString());
                case "bo_correlation" -> dto.setBoCorrelation(entry.getValueAsString());
                case "viscosity_correlation" -> dto.setViscosityCorrelation(entry.getValueAsString());
                case RvPvtStudyDto.ATTR_HAS_FOAMY_BEHAVIOR -> entry.getBooleanValue().ifPresent(dto::setHasFoamyBehavior);
                case "pseudo_bubble_point_psi" -> entry.getDoubleValue().ifPresent(v -> dto.setPseudoBubblePointPsi(BigDecimal.valueOf(v)));
                case "foam_critical_gas_saturation" -> entry.getDoubleValue().ifPresent(v -> dto.setFoamCriticalGasSaturation(BigDecimal.valueOf(v)));
            }
        }
    }

    private RvPvtStudyDto mapAssetToDto(Asset asset) {
        RvPvtStudyDto dto = new RvPvtStudyDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setLabel(asset.getLabel());
        dto.setCreatedTime(asset.getCreatedTime());
        loadPvtAttributes(dto);
        return dto;
    }
}
