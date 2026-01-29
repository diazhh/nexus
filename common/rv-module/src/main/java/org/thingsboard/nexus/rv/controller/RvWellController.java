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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvWellDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvWellService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Well (Pozo) management.
 * Base path: /api/nexus/rv/wells
 */
@RestController
@RequestMapping("/api/nexus/rv/wells")
@RequiredArgsConstructor
@Slf4j
public class RvWellController {

    private final RvWellService wellService;

    /**
     * Create a new Well.
     */
    @PostMapping
    public ResponseEntity<RvWellDto> createWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvWellDto dto) {
        log.info("POST /api/nexus/rv/wells - Creating well: {}", dto.getName());
        RvWellDto created = wellService.createWell(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Well by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvWellDto> getWellById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/wells/{}", id);
        return wellService.getWellById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Well", id));
    }

    /**
     * Get all Wells for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<RvWellDto>> getAllWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/wells - tenantId={}", tenantId);
        Page<RvWellDto> wells = wellService.getAllWells(tenantId, page, size);
        return ResponseEntity.ok(wells);
    }

    /**
     * Get Wells by Reservoir.
     */
    @GetMapping("/by-reservoir/{reservoirId}")
    public ResponseEntity<List<RvWellDto>> getWellsByReservoir(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID reservoirId) {
        log.debug("GET /api/nexus/rv/wells/by-reservoir/{}", reservoirId);
        List<RvWellDto> wells = wellService.getWellsByReservoir(tenantId, reservoirId);
        return ResponseEntity.ok(wells);
    }

    /**
     * Update a Well.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvWellDto> updateWell(
            @PathVariable UUID id,
            @Valid @RequestBody RvWellDto dto) {
        log.info("PUT /api/nexus/rv/wells/{}", id);
        dto.setAssetId(id);
        RvWellDto updated = wellService.updateWell(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update Well status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateWellStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        log.info("PATCH /api/nexus/rv/wells/{}/status", id);
        wellService.updateWellStatus(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    /**
     * Calculate Productivity Index for a Well.
     */
    @PostMapping("/{id}/calculate-pi")
    public ResponseEntity<Map<String, Object>> calculateProductivityIndex(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/wells/{}/calculate-pi", id);
        BigDecimal pi = wellService.calculateProductivityIndex(
            id,
            params.get("testRate"),
            params.get("reservoirPressure"),
            params.get("flowingPressure")
        );
        return ResponseEntity.ok(Map.of(
            "wellId", id,
            "productivityIndex_bpd_psi", pi,
            "calculatedAt", System.currentTimeMillis()
        ));
    }

    /**
     * Link Well to Drilling Job (integration hook).
     */
    @PostMapping("/{id}/link-drilling/{drillingJobId}")
    public ResponseEntity<Void> linkToDrillingJob(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @PathVariable UUID drillingJobId) {
        log.info("POST /api/nexus/rv/wells/{}/link-drilling/{}", id, drillingJobId);
        wellService.linkToDrillingJob(tenantId, id, drillingJobId);
        return ResponseEntity.ok().build();
    }

    /**
     * Link Well to Production Unit (integration hook).
     */
    @PostMapping("/{id}/link-production/{productionUnitId}")
    public ResponseEntity<Void> linkToProductionUnit(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @PathVariable UUID productionUnitId) {
        log.info("POST /api/nexus/rv/wells/{}/link-production/{}", id, productionUnitId);
        wellService.linkToProductionUnit(tenantId, id, productionUnitId);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a Well.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/wells/{}", id);
        wellService.deleteWell(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
