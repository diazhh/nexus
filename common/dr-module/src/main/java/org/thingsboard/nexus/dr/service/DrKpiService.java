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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.dto.kpi.*;
import org.thingsboard.nexus.dr.model.DrRun;
import org.thingsboard.nexus.dr.repository.DrRunRepository;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for calculating and retrieving drilling KPIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrKpiService {

    private final DrRunRepository runRepository;

    /**
     * Get comprehensive KPIs for a drilling run.
     */
    @Transactional(readOnly = true)
    public RunKpiDto getRunKpis(UUID runId) {
        log.debug("Calculating KPIs for run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        return calculateRunKpis(run);
    }

    /**
     * Get KPIs for all runs of a rig.
     */
    @Transactional(readOnly = true)
    public List<RunKpiDto> getRigRunKpis(UUID rigId) {
        log.debug("Calculating KPIs for all runs of rig: {}", rigId);

        return runRepository.findByRigId(rigId).stream()
                .map(this::calculateRunKpis)
                .collect(Collectors.toList());
    }

    /**
     * Get aggregated KPIs for a rig.
     */
    @Transactional(readOnly = true)
    public RigKpiDto getRigKpis(UUID rigId) {
        log.debug("Calculating aggregated KPIs for rig: {}", rigId);

        List<DrRun> runs = runRepository.findByRigId(rigId);

        if (runs.isEmpty()) {
            return RigKpiDto.builder()
                    .rigId(rigId)
                    .runsInPeriod(0)
                    .build();
        }

        BigDecimal totalFootage = BigDecimal.ZERO;
        BigDecimal totalRotatingHours = BigDecimal.ZERO;
        BigDecimal totalNptHours = BigDecimal.ZERO;
        BigDecimal totalConnectionHours = BigDecimal.ZERO;
        int completedRuns = 0;

        for (DrRun run : runs) {
            if (run.getTotalFootageFt() != null) {
                totalFootage = totalFootage.add(run.getTotalFootageFt());
            }
            if (run.getTotalRotatingHours() != null) {
                totalRotatingHours = totalRotatingHours.add(run.getTotalRotatingHours());
            }
            if (run.getTotalNptHours() != null) {
                totalNptHours = totalNptHours.add(run.getTotalNptHours());
            }
            if (run.getTotalConnectionTimeHours() != null) {
                totalConnectionHours = totalConnectionHours.add(run.getTotalConnectionTimeHours());
            }
            if ("COMPLETED".equals(run.getStatus())) {
                completedRuns++;
            }
        }

        // Calculate average ROP
        BigDecimal avgRop = null;
        if (totalRotatingHours.compareTo(BigDecimal.ZERO) > 0) {
            avgRop = totalFootage.divide(totalRotatingHours, 2, RoundingMode.HALF_UP);
        }

        // Calculate NPT percentage
        BigDecimal totalOperationalHours = totalRotatingHours.add(totalNptHours).add(totalConnectionHours);
        BigDecimal nptPercent = null;
        if (totalOperationalHours.compareTo(BigDecimal.ZERO) > 0) {
            nptPercent = totalNptHours
                    .divide(totalOperationalHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return RigKpiDto.builder()
                .rigId(rigId)
                .runsInPeriod(runs.size())
                .totalWellsDrilled(completedRuns)
                .totalFootageDrilledFt(totalFootage.setScale(2, RoundingMode.HALF_UP))
                .totalDrillingHours(totalRotatingHours.setScale(2, RoundingMode.HALF_UP))
                .totalNptHours(totalNptHours.setScale(2, RoundingMode.HALF_UP))
                .avgRopFtHr(avgRop)
                .avgNptPercent(nptPercent != null ? nptPercent.setScale(2, RoundingMode.HALF_UP) : null)
                .build();
    }

    /**
     * Get KPIs for a well (all runs).
     */
    @Transactional(readOnly = true)
    public WellKpiDto getWellKpis(UUID wellId) {
        log.debug("Calculating KPIs for well: {}", wellId);

        List<DrRun> runs = runRepository.findByWellId(wellId);

        if (runs.isEmpty()) {
            return WellKpiDto.builder()
                    .wellId(wellId)
                    .totalRuns(0)
                    .build();
        }

        BigDecimal totalFootage = BigDecimal.ZERO;
        BigDecimal totalRotatingHours = BigDecimal.ZERO;
        BigDecimal totalNptHours = BigDecimal.ZERO;
        BigDecimal maxDepthMd = BigDecimal.ZERO;
        BigDecimal maxDepthTvd = BigDecimal.ZERO;
        Long spudDate = null;
        Long completionDate = null;

        for (DrRun run : runs) {
            if (run.getTotalFootageFt() != null) {
                totalFootage = totalFootage.add(run.getTotalFootageFt());
            }
            if (run.getTotalRotatingHours() != null) {
                totalRotatingHours = totalRotatingHours.add(run.getTotalRotatingHours());
            }
            if (run.getTotalNptHours() != null) {
                totalNptHours = totalNptHours.add(run.getTotalNptHours());
            }
            if (run.getEndDepthMdFt() != null && run.getEndDepthMdFt().compareTo(maxDepthMd) > 0) {
                maxDepthMd = run.getEndDepthMdFt();
            }
            if (run.getEndDepthTvdFt() != null && run.getEndDepthTvdFt().compareTo(maxDepthTvd) > 0) {
                maxDepthTvd = run.getEndDepthTvdFt();
            }
            if (spudDate == null || (run.getSpudDate() != null && run.getSpudDate() < spudDate)) {
                spudDate = run.getSpudDate();
            }
            if (run.getEndDate() != null && (completionDate == null || run.getEndDate() > completionDate)) {
                completionDate = run.getEndDate();
            }
        }

        // Calculate total drilling days
        BigDecimal totalDays = null;
        if (spudDate != null && completionDate != null) {
            long durationMs = completionDate - spudDate;
            totalDays = BigDecimal.valueOf(durationMs).divide(BigDecimal.valueOf(86400000), 2, RoundingMode.HALF_UP);
        }

        // Calculate feet per day
        BigDecimal feetPerDay = null;
        if (totalDays != null && totalDays.compareTo(BigDecimal.ZERO) > 0) {
            feetPerDay = totalFootage.divide(totalDays, 2, RoundingMode.HALF_UP);
        }

        return WellKpiDto.builder()
                .wellId(wellId)
                .totalRuns(runs.size())
                .totalFootageDrilledFt(totalFootage.setScale(2, RoundingMode.HALF_UP))
                .totalDepthFt(maxDepthMd.setScale(2, RoundingMode.HALF_UP))
                .totalTvdFt(maxDepthTvd.setScale(2, RoundingMode.HALF_UP))
                .totalDrillingHours(totalRotatingHours.setScale(2, RoundingMode.HALF_UP))
                .totalNptHours(totalNptHours.setScale(2, RoundingMode.HALF_UP))
                .totalDrillingDays(totalDays)
                .overallAvgRopFtHr(feetPerDay != null ?
                        totalFootage.divide(totalRotatingHours.compareTo(BigDecimal.ZERO) > 0 ?
                                totalRotatingHours : BigDecimal.ONE, 2, RoundingMode.HALF_UP) : null)
                .spudDate(spudDate)
                .completionDate(completionDate)
                .build();
    }

    /**
     * Get connection time analysis for a run.
     */
    @Transactional(readOnly = true)
    public ConnectionTimeKpiDto getConnectionTimeKpis(UUID runId) {
        log.debug("Calculating connection time KPIs for run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        BigDecimal connectionHours = run.getTotalConnectionTimeHours() != null ?
                run.getTotalConnectionTimeHours() : BigDecimal.ZERO;
        BigDecimal footage = run.getTotalFootageFt() != null ?
                run.getTotalFootageFt() : BigDecimal.ZERO;

        // Estimate number of connections (typically every 90 ft stand)
        BigDecimal standLength = BigDecimal.valueOf(90);
        int estimatedConnections = footage.divide(standLength, 0, RoundingMode.CEILING).intValue();

        // Average connection time in minutes
        BigDecimal avgConnectionMinutes = null;
        if (estimatedConnections > 0) {
            avgConnectionMinutes = connectionHours
                    .multiply(BigDecimal.valueOf(60))
                    .divide(BigDecimal.valueOf(estimatedConnections), 2, RoundingMode.HALF_UP);
        }

        // Connection time as percentage of total drilling time
        BigDecimal rotatingHours = run.getTotalRotatingHours() != null ?
                run.getTotalRotatingHours() : BigDecimal.ZERO;
        BigDecimal totalDrillingHours = connectionHours.add(rotatingHours);
        BigDecimal connectionPercent = null;
        if (totalDrillingHours.compareTo(BigDecimal.ZERO) > 0) {
            connectionPercent = connectionHours
                    .divide(totalDrillingHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Benchmark comparison (industry avg is typically 4-8 minutes per connection)
        String performanceRating = "UNKNOWN";
        if (avgConnectionMinutes != null) {
            if (avgConnectionMinutes.compareTo(BigDecimal.valueOf(4)) <= 0) {
                performanceRating = "EXCELLENT";
            } else if (avgConnectionMinutes.compareTo(BigDecimal.valueOf(6)) <= 0) {
                performanceRating = "GOOD";
            } else if (avgConnectionMinutes.compareTo(BigDecimal.valueOf(8)) <= 0) {
                performanceRating = "AVERAGE";
            } else {
                performanceRating = "BELOW_AVERAGE";
            }
        }

        return ConnectionTimeKpiDto.builder()
                .entityId(runId)
                .entityType("RUN")
                .totalConnectionTimeMin(connectionHours.multiply(BigDecimal.valueOf(60)).setScale(2, RoundingMode.HALF_UP))
                .totalConnections(estimatedConnections)
                .avgConnectionTimeMin(avgConnectionMinutes)
                .trend(performanceRating)
                .build();
    }

    /**
     * Get drilling efficiency metrics.
     */
    @Transactional(readOnly = true)
    public DrillingEfficiencyKpiDto getDrillingEfficiencyKpis(UUID runId) {
        log.debug("Calculating drilling efficiency KPIs for run: {}", runId);

        DrRun run = runRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        BigDecimal rotatingHours = run.getTotalRotatingHours() != null ?
                run.getTotalRotatingHours() : BigDecimal.ZERO;
        BigDecimal circulatingHours = run.getTotalCirculatingHours() != null ?
                run.getTotalCirculatingHours() : BigDecimal.ZERO;
        BigDecimal connectionHours = run.getTotalConnectionTimeHours() != null ?
                run.getTotalConnectionTimeHours() : BigDecimal.ZERO;
        BigDecimal nptHours = run.getTotalNptHours() != null ?
                run.getTotalNptHours() : BigDecimal.ZERO;

        BigDecimal totalHours = rotatingHours.add(circulatingHours).add(connectionHours).add(nptHours);

        // Calculate percentages
        BigDecimal rotatingPercent = null;
        BigDecimal circulatingPercent = null;
        BigDecimal connectionPercent = null;
        BigDecimal nptPercent = null;

        if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
            rotatingPercent = rotatingHours.divide(totalHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            circulatingPercent = circulatingHours.divide(totalHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            connectionPercent = connectionHours.divide(totalHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            nptPercent = nptHours.divide(totalHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Overall drilling efficiency (rotating time / total time)
        BigDecimal overallEfficiency = run.getDrillingEfficiencyPercent();
        if (overallEfficiency == null && totalHours.compareTo(BigDecimal.ZERO) > 0) {
            overallEfficiency = rotatingHours.divide(totalHours, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return DrillingEfficiencyKpiDto.builder()
                .entityId(runId)
                .entityType("RUN")
                .totalTimeHours(totalHours.setScale(2, RoundingMode.HALF_UP))
                .rotatingHours(rotatingHours.setScale(2, RoundingMode.HALF_UP))
                .productiveTimeHours(rotatingHours.add(circulatingHours).setScale(2, RoundingMode.HALF_UP))
                .productiveTimePercent(rotatingPercent != null ? rotatingPercent.add(circulatingPercent != null ? circulatingPercent : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP) : null)
                .nptHours(nptHours.setScale(2, RoundingMode.HALF_UP))
                .nptPercent(nptPercent != null ? nptPercent.setScale(2, RoundingMode.HALF_UP) : null)
                .overallEfficiencyPercent(overallEfficiency != null ? overallEfficiency.setScale(2, RoundingMode.HALF_UP) : null)
                .build();
    }

    /**
     * Calculate comprehensive KPIs for a single run.
     */
    private RunKpiDto calculateRunKpis(DrRun run) {
        BigDecimal footage = run.getTotalFootageFt() != null ? run.getTotalFootageFt() : BigDecimal.ZERO;
        BigDecimal rotatingHours = run.getTotalRotatingHours() != null ? run.getTotalRotatingHours() : BigDecimal.ZERO;
        BigDecimal nptHours = run.getTotalNptHours() != null ? run.getTotalNptHours() : BigDecimal.ZERO;

        // Calculate ROP if not set
        BigDecimal rop = run.getAvgRopFtHr();
        if (rop == null && rotatingHours.compareTo(BigDecimal.ZERO) > 0) {
            rop = footage.divide(rotatingHours, 2, RoundingMode.HALF_UP);
        }

        // Calculate duration
        BigDecimal durationHours = null;
        if (run.getStartDate() != null && run.getEndDate() != null) {
            long durationMs = run.getEndDate() - run.getStartDate();
            durationHours = BigDecimal.valueOf(durationMs).divide(BigDecimal.valueOf(3600000), 2, RoundingMode.HALF_UP);
        }

        // Parse run number if possible
        Integer runNumberInt = null;
        if (run.getRunNumber() != null) {
            try {
                runNumberInt = Integer.parseInt(run.getRunNumber());
            } catch (NumberFormatException e) {
                // If not a pure integer, try to extract any number
                String numStr = run.getRunNumber().replaceAll("[^0-9]", "");
                if (!numStr.isEmpty()) {
                    runNumberInt = Integer.parseInt(numStr);
                }
            }
        }

        return RunKpiDto.builder()
                .runId(run.getId())
                .runNumber(runNumberInt)
                .wellId(run.getWellId())
                .rigId(run.getRigId())
                .startDepthFt(run.getStartDepthMdFt())
                .endDepthFt(run.getEndDepthMdFt())
                .footageDrilledFt(footage.setScale(2, RoundingMode.HALF_UP))
                .avgRopFtHr(rop != null ? rop.setScale(2, RoundingMode.HALF_UP) : null)
                .rotatingHours(rotatingHours.setScale(2, RoundingMode.HALF_UP))
                .nptHours(nptHours.setScale(2, RoundingMode.HALF_UP))
                .drillingEfficiencyPercent(run.getDrillingEfficiencyPercent())
                .totalHours(durationHours)
                .startTime(run.getStartDate())
                .endTime(run.getEndDate())
                .build();
    }
}
