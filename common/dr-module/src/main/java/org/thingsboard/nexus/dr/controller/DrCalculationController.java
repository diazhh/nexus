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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.dr.dto.calculation.*;
import org.thingsboard.nexus.dr.service.DrCalculationService;

import jakarta.validation.Valid;

/**
 * REST Controller for drilling engineering calculations.
 */
@RestController
@RequestMapping("/api/nexus/dr/calculations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DR Calculations", description = "Drilling Engineering Calculations API")
public class DrCalculationController {

    private final DrCalculationService calculationService;

    // --- MSE Calculation ---

    @PostMapping("/mse")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Mechanical Specific Energy",
            description = "Calculates MSE from drilling parameters. MSE represents energy required to remove a unit volume of rock.")
    public ResponseEntity<MseCalculationResult> calculateMse(
            @Valid @RequestBody MseCalculationRequest request) {
        log.debug("Calculating MSE: {}", request);
        MseCalculationResult result = calculationService.calculateMse(request);
        return ResponseEntity.ok(result);
    }

    // --- ECD Calculation ---

    @PostMapping("/ecd")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Equivalent Circulating Density",
            description = "Calculates ECD accounting for annular pressure losses while circulating.")
    public ResponseEntity<EcdCalculationResult> calculateEcd(
            @Valid @RequestBody EcdCalculationRequest request) {
        log.debug("Calculating ECD: {}", request);
        EcdCalculationResult result = calculationService.calculateEcd(request);
        return ResponseEntity.ok(result);
    }

    // --- Swab/Surge Calculation ---

    @PostMapping("/swab-surge")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Swab and Surge Pressures",
            description = "Calculates pressure changes due to pipe movement (tripping in/out).")
    public ResponseEntity<SwabSurgeCalculationResult> calculateSwabSurge(
            @Valid @RequestBody SwabSurgeCalculationRequest request) {
        log.debug("Calculating Swab/Surge: {}", request);
        SwabSurgeCalculationResult result = calculationService.calculateSwabSurge(request);
        return ResponseEntity.ok(result);
    }

    // --- Kick Tolerance Calculation ---

    @PostMapping("/kick-tolerance")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Kick Tolerance",
            description = "Calculates maximum influx volume that can be safely circulated out without fracturing the formation.")
    public ResponseEntity<KickToleranceCalculationResult> calculateKickTolerance(
            @Valid @RequestBody KickToleranceCalculationRequest request) {
        log.debug("Calculating Kick Tolerance: {}", request);
        KickToleranceCalculationResult result = calculationService.calculateKickTolerance(request);
        return ResponseEntity.ok(result);
    }

    // --- Torque & Drag Calculation ---

    @PostMapping("/torque-drag")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Torque and Drag",
            description = "Calculates torque and drag forces for directional wells using friction factor model.")
    public ResponseEntity<TorqueDragCalculationResult> calculateTorqueDrag(
            @Valid @RequestBody TorqueDragCalculationRequest request) {
        log.debug("Calculating Torque & Drag: {}", request);
        TorqueDragCalculationResult result = calculationService.calculateTorqueDrag(request);
        return ResponseEntity.ok(result);
    }

    // --- DLS Calculation ---

    @PostMapping("/dls")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Dog Leg Severity",
            description = "Calculates dogleg severity between two survey stations.")
    public ResponseEntity<DlsCalculationResult> calculateDls(
            @Valid @RequestBody DlsCalculationRequest request) {
        log.debug("Calculating DLS: {}", request);
        DlsCalculationResult result = calculationService.calculateDls(request);
        return ResponseEntity.ok(result);
    }

    // --- Bit Hydraulics Calculation ---

    @PostMapping("/bit-hydraulics")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Calculate Bit Hydraulics",
            description = "Calculates bit hydraulics including jet velocity, pressure drop, and hydraulic horsepower.")
    public ResponseEntity<BitHydraulicsCalculationResult> calculateBitHydraulics(
            @Valid @RequestBody BitHydraulicsCalculationRequest request) {
        log.debug("Calculating Bit Hydraulics: {}", request);
        BitHydraulicsCalculationResult result = calculationService.calculateBitHydraulics(request);
        return ResponseEntity.ok(result);
    }

    // --- Batch Calculations ---

    @PostMapping("/batch/mse")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Batch MSE Calculation",
            description = "Calculates MSE for multiple data points.")
    public ResponseEntity<java.util.List<MseCalculationResult>> batchCalculateMse(
            @Valid @RequestBody java.util.List<MseCalculationRequest> requests) {
        log.debug("Batch calculating MSE for {} points", requests.size());
        java.util.List<MseCalculationResult> results = requests.stream()
                .map(calculationService::calculateMse)
                .toList();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/batch/ecd")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Batch ECD Calculation",
            description = "Calculates ECD for multiple data points.")
    public ResponseEntity<java.util.List<EcdCalculationResult>> batchCalculateEcd(
            @Valid @RequestBody java.util.List<EcdCalculationRequest> requests) {
        log.debug("Batch calculating ECD for {} points", requests.size());
        java.util.List<EcdCalculationResult> results = requests.stream()
                .map(calculationService::calculateEcd)
                .toList();
        return ResponseEntity.ok(results);
    }

    // --- Utility Endpoints ---

    @GetMapping("/formulas")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Available Formulas",
            description = "Returns list of available drilling calculations with their descriptions.")
    public ResponseEntity<java.util.Map<String, String>> getAvailableFormulas() {
        java.util.Map<String, String> formulas = new java.util.LinkedHashMap<>();
        formulas.put("MSE", "Mechanical Specific Energy - Energy to remove unit volume of rock");
        formulas.put("ECD", "Equivalent Circulating Density - Effective mud weight while circulating");
        formulas.put("SwabSurge", "Swab/Surge Pressure - Pressure change due to pipe movement");
        formulas.put("KickTolerance", "Kick Tolerance - Maximum influx volume for safe circulation");
        formulas.put("TorqueDrag", "Torque & Drag - Forces in directional wells");
        formulas.put("DLS", "Dog Leg Severity - Rate of wellbore curvature");
        formulas.put("BitHydraulics", "Bit Hydraulics - Jet velocity, pressure drop, HHP at bit");
        return ResponseEntity.ok(formulas);
    }

    @GetMapping("/units")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @Operation(summary = "Get Units Reference",
            description = "Returns reference of units used in calculations.")
    public ResponseEntity<java.util.Map<String, java.util.Map<String, String>>> getUnitsReference() {
        java.util.Map<String, java.util.Map<String, String>> units = new java.util.LinkedHashMap<>();

        java.util.Map<String, String> pressure = new java.util.LinkedHashMap<>();
        pressure.put("psi", "Pounds per square inch");
        pressure.put("ppg", "Pounds per gallon (mud weight)");
        units.put("pressure", pressure);

        java.util.Map<String, String> depth = new java.util.LinkedHashMap<>();
        depth.put("ft", "Feet");
        depth.put("m", "Meters");
        units.put("depth", depth);

        java.util.Map<String, String> flow = new java.util.LinkedHashMap<>();
        flow.put("gpm", "Gallons per minute");
        flow.put("bpm", "Barrels per minute");
        units.put("flow", flow);

        java.util.Map<String, String> torque = new java.util.LinkedHashMap<>();
        torque.put("ft-lbs", "Foot-pounds");
        units.put("torque", torque);

        java.util.Map<String, String> rop = new java.util.LinkedHashMap<>();
        rop.put("ft/hr", "Feet per hour");
        units.put("rop", rop);

        java.util.Map<String, String> angle = new java.util.LinkedHashMap<>();
        angle.put("deg", "Degrees");
        angle.put("deg/100ft", "Degrees per 100 feet");
        units.put("angle", angle);

        return ResponseEntity.ok(units);
    }
}
