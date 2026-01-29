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
import org.thingsboard.nexus.rv.dto.RvFieldDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvFieldService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Field (Campo) management.
 * Base path: /api/nexus/rv/fields
 */
@RestController
@RequestMapping("/api/nexus/rv/fields")
@RequiredArgsConstructor
@Slf4j
public class RvFieldController {

    private final RvFieldService fieldService;

    /**
     * Create a new Field.
     */
    @PostMapping
    public ResponseEntity<RvFieldDto> createField(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvFieldDto fieldDto) {
        log.info("POST /api/nexus/rv/fields - Creating field: {}", fieldDto.getName());
        RvFieldDto created = fieldService.createField(tenantId, fieldDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Field by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvFieldDto> getFieldById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/fields/{}", id);
        return fieldService.getFieldById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Field", id));
    }

    /**
     * Get all Fields for a tenant with pagination.
     */
    @GetMapping
    public ResponseEntity<Page<RvFieldDto>> getAllFields(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/fields - tenantId={}, page={}, size={}", tenantId, page, size);
        Page<RvFieldDto> fields = fieldService.getAllFields(tenantId, page, size);
        return ResponseEntity.ok(fields);
    }

    /**
     * Get Fields by Basin.
     */
    @GetMapping("/by-basin/{basinId}")
    public ResponseEntity<List<RvFieldDto>> getFieldsByBasin(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID basinId) {
        log.debug("GET /api/nexus/rv/fields/by-basin/{}", basinId);
        List<RvFieldDto> fields = fieldService.getFieldsByBasin(tenantId, basinId);
        return ResponseEntity.ok(fields);
    }

    /**
     * Update a Field.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvFieldDto> updateField(
            @PathVariable UUID id,
            @Valid @RequestBody RvFieldDto fieldDto) {
        log.info("PUT /api/nexus/rv/fields/{}", id);
        fieldDto.setAssetId(id);
        RvFieldDto updated = fieldService.updateField(fieldDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a Field.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteField(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/fields/{}", id);
        fieldService.deleteField(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exception handler for entity not found.
     */
    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
