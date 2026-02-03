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
package org.thingsboard.nexus.ct.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.ct.dto.CTReelDto;
import org.thingsboard.nexus.ct.dto.CTUnitDto;
import org.thingsboard.nexus.ct.model.UnitStatus;
import org.thingsboard.nexus.ct.service.CTReelService;
import org.thingsboard.nexus.ct.service.CTUnitService;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for CT Unit operations.
 * All Units are stored as ThingsBoard Assets of type "ct_unit".
 */
@RestController
@RequestMapping("/api/nexus/ct/units")
@RequiredArgsConstructor
@Slf4j
public class CTUnitController {

    private final CTUnitService unitService;
    private final CTReelService reelService;

    @GetMapping("/{assetId}")
    public ResponseEntity<CTUnitDto> getUnitById(@PathVariable UUID assetId) {
        log.debug("REST request to get CT Unit: {}", assetId);
        CTUnitDto unit = unitService.getById(assetId);
        return ResponseEntity.ok(unit);
    }

    @GetMapping("/tenant/{tenantId}/code/{unitCode}")
    public ResponseEntity<CTUnitDto> getUnitByCode(
            @PathVariable UUID tenantId,
            @PathVariable String unitCode) {
        log.debug("REST request to get CT Unit by code: {}", unitCode);
        CTUnitDto unit = unitService.getByCode(tenantId, unitCode);
        return ResponseEntity.ok(unit);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PageData<CTUnitDto>> getUnitsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get CT Units for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CTUnitDto> units = unitService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(toPageData(units));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<CTUnitDto>> getUnitsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable UnitStatus status) {

        log.debug("REST request to get CT Units by status: {}", status);
        List<CTUnitDto> units = unitService.getByStatus(tenantId, status);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/tenant/{tenantId}/available")
    public ResponseEntity<List<CTUnitDto>> getAvailableUnits(@PathVariable UUID tenantId) {
        log.debug("REST request to get available CT Units");
        List<CTUnitDto> units = unitService.getAvailableUnits(tenantId);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/tenant/{tenantId}/maintenance-due")
    public ResponseEntity<List<CTUnitDto>> getUnitsRequiringMaintenance(@PathVariable UUID tenantId) {
        log.debug("REST request to get CT Units requiring maintenance");
        List<CTUnitDto> units = unitService.getUnitsRequiringMaintenance(tenantId);
        return ResponseEntity.ok(units);
    }

    @PostMapping("/tenant/{tenantId}/from-template")
    public ResponseEntity<CTUnitDto> createUnitFromTemplate(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create CT Unit from template: {}", request.getTemplateId());
        CTUnitDto createdUnit = unitService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnit);
    }

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<CTUnitDto> createUnit(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CTUnitDto unit) {
        log.info("REST request to create CT Unit: {}", unit.getUnitCode());
        CTUnitDto createdUnit = unitService.create(tenantId, unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnit);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<CTUnitDto> updateUnit(
            @PathVariable UUID assetId,
            @Valid @RequestBody CTUnitDto unit) {

        log.info("REST request to update CT Unit: {}", assetId);
        CTUnitDto updatedUnit = unitService.update(assetId, unit);
        return ResponseEntity.ok(updatedUnit);
    }

    @PutMapping("/{assetId}/status")
    public ResponseEntity<CTUnitDto> updateUnitStatus(
            @PathVariable UUID assetId,
            @RequestParam UnitStatus status) {

        log.info("REST request to update CT Unit status: {} to {}", assetId, status);
        CTUnitDto updatedUnit = unitService.updateStatus(assetId, status);
        return ResponseEntity.ok(updatedUnit);
    }

    @PutMapping("/{assetId}/location")
    public ResponseEntity<CTUnitDto> updateUnitLocation(
            @PathVariable UUID assetId,
            @RequestParam String location,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        log.info("REST request to update CT Unit location: {}", assetId);
        CTUnitDto updatedUnit = unitService.updateLocation(assetId, location, latitude, longitude);
        return ResponseEntity.ok(updatedUnit);
    }

    @PostMapping("/{unitAssetId}/reel/{reelAssetId}")
    public ResponseEntity<CTUnitDto> assignReel(
            @PathVariable UUID unitAssetId,
            @PathVariable UUID reelAssetId) {

        log.info("REST request to assign reel {} to unit {}", reelAssetId, unitAssetId);
        CTReelDto reelDto = reelService.getById(reelAssetId);
        CTUnitDto updatedUnit = unitService.assignReel(unitAssetId, reelAssetId, reelDto);
        return ResponseEntity.ok(updatedUnit);
    }

    @DeleteMapping("/{unitAssetId}/reel")
    public ResponseEntity<CTUnitDto> detachReel(@PathVariable UUID unitAssetId) {
        log.info("REST request to detach reel from unit {}", unitAssetId);
        CTUnitDto updatedUnit = unitService.detachReel(unitAssetId);
        return ResponseEntity.ok(updatedUnit);
    }

    @PutMapping("/{assetId}/maintenance")
    public ResponseEntity<CTUnitDto> recordMaintenance(
            @PathVariable UUID assetId,
            @RequestParam Long maintenanceDate,
            @RequestParam(required = false) String notes) {

        log.info("REST request to record maintenance for unit {}", assetId);
        CTUnitDto updatedUnit = unitService.recordMaintenance(assetId, maintenanceDate, notes);
        return ResponseEntity.ok(updatedUnit);
    }

    @DeleteMapping("/tenant/{tenantId}/{assetId}")
    public ResponseEntity<Void> deleteUnit(
            @PathVariable UUID tenantId,
            @PathVariable UUID assetId) {
        log.info("REST request to delete CT Unit: {}", assetId);
        unitService.delete(tenantId, assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<Long> countUnits(@PathVariable UUID tenantId) {
        log.debug("REST request to count CT Units for tenant: {}", tenantId);
        return ResponseEntity.ok(unitService.countByTenant(tenantId));
    }

    @GetMapping("/tenant/{tenantId}/count/status/{status}")
    public ResponseEntity<Long> countByStatus(
            @PathVariable UUID tenantId,
            @PathVariable UnitStatus status) {
        log.debug("REST request to count CT Units by status: {}", status);
        long count = unitService.countByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }
}
