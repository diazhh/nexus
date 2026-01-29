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
import org.thingsboard.nexus.rv.dto.RvBasinDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvBasinService;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Basin (Cuenca) management.
 * Base path: /api/nexus/rv/basins
 */
@RestController
@RequestMapping("/api/nexus/rv/basins")
@RequiredArgsConstructor
@Slf4j
public class RvBasinController {

    private final RvBasinService basinService;

    /**
     * Create a new Basin.
     */
    @PostMapping
    public ResponseEntity<RvBasinDto> createBasin(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvBasinDto basinDto) {
        log.info("POST /api/nexus/rv/basins - Creating basin: {}", basinDto.getName());
        RvBasinDto created = basinService.createBasin(tenantId, basinDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Basin by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvBasinDto> getBasinById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/basins/{}", id);
        return basinService.getBasinById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Basin", id));
    }

    /**
     * Get all Basins for a tenant with pagination.
     */
    @GetMapping
    public ResponseEntity<Page<RvBasinDto>> getAllBasins(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/basins - tenantId={}, page={}, size={}", tenantId, page, size);
        Page<RvBasinDto> basins = basinService.getAllBasins(tenantId, page, size);
        return ResponseEntity.ok(basins);
    }

    /**
     * Search Basins by name.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<RvBasinDto>> searchBasins(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/basins/search - q={}", q);
        Page<RvBasinDto> basins = basinService.searchBasins(tenantId, q, page, size);
        return ResponseEntity.ok(basins);
    }

    /**
     * Update a Basin.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvBasinDto> updateBasin(
            @PathVariable UUID id,
            @Valid @RequestBody RvBasinDto basinDto) {
        log.info("PUT /api/nexus/rv/basins/{}", id);
        basinDto.setAssetId(id);
        RvBasinDto updated = basinService.updateBasin(basinDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a Basin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBasin(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/basins/{}", id);
        basinService.deleteBasin(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get Basin statistics (field count, well count, etc).
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getBasinStatistics(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/basins/{}/statistics", id);
        Map<String, Object> stats = basinService.getBasinStatistics(tenantId, id);
        return ResponseEntity.ok(stats);
    }

    /**
     * Exception handler for entity not found.
     */
    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Exception handler for illegal state (e.g., deleting basin with fields).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
