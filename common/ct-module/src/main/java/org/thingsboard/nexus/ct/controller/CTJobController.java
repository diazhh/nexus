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
import org.thingsboard.nexus.ct.dto.CTJobDto;
import org.thingsboard.nexus.ct.model.CTJob;
import org.thingsboard.nexus.ct.model.JobStatus;
import org.thingsboard.nexus.ct.service.CTJobService;
import org.thingsboard.server.common.data.page.PageData;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/ct/jobs")
@RequiredArgsConstructor
@Slf4j
public class CTJobController {

    private final CTJobService jobService;

    @GetMapping("/{id}")
    public ResponseEntity<CTJobDto> getJobById(@PathVariable UUID id) {
        log.debug("REST request to get CT Job: {}", id);
        CTJobDto job = jobService.getById(id);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/number/{jobNumber}")
    public ResponseEntity<CTJobDto> getJobByJobNumber(@PathVariable String jobNumber) {
        log.debug("REST request to get CT Job by job number: {}", jobNumber);
        CTJobDto job = jobService.getByJobNumber(jobNumber);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PageData<CTJobDto>> getJobsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("REST request to get CT Jobs for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CTJobDto> jobs = jobService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(toPageData(jobs));
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<PageData<CTJobDto>> getJobsByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) String wellName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("REST request to get CT Jobs with filters");

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CTJobDto> jobs = jobService.getByFilters(tenantId, status, jobType,
                                                       unitId, wellName, pageable);
        return ResponseEntity.ok(toPageData(jobs));
    }

    @GetMapping("/tenant/{tenantId}/status/{status}")
    public ResponseEntity<List<CTJobDto>> getJobsByStatus(
            @PathVariable UUID tenantId,
            @PathVariable JobStatus status) {
        
        log.debug("REST request to get CT Jobs by status: {}", status);
        List<CTJobDto> jobs = jobService.getByStatus(tenantId, status);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/tenant/{tenantId}/active")
    public ResponseEntity<List<CTJobDto>> getActiveJobs(@PathVariable UUID tenantId) {
        log.debug("REST request to get active CT Jobs");
        List<CTJobDto> jobs = jobService.getActiveJobs(tenantId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/tenant/{tenantId}/unit/{unitId}")
    public ResponseEntity<PageData<CTJobDto>> getJobsByUnit(
            @PathVariable UUID tenantId,
            @PathVariable UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to get CT Jobs for unit: {}", unitId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("actualStartDate").descending());
        Page<CTJobDto> jobs = jobService.getJobsByUnit(tenantId, unitId, pageable);
        return ResponseEntity.ok(toPageData(jobs));
    }

    @GetMapping("/tenant/{tenantId}/reel/{reelId}")
    public ResponseEntity<PageData<CTJobDto>> getJobsByReel(
            @PathVariable UUID tenantId,
            @PathVariable UUID reelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to get CT Jobs for reel: {}", reelId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("actualStartDate").descending());
        Page<CTJobDto> jobs = jobService.getJobsByReel(tenantId, reelId, pageable);
        return ResponseEntity.ok(toPageData(jobs));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/tenant/{tenantId}/date-range")
    public ResponseEntity<List<CTJobDto>> getJobsInDateRange(
            @PathVariable UUID tenantId,
            @RequestParam Long fromDate,
            @RequestParam Long toDate) {
        
        log.debug("REST request to get CT Jobs in date range: {} to {}", fromDate, toDate);
        List<CTJobDto> jobs = jobService.getJobsInDateRange(tenantId, fromDate, toDate);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping
    public ResponseEntity<CTJobDto> createJob(@Valid @RequestBody CTJob job) {
        log.info("REST request to create CT Job: {}", job.getJobNumber());
        CTJobDto createdJob = jobService.create(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdJob);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CTJobDto> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody CTJob job) {
        
        log.info("REST request to update CT Job: {}", id);
        CTJobDto updatedJob = jobService.update(id, job);
        return ResponseEntity.ok(updatedJob);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CTJobDto> updateJobStatus(
            @PathVariable UUID id,
            @RequestParam JobStatus status,
            @RequestParam(required = false) String statusReason) {
        
        log.info("REST request to update CT Job status: {} to {}", id, status);
        CTJobDto updatedJob = jobService.updateStatus(id, status, statusReason);
        return ResponseEntity.ok(updatedJob);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<CTJobDto> startJob(@PathVariable UUID id) {
        log.info("REST request to start CT Job: {}", id);
        CTJobDto updatedJob = jobService.startJob(id);
        return ResponseEntity.ok(updatedJob);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<CTJobDto> pauseJob(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        
        log.info("REST request to pause CT Job: {}", id);
        CTJobDto updatedJob = jobService.pauseJob(id, reason);
        return ResponseEntity.ok(updatedJob);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<CTJobDto> resumeJob(@PathVariable UUID id) {
        log.info("REST request to resume CT Job: {}", id);
        CTJobDto updatedJob = jobService.resumeJob(id);
        return ResponseEntity.ok(updatedJob);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CTJobDto> completeJob(
            @PathVariable UUID id,
            @RequestParam Boolean success,
            @RequestParam Boolean objectivesAchieved,
            @RequestParam(required = false) String lessonsLearned) {
        
        log.info("REST request to complete CT Job: {}", id);
        CTJobDto updatedJob = jobService.completeJob(id, success, objectivesAchieved, lessonsLearned);
        return ResponseEntity.ok(updatedJob);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CTJobDto> cancelJob(
            @PathVariable UUID id,
            @RequestParam String reason) {
        
        log.info("REST request to cancel CT Job: {}", id);
        CTJobDto updatedJob = jobService.cancelJob(id, reason);
        return ResponseEntity.ok(updatedJob);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<CTJobDto> approveJob(
            @PathVariable UUID id,
            @RequestParam UUID approvedBy) {
        
        log.info("REST request to approve CT Job: {}", id);
        CTJobDto updatedJob = jobService.approveJob(id, approvedBy);
        return ResponseEntity.ok(updatedJob);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        log.info("REST request to delete CT Job: {}", id);
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
