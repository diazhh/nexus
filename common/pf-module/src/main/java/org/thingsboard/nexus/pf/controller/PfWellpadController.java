/*
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
import org.thingsboard.nexus.pf.dto.OperationalStatus;
import org.thingsboard.nexus.pf.dto.PfWellpadDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfWellpadService;
import org.thingsboard.nexus.pf.service.PfWellpadService.WellpadSummary;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Wellpad management.
 */
@RestController
@RequestMapping("/api/nexus/pf/wellpads")
@RequiredArgsConstructor
@Slf4j
public class PfWellpadController {

    private final PfWellpadService wellpadService;

    /**
     * Creates a new wellpad.
     */
    @PostMapping
    public ResponseEntity<PfWellpadDto> createWellpad(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfWellpadDto dto) {
        log.info("Creating wellpad: {}", dto.getName());
        dto.setTenantId(tenantId);
        PfWellpadDto created = wellpadService.createWellpad(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a wellpad by ID.
     */
    @GetMapping("/{wellpadId}")
    public ResponseEntity<PfWellpadDto> getWellpad(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellpadId) {
        return wellpadService.getWellpadById(wellpadId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("Wellpad", wellpadId));
    }

    /**
     * Gets all wellpads for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfWellpadDto>> getAllWellpads(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(wellpadService.getAllWellpads(tenantId, page, size));
    }

    /**
     * Gets wellpads by flow station.
     */
    @GetMapping("/by-flow-station/{flowStationId}")
    public ResponseEntity<List<PfWellpadDto>> getWellpadsByFlowStation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID flowStationId) {
        return ResponseEntity.ok(wellpadService.getWellpadsByFlowStation(tenantId, flowStationId));
    }

    /**
     * Updates a wellpad.
     */
    @PutMapping("/{wellpadId}")
    public ResponseEntity<PfWellpadDto> updateWellpad(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellpadId,
            @Valid @RequestBody PfWellpadDto dto) {
        log.info("Updating wellpad: {}", wellpadId);
        dto.setAssetId(wellpadId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(wellpadService.updateWellpad(dto));
    }

    /**
     * Updates wellpad operational status.
     */
    @PatchMapping("/{wellpadId}/status")
    public ResponseEntity<Void> updateWellpadStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellpadId,
            @RequestBody Map<String, String> body) {
        OperationalStatus status = OperationalStatus.valueOf(body.get("status"));
        wellpadService.updateOperationalStatus(wellpadId, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a wellpad.
     */
    @DeleteMapping("/{wellpadId}")
    public ResponseEntity<Void> deleteWellpad(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellpadId) {
        log.warn("Deleting wellpad: {}", wellpadId);
        wellpadService.deleteWellpad(tenantId, wellpadId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets wellpad summary for dashboard.
     */
    @GetMapping("/summary")
    public ResponseEntity<WellpadSummary> getWellpadSummary(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(wellpadService.getWellpadSummary(tenantId));
    }
}
