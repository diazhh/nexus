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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvMaterialBalanceDto;
import org.thingsboard.nexus.rv.service.RvMaterialBalanceService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Material Balance Analysis.
 * Implements the Havlena-Odeh method for OOIP estimation and drive mechanism analysis.
 */
@RestController
@RequestMapping("/api/nexus/rv/material-balance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RV Material Balance", description = "Material Balance Analysis (Havlena-Odeh Method)")
public class RvMaterialBalanceController {

    private final RvMaterialBalanceService materialBalanceService;

    @GetMapping
    @Operation(summary = "Get all material balance studies")
    public ResponseEntity<Page<RvMaterialBalanceDto>> getAllMaterialBalanceStudies(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /material-balance - tenantId={}, page={}, size={}", tenantId, page, size);
        return ResponseEntity.ok(materialBalanceService.getAllMaterialBalanceStudies(tenantId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get material balance study by ID")
    public ResponseEntity<RvMaterialBalanceDto> getMaterialBalanceStudyById(@PathVariable UUID id) {
        log.debug("GET /material-balance/{}", id);
        return materialBalanceService.getMaterialBalanceStudyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-reservoir/{reservoirId}")
    @Operation(summary = "Get material balance studies by reservoir")
    public ResponseEntity<List<RvMaterialBalanceDto>> getMaterialBalanceStudiesByReservoir(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID reservoirId) {
        log.debug("GET /material-balance/by-reservoir/{}", reservoirId);
        return ResponseEntity.ok(materialBalanceService.getMaterialBalanceStudiesByReservoir(tenantId, reservoirId));
    }

    @PostMapping
    @Operation(summary = "Create a new material balance study")
    public ResponseEntity<RvMaterialBalanceDto> createMaterialBalanceStudy(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody RvMaterialBalanceDto dto) {
        log.info("POST /material-balance - name={}, reservoir={}", dto.getName(), dto.getReservoirId());
        dto.setTenantId(tenantId);
        RvMaterialBalanceDto created = materialBalanceService.createMaterialBalanceStudy(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a material balance study")
    public ResponseEntity<RvMaterialBalanceDto> updateMaterialBalanceStudy(
            @PathVariable UUID id,
            @RequestBody RvMaterialBalanceDto dto) {
        log.info("PUT /material-balance/{}", id);
        dto.setId(id);
        return ResponseEntity.ok(materialBalanceService.updateMaterialBalanceStudy(dto));
    }

    @PostMapping("/{id}/calculate-mbe-terms")
    @Operation(summary = "Calculate MBE terms (F, Eo, Eg, Efw) for all data points",
               description = "Calculates the Material Balance Equation terms for each pressure/production data point")
    public ResponseEntity<RvMaterialBalanceDto> calculateMBETerms(
            @PathVariable UUID id,
            @RequestBody RvMaterialBalanceDto dto) {
        log.info("POST /material-balance/{}/calculate-mbe-terms", id);
        dto.setId(id);
        RvMaterialBalanceDto result = materialBalanceService.calculateMBETerms(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/havlena-odeh-analysis")
    @Operation(summary = "Perform Havlena-Odeh analysis",
               description = "Performs complete Havlena-Odeh analysis to determine OOIP and drive mechanisms. " +
                             "Returns calculated OOIP, regression parameters, and drive mechanism indices.")
    public ResponseEntity<RvMaterialBalanceDto> performHavlenaOdehAnalysis(
            @PathVariable UUID id,
            @RequestBody RvMaterialBalanceDto dto) {
        log.info("POST /material-balance/{}/havlena-odeh-analysis", id);
        dto.setId(id);
        RvMaterialBalanceDto result = materialBalanceService.performHavlenaOdehAnalysis(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/drive-mechanisms")
    @Operation(summary = "Get drive mechanism analysis",
               description = "Returns the drive mechanism indices: DDI (Depletion), SDI (Gas Cap), WDI (Water), CDI (Compaction)")
    public ResponseEntity<Map<String, Object>> getDriveMechanisms(
            @PathVariable UUID id) {
        log.debug("GET /material-balance/{}/drive-mechanisms", id);
        return materialBalanceService.getMaterialBalanceStudyById(id)
                .map(study -> {
                    Map<String, Object> mechanisms = Map.of(
                            "primaryMechanism", study.getPrimaryDriveMechanism() != null ? study.getPrimaryDriveMechanism() : "UNKNOWN",
                            "depletionDriveIndex", study.getSolutionGasDriveIndex() != null ? study.getSolutionGasDriveIndex() : 0,
                            "gasCapDriveIndex", study.getGasCapDriveIndex() != null ? study.getGasCapDriveIndex() : 0,
                            "waterDriveIndex", study.getWaterDriveIndex() != null ? study.getWaterDriveIndex() : 0,
                            "compactionDriveIndex", study.getCompactionDriveIndex() != null ? study.getCompactionDriveIndex() : 0
                    );
                    return ResponseEntity.ok(mechanisms);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/havlena-odeh-plot")
    @Operation(summary = "Get Havlena-Odeh plot data",
               description = "Returns X and Y axis data for plotting the Havlena-Odeh straight line")
    public ResponseEntity<Map<String, Object>> getHavlenaOdehPlotData(
            @PathVariable UUID id) {
        log.debug("GET /material-balance/{}/havlena-odeh-plot", id);
        return materialBalanceService.getMaterialBalanceStudyById(id)
                .map(study -> {
                    Map<String, Object> plotData = Map.of(
                            "plotType", study.getPlotType() != null ? study.getPlotType() : "F_vs_Eo",
                            "dataPoints", study.getDataPoints() != null ? study.getDataPoints() : List.of(),
                            "regressionSlope", study.getRegressionSlope() != null ? study.getRegressionSlope() : 0,
                            "regressionIntercept", study.getRegressionIntercept() != null ? study.getRegressionIntercept() : 0,
                            "r2", study.getRegressionR2() != null ? study.getRegressionR2() : 0,
                            "calculatedOOIP", study.getCalculatedOOIP() != null ? study.getCalculatedOOIP() : 0
                    );
                    return ResponseEntity.ok(plotData);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/add-data-point")
    @Operation(summary = "Add a production/pressure data point",
               description = "Adds a new data point to the material balance study for analysis")
    public ResponseEntity<RvMaterialBalanceDto> addDataPoint(
            @PathVariable UUID id,
            @RequestBody RvMaterialBalanceDto.MaterialBalanceDataPoint dataPoint) {
        log.info("POST /material-balance/{}/add-data-point - pressure={}", id, dataPoint.getPressure());
        return materialBalanceService.getMaterialBalanceStudyById(id)
                .map(study -> {
                    if (study.getDataPoints() == null) {
                        study.setDataPoints(new java.util.ArrayList<>());
                    }
                    study.getDataPoints().add(dataPoint);
                    RvMaterialBalanceDto updated = materialBalanceService.updateMaterialBalanceStudy(study);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a material balance study")
    public ResponseEntity<Void> deleteMaterialBalanceStudy(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /material-balance/{}", id);
        materialBalanceService.deleteMaterialBalanceStudy(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
