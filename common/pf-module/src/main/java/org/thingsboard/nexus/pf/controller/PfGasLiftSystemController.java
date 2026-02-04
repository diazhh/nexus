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
import org.thingsboard.nexus.pf.dto.PfGasLiftSystemDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfGasLiftSystemService;
import org.thingsboard.nexus.pf.service.PfGasLiftSystemService.GasLiftEfficiency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Gas Lift System management.
 */
@RestController
@RequestMapping("/api/nexus/pf/gas-lift-systems")
@RequiredArgsConstructor
@Slf4j
public class PfGasLiftSystemController {

    private final PfGasLiftSystemService gasLiftSystemService;

    /**
     * Creates a new Gas Lift system.
     */
    @PostMapping
    public ResponseEntity<PfGasLiftSystemDto> createGasLiftSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfGasLiftSystemDto dto) {
        log.info("Creating Gas Lift system for well: {}", dto.getWellId());
        dto.setTenantId(tenantId);
        PfGasLiftSystemDto created = gasLiftSystemService.createGasLiftSystem(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a Gas Lift system by ID.
     */
    @GetMapping("/{systemId}")
    public ResponseEntity<PfGasLiftSystemDto> getGasLiftSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        return gasLiftSystemService.getGasLiftSystemById(systemId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("Gas Lift System", systemId));
    }

    /**
     * Gets Gas Lift system by well ID.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<PfGasLiftSystemDto> getGasLiftSystemByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        return gasLiftSystemService.getGasLiftSystemByWell(tenantId, wellId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets all Gas Lift systems for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfGasLiftSystemDto>> getAllGasLiftSystems(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gasLiftSystemService.getAllGasLiftSystems(tenantId, page, size));
    }

    /**
     * Updates a Gas Lift system.
     */
    @PutMapping("/{systemId}")
    public ResponseEntity<PfGasLiftSystemDto> updateGasLiftSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @Valid @RequestBody PfGasLiftSystemDto dto) {
        log.info("Updating Gas Lift system: {}", systemId);
        dto.setAssetId(systemId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(gasLiftSystemService.updateGasLiftSystem(dto));
    }

    /**
     * Records a survey/optimization date.
     */
    @PostMapping("/{systemId}/survey")
    public ResponseEntity<Void> recordSurvey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @RequestBody Map<String, String> body) {
        LocalDate surveyDate = LocalDate.parse(body.getOrDefault("date", LocalDate.now().toString()));
        gasLiftSystemService.recordSurvey(systemId, surveyDate);
        return ResponseEntity.ok().build();
    }

    /**
     * Calculates gas lift efficiency.
     */
    @PostMapping("/{systemId}/efficiency")
    public ResponseEntity<GasLiftEfficiency> calculateEfficiency(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal injectionRate = body.get("injectionRateMscfd");
        BigDecimal oilProduction = body.get("oilProductionBpd");

        if (injectionRate == null || oilProduction == null || oilProduction.compareTo(BigDecimal.ZERO) == 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(gasLiftSystemService.calculateEfficiency(systemId, injectionRate, oilProduction));
    }

    /**
     * Deletes a Gas Lift system.
     */
    @DeleteMapping("/{systemId}")
    public ResponseEntity<Void> deleteGasLiftSystem(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID systemId) {
        log.warn("Deleting Gas Lift system: {}", systemId);
        gasLiftSystemService.deleteGasLiftSystem(tenantId, systemId);
        return ResponseEntity.noContent().build();
    }
}
