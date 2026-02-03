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
import org.thingsboard.nexus.dr.service.DrRigService;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Drilling Rig operations.
 * All rigs are stored as ThingsBoard Assets of type "dr_rig".
 */
@RestController
@RequestMapping("/api/nexus/dr/rigs")
@RequiredArgsConstructor
@Slf4j
public class DrRigController {

    private final DrRigService rigService;

    @GetMapping("/{assetId}")
    public ResponseEntity<DrRigDto> getRigById(@PathVariable UUID assetId) {
        log.debug("REST request to get Drilling Rig: {}", assetId);
        DrRigDto rig = rigService.getById(assetId);
        return ResponseEntity.ok(rig);
    }

    @GetMapping("/tenant/{tenantId}/code/{rigCode}")
    public ResponseEntity<DrRigDto> getRigByCode(
            @PathVariable UUID tenantId,
            @PathVariable String rigCode) {
        log.debug("REST request to get Drilling Rig by code: {}", rigCode);
        DrRigDto rig = rigService.getByCode(tenantId, rigCode);
        return ResponseEntity.ok(rig);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PageData<DrRigDto>> getRigsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Drilling Rigs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrRigDto> rigs = rigService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(toPageData(rigs));
    }

    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<PageData<DrRigDto>> searchRigs(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to search Drilling Rigs: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<DrRigDto> rigs = rigService.searchByName(tenantId, query, pageable);
        return ResponseEntity.ok(toPageData(rigs));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<DrRigDto>> getRigsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable String status) {

        log.debug("REST request to get Drilling Rigs by status: {}", status);
        List<DrRigDto> rigs = rigService.getByStatus(tenantId, status);
        return ResponseEntity.ok(rigs);
    }

    @GetMapping("/tenant/{tenantId}/type/{rigType}")
    public ResponseEntity<List<DrRigDto>> getRigsByType(
            @PathVariable UUID tenantId,
            @PathVariable String rigType) {

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

    @PostMapping("/tenant/{tenantId}/from-template")
    public ResponseEntity<DrRigDto> createRigFromTemplate(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create Drilling Rig from template: {}", request.getTemplateId());
        DrRigDto createdRig = rigService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRig);
    }

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<DrRigDto> createRig(
            @PathVariable UUID tenantId,
            @Valid @RequestBody DrRigDto rig) {
        log.info("REST request to create Drilling Rig: {}", rig.getRigCode());
        DrRigDto createdRig = rigService.create(tenantId, rig);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRig);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<DrRigDto> updateRig(
            @PathVariable UUID assetId,
            @Valid @RequestBody DrRigDto rig) {

        log.info("REST request to update Drilling Rig: {}", assetId);
        DrRigDto updatedRig = rigService.update(assetId, rig);
        return ResponseEntity.ok(updatedRig);
    }

    @PutMapping("/{assetId}/status")
    public ResponseEntity<DrRigDto> updateRigStatus(
            @PathVariable UUID assetId,
            @RequestParam String status) {

        log.info("REST request to update Drilling Rig status: {} to {}", assetId, status);
        DrRigDto updatedRig = rigService.updateStatus(assetId, status);
        return ResponseEntity.ok(updatedRig);
    }

    @PutMapping("/{assetId}/location")
    public ResponseEntity<DrRigDto> updateRigLocation(
            @PathVariable UUID assetId,
            @RequestParam String location,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        log.info("REST request to update Drilling Rig location: {}", assetId);
        BigDecimal lat = latitude != null ? BigDecimal.valueOf(latitude) : null;
        BigDecimal lon = longitude != null ? BigDecimal.valueOf(longitude) : null;
        DrRigDto updatedRig = rigService.updateLocation(assetId, location, lat, lon);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{assetId}/assign-well/{wellId}")
    public ResponseEntity<DrRigDto> assignToWell(
            @PathVariable UUID assetId,
            @PathVariable UUID wellId) {

        log.info("REST request to assign rig {} to well {}", assetId, wellId);
        DrRigDto updatedRig = rigService.assignToWell(assetId, wellId);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{assetId}/release-well")
    public ResponseEntity<DrRigDto> releaseFromWell(@PathVariable UUID assetId) {
        log.info("REST request to release rig {} from well", assetId);
        DrRigDto updatedRig = rigService.releaseFromWell(assetId);
        return ResponseEntity.ok(updatedRig);
    }

    @PostMapping("/{assetId}/bop-test")
    public ResponseEntity<DrRigDto> recordBopTest(
            @PathVariable UUID assetId,
            @RequestParam Long testDate,
            @RequestParam(required = false) String notes) {

        log.info("REST request to record BOP test for rig {}", assetId);
        DrRigDto updatedRig = rigService.recordBopTest(assetId, testDate, notes);
        return ResponseEntity.ok(updatedRig);
    }

    @DeleteMapping("/tenant/{tenantId}/{assetId}")
    public ResponseEntity<Void> deleteRig(
            @PathVariable UUID tenantId,
            @PathVariable UUID assetId) {
        log.info("REST request to delete Drilling Rig: {}", assetId);
        rigService.delete(tenantId, assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<Long> countRigs(@PathVariable UUID tenantId) {
        log.debug("REST request to count Drilling Rigs for tenant: {}", tenantId);
        return ResponseEntity.ok(rigService.countByTenant(tenantId));
    }

    @GetMapping("/tenant/{tenantId}/count/status/{status}")
    public ResponseEntity<Long> countRigsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable String status) {
        log.debug("REST request to count Drilling Rigs by status: {}", status);
        return ResponseEntity.ok(rigService.countByStatus(tenantId, status));
    }

    @GetMapping("/tenant/{tenantId}/count/type/{rigType}")
    public ResponseEntity<Long> countRigsByType(
            @PathVariable UUID tenantId,
            @PathVariable String rigType) {
        log.debug("REST request to count Drilling Rigs by type: {}", rigType);
        return ResponseEntity.ok(rigService.countByType(tenantId, rigType));
    }
}
