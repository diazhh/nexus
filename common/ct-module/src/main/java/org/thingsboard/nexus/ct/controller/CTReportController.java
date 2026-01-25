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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.ct.dto.CTReportRequest;
import org.thingsboard.nexus.ct.dto.CTReportResponse;
import org.thingsboard.nexus.ct.service.CTReportService;

import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/ct/reports")
@RequiredArgsConstructor
@Slf4j
public class CTReportController {

    private final CTReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<byte[]> generateReport(@RequestBody CTReportRequest request) {
        log.info("Generating report: type={}, format={}", request.getReportType(), request.getFormat());

        CTReportResponse response = reportService.generateReport(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(response.getContentType()));
        headers.setContentDispositionFormData("attachment", response.getFileName());
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(response.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/job-summary/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<byte[]> generateJobSummary(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "CSV") String format) {
        
        CTReportRequest request = CTReportRequest.builder()
                .reportType(CTReportRequest.ReportType.JOB_SUMMARY)
                .format(CTReportRequest.ReportFormat.valueOf(format))
                .tenantId(tenantId)
                .build();

        return generateReport(request);
    }

    @GetMapping("/reel-lifecycle/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<byte[]> generateReelLifecycle(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "CSV") String format) {
        
        CTReportRequest request = CTReportRequest.builder()
                .reportType(CTReportRequest.ReportType.REEL_LIFECYCLE)
                .format(CTReportRequest.ReportFormat.valueOf(format))
                .tenantId(tenantId)
                .build();

        return generateReport(request);
    }

    @GetMapping("/fleet-utilization/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<byte[]> generateFleetUtilization(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "CSV") String format) {
        
        CTReportRequest request = CTReportRequest.builder()
                .reportType(CTReportRequest.ReportType.FLEET_UTILIZATION)
                .format(CTReportRequest.ReportFormat.valueOf(format))
                .tenantId(tenantId)
                .build();

        return generateReport(request);
    }

    @GetMapping("/fatigue-analysis/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<byte[]> generateFatigueAnalysis(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "CSV") String format) {
        
        CTReportRequest request = CTReportRequest.builder()
                .reportType(CTReportRequest.ReportType.FATIGUE_ANALYSIS)
                .format(CTReportRequest.ReportFormat.valueOf(format))
                .tenantId(tenantId)
                .build();

        return generateReport(request);
    }

    @GetMapping("/maintenance-schedule/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<byte[]> generateMaintenanceSchedule(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "CSV") String format) {
        
        CTReportRequest request = CTReportRequest.builder()
                .reportType(CTReportRequest.ReportType.MAINTENANCE_SCHEDULE)
                .format(CTReportRequest.ReportFormat.valueOf(format))
                .tenantId(tenantId)
                .build();

        return generateReport(request);
    }
}
