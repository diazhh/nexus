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
package org.thingsboard.nexus.pf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.OperationalStatus;
import org.thingsboard.nexus.pf.dto.PfFlowStationDto;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Flow Station entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfFlowStationService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;

    /**
     * Creates a new Flow Station.
     */
    public PfFlowStationDto createFlowStation(UUID tenantId, PfFlowStationDto dto) {
        log.info("Creating flow station: {}", dto.getName());

        Asset asset = pfAssetService.createAsset(
                tenantId,
                PfFlowStationDto.ASSET_TYPE,
                dto.getName(),
                dto.getName()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        saveFlowStationAttributes(dto);

        log.info("Flow station created with ID: {}", dto.getAssetId());
        return dto;
    }

    /**
     * Gets a Flow Station by ID.
     */
    public Optional<PfFlowStationDto> getFlowStationById(UUID assetId) {
        return pfAssetService.getAssetById(assetId)
                .filter(a -> PfFlowStationDto.ASSET_TYPE.equals(a.getType()))
                .map(asset -> {
                    PfFlowStationDto dto = new PfFlowStationDto();
                    dto.setAssetId(asset.getId().getId());
                    dto.setTenantId(asset.getTenantId().getId());
                    dto.setName(asset.getName());
                    dto.setCreatedTime(asset.getCreatedTime());

                    loadFlowStationAttributes(dto);

                    return dto;
                });
    }

    /**
     * Gets all Flow Stations for a tenant.
     */
    public Page<PfFlowStationDto> getAllFlowStations(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(tenantId, PfFlowStationDto.ASSET_TYPE, page, size);
        return assets.map(this::mapAssetToDto);
    }

    /**
     * Updates a Flow Station.
     */
    public PfFlowStationDto updateFlowStation(PfFlowStationDto dto) {
        log.info("Updating flow station: {}", dto.getAssetId());

        pfAssetService.getAssetById(dto.getAssetId()).ifPresent(asset -> {
            if (!asset.getName().equals(dto.getName())) {
                asset.setName(dto.getName());
                pfAssetService.updateAsset(asset);
            }
        });

        dto.setUpdatedTime(System.currentTimeMillis());
        saveFlowStationAttributes(dto);

        return dto;
    }

    /**
     * Updates current throughput.
     */
    public void updateThroughput(UUID assetId, BigDecimal throughputBpd) {
        log.debug("Updating flow station {} throughput to {} BPD", assetId, throughputBpd);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PfFlowStationDto.ATTR_CURRENT_THROUGHPUT_BPD, throughputBpd);
        pfAttributeService.saveServerAttributes(assetId, attrs);
    }

    /**
     * Deletes a Flow Station.
     */
    public void deleteFlowStation(UUID tenantId, UUID assetId) {
        log.warn("Deleting flow station: {}", assetId);

        // Check for connected wellpads
        List<UUID> wellpadIds = pfHierarchyService.getChildren(tenantId, assetId);
        if (!wellpadIds.isEmpty()) {
            throw new IllegalStateException("Cannot delete flow station with " + wellpadIds.size() + " wellpads connected.");
        }

        pfHierarchyService.removeAllRelations(tenantId, assetId);
        pfAssetService.deleteAsset(tenantId, assetId);
    }

    /**
     * Gets flow station utilization percentage.
     */
    public BigDecimal getUtilization(UUID assetId) {
        return getFlowStationById(assetId)
                .map(dto -> {
                    if (dto.getDesignCapacityBpd() != null && dto.getCurrentThroughputBpd() != null
                            && dto.getDesignCapacityBpd().compareTo(BigDecimal.ZERO) > 0) {
                        return dto.getCurrentThroughputBpd()
                                .divide(dto.getDesignCapacityBpd(), 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }
                    return BigDecimal.ZERO;
                })
                .orElse(BigDecimal.ZERO);
    }

    // Helper methods

    private void saveFlowStationAttributes(PfFlowStationDto dto) {
        Map<String, Object> attrs = new HashMap<>();

        if (dto.getCode() != null) attrs.put(PfFlowStationDto.ATTR_CODE, dto.getCode());
        if (dto.getLatitude() != null) attrs.put(PfFlowStationDto.ATTR_LATITUDE, dto.getLatitude());
        if (dto.getLongitude() != null) attrs.put(PfFlowStationDto.ATTR_LONGITUDE, dto.getLongitude());
        if (dto.getDesignCapacityBpd() != null) attrs.put(PfFlowStationDto.ATTR_DESIGN_CAPACITY_BPD, dto.getDesignCapacityBpd());
        if (dto.getCurrentThroughputBpd() != null) attrs.put(PfFlowStationDto.ATTR_CURRENT_THROUGHPUT_BPD, dto.getCurrentThroughputBpd());
        if (dto.getCommissioningDate() != null) attrs.put(PfFlowStationDto.ATTR_COMMISSIONING_DATE, dto.getCommissioningDate().toString());
        if (dto.getOperationalStatus() != null) attrs.put(PfFlowStationDto.ATTR_OPERATIONAL_STATUS, dto.getOperationalStatus().name());
        if (dto.getSeparatorCount() != null) attrs.put(PfFlowStationDto.ATTR_SEPARATOR_COUNT, dto.getSeparatorCount());
        if (dto.getTankCount() != null) attrs.put(PfFlowStationDto.ATTR_TANK_COUNT, dto.getTankCount());

        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }
    }

    private void loadFlowStationAttributes(PfFlowStationDto dto) {
        List<AttributeKvEntry> entries = pfAttributeService.getServerAttributes(dto.getAssetId());

        for (AttributeKvEntry entry : entries) {
            String strValue = entry.getValueAsString();
            switch (entry.getKey()) {
                case PfFlowStationDto.ATTR_CODE -> dto.setCode(strValue);
                case PfFlowStationDto.ATTR_LATITUDE -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setLatitude(BigDecimal.valueOf(v)));
                case PfFlowStationDto.ATTR_LONGITUDE -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setLongitude(BigDecimal.valueOf(v)));
                case PfFlowStationDto.ATTR_DESIGN_CAPACITY_BPD -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setDesignCapacityBpd(BigDecimal.valueOf(v)));
                case PfFlowStationDto.ATTR_CURRENT_THROUGHPUT_BPD -> entry.getDoubleValue()
                        .ifPresent(v -> dto.setCurrentThroughputBpd(BigDecimal.valueOf(v)));
                case PfFlowStationDto.ATTR_COMMISSIONING_DATE -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setCommissioningDate(LocalDate.parse(strValue));
                    }
                }
                case PfFlowStationDto.ATTR_OPERATIONAL_STATUS -> {
                    if (strValue != null && !strValue.isEmpty()) {
                        dto.setOperationalStatus(OperationalStatus.valueOf(strValue));
                    }
                }
                case PfFlowStationDto.ATTR_SEPARATOR_COUNT -> entry.getLongValue()
                        .ifPresent(v -> dto.setSeparatorCount(v.intValue()));
                case PfFlowStationDto.ATTR_TANK_COUNT -> entry.getLongValue()
                        .ifPresent(v -> dto.setTankCount(v.intValue()));
            }
        }
    }

    private PfFlowStationDto mapAssetToDto(Asset asset) {
        PfFlowStationDto dto = new PfFlowStationDto();
        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(asset.getTenantId().getId());
        dto.setName(asset.getName());
        dto.setCreatedTime(asset.getCreatedTime());
        loadFlowStationAttributes(dto);
        return dto;
    }
}
