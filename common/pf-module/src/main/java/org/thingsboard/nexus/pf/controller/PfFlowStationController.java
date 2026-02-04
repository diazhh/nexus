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
import org.thingsboard.nexus.pf.dto.PfFlowStationDto;
import org.thingsboard.nexus.pf.exception.PfEntityNotFoundException;
import org.thingsboard.nexus.pf.service.PfFlowStationService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Flow Station management.
 */
@RestController
@RequestMapping("/api/nexus/pf/flow-stations")
@RequiredArgsConstructor
@Slf4j
public class PfFlowStationController {

    private final PfFlowStationService flowStationService;

    /**
     * Creates a new flow station.
     */
    @PostMapping
    public ResponseEntity<PfFlowStationDto> createFlowStation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PfFlowStationDto dto) {
        log.info("Creating flow station: {}", dto.getName());
        dto.setTenantId(tenantId);
        PfFlowStationDto created = flowStationService.createFlowStation(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a flow station by ID.
     */
    @GetMapping("/{flowStationId}")
    public ResponseEntity<PfFlowStationDto> getFlowStation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID flowStationId) {
        return flowStationService.getFlowStationById(flowStationId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PfEntityNotFoundException("FlowStation", flowStationId));
    }

    /**
     * Gets all flow stations for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<PfFlowStationDto>> getAllFlowStations(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(flowStationService.getAllFlowStations(tenantId, page, size));
    }

    /**
     * Updates a flow station.
     */
    @PutMapping("/{flowStationId}")
    public ResponseEntity<PfFlowStationDto> updateFlowStation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID flowStationId,
            @Valid @RequestBody PfFlowStationDto dto) {
        log.info("Updating flow station: {}", flowStationId);
        dto.setAssetId(flowStationId);
        dto.setTenantId(tenantId);
        return ResponseEntity.ok(flowStationService.updateFlowStation(dto));
    }

    /**
     * Updates flow station throughput.
     */
    @PatchMapping("/{flowStationId}/throughput")
    public ResponseEntity<Void> updateThroughput(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID flowStationId,
            @RequestBody Map<String, Object> body) {
        BigDecimal throughput = new BigDecimal(body.get("throughputBpd").toString());
        flowStationService.updateThroughput(flowStationId, throughput);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets flow station utilization percentage.
     */
    @GetMapping("/{flowStationId}/utilization")
    public ResponseEntity<Map<String, BigDecimal>> getUtilization(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID flowStationId) {
        BigDecimal utilization = flowStationService.getUtilization(flowStationId);
        return ResponseEntity.ok(Map.of("utilizationPercent", utilization));
    }

    /**
     * Deletes a flow station.
     */
    @DeleteMapping("/{flowStationId}")
    public ResponseEntity<Void> deleteFlowStation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID flowStationId) {
        log.warn("Deleting flow station: {}", flowStationId);
        flowStationService.deleteFlowStation(tenantId, flowStationId);
        return ResponseEntity.noContent().build();
    }
}
