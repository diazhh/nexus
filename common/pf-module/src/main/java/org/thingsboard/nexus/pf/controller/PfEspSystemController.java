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
import org.thingsboard.nexus.pf.dto.PfEspSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfEspSystemService;
import org.thingsboard.nexus.pf.service.PfEspSystemService.EspHealthStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for ESP System management.
 */
@RestController
@RequestMapping("/api/nexus/pf/esp-systems")
@RequiredArgsConstructor
@Slf4j
public class PfEspSystemController {

    private final PfEspSystemService espSystemService;

    /**
     * Creates a new ESP system.
     */
    @PostMapping
    public ResponseEntity<PfEspSystemDto> createEspSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfEspSystemDto dto) {
        log.info("Creating ESP system for well: {}", dto.getWellId());
        dto.setTenantId(tenantId);
        PfEspSystemDto created = espSystemService.createEspSystem(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets an ESP system by ID.
     */
    @GetMapping("/{espId}")
    public ResponseEntity<PfEspSystemDto> getEspSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID espId) {
        return espSystemService.getEspSystemById(espId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("ESP System", espId));
    }

    /**
     * Gets ESP system by well ID.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<PfEspSystemDto> getEspSystemByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        return espSystemService.getEspSystemByWell(tenantId, wellId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets all ESP systems for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfEspSystemDto>> getAllEspSystems(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(espSystemService.getAllEspSystems(tenantId, page, size));
    }

    /**
     * Updates an ESP system.
     */
    @PutMapping("/{espId}")
    public ResponseEntity<PfEspSystemDto> updateEspSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID espId,
            @Valid @RequestBody PfEspSystemDto dto) {
        log.info("Updating ESP system: {}", espId);
        dto.setAssetId(espId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(espSystemService.updateEspSystem(dto));
    }

    /**
     * Records a pulling event.
     */
    @PostMapping("/{espId}/pulling")
    public ResponseEntity<Void> recordPulling(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID espId,
            @RequestBody Map<String, String> body) {
        LocalDate pullingDate = LocalDate.parse(body.get("pullingDate"));
        espSystemService.recordPulling(espId, pullingDate);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets ESP health status.
     */
    @GetMapping("/{espId}/health")
    public ResponseEntity<EspHealthStatus> getEspHealth(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID espId) {
        return ResponseEntity.ok(espSystemService.getHealthStatus(espId));
    }

    /**
     * Deletes an ESP system.
     */
    @DeleteMapping("/{espId}")
    public ResponseEntity<Void> deleteEspSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID espId) {
        log.warn("Deleting ESP system: {}", espId);
        espSystemService.deleteEspSystem(tenantId, espId);
        return ResponseEntity.noContent().build();
    }
}
