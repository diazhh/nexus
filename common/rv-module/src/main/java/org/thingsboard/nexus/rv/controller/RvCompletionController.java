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
import org.thingsboard.nexus.rv.dto.RvCompletionDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvCompletionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Completion (Completación) management.
 * Base path: /api/nexus/rv/completions
 */
@RestController
@RequestMapping("/api/nexus/rv/completions")
@RequiredArgsConstructor
@Slf4j
public class RvCompletionController {

    private final RvCompletionService completionService;

    /**
     * Create a new Completion.
     */
    @PostMapping
    public ResponseEntity<RvCompletionDto> createCompletion(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvCompletionDto dto) {
        log.info("POST /api/nexus/rv/completions - Creating completion: {}", dto.getName());
        RvCompletionDto created = completionService.createCompletion(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Completion by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvCompletionDto> getCompletionById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/completions/{}", id);
        return completionService.getCompletionById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Completion", id));
    }

    /**
     * Get all Completions for a tenant.
     */
    @GetMapping
    public ResponseEntity<PageData<RvCompletionDto>> getAllCompletions(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/completions - tenantId={}", tenantId);
        Page<RvCompletionDto> completions = completionService.getAllCompletions(tenantId, page, size);
        return ResponseEntity.ok(toPageData(completions));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * Get Completions by Well.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<List<RvCompletionDto>> getCompletionsByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        log.debug("GET /api/nexus/rv/completions/by-well/{}", wellId);
        List<RvCompletionDto> completions = completionService.getCompletionsByWell(tenantId, wellId);
        return ResponseEntity.ok(completions);
    }

    /**
     * Update a Completion.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvCompletionDto> updateCompletion(
            @PathVariable UUID id,
            @Valid @RequestBody RvCompletionDto dto) {
        log.info("PUT /api/nexus/rv/completions/{}", id);
        dto.setAssetId(id);
        RvCompletionDto updated = completionService.updateCompletion(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update completion status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateCompletionStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        log.info("PATCH /api/nexus/rv/completions/{}/status", id);
        completionService.updateCompletionStatus(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    /**
     * Record a stimulation treatment.
     */
    @PostMapping("/{id}/stimulation")
    public ResponseEntity<Void> recordStimulation(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        log.info("POST /api/nexus/rv/completions/{}/stimulation", id);
        completionService.recordStimulation(
            id,
            (String) body.get("stimulationType"),
            ((Number) body.get("stimulationDate")).longValue()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Update artificial lift configuration.
     */
    @PutMapping("/{id}/artificial-lift")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> updateArtificialLift(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        log.info("PUT /api/nexus/rv/completions/{}/artificial-lift", id);
        String liftMethod = (String) body.get("liftMethod");
        Map<String, Object> liftParams = (Map<String, Object>) body.get("liftParams");
        if (liftParams == null) {
            liftParams = Map.of();
        }
        completionService.updateArtificialLift(id, liftMethod, liftParams);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a Completion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompletion(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/completions/{}", id);
        completionService.deleteCompletion(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
