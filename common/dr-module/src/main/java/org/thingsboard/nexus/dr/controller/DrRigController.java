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
import org.thingsboard.nexus.dr.dto.DrRigDto;
import org.thingsboard.nexus.dr.model.DrRig;
import org.thingsboard.nexus.dr.model.enums.RigStatus;
import org.thingsboard.nexus.dr.model.enums.RigType;
import org.thingsboard.nexus.dr.service.DrRigService;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Drilling Rig operations
 */
@RestController
@RequestMapping("/api/nexus/dr/rigs")
@RequiredArgsConstructor
@Slf4j
public class DrRigController {

    private final DrRigService rigService;

    @GetMapping("/{id}")
    public ResponseEntity<DrRigDto> getRigById(@PathVariable UUID id) {
        log.debug("REST request to get Drilling Rig: {}", id);
        DrRigDto rig = rigService.getById(id);
        return ResponseEntity.ok(rig);
    }

    @GetMapping("/code/{rigCode}")
    public ResponseEntity<DrRigDto> getRigByCode(@PathVariable String rigCode) {
        log.debug("REST request to get Drilling Rig by code: {}", rigCode);
        DrRigDto rig = rigService.getByCode(rigCode);
        return ResponseEntity.ok(rig);
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<DrRigDto> getRigByAssetId(@PathVariable UUID assetId) {
        log.debug("REST request to get Drilling Rig by asset ID: {}", assetId);
        DrRigDto rig = rigService.getByAssetId(assetId);
        return ResponseEntity.ok(rig);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PageData<DrRigDto>> getRigsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "rigCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Drilling Rigs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRigDto> rigs = rigService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(toPageData(rigs));
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<PageData<DrRigDto>> getRigsByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) RigStatus status,
            @RequestParam(required = false) RigType rigType,
            @RequestParam(required = false) String contractor,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "rigCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Drilling Rigs with filters");

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRigDto> rigs = rigService.getByFilters(tenantId, status, rigType, contractor, location, pageable);
        return ResponseEntity.ok(toPageData(rigs));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<DrRigDto>> getRigsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable RigStatus status) {

        log.debug("REST request to get Drilling Rigs by status: {}", status);
        List<DrRigDto> rigs = rigService.getByStatus(tenantId, status);
        return ResponseEntity.ok(rigs);
    }

    @GetMapping("/tenant/{tenantId}/type/{rigType}")
    public ResponseEntity<List<DrRigDto>> getRigsByType(
            @PathVariable UUID tenantId,
            @PathVariable RigType rigType) {

        log.debug("REST request to get Drilling Rigs by type: {}", rigType);
        List<DrRigDto> rigs = rigService.getByType(tenantId, rigType);
        return ResponseEntity.ok(rigs);
    }

    @GetMapping("/tenant/{tenantId}/available")
    public ResponseEntity<List<DrRigDto>> getAvailableRigs(@PathVariable UUID tenantId) {
        log.debug("REST request to get available Drilling Rigs");
        List<DrRigDto> rigs = rigService.getAvailableRigs(tenantId);
        return ResponseEntity.ok(rigs);
    }

    @GetMapping("/tenant/{tenantId}/bop-overdue")
    public ResponseEntity<List<DrRigDto>> getRigsWithOverdueBopTest(@PathVariable UUID tenantId) {
        log.debug("REST request to get Drilling Rigs with overdue BOP test");
        List<DrRigDto> rigs = rigService.getRigsWithOverdueBopTest(tenantId);
        return ResponseEntity.ok(rigs);
    }

    @PostMapping("/from-template")
    public ResponseEntity<DrRigDto> createRigFromTemplate(
            @RequestParam UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create Drilling Rig from template: {}", request.getTemplateId());
        DrRigDto createdRig = rigService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRig);
    }

    @PostMapping
    public ResponseEntity<DrRigDto> createRig(@Valid @RequestBody DrRig rig) {
        log.info("REST request to create Drilling Rig: {}", rig.getRigCode());
        DrRigDto createdRig = rigService.create(rig);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRig);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DrRigDto> updateRig(
            @PathVariable UUID id,
            @Valid @RequestBody DrRig rig) {

        log.info("REST request to update Drilling Rig: {}", id);
        DrRigDto updatedRig = rigService.update(id, rig);
        return ResponseEntity.ok(updatedRig);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DrRigDto> updateRigStatus(
            @PathVariable UUID id,
            @RequestParam RigStatus status) {

        log.info("REST request to update Drilling Rig status: {} to {}", id, status);
        DrRigDto updatedRig = rigService.updateStatus(id, status);
        return ResponseEntity.ok(updatedRig);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<DrRigDto> updateRigLocation(
            @PathVariable UUID id,
            @RequestParam String location,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        log.info("REST request to update Drilling Rig location: {}", id);
        DrRigDto updatedRig = rigService.updateLocation(id, location, latitude, longitude);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{rigId}/assign-well/{wellId}")
    public ResponseEntity<DrRigDto> assignToWell(
            @PathVariable UUID rigId,
            @PathVariable UUID wellId) {

        log.info("REST request to assign rig {} to well {}", rigId, wellId);
        DrRigDto updatedRig = rigService.assignToWell(rigId, wellId);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{rigId}/release-well")
    public ResponseEntity<DrRigDto> releaseFromWell(@PathVariable UUID rigId) {
        log.info("REST request to release rig {} from well", rigId);
        DrRigDto updatedRig = rigService.releaseFromWell(rigId);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{id}/bop-test")
    public ResponseEntity<DrRigDto> recordBopTest(
            @PathVariable UUID id,
            @RequestParam Long testDate,
            @RequestParam(required = false) String notes) {

        log.info("REST request to record BOP test for rig {}", id);
        DrRigDto updatedRig = rigService.recordBopTest(id, testDate, notes);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{id}/inspection")
    public ResponseEntity<DrRigDto> recordInspection(
            @PathVariable UUID id,
            @RequestParam Long inspectionDate,
            @RequestParam(required = false) Long nextInspectionDue,
            @RequestParam(required = false) String notes) {

        log.info("REST request to record inspection for rig {}", id);
        DrRigDto updatedRig = rigService.recordInspection(id, inspectionDate, nextInspectionDue, notes);
        return ResponseEntity.ok(updatedRig);
    }

    @PutMapping("/{id}/statistics")
    public ResponseEntity<DrRigDto> updateStatistics(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer totalWellsDrilled,
            @RequestParam(required = false) Double totalFootageDrilledFt,
            @RequestParam(required = false) Double totalNptHours,
            @RequestParam(required = false) Double totalOperationalHours) {

        log.info("REST request to update statistics for rig {}", id);
        DrRigDto updatedRig = rigService.updateStatistics(id, totalWellsDrilled,
            totalFootageDrilledFt, totalNptHours, totalOperationalHours);
        return ResponseEntity.ok(updatedRig);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRig(@PathVariable UUID id) {
        log.info("REST request to delete Drilling Rig: {}", id);
        rigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
