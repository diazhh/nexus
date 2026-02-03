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
package org.thingsboard.nexus.ct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.dto.CTJobDto;
import org.thingsboard.nexus.ct.dto.CTReelDto;
import org.thingsboard.nexus.ct.dto.CTUnitDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.*;
import org.thingsboard.nexus.ct.repository.CTJobRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTJobService {

    private final CTJobRepository jobRepository;
    private final CTUnitService unitService;
    private final CTReelService reelService;

    @Transactional(readOnly = true)
    public CTJobDto getById(UUID id) {
        log.debug("Getting CT Job by id: {}", id);
        CTJob job = jobRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", id.toString()));
        return enrichJobDto(CTJobDto.fromEntity(job));
    }

    @Transactional(readOnly = true)
    public CTJobDto getByJobNumber(String jobNumber) {
        log.debug("Getting CT Job by job number: {}", jobNumber);
        CTJob job = jobRepository.findByJobNumber(jobNumber)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", jobNumber));
        return enrichJobDto(CTJobDto.fromEntity(job));
    }

    @Transactional(readOnly = true)
    public Page<CTJobDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting CT Jobs for tenant: {}", tenantId);
        Page<CTJob> jobs = jobRepository.findByTenantId(tenantId, pageable);
        return jobs.map(job -> enrichJobDto(CTJobDto.fromEntity(job)));
    }

    @Transactional(readOnly = true)
    public Page<CTJobDto> getByFilters(UUID tenantId, JobStatus status, String jobType, 
                                       UUID unitId, String wellName, Pageable pageable) {
        log.debug("Getting CT Jobs with filters - tenant: {}, status: {}, jobType: {}", 
                  tenantId, status, jobType);
        Page<CTJob> jobs = jobRepository.findByFilters(tenantId, status, jobType, unitId, 
                                                       wellName, pageable);
        return jobs.map(job -> enrichJobDto(CTJobDto.fromEntity(job)));
    }

    @Transactional(readOnly = true)
    public List<CTJobDto> getByStatus(UUID tenantId, JobStatus status) {
        log.debug("Getting CT Jobs by status - tenant: {}, status: {}", tenantId, status);
        List<CTJob> jobs = jobRepository.findByTenantIdAndStatus(tenantId, status);
        return jobs.stream()
            .map(job -> enrichJobDto(CTJobDto.fromEntity(job)))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CTJobDto> getActiveJobs(UUID tenantId) {
        log.debug("Getting active CT Jobs for tenant: {}", tenantId);
        List<CTJob> jobs = jobRepository.findActiveJobs(tenantId);
        return jobs.stream()
            .map(job -> enrichJobDto(CTJobDto.fromEntity(job)))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CTJobDto> getJobsByUnit(UUID tenantId, UUID unitId, Pageable pageable) {
        log.debug("Getting CT Jobs for unit: {}", unitId);
        Page<CTJob> jobs = jobRepository.findJobsByUnit(tenantId, unitId, pageable);
        return jobs.map(job -> enrichJobDto(CTJobDto.fromEntity(job)));
    }

    @Transactional(readOnly = true)
    public Page<CTJobDto> getJobsByReel(UUID tenantId, UUID reelId, Pageable pageable) {
        log.debug("Getting CT Jobs for reel: {}", reelId);
        Page<CTJob> jobs = jobRepository.findJobsByReel(tenantId, reelId, pageable);
        return jobs.map(job -> enrichJobDto(CTJobDto.fromEntity(job)));
    }

    @Transactional(readOnly = true)
    public List<CTJobDto> getJobsInDateRange(UUID tenantId, Long fromDate, Long toDate) {
        log.debug("Getting CT Jobs in date range: {} to {}", fromDate, toDate);
        List<CTJob> jobs = jobRepository.findJobsInDateRange(tenantId, fromDate, toDate);
        return jobs.stream()
            .map(job -> enrichJobDto(CTJobDto.fromEntity(job)))
            .collect(Collectors.toList());
    }

    @Transactional
    public CTJobDto create(CTJob job) {
        log.info("Creating new CT Job: {}", job.getJobNumber());
        
        if (jobRepository.existsByJobNumber(job.getJobNumber())) {
            throw new CTBusinessException("Job number already exists: " + job.getJobNumber());
        }
        
        validateUnitAvailability(job.getUnitId(), job.getTenantId());
        validateReelAvailability(job.getReelId(), job.getTenantId());
        
        if (job.getCreatedTime() == null) {
            job.setCreatedTime(System.currentTimeMillis());
        }
        
        CTJob savedJob = jobRepository.save(job);
        log.info("CT Job created successfully: {}", savedJob.getId());
        
        return enrichJobDto(CTJobDto.fromEntity(savedJob));
    }

    @Transactional
    public CTJobDto update(UUID id, CTJob updatedJob) {
        log.info("Updating CT Job: {}", id);
        
        CTJob existingJob = jobRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", id.toString()));
        
        if (!existingJob.getJobNumber().equals(updatedJob.getJobNumber()) &&
            jobRepository.existsByJobNumber(updatedJob.getJobNumber())) {
            throw new CTBusinessException("Job number already exists: " + updatedJob.getJobNumber());
        }
        
        existingJob.setJobName(updatedJob.getJobName());
        existingJob.setJobType(updatedJob.getJobType());
        existingJob.setPriority(updatedJob.getPriority());
        existingJob.setWellName(updatedJob.getWellName());
        existingJob.setFieldName(updatedJob.getFieldName());
        existingJob.setClientName(updatedJob.getClientName());
        existingJob.setWellDepthMdFt(updatedJob.getWellDepthMdFt());
        existingJob.setWellDepthTvdFt(updatedJob.getWellDepthTvdFt());
        existingJob.setTargetDepthFromFt(updatedJob.getTargetDepthFromFt());
        existingJob.setTargetDepthToFt(updatedJob.getTargetDepthToFt());
        existingJob.setWellheadPressurePsi(updatedJob.getWellheadPressurePsi());
        existingJob.setDescription(updatedJob.getDescription());
        existingJob.setNotes(updatedJob.getNotes());
        existingJob.setMetadata(updatedJob.getMetadata());
        existingJob.setUpdatedTime(System.currentTimeMillis());
        
        CTJob savedJob = jobRepository.save(existingJob);
        log.info("CT Job updated successfully: {}", savedJob.getId());
        
        return enrichJobDto(CTJobDto.fromEntity(savedJob));
    }

    @Transactional
    public CTJobDto updateStatus(UUID id, JobStatus newStatus, String statusReason) {
        log.info("Updating CT Job status: {} to {}", id, newStatus);
        
        CTJob job = jobRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", id.toString()));
        
        validateStatusTransition(job.getStatus(), newStatus);
        
        job.setStatus(newStatus);
        job.setStatusReason(statusReason);
        job.setUpdatedTime(System.currentTimeMillis());
        
        if (newStatus == JobStatus.IN_PROGRESS && job.getActualStartDate() == null) {
            job.setActualStartDate(System.currentTimeMillis());
            updateUnitStatusForJob(job.getUnitId(), UnitStatus.OPERATIONAL);
            updateReelStatusForJob(job.getReelId(), ReelStatus.IN_USE);
        }
        
        if (newStatus == JobStatus.COMPLETED || newStatus == JobStatus.CANCELLED || 
            newStatus == JobStatus.FAILED) {
            if (job.getActualEndDate() == null) {
                job.setActualEndDate(System.currentTimeMillis());
            }
            calculateActualDuration(job);
            releaseResources(job);
        }
        
        CTJob savedJob = jobRepository.save(job);
        log.info("CT Job status updated successfully: {}", savedJob.getId());
        
        return enrichJobDto(CTJobDto.fromEntity(savedJob));
    }

    @Transactional
    public CTJobDto startJob(UUID id) {
        log.info("Starting CT Job: {}", id);
        return updateStatus(id, JobStatus.IN_PROGRESS, "Job started");
    }

    @Transactional
    public CTJobDto pauseJob(UUID id, String reason) {
        log.info("Pausing CT Job: {}", id);
        return updateStatus(id, JobStatus.PAUSED, reason);
    }

    @Transactional
    public CTJobDto resumeJob(UUID id) {
        log.info("Resuming CT Job: {}", id);
        return updateStatus(id, JobStatus.IN_PROGRESS, "Job resumed");
    }

    @Transactional
    public CTJobDto completeJob(UUID id, Boolean success, Boolean objectivesAchieved, 
                                String lessonsLearned) {
        log.info("Completing CT Job: {}", id);
        
        CTJob job = jobRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", id.toString()));
        
        job.setJobSuccess(success);
        job.setObjectivesAchieved(objectivesAchieved);
        job.setLessonsLearned(lessonsLearned);
        
        if (job.getMetersDeployed() != null && job.getReelId() != null) {
            updateReelMetrics(job.getReelId(), job.getMetersDeployed(), job.getCyclesPerformed());
        }
        
        if (job.getActualDurationHours() != null && job.getUnitId() != null) {
            updateUnitMetrics(job.getUnitId(), job.getActualDurationHours(), job.getMetersDeployed());
        }
        
        jobRepository.save(job);
        
        return updateStatus(id, JobStatus.COMPLETED, "Job completed");
    }

    @Transactional
    public CTJobDto cancelJob(UUID id, String reason) {
        log.info("Cancelling CT Job: {}", id);
        return updateStatus(id, JobStatus.CANCELLED, reason);
    }

    @Transactional
    public CTJobDto approveJob(UUID id, UUID approvedBy) {
        log.info("Approving CT Job: {}", id);
        
        CTJob job = jobRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", id.toString()));
        
        if (job.getStatus() != JobStatus.PLANNED) {
            throw new CTBusinessException("Only PLANNED jobs can be approved");
        }
        
        job.setStatus(JobStatus.APPROVED);
        job.setApprovedBy(approvedBy);
        job.setApprovedTime(System.currentTimeMillis());
        job.setUpdatedTime(System.currentTimeMillis());
        
        CTJob savedJob = jobRepository.save(job);
        log.info("CT Job approved successfully: {}", savedJob.getId());
        
        return enrichJobDto(CTJobDto.fromEntity(savedJob));
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting CT Job: {}", id);
        
        CTJob job = jobRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Job", id.toString()));
        
        if (job.getStatus() == JobStatus.IN_PROGRESS) {
            throw new CTBusinessException("Cannot delete job in progress");
        }
        
        jobRepository.delete(job);
        log.info("CT Job deleted successfully: {}", id);
    }

    private CTJobDto enrichJobDto(CTJobDto dto) {
        if (dto.getUnitId() != null) {
            try {
                CTUnitDto unit = unitService.getById(dto.getUnitId());
                dto.setUnitCode(unit.getUnitCode());
            } catch (CTEntityNotFoundException e) {
                log.warn("Unit not found for job enrichment: {}", dto.getUnitId());
            }
        }

        if (dto.getReelId() != null) {
            try {
                CTReelDto reel = reelService.getById(dto.getReelId());
                dto.setReelCode(reel.getReelCode());
            } catch (CTEntityNotFoundException e) {
                log.warn("Reel not found for job enrichment: {}", dto.getReelId());
            }
        }

        return dto;
    }

    private void validateUnitAvailability(UUID unitId, UUID tenantId) {
        CTUnitDto unit = unitService.getById(unitId);

        if (!unit.getTenantId().equals(tenantId)) {
            throw new CTBusinessException("Unit does not belong to the tenant");
        }

        if (unit.getOperationalStatus() == UnitStatus.DECOMMISSIONED ||
            unit.getOperationalStatus() == UnitStatus.OFFLINE) {
            throw new CTBusinessException("Unit is not available for jobs: " + unit.getOperationalStatus());
        }
    }

    private void validateReelAvailability(UUID reelId, UUID tenantId) {
        CTReelDto reel = reelService.getById(reelId);

        if (!reel.getTenantId().equals(tenantId)) {
            throw new CTBusinessException("Reel does not belong to the tenant");
        }

        if (reel.getStatus() == ReelStatus.RETIRED || reel.getStatus() == ReelStatus.DAMAGED) {
            throw new CTBusinessException("Reel is not available for jobs: " + reel.getStatus());
        }

        if (reel.getAccumulatedFatiguePercent() != null &&
            reel.getAccumulatedFatiguePercent().compareTo(new BigDecimal("90.0")) >= 0) {
            log.warn("Reel {} has high fatigue: {}%", reelId, reel.getAccumulatedFatiguePercent());
        }
    }

    private void validateStatusTransition(JobStatus currentStatus, JobStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        
        switch (currentStatus) {
            case PLANNED:
                if (newStatus != JobStatus.APPROVED && newStatus != JobStatus.CANCELLED) {
                    throw new CTBusinessException("PLANNED job can only transition to APPROVED or CANCELLED");
                }
                break;
            case APPROVED:
                if (newStatus != JobStatus.IN_PROGRESS && newStatus != JobStatus.CANCELLED) {
                    throw new CTBusinessException("APPROVED job can only transition to IN_PROGRESS or CANCELLED");
                }
                break;
            case IN_PROGRESS:
                if (newStatus != JobStatus.PAUSED && newStatus != JobStatus.COMPLETED && 
                    newStatus != JobStatus.FAILED && newStatus != JobStatus.CANCELLED) {
                    throw new CTBusinessException("IN_PROGRESS job can only transition to PAUSED, COMPLETED, FAILED or CANCELLED");
                }
                break;
            case PAUSED:
                if (newStatus != JobStatus.IN_PROGRESS && newStatus != JobStatus.CANCELLED) {
                    throw new CTBusinessException("PAUSED job can only transition to IN_PROGRESS or CANCELLED");
                }
                break;
            case COMPLETED:
            case CANCELLED:
            case FAILED:
                throw new CTBusinessException("Cannot transition from terminal state: " + currentStatus);
        }
    }

    private void calculateActualDuration(CTJob job) {
        if (job.getActualStartDate() != null && job.getActualEndDate() != null) {
            long durationMillis = job.getActualEndDate() - job.getActualStartDate();
            BigDecimal durationHours = new BigDecimal(durationMillis).divide(
                new BigDecimal(3600000), 2, BigDecimal.ROUND_HALF_UP
            );
            job.setActualDurationHours(durationHours);
        }
    }

    private void updateUnitStatusForJob(UUID unitId, UnitStatus status) {
        try {
            unitService.updateStatus(unitId, status);
        } catch (CTEntityNotFoundException e) {
            log.warn("Unit not found for status update: {}", unitId);
        }
    }

    private void updateReelStatusForJob(UUID reelId, ReelStatus status) {
        try {
            reelService.updateStatus(reelId, status);
        } catch (CTEntityNotFoundException e) {
            log.warn("Reel not found for status update: {}", reelId);
        }
    }

    private void releaseResources(CTJob job) {
        updateUnitStatusForJob(job.getUnitId(), UnitStatus.STANDBY);
        updateReelStatusForJob(job.getReelId(), ReelStatus.AVAILABLE);
    }

    private void updateReelMetrics(UUID reelId, BigDecimal metersDeployed, Integer cycles) {
        try {
            CTReelDto reel = reelService.getById(reelId);
            BigDecimal newFatigue = reel.getAccumulatedFatiguePercent();

            if (cycles != null) {
                Integer currentCycles = reel.getTotalCycles() != null ? reel.getTotalCycles() : 0;
                reelService.updateFatigue(reelId, newFatigue, currentCycles + cycles);
            }
            // Note: totalMetersDeployed and totalJobsUsed updates would need additional methods in CTReelService
        } catch (CTEntityNotFoundException e) {
            log.warn("Reel not found for metrics update: {}", reelId);
        }
    }

    private void updateUnitMetrics(UUID unitId, BigDecimal durationHours, BigDecimal metersDeployed) {
        // Note: Unit metrics updates would need additional methods in CTUnitService
        // For now, just log the intended update
        log.debug("Unit {} metrics update - hours: {}, meters: {}", unitId, durationHours, metersDeployed);
    }
}
