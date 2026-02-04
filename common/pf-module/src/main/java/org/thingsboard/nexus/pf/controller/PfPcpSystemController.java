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
import org.thingsboard.nexus.pf.dto.PfPcpSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfPcpSystemService;
import org.thingsboard.nexus.pf.service.PfPcpSystemService.PcpHealthStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for PCP (Progressing Cavity Pump) System management.
 */
@RestController
@RequestMapping("/api/nexus/pf/pcp-systems")
@RequiredArgsConstructor
@Slf4j
public class PfPcpSystemController {

    private final PfPcpSystemService pcpSystemService;

    /**
     * Creates a new PCP system.
     */
    @PostMapping
    public ResponseEntity<PfPcpSystemDto> createPcpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfPcpSystemDto dto) {
        log.info("Creating PCP system for well: {}", dto.getWellId());
        dto.setTenantId(tenantId);
        PfPcpSystemDto created = pcpSystemService.createPcpSystem(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a PCP system by ID.
     */
    @GetMapping("/{systemId}")
    public ResponseEntity<PfPcpSystemDto> getPcpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        return pcpSystemService.getPcpSystemById(systemId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("PCP System", systemId));
    }

    /**
     * Gets PCP system by well ID.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<PfPcpSystemDto> getPcpSystemByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        return pcpSystemService.getPcpSystemByWell(tenantId, wellId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets all PCP systems for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfPcpSystemDto>> getAllPcpSystems(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pcpSystemService.getAllPcpSystems(tenantId, page, size));
    }

    /**
     * Updates a PCP system.
     */
    @PutMapping("/{systemId}")
    public ResponseEntity<PfPcpSystemDto> updatePcpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @Valid @RequestBody PfPcpSystemDto dto) {
        log.info("Updating PCP system: {}", systemId);
        dto.setAssetId(systemId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(pcpSystemService.updatePcpSystem(dto));
    }

    /**
     * Records a pulling event.
     */
    @PostMapping("/{systemId}/pulling")
    public ResponseEntity<Void> recordPulling(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @RequestBody Map<String, String> body) {
        LocalDate pullingDate = LocalDate.parse(body.getOrDefault("date", LocalDate.now().toString()));
        pcpSystemService.recordPulling(systemId, pullingDate);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets health status of a PCP system.
     */
    @GetMapping("/{systemId}/health")
    public ResponseEntity<PcpHealthStatus> getHealthStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        return ResponseEntity.ok(pcpSystemService.getHealthStatus(systemId));
    }

    /**
     * Deletes a PCP system.
     */
    @DeleteMapping("/{systemId}")
    public ResponseEntity<Void> deletePcpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        log.warn("Deleting PCP system: {}", systemId);
        pcpSystemService.deletePcpSystem(tenantId, systemId);
        return ResponseEntity.noContent().build();
    }
}
