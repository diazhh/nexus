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
import org.thingsboard.nexus.dr.dto.DrRunDto;
import org.thingsboard.nexus.dr.model.DrRun;
import org.thingsboard.nexus.dr.model.enums.HoleSection;
import org.thingsboard.nexus.dr.model.enums.RunStatus;
import org.thingsboard.nexus.dr.service.DrRunService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Drilling Run operations
 */
@RestController
@RequestMapping("/api/nexus/dr/runs")
@RequiredArgsConstructor
@Slf4j
public class DrRunController {

    private final DrRunService runService;

    @GetMapping("/{id}")
    public ResponseEntity<DrRunDto> getRunById(@PathVariable UUID id) {
        log.debug("REST request to get Drilling Run: {}", id);
        DrRunDto run = runService.getById(id);
        return ResponseEntity.ok(run);
    }

    @GetMapping("/tenant/{tenantId}/number/{runNumber}")
    public ResponseEntity<DrRunDto> getRunByNumber(
            @PathVariable UUID tenantId,
            @PathVariable String runNumber) {
        log.debug("REST request to get Drilling Run by number: {}", runNumber);
        DrRunDto run = runService.getByNumber(tenantId, runNumber);
        return ResponseEntity.ok(run);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<DrRunDto>> getRunsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "runNumber") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("REST request to get Drilling Runs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRunDto> runs = runService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/rig/{rigId}")
    public ResponseEntity<List<DrRunDto>> getRunsByRig(@PathVariable UUID rigId) {
        log.debug("REST request to get Drilling Runs for rig: {}", rigId);
        List<DrRunDto> runs = runService.getByRig(rigId);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/rig/{rigId}/paged")
    public ResponseEntity<Page<DrRunDto>> getRunsByRigPaged(
            @PathVariable UUID rigId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("REST request to get Drilling Runs for rig: {} (paged)", rigId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRunDto> runs = runService.getByRig(rigId, pageable);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/well/{wellId}")
    public ResponseEntity<List<DrRunDto>> getRunsByWell(@PathVariable UUID wellId) {
        log.debug("REST request to get Drilling Runs for well: {}", wellId);
        List<DrRunDto> runs = runService.getByWell(wellId);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/well/{wellId}/paged")
    public ResponseEntity<Page<DrRunDto>> getRunsByWellPaged(
            @PathVariable UUID wellId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("REST request to get Drilling Runs for well: {} (paged)", wellId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRunDto> runs = runService.getByWell(wellId, pageable);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/bha/{bhaId}")
    public ResponseEntity<List<DrRunDto>> getRunsByBha(@PathVariable UUID bhaId) {
        log.debug("REST request to get Drilling Runs for BHA: {}", bhaId);
        List<DrRunDto> runs = runService.getByBha(bhaId);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<DrRunDto>> getRunsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable RunStatus status) {

        log.debug("REST request to get Drilling Runs by status: {}", status);
        List<DrRunDto> runs = runService.getByStatus(tenantId, status);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/tenant/{tenantId}/active")
    public ResponseEntity<List<DrRunDto>> getActiveRuns(@PathVariable UUID tenantId) {
        log.debug("REST request to get active Drilling Runs");
        List<DrRunDto> runs = runService.getActiveRuns(tenantId);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/rig/{rigId}/current")
    public ResponseEntity<DrRunDto> getCurrentRunByRig(@PathVariable UUID rigId) {
        log.debug("REST request to get current run for rig: {}", rigId);
        DrRunDto run = runService.getCurrentRunByRig(rigId);
        if (run == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(run);
    }

    @GetMapping("/well/{wellId}/current")
    public ResponseEntity<DrRunDto> getCurrentRunByWell(@PathVariable UUID wellId) {
        log.debug("REST request to get current run for well: {}", wellId);
        DrRunDto run = runService.getCurrentRunByWell(wellId);
        if (run == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(run);
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<Page<DrRunDto>> getRunsByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) UUID rigId,
            @RequestParam(required = false) UUID wellId,
            @RequestParam(required = false) HoleSection holeSection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("REST request to get Drilling Runs with filters");

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRunDto> runs = runService.getByFilters(tenantId, status, rigId, wellId, holeSection, pageable);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/tenant/{tenantId}/date-range")
    public ResponseEntity<List<DrRunDto>> getRunsByDateRange(
            @PathVariable UUID tenantId,
            @RequestParam Long startDate,
            @RequestParam Long endDate) {

        log.debug("REST request to get Drilling Runs by date range");
        List<DrRunDto> runs = runService.getByDateRange(tenantId, startDate, endDate);
        return ResponseEntity.ok(runs);
    }

    @PostMapping
    public ResponseEntity<DrRunDto> createRun(@Valid @RequestBody DrRun run) {
        log.info("REST request to create Drilling Run: {}", run.getRunNumber());
        DrRunDto createdRun = runService.create(run);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRun);
    }

    @PostMapping("/create")
    public ResponseEntity<DrRunDto> createRunSimplified(
            @RequestParam UUID tenantId,
            @RequestParam UUID rigId,
            @RequestParam UUID wellId,
            @RequestParam String runNumber,
            @RequestParam HoleSection holeSection,
            @RequestParam BigDecimal holeSizeIn,
            @RequestParam BigDecimal plannedStartDepthMdFt,
            @RequestParam BigDecimal plannedEndDepthMdFt,
            @RequestParam(required = false) UUID createdBy) {

        log.info("REST request to create Drilling Run: {} for rig {} on well {}", runNumber, rigId, wellId);
        DrRunDto createdRun = runService.createRun(tenantId, rigId, wellId, runNumber,
                holeSection, holeSizeIn, plannedStartDepthMdFt, plannedEndDepthMdFt, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRun);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DrRunDto> updateRun(
            @PathVariable UUID id,
            @Valid @RequestBody DrRun run) {

        log.info("REST request to update Drilling Run: {}", id);
        DrRunDto updatedRun = runService.update(id, run);
        return ResponseEntity.ok(updatedRun);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<DrRunDto> startRun(
            @PathVariable UUID id,
            @RequestParam(required = false) Long startDate,
            @RequestParam BigDecimal startDepthMdFt,
            @RequestParam(required = false) BigDecimal startDepthTvdFt) {

        log.info("REST request to start Drilling Run: {}", id);
        DrRunDto updatedRun = runService.startRun(id, startDate, startDepthMdFt, startDepthTvdFt);
        return ResponseEntity.ok(updatedRun);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<DrRunDto> completeRun(
            @PathVariable UUID id,
            @RequestParam(required = false) Long endDate,
            @RequestParam BigDecimal endDepthMdFt,
            @RequestParam(required = false) BigDecimal endDepthTvdFt,
            @RequestParam(required = false) String reasonEnded,
            @RequestParam(required = false) String bitConditionOut) {

        log.info("REST request to complete Drilling Run: {}", id);
        DrRunDto updatedRun = runService.completeRun(id, endDate, endDepthMdFt, endDepthTvdFt, reasonEnded, bitConditionOut);
        return ResponseEntity.ok(updatedRun);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DrRunDto> updateRunStatus(
            @PathVariable UUID id,
            @RequestParam RunStatus status) {

        log.info("REST request to update Drilling Run status: {} to {}", id, status);
        DrRunDto updatedRun = runService.updateStatus(id, status);
        return ResponseEntity.ok(updatedRun);
    }

    @PutMapping("/{id}/depth")
    public ResponseEntity<DrRunDto> updateCurrentDepth(
            @PathVariable UUID id,
            @RequestParam BigDecimal currentDepthMdFt,
            @RequestParam(required = false) BigDecimal currentDepthTvdFt) {

        log.debug("REST request to update current depth for run: {}", id);
        DrRunDto updatedRun = runService.updateCurrentDepth(id, currentDepthMdFt, currentDepthTvdFt);
        return ResponseEntity.ok(updatedRun);
    }

    @PostMapping("/{runId}/assign-bha/{bhaId}")
    public ResponseEntity<DrRunDto> assignBha(
            @PathVariable UUID runId,
            @PathVariable UUID bhaId) {

        log.info("REST request to assign BHA {} to run {}", bhaId, runId);
        DrRunDto updatedRun = runService.assignBha(runId, bhaId);
        return ResponseEntity.ok(updatedRun);
    }

    @PostMapping("/{runId}/assign-mwd/{mwdToolAssetId}")
    public ResponseEntity<DrRunDto> assignMwdTool(
            @PathVariable UUID runId,
            @PathVariable UUID mwdToolAssetId) {

        log.info("REST request to assign MWD tool {} to run {}", mwdToolAssetId, runId);
        DrRunDto updatedRun = runService.assignMwdTool(runId, mwdToolAssetId);
        return ResponseEntity.ok(updatedRun);
    }

    @PostMapping("/{runId}/assign-lwd/{lwdToolAssetId}")
    public ResponseEntity<DrRunDto> assignLwdTool(
            @PathVariable UUID runId,
            @PathVariable UUID lwdToolAssetId) {

        log.info("REST request to assign LWD tool {} to run {}", lwdToolAssetId, runId);
        DrRunDto updatedRun = runService.assignLwdTool(runId, lwdToolAssetId);
        return ResponseEntity.ok(updatedRun);
    }

    @PutMapping("/{id}/kpis")
    public ResponseEntity<DrRunDto> updateKpis(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal avgRopFtHr,
            @RequestParam(required = false) BigDecimal maxRopFtHr,
            @RequestParam(required = false) BigDecimal totalRotatingHours,
            @RequestParam(required = false) BigDecimal totalSlidingHours,
            @RequestParam(required = false) BigDecimal totalCirculatingHours,
            @RequestParam(required = false) BigDecimal totalConnectionTimeHours,
            @RequestParam(required = false) BigDecimal totalNptHours,
            @RequestParam(required = false) Integer totalConnections) {

        log.debug("REST request to update KPIs for run: {}", id);
        DrRunDto updatedRun = runService.updateKpis(id, avgRopFtHr, maxRopFtHr,
                totalRotatingHours, totalSlidingHours, totalCirculatingHours,
                totalConnectionTimeHours, totalNptHours, totalConnections);
        return ResponseEntity.ok(updatedRun);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRun(@PathVariable UUID id) {
        log.info("REST request to delete Drilling Run: {}", id);
        runService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Statistics Endpoints ---

    @GetMapping("/tenant/{tenantId}/count/status/{status}")
    public ResponseEntity<Long> countByStatus(
            @PathVariable UUID tenantId,
            @PathVariable RunStatus status) {
        long count = runService.countByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/rig/{rigId}/count")
    public ResponseEntity<Long> countByRig(@PathVariable UUID rigId) {
        long count = runService.countByRig(rigId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/well/{wellId}/count")
    public ResponseEntity<Long> countByWell(@PathVariable UUID wellId) {
        long count = runService.countByWell(wellId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/rig/{rigId}/total-footage")
    public ResponseEntity<BigDecimal> getTotalFootageByRig(@PathVariable UUID rigId) {
        BigDecimal footage = runService.getTotalFootageByRig(rigId);
        return ResponseEntity.ok(footage);
    }

    @GetMapping("/well/{wellId}/total-footage")
    public ResponseEntity<BigDecimal> getTotalFootageByWell(@PathVariable UUID wellId) {
        BigDecimal footage = runService.getTotalFootageByWell(wellId);
        return ResponseEntity.ok(footage);
    }

    @GetMapping("/rig/{rigId}/avg-rop")
    public ResponseEntity<BigDecimal> getAverageRopByRig(@PathVariable UUID rigId) {
        BigDecimal avgRop = runService.getAverageRopByRig(rigId);
        return ResponseEntity.ok(avgRop);
    }
}
