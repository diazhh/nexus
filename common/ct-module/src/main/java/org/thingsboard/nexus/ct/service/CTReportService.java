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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.dto.*;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.model.CTJob;
import org.thingsboard.nexus.ct.model.JobStatus;
import org.thingsboard.nexus.ct.repository.CTJobRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTReportService {

    private final CTJobRepository jobRepository;
    private final CTReelService reelService;
    private final CTUnitService unitService;

    @Transactional(readOnly = true)
    public CTReportResponse generateReport(CTReportRequest request) {
        log.info("Generating report: type={}, format={}", request.getReportType(), request.getFormat());

        try {
            byte[] content;
            String fileName;
            String contentType;

            switch (request.getReportType()) {
                case JOB_SUMMARY:
                    content = generateJobSummaryReport(request);
                    fileName = "job_summary_" + System.currentTimeMillis();
                    break;
                case REEL_LIFECYCLE:
                    content = generateReelLifecycleReport(request);
                    fileName = "reel_lifecycle_" + System.currentTimeMillis();
                    break;
                case FLEET_UTILIZATION:
                    content = generateFleetUtilizationReport(request);
                    fileName = "fleet_utilization_" + System.currentTimeMillis();
                    break;
                case FATIGUE_ANALYSIS:
                    content = generateFatigueAnalysisReport(request);
                    fileName = "fatigue_analysis_" + System.currentTimeMillis();
                    break;
                case MAINTENANCE_SCHEDULE:
                    content = generateMaintenanceScheduleReport(request);
                    fileName = "maintenance_schedule_" + System.currentTimeMillis();
                    break;
                default:
                    throw new CTBusinessException("Unsupported report type: " + request.getReportType());
            }

            if (request.getFormat() == CTReportRequest.ReportFormat.CSV) {
                fileName += ".csv";
                contentType = "text/csv";
            } else {
                fileName += ".txt";
                contentType = "text/plain";
            }

            return CTReportResponse.builder()
                    .reportId(UUID.randomUUID().toString())
                    .fileName(fileName)
                    .contentType(contentType)
                    .content(content)
                    .generatedAt(System.currentTimeMillis())
                    .reportType(request.getReportType().name())
                    .format(request.getFormat().name())
                    .build();

        } catch (Exception e) {
            log.error("Error generating report", e);
            throw new CTBusinessException("Failed to generate report: " + e.getMessage());
        }
    }

    private byte[] generateJobSummaryReport(CTReportRequest request) {
        List<CTJob> jobs;
        
        if (request.getEntityId() != null) {
            jobs = Collections.singletonList(
                jobRepository.findById(request.getEntityId())
                    .orElseThrow(() -> new CTBusinessException("Job not found"))
            );
        } else {
            Pageable pageable = PageRequest.of(0, 1000);
            Page<CTJob> page = jobRepository.findByTenantId(request.getTenantId(), pageable);
            jobs = page.getContent();
        }

        if (request.getFormat() == CTReportRequest.ReportFormat.CSV) {
            return generateJobSummaryCSV(jobs);
        } else {
            return generateJobSummaryText(jobs);
        }
    }

    private byte[] generateJobSummaryCSV(List<CTJob> jobs) {
        StringBuilder csv = new StringBuilder();
        csv.append("Job Number,Job Type,Well Name,Status,Priority,Planned Start,Planned End,Duration (hrs),Well Depth (ft)\n");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        for (CTJob job : jobs) {
            csv.append(escape(job.getJobNumber())).append(",");
            csv.append(escape(job.getJobType())).append(",");
            csv.append(escape(job.getWellName())).append(",");
            csv.append(job.getStatus().name()).append(",");
            csv.append(escape(job.getPriority())).append(",");
            csv.append(job.getPlannedStartDate() != null ? sdf.format(new Date(job.getPlannedStartDate())) : "N/A").append(",");
            csv.append(job.getPlannedEndDate() != null ? sdf.format(new Date(job.getPlannedEndDate())) : "N/A").append(",");
            csv.append(job.getEstimatedDurationHours() != null ? job.getEstimatedDurationHours().toString() : "N/A").append(",");
            csv.append(job.getWellDepthMdFt() != null ? job.getWellDepthMdFt().toString() : "N/A").append("\n");
        }
        
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateJobSummaryText(List<CTJob> jobs) {
        StringBuilder text = new StringBuilder();
        text.append("=".repeat(80)).append("\n");
        text.append("JOB SUMMARY REPORT\n");
        text.append("=".repeat(80)).append("\n");
        text.append("Generated: ").append(new Date()).append("\n");
        text.append("Total Jobs: ").append(jobs.size()).append("\n\n");
        
        for (CTJob job : jobs) {
            text.append("-".repeat(80)).append("\n");
            text.append("Job Number: ").append(job.getJobNumber()).append("\n");
            text.append("Type: ").append(job.getJobType()).append("\n");
            text.append("Well: ").append(job.getWellName()).append("\n");
            text.append("Status: ").append(job.getStatus()).append("\n");
            text.append("Priority: ").append(job.getPriority()).append("\n");
            if (job.getEstimatedDurationHours() != null) {
                text.append("Duration: ").append(job.getEstimatedDurationHours()).append(" hrs\n");
            }
            text.append("\n");
        }
        
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateReelLifecycleReport(CTReportRequest request) {
        List<CTReelDto> reels;

        if (request.getEntityId() != null) {
            reels = Collections.singletonList(reelService.getById(request.getEntityId()));
        } else {
            Pageable pageable = PageRequest.of(0, 1000);
            Page<CTReelDto> page = reelService.getByTenant(request.getTenantId(), pageable);
            reels = page.getContent();
        }

        if (request.getFormat() == CTReportRequest.ReportFormat.CSV) {
            return generateReelLifecycleCSV(reels);
        } else {
            return generateReelLifecycleText(reels);
        }
    }

    private byte[] generateReelLifecycleCSV(List<CTReelDto> reels) {
        StringBuilder csv = new StringBuilder();
        csv.append("Reel Code,Status,Material Grade,Length (ft),OD (in),Wall (in),Fatigue (%),Total Cycles,Remaining Life (%)\n");

        for (CTReelDto reel : reels) {
            csv.append(escape(reel.getReelCode())).append(",");
            csv.append(reel.getStatus().name()).append(",");
            csv.append(escape(reel.getMaterialGrade())).append(",");
            csv.append(reel.getTotalLengthFt() != null ? reel.getTotalLengthFt().toString() : "N/A").append(",");
            csv.append(reel.getTubingOdInch() != null ? reel.getTubingOdInch().toString() : "N/A").append(",");
            csv.append(reel.getWallThicknessInch() != null ? reel.getWallThicknessInch().toString() : "N/A").append(",");
            csv.append(reel.getAccumulatedFatiguePercent() != null ? reel.getAccumulatedFatiguePercent().toString() : "0").append(",");
            csv.append(reel.getTotalCycles() != null ? reel.getTotalCycles().toString() : "0").append(",");

            BigDecimal fatigue = reel.getAccumulatedFatiguePercent() != null ? reel.getAccumulatedFatiguePercent() : BigDecimal.ZERO;
            BigDecimal remainingLife = new BigDecimal("100").subtract(fatigue);
            csv.append(remainingLife.toString()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateReelLifecycleText(List<CTReelDto> reels) {
        StringBuilder text = new StringBuilder();
        text.append("=".repeat(80)).append("\n");
        text.append("REEL LIFECYCLE REPORT\n");
        text.append("=".repeat(80)).append("\n");
        text.append("Generated: ").append(new Date()).append("\n");
        text.append("Total Reels: ").append(reels.size()).append("\n\n");

        for (CTReelDto reel : reels) {
            text.append("-".repeat(80)).append("\n");
            text.append("Reel Code: ").append(reel.getReelCode()).append("\n");
            text.append("Status: ").append(reel.getStatus()).append("\n");
            text.append("Material: ").append(reel.getMaterialGrade()).append("\n");
            text.append("Fatigue: ").append(reel.getAccumulatedFatiguePercent() != null ? reel.getAccumulatedFatiguePercent() : "0").append("%\n");
            text.append("Total Cycles: ").append(reel.getTotalCycles() != null ? reel.getTotalCycles() : 0).append("\n");

            BigDecimal fatigue = reel.getAccumulatedFatiguePercent() != null ? reel.getAccumulatedFatiguePercent() : BigDecimal.ZERO;
            BigDecimal remainingLife = new BigDecimal("100").subtract(fatigue);
            text.append("Remaining Life: ").append(remainingLife).append("%\n\n");
        }

        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateFleetUtilizationReport(CTReportRequest request) {
        Pageable pageable = PageRequest.of(0, 1000);
        Page<CTUnitDto> unitsPage = unitService.getByTenant(request.getTenantId(), pageable);
        List<CTUnitDto> units = unitsPage.getContent();

        Page<CTJob> jobsPage = jobRepository.findByTenantId(request.getTenantId(), pageable);
        List<CTJob> jobs = jobsPage.getContent();

        if (request.getFormat() == CTReportRequest.ReportFormat.CSV) {
            return generateFleetUtilizationCSV(units, jobs);
        } else {
            return generateFleetUtilizationText(units, jobs);
        }
    }

    private byte[] generateFleetUtilizationCSV(List<CTUnitDto> units, List<CTJob> jobs) {
        StringBuilder csv = new StringBuilder();
        csv.append("Unit Code,Unit Name,Status,Total Hours,Jobs Completed,Location,Utilization (%)\n");

        for (CTUnitDto unit : units) {
            long completedJobs = jobs.stream()
                .filter(j -> j.getUnitId() != null && j.getUnitId().equals(unit.getAssetId()))
                .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                .count();

            BigDecimal hours = unit.getTotalOperationalHours() != null ? unit.getTotalOperationalHours() : BigDecimal.ZERO;
            double utilization = hours.doubleValue() / 720.0 * 100;

            csv.append(escape(unit.getUnitCode())).append(",");
            csv.append(escape(unit.getUnitName())).append(",");
            csv.append(unit.getOperationalStatus().name()).append(",");
            csv.append(hours.toString()).append(",");
            csv.append(completedJobs).append(",");
            csv.append(escape(unit.getCurrentLocation() != null ? unit.getCurrentLocation() : "N/A")).append(",");
            csv.append(String.format("%.2f", utilization)).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateFleetUtilizationText(List<CTUnitDto> units, List<CTJob> jobs) {
        StringBuilder text = new StringBuilder();
        text.append("=".repeat(80)).append("\n");
        text.append("FLEET UTILIZATION REPORT\n");
        text.append("=".repeat(80)).append("\n");
        text.append("Generated: ").append(new Date()).append("\n");
        text.append("Total Units: ").append(units.size()).append("\n\n");

        for (CTUnitDto unit : units) {
            long completedJobs = jobs.stream()
                .filter(j -> j.getUnitId() != null && j.getUnitId().equals(unit.getAssetId()))
                .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                .count();

            text.append("-".repeat(80)).append("\n");
            text.append("Unit Code: ").append(unit.getUnitCode()).append("\n");
            text.append("Status: ").append(unit.getOperationalStatus()).append("\n");
            text.append("Hours: ").append(unit.getTotalOperationalHours() != null ? unit.getTotalOperationalHours() : "0").append("\n");
            text.append("Jobs Completed: ").append(completedJobs).append("\n");
            text.append("Location: ").append(unit.getCurrentLocation() != null ? unit.getCurrentLocation() : "N/A").append("\n\n");
        }

        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateFatigueAnalysisReport(CTReportRequest request) {
        Pageable pageable = PageRequest.of(0, 1000);
        Page<CTReelDto> page = reelService.getByTenant(request.getTenantId(), pageable);
        List<CTReelDto> allReels = page.getContent();

        List<CTReelDto> criticalReels = allReels.stream()
            .filter(r -> r.getAccumulatedFatiguePercent() != null &&
                        r.getAccumulatedFatiguePercent().compareTo(new BigDecimal("80")) >= 0)
            .sorted((r1, r2) -> {
                BigDecimal f1 = r1.getAccumulatedFatiguePercent() != null ? r1.getAccumulatedFatiguePercent() : BigDecimal.ZERO;
                BigDecimal f2 = r2.getAccumulatedFatiguePercent() != null ? r2.getAccumulatedFatiguePercent() : BigDecimal.ZERO;
                return f2.compareTo(f1);
            })
            .collect(Collectors.toList());

        if (request.getFormat() == CTReportRequest.ReportFormat.CSV) {
            return generateFatigueAnalysisCSV(criticalReels);
        } else {
            return generateFatigueAnalysisText(criticalReels);
        }
    }

    private byte[] generateFatigueAnalysisCSV(List<CTReelDto> reels) {
        StringBuilder csv = new StringBuilder();
        csv.append("Reel Code,Fatigue Level (%),Status,Total Cycles,Remaining Life (%),Recommendation\n");

        for (CTReelDto reel : reels) {
            BigDecimal fatigue = reel.getAccumulatedFatiguePercent() != null ? reel.getAccumulatedFatiguePercent() : BigDecimal.ZERO;
            BigDecimal remainingLife = new BigDecimal("100").subtract(fatigue);
            String recommendation = fatigue.compareTo(new BigDecimal("95")) >= 0 ? "RETIRE IMMEDIATELY"
                : fatigue.compareTo(new BigDecimal("80")) >= 0 ? "SCHEDULE RETIREMENT"
                : "MONITOR";

            csv.append(escape(reel.getReelCode())).append(",");
            csv.append(fatigue.toString()).append(",");
            csv.append(reel.getStatus().name()).append(",");
            csv.append(reel.getTotalCycles() != null ? reel.getTotalCycles().toString() : "0").append(",");
            csv.append(remainingLife.toString()).append(",");
            csv.append(recommendation).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateFatigueAnalysisText(List<CTReelDto> reels) {
        StringBuilder text = new StringBuilder();
        text.append("=".repeat(80)).append("\n");
        text.append("FATIGUE ANALYSIS REPORT\n");
        text.append("=".repeat(80)).append("\n");
        text.append("Generated: ").append(new Date()).append("\n");
        text.append("Critical Reels (>80% fatigue): ").append(reels.size()).append("\n\n");

        for (CTReelDto reel : reels) {
            BigDecimal fatigue = reel.getAccumulatedFatiguePercent() != null ? reel.getAccumulatedFatiguePercent() : BigDecimal.ZERO;
            String recommendation = fatigue.compareTo(new BigDecimal("95")) >= 0 ? "RETIRE IMMEDIATELY"
                : fatigue.compareTo(new BigDecimal("80")) >= 0 ? "SCHEDULE RETIREMENT"
                : "MONITOR";

            text.append("-".repeat(80)).append("\n");
            text.append("Reel Code: ").append(reel.getReelCode()).append("\n");
            text.append("Fatigue: ").append(fatigue).append("%\n");
            text.append("Status: ").append(reel.getStatus()).append("\n");
            text.append("Recommendation: ").append(recommendation).append("\n\n");
        }

        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateMaintenanceScheduleReport(CTReportRequest request) {
        Pageable pageable = PageRequest.of(0, 1000);
        Page<CTUnitDto> page = unitService.getByTenant(request.getTenantId(), pageable);
        List<CTUnitDto> units = page.getContent();

        if (request.getFormat() == CTReportRequest.ReportFormat.CSV) {
            return generateMaintenanceScheduleCSV(units);
        } else {
            return generateMaintenanceScheduleText(units);
        }
    }

    private byte[] generateMaintenanceScheduleCSV(List<CTUnitDto> units) {
        StringBuilder csv = new StringBuilder();
        csv.append("Unit Code,Status,Total Hours,Hours Since Maintenance,Next Maintenance (hrs),Priority\n");

        for (CTUnitDto unit : units) {
            BigDecimal totalHours = unit.getTotalOperationalHours() != null ? unit.getTotalOperationalHours() : BigDecimal.ZERO;
            double hoursSinceMaintenance = totalHours.doubleValue() % 500;
            double nextMaintenance = 500 - hoursSinceMaintenance;
            String priority = nextMaintenance < 50 ? "HIGH" : nextMaintenance < 100 ? "MEDIUM" : "LOW";

            csv.append(escape(unit.getUnitCode())).append(",");
            csv.append(unit.getOperationalStatus().name()).append(",");
            csv.append(String.format("%.2f", totalHours.doubleValue())).append(",");
            csv.append(String.format("%.2f", hoursSinceMaintenance)).append(",");
            csv.append(String.format("%.2f", nextMaintenance)).append(",");
            csv.append(priority).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateMaintenanceScheduleText(List<CTUnitDto> units) {
        StringBuilder text = new StringBuilder();
        text.append("=".repeat(80)).append("\n");
        text.append("MAINTENANCE SCHEDULE REPORT\n");
        text.append("=".repeat(80)).append("\n");
        text.append("Generated: ").append(new Date()).append("\n");
        text.append("Total Units: ").append(units.size()).append("\n\n");

        for (CTUnitDto unit : units) {
            BigDecimal totalHours = unit.getTotalOperationalHours() != null ? unit.getTotalOperationalHours() : BigDecimal.ZERO;
            double hoursSinceMaintenance = totalHours.doubleValue() % 500;
            double nextMaintenance = 500 - hoursSinceMaintenance;

            text.append("-".repeat(80)).append("\n");
            text.append("Unit Code: ").append(unit.getUnitCode()).append("\n");
            text.append("Status: ").append(unit.getOperationalStatus()).append("\n");
            text.append("Total Hours: ").append(String.format("%.2f", totalHours.doubleValue())).append("\n");
            text.append("Next Maintenance: ").append(String.format("%.2f", nextMaintenance)).append(" hrs\n\n");
        }

        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
