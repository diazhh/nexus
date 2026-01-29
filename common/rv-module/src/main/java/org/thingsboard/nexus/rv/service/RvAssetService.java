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
import java.util.stream.Collectors;

/**
 * Servicio wrapper sobre AssetService de ThingsBoard para gestionar
 * Assets específicos del módulo de Yacimientos (rv_*).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvAssetService {

    private final AssetService assetService;
    private final RvAttributeService rvAttributeService;

    // Asset Types del módulo de Yacimientos
    public static final String TYPE_BASIN = "rv_basin";
    public static final String TYPE_FIELD = "rv_field";
    public static final String TYPE_RESERVOIR = "rv_reservoir";
    public static final String TYPE_ZONE = "rv_zone";
    public static final String TYPE_WELL = "rv_well";
    public static final String TYPE_COMPLETION = "rv_completion";
    public static final String TYPE_SEISMIC_SURVEY = "rv_seismic_survey";
    public static final String TYPE_FAULT = "rv_fault";
    public static final String TYPE_WELL_LOG_RUN = "rv_well_log_run";
    public static final String TYPE_CORE = "rv_core";
    public static final String TYPE_PVT_STUDY = "rv_pvt_study";
    public static final String TYPE_IPR_MODEL = "rv_ipr_model";
    public static final String TYPE_DECLINE_ANALYSIS = "rv_decline_analysis";
    public static final String TYPE_GEOMECHANICAL_MODEL = "rv_geomechanical_model";
    public static final String TYPE_FOAMY_OIL_MODEL = "rv_foamy_oil_model";
    public static final String TYPE_DILUENT_MODEL = "rv_diluent_model";

    /**
     * Crea un nuevo Asset de yacimiento.
     */
    public Asset createAsset(UUID tenantId, String assetType, String name, String label) {
        log.info("Creating RV asset: type={}, name={}", assetType, name);

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
     * Obtiene todos los tipos de Assets del módulo RV.
     */
    public List<String> getAllRvAssetTypes() {
        return List.of(
            TYPE_BASIN,
            TYPE_FIELD,
            TYPE_RESERVOIR,
            TYPE_ZONE,
            TYPE_WELL,
            TYPE_COMPLETION,
            TYPE_SEISMIC_SURVEY,
            TYPE_FAULT,
            TYPE_WELL_LOG_RUN,
            TYPE_CORE,
            TYPE_PVT_STUDY,
            TYPE_IPR_MODEL,
            TYPE_DECLINE_ANALYSIS,
            TYPE_GEOMECHANICAL_MODEL,
            TYPE_FOAMY_OIL_MODEL,
            TYPE_DILUENT_MODEL
        );
    }

    /**
     * Valida que el tipo de Asset sea válido para el módulo RV.
     */
    public boolean isValidRvAssetType(String assetType) {
        return getAllRvAssetTypes().contains(assetType);
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
