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
import org.thingsboard.nexus.dr.dto.DrBhaDto;
import org.thingsboard.nexus.dr.model.DrBha;
import org.thingsboard.nexus.dr.model.enums.BhaType;
import org.thingsboard.nexus.dr.service.DrBhaService;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Bottom Hole Assembly (BHA) operations
 */
@RestController
@RequestMapping("/api/nexus/dr/bhas")
@RequiredArgsConstructor
@Slf4j
public class DrBhaController {

    private final DrBhaService bhaService;

    @GetMapping("/{id}")
    public ResponseEntity<DrBhaDto> getBhaById(@PathVariable UUID id) {
        log.debug("REST request to get BHA: {}", id);
        DrBhaDto bha = bhaService.getById(id);
        return ResponseEntity.ok(bha);
    }

    @GetMapping("/tenant/{tenantId}/number/{bhaNumber}")
    public ResponseEntity<DrBhaDto> getBhaByNumber(
            @PathVariable UUID tenantId,
            @PathVariable String bhaNumber) {
        log.debug("REST request to get BHA by number: {}", bhaNumber);
        DrBhaDto bha = bhaService.getByNumber(tenantId, bhaNumber);
        return ResponseEntity.ok(bha);
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<DrBhaDto> getBhaByAssetId(@PathVariable UUID assetId) {
        log.debug("REST request to get BHA by asset ID: {}", assetId);
        DrBhaDto bha = bhaService.getByAssetId(assetId);
        return ResponseEntity.ok(bha);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<DrBhaDto>> getBhasByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bhaNumber") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get BHAs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrBhaDto> bhas = bhaService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(bhas);
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<Page<DrBhaDto>> getBhasByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BhaType bhaType,
            @RequestParam(required = false) Boolean isDirectional,
            @RequestParam(required = false) BigDecimal bitSizeIn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bhaNumber") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get BHAs with filters");

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrBhaDto> bhas = bhaService.getByFilters(tenantId, status, bhaType, isDirectional, bitSizeIn, pageable);
        return ResponseEntity.ok(bhas);
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<DrBhaDto>> getBhasByStatus(
            @PathVariable UUID tenantId,
            @PathVariable String status) {

        log.debug("REST request to get BHAs by status: {}", status);
        List<DrBhaDto> bhas = bhaService.getByStatus(tenantId, status);
        return ResponseEntity.ok(bhas);
    }

    @GetMapping("/tenant/{tenantId}/type/{bhaType}")
    public ResponseEntity<List<DrBhaDto>> getBhasByType(
            @PathVariable UUID tenantId,
            @PathVariable BhaType bhaType) {

        log.debug("REST request to get BHAs by type: {}", bhaType);
        List<DrBhaDto> bhas = bhaService.getByType(tenantId, bhaType);
        return ResponseEntity.ok(bhas);
    }

    @GetMapping("/tenant/{tenantId}/available")
    public ResponseEntity<List<DrBhaDto>> getAvailableBhas(@PathVariable UUID tenantId) {
        log.debug("REST request to get available BHAs");
        List<DrBhaDto> bhas = bhaService.getAvailableBhas(tenantId);
        return ResponseEntity.ok(bhas);
    }

    @GetMapping("/tenant/{tenantId}/available/directional")
    public ResponseEntity<List<DrBhaDto>> getAvailableDirectionalBhas(@PathVariable UUID tenantId) {
        log.debug("REST request to get available directional BHAs");
        List<DrBhaDto> bhas = bhaService.getAvailableDirectionalBhas(tenantId);
        return ResponseEntity.ok(bhas);
    }

    @GetMapping("/tenant/{tenantId}/bit-size/{bitSizeIn}")
    public ResponseEntity<List<DrBhaDto>> getBhasByBitSize(
            @PathVariable UUID tenantId,
            @PathVariable BigDecimal bitSizeIn) {

        log.debug("REST request to get BHAs by bit size: {}", bitSizeIn);
        List<DrBhaDto> bhas = bhaService.getByBitSize(tenantId, bitSizeIn);
        return ResponseEntity.ok(bhas);
    }

    @PostMapping("/from-template")
    public ResponseEntity<DrBhaDto> createBhaFromTemplate(
            @RequestParam UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create BHA from template: {}", request.getTemplateId());
        DrBhaDto createdBha = bhaService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBha);
    }

    @PostMapping
    public ResponseEntity<DrBhaDto> createBha(@Valid @RequestBody DrBha bha) {
        log.info("REST request to create BHA: {}", bha.getBhaNumber());
        DrBhaDto createdBha = bhaService.create(bha);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBha);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DrBhaDto> updateBha(
            @PathVariable UUID id,
            @Valid @RequestBody DrBha bha) {

        log.info("REST request to update BHA: {}", id);
        DrBhaDto updatedBha = bhaService.update(id, bha);
        return ResponseEntity.ok(updatedBha);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DrBhaDto> updateBhaStatus(
            @PathVariable UUID id,
            @RequestParam String status) {

        log.info("REST request to update BHA status: {} to {}", id, status);
        DrBhaDto updatedBha = bhaService.updateStatus(id, status);
        return ResponseEntity.ok(updatedBha);
    }

    @PostMapping("/{id}/dull-grade")
    public ResponseEntity<DrBhaDto> recordDullGrade(
            @PathVariable UUID id,
            @RequestParam(required = false) String dullInner,
            @RequestParam(required = false) String dullOuter,
            @RequestParam(required = false) String dullChar,
            @RequestParam(required = false) String dullLocation,
            @RequestParam(required = false) String bearingCondition,
            @RequestParam(required = false) String gaugeCondition,
            @RequestParam(required = false) String reasonPulled) {

        log.info("REST request to record dull grade for BHA {}", id);
        DrBhaDto updatedBha = bhaService.recordDullGrade(id, dullInner, dullOuter, dullChar,
                dullLocation, bearingCondition, gaugeCondition, reasonPulled);
        return ResponseEntity.ok(updatedBha);
    }

    @PutMapping("/{id}/statistics")
    public ResponseEntity<DrBhaDto> updateBhaStatistics(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal footageDrilled,
            @RequestParam(required = false) BigDecimal hoursOnBottom) {

        log.info("REST request to update statistics for BHA {}", id);
        DrBhaDto updatedBha = bhaService.updateStatistics(id, footageDrilled, hoursOnBottom);
        return ResponseEntity.ok(updatedBha);
    }

    @PostMapping("/{id}/increment-run")
    public ResponseEntity<DrBhaDto> incrementRunCount(@PathVariable UUID id) {
        log.info("REST request to increment run count for BHA {}", id);
        DrBhaDto updatedBha = bhaService.incrementRunCount(id);
        return ResponseEntity.ok(updatedBha);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBha(@PathVariable UUID id) {
        log.info("REST request to delete BHA: {}", id);
        bhaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}/count/status/{status}")
    public ResponseEntity<Long> countByStatus(
            @PathVariable UUID tenantId,
            @PathVariable String status) {
        long count = bhaService.countByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/tenant/{tenantId}/count/type/{bhaType}")
    public ResponseEntity<Long> countByType(
            @PathVariable UUID tenantId,
            @PathVariable BhaType bhaType) {
        long count = bhaService.countByType(tenantId, bhaType);
        return ResponseEntity.ok(count);
    }
}
