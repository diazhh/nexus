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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.nexus.pf.dto.AlarmSeverity;
import org.thingsboard.nexus.pf.dto.AlarmStatus;
import org.thingsboard.nexus.pf.dto.OperationalLimitDto;
import org.thingsboard.nexus.pf.dto.PfAlarmDto;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.alarm.AlarmService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PfAlarmService.
 */
@ExtendWith(MockitoExtension.class)
class PfAlarmServiceTest {

    @Mock
    private AlarmService tbAlarmService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PfAlarmService alarmService;

    private UUID tenantId;
    private UUID entityId;
    private UUID alarmId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        alarmId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createOrUpdateAlarm tests")
    class CreateOrUpdateAlarmTests {

        @Test
        @DisplayName("Should create alarm successfully")
        void shouldCreateAlarmSuccessfully() {
            AlarmInfo createdAlarm = createMockAlarmInfo(alarmId, tenantId, entityId, "PF_HIGH_TEMPERATURE",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            AlarmApiCallResult result = mock(AlarmApiCallResult.class);
            when(result.isSuccessful()).thenReturn(true);
            when(result.getAlarm()).thenReturn(createdAlarm);

            when(tbAlarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class)))
                    .thenReturn(result);

            PfAlarmDto dto = alarmService.createOrUpdateAlarm(
                    tenantId, entityId, "well",
                    "HIGH_TEMPERATURE", AlarmSeverity.CRITICAL,
                    "Motor temperature exceeded limit", "motor_temp", 250.0);

            assertNotNull(dto);
            assertEquals(alarmId, dto.getId());
            assertEquals(AlarmSeverity.CRITICAL, dto.getSeverity());
            assertEquals(AlarmStatus.ACTIVE, dto.getStatus());

            ArgumentCaptor<AlarmCreateOrUpdateActiveRequest> captor =
                    ArgumentCaptor.forClass(AlarmCreateOrUpdateActiveRequest.class);
            verify(tbAlarmService).createAlarm(captor.capture());

            AlarmCreateOrUpdateActiveRequest captured = captor.getValue();
            assertEquals("PF_HIGH_TEMPERATURE", captured.getType());
            assertEquals(org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL, captured.getSeverity());
        }

        @Test
        @DisplayName("Should return null when alarm creation fails")
        void shouldReturnNullWhenAlarmCreationFails() {
            AlarmApiCallResult result = mock(AlarmApiCallResult.class);
            when(result.isSuccessful()).thenReturn(false);

            when(tbAlarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class)))
                    .thenReturn(result);

            PfAlarmDto dto = alarmService.createOrUpdateAlarm(
                    tenantId, entityId, "well",
                    "HIGH_TEMPERATURE", AlarmSeverity.CRITICAL,
                    "Motor temperature exceeded limit", "motor_temp", 250.0);

            assertNull(dto);
        }

        @Test
        @DisplayName("Should map severity HIGH to MAJOR")
        void shouldMapSeverityHighToMajor() {
            AlarmInfo createdAlarm = createMockAlarmInfo(alarmId, tenantId, entityId, "PF_PRESSURE_WARNING",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.MAJOR,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            AlarmApiCallResult result = mock(AlarmApiCallResult.class);
            when(result.isSuccessful()).thenReturn(true);
            when(result.getAlarm()).thenReturn(createdAlarm);

            when(tbAlarmService.createAlarm(any(AlarmCreateOrUpdateActiveRequest.class)))
                    .thenReturn(result);

            PfAlarmDto dto = alarmService.createOrUpdateAlarm(
                    tenantId, entityId, "well",
                    "PRESSURE_WARNING", AlarmSeverity.HIGH,
                    "Pressure warning", "pressure", 150.0);

            assertNotNull(dto);
            assertEquals(AlarmSeverity.HIGH, dto.getSeverity());

            ArgumentCaptor<AlarmCreateOrUpdateActiveRequest> captor =
                    ArgumentCaptor.forClass(AlarmCreateOrUpdateActiveRequest.class);
            verify(tbAlarmService).createAlarm(captor.capture());
            assertEquals(org.thingsboard.server.common.data.alarm.AlarmSeverity.MAJOR, captor.getValue().getSeverity());
        }
    }

    @Nested
    @DisplayName("clearAlarm tests")
    class ClearAlarmTests {

        @Test
        @DisplayName("Should clear existing alarm")
        void shouldClearExistingAlarm() {
            Alarm existingAlarm = createMockAlarm(alarmId, tenantId, entityId, "PF_HIGH_TEMPERATURE");

            when(tbAlarmService.findLatestActiveByOriginatorAndType(any(TenantId.class), any(), eq("PF_HIGH_TEMPERATURE")))
                    .thenReturn(existingAlarm);

            alarmService.clearAlarm(tenantId, entityId, "well", "HIGH_TEMPERATURE");

            verify(tbAlarmService).clearAlarm(any(TenantId.class), eq(new AlarmId(alarmId)), anyLong(), isNull(), eq(true));
        }

        @Test
        @DisplayName("Should do nothing when no alarm found")
        void shouldDoNothingWhenNoAlarmFound() {
            when(tbAlarmService.findLatestActiveByOriginatorAndType(any(TenantId.class), any(), anyString()))
                    .thenReturn(null);

            alarmService.clearAlarm(tenantId, entityId, "well", "HIGH_TEMPERATURE");

            verify(tbAlarmService, never()).clearAlarm(any(), any(), anyLong(), any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("acknowledgeAlarm tests")
    class AcknowledgeAlarmTests {

        @Test
        @DisplayName("Should acknowledge alarm successfully")
        void shouldAcknowledgeAlarmSuccessfully() {
            AlarmInfo acknowledgedAlarm = createMockAlarmInfo(alarmId, tenantId, entityId, "PF_HIGH_TEMPERATURE",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_ACK);

            AlarmApiCallResult result = mock(AlarmApiCallResult.class);
            when(result.isSuccessful()).thenReturn(true);
            when(result.getAlarm()).thenReturn(acknowledgedAlarm);

            when(tbAlarmService.acknowledgeAlarm(any(TenantId.class), any(AlarmId.class), anyLong()))
                    .thenReturn(result);

            UUID userId = UUID.randomUUID();
            PfAlarmDto dto = alarmService.acknowledgeAlarm(tenantId, alarmId, userId);

            assertNotNull(dto);
            assertEquals(AlarmStatus.ACKNOWLEDGED, dto.getStatus());
            verify(tbAlarmService).acknowledgeAlarm(any(TenantId.class), eq(new AlarmId(alarmId)), anyLong());
        }

        @Test
        @DisplayName("Should return null when acknowledge fails")
        void shouldReturnNullWhenAcknowledgeFails() {
            AlarmApiCallResult result = mock(AlarmApiCallResult.class);
            when(result.isSuccessful()).thenReturn(false);

            when(tbAlarmService.acknowledgeAlarm(any(TenantId.class), any(AlarmId.class), anyLong()))
                    .thenReturn(result);

            PfAlarmDto dto = alarmService.acknowledgeAlarm(tenantId, alarmId, UUID.randomUUID());

            assertNull(dto);
        }
    }

    @Nested
    @DisplayName("getActiveAlarms tests")
    class GetActiveAlarmsTests {

        @Test
        @DisplayName("Should get active alarms filtered by PF prefix")
        void shouldGetActiveAlarmsFilteredByPfPrefix() {
            AlarmInfo pfAlarm = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_HIGH_TEMPERATURE",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);
            AlarmInfo otherAlarm = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "OTHER_ALARM",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            PageData<AlarmInfo> pageData = new PageData<>(List.of(pfAlarm, otherAlarm), 2, 2, false);

            when(tbAlarmService.findAlarms(any(TenantId.class), any(AlarmQuery.class)))
                    .thenReturn(pageData);

            List<PfAlarmDto> alarms = alarmService.getActiveAlarms(tenantId);

            assertEquals(1, alarms.size());
            assertEquals("HIGH_TEMPERATURE", alarms.get(0).getAlarmType());
        }

        @Test
        @DisplayName("Should return empty list when no PF alarms")
        void shouldReturnEmptyListWhenNoPfAlarms() {
            AlarmInfo otherAlarm = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "OTHER_ALARM",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            PageData<AlarmInfo> pageData = new PageData<>(List.of(otherAlarm), 1, 1, false);

            when(tbAlarmService.findAlarms(any(TenantId.class), any(AlarmQuery.class)))
                    .thenReturn(pageData);

            List<PfAlarmDto> alarms = alarmService.getActiveAlarms(tenantId);

            assertTrue(alarms.isEmpty());
        }
    }

    @Nested
    @DisplayName("getActiveAlarmsForEntity tests")
    class GetActiveAlarmsForEntityTests {

        @Test
        @DisplayName("Should get active alarms for specific entity")
        void shouldGetActiveAlarmsForSpecificEntity() {
            AlarmInfo alarm1 = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_HIGH_TEMPERATURE",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);
            AlarmInfo alarm2 = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_LOW_PRESSURE",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.MAJOR,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            PageData<AlarmInfo> pageData = new PageData<>(List.of(alarm1, alarm2), 2, 2, false);

            when(tbAlarmService.findAlarms(any(TenantId.class), any(AlarmQuery.class)))
                    .thenReturn(pageData);

            List<PfAlarmDto> alarms = alarmService.getActiveAlarmsForEntity(tenantId, entityId, "well");

            assertEquals(2, alarms.size());
        }
    }

    @Nested
    @DisplayName("getAlarmsBySeverity tests")
    class GetAlarmsBySeverityTests {

        @Test
        @DisplayName("Should filter alarms by severity")
        void shouldFilterAlarmsBySeverity() {
            AlarmInfo criticalAlarm = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_CRITICAL_ALARM",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);
            AlarmInfo warningAlarm = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_WARNING_ALARM",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.WARNING,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            PageData<AlarmInfo> pageData = new PageData<>(List.of(criticalAlarm, warningAlarm), 2, 2, false);

            when(tbAlarmService.findAlarms(any(TenantId.class), any(AlarmQuery.class)))
                    .thenReturn(pageData);

            List<PfAlarmDto> alarms = alarmService.getAlarmsBySeverity(tenantId, List.of(AlarmSeverity.CRITICAL));

            assertEquals(1, alarms.size());
            assertEquals(AlarmSeverity.CRITICAL, alarms.get(0).getSeverity());
        }
    }

    @Nested
    @DisplayName("getAlarmCounts tests")
    class GetAlarmCountsTests {

        @Test
        @DisplayName("Should count alarms by severity")
        void shouldCountAlarmsBySeverity() {
            AlarmInfo critical1 = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_CRITICAL_1",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);
            AlarmInfo critical2 = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_CRITICAL_2",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);
            AlarmInfo high = createMockAlarmInfo(UUID.randomUUID(), tenantId, entityId, "PF_HIGH",
                    org.thingsboard.server.common.data.alarm.AlarmSeverity.MAJOR,
                    org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);

            PageData<AlarmInfo> pageData = new PageData<>(List.of(critical1, critical2, high), 3, 3, false);

            when(tbAlarmService.findAlarms(any(TenantId.class), any(AlarmQuery.class)))
                    .thenReturn(pageData);

            PfAlarmService.AlarmCounts counts = alarmService.getAlarmCounts(tenantId);

            assertEquals(2, counts.critical());
            assertEquals(1, counts.high());
            assertEquals(0, counts.medium());
            assertEquals(0, counts.low());
            assertEquals(3, counts.total());
        }
    }

    @Nested
    @DisplayName("saveOperationalLimit tests")
    class SaveOperationalLimitTests {

        @Test
        @DisplayName("Should return limit with warning")
        void shouldReturnLimitWithWarning() {
            OperationalLimitDto limit = OperationalLimitDto.builder()
                    .entityId(entityId)
                    .entityType("well")
                    .variableKey("temperature")
                    .highLimit(BigDecimal.valueOf(200.0))
                    .lowLimit(BigDecimal.valueOf(50.0))
                    .build();

            OperationalLimitDto result = alarmService.saveOperationalLimit(limit);

            assertNotNull(result);
            assertEquals(limit, result);
        }
    }

    @Nested
    @DisplayName("getOperationalLimits tests")
    class GetOperationalLimitsTests {

        @Test
        @DisplayName("Should return empty list with warning")
        void shouldReturnEmptyListWithWarning() {
            List<OperationalLimitDto> limits = alarmService.getOperationalLimits(entityId);

            assertTrue(limits.isEmpty());
        }
    }

    // Helper methods

    private Alarm createMockAlarm(UUID alarmId, UUID tenantId, UUID entityId, String type) {
        Alarm alarm = mock(Alarm.class);
        lenient().when(alarm.getId()).thenReturn(new AlarmId(alarmId));
        lenient().when(alarm.getTenantId()).thenReturn(TenantId.fromUUID(tenantId));
        lenient().when(alarm.getOriginator()).thenReturn(new AssetId(entityId));
        lenient().when(alarm.getType()).thenReturn(type);
        lenient().when(alarm.getSeverity()).thenReturn(org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL);
        lenient().when(alarm.getStatus()).thenReturn(org.thingsboard.server.common.data.alarm.AlarmStatus.ACTIVE_UNACK);
        lenient().when(alarm.getStartTs()).thenReturn(System.currentTimeMillis());

        ObjectNode details = objectMapper.createObjectNode();
        details.put("message", "Test alarm message");
        lenient().when(alarm.getDetails()).thenReturn(details);

        return alarm;
    }

    private AlarmInfo createMockAlarmInfo(UUID alarmId, UUID tenantId, UUID entityId, String type,
                                           org.thingsboard.server.common.data.alarm.AlarmSeverity severity,
                                           org.thingsboard.server.common.data.alarm.AlarmStatus status) {
        AlarmInfo alarm = mock(AlarmInfo.class);
        lenient().when(alarm.getId()).thenReturn(new AlarmId(alarmId));
        lenient().when(alarm.getTenantId()).thenReturn(TenantId.fromUUID(tenantId));
        lenient().when(alarm.getOriginator()).thenReturn(new AssetId(entityId));
        lenient().when(alarm.getType()).thenReturn(type);
        lenient().when(alarm.getSeverity()).thenReturn(severity);
        lenient().when(alarm.getStatus()).thenReturn(status);
        lenient().when(alarm.getStartTs()).thenReturn(System.currentTimeMillis());

        ObjectNode details = objectMapper.createObjectNode();
        details.put("message", "Test alarm message");
        lenient().when(alarm.getDetails()).thenReturn(details);

        return alarm;
    }
}
