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
package org.thingsboard.nexus.pf.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.pf.dto.PfWellDto;
import org.thingsboard.nexus.pf.dto.WellStatus;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfWellService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Production Well management.
 */
@RestController
@RequestMapping("/api/nexus/pf/wells")
@RequiredArgsConstructor
@Slf4j
public class PfWellController {

    private final PfWellService wellService;

    /**
     * Creates a new production well.
     */
    @PostMapping
    public ResponseEntity<PfWellDto> createWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfWellDto dto) {
        log.info("Creating well: {}", dto.getName());
        dto.setTenantId(tenantId);
        PfWellDto created = wellService.createWell(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a well by ID.
     */
    @GetMapping("/{wellId}")
    public ResponseEntity<PfWellDto> getWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        return wellService.getWellById(wellId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("Well", wellId));
    }

    /**
     * Gets all wells for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfWellDto>> getAllWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(wellService.getAllWells(tenantId, page, size));
    }

    /**
     * Gets wells by wellpad.
     */
    @GetMapping("/by-wellpad/{wellpadId}")
    public ResponseEntity<List<PfWellDto>> getWellsByWellpad(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellpadId) {
        return ResponseEntity.ok(wellService.getWellsByWellpad(tenantId, wellpadId));
    }

    /**
     * Gets wells by status.
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<PfWellDto>> getWellsByStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable WellStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(wellService.getWellsByStatus(tenantId, status, page, size));
    }

    /**
     * Updates a well.
     */
    @PutMapping("/{wellId}")
    public ResponseEntity<PfWellDto> updateWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId,
            @Valid @RequestBody PfWellDto dto) {
        log.info("Updating well: {}", wellId);
        dto.setAssetId(wellId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(wellService.updateWell(dto));
    }

    /**
     * Updates well status.
     */
    @PatchMapping("/{wellId}/status")
    public ResponseEntity<Void> updateWellStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId,
            @RequestBody Map<String, String> body) {
        WellStatus status = WellStatus.valueOf(body.get("status"));
        wellService.updateWellStatus(wellId, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a well.
     */
    @DeleteMapping("/{wellId}")
    public ResponseEntity<Void> deleteWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        log.warn("Deleting well: {}", wellId);
        wellService.deleteWell(tenantId, wellId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets well counts by status (for dashboard).
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<WellStatus, Long>> getWellCounts(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(wellService.countWellsByStatus(tenantId));
    }
}
