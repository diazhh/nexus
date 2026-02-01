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
import org.thingsboard.nexus.rv.dto.RvPvtStudyDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvPvtStudyService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for PVT Study management.
 * Base path: /api/nexus/rv/pvt-studies
 */
@RestController
@RequestMapping("/api/nexus/rv/pvt-studies")
@RequiredArgsConstructor
@Slf4j
public class RvPvtStudyController {

    private final RvPvtStudyService pvtStudyService;

    /**
     * Create a new PVT Study.
     */
    @PostMapping
    public ResponseEntity<RvPvtStudyDto> createPvtStudy(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvPvtStudyDto dto) {
        log.info("POST /api/nexus/rv/pvt-studies - Creating PVT study: {}", dto.getName());
        RvPvtStudyDto created = pvtStudyService.createPvtStudy(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a PVT Study by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvPvtStudyDto> getPvtStudyById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/pvt-studies/{}", id);
        return pvtStudyService.getPvtStudyById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("PVT Study", id));
    }

    /**
     * Get all PVT Studies for a tenant.
     */
    @GetMapping
    public ResponseEntity<PageData<RvPvtStudyDto>> getAllPvtStudies(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/pvt-studies - tenantId={}", tenantId);
        Page<RvPvtStudyDto> studies = pvtStudyService.getAllPvtStudies(tenantId, page, size);
        return ResponseEntity.ok(toPageData(studies));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * Get PVT Studies by Reservoir.
     */
    @GetMapping("/by-reservoir/{reservoirId}")
    public ResponseEntity<List<RvPvtStudyDto>> getPvtStudiesByReservoir(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID reservoirId) {
        log.debug("GET /api/nexus/rv/pvt-studies/by-reservoir/{}", reservoirId);
        List<RvPvtStudyDto> studies = pvtStudyService.getPvtStudiesByReservoir(tenantId, reservoirId);
        return ResponseEntity.ok(studies);
    }

    /**
     * Update a PVT Study.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvPvtStudyDto> updatePvtStudy(
            @PathVariable UUID id,
            @Valid @RequestBody RvPvtStudyDto dto) {
        log.info("PUT /api/nexus/rv/pvt-studies/{}", id);
        dto.setAssetId(id);
        RvPvtStudyDto updated = pvtStudyService.updatePvtStudy(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Calculate PVT properties using correlations.
     */
    @PostMapping("/{id}/calculate-correlations")
    public ResponseEntity<RvPvtStudyDto> calculateFromCorrelations(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/pvt-studies/{}/calculate-correlations", id);
        RvPvtStudyDto result = pvtStudyService.calculatePvtFromCorrelations(
            id,
            params.get("temperature"),
            params.get("rs"),
            params.get("gasGravity"),
            params.get("apiGravity")
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Validate PVT data consistency.
     */
    @GetMapping("/{id}/validate")
    public ResponseEntity<Map<String, String>> validatePvtData(@PathVariable UUID id) {
        log.info("GET /api/nexus/rv/pvt-studies/{}/validate", id);
        Map<String, String> issues = pvtStudyService.validatePvtData(id);
        return ResponseEntity.ok(issues);
    }

    /**
     * Delete a PVT Study.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePvtStudy(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/pvt-studies/{}", id);
        pvtStudyService.deletePvtStudy(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
