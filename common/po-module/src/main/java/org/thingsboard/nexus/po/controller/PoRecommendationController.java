/*
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.nexus.po.dto.RecommendationStatus;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.service.PoRecommendationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for optimization recommendations.
 */
@RestController
@RequestMapping("/api/nexus/po/recommendations")
@RequiredArgsConstructor
@Slf4j
public class PoRecommendationController {

    private final PoRecommendationService recommendationService;

    /**
     * Creates a new recommendation.
     */
    @PostMapping
    public ResponseEntity<RecommendationDto> createRecommendation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody RecommendationDto dto) {
        log.info("Creating recommendation for asset: {}", dto.getAssetId());
        dto.setTenantId(tenantId);
        RecommendationDto created = recommendationService.createRecommendation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a recommendation by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecommendationDto> getRecommendation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        return recommendationService.getRecommendation(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PoEntityNotFoundException("Recommendation", id));
    }

    /**
     * Gets all recommendations for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<RecommendationDto>> getRecommendations(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(recommendationService.getRecommendations(tenantId, page, size));
    }

    /**
     * Gets recommendations by status.
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<RecommendationDto>> getRecommendationsByStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable RecommendationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(recommendationService.getRecommendationsByStatus(tenantId, status, page, size));
    }

    /**
     * Gets pending recommendations.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<RecommendationDto>> getPendingRecommendations(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(recommendationService.getPendingRecommendations(tenantId));
    }

    /**
     * Gets pending recommendations for an asset.
     */
    @GetMapping("/pending/asset/{assetId}")
    public ResponseEntity<List<RecommendationDto>> getPendingRecommendationsForAsset(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID assetId) {
        return ResponseEntity.ok(recommendationService.getPendingRecommendationsForAsset(assetId));
    }

    /**
     * Gets recommendations by optimization type.
     */
    @GetMapping("/by-type/{type}")
    public ResponseEntity<Page<RecommendationDto>> getRecommendationsByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable OptimizationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(recommendationService.getRecommendationsByType(tenantId, type, page, size));
    }

    /**
     * Approves a recommendation.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<RecommendationDto> approveRecommendation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("Approving recommendation: {} by user: {}", id, userId);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(recommendationService.approveRecommendation(id, userId, notes));
    }

    /**
     * Rejects a recommendation.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<RecommendationDto> rejectRecommendation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        log.info("Rejecting recommendation: {} by user: {}", id, userId);
        String reason = body.get("reason");
        return ResponseEntity.ok(recommendationService.rejectRecommendation(id, userId, reason));
    }

    /**
     * Executes a recommendation.
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<RecommendationDto> executeRecommendation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        log.info("Executing recommendation: {} by user: {}", id, userId);
        return ResponseEntity.ok(recommendationService.executeRecommendation(id, userId));
    }

    /**
     * Cancels a recommendation.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<RecommendationDto> cancelRecommendation(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("Cancelling recommendation: {} by user: {}", id, userId);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(recommendationService.cancelRecommendation(id, userId, reason));
    }

    /**
     * Gets recommendation counts.
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getRecommendationCounts(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(Map.of(
                "pending", recommendationService.countPending(tenantId),
                "approved", recommendationService.countByStatus(tenantId, RecommendationStatus.APPROVED),
                "rejected", recommendationService.countByStatus(tenantId, RecommendationStatus.REJECTED),
                "executed", recommendationService.countByStatus(tenantId, RecommendationStatus.EXECUTED)
        ));
    }

    /**
     * Gets recommendation acceptance rate.
     */
    @GetMapping("/acceptance-rate")
    public ResponseEntity<Map<String, Double>> getAcceptanceRate(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        Double rate = recommendationService.getAcceptanceRate(tenantId);
        return ResponseEntity.ok(Map.of("acceptanceRate", rate != null ? rate : 0.0));
    }
}
