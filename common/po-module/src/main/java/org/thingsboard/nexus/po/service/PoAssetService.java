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
package org.thingsboard.nexus.po.service;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Wrapper service for ThingsBoard AssetService.
 * Provides simplified access to PF assets for the PO module.
 */
@Service("poAssetService")
@RequiredArgsConstructor
@Slf4j
public class PoAssetService {

    private final AssetService tbAssetService;

    // PF Asset types that PO module works with
    public static final String ASSET_TYPE_WELL = "pf_well";
    public static final String ASSET_TYPE_WELLPAD = "pf_wellpad";
    public static final String ASSET_TYPE_FIELD = "pf_field";
    public static final String ASSET_TYPE_ESP = "pf_esp_system";
    public static final String ASSET_TYPE_PCP = "pf_pcp_system";
    public static final String ASSET_TYPE_GAS_LIFT = "pf_gas_lift_system";
    public static final String ASSET_TYPE_ROD_PUMP = "pf_rod_pump_system";
    public static final String ASSET_TYPE_FLOW_STATION = "pf_flow_station";

    /**
     * Gets an asset by ID.
     */
    public Optional<Asset> getAssetById(UUID assetId) {
        try {
            Asset asset = tbAssetService.findAssetById(null, new AssetId(assetId));
            return Optional.ofNullable(asset);
        } catch (Exception e) {
            log.error("Error getting asset {}: {}", assetId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets an asset by ID with tenant validation.
     */
    public Optional<Asset> getAssetById(UUID tenantId, UUID assetId) {
        try {
            TenantId tbTenantId = TenantId.fromUUID(tenantId);
            Asset asset = tbAssetService.findAssetById(tbTenantId, new AssetId(assetId));
            if (asset != null && asset.getTenantId().getId().equals(tenantId)) {
                return Optional.of(asset);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting asset {} for tenant {}: {}", assetId, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets assets by type for a tenant.
     */
    public Page<Asset> getAssetsByType(UUID tenantId, String assetType, int page, int size) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        PageLink pageLink = new PageLink(size, page);

        PageData<Asset> pageData = tbAssetService.findAssetsByTenantIdAndType(tbTenantId, assetType, pageLink);

        return new PageImpl<>(
                pageData.getData(),
                PageRequest.of(page, size),
                pageData.getTotalElements()
        );
    }

    /**
     * Gets all wells for a tenant.
     */
    public Page<Asset> getWells(UUID tenantId, int page, int size) {
        return getAssetsByType(tenantId, ASSET_TYPE_WELL, page, size);
    }

    /**
     * Gets all fields for a tenant.
     */
    public Page<Asset> getFields(UUID tenantId, int page, int size) {
        return getAssetsByType(tenantId, ASSET_TYPE_FIELD, page, size);
    }

    /**
     * Gets all ESP systems for a tenant.
     */
    public Page<Asset> getEspSystems(UUID tenantId, int page, int size) {
        return getAssetsByType(tenantId, ASSET_TYPE_ESP, page, size);
    }

    /**
     * Gets all gas lift systems for a tenant.
     */
    public Page<Asset> getGasLiftSystems(UUID tenantId, int page, int size) {
        return getAssetsByType(tenantId, ASSET_TYPE_GAS_LIFT, page, size);
    }

    /**
     * Gets all PCP systems for a tenant.
     */
    public Page<Asset> getPcpSystems(UUID tenantId, int page, int size) {
        return getAssetsByType(tenantId, ASSET_TYPE_PCP, page, size);
    }

    /**
     * Gets all rod pump systems for a tenant.
     */
    public Page<Asset> getRodPumpSystems(UUID tenantId, int page, int size) {
        return getAssetsByType(tenantId, ASSET_TYPE_ROD_PUMP, page, size);
    }

    /**
     * Checks if an asset exists.
     */
    public boolean existsById(UUID assetId) {
        return getAssetById(assetId).isPresent();
    }

    /**
     * Checks if an asset exists for tenant.
     */
    public boolean existsById(UUID tenantId, UUID assetId) {
        return getAssetById(tenantId, assetId).isPresent();
    }

    /**
     * Gets all lift system assets for a well.
     * This queries for ESP, PCP, Gas Lift, and Rod Pump assets related to a well.
     */
    public List<Asset> getLiftSystemsForWell(UUID tenantId, UUID wellAssetId) {
        // This would typically use TB relations to find child lift systems
        // For now, return empty list - would need RelationService integration
        log.debug("Getting lift systems for well: {}", wellAssetId);
        return new ArrayList<>();
    }

    /**
     * Gets asset name by ID.
     */
    public String getAssetName(UUID assetId) {
        return getAssetById(assetId)
                .map(Asset::getName)
                .orElse("Unknown");
    }

    /**
     * Gets asset type by ID.
     */
    public String getAssetType(UUID assetId) {
        return getAssetById(assetId)
                .map(Asset::getType)
                .orElse("unknown");
    }
}
