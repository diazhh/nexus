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
import org.thingsboard.nexus.pf.dto.PfRodPumpSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfRodPumpSystemService;
import org.thingsboard.nexus.pf.service.PfRodPumpSystemService.PumpFillageResult;
import org.thingsboard.nexus.pf.service.PfRodPumpSystemService.RodPumpHealthStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Rod Pump (Sucker Rod Pump / Beam Pump) System management.
 */
@RestController
@RequestMapping("/api/nexus/pf/rod-pump-systems")
@RequiredArgsConstructor
@Slf4j
public class PfRodPumpSystemController {

    private final PfRodPumpSystemService rodPumpSystemService;

    /**
     * Creates a new Rod Pump system.
     */
    @PostMapping
    public ResponseEntity<PfRodPumpSystemDto> createRodPumpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfRodPumpSystemDto dto) {
        log.info("Creating Rod Pump system for well: {}", dto.getWellId());
        dto.setTenantId(tenantId);
        PfRodPumpSystemDto created = rodPumpSystemService.createRodPumpSystem(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a Rod Pump system by ID.
     */
    @GetMapping("/{systemId}")
    public ResponseEntity<PfRodPumpSystemDto> getRodPumpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        return rodPumpSystemService.getRodPumpSystemById(systemId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("Rod Pump System", systemId));
    }

    /**
     * Gets Rod Pump system by well ID.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<PfRodPumpSystemDto> getRodPumpSystemByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        return rodPumpSystemService.getRodPumpSystemByWell(tenantId, wellId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets all Rod Pump systems for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfRodPumpSystemDto>> getAllRodPumpSystems(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rodPumpSystemService.getAllRodPumpSystems(tenantId, page, size));
    }

    /**
     * Updates a Rod Pump system.
     */
    @PutMapping("/{systemId}")
    public ResponseEntity<PfRodPumpSystemDto> updateRodPumpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @Valid @RequestBody PfRodPumpSystemDto dto) {
        log.info("Updating Rod Pump system: {}", systemId);
        dto.setAssetId(systemId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(rodPumpSystemService.updateRodPumpSystem(dto));
    }

    /**
     * Records a dynamometer survey.
     */
    @PostMapping("/{systemId}/dynamometer")
    public ResponseEntity<Void> recordDynamometerSurvey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @RequestBody Map<String, String> body) {
        LocalDate surveyDate = LocalDate.parse(body.getOrDefault("date", LocalDate.now().toString()));
        rodPumpSystemService.recordDynamometerSurvey(systemId, surveyDate);
        return ResponseEntity.ok().build();
    }

    /**
     * Estimates pump fillage from dynamometer card data.
     */
    @PostMapping("/{systemId}/fillage")
    public ResponseEntity<PumpFillageResult> estimatePumpFillage(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal peakLoad = body.get("peakLoadLb");
        BigDecimal minLoad = body.get("minLoadLb");
        BigDecimal theoreticalFluidLoad = body.get("theoreticalFluidLoadLb");

        if (peakLoad == null || minLoad == null || theoreticalFluidLoad == null ||
                theoreticalFluidLoad.compareTo(BigDecimal.ZERO) == 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(rodPumpSystemService.estimatePumpFillage(systemId, peakLoad, minLoad, theoreticalFluidLoad));
    }

    /**
     * Gets health status of a Rod Pump system.
     */
    @GetMapping("/{systemId}/health")
    public ResponseEntity<RodPumpHealthStatus> getHealthStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        return ResponseEntity.ok(rodPumpSystemService.getHealthStatus(systemId));
    }

    /**
     * Deletes a Rod Pump system.
     */
    @DeleteMapping("/{systemId}")
    public ResponseEntity<Void> deleteRodPumpSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        log.warn("Deleting Rod Pump system: {}", systemId);
        rodPumpSystemService.deleteRodPumpSystem(tenantId, systemId);
        return ResponseEntity.noContent().build();
    }
}
