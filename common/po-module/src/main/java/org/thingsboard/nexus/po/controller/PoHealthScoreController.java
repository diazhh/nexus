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
package org.thingsboard.nexus.po.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.po.dto.HealthLevel;
import org.thingsboard.nexus.po.dto.HealthScoreDto;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.service.PoHealthScoreService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for equipment health scores.
 */
@RestController
@RequestMapping("/api/nexus/po/health")
@RequiredArgsConstructor
@Slf4j
public class PoHealthScoreController {

    private final PoHealthScoreService healthScoreService;

    /**
     * Calculates health score for an asset.
     */
    @PostMapping("/calculate/{assetId}")
    public ResponseEntity<HealthScoreDto> calculateHealthScore(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID assetId) {
        log.info("Calculating health score for asset: {}", assetId);
        HealthScoreDto result = healthScoreService.calculateHealthScore(tenantId, assetId);
        if (result == null) {
            throw new PoEntityNotFoundException("Asset", assetId);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Gets current health score for an asset.
     */
    @GetMapping("/{assetId}")
    public ResponseEntity<HealthScoreDto> getHealthScore(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID assetId) {
        return healthScoreService.getHealthScore(tenantId, assetId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PoEntityNotFoundException("HealthScore", assetId));
    }

    /**
     * Gets health scores for all wells.
     */
    @GetMapping("/wells")
    public ResponseEntity<List<HealthScoreDto>> getWellHealthScores(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(healthScoreService.getWellHealthScores(tenantId, page, size));
    }

    /**
     * Gets wells by health level.
     */
    @GetMapping("/wells/by-level/{level}")
    public ResponseEntity<List<HealthScoreDto>> getWellsByHealthLevel(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable HealthLevel level) {
        return ResponseEntity.ok(healthScoreService.getWellsByHealthLevel(tenantId, level));
    }

    /**
     * Gets critical wells (CRITICAL or POOR health).
     */
    @GetMapping("/wells/critical")
    public ResponseEntity<List<HealthScoreDto>> getCriticalWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(healthScoreService.getCriticalWells(tenantId));
    }

    /**
     * Gets health summary (count by level).
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<HealthLevel, Long>> getHealthSummary(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(healthScoreService.getHealthSummary(tenantId));
    }
}
