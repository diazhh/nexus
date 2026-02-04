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
package org.thingsboard.nexus.pf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.pf.dto.DataQualityResultDto;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.nexus.pf.service.PfTelemetryService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Production Facility Telemetry.
 */
@RestController
@RequestMapping("/api/nexus/pf")
@RequiredArgsConstructor
@Slf4j
public class PfTelemetryController {

    private final PfTelemetryService telemetryService;

    /**
     * Ingests telemetry data with data quality validation.
     * Returns detailed quality assessment including any validation issues.
     */
    @PostMapping("/telemetry")
    public ResponseEntity<DataQualityResultDto> ingestTelemetry(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody TelemetryDataDto data) {
        log.debug("Ingesting telemetry for entity: {}", data.getEntityId());
        DataQualityResultDto result = telemetryService.processTelemetry(tenantId, data);
        return ResponseEntity.ok(result);
    }

    /**
     * Ingests telemetry batch with data quality validation.
     * Returns list of quality assessments for each entry.
     */
    @PostMapping("/telemetry/batch")
    public ResponseEntity<List<DataQualityResultDto>> ingestTelemetryBatch(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody List<TelemetryDataDto> batch) {
        log.info("Ingesting telemetry batch of {} items", batch.size());
        telemetryService.processTelemetryBatch(tenantId, batch);
        // Note: async processing, return accepted status
        return ResponseEntity.accepted().build();
    }

    /**
     * Ingests telemetry batch synchronously with data quality results.
     */
    @PostMapping("/telemetry/batch/sync")
    public ResponseEntity<List<DataQualityResultDto>> ingestTelemetryBatchSync(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody List<TelemetryDataDto> batch) {
        log.info("Ingesting telemetry batch (sync) of {} items", batch.size());
        List<DataQualityResultDto> results = batch.stream()
                .map(data -> telemetryService.processTelemetry(tenantId, data))
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * Gets latest telemetry for a well.
     */
    @GetMapping("/wells/{wellId}/telemetry/latest")
    public ResponseEntity<TelemetryDataDto> getLatestWellTelemetry(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId) {
        TelemetryDataDto data = telemetryService.getLatestTelemetry(tenantId, wellId, "pf_well");
        if (data == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(data);
    }

    /**
     * Gets historical telemetry for a well.
     */
    @GetMapping("/wells/{wellId}/telemetry")
    public ResponseEntity<List<TelemetryDataDto>> getWellTelemetry(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID wellId,
            @RequestParam Long from,
            @RequestParam Long to,
            @RequestParam(required = false) String keys) {

        List<String> keyList = null;
        if (keys != null && !keys.isEmpty()) {
            keyList = Arrays.asList(keys.split(","));
        }

        List<TelemetryDataDto> data = telemetryService.getHistoricalTelemetry(tenantId, wellId, "pf_well", keyList, from, to);
        return ResponseEntity.ok(data);
    }

    /**
     * Gets latest telemetry for any entity.
     */
    @GetMapping("/entities/{entityId}/telemetry/latest")
    public ResponseEntity<TelemetryDataDto> getLatestEntityTelemetry(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID entityId,
            @RequestParam(required = false, defaultValue = "asset") String entityType) {
        TelemetryDataDto data = telemetryService.getLatestTelemetry(tenantId, entityId, entityType);
        if (data == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(data);
    }

    /**
     * Gets historical telemetry for any entity.
     */
    @GetMapping("/entities/{entityId}/telemetry")
    public ResponseEntity<List<TelemetryDataDto>> getEntityTelemetry(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID entityId,
            @RequestParam Long from,
            @RequestParam Long to,
            @RequestParam(required = false, defaultValue = "asset") String entityType,
            @RequestParam(required = false) String keys) {

        List<String> keyList = null;
        if (keys != null && !keys.isEmpty()) {
            keyList = Arrays.asList(keys.split(","));
        }

        List<TelemetryDataDto> data = telemetryService.getHistoricalTelemetry(tenantId, entityId, entityType, keyList, from, to);
        return ResponseEntity.ok(data);
    }
}
