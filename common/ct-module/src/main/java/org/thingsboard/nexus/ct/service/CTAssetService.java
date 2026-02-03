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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;

import java.util.Optional;
import java.util.UUID;

/**
 * Service wrapper for ThingsBoard Asset operations in CT module.
 * Provides simplified access to Asset CRUD operations for CT entities (Units, Reels).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CTAssetService {

    private final AssetService assetService;

    // Asset type constants for CT module
    public static final String TYPE_UNIT = "ct_unit";
    public static final String TYPE_REEL = "ct_reel";

    /**
     * Create a new asset
     */
    public Asset createAsset(UUID tenantId, String assetType, String name, String label) {
        log.debug("Creating asset: type={}, name={}, tenant={}", assetType, name, tenantId);

        Asset asset = new Asset();
        asset.setTenantId(new TenantId(tenantId));
        asset.setType(assetType);
        asset.setName(name);
        asset.setLabel(label);

        Asset savedAsset = assetService.saveAsset(asset);
        log.info("Asset created: id={}, type={}, name={}", savedAsset.getId().getId(), assetType, name);

        return savedAsset;
    }

    /**
     * Get asset by ID
     */
    public Optional<Asset> getAssetById(UUID assetId) {
        try {
            Asset asset = assetService.findAssetById(null, new AssetId(assetId));
            return Optional.ofNullable(asset);
        } catch (Exception e) {
            log.error("Error finding asset by id {}: {}", assetId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get assets by type for a tenant with pagination
     */
    public Page<Asset> getAssetsByType(UUID tenantId, String assetType, int page, int size) {
        log.debug("Getting assets by type: tenant={}, type={}, page={}, size={}", tenantId, assetType, page, size);

        PageLink pageLink = new PageLink(size, page);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
                new TenantId(tenantId), assetType, pageLink);

        return new PageImpl<>(pageData.getData(),
                Pageable.ofSize(size).withPage(page),
                pageData.getTotalElements());
    }

    /**
     * Search assets by name
     */
    public Page<Asset> searchAssetsByName(UUID tenantId, String assetType, String searchText, int page, int size) {
        log.debug("Searching assets: tenant={}, type={}, search={}", tenantId, assetType, searchText);

        PageLink pageLink = new PageLink(size, page, searchText);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
                new TenantId(tenantId), assetType, pageLink);

        return new PageImpl<>(pageData.getData(),
                Pageable.ofSize(size).withPage(page),
                pageData.getTotalElements());
    }

    /**
     * Update an existing asset
     */
    public Asset updateAsset(Asset asset) {
        log.debug("Updating asset: id={}", asset.getId().getId());
        return assetService.saveAsset(asset);
    }

    /**
     * Delete an asset
     */
    public void deleteAsset(UUID tenantId, UUID assetId) {
        log.info("Deleting asset: id={}", assetId);
        assetService.deleteAsset(new TenantId(tenantId), new AssetId(assetId));
    }

    /**
     * Check if asset exists
     */
    public boolean assetExists(UUID assetId) {
        return getAssetById(assetId).isPresent();
    }

    /**
     * Count assets by type for a tenant
     */
    public long countAssetsByType(UUID tenantId, String assetType) {
        PageLink pageLink = new PageLink(1);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
                new TenantId(tenantId), assetType, pageLink);
        return pageData.getTotalElements();
    }
}
