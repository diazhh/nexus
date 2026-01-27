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
import org.thingsboard.nexus.ct.dto.CTUnitDto;
import org.thingsboard.nexus.ct.dto.CreateFromTemplateRequest;
import org.thingsboard.nexus.ct.dto.template.CTTemplateDto;
import org.thingsboard.nexus.ct.model.CTUnit;
import org.thingsboard.nexus.ct.model.UnitStatus;
import org.thingsboard.nexus.ct.service.CTTemplateService;
import org.thingsboard.nexus.ct.service.CTUnitService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/ct/units")
@RequiredArgsConstructor
@Slf4j
public class CTUnitController {

    private final CTUnitService unitService;
    private final CTTemplateService templateService;

    @GetMapping("/{id}")
    public ResponseEntity<CTUnitDto> getUnitById(@PathVariable UUID id) {
        log.debug("REST request to get CT Unit: {}", id);
        CTUnitDto unit = unitService.getById(id);
        return ResponseEntity.ok(unit);
    }

    @GetMapping("/code/{unitCode}")
    public ResponseEntity<CTUnitDto> getUnitByCode(@PathVariable String unitCode) {
        log.debug("REST request to get CT Unit by code: {}", unitCode);
        CTUnitDto unit = unitService.getByCode(unitCode);
        return ResponseEntity.ok(unit);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<CTUnitDto>> getUnitsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "unitCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        log.debug("REST request to get CT Units for tenant: {}", tenantId);
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CTUnitDto> units = unitService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<Page<CTUnitDto>> getUnitsByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) UnitStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "unitCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        log.debug("REST request to get CT Units with filters");
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CTUnitDto> units = unitService.getByFilters(tenantId, status, location, pageable);
        return ResponseEntity.ok(units);
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

    @GetMapping("/templates")
    public ResponseEntity<List<CTTemplateDto>> getAvailableTemplates(
            @RequestParam(required = false) String category) {
        log.debug("REST request to get available CT Unit templates");
        List<CTTemplateDto> templates = templateService.getAvailableTemplates(category);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/from-template")
    public ResponseEntity<CTUnitDto> createUnitFromTemplate(
            @RequestParam UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create CT Unit from template: {}", request.getTemplateId());
        CTUnitDto createdUnit = unitService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnit);
    }

    @PostMapping
    public ResponseEntity<CTUnitDto> createUnit(@Valid @RequestBody CTUnit unit) {
        log.info("REST request to create CT Unit: {}", unit.getUnitCode());
        CTUnitDto createdUnit = unitService.create(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnit);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CTUnitDto> updateUnit(
            @PathVariable UUID id,
            @Valid @RequestBody CTUnit unit) {
        
        log.info("REST request to update CT Unit: {}", id);
        CTUnitDto updatedUnit = unitService.update(id, unit);
        return ResponseEntity.ok(updatedUnit);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CTUnitDto> updateUnitStatus(
            @PathVariable UUID id,
            @RequestParam UnitStatus status) {
        
        log.info("REST request to update CT Unit status: {} to {}", id, status);
        CTUnitDto updatedUnit = unitService.updateStatus(id, status);
        return ResponseEntity.ok(updatedUnit);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<CTUnitDto> updateUnitLocation(
            @PathVariable UUID id,
            @RequestParam String location,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        
        log.info("REST request to update CT Unit location: {}", id);
        CTUnitDto updatedUnit = unitService.updateLocation(id, location, latitude, longitude);
        return ResponseEntity.ok(updatedUnit);
    }

    @PostMapping("/{unitId}/reel/{reelId}")
    public ResponseEntity<CTUnitDto> assignReel(
            @PathVariable UUID unitId,
            @PathVariable UUID reelId) {
        
        log.info("REST request to assign reel {} to unit {}", reelId, unitId);
        CTUnitDto updatedUnit = unitService.assignReel(unitId, reelId);
        return ResponseEntity.ok(updatedUnit);
    }

    @DeleteMapping("/{unitId}/reel")
    public ResponseEntity<CTUnitDto> detachReel(@PathVariable UUID unitId) {
        log.info("REST request to detach reel from unit {}", unitId);
        CTUnitDto updatedUnit = unitService.detachReel(unitId);
        return ResponseEntity.ok(updatedUnit);
    }

    @PutMapping("/{id}/maintenance")
    public ResponseEntity<CTUnitDto> recordMaintenance(
            @PathVariable UUID id,
            @RequestParam Long maintenanceDate,
            @RequestParam(required = false) String notes) {
        
        log.info("REST request to record maintenance for unit {}", id);
        CTUnitDto updatedUnit = unitService.recordMaintenance(id, maintenanceDate, notes);
        return ResponseEntity.ok(updatedUnit);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable UUID id) {
        log.info("REST request to delete CT Unit: {}", id);
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
