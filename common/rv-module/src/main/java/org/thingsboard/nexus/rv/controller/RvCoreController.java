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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvCoreDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.service.RvCoreService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Core assets.
 */
@RestController
@RequestMapping("/api/nexus/rv/cores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RV Cores", description = "Core Sample Management")
public class RvCoreController {

    private final RvCoreService coreService;

    @GetMapping
    @Operation(summary = "Get all cores")
    public ResponseEntity<PageData<RvCoreDto>> getAllCores(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /cores - tenantId={}, page={}, size={}", tenantId, page, size);
        return ResponseEntity.ok(toPageData(coreService.getAllCores(tenantId, page, size)));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get core by ID")
    public ResponseEntity<RvCoreDto> getCoreById(@PathVariable UUID id) {
        log.debug("GET /cores/{}", id);
        return coreService.getCoreById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-well/{wellId}")
    @Operation(summary = "Get cores by well")
    public ResponseEntity<List<RvCoreDto>> getCoresByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        log.debug("GET /cores/by-well/{}", wellId);
        return ResponseEntity.ok(coreService.getCoresByWell(tenantId, wellId));
    }

    @GetMapping("/with-rca")
    @Operation(summary = "Get cores with RCA completed")
    public ResponseEntity<List<RvCoreDto>> getCoresWithRCA(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /cores/with-rca");
        return ResponseEntity.ok(coreService.getCoresWithRCA(tenantId, page, size));
    }

    @GetMapping("/with-scal")
    @Operation(summary = "Get cores with SCAL completed")
    public ResponseEntity<List<RvCoreDto>> getCoresWithSCAL(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /cores/with-scal");
        return ResponseEntity.ok(coreService.getCoresWithSCAL(tenantId, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a new core")
    public ResponseEntity<RvCoreDto> createCore(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody RvCoreDto dto) {
        log.info("POST /cores - name={}, well={}", dto.getName(), dto.getWellId());
        dto.setTenantId(tenantId);
        RvCoreDto created = coreService.createCore(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a core")
    public ResponseEntity<RvCoreDto> updateCore(
            @PathVariable UUID id,
            @RequestBody RvCoreDto dto) {
        log.info("PUT /cores/{}", id);
        dto.setId(id);
        return ResponseEntity.ok(coreService.updateCore(dto));
    }

    @PostMapping("/{id}/rca-results")
    @Operation(summary = "Update RCA (Routine Core Analysis) results")
    public ResponseEntity<Void> updateRCAResults(
            @PathVariable UUID id,
            @RequestParam String laboratory,
            @RequestParam(required = false) BigDecimal avgPorosity,
            @RequestParam(required = false) BigDecimal avgPermH,
            @RequestParam(required = false) BigDecimal avgPermV,
            @RequestParam(required = false) BigDecimal grainDensity,
            @RequestParam(required = false) BigDecimal avgWaterSat) {
        log.info("POST /cores/{}/rca-results", id);
        coreService.updateRCAResults(id, laboratory, avgPorosity, avgPermH, avgPermV, grainDensity, avgWaterSat);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/scal-results")
    @Operation(summary = "Update SCAL (Special Core Analysis) results")
    public ResponseEntity<Void> updateSCALResults(
            @PathVariable UUID id,
            @RequestParam String laboratory,
            @RequestParam(required = false) BigDecimal swirr,
            @RequestParam(required = false) BigDecimal sor,
            @RequestParam(required = false) BigDecimal krwEndpoint,
            @RequestParam(required = false) BigDecimal kroEndpoint,
            @RequestParam(required = false) BigDecimal wettabilityIndex,
            @RequestParam(required = false) String wettabilityClass) {
        log.info("POST /cores/{}/scal-results", id);
        coreService.updateSCALResults(id, laboratory, swirr, sor, krwEndpoint, kroEndpoint, wettabilityIndex, wettabilityClass);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a core")
    public ResponseEntity<Void> deleteCore(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /cores/{}", id);
        coreService.deleteCore(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
