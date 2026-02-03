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
import org.thingsboard.nexus.dr.service.DrBhaService;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Bottom Hole Assembly (BHA) operations.
 * All BHAs are stored as ThingsBoard Assets of type "dr_bha".
 */
@RestController
@RequestMapping("/api/nexus/dr/bhas")
@RequiredArgsConstructor
@Slf4j
public class DrBhaController {

    private final DrBhaService bhaService;

    @GetMapping("/{assetId}")
    public ResponseEntity<DrBhaDto> getBhaById(@PathVariable UUID assetId) {
        log.debug("REST request to get BHA: {}", assetId);
        DrBhaDto bha = bhaService.getById(assetId);
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

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PageData<DrBhaDto>> getBhasByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get BHAs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrBhaDto> bhas = bhaService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(toPageData(bhas));
    }

    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<PageData<DrBhaDto>> searchBhas(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to search BHAs: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<DrBhaDto> bhas = bhaService.searchByName(tenantId, query, pageable);
        return ResponseEntity.ok(toPageData(bhas));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
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
            @PathVariable String bhaType) {

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

    @PostMapping("/tenant/{tenantId}/from-template")
    public ResponseEntity<DrBhaDto> createBhaFromTemplate(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create BHA from template: {}", request.getTemplateId());
        DrBhaDto createdBha = bhaService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBha);
    }

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<DrBhaDto> createBha(
            @PathVariable UUID tenantId,
            @Valid @RequestBody DrBhaDto bha) {
        log.info("REST request to create BHA: {}", bha.getBhaNumber());
        DrBhaDto createdBha = bhaService.create(tenantId, bha);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBha);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<DrBhaDto> updateBha(
            @PathVariable UUID assetId,
            @Valid @RequestBody DrBhaDto bha) {

        log.info("REST request to update BHA: {}", assetId);
        DrBhaDto updatedBha = bhaService.update(assetId, bha);
        return ResponseEntity.ok(updatedBha);
    }

    @PutMapping("/{assetId}/status")
    public ResponseEntity<DrBhaDto> updateBhaStatus(
            @PathVariable UUID assetId,
            @RequestParam String status) {

        log.info("REST request to update BHA status: {} to {}", assetId, status);
        DrBhaDto updatedBha = bhaService.updateStatus(assetId, status);
        return ResponseEntity.ok(updatedBha);
    }

    @PostMapping("/{assetId}/dull-grade")
    public ResponseEntity<DrBhaDto> recordDullGrade(
            @PathVariable UUID assetId,
            @RequestParam(required = false) String dullInner,
            @RequestParam(required = false) String dullOuter,
            @RequestParam(required = false) String dullChar,
            @RequestParam(required = false) String dullLocation,
            @RequestParam(required = false) String bearingCondition,
            @RequestParam(required = false) String gaugeCondition,
            @RequestParam(required = false) String reasonPulled) {

        log.info("REST request to record dull grade for BHA {}", assetId);
        DrBhaDto updatedBha = bhaService.recordDullGrade(assetId, dullInner, dullOuter, dullChar,
                dullLocation, bearingCondition, gaugeCondition, reasonPulled);
        return ResponseEntity.ok(updatedBha);
    }

    @PutMapping("/{assetId}/statistics")
    public ResponseEntity<DrBhaDto> updateBhaStatistics(
            @PathVariable UUID assetId,
            @RequestParam(required = false) BigDecimal footageDrilled,
            @RequestParam(required = false) BigDecimal hoursOnBottom) {

        log.info("REST request to update statistics for BHA {}", assetId);
        DrBhaDto updatedBha = bhaService.updateStatistics(assetId, footageDrilled, hoursOnBottom);
        return ResponseEntity.ok(updatedBha);
    }

    @PostMapping("/{assetId}/increment-run")
    public ResponseEntity<DrBhaDto> incrementRunCount(@PathVariable UUID assetId) {
        log.info("REST request to increment run count for BHA {}", assetId);
        DrBhaDto updatedBha = bhaService.incrementRunCount(assetId);
        return ResponseEntity.ok(updatedBha);
    }

    @DeleteMapping("/tenant/{tenantId}/{assetId}")
    public ResponseEntity<Void> deleteBha(
            @PathVariable UUID tenantId,
            @PathVariable UUID assetId) {
        log.info("REST request to delete BHA: {}", assetId);
        bhaService.delete(tenantId, assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<Long> countBhas(@PathVariable UUID tenantId) {
        log.debug("REST request to count BHAs for tenant: {}", tenantId);
        return ResponseEntity.ok(bhaService.countByTenant(tenantId));
    }

    @GetMapping("/tenant/{tenantId}/count/status/{status}")
    public ResponseEntity<Long> countByStatus(
            @PathVariable UUID tenantId,
            @PathVariable String status) {
        log.debug("REST request to count BHAs by status: {}", status);
        long count = bhaService.countByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/tenant/{tenantId}/count/type/{bhaType}")
    public ResponseEntity<Long> countByType(
            @PathVariable UUID tenantId,
            @PathVariable String bhaType) {
        log.debug("REST request to count BHAs by type: {}", bhaType);
        long count = bhaService.countByType(tenantId, bhaType);
        return ResponseEntity.ok(count);
    }
}
