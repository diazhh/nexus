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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.po.dto.ml.PoMlTrainingJobDto;
import org.thingsboard.nexus.po.dto.ml.StartTrainingRequest;
import org.thingsboard.nexus.po.service.ml.PoMlTrainingService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for ML training job management.
 */
@RestController
@RequestMapping("/api/nexus/po/ml/training")
@RequiredArgsConstructor
@Slf4j
public class PoMlTrainingController {

    private final PoMlTrainingService trainingService;

    /**
     * Get training jobs for tenant with pagination.
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<PoMlTrainingJobDto>> getTrainingJobs(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTime"));
        return ResponseEntity.ok(trainingService.getTrainingJobs(tenantId, pageRequest));
    }

    /**
     * Get a training job by ID.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<PoMlTrainingJobDto> getTrainingJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(trainingService.getTrainingJob(jobId));
    }

    /**
     * Get running jobs for tenant.
     */
    @GetMapping("/jobs/running")
    public ResponseEntity<List<PoMlTrainingJobDto>> getRunningJobs(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(trainingService.getRunningJobs(tenantId));
    }

    /**
     * Start a new training job.
     */
    @PostMapping("/start")
    public ResponseEntity<PoMlTrainingJobDto> startTrainingJob(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody StartTrainingRequest request) {
        log.info("Starting training job for model {} by user {}", request.getModelName(), userId);
        return ResponseEntity.ok(trainingService.startTrainingJob(tenantId, userId, request));
    }

    /**
     * Cancel a training job.
     */
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<PoMlTrainingJobDto> cancelJob(
            @PathVariable UUID jobId) {
        log.info("Cancelling training job: {}", jobId);
        return ResponseEntity.ok(trainingService.cancelJob(jobId));
    }

    /**
     * Update job progress (internal API for Python ML service).
     */
    @PutMapping("/jobs/{jobId}/progress")
    public ResponseEntity<Void> updateJobProgress(
            @PathVariable UUID jobId,
            @RequestParam int progressPercent,
            @RequestParam(required = false) Integer currentEpoch,
            @RequestParam(required = false) Integer totalEpochs,
            @RequestParam(required = false) String currentStep) {
        trainingService.updateJobProgress(jobId, progressPercent, currentEpoch, totalEpochs, currentStep);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark job as completed (internal API for Python ML service).
     */
    @PostMapping("/jobs/{jobId}/complete")
    public ResponseEntity<Void> completeJob(
            @PathVariable UUID jobId,
            @RequestParam UUID resultModelId) {
        log.info("Completing training job {} with model {}", jobId, resultModelId);
        trainingService.completeJob(jobId, resultModelId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark job as failed (internal API for Python ML service).
     */
    @PostMapping("/jobs/{jobId}/fail")
    public ResponseEntity<Void> failJob(
            @PathVariable UUID jobId,
            @RequestParam String errorMessage) {
        log.error("Training job {} failed: {}", jobId, errorMessage);
        trainingService.failJob(jobId, errorMessage);
        return ResponseEntity.ok().build();
    }
}
