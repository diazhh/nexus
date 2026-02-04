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
package org.thingsboard.nexus.pf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.AlarmSeverity;
import org.thingsboard.nexus.pf.dto.AlarmStatus;
import org.thingsboard.nexus.pf.dto.OperationalLimitDto;
import org.thingsboard.nexus.pf.dto.PfAlarmDto;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Production Facility Alarms using ThingsBoard Alarm System.
 * This is a wrapper over TB's native AlarmService following the CT/RV module pattern.
 *
 * Key changes from old architecture:
 * - Uses TB Alarm System instead of custom pf.alarm table
 * - Alarm Rules are configured in Asset Profiles
 * - Operational limits are defined as Alarm Rules in Asset Profiles
 */
@Service("pfAlarmService")
@RequiredArgsConstructor
@Slf4j
public class PfAlarmService {

    private final AlarmService tbAlarmService;
    private final ObjectMapper objectMapper;

    // Alarm type prefix for PF module alarms
    public static final String PF_ALARM_PREFIX = "PF_";

    /**
     * Creates or updates an alarm using TB Alarm System.
     * This replaces the old custom alarm creation logic.
     */
    public PfAlarmDto createOrUpdateAlarm(UUID tenantId, UUID entityId, String entityType,
                                           String alarmType, AlarmSeverity severity, String message,
                                           String key, double value) {

        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(entityId, entityType);

        String fullAlarmType = PF_ALARM_PREFIX + alarmType;

        AlarmCreateOrUpdateActiveRequest request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tbTenantId)
                .originator(tbEntityId)
                .type(fullAlarmType)
                .severity(mapToTbSeverity(severity))
                .startTs(System.currentTimeMillis())
                .details(buildAlarmDetails(key, value, message))
                .build();

        AlarmApiCallResult result = tbAlarmService.createAlarm(request);

        if (result.isSuccessful() && result.getAlarm() != null) {
            log.info("Created/updated alarm: entity={}, type={}, severity={}", entityId, fullAlarmType, severity);
            return mapToDto(result.getAlarm());
        }

        log.warn("Failed to create/update alarm: entity={}, type={}", entityId, fullAlarmType);
        return null;
    }

    /**
     * Clears an alarm (condition no longer exists).
     * Uses TB's alarm clearing mechanism.
     */
    public void clearAlarm(UUID tenantId, UUID entityId, String entityType, String alarmType) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(entityId, entityType);
        String fullAlarmType = PF_ALARM_PREFIX + alarmType;

        // Find active alarm by originator and type
        Alarm alarm = tbAlarmService.findLatestActiveByOriginatorAndType(tbTenantId, tbEntityId, fullAlarmType);

        if (alarm != null) {
            tbAlarmService.clearAlarm(tbTenantId, alarm.getId(), System.currentTimeMillis(), null, true);
            log.info("Cleared alarm: entity={}, type={}", entityId, fullAlarmType);
        }
    }

    /**
     * Acknowledges an alarm.
     */
    public PfAlarmDto acknowledgeAlarm(UUID tenantId, UUID alarmId, UUID userId) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        AlarmId tbAlarmId = new AlarmId(alarmId);

        AlarmApiCallResult result = tbAlarmService.acknowledgeAlarm(tbTenantId, tbAlarmId, System.currentTimeMillis());

        if (result.isSuccessful() && result.getAlarm() != null) {
            log.info("Acknowledged alarm: {}", alarmId);
            return mapToDto(result.getAlarm());
        }

        log.warn("Failed to acknowledge alarm: {}", alarmId);
        return null;
    }

    /**
     * Gets active alarms for a tenant.
     */
    public List<PfAlarmDto> getActiveAlarms(UUID tenantId) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);

        AlarmQuery query = AlarmQuery.builder()
                .searchStatus(AlarmSearchStatus.ACTIVE)
                .pageLink(new TimePageLink(100))
                .build();

        PageData<AlarmInfo> alarms = tbAlarmService.findAlarms(tbTenantId, query);

        return alarms.getData().stream()
                .filter(a -> a.getType().startsWith(PF_ALARM_PREFIX))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets active alarms for an entity.
     */
    public List<PfAlarmDto> getActiveAlarmsForEntity(UUID tenantId, UUID entityId, String entityType) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);
        EntityId tbEntityId = createEntityId(entityId, entityType);

        AlarmQuery query = AlarmQuery.builder()
                .affectedEntityId(tbEntityId)
                .searchStatus(AlarmSearchStatus.ACTIVE)
                .pageLink(new TimePageLink(100))
                .build();

        PageData<AlarmInfo> alarms = tbAlarmService.findAlarms(tbTenantId, query);

        return alarms.getData().stream()
                .filter(a -> a.getType().startsWith(PF_ALARM_PREFIX))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets alarms by severity.
     */
    public List<PfAlarmDto> getAlarmsBySeverity(UUID tenantId, List<AlarmSeverity> severities) {
        TenantId tbTenantId = TenantId.fromUUID(tenantId);

        List<org.thingsboard.server.common.data.alarm.AlarmSeverity> tbSeverities = severities.stream()
                .map(this::mapToTbSeverity)
                .collect(Collectors.toList());

        AlarmQuery query = AlarmQuery.builder()
                .searchStatus(AlarmSearchStatus.ACTIVE)
                .pageLink(new TimePageLink(100))
                .build();

        PageData<AlarmInfo> alarms = tbAlarmService.findAlarms(tbTenantId, query);

        return alarms.getData().stream()
                .filter(a -> a.getType().startsWith(PF_ALARM_PREFIX))
                .filter(a -> tbSeverities.contains(a.getSeverity()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets alarm counts for dashboard.
     */
    public AlarmCounts getAlarmCounts(UUID tenantId) {
        List<PfAlarmDto> activeAlarms = getActiveAlarms(tenantId);

        long critical = activeAlarms.stream().filter(a -> a.getSeverity() == AlarmSeverity.CRITICAL).count();
        long high = activeAlarms.stream().filter(a -> a.getSeverity() == AlarmSeverity.HIGH).count();
        long medium = activeAlarms.stream().filter(a -> a.getSeverity() == AlarmSeverity.MEDIUM).count();
        long low = activeAlarms.stream().filter(a -> a.getSeverity() == AlarmSeverity.LOW).count();

        return new AlarmCounts(critical, high, medium, low);
    }

    /**
     * Saves an operational limit.
     * In the new architecture, operational limits are configured as Alarm Rules in Asset Profiles.
     * This method is kept for backward compatibility but logs a warning.
     */
    public OperationalLimitDto saveOperationalLimit(OperationalLimitDto limit) {
        log.warn("saveOperationalLimit called - operational limits should be configured as Alarm Rules in Asset Profiles");
        // In the new architecture, operational limits are managed through Asset Profile alarm rules
        // This is a no-op for backward compatibility
        return limit;
    }

    /**
     * Gets operational limits for an entity.
     * In the new architecture, operational limits are configured as Alarm Rules in Asset Profiles.
     */
    public List<OperationalLimitDto> getOperationalLimits(UUID entityId) {
        log.warn("getOperationalLimits called - operational limits should be retrieved from Asset Profile alarm rules");
        // In the new architecture, operational limits are managed through Asset Profile alarm rules
        return new ArrayList<>();
    }

    // Helper methods

    private ObjectNode buildAlarmDetails(String key, double value, String message) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("variable", key);
        details.put("value", value);
        details.put("message", message);
        details.put("timestamp", System.currentTimeMillis());
        details.put("source", "pf_module");
        return details;
    }

    private EntityId createEntityId(UUID id, String type) {
        if (type == null) {
            return new AssetId(id);
        }

        return switch (type.toLowerCase()) {
            case "pf_well", "well" -> new AssetId(id);
            case "pf_wellpad", "wellpad" -> new AssetId(id);
            case "pf_esp_system", "esp" -> new AssetId(id);
            case "pf_pcp_system", "pcp" -> new AssetId(id);
            case "pf_gas_lift_system", "gas_lift" -> new AssetId(id);
            case "pf_rod_pump_system", "rod_pump" -> new AssetId(id);
            case "device" -> new DeviceId(id);
            default -> new AssetId(id);
        };
    }

    private org.thingsboard.server.common.data.alarm.AlarmSeverity mapToTbSeverity(AlarmSeverity severity) {
        return switch (severity) {
            case CRITICAL -> org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL;
            case HIGH -> org.thingsboard.server.common.data.alarm.AlarmSeverity.MAJOR;
            case MEDIUM -> org.thingsboard.server.common.data.alarm.AlarmSeverity.MINOR;
            case LOW -> org.thingsboard.server.common.data.alarm.AlarmSeverity.WARNING;
            case INFO -> org.thingsboard.server.common.data.alarm.AlarmSeverity.INDETERMINATE;
        };
    }

    private AlarmSeverity mapFromTbSeverity(org.thingsboard.server.common.data.alarm.AlarmSeverity severity) {
        return switch (severity) {
            case CRITICAL -> AlarmSeverity.CRITICAL;
            case MAJOR -> AlarmSeverity.HIGH;
            case MINOR -> AlarmSeverity.MEDIUM;
            case WARNING -> AlarmSeverity.LOW;
            default -> AlarmSeverity.LOW;
        };
    }

    private AlarmStatus mapFromTbStatus(org.thingsboard.server.common.data.alarm.AlarmStatus status) {
        return switch (status) {
            case ACTIVE_UNACK -> AlarmStatus.ACTIVE;
            case ACTIVE_ACK -> AlarmStatus.ACKNOWLEDGED;
            case CLEARED_UNACK, CLEARED_ACK -> AlarmStatus.CLEARED;
        };
    }

    private PfAlarmDto mapToDto(Alarm alarm) {
        return PfAlarmDto.builder()
                .id(alarm.getId().getId())
                .tenantId(alarm.getTenantId().getId())
                .entityId(alarm.getOriginator().getId())
                .entityType(alarm.getOriginator().getEntityType().name())
                .alarmType(alarm.getType().replace(PF_ALARM_PREFIX, ""))
                .severity(mapFromTbSeverity(alarm.getSeverity()))
                .status(mapFromTbStatus(alarm.getStatus()))
                .message(alarm.getDetails() != null && alarm.getDetails().has("message")
                        ? alarm.getDetails().get("message").asText() : null)
                .details(alarm.getDetails())
                .startTime(alarm.getStartTs())
                .endTime(alarm.getEndTs())
                .acknowledgedTime(alarm.getAckTs())
                .clearedTime(alarm.getClearTs())
                .build();
    }

    private PfAlarmDto mapToDto(AlarmInfo alarm) {
        return PfAlarmDto.builder()
                .id(alarm.getId().getId())
                .tenantId(alarm.getTenantId().getId())
                .entityId(alarm.getOriginator().getId())
                .entityType(alarm.getOriginator().getEntityType().name())
                .alarmType(alarm.getType().replace(PF_ALARM_PREFIX, ""))
                .severity(mapFromTbSeverity(alarm.getSeverity()))
                .status(mapFromTbStatus(alarm.getStatus()))
                .message(alarm.getDetails() != null && alarm.getDetails().has("message")
                        ? alarm.getDetails().get("message").asText() : null)
                .details(alarm.getDetails())
                .startTime(alarm.getStartTs())
                .endTime(alarm.getEndTs())
                .acknowledgedTime(alarm.getAckTs())
                .clearedTime(alarm.getClearTs())
                .build();
    }

    /**
     * Record class for alarm counts.
     */
    public record AlarmCounts(long critical, long high, long medium, long low) {
        public long total() {
            return critical + high + medium + low;
        }
    }
}
