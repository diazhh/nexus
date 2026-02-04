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
package org.thingsboard.nexus.po.service.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.po.dto.ml.PoMlTrainingJobDto;
import org.thingsboard.nexus.po.dto.ml.StartTrainingRequest;
import org.thingsboard.nexus.po.dto.ml.TrainingJobStatus;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.model.PoMlTrainingJob;
import org.thingsboard.nexus.po.repository.PoMlTrainingJobRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing ML training jobs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PoMlTrainingService {

    private final PoMlTrainingJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    // Maximum concurrent training jobs per tenant
    private static final int MAX_CONCURRENT_JOBS = 2;

    /**
     * Get training jobs for a tenant with pagination.
     */
    public Page<PoMlTrainingJobDto> getTrainingJobs(UUID tenantId, Pageable pageable) {
        return jobRepository.findByTenantIdOrderByCreatedTimeDesc(tenantId, pageable)
                .map(this::toDto);
    }

    /**
     * Get a training job by ID.
     */
    public PoMlTrainingJobDto getTrainingJob(UUID jobId) {
        PoMlTrainingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new PoEntityNotFoundException("TrainingJob", jobId));
        return toDto(job);
    }

    /**
     * Get running jobs for a tenant.
     */
    public List<PoMlTrainingJobDto> getRunningJobs(UUID tenantId) {
        return jobRepository.findRunningJobs(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Start a new training job.
     */
    @Transactional
    public PoMlTrainingJobDto startTrainingJob(UUID tenantId, UUID userId, StartTrainingRequest request) {
        // Check concurrent job limit
        long runningCount = jobRepository.countRunningJobs(tenantId);
        if (runningCount >= MAX_CONCURRENT_JOBS) {
            throw new IllegalStateException("Maximum concurrent training jobs reached (" + MAX_CONCURRENT_JOBS + "). Please wait for existing jobs to complete.");
        }

        PoMlTrainingJob job = PoMlTrainingJob.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .modelName(request.getModelName())
                .status(TrainingJobStatus.PENDING)
                .dataStartDate(request.getDataStartDate())
                .dataEndDate(request.getDataEndDate())
                .progressPercent(0)
                .createdTime(System.currentTimeMillis())
                .createdBy(userId)
                .build();

        // Set hyperparameters if provided
        if (request.getHyperparameters() != null) {
            job.setHyperparameters(objectMapper.valueToTree(request.getHyperparameters()));
        }

        PoMlTrainingJob saved = jobRepository.save(job);
        log.info("Created training job {} for model {} by user {}", saved.getId(), request.getModelName(), userId);

        return toDto(saved);
    }

    /**
     * Update job progress (called by Python ML service).
     */
    @Transactional
    public void updateJobProgress(UUID jobId, int progressPercent, Integer currentEpoch, Integer totalEpochs, String currentStep) {
        PoMlTrainingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new PoEntityNotFoundException("TrainingJob", jobId));

        if (job.getStatus() == TrainingJobStatus.CANCELLED) {
            log.warn("Attempting to update cancelled job {}", jobId);
            return;
        }

        // Mark as running if it was pending
        if (job.getStatus() == TrainingJobStatus.PENDING) {
            job.setStatus(TrainingJobStatus.RUNNING);
            job.setStartedTime(System.currentTimeMillis());
        }

        job.setProgressPercent(progressPercent);
        job.setCurrentEpoch(currentEpoch);
        job.setTotalEpochs(totalEpochs);
        job.setCurrentStep(currentStep);

        jobRepository.save(job);
        log.debug("Updated job {} progress: {}% (epoch {}/{})", jobId, progressPercent, currentEpoch, totalEpochs);
    }

    /**
     * Mark job as completed.
     */
    @Transactional
    public void completeJob(UUID jobId, UUID resultModelId) {
        PoMlTrainingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new PoEntityNotFoundException("TrainingJob", jobId));

        job.setStatus(TrainingJobStatus.COMPLETED);
        job.setProgressPercent(100);
        job.setCompletedTime(System.currentTimeMillis());
        job.setResultModelId(resultModelId);

        jobRepository.save(job);
        log.info("Training job {} completed with model {}", jobId, resultModelId);
    }

    /**
     * Mark job as failed.
     */
    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        PoMlTrainingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new PoEntityNotFoundException("TrainingJob", jobId));

        job.setStatus(TrainingJobStatus.FAILED);
        job.setCompletedTime(System.currentTimeMillis());
        job.setErrorMessage(errorMessage);

        jobRepository.save(job);
        log.error("Training job {} failed: {}", jobId, errorMessage);
    }

    /**
     * Cancel a training job.
     */
    @Transactional
    public PoMlTrainingJobDto cancelJob(UUID jobId) {
        PoMlTrainingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new PoEntityNotFoundException("TrainingJob", jobId));

        if (job.getStatus() == TrainingJobStatus.COMPLETED || job.getStatus() == TrainingJobStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a job that has already completed or failed");
        }

        job.setStatus(TrainingJobStatus.CANCELLED);
        job.setCompletedTime(System.currentTimeMillis());

        PoMlTrainingJob saved = jobRepository.save(job);
        log.info("Training job {} cancelled", jobId);

        return toDto(saved);
    }

    /**
     * Find and mark stale jobs as failed.
     * Called by a scheduled task.
     */
    @Transactional
    public int cleanupStaleJobs(long maxRunningTimeMs) {
        long cutoffTime = System.currentTimeMillis() - maxRunningTimeMs;
        List<PoMlTrainingJob> staleJobs = jobRepository.findStaleJobs(cutoffTime);

        for (PoMlTrainingJob job : staleJobs) {
            job.setStatus(TrainingJobStatus.FAILED);
            job.setCompletedTime(System.currentTimeMillis());
            job.setErrorMessage("Job timed out after " + (maxRunningTimeMs / 1000 / 60) + " minutes");
            jobRepository.save(job);
            log.warn("Marked stale job {} as failed", job.getId());
        }

        return staleJobs.size();
    }

    /**
     * Get all running jobs (for scheduler).
     */
    public List<PoMlTrainingJob> getAllRunningJobs() {
        return jobRepository.findAllRunningJobs();
    }

    private PoMlTrainingJobDto toDto(PoMlTrainingJob job) {
        return PoMlTrainingJobDto.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .dataStartDate(job.getDataStartDate())
                .dataEndDate(job.getDataEndDate())
                .hyperparameters(job.getHyperparameters())
                .progressPercent(job.getProgressPercent())
                .currentEpoch(job.getCurrentEpoch())
                .totalEpochs(job.getTotalEpochs())
                .currentStep(job.getCurrentStep())
                .resultModelId(job.getResultModelId())
                .errorMessage(job.getErrorMessage())
                .startedTime(job.getStartedTime())
                .completedTime(job.getCompletedTime())
                .createdTime(job.getCreatedTime())
                .createdBy(job.getCreatedBy())
                .build();
    }
}
