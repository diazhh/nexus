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
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.service.CTReelService;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for CT Reel operations.
 * All Reels are stored as ThingsBoard Assets of type "ct_reel".
 */
@RestController
@RequestMapping("/api/nexus/ct/reels")
@RequiredArgsConstructor
@Slf4j
public class CTReelController {

    private final CTReelService reelService;

    @GetMapping("/{assetId}")
    public ResponseEntity<CTReelDto> getReelById(@PathVariable UUID assetId) {
        log.debug("REST request to get CT Reel: {}", assetId);
        CTReelDto reel = reelService.getById(assetId);
        return ResponseEntity.ok(reel);
    }

    @GetMapping("/tenant/{tenantId}/code/{reelCode}")
    public ResponseEntity<CTReelDto> getReelByCode(
            @PathVariable UUID tenantId,
            @PathVariable String reelCode) {
        log.debug("REST request to get CT Reel by code: {}", reelCode);
        CTReelDto reel = reelService.getByCode(tenantId, reelCode);
        return ResponseEntity.ok(reel);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PageData<CTReelDto>> getReelsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get CT Reels for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CTReelDto> reels = reelService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(toPageData(reels));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<CTReelDto>> getReelsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable ReelStatus status) {

        log.debug("REST request to get CT Reels by status: {}", status);
        List<CTReelDto> reels = reelService.getByStatus(tenantId, status);
        return ResponseEntity.ok(reels);
    }

    @GetMapping("/tenant/{tenantId}/available")
    public ResponseEntity<List<CTReelDto>> getAvailableReelsBySize(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) BigDecimal odInch,
            @RequestParam(defaultValue = "90.0") BigDecimal maxFatigue) {

        log.debug("REST request to get available CT Reels by size: {} inch", odInch);
        List<CTReelDto> reels = reelService.getAvailableReelsBySize(tenantId, odInch, maxFatigue);
        return ResponseEntity.ok(reels);
    }

    @GetMapping("/tenant/{tenantId}/high-fatigue")
    public ResponseEntity<List<CTReelDto>> getReelsAboveFatigueThreshold(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "70.0") BigDecimal threshold) {

        log.debug("REST request to get CT Reels above fatigue threshold: {}%", threshold);
        List<CTReelDto> reels = reelService.getReelsAboveFatigueThreshold(tenantId, threshold);
        return ResponseEntity.ok(reels);
    }

    @PostMapping("/tenant/{tenantId}/from-template")
    public ResponseEntity<CTReelDto> createReelFromTemplate(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateFromTemplateRequest request) {
        log.info("REST request to create CT Reel from template: {}", request.getTemplateId());
        CTReelDto createdReel = reelService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReel);
    }

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<CTReelDto> createReel(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CTReelDto reel) {
        log.info("REST request to create CT Reel: {}", reel.getReelCode());
        CTReelDto createdReel = reelService.create(tenantId, reel);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReel);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<CTReelDto> updateReel(
            @PathVariable UUID assetId,
            @Valid @RequestBody CTReelDto reel) {

        log.info("REST request to update CT Reel: {}", assetId);
        CTReelDto updatedReel = reelService.update(assetId, reel);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{assetId}/status")
    public ResponseEntity<CTReelDto> updateReelStatus(
            @PathVariable UUID assetId,
            @RequestParam ReelStatus status) {

        log.info("REST request to update CT Reel status: {} to {}", assetId, status);
        CTReelDto updatedReel = reelService.updateStatus(assetId, status);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{assetId}/fatigue")
    public ResponseEntity<CTReelDto> updateFatigue(
            @PathVariable UUID assetId,
            @RequestParam BigDecimal fatiguePercent,
            @RequestParam(required = false) Integer cycles) {

        log.info("REST request to update CT Reel fatigue: {}", assetId);
        CTReelDto updatedReel = reelService.updateFatigue(assetId, fatiguePercent, cycles);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{assetId}/inspection")
    public ResponseEntity<CTReelDto> recordInspection(
            @PathVariable UUID assetId,
            @RequestParam Long inspectionDate,
            @RequestParam String inspectionType,
            @RequestParam String inspectionResult,
            @RequestParam(required = false) String notes) {

        log.info("REST request to record inspection for reel {}", assetId);
        CTReelDto updatedReel = reelService.recordInspection(assetId, inspectionDate,
                                                             inspectionType, inspectionResult, notes);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{assetId}/retire")
    public ResponseEntity<CTReelDto> retireReel(
            @PathVariable UUID assetId,
            @RequestParam String reason) {

        log.info("REST request to retire CT Reel: {}", assetId);
        CTReelDto updatedReel = reelService.retireReel(assetId, reason);
        return ResponseEntity.ok(updatedReel);
    }

    @DeleteMapping("/tenant/{tenantId}/{assetId}")
    public ResponseEntity<Void> deleteReel(
            @PathVariable UUID tenantId,
            @PathVariable UUID assetId) {
        log.info("REST request to delete CT Reel: {}", assetId);
        reelService.delete(tenantId, assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<Long> countReels(@PathVariable UUID tenantId) {
        log.debug("REST request to count CT Reels for tenant: {}", tenantId);
        return ResponseEntity.ok(reelService.countByTenant(tenantId));
    }

    @GetMapping("/tenant/{tenantId}/count/status/{status}")
    public ResponseEntity<Long> countByStatus(
            @PathVariable UUID tenantId,
            @PathVariable ReelStatus status) {
        log.debug("REST request to count CT Reels by status: {}", status);
        long count = reelService.countByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }
}
