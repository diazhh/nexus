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
import org.thingsboard.nexus.po.dto.ml.PoMlPredictionDto;
import org.thingsboard.nexus.po.dto.ml.PredictionType;
import org.thingsboard.nexus.po.dto.ml.WellPredictionSummaryDto;
import org.thingsboard.nexus.po.service.ml.PoMlPredictionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for ML prediction queries.
 */
@RestController
@RequestMapping("/api/nexus/po/ml/predictions")
@RequiredArgsConstructor
@Slf4j
public class PoMlPredictionController {

    private final PoMlPredictionService predictionService;

    /**
     * Get prediction by ID.
     */
    @GetMapping("/{predictionId}")
    public ResponseEntity<PoMlPredictionDto> getPrediction(
            @PathVariable UUID predictionId) {
        return ResponseEntity.ok(predictionService.getPrediction(predictionId));
    }

    /**
     * Get latest prediction for a well by type.
     */
    @GetMapping("/well/{wellAssetId}/latest")
    public ResponseEntity<PoMlPredictionDto> getLatestPrediction(
            @PathVariable UUID wellAssetId,
            @RequestParam PredictionType predictionType) {
        PoMlPredictionDto prediction = predictionService.getLatestPrediction(wellAssetId, predictionType);
        if (prediction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(prediction);
    }

    /**
     * Get all predictions for a well.
     */
    @GetMapping("/well/{wellAssetId}")
    public ResponseEntity<List<PoMlPredictionDto>> getWellPredictions(
            @PathVariable UUID wellAssetId) {
        return ResponseEntity.ok(predictionService.getWellPredictions(wellAssetId));
    }

    /**
     * Get prediction history for a well.
     */
    @GetMapping("/well/{wellAssetId}/history")
    public ResponseEntity<List<PoMlPredictionDto>> getPredictionHistory(
            @PathVariable UUID wellAssetId,
            @RequestParam PredictionType predictionType,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        return ResponseEntity.ok(predictionService.getPredictionHistory(wellAssetId, predictionType, startTime, endTime));
    }

    /**
     * Get high-risk wells for a tenant.
     */
    @GetMapping("/high-risk")
    public ResponseEntity<List<WellPredictionSummaryDto>> getHighRiskWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0.7") BigDecimal threshold) {
        return ResponseEntity.ok(predictionService.getHighRiskWells(tenantId, threshold));
    }

    /**
     * Get wells with anomalies.
     */
    @GetMapping("/anomalous")
    public ResponseEntity<List<WellPredictionSummaryDto>> getAnomalousWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(predictionService.getAnomalousWells(tenantId));
    }

    /**
     * Get failure prediction summaries for all wells.
     */
    @GetMapping("/summaries/failure")
    public ResponseEntity<List<WellPredictionSummaryDto>> getFailurePredictionSummaries(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(predictionService.getFailurePredictionSummaries(tenantId));
    }

    /**
     * Get health score summaries for all wells.
     */
    @GetMapping("/summaries/health")
    public ResponseEntity<List<WellPredictionSummaryDto>> getHealthScoreSummaries(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(predictionService.getHealthScoreSummaries(tenantId));
    }

    /**
     * Acknowledge a prediction.
     */
    @PostMapping("/{predictionId}/acknowledge")
    public ResponseEntity<PoMlPredictionDto> acknowledgePrediction(
            @PathVariable UUID predictionId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Acknowledging prediction {} by user {}", predictionId, userId);
        return ResponseEntity.ok(predictionService.acknowledgePrediction(predictionId, userId));
    }

    /**
     * Dismiss a prediction.
     */
    @PostMapping("/{predictionId}/dismiss")
    public ResponseEntity<PoMlPredictionDto> dismissPrediction(
            @PathVariable UUID predictionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String reason) {
        log.info("Dismissing prediction {} by user {} with reason: {}", predictionId, userId, reason);
        return ResponseEntity.ok(predictionService.dismissPrediction(predictionId, userId, reason));
    }

    /**
     * Count pending actions.
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Long> countPendingActions(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0.7") BigDecimal threshold) {
        return ResponseEntity.ok(predictionService.countPendingActions(tenantId, threshold));
    }
}
