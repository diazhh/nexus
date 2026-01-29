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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.exception.RvBusinessException;
import org.thingsboard.nexus.rv.service.RvCalculationService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST Controller for petroleum engineering calculations.
 * Base path: /api/nexus/rv/calculations
 */
@RestController
@RequestMapping("/api/nexus/rv/calculations")
@RequiredArgsConstructor
@Slf4j
public class RvCalculationController {

    private final RvCalculationService calculationService;

    /**
     * Calculate OOIP (Original Oil In Place) using volumetric method.
     */
    @PostMapping("/ooip")
    public ResponseEntity<Map<String, Object>> calculateOOIP(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/ooip");

        BigDecimal ooip = calculationService.calculateOOIP(
            params.get("areaAcres"),
            params.get("thicknessM"),
            params.get("porosity"),
            params.get("waterSaturation"),
            params.get("bo")
        );

        return ResponseEntity.ok(Map.of(
            "ooip_stb", ooip,
            "ooip_mmbbl", ooip.divide(BigDecimal.valueOf(1_000_000), 4, java.math.RoundingMode.HALF_UP)
        ));
    }

    /**
     * Calculate Water Saturation using Archie equation.
     */
    @PostMapping("/sw-archie")
    public ResponseEntity<Map<String, Object>> calculateSwArchie(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/sw-archie");

        BigDecimal sw = calculationService.calculateSwArchie(
            params.get("porosity"),
            params.get("rw"),
            params.get("rt"),
            params.get("a"),
            params.get("m"),
            params.get("n")
        );

        return ResponseEntity.ok(Map.of(
            "sw_frac", sw,
            "sw_percent", sw.multiply(BigDecimal.valueOf(100))
        ));
    }

    /**
     * Calculate Shale Volume using Larionov equation.
     */
    @PostMapping("/vsh-larionov")
    public ResponseEntity<Map<String, Object>> calculateVshLarionov(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/vsh-larionov");

        BigDecimal vsh = calculationService.calculateVshLarionov(
            params.get("grLog"),
            params.get("grClean"),
            params.get("grShale")
        );

        return ResponseEntity.ok(Map.of(
            "vsh_frac", vsh,
            "vsh_percent", vsh.multiply(BigDecimal.valueOf(100))
        ));
    }

    /**
     * Calculate Bubble Point using Standing correlation.
     */
    @PostMapping("/pb-standing")
    public ResponseEntity<Map<String, Object>> calculatePbStanding(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/pb-standing");

        BigDecimal pb = calculationService.calculatePbStanding(
            params.get("rs"),
            params.get("gasGravity"),
            params.get("temperature"),
            params.get("apiGravity")
        );

        return ResponseEntity.ok(Map.of(
            "pb_psia", pb,
            "pb_bara", pb.multiply(BigDecimal.valueOf(0.0689476))
        ));
    }

    /**
     * Calculate Oil Formation Volume Factor using Standing correlation.
     */
    @PostMapping("/bo-standing")
    public ResponseEntity<Map<String, Object>> calculateBoStanding(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/bo-standing");

        BigDecimal bo = calculationService.calculateBoStanding(
            params.get("rs"),
            params.get("gasGravity"),
            params.get("oilGravity"),
            params.get("temperature")
        );

        return ResponseEntity.ok(Map.of("bo_rb_stb", bo));
    }

    /**
     * Calculate Dead Oil Viscosity using Beggs-Robinson correlation.
     */
    @PostMapping("/viscosity-beggs-robinson")
    public ResponseEntity<Map<String, Object>> calculateViscosityBeggsRobinson(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/viscosity-beggs-robinson");

        BigDecimal viscosity = calculationService.calculateViscosityBeggsRobinson(
            params.get("apiGravity"),
            params.get("temperature")
        );

        return ResponseEntity.ok(Map.of("viscosity_cp", viscosity));
    }

    /**
     * Calculate IPR using Vogel equation.
     */
    @PostMapping("/ipr-vogel")
    public ResponseEntity<Map<String, Object>> calculateIprVogel(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/ipr-vogel");

        BigDecimal rate = calculationService.calculateIprVogel(
            params.get("qmax"),
            params.get("reservoirPressure"),
            params.get("flowingPressure")
        );

        return ResponseEntity.ok(Map.of("rate_bopd", rate));
    }

    /**
     * Calculate Productivity Index from test data.
     */
    @PostMapping("/productivity-index")
    public ResponseEntity<Map<String, Object>> calculateProductivityIndex(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/productivity-index");

        BigDecimal pi = calculationService.calculateProductivityIndex(
            params.get("testRate"),
            params.get("reservoirPressure"),
            params.get("flowingPressure")
        );

        return ResponseEntity.ok(Map.of("pi_bpd_psi", pi));
    }

    /**
     * Calculate Arps decline rate at time t.
     */
    @PostMapping("/arps-decline")
    public ResponseEntity<Map<String, Object>> calculateArpsDecline(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/arps-decline");

        BigDecimal rate = calculationService.calculateArpsDecline(
            params.get("qi"),
            params.get("di"),
            params.get("b"),
            params.get("time")
        );

        return ResponseEntity.ok(Map.of("rate_bpd", rate));
    }

    /**
     * Calculate Arps cumulative production.
     */
    @PostMapping("/arps-cumulative")
    public ResponseEntity<Map<String, Object>> calculateArpsCumulative(@RequestBody Map<String, BigDecimal> params) {
        log.info("POST /api/nexus/rv/calculations/arps-cumulative");

        BigDecimal cumulative = calculationService.calculateArpsCumulative(
            params.get("qi"),
            params.get("di"),
            params.get("b"),
            params.get("time")
        );

        return ResponseEntity.ok(Map.of("cumulative_bbl", cumulative));
    }

    /**
     * Exception handler for business errors.
     */
    @ExceptionHandler(RvBusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(RvBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", ex.getMessage(),
                    "code", ex.getErrorCode()
                ));
    }
}
