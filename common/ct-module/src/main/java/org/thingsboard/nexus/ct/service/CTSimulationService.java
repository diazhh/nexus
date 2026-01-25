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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.nexus.ct.model.CTJob;
import org.thingsboard.nexus.ct.model.CTReel;
import org.thingsboard.nexus.ct.model.CTUnit;
import org.thingsboard.nexus.ct.repository.CTJobRepository;
import org.thingsboard.nexus.ct.repository.CTReelRepository;
import org.thingsboard.nexus.ct.repository.CTUnitRepository;
import org.thingsboard.nexus.ct.rule.CTJobSimulationNode;
import org.thingsboard.nexus.ct.rule.CTJobSimulationNode.JobParameters;
import org.thingsboard.nexus.ct.rule.CTJobSimulationNode.SimulationResult;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTSimulationService {

    private final CTJobRepository jobRepository;
    private final CTUnitRepository unitRepository;
    private final CTReelRepository reelRepository;
    private final CTJobSimulationNode simulationNode = new CTJobSimulationNode();

    @Transactional(readOnly = true)
    public SimulationResult simulateJob(UUID jobId) {
        log.info("Simulating job: {}", jobId);

        CTJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new CTEntityNotFoundException("Job", jobId.toString()));

        CTUnit unit = unitRepository.findById(job.getUnitId())
            .orElseThrow(() -> new CTEntityNotFoundException("Unit", job.getUnitId().toString()));

        CTReel reel = reelRepository.findById(job.getReelId())
            .orElseThrow(() -> new CTEntityNotFoundException("Reel", job.getReelId().toString()));

        JobParameters params = buildJobParameters(job, unit, reel);
        SimulationResult result = simulationNode.simulate(params);

        log.info("Simulation completed for job {}: feasible={}, duration={} hrs",
                 jobId, 
                 result.getFeasibility() != null && result.getFeasibility().isFeasible(),
                 result.getTimes() != null ? result.getTimes().getTotalDurationHours() : 0);

        return result;
    }

    @Transactional(readOnly = true)
    public SimulationResult simulateCustomJob(JobParameters params) {
        log.info("Simulating custom job for well: {}", params.getWellName());

        if (params.getWellName() == null || params.getWellName().isEmpty()) {
            throw new CTBusinessException("Well name is required");
        }

        if (params.getTargetDepthFt() <= 0) {
            throw new CTBusinessException("Target depth must be positive");
        }

        SimulationResult result = simulationNode.simulate(params);

        log.info("Custom simulation completed: feasible={}, duration={} hrs",
                 result.getFeasibility() != null && result.getFeasibility().isFeasible(),
                 result.getTimes() != null ? result.getTimes().getTotalDurationHours() : 0);

        return result;
    }

    private JobParameters buildJobParameters(CTJob job, CTUnit unit, CTReel reel) {
        JobParameters params = new JobParameters();

        params.setJobId(job.getId());
        params.setWellName(job.getWellName());
        params.setTargetDepthFt(job.getTargetDepthToFt() != null ? 
                                job.getTargetDepthToFt().doubleValue() : 10000.0);
        params.setWellboreDiameterInch(7.0); // Default, debería venir del job
        params.setMaxInclinationDeg(30.0); // Default, debería venir del well profile

        params.setTubingOdInch(reel.getTubingOdInch() != null ? 
                               reel.getTubingOdInch().doubleValue() : 2.375);
        params.setTubingIdInch(reel.getTubingIdInch() != null ? 
                               reel.getTubingIdInch().doubleValue() : 1.995);
        params.setTubingLengthFt(reel.getTotalLengthFt() != null ? 
                                 reel.getTotalLengthFt().doubleValue() : 20000.0);

        params.setFluidDensityPpg(8.33);
        params.setPumpRateBpm(job.getPlannedPumpRateBpm() != null ? 
                              job.getPlannedPumpRateBpm().doubleValue() : null);
        params.setMaxPressurePsi(job.getMaxPlannedPressurePsi() != null ? 
                                 job.getMaxPlannedPressurePsi() : 15000);
        params.setMaxRunningSpeedFtMin(job.getMaxPlannedSpeedFtMin() != null ? 
                                       job.getMaxPlannedSpeedFtMin() : 60.0);

        params.setUnitMaxPressurePsi(35000.0); // Default, debería venir del unit
        params.setUnitMaxTensionLbf(80000.0); // Default, debería venir del unit

        params.setEstimatedTreatmentHours(job.getEstimatedDurationHours() != null ? 
                                          job.getEstimatedDurationHours().doubleValue() : 2.0);

        return params;
    }
}
