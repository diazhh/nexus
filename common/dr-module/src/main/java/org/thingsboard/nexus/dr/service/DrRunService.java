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
package org.thingsboard.nexus.dr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.dto.DrBhaDto;
import org.thingsboard.nexus.dr.dto.DrRigDto;
import org.thingsboard.nexus.dr.dto.DrRunDto;
import org.thingsboard.nexus.dr.exception.DrBusinessException;
import org.thingsboard.nexus.dr.exception.DrEntityNotFoundException;
import org.thingsboard.nexus.dr.model.DrRun;
import org.thingsboard.nexus.dr.model.enums.HoleSection;
import org.thingsboard.nexus.dr.model.enums.RigStatus;
import org.thingsboard.nexus.dr.model.enums.RunStatus;
import org.thingsboard.nexus.dr.repository.DrRunRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Drilling Runs (bit runs)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrRunService {

    private final DrRunRepository runRepository;
    private final DrRigService rigService;
    private final DrBhaService bhaService;

    // --- Query Operations ---

    @Transactional(readOnly = true)
    public DrRunDto getById(UUID id) {
        log.debug("Getting run by id: {}", id);
        DrRun run = runRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", id.toString()));
        return enrichDto(DrRunDto.fromEntity(run));
    }

    @Transactional(readOnly = true)
    public DrRunDto getByNumber(UUID tenantId, String runNumber) {
        log.debug("Getting run by number: {}", runNumber);
        DrRun run = runRepository.findByTenantIdAndRunNumber(tenantId, runNumber)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runNumber));
        return enrichDto(DrRunDto.fromEntity(run));
    }

    @Transactional(readOnly = true)
    public Page<DrRunDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting runs for tenant: {}", tenantId);
        Page<DrRun> runs = runRepository.findByTenantId(tenantId, pageable);
        return runs.map(r -> enrichDto(DrRunDto.fromEntity(r)));
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getAllByTenant(UUID tenantId) {
        log.debug("Getting all runs for tenant: {}", tenantId);
        List<DrRun> runs = runRepository.findByTenantId(tenantId);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getByRig(UUID rigId) {
        log.debug("Getting runs for rig: {}", rigId);
        List<DrRun> runs = runRepository.findByRigId(rigId);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DrRunDto> getByRig(UUID rigId, Pageable pageable) {
        log.debug("Getting runs for rig: {} (pageable)", rigId);
        Page<DrRun> runs = runRepository.findByRigId(rigId, pageable);
        return runs.map(r -> enrichDto(DrRunDto.fromEntity(r)));
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getByWell(UUID wellId) {
        log.debug("Getting runs for well: {}", wellId);
        List<DrRun> runs = runRepository.findByWellId(wellId);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DrRunDto> getByWell(UUID wellId, Pageable pageable) {
        log.debug("Getting runs for well: {} (pageable)", wellId);
        Page<DrRun> runs = runRepository.findByWellId(wellId, pageable);
        return runs.map(r -> enrichDto(DrRunDto.fromEntity(r)));
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getByBha(UUID bhaId) {
        log.debug("Getting runs for BHA: {}", bhaId);
        List<DrRun> runs = runRepository.findByBhaId(bhaId);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getByStatus(UUID tenantId, RunStatus status) {
        log.debug("Getting runs by status - tenant: {}, status: {}", tenantId, status);
        List<DrRun> runs = runRepository.findByTenantIdAndStatus(tenantId, status);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getActiveRuns(UUID tenantId) {
        log.debug("Getting active runs for tenant: {}", tenantId);
        List<DrRun> runs = runRepository.findActiveRuns(tenantId);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DrRunDto getCurrentRunByRig(UUID rigId) {
        log.debug("Getting current run for rig: {}", rigId);
        return runRepository.findCurrentRunByRig(rigId)
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public DrRunDto getCurrentRunByWell(UUID wellId) {
        log.debug("Getting current run for well: {}", wellId);
        return runRepository.findCurrentRunByWell(wellId)
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<DrRunDto> getByFilters(UUID tenantId, RunStatus status, UUID rigId,
                                       UUID wellId, HoleSection holeSection, Pageable pageable) {
        log.debug("Getting runs with filters - tenant: {}, status: {}", tenantId, status);
        Page<DrRun> runs = runRepository.findByFilters(tenantId, status, rigId, wellId, holeSection, pageable);
        return runs.map(r -> enrichDto(DrRunDto.fromEntity(r)));
    }

    @Transactional(readOnly = true)
    public List<DrRunDto> getByDateRange(UUID tenantId, Long startDate, Long endDate) {
        log.debug("Getting runs by date range - tenant: {}", tenantId);
        List<DrRun> runs = runRepository.findByDateRange(tenantId, startDate, endDate);
        return runs.stream()
                .map(r -> enrichDto(DrRunDto.fromEntity(r)))
                .collect(Collectors.toList());
    }

    // --- Create Operations ---

    @Transactional
    public DrRunDto create(DrRun run) {
        log.info("Creating new run: {}", run.getRunNumber());

        validateRunCreation(run);

        if (run.getCreatedTime() == null) {
            run.setCreatedTime(System.currentTimeMillis());
        }

        DrRun savedRun = runRepository.save(run);
        log.info("Run created: {}", savedRun.getId());

        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto createRun(UUID tenantId, UUID rigId, UUID wellId, String runNumber,
                              HoleSection holeSection, BigDecimal holeSizeIn,
                              BigDecimal plannedStartDepthMdFt, BigDecimal plannedEndDepthMdFt,
                              UUID createdBy) {
        log.info("Creating run {} for rig {} on well {}", runNumber, rigId, wellId);

        // Validate rig exists and is assigned to the well
        DrRigDto rig = rigService.getById(rigId);

        if (rig.getCurrentWellId() == null || !rig.getCurrentWellId().equals(wellId)) {
            throw new DrBusinessException("Rig is not assigned to the specified well");
        }

        // Check for duplicate run number
        if (runRepository.existsByTenantIdAndRunNumber(tenantId, runNumber)) {
            throw new DrBusinessException("Run number already exists: " + runNumber);
        }

        // Check for existing active run on this rig
        runRepository.findCurrentRunByRig(rigId).ifPresent(existingRun -> {
            throw new DrBusinessException("Rig already has an active run: " + existingRun.getRunNumber());
        });

        DrRun run = new DrRun();
        run.setId(UUID.randomUUID());
        run.setTenantId(tenantId);
        run.setRunNumber(runNumber);
        run.setRigId(rigId);
        run.setWellId(wellId);
        run.setHoleSection(holeSection);
        run.setHoleSizeIn(holeSizeIn);
        run.setPlannedStartDepthMdFt(plannedStartDepthMdFt);
        run.setPlannedEndDepthMdFt(plannedEndDepthMdFt);
        run.setStatus(RunStatus.PLANNED);
        run.setCreatedBy(createdBy);
        run.setCreatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(run);

        // Note: Rig's currentRunId should be updated via rigService if needed
        log.info("Run created: {} for rig {}", savedRun.getId(), rigId);

        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    // --- Update Operations ---

    @Transactional
    public DrRunDto update(UUID id, DrRun updatedRun) {
        log.info("Updating run: {}", id);

        DrRun existingRun = runRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", id.toString()));

        // Check for duplicate run number
        if (!existingRun.getRunNumber().equals(updatedRun.getRunNumber()) &&
            runRepository.existsByTenantIdAndRunNumberAndIdNot(existingRun.getTenantId(), updatedRun.getRunNumber(), id)) {
            throw new DrBusinessException("Run number already exists: " + updatedRun.getRunNumber());
        }

        // Update fields
        existingRun.setRunNumber(updatedRun.getRunNumber());
        existingRun.setHoleSection(updatedRun.getHoleSection());
        existingRun.setHoleSizeIn(updatedRun.getHoleSizeIn());
        existingRun.setCasingSizeIn(updatedRun.getCasingSizeIn());
        existingRun.setPreviousCasingShoeMdFt(updatedRun.getPreviousCasingShoeMdFt());

        // Planned depths
        existingRun.setPlannedStartDepthMdFt(updatedRun.getPlannedStartDepthMdFt());
        existingRun.setPlannedEndDepthMdFt(updatedRun.getPlannedEndDepthMdFt());
        existingRun.setPlannedStartDepthTvdFt(updatedRun.getPlannedStartDepthTvdFt());
        existingRun.setPlannedEndDepthTvdFt(updatedRun.getPlannedEndDepthTvdFt());

        // Mud properties
        existingRun.setMudType(updatedRun.getMudType());
        existingRun.setMudWeightPpg(updatedRun.getMudWeightPpg());
        existingRun.setPorePressurePpg(updatedRun.getPorePressurePpg());
        existingRun.setFracGradientPpg(updatedRun.getFracGradientPpg());

        // Metadata
        existingRun.setNotes(updatedRun.getNotes());
        existingRun.setMetadata(updatedRun.getMetadata());
        existingRun.setUpdatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(existingRun);
        log.info("Run updated: {}", savedRun.getId());

        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto startRun(UUID runId, Long startDate, BigDecimal startDepthMdFt, BigDecimal startDepthTvdFt) {
        log.info("Starting run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        if (run.getStatus() != RunStatus.PLANNED) {
            throw new DrBusinessException("Run can only be started from PLANNED status");
        }

        run.setStatus(RunStatus.IN_PROGRESS);
        run.setStartDate(startDate != null ? startDate : System.currentTimeMillis());
        run.setStartDepthMdFt(startDepthMdFt);
        run.setStartDepthTvdFt(startDepthTvdFt);
        run.setCurrentDepthMdFt(startDepthMdFt);
        run.setCurrentDepthTvdFt(startDepthTvdFt);
        run.setUpdatedTime(System.currentTimeMillis());

        // Update rig status to DRILLING
        try {
            rigService.updateStatus(run.getRigId(), RigStatus.DRILLING.name());
        } catch (DrEntityNotFoundException e) {
            log.warn("Rig not found for status update: {}", run.getRigId());
        }

        DrRun savedRun = runRepository.save(run);
        log.info("Run started: {}", runId);

        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto completeRun(UUID runId, Long endDate, BigDecimal endDepthMdFt, BigDecimal endDepthTvdFt,
                                String reasonEnded, String bitConditionOut) {
        log.info("Completing run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        if (run.getStatus() != RunStatus.IN_PROGRESS && run.getStatus() != RunStatus.ON_HOLD) {
            throw new DrBusinessException("Run can only be completed from IN_PROGRESS or ON_HOLD status");
        }

        run.setStatus(RunStatus.COMPLETED);
        run.setEndDate(endDate != null ? endDate : System.currentTimeMillis());
        run.setEndDepthMdFt(endDepthMdFt);
        run.setEndDepthTvdFt(endDepthTvdFt);
        run.setCurrentDepthMdFt(endDepthMdFt);
        run.setCurrentDepthTvdFt(endDepthTvdFt);
        run.setReasonEnded(reasonEnded);
        run.setBitConditionOut(bitConditionOut);

        // Calculate total footage
        if (run.getStartDepthMdFt() != null && endDepthMdFt != null) {
            run.setTotalFootageFt(endDepthMdFt.subtract(run.getStartDepthMdFt()));
        }

        run.setUpdatedTime(System.currentTimeMillis());

        // Update BHA statistics if assigned
        if (run.getBhaId() != null) {
            try {
                bhaService.updateStatus(run.getBhaId(), "AVAILABLE");
                // Note: BHA footage/runs statistics updates would need additional methods in DrBhaService
            } catch (DrEntityNotFoundException e) {
                log.warn("BHA not found for status update: {}", run.getBhaId());
            }
        }

        // Update rig status to STANDBY
        try {
            rigService.updateStatus(run.getRigId(), RigStatus.STANDBY.name());
            // Note: Rig statistics updates would need additional methods in DrRigService
        } catch (DrEntityNotFoundException e) {
            log.warn("Rig not found for status update: {}", run.getRigId());
        }

        DrRun savedRun = runRepository.save(run);
        log.info("Run completed: {}", runId);

        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto updateStatus(UUID runId, RunStatus newStatus) {
        log.info("Updating run {} status to {}", runId, newStatus);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        run.setStatus(newStatus);
        run.setUpdatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(run);
        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto updateCurrentDepth(UUID runId, BigDecimal currentDepthMdFt, BigDecimal currentDepthTvdFt) {
        log.debug("Updating current depth for run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        run.setCurrentDepthMdFt(currentDepthMdFt);
        run.setCurrentDepthTvdFt(currentDepthTvdFt);
        run.setUpdatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(run);
        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto assignBha(UUID runId, UUID bhaId) {
        log.info("Assigning BHA {} to run {}", bhaId, runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        DrBhaDto bha = bhaService.getById(bhaId);

        if (!"AVAILABLE".equals(bha.getStatus())) {
            throw new DrBusinessException("BHA is not available for assignment");
        }

        run.setBhaId(bhaId);
        run.setUpdatedTime(System.currentTimeMillis());

        // Update BHA status
        bhaService.updateStatus(bhaId, "IN_USE");

        DrRun savedRun = runRepository.save(run);
        log.info("BHA assigned to run successfully");

        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto assignMwdTool(UUID runId, UUID mwdToolAssetId) {
        log.info("Assigning MWD tool {} to run {}", mwdToolAssetId, runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        run.setMwdToolAssetId(mwdToolAssetId);
        run.setUpdatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(run);
        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto assignLwdTool(UUID runId, UUID lwdToolAssetId) {
        log.info("Assigning LWD tool {} to run {}", lwdToolAssetId, runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        run.setLwdToolAssetId(lwdToolAssetId);
        run.setUpdatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(run);
        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    @Transactional
    public DrRunDto updateKpis(UUID runId, BigDecimal avgRopFtHr, BigDecimal maxRopFtHr,
                               BigDecimal totalRotatingHours, BigDecimal totalSlidingHours,
                               BigDecimal totalCirculatingHours, BigDecimal totalConnectionTimeHours,
                               BigDecimal totalNptHours, Integer totalConnections) {
        log.debug("Updating KPIs for run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", runId.toString()));

        if (avgRopFtHr != null) run.setAvgRopFtHr(avgRopFtHr);
        if (maxRopFtHr != null) run.setMaxRopFtHr(maxRopFtHr);
        if (totalRotatingHours != null) run.setTotalRotatingHours(totalRotatingHours);
        if (totalSlidingHours != null) run.setTotalSlidingHours(totalSlidingHours);
        if (totalCirculatingHours != null) run.setTotalCirculatingHours(totalCirculatingHours);
        if (totalConnectionTimeHours != null) run.setTotalConnectionTimeHours(totalConnectionTimeHours);
        if (totalNptHours != null) run.setTotalNptHours(totalNptHours);
        if (totalConnections != null) run.setTotalConnections(totalConnections);

        // Calculate drilling efficiency
        BigDecimal totalDrillingHours = (totalRotatingHours != null ? totalRotatingHours : BigDecimal.ZERO)
                .add(totalSlidingHours != null ? totalSlidingHours : BigDecimal.ZERO);
        BigDecimal totalHours = totalDrillingHours
                .add(totalCirculatingHours != null ? totalCirculatingHours : BigDecimal.ZERO)
                .add(totalConnectionTimeHours != null ? totalConnectionTimeHours : BigDecimal.ZERO)
                .add(totalNptHours != null ? totalNptHours : BigDecimal.ZERO);

        if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
            run.setDrillingEfficiencyPercent(
                    totalDrillingHours.multiply(BigDecimal.valueOf(100)).divide(totalHours, 2, RoundingMode.HALF_UP)
            );
        }

        // Calculate average connection time
        if (totalConnections != null && totalConnections > 0 && totalConnectionTimeHours != null) {
            run.setAvgConnectionTimeMin(
                    totalConnectionTimeHours.multiply(BigDecimal.valueOf(60)).divide(BigDecimal.valueOf(totalConnections), 2, RoundingMode.HALF_UP)
            );
        }

        run.setUpdatedTime(System.currentTimeMillis());

        DrRun savedRun = runRepository.save(run);
        return enrichDto(DrRunDto.fromEntity(savedRun));
    }

    // --- Delete Operations ---

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting run: {}", id);

        DrRun run = runRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("Drilling Run", id.toString()));

        if (run.getStatus() == RunStatus.IN_PROGRESS) {
            throw new DrBusinessException("Cannot delete an active run. Complete or cancel it first.");
        }

        // Note: Rig's currentRunId cleanup would need additional methods in DrRigService

        runRepository.delete(run);
        log.info("Run deleted: {}", id);
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public long countByStatus(UUID tenantId, RunStatus status) {
        return runRepository.countByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public long countByRig(UUID rigId) {
        return runRepository.countByRigId(rigId);
    }

    @Transactional(readOnly = true)
    public long countByWell(UUID wellId) {
        return runRepository.countByWellId(wellId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalFootageByRig(UUID rigId) {
        return runRepository.getTotalFootageByRig(rigId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalFootageByWell(UUID wellId) {
        return runRepository.getTotalFootageByWell(wellId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageRopByRig(UUID rigId) {
        return runRepository.getAverageRopByRig(rigId);
    }

    // --- Private Helper Methods ---

    private void validateRunCreation(DrRun run) {
        if (run.getTenantId() == null) {
            throw new DrBusinessException("Tenant ID is required");
        }
        if (run.getRigId() == null) {
            throw new DrBusinessException("Rig ID is required");
        }
        if (run.getWellId() == null) {
            throw new DrBusinessException("Well ID is required");
        }
        if (run.getRunNumber() == null || run.getRunNumber().isEmpty()) {
            throw new DrBusinessException("Run number is required");
        }

        if (runRepository.existsByTenantIdAndRunNumber(run.getTenantId(), run.getRunNumber())) {
            throw new DrBusinessException("Run number already exists: " + run.getRunNumber());
        }

        // Verify rig exists
        try {
            rigService.getById(run.getRigId());
        } catch (DrEntityNotFoundException e) {
            throw new DrEntityNotFoundException("Drilling Rig", run.getRigId().toString());
        }
    }

    private DrRunDto enrichDto(DrRunDto dto) {
        if (dto == null) return null;

        // Enrich with rig info
        if (dto.getRigId() != null) {
            try {
                DrRigDto rig = rigService.getById(dto.getRigId());
                dto.setRigCode(rig.getRigCode());
                dto.setRigName(rig.getRigName());
            } catch (DrEntityNotFoundException e) {
                log.warn("Rig not found for enrichment: {}", dto.getRigId());
            }
        }

        // Enrich with BHA info
        if (dto.getBhaId() != null) {
            try {
                DrBhaDto bha = bhaService.getById(dto.getBhaId());
                dto.setBhaNumber(bha.getBhaNumber());
            } catch (DrEntityNotFoundException e) {
                log.warn("BHA not found for enrichment: {}", dto.getBhaId());
            }
        }

        return dto;
    }
}
