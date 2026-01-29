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
import org.thingsboard.nexus.rv.dto.RvReservoirDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvReservoirService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Reservoir (Yacimiento) management.
 * Base path: /api/nexus/rv/reservoirs
 */
@RestController
@RequestMapping("/api/nexus/rv/reservoirs")
@RequiredArgsConstructor
@Slf4j
public class RvReservoirController {

    private final RvReservoirService reservoirService;

    /**
     * Create a new Reservoir.
     */
    @PostMapping
    public ResponseEntity<RvReservoirDto> createReservoir(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvReservoirDto dto) {
        log.info("POST /api/nexus/rv/reservoirs - Creating reservoir: {}", dto.getName());
        RvReservoirDto created = reservoirService.createReservoir(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Reservoir by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvReservoirDto> getReservoirById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/reservoirs/{}", id);
        return reservoirService.getReservoirById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Reservoir", id));
    }

    /**
     * Get all Reservoirs for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<RvReservoirDto>> getAllReservoirs(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/reservoirs - tenantId={}", tenantId);
        Page<RvReservoirDto> reservoirs = reservoirService.getAllReservoirs(tenantId, page, size);
        return ResponseEntity.ok(reservoirs);
    }

    /**
     * Get Reservoirs by Field.
     */
    @GetMapping("/by-field/{fieldId}")
    public ResponseEntity<List<RvReservoirDto>> getReservoirsByField(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID fieldId) {
        log.debug("GET /api/nexus/rv/reservoirs/by-field/{}", fieldId);
        List<RvReservoirDto> reservoirs = reservoirService.getReservoirsByField(tenantId, fieldId);
        return ResponseEntity.ok(reservoirs);
    }

    /**
     * Update a Reservoir.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvReservoirDto> updateReservoir(
            @PathVariable UUID id,
            @Valid @RequestBody RvReservoirDto dto) {
        log.info("PUT /api/nexus/rv/reservoirs/{}", id);
        dto.setAssetId(id);
        RvReservoirDto updated = reservoirService.updateReservoir(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Calculate OOIP for a Reservoir.
     */
    @PostMapping("/{id}/calculate-ooip")
    public ResponseEntity<Map<String, Object>> calculateOOIP(@PathVariable UUID id) {
        log.info("POST /api/nexus/rv/reservoirs/{}/calculate-ooip", id);
        BigDecimal ooip = reservoirService.calculateOOIP(id);
        return ResponseEntity.ok(Map.of(
            "reservoirId", id,
            "ooip_mmbbl", ooip,
            "calculatedAt", System.currentTimeMillis()
        ));
    }

    /**
     * Associate a PVT Study with a Reservoir.
     */
    @PostMapping("/{id}/pvt-studies/{pvtStudyId}")
    public ResponseEntity<Void> associatePvtStudy(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id,
            @PathVariable UUID pvtStudyId) {
        log.info("POST /api/nexus/rv/reservoirs/{}/pvt-studies/{}", id, pvtStudyId);
        reservoirService.associatePvtStudy(tenantId, id, pvtStudyId);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a Reservoir.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservoir(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/reservoirs/{}", id);
        reservoirService.deleteReservoir(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
