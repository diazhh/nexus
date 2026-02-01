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
import org.thingsboard.nexus.rv.dto.RvZoneDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvZoneService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Zone (Zona/Formación) management.
 * Base path: /api/nexus/rv/zones
 */
@RestController
@RequestMapping("/api/nexus/rv/zones")
@RequiredArgsConstructor
@Slf4j
public class RvZoneController {

    private final RvZoneService zoneService;

    /**
     * Create a new Zone.
     */
    @PostMapping
    public ResponseEntity<RvZoneDto> createZone(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvZoneDto dto) {
        log.info("POST /api/nexus/rv/zones - Creating zone: {}", dto.getName());
        RvZoneDto created = zoneService.createZone(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Zone by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvZoneDto> getZoneById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/zones/{}", id);
        return zoneService.getZoneById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Zone", id));
    }

    /**
     * Get all Zones for a tenant.
     */
    @GetMapping
    public ResponseEntity<PageData<RvZoneDto>> getAllZones(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/zones - tenantId={}", tenantId);
        Page<RvZoneDto> zones = zoneService.getAllZones(tenantId, page, size);
        return ResponseEntity.ok(toPageData(zones));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * Get Zones by Reservoir.
     */
    @GetMapping("/by-reservoir/{reservoirId}")
    public ResponseEntity<List<RvZoneDto>> getZonesByReservoir(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID reservoirId) {
        log.debug("GET /api/nexus/rv/zones/by-reservoir/{}", reservoirId);
        List<RvZoneDto> zones = zoneService.getZonesByReservoir(tenantId, reservoirId);
        return ResponseEntity.ok(zones);
    }

    /**
     * Update a Zone.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvZoneDto> updateZone(
            @PathVariable UUID id,
            @Valid @RequestBody RvZoneDto dto) {
        log.info("PUT /api/nexus/rv/zones/{}", id);
        dto.setAssetId(id);
        RvZoneDto updated = zoneService.updateZone(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update zone net-to-gross.
     */
    @PatchMapping("/{id}/ntg")
    public ResponseEntity<Void> updateNetToGross(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        log.info("PATCH /api/nexus/rv/zones/{}/ntg", id);
        zoneService.updateNetToGross(id,
            new java.math.BigDecimal(body.get("ntgRatio").toString()));
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a Zone.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/zones/{}", id);
        zoneService.deleteZone(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
