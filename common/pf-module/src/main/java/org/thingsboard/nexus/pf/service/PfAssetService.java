/*
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service wrapper over ThingsBoard AssetService for managing
 * Production Facilities (PF) specific assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAssetService {

    private final AssetService assetService;

    // Asset Types for Production Facilities module
    public static final String TYPE_WELL = "pf_well";
    public static final String TYPE_WELLPAD = "pf_wellpad";
    public static final String TYPE_FLOW_STATION = "pf_flow_station";
    public static final String TYPE_SEPARATOR = "pf_separator";
    public static final String TYPE_TANK = "pf_tank";
    public static final String TYPE_PIPELINE = "pf_pipeline";
    public static final String TYPE_ESP_SYSTEM = "pf_esp_system";
    public static final String TYPE_PCP_SYSTEM = "pf_pcp_system";
    public static final String TYPE_GAS_LIFT_SYSTEM = "pf_gas_lift_system";
    public static final String TYPE_ROD_PUMP_SYSTEM = "pf_rod_pump_system";

    /**
     * Creates a new PF Asset.
     */
    public Asset createAsset(UUID tenantId, String assetType, String name, String label) {
        log.info("Creating PF asset: type={}, name={}", assetType, name);

        Asset asset = new Asset();
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setType(assetType);
        asset.setName(name);
        asset.setLabel(label);

        Asset savedAsset = assetService.saveAsset(asset);
        log.debug("Created PF asset with ID: {}", savedAsset.getId());

        return savedAsset;
    }

    /**
     * Gets an Asset by its ID.
     */
    public Optional<Asset> getAssetById(UUID assetId) {
        try {
            Asset asset = assetService.findAssetById(null, new AssetId(assetId));
            return Optional.ofNullable(asset);
        } catch (Exception e) {
            log.error("Error getting asset {}: {}", assetId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets all Assets of a specific type for a tenant.
     */
    public Page<Asset> getAssetsByType(UUID tenantId, String assetType, int page, int size) {
        log.debug("Getting PF assets: tenantId={}, type={}, page={}, size={}", tenantId, assetType, page, size);

        PageLink pageLink = new PageLink(size, page);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
                TenantId.fromUUID(tenantId),
                assetType,
                pageLink
        );

        return new PageImpl<>(
                pageData.getData(),
                PageRequest.of(page, size),
                pageData.getTotalElements()
        );
    }

    /**
     * Searches Assets by name.
     */
    public Page<Asset> searchAssetsByName(UUID tenantId, String assetType, String searchText, int page, int size) {
        log.debug("Searching PF assets: tenantId={}, type={}, search={}", tenantId, assetType, searchText);

        PageLink pageLink = new PageLink(size, page, searchText);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
                TenantId.fromUUID(tenantId),
                assetType,
                pageLink
        );

        return new PageImpl<>(
                pageData.getData(),
                PageRequest.of(page, size),
                pageData.getTotalElements()
        );
    }

    /**
     * Updates an existing Asset.
     */
    public Asset updateAsset(Asset asset) {
        log.info("Updating PF asset: {}", asset.getId());
        return assetService.saveAsset(asset);
    }

    /**
     * Deletes an Asset.
     */
    public void deleteAsset(UUID tenantId, UUID assetId) {
        log.warn("Deleting PF asset: {}", assetId);
        assetService.deleteAsset(TenantId.fromUUID(tenantId), new AssetId(assetId));
    }

    /**
     * Checks if an Asset exists.
     */
    public boolean existsById(UUID assetId) {
        return getAssetById(assetId).isPresent();
    }

    /**
     * Gets all PF Asset types.
     */
    public List<String> getAllPfAssetTypes() {
        return List.of(
                TYPE_WELL,
                TYPE_WELLPAD,
                TYPE_FLOW_STATION,
                TYPE_SEPARATOR,
                TYPE_TANK,
                TYPE_PIPELINE,
                TYPE_ESP_SYSTEM,
                TYPE_PCP_SYSTEM,
                TYPE_GAS_LIFT_SYSTEM,
                TYPE_ROD_PUMP_SYSTEM
        );
    }

    /**
     * Validates that the asset type is valid for the PF module.
     */
    public boolean isValidPfAssetType(String assetType) {
        return getAllPfAssetTypes().contains(assetType);
    }

    /**
     * Counts Assets by type.
     */
    public long countByType(UUID tenantId, String assetType) {
        PageLink pageLink = new PageLink(1, 0);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
                TenantId.fromUUID(tenantId),
                assetType,
                pageLink
        );
        return pageData.getTotalElements();
    }
}
