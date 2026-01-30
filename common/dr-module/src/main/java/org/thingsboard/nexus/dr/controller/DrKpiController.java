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
package org.thingsboard.nexus.dr.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.dr.dto.kpi.*;
import org.thingsboard.nexus.dr.service.DrKpiService;

import java.util.UUID;

/**
 * REST Controller for drilling KPIs.
 */
@RestController
@RequestMapping("/api/nexus/dr/kpis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DR KPIs", description = "Drilling Key Performance Indicators API")
public class DrKpiController {

    private final DrKpiService kpiService;

    // --- Run KPIs ---

    @GetMapping("/runs/{runId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Run KPIs",
            description = "Retrieves comprehensive KPIs for a specific drilling run.")
    public ResponseEntity<RunKpiDto> getRunKpis(
            @Parameter(description = "Run ID") @PathVariable UUID runId) {
        log.debug("Getting KPIs for run: {}", runId);
        RunKpiDto kpis = kpiService.getRunKpis(runId);
        return ResponseEntity.ok(kpis);
    }

    // --- Rig KPIs ---

    @GetMapping("/rigs/{rigId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Rig KPIs",
            description = "Retrieves comprehensive KPIs for a specific rig.")
    public ResponseEntity<RigKpiDto> getRigKpis(
            @Parameter(description = "Rig ID") @PathVariable UUID rigId,
            @Parameter(description = "Period start time (epoch ms)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "Period end time (epoch ms)") @RequestParam(required = false) Long endTime) {
        log.debug("Getting KPIs for rig: {}, period: {} - {}", rigId, startTime, endTime);
        RigKpiDto kpis = kpiService.getRigKpis(rigId);
        return ResponseEntity.ok(kpis);
    }

    // --- Well KPIs ---

    @GetMapping("/wells/{wellId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Well KPIs",
            description = "Retrieves comprehensive KPIs for a specific well.")
    public ResponseEntity<WellKpiDto> getWellKpis(
            @Parameter(description = "Well ID") @PathVariable UUID wellId) {
        log.debug("Getting KPIs for well: {}", wellId);
        WellKpiDto kpis = kpiService.getWellKpis(wellId);
        return ResponseEntity.ok(kpis);
    }

    // --- Connection Time KPIs ---

    @GetMapping("/runs/{runId}/connection-time")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Run Connection Time KPIs",
            description = "Retrieves connection time analysis KPIs for a run.")
    public ResponseEntity<ConnectionTimeKpiDto> getRunConnectionTimeKpis(
            @Parameter(description = "Run ID") @PathVariable UUID runId) {
        log.debug("Getting connection time KPIs for run: {}", runId);
        ConnectionTimeKpiDto kpis = kpiService.getConnectionTimeKpis(runId);
        return ResponseEntity.ok(kpis);
    }

    // --- Drilling Efficiency KPIs ---

    @GetMapping("/runs/{runId}/efficiency")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Run Drilling Efficiency KPIs",
            description = "Retrieves drilling efficiency analysis KPIs for a run.")
    public ResponseEntity<DrillingEfficiencyKpiDto> getRunDrillingEfficiencyKpis(
            @Parameter(description = "Run ID") @PathVariable UUID runId) {
        log.debug("Getting drilling efficiency KPIs for run: {}", runId);
        DrillingEfficiencyKpiDto kpis = kpiService.getDrillingEfficiencyKpis(runId);
        return ResponseEntity.ok(kpis);
    }

    // --- Summary Endpoints ---

    @GetMapping("/summary/runs/{runId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Run KPI Summary",
            description = "Retrieves a quick summary of key KPIs for a run.")
    public ResponseEntity<java.util.Map<String, Object>> getRunKpiSummary(
            @Parameter(description = "Run ID") @PathVariable UUID runId) {
        log.debug("Getting KPI summary for run: {}", runId);
        RunKpiDto kpis = kpiService.getRunKpis(runId);

        java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("runId", kpis.getRunId());
        summary.put("footageDrilledFt", kpis.getFootageDrilledFt());
        summary.put("totalHours", kpis.getTotalHours());
        summary.put("avgRopFtHr", kpis.getAvgRopFtHr());
        summary.put("drillingEfficiencyPercent", kpis.getDrillingEfficiencyPercent());

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/summary/wells/{wellId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Well KPI Summary",
            description = "Retrieves a quick summary of key KPIs for a well.")
    public ResponseEntity<java.util.Map<String, Object>> getWellKpiSummary(
            @Parameter(description = "Well ID") @PathVariable UUID wellId) {
        log.debug("Getting KPI summary for well: {}", wellId);
        WellKpiDto kpis = kpiService.getWellKpis(wellId);

        java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("wellId", kpis.getWellId());
        summary.put("totalRuns", kpis.getTotalRuns());
        summary.put("totalFootageDrilledFt", kpis.getTotalFootageDrilledFt());
        summary.put("totalDepthFt", kpis.getTotalDepthFt());
        summary.put("totalDrillingDays", kpis.getTotalDrillingDays());
        summary.put("overallAvgRopFtHr", kpis.getOverallAvgRopFtHr());

        return ResponseEntity.ok(summary);
    }

    // --- Comparison Endpoints ---

    @GetMapping("/compare/runs")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Compare Run KPIs",
            description = "Compares KPIs between multiple runs.")
    public ResponseEntity<java.util.List<RunKpiDto>> compareRunKpis(
            @Parameter(description = "Run IDs to compare") @RequestParam java.util.List<UUID> runIds) {
        log.debug("Comparing KPIs for runs: {}", runIds);
        java.util.List<RunKpiDto> kpis = runIds.stream()
                .map(kpiService::getRunKpis)
                .toList();
        return ResponseEntity.ok(kpis);
    }

    @GetMapping("/compare/wells")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Compare Well KPIs",
            description = "Compares KPIs between multiple wells (offset well comparison).")
    public ResponseEntity<java.util.List<WellKpiDto>> compareWellKpis(
            @Parameter(description = "Well IDs to compare") @RequestParam java.util.List<UUID> wellIds) {
        log.debug("Comparing KPIs for wells: {}", wellIds);
        java.util.List<WellKpiDto> kpis = wellIds.stream()
                .map(kpiService::getWellKpis)
                .toList();
        return ResponseEntity.ok(kpis);
    }

    // --- KPI Definitions ---

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get KPI Definitions",
            description = "Returns definitions and descriptions of available KPIs.")
    public ResponseEntity<java.util.Map<String, java.util.Map<String, String>>> getKpiDefinitions() {
        java.util.Map<String, java.util.Map<String, String>> definitions = new java.util.LinkedHashMap<>();

        java.util.Map<String, String> performance = new java.util.LinkedHashMap<>();
        performance.put("ROP", "Rate of Penetration - Speed of drilling (ft/hr)");
        performance.put("MSE", "Mechanical Specific Energy - Drilling efficiency indicator (psi)");
        performance.put("ConnectionTime", "Time to make a connection (minutes)");
        performance.put("TripSpeed", "Speed of pipe movement during trips (ft/hr)");
        definitions.put("performance", performance);

        java.util.Map<String, String> efficiency = new java.util.LinkedHashMap<>();
        efficiency.put("DrillingEfficiency", "Percentage of productive drilling time");
        efficiency.put("NPT", "Non-Productive Time percentage");
        efficiency.put("ILT", "Invisible Lost Time - Hidden inefficiencies");
        efficiency.put("ProductiveTime", "Percentage of time adding value");
        definitions.put("efficiency", efficiency);

        java.util.Map<String, String> cost = new java.util.LinkedHashMap<>();
        cost.put("CostPerFoot", "Total cost divided by footage ($/ft)");
        cost.put("CostPerDay", "Daily operational cost ($/day)");
        cost.put("CostVsAFE", "Actual cost vs approved budget percentage");
        definitions.put("cost", cost);

        java.util.Map<String, String> directional = new java.util.LinkedHashMap<>();
        directional.put("DLS", "Dog Leg Severity - Rate of wellbore curvature (deg/100ft)");
        directional.put("BuildRate", "Rate of inclination change (deg/100ft)");
        directional.put("TurnRate", "Rate of azimuth change (deg/100ft)");
        directional.put("SlidingPercent", "Percentage of footage drilled while sliding");
        definitions.put("directional", directional);

        return ResponseEntity.ok(definitions);
    }
}
