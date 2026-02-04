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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.nexus.pf.config.PfModuleConfiguration;
import org.thingsboard.nexus.pf.dto.DataQualityResultDto;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PfTelemetryService.
 */
@ExtendWith(MockitoExtension.class)
class PfTelemetryServiceTest {

    @Mock
    private TimeseriesService tbTimeseriesService;

    @Mock
    private PfAlarmService alarmService;

    @Mock
    private PfDataQualityService dataQualityService;

    @Mock
    private PfModuleConfiguration config;

    @InjectMocks
    private PfTelemetryService telemetryService;

    private UUID tenantId;
    private UUID entityId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        entityId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("processTelemetry tests")
    class ProcessTelemetryTests {

        @Test
        @DisplayName("Should process telemetry when quality is accepted")
        void shouldProcessTelemetryWhenQualityAccepted() {
            TelemetryDataDto data = createTelemetryData();
            DataQualityResultDto qualityResult = DataQualityResultDto.builder()
                    .overallScore(0.95)
                    .accepted(true)
                    .qualityLevel(DataQualityResultDto.QualityLevel.GOOD)
                    .build();

            when(dataQualityService.validateTelemetry(data)).thenReturn(qualityResult);
            when(config.isAlarmEvaluationEnabled()).thenReturn(true);
            when(tbTimeseriesService.save(any(), any(), anyList(), anyLong()))
                    .thenReturn(Futures.immediateFuture(null));

            DataQualityResultDto result = telemetryService.processTelemetry(tenantId, data);

            assertTrue(result.isAccepted());
            assertEquals(0.95, result.getOverallScore());
            verify(tbTimeseriesService).save(any(TenantId.class), any(EntityId.class), anyList(), anyLong());
        }

        @Test
        @DisplayName("Should reject telemetry when quality is low")
        void shouldRejectTelemetryWhenQualityLow() {
            TelemetryDataDto data = createTelemetryData();
            DataQualityResultDto qualityResult = DataQualityResultDto.builder()
                    .overallScore(0.3)
                    .accepted(false)
                    .qualityLevel(DataQualityResultDto.QualityLevel.POOR)
                    .build();

            when(dataQualityService.validateTelemetry(data)).thenReturn(qualityResult);

            DataQualityResultDto result = telemetryService.processTelemetry(tenantId, data);

            assertFalse(result.isAccepted());
            assertEquals(0.3, result.getOverallScore());
            verify(tbTimeseriesService, never()).save(any(), any(), anyList(), anyLong());
        }

        @Test
        @DisplayName("Should skip alarm evaluation when disabled")
        void shouldSkipAlarmEvaluationWhenDisabled() {
            TelemetryDataDto data = createTelemetryData();
            DataQualityResultDto qualityResult = DataQualityResultDto.builder()
                    .overallScore(0.9)
                    .accepted(true)
                    .qualityLevel(DataQualityResultDto.QualityLevel.GOOD)
                    .build();

            when(dataQualityService.validateTelemetry(data)).thenReturn(qualityResult);
            when(config.isAlarmEvaluationEnabled()).thenReturn(false);
            when(tbTimeseriesService.save(any(), any(), anyList(), anyLong()))
                    .thenReturn(Futures.immediateFuture(null));

            telemetryService.processTelemetry(tenantId, data);

            verify(config).isAlarmEvaluationEnabled();
            // Alarm evaluation should be skipped
        }
    }

    @Nested
    @DisplayName("processTelemetrySimple tests")
    class ProcessTelemetrySimpleTests {

        @Test
        @DisplayName("Should return true when accepted")
        void shouldReturnTrueWhenAccepted() {
            TelemetryDataDto data = createTelemetryData();
            DataQualityResultDto qualityResult = DataQualityResultDto.builder()
                    .overallScore(0.85)
                    .accepted(true)
                    .qualityLevel(DataQualityResultDto.QualityLevel.GOOD)
                    .build();

            when(dataQualityService.validateTelemetry(data)).thenReturn(qualityResult);
            when(config.isAlarmEvaluationEnabled()).thenReturn(false);
            when(tbTimeseriesService.save(any(), any(), anyList(), anyLong()))
                    .thenReturn(Futures.immediateFuture(null));

            boolean result = telemetryService.processTelemetrySimple(tenantId, data);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when rejected")
        void shouldReturnFalseWhenRejected() {
            TelemetryDataDto data = createTelemetryData();
            DataQualityResultDto qualityResult = DataQualityResultDto.builder()
                    .overallScore(0.2)
                    .accepted(false)
                    .qualityLevel(DataQualityResultDto.QualityLevel.POOR)
                    .build();

            when(dataQualityService.validateTelemetry(data)).thenReturn(qualityResult);

            boolean result = telemetryService.processTelemetrySimple(tenantId, data);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("processTelemetryBatch tests")
    class ProcessTelemetryBatchTests {

        @Test
        @DisplayName("Should process batch of telemetry data")
        void shouldProcessBatchOfTelemetryData() throws Exception {
            TelemetryDataDto data1 = createTelemetryData();
            TelemetryDataDto data2 = createTelemetryData();
            List<TelemetryDataDto> batch = List.of(data1, data2);

            DataQualityResultDto qualityResult = DataQualityResultDto.builder()
                    .overallScore(0.9)
                    .accepted(true)
                    .qualityLevel(DataQualityResultDto.QualityLevel.GOOD)
                    .build();

            when(dataQualityService.validateTelemetry(any())).thenReturn(qualityResult);
            when(config.isAlarmEvaluationEnabled()).thenReturn(false);
            when(tbTimeseriesService.save(any(), any(), anyList(), anyLong()))
                    .thenReturn(Futures.immediateFuture(null));

            CompletableFuture<List<DataQualityResultDto>> future =
                    telemetryService.processTelemetryBatch(tenantId, batch);

            List<DataQualityResultDto> results = future.get();

            assertEquals(2, results.size());
            assertTrue(results.get(0).isAccepted());
            assertTrue(results.get(1).isAccepted());
        }

        @Test
        @DisplayName("Should handle mixed quality results in batch")
        void shouldHandleMixedQualityResultsInBatch() throws Exception {
            TelemetryDataDto data1 = createTelemetryData();
            TelemetryDataDto data2 = createTelemetryData();
            List<TelemetryDataDto> batch = List.of(data1, data2);

            DataQualityResultDto goodResult = DataQualityResultDto.builder()
                    .overallScore(0.9)
                    .accepted(true)
                    .qualityLevel(DataQualityResultDto.QualityLevel.GOOD)
                    .build();

            DataQualityResultDto badResult = DataQualityResultDto.builder()
                    .overallScore(0.2)
                    .accepted(false)
                    .qualityLevel(DataQualityResultDto.QualityLevel.POOR)
                    .build();

            when(dataQualityService.validateTelemetry(any()))
                    .thenReturn(goodResult)
                    .thenReturn(badResult);
            when(config.isAlarmEvaluationEnabled()).thenReturn(false);
            when(tbTimeseriesService.save(any(), any(), anyList(), anyLong()))
                    .thenReturn(Futures.immediateFuture(null));

            CompletableFuture<List<DataQualityResultDto>> future =
                    telemetryService.processTelemetryBatch(tenantId, batch);

            List<DataQualityResultDto> results = future.get();

            assertEquals(2, results.size());
            // One should be accepted, one rejected
            long acceptedCount = results.stream().filter(DataQualityResultDto::isAccepted).count();
            assertEquals(1, acceptedCount);
            // Only one save call for the accepted data
            verify(tbTimeseriesService, times(1)).save(any(), any(), anyList(), anyLong());
        }
    }

    @Nested
    @DisplayName("getLatestTelemetry tests")
    class GetLatestTelemetryTests {

        @Test
        @DisplayName("Should get latest telemetry")
        void shouldGetLatestTelemetry() {
            List<TsKvEntry> entries = List.of(
                    new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 75.5)),
                    new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("pressure", 120.0))
            );

            ListenableFuture<List<TsKvEntry>> future = Futures.immediateFuture(entries);
            when(tbTimeseriesService.findAllLatest(any(TenantId.class), any(EntityId.class)))
                    .thenReturn(future);

            TelemetryDataDto result = telemetryService.getLatestTelemetry(tenantId, entityId, "well");

            assertNotNull(result);
            assertEquals(entityId, result.getEntityId());
            assertEquals(2, result.getValues().size());
            assertEquals(75.5, result.getValues().get("temperature"));
            assertEquals(120.0, result.getValues().get("pressure"));
        }

        @Test
        @DisplayName("Should return null when no telemetry found")
        void shouldReturnNullWhenNoTelemetryFound() {
            ListenableFuture<List<TsKvEntry>> future = Futures.immediateFuture(new ArrayList<>());
            when(tbTimeseriesService.findAllLatest(any(TenantId.class), any(EntityId.class)))
                    .thenReturn(future);

            TelemetryDataDto result = telemetryService.getLatestTelemetry(tenantId, entityId, "well");

            assertNull(result);
        }

        @Test
        @DisplayName("Should return null on exception")
        void shouldReturnNullOnException() {
            ListenableFuture<List<TsKvEntry>> future =
                    Futures.immediateFailedFuture(new RuntimeException("DB error"));
            when(tbTimeseriesService.findAllLatest(any(TenantId.class), any(EntityId.class)))
                    .thenReturn(future);

            TelemetryDataDto result = telemetryService.getLatestTelemetry(tenantId, entityId, "well");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getHistoricalTelemetry tests")
    class GetHistoricalTelemetryTests {

        @Test
        @DisplayName("Should get historical telemetry")
        void shouldGetHistoricalTelemetry() {
            long ts1 = System.currentTimeMillis() - 3600000;
            long ts2 = System.currentTimeMillis();

            List<TsKvEntry> entries = List.of(
                    new BasicTsKvEntry(ts1, new DoubleDataEntry("temperature", 70.0)),
                    new BasicTsKvEntry(ts2, new DoubleDataEntry("temperature", 75.5))
            );

            ListenableFuture<List<TsKvEntry>> latestFuture = Futures.immediateFuture(
                    List.of(new BasicTsKvEntry(ts2, new DoubleDataEntry("temperature", 75.5)))
            );
            ListenableFuture<List<TsKvEntry>> historyFuture = Futures.immediateFuture(entries);

            when(tbTimeseriesService.findAllLatest(any(TenantId.class), any(EntityId.class)))
                    .thenReturn(latestFuture);
            when(tbTimeseriesService.findAll(any(TenantId.class), any(EntityId.class), anyList()))
                    .thenReturn(historyFuture);

            List<TelemetryDataDto> result = telemetryService.getHistoricalTelemetry(
                    tenantId, entityId, "well", null, ts1, ts2);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when no keys available")
        void shouldReturnEmptyListWhenNoKeysAvailable() {
            ListenableFuture<List<TsKvEntry>> latestFuture = Futures.immediateFuture(new ArrayList<>());
            when(tbTimeseriesService.findAllLatest(any(TenantId.class), any(EntityId.class)))
                    .thenReturn(latestFuture);

            List<TelemetryDataDto> result = telemetryService.getHistoricalTelemetry(
                    tenantId, entityId, "well", null, 0L, System.currentTimeMillis());

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should use provided keys")
        void shouldUseProvidedKeys() {
            long ts = System.currentTimeMillis();
            List<String> keys = List.of("temperature", "pressure");

            List<TsKvEntry> entries = List.of(
                    new BasicTsKvEntry(ts, new DoubleDataEntry("temperature", 75.5)),
                    new BasicTsKvEntry(ts, new DoubleDataEntry("pressure", 120.0))
            );

            ListenableFuture<List<TsKvEntry>> historyFuture = Futures.immediateFuture(entries);
            when(tbTimeseriesService.findAll(any(TenantId.class), any(EntityId.class), anyList()))
                    .thenReturn(historyFuture);

            List<TelemetryDataDto> result = telemetryService.getHistoricalTelemetry(
                    tenantId, entityId, "well", keys, 0L, ts);

            assertFalse(result.isEmpty());
            verify(tbTimeseriesService, never()).findAllLatest(any(), any());
        }
    }

    @Nested
    @DisplayName("getAggregatedTelemetry tests")
    class GetAggregatedTelemetryTests {

        @Test
        @DisplayName("Should get aggregated telemetry")
        void shouldGetAggregatedTelemetry() {
            long ts = System.currentTimeMillis();
            List<String> keys = List.of("temperature");

            List<TsKvEntry> entries = List.of(
                    new BasicTsKvEntry(ts, new DoubleDataEntry("temperature", 72.5))
            );

            ListenableFuture<List<TsKvEntry>> future = Futures.immediateFuture(entries);
            when(tbTimeseriesService.findAll(any(TenantId.class), any(EntityId.class), anyList()))
                    .thenReturn(future);

            List<TelemetryDataDto> result = telemetryService.getAggregatedTelemetry(
                    tenantId, entityId, "well", keys,
                    ts - 3600000, ts,
                    org.thingsboard.server.common.data.kv.Aggregation.AVG,
                    3600000);

            assertFalse(result.isEmpty());
            assertEquals(72.5, result.get(0).getValues().get("temperature"));
        }

        @Test
        @DisplayName("Should return empty list on exception")
        void shouldReturnEmptyListOnException() {
            List<String> keys = List.of("temperature");

            ListenableFuture<List<TsKvEntry>> future =
                    Futures.immediateFailedFuture(new RuntimeException("Query failed"));
            when(tbTimeseriesService.findAll(any(TenantId.class), any(EntityId.class), anyList()))
                    .thenReturn(future);

            List<TelemetryDataDto> result = telemetryService.getAggregatedTelemetry(
                    tenantId, entityId, "well", keys,
                    0L, System.currentTimeMillis(),
                    org.thingsboard.server.common.data.kv.Aggregation.AVG,
                    3600000);

            assertTrue(result.isEmpty());
        }
    }

    // Helper methods

    private TelemetryDataDto createTelemetryData() {
        Map<String, Object> values = new HashMap<>();
        values.put("temperature", 75.5);
        values.put("pressure", 120.0);
        values.put("flow_rate", 500.0);

        return TelemetryDataDto.builder()
                .entityId(entityId)
                .entityType("well")
                .timestamp(System.currentTimeMillis())
                .values(values)
                .quality(1.0)
                .build();
    }
}
