/*
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
package org.thingsboard.nexus.pf.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.pf.dto.AlarmSeverity;
import org.thingsboard.nexus.pf.dto.OperationalLimitDto;
import org.thingsboard.nexus.pf.dto.PfAlarmDto;
import org.thingsboard.nexus.pf.service.PfAlarmService;
import org.thingsboard.nexus.pf.service.PfAlarmService.AlarmCounts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Production Facility Alarms.
 */
@RestController
@RequestMapping("/api/nexus/pf/alarms")
@RequiredArgsConstructor
@Slf4j
public class PfAlarmController {

    private final PfAlarmService alarmService;

    /**
     * Gets active alarms for a tenant.
     */
    @GetMapping("/active")
    public ResponseEntity<List<PfAlarmDto>> getActiveAlarms(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(required = false) String severity) {

        if (severity != null && !severity.isEmpty()) {
            List<AlarmSeverity> severities = Arrays.stream(severity.split(","))
                    .map(AlarmSeverity::valueOf)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(alarmService.getAlarmsBySeverity(tenantId, severities));
        }

        return ResponseEntity.ok(alarmService.getActiveAlarms(tenantId));
    }

    /**
     * Gets active alarms for a specific entity.
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<PfAlarmDto>> getAlarmsForEntity(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID entityId,
            @RequestParam(required = false, defaultValue = "asset") String entityType) {
        return ResponseEntity.ok(alarmService.getActiveAlarmsForEntity(tenantId, entityId, entityType));
    }

    /**
     * Gets all active alarms (same as /active but at root path for convenience).
     */
    @GetMapping
    public ResponseEntity<List<PfAlarmDto>> getAlarms(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(alarmService.getActiveAlarms(tenantId));
    }

    /**
     * Acknowledges an alarm.
     */
    @PostMapping("/{alarmId}/acknowledge")
    public ResponseEntity<PfAlarmDto> acknowledgeAlarm(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID alarmId,
            @RequestBody(required = false) Map<String, String> body) {

        PfAlarmDto result = alarmService.acknowledgeAlarm(tenantId, alarmId, userId);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Gets alarm counts for dashboard.
     */
    @GetMapping("/counts")
    public ResponseEntity<AlarmCounts> getAlarmCounts(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(alarmService.getAlarmCounts(tenantId));
    }

    // Operational Limits endpoints
    // Note: In the new architecture, operational limits are configured as Alarm Rules in Asset Profiles.
    // These endpoints are kept for backward compatibility.

    /**
     * Creates or updates an operational limit.
     * @deprecated Use Asset Profile Alarm Rules instead
     */
    @PostMapping("/limits")
    @Deprecated
    public ResponseEntity<OperationalLimitDto> saveOperationalLimit(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody OperationalLimitDto dto) {
        log.warn("saveOperationalLimit endpoint called - operational limits should be configured in Asset Profiles");
        return ResponseEntity.ok(alarmService.saveOperationalLimit(dto));
    }

    /**
     * Gets operational limits for an entity.
     * @deprecated Use Asset Profile Alarm Rules instead
     */
    @GetMapping("/limits/{entityId}")
    @Deprecated
    public ResponseEntity<List<OperationalLimitDto>> getOperationalLimits(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID entityId) {
        log.warn("getOperationalLimits endpoint called - operational limits should be retrieved from Asset Profiles");
        return ResponseEntity.ok(alarmService.getOperationalLimits(entityId));
    }
}
