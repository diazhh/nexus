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
package org.thingsboard.nexus.dr.service;

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
 * Servicio wrapper sobre AssetService de ThingsBoard para gestionar
 * Assets específicos del módulo de Drilling (dr_*).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrAssetService {

    private final AssetService assetService;
    private final DrAttributeService drAttributeService;

    // Asset Types del módulo de Drilling
    public static final String TYPE_RIG = "dr_rig";
    public static final String TYPE_BHA = "dr_bha";

    /**
     * Crea un nuevo Asset de drilling.
     */
    public Asset createAsset(UUID tenantId, String assetType, String name, String label) {
        log.info("Creating DR asset: type={}, name={}", assetType, name);

        Asset asset = new Asset();
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setType(assetType);
        asset.setName(name);
        asset.setLabel(label);

        Asset savedAsset = assetService.saveAsset(asset);
        log.debug("Created asset with ID: {}", savedAsset.getId());

        return savedAsset;
    }

    /**
     * Obtiene un Asset por su ID.
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
     * Obtiene todos los Assets de un tipo específico para un tenant.
     */
    public Page<Asset> getAssetsByType(UUID tenantId, String assetType, int page, int size) {
        log.debug("Getting assets: tenantId={}, type={}, page={}, size={}", tenantId, assetType, page, size);

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
     * Busca Assets por nombre (parcial).
     */
    public Page<Asset> searchAssetsByName(UUID tenantId, String assetType, String searchText, int page, int size) {
        log.debug("Searching assets: tenantId={}, type={}, search={}", tenantId, assetType, searchText);

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
     * Actualiza un Asset existente.
     */
    public Asset updateAsset(Asset asset) {
        log.info("Updating asset: {}", asset.getId());
        return assetService.saveAsset(asset);
    }

    /**
     * Elimina un Asset.
     */
    public void deleteAsset(UUID tenantId, UUID assetId) {
        log.warn("Deleting asset: {}", assetId);
        assetService.deleteAsset(TenantId.fromUUID(tenantId), new AssetId(assetId));
    }

    /**
     * Verifica si un Asset existe.
     */
    public boolean existsById(UUID assetId) {
        return getAssetById(assetId).isPresent();
    }

    /**
     * Obtiene todos los tipos de Assets del módulo DR.
     */
    public List<String> getAllDrAssetTypes() {
        return List.of(
            TYPE_RIG,
            TYPE_BHA
        );
    }

    /**
     * Valida que el tipo de Asset sea válido para el módulo DR.
     */
    public boolean isValidDrAssetType(String assetType) {
        return getAllDrAssetTypes().contains(assetType);
    }

    /**
     * Cuenta Assets por tipo.
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
