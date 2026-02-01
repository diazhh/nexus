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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvFaultDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.service.RvFaultService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Fault assets.
 */
@RestController
@RequestMapping("/api/nexus/rv/faults")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RV Faults", description = "Geological Fault Management")
public class RvFaultController {

    private final RvFaultService faultService;

    @GetMapping
    @Operation(summary = "Get all faults")
    public ResponseEntity<PageData<RvFaultDto>> getAllFaults(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /faults - tenantId={}, page={}, size={}", tenantId, page, size);
        return ResponseEntity.ok(toPageData(faultService.getAllFaults(tenantId, page, size)));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fault by ID")
    public ResponseEntity<RvFaultDto> getFaultById(@PathVariable UUID id) {
        log.debug("GET /faults/{}", id);
        return faultService.getFaultById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-field/{fieldId}")
    @Operation(summary = "Get faults by field")
    public ResponseEntity<List<RvFaultDto>> getFaultsByField(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID fieldId) {
        log.debug("GET /faults/by-field/{}", fieldId);
        return ResponseEntity.ok(faultService.getFaultsByField(tenantId, fieldId));
    }

    @GetMapping("/by-seismic/{surveyId}")
    @Operation(summary = "Get faults by seismic survey")
    public ResponseEntity<List<RvFaultDto>> getFaultsBySeismicSurvey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID surveyId) {
        log.debug("GET /faults/by-seismic/{}", surveyId);
        return ResponseEntity.ok(faultService.getFaultsBySeismicSurvey(tenantId, surveyId));
    }

    @GetMapping("/sealing")
    @Operation(summary = "Get sealing faults")
    public ResponseEntity<List<RvFaultDto>> getSealingFaults(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /faults/sealing");
        return ResponseEntity.ok(faultService.getSealingFaults(tenantId, page, size));
    }

    @GetMapping("/compartmentalizing")
    @Operation(summary = "Get compartmentalizing faults")
    public ResponseEntity<List<RvFaultDto>> getCompartmentalizingFaults(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /faults/compartmentalizing");
        return ResponseEntity.ok(faultService.getCompartmentalizingFaults(tenantId, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a new fault")
    public ResponseEntity<RvFaultDto> createFault(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody RvFaultDto dto) {
        log.info("POST /faults - name={}, type={}", dto.getName(), dto.getFaultType());
        dto.setTenantId(tenantId);
        RvFaultDto created = faultService.createFault(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fault")
    public ResponseEntity<RvFaultDto> updateFault(
            @PathVariable UUID id,
            @RequestBody RvFaultDto dto) {
        log.info("PUT /faults/{}", id);
        dto.setId(id);
        return ResponseEntity.ok(faultService.updateFault(dto));
    }

    @PostMapping("/{id}/sealing-analysis")
    @Operation(summary = "Update sealing analysis results")
    public ResponseEntity<Void> updateSealingAnalysis(
            @PathVariable UUID id,
            @RequestParam String sealingPotential,
            @RequestParam(required = false) BigDecimal sgr,
            @RequestParam(required = false) BigDecimal csp,
            @RequestParam(required = false) Boolean compartmentalizing) {
        log.info("POST /faults/{}/sealing-analysis", id);
        faultService.updateSealingAnalysis(id, sealingPotential, sgr, csp, compartmentalizing);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fault")
    public ResponseEntity<Void> deleteFault(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /faults/{}", id);
        faultService.deleteFault(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
