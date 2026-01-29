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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvDeclineAnalysisDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.nexus.rv.service.RvDeclineAnalysisService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Decline Curve Analysis management.
 * Base path: /api/nexus/rv/decline-analyses
 */
@RestController
@RequestMapping("/api/nexus/rv/decline-analyses")
@RequiredArgsConstructor
@Slf4j
public class RvDeclineAnalysisController {

    private final RvDeclineAnalysisService declineService;

    /**
     * Create a new Decline Analysis.
     */
    @PostMapping
    public ResponseEntity<RvDeclineAnalysisDto> createDeclineAnalysis(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RvDeclineAnalysisDto dto) {
        log.info("POST /api/nexus/rv/decline-analyses - Creating analysis: {}", dto.getName());
        RvDeclineAnalysisDto created = declineService.createDeclineAnalysis(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a Decline Analysis by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RvDeclineAnalysisDto> getDeclineAnalysisById(@PathVariable UUID id) {
        log.debug("GET /api/nexus/rv/decline-analyses/{}", id);
        return declineService.getDeclineAnalysisById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RvEntityNotFoundException("Decline Analysis", id));
    }

    /**
     * Get all Decline Analyses for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<RvDeclineAnalysisDto>> getAllDeclineAnalyses(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/nexus/rv/decline-analyses - tenantId={}", tenantId);
        Page<RvDeclineAnalysisDto> analyses = declineService.getAllDeclineAnalyses(tenantId, page, size);
        return ResponseEntity.ok(analyses);
    }

    /**
     * Get Decline Analyses by Well.
     */
    @GetMapping("/by-well/{wellId}")
    public ResponseEntity<List<RvDeclineAnalysisDto>> getDeclineAnalysesByWell(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        log.debug("GET /api/nexus/rv/decline-analyses/by-well/{}", wellId);
        List<RvDeclineAnalysisDto> analyses = declineService.getDeclineAnalysesByWell(tenantId, wellId);
        return ResponseEntity.ok(analyses);
    }

    /**
     * Perform decline curve analysis with Arps parameters.
     */
    @PostMapping("/{id}/perform-analysis")
    public ResponseEntity<RvDeclineAnalysisDto> performAnalysis(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> params) {
        log.info("POST /api/nexus/rv/decline-analyses/{}/perform-analysis", id);

        BigDecimal qi = new BigDecimal(params.get("qi").toString());
        BigDecimal di = new BigDecimal(params.get("di").toString());
        BigDecimal b = new BigDecimal(params.get("b").toString());
        BigDecimal economicLimit = params.containsKey("economicLimit") ?
            new BigDecimal(params.get("economicLimit").toString()) : BigDecimal.valueOf(10);
        int forecastYears = params.containsKey("forecastYears") ?
            ((Number) params.get("forecastYears")).intValue() : 20;

        RvDeclineAnalysisDto result = declineService.performAnalysis(id, qi, di, b, economicLimit, forecastYears);
        return ResponseEntity.ok(result);
    }

    /**
     * Generate production forecast.
     */
    @GetMapping("/{id}/forecast")
    public ResponseEntity<List<Map<String, Object>>> generateForecast(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int forecastYears,
            @RequestParam(defaultValue = "6") int monthlyIntervals) {
        log.info("GET /api/nexus/rv/decline-analyses/{}/forecast", id);
        List<Map<String, Object>> forecast = declineService.generateForecast(id, forecastYears, monthlyIntervals);
        return ResponseEntity.ok(forecast);
    }

    /**
     * Update a Decline Analysis.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RvDeclineAnalysisDto> updateDeclineAnalysis(
            @PathVariable UUID id,
            @Valid @RequestBody RvDeclineAnalysisDto dto) {
        log.info("PUT /api/nexus/rv/decline-analyses/{}", id);
        dto.setAssetId(id);
        RvDeclineAnalysisDto updated = declineService.updateDeclineAnalysis(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a Decline Analysis.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeclineAnalysis(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /api/nexus/rv/decline-analyses/{}", id);
        declineService.deleteDeclineAnalysis(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RvEntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RvEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
