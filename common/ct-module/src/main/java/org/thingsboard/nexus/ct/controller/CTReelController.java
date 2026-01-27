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
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.ReelStatus;
import org.thingsboard.nexus.ct.service.CTReelService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/ct/reels")
@RequiredArgsConstructor
@Slf4j
public class CTReelController {

    private final CTReelService reelService;

    @GetMapping("/{id}")
    public ResponseEntity<CTReelDto> getReelById(@PathVariable UUID id) {
        log.debug("REST request to get CT Reel: {}", id);
        CTReelDto reel = reelService.getById(id);
        return ResponseEntity.ok(reel);
    }

    @GetMapping("/code/{reelCode}")
    public ResponseEntity<CTReelDto> getReelByCode(@PathVariable String reelCode) {
        log.debug("REST request to get CT Reel by code: {}", reelCode);
        CTReelDto reel = reelService.getByCode(reelCode);
        return ResponseEntity.ok(reel);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<CTReelDto>> getReelsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reelCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        log.debug("REST request to get CT Reels for tenant: {}", tenantId);
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CTReelDto> reels = reelService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(reels);
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<Page<CTReelDto>> getReelsByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) ReelStatus status,
            @RequestParam(required = false) BigDecimal odInch,
            @RequestParam(required = false) BigDecimal fatigueMin,
            @RequestParam(required = false) BigDecimal fatigueMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reelCode") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        log.debug("REST request to get CT Reels with filters");
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CTReelDto> reels = reelService.getByFilters(tenantId, status, odInch, 
                                                         fatigueMin, fatigueMax, pageable);
        return ResponseEntity.ok(reels);
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
            @RequestParam BigDecimal odInch,
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

    @PostMapping
    public ResponseEntity<CTReelDto> createReel(@Valid @RequestBody CTReel reel) {
        log.info("REST request to create CT Reel: {}", reel.getReelCode());
        CTReelDto createdReel = reelService.create(reel);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CTReelDto> updateReel(
            @PathVariable UUID id,
            @Valid @RequestBody CTReel reel) {
        
        log.info("REST request to update CT Reel: {}", id);
        CTReelDto updatedReel = reelService.update(id, reel);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CTReelDto> updateReelStatus(
            @PathVariable UUID id,
            @RequestParam ReelStatus status) {
        
        log.info("REST request to update CT Reel status: {} to {}", id, status);
        CTReelDto updatedReel = reelService.updateStatus(id, status);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{id}/fatigue")
    public ResponseEntity<CTReelDto> updateFatigue(
            @PathVariable UUID id,
            @RequestParam BigDecimal fatiguePercent,
            @RequestParam(required = false) Integer cycles) {
        
        log.info("REST request to update CT Reel fatigue: {}", id);
        CTReelDto updatedReel = reelService.updateFatigue(id, fatiguePercent, cycles);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{id}/inspection")
    public ResponseEntity<CTReelDto> recordInspection(
            @PathVariable UUID id,
            @RequestParam Long inspectionDate,
            @RequestParam String inspectionType,
            @RequestParam String inspectionResult,
            @RequestParam(required = false) String notes) {
        
        log.info("REST request to record inspection for reel {}", id);
        CTReelDto updatedReel = reelService.recordInspection(id, inspectionDate, 
                                                             inspectionType, inspectionResult, notes);
        return ResponseEntity.ok(updatedReel);
    }

    @PutMapping("/{id}/retire")
    public ResponseEntity<CTReelDto> retireReel(
            @PathVariable UUID id,
            @RequestParam String reason) {
        
        log.info("REST request to retire CT Reel: {}", id);
        CTReelDto updatedReel = reelService.retireReel(id, reason);
        return ResponseEntity.ok(updatedReel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReel(@PathVariable UUID id) {
        log.info("REST request to delete CT Reel: {}", id);
        reelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
