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
import org.thingsboard.nexus.rv.dto.RvWellLogRunDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.service.RvWellLogService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Well Log Run assets.
 */
@RestController
@RequestMapping("/api/nexus/rv/well-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RV Well Logs", description = "Well Log Run Management")
public class RvWellLogController {

    private final RvWellLogService wellLogService;

    @GetMapping
    @Operation(summary = "Get all well log runs")
    public ResponseEntity<PageData<RvWellLogRunDto>> getAllWellLogRuns(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /well-logs - tenantId={}, page={}, size={}", tenantId, page, size);
        return ResponseEntity.ok(toPageData(wellLogService.getAllWellLogRuns(tenantId, page, size)));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get well log run by ID")
    public ResponseEntity<RvWellLogRunDto> getWellLogRunById(@PathVariable UUID id) {
        log.debug("GET /well-logs/{}", id);
        return wellLogService.getWellLogRunById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-well/{wellId}")
    @Operation(summary = "Get well log runs by well")
    public ResponseEntity<List<RvWellLogRunDto>> getWellLogRunsByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        log.debug("GET /well-logs/by-well/{}", wellId);
        return ResponseEntity.ok(wellLogService.getWellLogRunsByWell(tenantId, wellId));
    }

    @GetMapping("/by-type/{loggingType}")
    @Operation(summary = "Get well log runs by type (WIRELINE, LWD, MWD)")
    public ResponseEntity<List<RvWellLogRunDto>> getWellLogRunsByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable String loggingType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /well-logs/by-type/{}", loggingType);
        return ResponseEntity.ok(wellLogService.getWellLogRunsByType(tenantId, loggingType, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a new well log run")
    public ResponseEntity<RvWellLogRunDto> createWellLogRun(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody RvWellLogRunDto dto) {
        log.info("POST /well-logs - name={}, well={}", dto.getName(), dto.getWellId());
        dto.setTenantId(tenantId);
        RvWellLogRunDto created = wellLogService.createWellLogRun(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a well log run")
    public ResponseEntity<RvWellLogRunDto> updateWellLogRun(
            @PathVariable UUID id,
            @RequestBody RvWellLogRunDto dto) {
        log.info("PUT /well-logs/{}", id);
        dto.setId(id);
        return ResponseEntity.ok(wellLogService.updateWellLogRun(dto));
    }

    @PostMapping("/{id}/interpretation")
    @Operation(summary = "Update interpretation results")
    public ResponseEntity<Void> updateInterpretationResults(
            @PathVariable UUID id,
            @RequestParam String interpreter,
            @RequestParam(required = false) BigDecimal avgPorosity,
            @RequestParam(required = false) BigDecimal avgWaterSat,
            @RequestParam(required = false) BigDecimal avgVshale,
            @RequestParam(required = false) BigDecimal netPay,
            @RequestParam(required = false) BigDecimal grossThickness) {
        log.info("POST /well-logs/{}/interpretation", id);
        wellLogService.updateInterpretationResults(id, interpreter, avgPorosity, avgWaterSat, avgVshale, netPay, grossThickness);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a well log run")
    public ResponseEntity<Void> deleteWellLogRun(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /well-logs/{}", id);
        wellLogService.deleteWellLogRun(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
