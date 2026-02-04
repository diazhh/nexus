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
package org.thingsboard.nexus.po.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.po.dto.GasLiftAllocationDto;
import org.thingsboard.nexus.po.dto.OptimizationResultDto;
import org.thingsboard.nexus.po.dto.OptimizationResultDto.OptimizationRunStatus;
import org.thingsboard.nexus.po.dto.OptimizationType;

import java.math.BigDecimal;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.service.PoOptimizationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for production optimization.
 */
@RestController
@RequestMapping("/api/nexus/po/optimization")
@RequiredArgsConstructor
@Slf4j
public class PoOptimizationController {

    private final PoOptimizationService optimizationService;

    /**
     * Runs ESP frequency optimization for a well.
     */
    @PostMapping("/esp/frequency/{wellId}")
    public ResponseEntity<OptimizationResultDto> optimizeEspFrequency(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID wellId) {
        log.info("Running ESP frequency optimization for well: {}", wellId);
        OptimizationResultDto result = optimizationService.optimizeEspFrequency(tenantId, wellId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Runs ESP frequency optimization for all wells.
     */
    @PostMapping("/esp/frequency/all")
    public ResponseEntity<List<OptimizationResultDto>> optimizeAllEspWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Running batch ESP frequency optimization for tenant: {}", tenantId);
        List<OptimizationResultDto> results = optimizationService.optimizeAllEspWells(tenantId, userId);
        return ResponseEntity.ok(results);
    }

    // ==================== GAS LIFT OPTIMIZATION ====================

    /**
     * Runs gas lift allocation optimization for a field.
     */
    @PostMapping("/gaslift/allocation/{fieldId}")
    public ResponseEntity<OptimizationResultDto> optimizeGasLiftAllocation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID fieldId,
            @RequestParam(required = false) BigDecimal totalAvailableGas) {
        log.info("Running gas lift allocation optimization for field: {}", fieldId);
        OptimizationResultDto result = optimizationService.optimizeGasLiftAllocation(tenantId, fieldId, totalAvailableGas, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Gets gas lift allocation preview without saving.
     */
    @GetMapping("/gaslift/allocation/{fieldId}/preview")
    public ResponseEntity<GasLiftAllocationDto> previewGasLiftAllocation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID fieldId,
            @RequestParam(required = false) BigDecimal totalAvailableGas) {
        log.info("Previewing gas lift allocation for field: {}", fieldId);
        GasLiftAllocationDto result = optimizationService.getGasLiftAllocation(tenantId, fieldId, totalAvailableGas);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    // ==================== PCP OPTIMIZATION ====================

    /**
     * Runs PCP speed optimization for a well.
     */
    @PostMapping("/pcp/speed/{wellId}")
    public ResponseEntity<OptimizationResultDto> optimizePcpSpeed(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID wellId) {
        log.info("Running PCP speed optimization for well: {}", wellId);
        OptimizationResultDto result = optimizationService.optimizePcpSpeed(tenantId, wellId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Runs PCP speed optimization for all PCP wells.
     */
    @PostMapping("/pcp/speed/all")
    public ResponseEntity<List<OptimizationResultDto>> optimizeAllPcpWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Running batch PCP speed optimization for tenant: {}", tenantId);
        List<OptimizationResultDto> results = optimizationService.optimizeAllPcpWells(tenantId, userId);
        return ResponseEntity.ok(results);
    }

    // ==================== ROD PUMP OPTIMIZATION ====================

    /**
     * Runs rod pump optimization for a well.
     */
    @PostMapping("/rodpump/{wellId}")
    public ResponseEntity<OptimizationResultDto> optimizeRodPump(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID wellId) {
        log.info("Running rod pump optimization for well: {}", wellId);
        OptimizationResultDto result = optimizationService.optimizeRodPump(tenantId, wellId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Runs rod pump optimization for all rod pump wells.
     */
    @PostMapping("/rodpump/all")
    public ResponseEntity<List<OptimizationResultDto>> optimizeAllRodPumpWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Running batch rod pump optimization for tenant: {}", tenantId);
        List<OptimizationResultDto> results = optimizationService.optimizeAllRodPumpWells(tenantId, userId);
        return ResponseEntity.ok(results);
    }

    // ==================== RESULT QUERIES ====================

    /**
     * Gets an optimization result by ID.
     */
    @GetMapping("/results/{id}")
    public ResponseEntity<OptimizationResultDto> getResult(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        return optimizationService.getResult(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PoEntityNotFoundException("OptimizationResult", id));
    }

    /**
     * Gets all optimization results for a tenant.
     */
    @GetMapping("/results")
    public ResponseEntity<Page<OptimizationResultDto>> getResults(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(optimizationService.getResults(tenantId, page, size));
    }

    /**
     * Gets optimization results for an asset.
     */
    @GetMapping("/results/asset/{assetId}")
    public ResponseEntity<Page<OptimizationResultDto>> getResultsForAsset(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID assetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(optimizationService.getResultsForAsset(assetId, page, size));
    }

    /**
     * Gets latest optimization result for an asset.
     */
    @GetMapping("/results/asset/{assetId}/latest")
    public ResponseEntity<OptimizationResultDto> getLatestResult(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID assetId) {
        return optimizationService.getLatestResult(assetId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets latest optimization result for an asset and type.
     */
    @GetMapping("/results/asset/{assetId}/latest/{type}")
    public ResponseEntity<OptimizationResultDto> getLatestResultByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID assetId,
            @PathVariable OptimizationType type) {
        return optimizationService.getLatestResult(assetId, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets optimization results by type.
     */
    @GetMapping("/results/by-type/{type}")
    public ResponseEntity<Page<OptimizationResultDto>> getResultsByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable OptimizationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(optimizationService.getResultsByType(tenantId, type, page, size));
    }

    /**
     * Gets optimization results in time range.
     */
    @GetMapping("/results/time-range")
    public ResponseEntity<List<OptimizationResultDto>> getResultsInTimeRange(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam Long from,
            @RequestParam Long to) {
        return ResponseEntity.ok(optimizationService.getResultsInTimeRange(tenantId, from, to));
    }

    /**
     * Gets optimization counts.
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getOptimizationCounts(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(Map.of(
                "completed", optimizationService.countByStatus(tenantId, OptimizationRunStatus.COMPLETED),
                "failed", optimizationService.countByStatus(tenantId, OptimizationRunStatus.FAILED),
                "running", optimizationService.countByStatus(tenantId, OptimizationRunStatus.RUNNING),
                "espFrequency", optimizationService.countByType(tenantId, OptimizationType.ESP_FREQUENCY),
                "gasLift", optimizationService.countByType(tenantId, OptimizationType.GAS_LIFT_ALLOCATION),
                "pcpSpeed", optimizationService.countByType(tenantId, OptimizationType.PCP_SPEED),
                "rodPump", optimizationService.countByType(tenantId, OptimizationType.ROD_PUMP_SPEED)
        ));
    }
}
