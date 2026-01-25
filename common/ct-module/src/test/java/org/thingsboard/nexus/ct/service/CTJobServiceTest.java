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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.thingsboard.nexus.ct.dto.CTJobDto;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.*;
import org.thingsboard.nexus.ct.repository.CTJobRepository;
import org.thingsboard.nexus.ct.repository.CTReelRepository;
import org.thingsboard.nexus.ct.repository.CTUnitRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CTJobServiceTest {

    @Mock
    private CTJobRepository jobRepository;

    @Mock
    private CTUnitRepository unitRepository;

    @Mock
    private CTReelRepository reelRepository;

    @InjectMocks
    private CTJobService jobService;

    private UUID tenantId;
    private UUID jobId;
    private UUID unitId;
    private UUID reelId;
    private CTJob testJob;
    private CTUnit testUnit;
    private CTReel testReel;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        unitId = UUID.randomUUID();
        reelId = UUID.randomUUID();

        testJob = new CTJob();
        testJob.setId(jobId);
        testJob.setTenantId(tenantId);
        testJob.setJobNumber("JOB-001");
        testJob.setJobType("DRILLING");
        testJob.setStatus(JobStatus.PLANNED);
        testJob.setUnitId(unitId);
        testJob.setReelId(reelId);
        testJob.setWellName("Test Well");
        testJob.setCreatedTime(System.currentTimeMillis());

        testUnit = new CTUnit();
        testUnit.setId(unitId);
        testUnit.setTenantId(tenantId);
        testUnit.setUnitCode("CT-001");
        testUnit.setOperationalStatus(UnitStatus.OPERATIONAL);

        testReel = new CTReel();
        testReel.setId(reelId);
        testReel.setTenantId(tenantId);
        testReel.setReelCode("REEL-001");
        testReel.setStatus(ReelStatus.AVAILABLE);
    }

    @Test
    void testGetById_Success() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        CTJobDto result = jobService.getById(jobId);

        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertEquals("JOB-001", result.getJobNumber());
        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    void testGetById_NotFound() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(CTEntityNotFoundException.class, () -> jobService.getById(jobId));
        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    void testGetByJobNumber_Success() {
        when(jobRepository.findByJobNumber("JOB-001")).thenReturn(Optional.of(testJob));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        CTJobDto result = jobService.getByJobNumber("JOB-001");

        assertNotNull(result);
        assertEquals("JOB-001", result.getJobNumber());
        verify(jobRepository, times(1)).findByJobNumber("JOB-001");
    }

    @Test
    void testGetByTenant_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CTJob> jobs = Arrays.asList(testJob);
        Page<CTJob> page = new PageImpl<>(jobs, pageable, jobs.size());

        when(jobRepository.findByTenantId(tenantId, pageable)).thenReturn(page);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        Page<CTJobDto> result = jobService.getByTenant(tenantId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(jobRepository, times(1)).findByTenantId(tenantId, pageable);
    }

    @Test
    void testGetByStatus_Success() {
        List<CTJob> jobs = Arrays.asList(testJob);
        when(jobRepository.findByTenantIdAndStatus(tenantId, JobStatus.PLANNED)).thenReturn(jobs);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        List<CTJobDto> result = jobService.getByStatus(tenantId, JobStatus.PLANNED);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(JobStatus.PLANNED, result.get(0).getStatus());
        verify(jobRepository, times(1)).findByTenantIdAndStatus(tenantId, JobStatus.PLANNED);
    }

    @Test
    void testCreate_Success() {
        when(jobRepository.existsByJobNumber("JOB-001")).thenReturn(false);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(jobRepository.save(any(CTJob.class))).thenReturn(testJob);

        CTJobDto result = jobService.create(testJob);

        assertNotNull(result);
        assertEquals("JOB-001", result.getJobNumber());
        verify(jobRepository, times(1)).existsByJobNumber("JOB-001");
        verify(jobRepository, times(1)).save(any(CTJob.class));
    }

    @Test
    void testCreate_DuplicateJobNumber() {
        when(jobRepository.existsByJobNumber("JOB-001")).thenReturn(true);

        assertThrows(CTBusinessException.class, () -> jobService.create(testJob));
        verify(jobRepository, times(1)).existsByJobNumber("JOB-001");
        verify(jobRepository, never()).save(any(CTJob.class));
    }

    @Test
    void testStartJob_Success() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(jobRepository.save(any(CTJob.class))).thenReturn(testJob);

        CTJobDto result = jobService.startJob(jobId);

        assertNotNull(result);
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, times(1)).save(any(CTJob.class));
    }

    @Test
    void testStartJob_InvalidStatus() {
        testJob.setStatus(JobStatus.COMPLETED);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        assertThrows(CTBusinessException.class, () -> jobService.startJob(jobId));
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, never()).save(any(CTJob.class));
    }

    @Test
    void testCompleteJob_Success() {
        testJob.setStatus(JobStatus.IN_PROGRESS);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(jobRepository.save(any(CTJob.class))).thenReturn(testJob);

        CTJobDto result = jobService.completeJob(jobId, true, true, "Job completed successfully");

        assertNotNull(result);
        verify(jobRepository, atLeast(1)).findById(jobId);
        verify(jobRepository, atLeast(1)).save(any(CTJob.class));
    }

    @Test
    void testCancelJob_Success() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));
        when(jobRepository.save(any(CTJob.class))).thenReturn(testJob);

        CTJobDto result = jobService.cancelJob(jobId, "Test cancellation");

        assertNotNull(result);
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, times(1)).save(any(CTJob.class));
    }

    @Test
    void testGetActiveJobs_Success() {
        List<CTJob> jobs = Arrays.asList(testJob);
        when(jobRepository.findActiveJobs(tenantId)).thenReturn(jobs);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(testUnit));
        when(reelRepository.findById(reelId)).thenReturn(Optional.of(testReel));

        List<CTJobDto> result = jobService.getActiveJobs(tenantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jobRepository, times(1)).findActiveJobs(tenantId);
    }

    @Test
    void testDelete_Success() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        doNothing().when(jobRepository).delete(testJob);

        assertDoesNotThrow(() -> jobService.delete(jobId));
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, times(1)).delete(testJob);
    }

    @Test
    void testDelete_JobInProgress() {
        testJob.setStatus(JobStatus.IN_PROGRESS);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        assertThrows(CTBusinessException.class, () -> jobService.delete(jobId));
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, never()).delete(any(CTJob.class));
    }
}
