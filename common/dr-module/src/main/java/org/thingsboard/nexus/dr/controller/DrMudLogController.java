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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.dr.dto.DrMudLogDto;
import org.thingsboard.nexus.dr.model.DrMudLog;
import org.thingsboard.nexus.dr.model.enums.LithologyType;
import org.thingsboard.nexus.dr.service.DrMudLogService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Mud Log operations
 */
@RestController
@RequestMapping("/api/nexus/dr/mudlogs")
@RequiredArgsConstructor
@Slf4j
public class DrMudLogController {

    private final DrMudLogService mudLogService;

    @GetMapping("/{id}")
    public ResponseEntity<DrMudLogDto> getMudLogById(@PathVariable UUID id) {
        log.debug("REST request to get Mud Log: {}", id);
        DrMudLogDto mudLog = mudLogService.getById(id);
        return ResponseEntity.ok(mudLog);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<DrMudLogDto>> getMudLogsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "mdFt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Mud Logs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrMudLogDto> mudLogs = mudLogService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(mudLogs);
    }

    @GetMapping("/run/{runId}")
    public ResponseEntity<Page<DrMudLogDto>> getMudLogsByRun(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "mdFt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Mud Logs for run: {}", runId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrMudLogDto> mudLogs = mudLogService.getByRun(runId, pageable);
        return ResponseEntity.ok(mudLogs);
    }

    @GetMapping("/run/{runId}/ordered")
    public ResponseEntity<List<DrMudLogDto>> getMudLogsByRunOrdered(@PathVariable UUID runId) {
        log.debug("REST request to get ordered Mud Logs for run: {}", runId);
        List<DrMudLogDto> mudLogs = mudLogService.getMudLogsByRunOrdered(runId);
        return ResponseEntity.ok(mudLogs);
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<Page<DrMudLogDto>> getMudLogsByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) UUID runId,
            @RequestParam(required = false) UUID wellId,
            @RequestParam(required = false) LithologyType lithology,
            @RequestParam(required = false) String formationName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "mdFt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Mud Logs with filters");

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrMudLogDto> mudLogs = mudLogService.getByFilters(
                tenantId, runId, wellId, lithology, formationName, pageable);
        return ResponseEntity.ok(mudLogs);
    }

    @PostMapping
    public ResponseEntity<DrMudLogDto> createMudLog(@Valid @RequestBody DrMudLog mudLog) {
        log.info("REST request to create Mud Log at MD {} ft", mudLog.getMdFt());
        DrMudLogDto createdMudLog = mudLogService.create(mudLog);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMudLog);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DrMudLogDto> updateMudLog(
            @PathVariable UUID id,
            @Valid @RequestBody DrMudLog mudLog) {

        log.info("REST request to update Mud Log: {}", id);
        DrMudLogDto updatedMudLog = mudLogService.update(id, mudLog);
        return ResponseEntity.ok(updatedMudLog);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMudLog(@PathVariable UUID id) {
        log.info("REST request to delete Mud Log: {}", id);
        mudLogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Lithology Endpoints ---

    @GetMapping("/run/{runId}/lithology/{lithology}")
    public ResponseEntity<List<DrMudLogDto>> getMudLogsByLithology(
            @PathVariable UUID runId,
            @PathVariable LithologyType lithology) {

        log.debug("REST request to get Mud Logs by lithology: {}", lithology);
        List<DrMudLogDto> mudLogs = mudLogService.getByLithology(runId, lithology);
        return ResponseEntity.ok(mudLogs);
    }

    @GetMapping("/run/{runId}/lithologies")
    public ResponseEntity<List<LithologyType>> getDistinctLithologies(@PathVariable UUID runId) {
        log.debug("REST request to get distinct lithologies for run: {}", runId);
        List<LithologyType> lithologies = mudLogService.getDistinctLithologies(runId);
        return ResponseEntity.ok(lithologies);
    }

    @GetMapping("/run/{runId}/lithology-distribution")
    public ResponseEntity<Map<LithologyType, Long>> getLithologyDistribution(@PathVariable UUID runId) {
        log.debug("REST request to get lithology distribution for run: {}", runId);
        Map<LithologyType, Long> distribution = mudLogService.getLithologyDistribution(runId);
        return ResponseEntity.ok(distribution);
    }

    // --- Formation Endpoints ---

    @GetMapping("/run/{runId}/formation/{formationName}")
    public ResponseEntity<List<DrMudLogDto>> getMudLogsByFormation(
            @PathVariable UUID runId,
            @PathVariable String formationName) {

        log.debug("REST request to get Mud Logs by formation: {}", formationName);
        List<DrMudLogDto> mudLogs = mudLogService.getByFormation(runId, formationName);
        return ResponseEntity.ok(mudLogs);
    }

    @GetMapping("/run/{runId}/formations")
    public ResponseEntity<List<String>> getDistinctFormations(@PathVariable UUID runId) {
        log.debug("REST request to get distinct formations for run: {}", runId);
        List<String> formations = mudLogService.getDistinctFormations(runId);
        return ResponseEntity.ok(formations);
    }

    @GetMapping("/run/{runId}/formation-tops")
    public ResponseEntity<List<DrMudLogDto>> getFormationTops(@PathVariable UUID runId) {
        log.debug("REST request to get formation tops for run: {}", runId);
        List<DrMudLogDto> tops = mudLogService.getFormationTops(runId);
        return ResponseEntity.ok(tops);
    }

    @PostMapping("/{id}/formation-top")
    public ResponseEntity<DrMudLogDto> recordFormationTop(
            @PathVariable UUID id,
            @RequestParam String formationName,
            @RequestParam BigDecimal topDepth) {

        log.info("REST request to record formation top for Mud Log {}", id);
        DrMudLogDto updatedMudLog = mudLogService.recordFormationTop(id, formationName, topDepth);
        return ResponseEntity.ok(updatedMudLog);
    }

    // --- Gas Analysis Endpoints ---

    @GetMapping("/run/{runId}/gas-shows")
    public ResponseEntity<List<DrMudLogDto>> getGasShows(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "100") BigDecimal threshold) {

        log.debug("REST request to get gas shows for run: {}", runId);
        List<DrMudLogDto> shows = mudLogService.getGasShows(runId, threshold);
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/run/{runId}/max-gas")
    public ResponseEntity<BigDecimal> getMaxTotalGas(@PathVariable UUID runId) {
        log.debug("REST request to get max total gas for run: {}", runId);
        BigDecimal maxGas = mudLogService.getMaxTotalGas(runId);
        return ResponseEntity.ok(maxGas);
    }

    @GetMapping("/run/{runId}/avg-background-gas")
    public ResponseEntity<BigDecimal> getAverageBackgroundGas(@PathVariable UUID runId) {
        log.debug("REST request to get average background gas for run: {}", runId);
        BigDecimal avgGas = mudLogService.getAverageBackgroundGas(runId);
        return ResponseEntity.ok(avgGas);
    }

    @GetMapping("/run/{runId}/connection-gas")
    public ResponseEntity<List<DrMudLogDto>> getConnectionGasEvents(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "50") BigDecimal threshold) {

        log.debug("REST request to get connection gas events for run: {}", runId);
        List<DrMudLogDto> events = mudLogService.getConnectionGasEvents(runId, threshold);
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{id}/gas-readings")
    public ResponseEntity<DrMudLogDto> updateGasReadings(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal totalGas,
            @RequestParam(required = false) BigDecimal backgroundGas,
            @RequestParam(required = false) BigDecimal c1,
            @RequestParam(required = false) BigDecimal c2,
            @RequestParam(required = false) BigDecimal c3,
            @RequestParam(required = false) BigDecimal ic4,
            @RequestParam(required = false) BigDecimal nc4,
            @RequestParam(required = false) BigDecimal ic5,
            @RequestParam(required = false) BigDecimal nc5) {

        log.info("REST request to update gas readings for Mud Log {}", id);
        DrMudLogDto updatedMudLog = mudLogService.updateGasReadings(
                id, totalGas, backgroundGas, c1, c2, c3, ic4, nc4, ic5, nc5);
        return ResponseEntity.ok(updatedMudLog);
    }

    // --- Hydrocarbon Show Endpoints ---

    @GetMapping("/run/{runId}/oil-shows")
    public ResponseEntity<List<DrMudLogDto>> getOilShows(@PathVariable UUID runId) {
        log.debug("REST request to get oil shows for run: {}", runId);
        List<DrMudLogDto> shows = mudLogService.getOilShows(runId);
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/run/{runId}/fluorescence")
    public ResponseEntity<List<DrMudLogDto>> getFluorescenceShows(@PathVariable UUID runId) {
        log.debug("REST request to get fluorescence shows for run: {}", runId);
        List<DrMudLogDto> shows = mudLogService.getFluorescenceShows(runId);
        return ResponseEntity.ok(shows);
    }

    @PostMapping("/{id}/oil-show")
    public ResponseEntity<DrMudLogDto> recordOilShow(
            @PathVariable UUID id,
            @RequestParam(required = false) String showType,
            @RequestParam(required = false) String intensity,
            @RequestParam(required = false) String fluorescenceColor,
            @RequestParam(required = false) BigDecimal fluorescencePercent,
            @RequestParam(required = false) String cutDescription,
            @RequestParam(required = false) String stainDescription) {

        log.info("REST request to record oil show for Mud Log {}", id);
        DrMudLogDto updatedMudLog = mudLogService.recordOilShow(
                id, showType, intensity, fluorescenceColor, fluorescencePercent,
                cutDescription, stainDescription);
        return ResponseEntity.ok(updatedMudLog);
    }

    // --- Depth Range Endpoints ---

    @GetMapping("/run/{runId}/depth-range")
    public ResponseEntity<List<DrMudLogDto>> getMudLogsByDepthRange(
            @PathVariable UUID runId,
            @RequestParam BigDecimal minDepth,
            @RequestParam BigDecimal maxDepth) {

        log.debug("REST request to get Mud Logs in depth range {} - {} ft", minDepth, maxDepth);
        List<DrMudLogDto> mudLogs = mudLogService.getByDepthRange(runId, minDepth, maxDepth);
        return ResponseEntity.ok(mudLogs);
    }

    @GetMapping("/run/{runId}/deepest")
    public ResponseEntity<DrMudLogDto> getDeepestMudLog(@PathVariable UUID runId) {
        log.debug("REST request to get deepest Mud Log for run: {}", runId);
        DrMudLogDto mudLog = mudLogService.getDeepestMudLog(runId);
        if (mudLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mudLog);
    }

    @GetMapping("/run/{runId}/latest")
    public ResponseEntity<DrMudLogDto> getLatestMudLog(@PathVariable UUID runId) {
        log.debug("REST request to get latest Mud Log for run: {}", runId);
        DrMudLogDto mudLog = mudLogService.getLatestMudLog(runId);
        if (mudLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mudLog);
    }

    // --- Drilling Parameters Endpoints ---

    @GetMapping("/run/{runId}/high-rop")
    public ResponseEntity<List<DrMudLogDto>> getHighRopZones(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "100") BigDecimal threshold) {

        log.debug("REST request to get high ROP zones for run: {}", runId);
        List<DrMudLogDto> zones = mudLogService.getHighRopZones(runId, threshold);
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/run/{runId}/avg-rop")
    public ResponseEntity<BigDecimal> getAverageRopForDepthRange(
            @PathVariable UUID runId,
            @RequestParam BigDecimal minDepth,
            @RequestParam BigDecimal maxDepth) {

        log.debug("REST request to get average ROP for depth range");
        BigDecimal avgRop = mudLogService.getAverageRopForDepthRange(runId, minDepth, maxDepth);
        return ResponseEntity.ok(avgRop);
    }

    // --- Porosity Endpoints ---

    @GetMapping("/run/{runId}/porous-zones")
    public ResponseEntity<List<DrMudLogDto>> getPorousZones(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "5") BigDecimal threshold) {

        log.debug("REST request to get porous zones for run: {}", runId);
        List<DrMudLogDto> zones = mudLogService.getPorousZones(runId, threshold);
        return ResponseEntity.ok(zones);
    }

    // --- Statistics ---

    @GetMapping("/run/{runId}/count")
    public ResponseEntity<Long> countByRun(@PathVariable UUID runId) {
        long count = mudLogService.countByRun(runId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/well/{wellId}/count")
    public ResponseEntity<Long> countByWell(@PathVariable UUID wellId) {
        long count = mudLogService.countByWell(wellId);
        return ResponseEntity.ok(count);
    }

    // --- Geologist Endpoints ---

    @GetMapping("/run/{runId}/geologists")
    public ResponseEntity<List<String>> getDistinctGeologists(@PathVariable UUID runId) {
        log.debug("REST request to get distinct geologists for run: {}", runId);
        List<String> geologists = mudLogService.getDistinctGeologists(runId);
        return ResponseEntity.ok(geologists);
    }

    @GetMapping("/run/{runId}/geologist/{loggedBy}")
    public ResponseEntity<List<DrMudLogDto>> getMudLogsByGeologist(
            @PathVariable UUID runId,
            @PathVariable String loggedBy) {

        log.debug("REST request to get Mud Logs by geologist: {}", loggedBy);
        List<DrMudLogDto> mudLogs = mudLogService.getByGeologist(runId, loggedBy);
        return ResponseEntity.ok(mudLogs);
    }
}
