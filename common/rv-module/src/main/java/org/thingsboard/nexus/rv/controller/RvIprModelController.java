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
import org.thingsboard.nexus.rv.dto.RvIprModelDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvIprModelService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for IPR (Inflow Performance Relationship) Model management.
 * Base path: /api/nexus/rv/ipr-models
 */
@RestController
@RequestMapping("/api/nexus/rv/ipr-models")
@RequiredArgsConstructor
@Slf4j
public class RvIprModelController {

    private final RvIprModelService iprService;

    /**
     * Create a new IPR Model.
     */
    @PostMapping
    public ResponseEntity<RvIprModelDto> createIprModel(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvIprModelDto dto) {
        log.info("POST /api/nexus/rv/ipr-models - Creating IPR model: {}", dto.getName());
        RvIprModelDto created = iprService.createIprModel(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get an IPR Model by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvIprModelDto> getIprModelById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/ipr-models/{}", id);
        return iprService.getIprModelById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("IPR Model", id));
    }

    /**
     * Get all IPR Models for a tenant.
     */
    @GetMapping
    public ResponseEntity<PageData<RvIprModelDto>> getAllIprModels(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/ipr-models - tenantId={}", tenantId);
        Page<RvIprModelDto> models = iprService.getAllIprModels(tenantId, page, size);
        return ResponseEntity.ok(toPageData(models));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * Get IPR Models by Well.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<List<RvIprModelDto>> getIprModelsByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        log.debug("GET /api/nexus/rv/ipr-models/by-well/{}", wellId);
        List<RvIprModelDto> models = iprService.getIprModelsByWell(tenantId, wellId);
        return ResponseEntity.ok(models);
    }

    /**
     * Calculate Vogel IPR from test data.
     */
    @PostMapping("/{id}/calculate-vogel")
    public ResponseEntity<RvIprModelDto> calculateVogelIpr(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/ipr-models/{}/calculate-vogel", id);

        RvIprModelDto result = iprService.calculateVogelIpr(
            id,
            params.get("reservoirPressure"),
            params.get("bubblePointPressure"),
            params.get("testRate"),
            params.get("testPwf")
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Generate IPR curve data points.
     */
    @GetMapping("/{id}/curve")
    public ResponseEntity<List<Map<String, Object>>> generateIprCurve(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int numPoints) {
        log.info("GET /api/nexus/rv/ipr-models/{}/curve", id);
        List<Map<String, Object>> curve = iprService.generateIprCurve(id, numPoints);
        return ResponseEntity.ok(curve);
    }

    /**
     * Calculate operating point efficiency.
     */
    @PostMapping("/{id}/operating-point")
    public ResponseEntity<Map<String, Object>> calculateOperatingPoint(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/ipr-models/{}/operating-point", id);
        Map<String, Object> result = iprService.calculateOperatingPoint(id, params.get("currentPwf"));
        return ResponseEntity.ok(result);
    }

    /**
     * Update an IPR Model.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvIprModelDto> updateIprModel(
            @PathVariable UUID id,
            @Valid @RequestBody RvIprModelDto dto) {
        log.info("PUT /api/nexus/rv/ipr-models/{}", id);
        dto.setAssetId(id);
        RvIprModelDto updated = iprService.updateIprModel(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an IPR Model.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIprModel(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/ipr-models/{}", id);
        iprService.deleteIprModel(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
