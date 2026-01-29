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
package org.thingsboard.nexus.rv.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvCatalogDto;
import org.thingsboard.nexus.rv.service.RvCatalogService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing RV catalogs.
 * Provides endpoints for CRUD operations on catalog items like well types, lithologies, etc.
 */
@RestController
@RequestMapping("/api/nexus/rv/catalogs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RV Catalogs", description = "Reservoir Module Catalog Management")
public class RvCatalogController {

    private final RvCatalogService catalogService;

    // ========================================
    // GET Operations
    // ========================================

    @GetMapping("/types")
    @Operation(summary = "Get all available catalog types")
    public ResponseEntity<List<String>> getAvailableCatalogTypes(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/types - tenantId={}", tenantId);
        List<String> types = catalogService.getAvailableCatalogTypes(tenantId);
        return ResponseEntity.ok(types);
    }

    @GetMapping
    @Operation(summary = "Get catalogs by type")
    public ResponseEntity<List<RvCatalogDto>> getCatalogsByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Parameter(description = "Catalog type (e.g., WELL_TYPE, LITHOLOGY)")
            @RequestParam String type) {
        log.debug("GET /catalogs - tenantId={}, type={}", tenantId, type);
        List<RvCatalogDto> catalogs = catalogService.getCatalogsByType(tenantId, type);
        return ResponseEntity.ok(catalogs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a catalog item by ID")
    public ResponseEntity<RvCatalogDto> getCatalogById(
            @PathVariable UUID id) {
        log.debug("GET /catalogs/{}", id);
        return catalogService.getCatalogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-code")
    @Operation(summary = "Get a catalog item by type and code")
    public ResponseEntity<RvCatalogDto> getCatalogByCode(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam String type,
            @RequestParam String code) {
        log.debug("GET /catalogs/by-code - tenantId={}, type={}, code={}", tenantId, type, code);
        return catalogService.getCatalogByCode(tenantId, type, code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search catalogs by name or code")
    public ResponseEntity<List<RvCatalogDto>> searchCatalogs(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam String type,
            @RequestParam String q) {
        log.debug("GET /catalogs/search - tenantId={}, type={}, q={}", tenantId, type, q);
        List<RvCatalogDto> catalogs = catalogService.searchCatalogs(tenantId, type, q);
        return ResponseEntity.ok(catalogs);
    }

    // ========================================
    // Convenience endpoints for specific catalog types
    // ========================================

    @GetMapping("/well-types")
    @Operation(summary = "Get all well types")
    public ResponseEntity<List<RvCatalogDto>> getWellTypes(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/well-types - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getWellTypes(tenantId));
    }

    @GetMapping("/lithologies")
    @Operation(summary = "Get all lithology types")
    public ResponseEntity<List<RvCatalogDto>> getLithologies(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/lithologies - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getLithologies(tenantId));
    }

    @GetMapping("/drive-mechanisms")
    @Operation(summary = "Get all drive mechanism types")
    public ResponseEntity<List<RvCatalogDto>> getDriveMechanisms(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/drive-mechanisms - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getDriveMechanisms(tenantId));
    }

    @GetMapping("/formations-venezuela")
    @Operation(summary = "Get all Venezuelan formations")
    public ResponseEntity<List<RvCatalogDto>> getVenezuelanFormations(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/formations-venezuela - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getVenezuelanFormations(tenantId));
    }

    @GetMapping("/faja-regions")
    @Operation(summary = "Get all Faja del Orinoco regions")
    public ResponseEntity<List<RvCatalogDto>> getFajaRegions(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/faja-regions - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getFajaRegions(tenantId));
    }

    @GetMapping("/completion-types")
    @Operation(summary = "Get all completion types")
    public ResponseEntity<List<RvCatalogDto>> getCompletionTypes(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/completion-types - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getCompletionTypes(tenantId));
    }

    @GetMapping("/artificial-lift")
    @Operation(summary = "Get all artificial lift methods")
    public ResponseEntity<List<RvCatalogDto>> getArtificialLiftMethods(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/artificial-lift - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getArtificialLiftMethods(tenantId));
    }

    @GetMapping("/ipr-methods")
    @Operation(summary = "Get all IPR calculation methods")
    public ResponseEntity<List<RvCatalogDto>> getIprMethods(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/ipr-methods - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getIprMethods(tenantId));
    }

    @GetMapping("/decline-types")
    @Operation(summary = "Get all decline analysis types")
    public ResponseEntity<List<RvCatalogDto>> getDeclineTypes(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("GET /catalogs/decline-types - tenantId={}", tenantId);
        return ResponseEntity.ok(catalogService.getDeclineTypes(tenantId));
    }

    // ========================================
    // POST Operations
    // ========================================

    @PostMapping
    @Operation(summary = "Create a new catalog item")
    public ResponseEntity<RvCatalogDto> createCatalog(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody RvCatalogDto catalog) {
        log.info("POST /catalogs - tenantId={}, type={}, code={}",
                tenantId, catalog.getCatalogType(), catalog.getCode());

        catalog.setTenantId(tenantId);
        RvCatalogDto created = catalogService.createCatalog(catalog);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========================================
    // PUT Operations
    // ========================================

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing catalog item")
    public ResponseEntity<RvCatalogDto> updateCatalog(
            @PathVariable UUID id,
            @RequestBody RvCatalogDto catalog) {
        log.info("PUT /catalogs/{}", id);

        catalog.setId(id);
        RvCatalogDto updated = catalogService.updateCatalog(catalog);
        return ResponseEntity.ok(updated);
    }

    // ========================================
    // DELETE Operations
    // ========================================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a catalog item (soft delete)")
    public ResponseEntity<Void> deleteCatalog(@PathVariable UUID id) {
        log.warn("DELETE /catalogs/{}", id);

        catalogService.deleteCatalog(id);
        return ResponseEntity.noContent().build();
    }
}
