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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.pf.dto.DataQualityResultDto;
import org.thingsboard.nexus.pf.dto.DataQualityRuleDto;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.nexus.pf.service.PfDataQualityService;
import org.thingsboard.nexus.pf.service.PfDataQualityService.DataQualityStatistics;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Data Quality Rules management and validation.
 */
@RestController
@RequestMapping("/api/nexus/pf/data-quality")
@RequiredArgsConstructor
@Slf4j
public class PfDataQualityController {

    private final PfDataQualityService dataQualityService;

    // ==================== Rule Management ====================

    /**
     * Creates or updates a data quality rule.
     */
    @PostMapping("/rules")
    public ResponseEntity<DataQualityRuleDto> saveRule(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody DataQualityRuleDto dto) {
        log.info("Saving data quality rule for variable: {}", dto.getVariableKey());
        DataQualityRuleDto saved = dataQualityService.saveRule(dto);
        return ResponseEntity.status(dto.getId() == null ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    /**
     * Gets all enabled data quality rules.
     */
    @GetMapping("/rules")
    public ResponseEntity<List<DataQualityRuleDto>> getAllRules(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(dataQualityService.getAllRules());
    }

    /**
     * Gets rules for a specific entity type.
     */
    @GetMapping("/rules/by-entity-type/{entityType}")
    public ResponseEntity<List<DataQualityRuleDto>> getRulesByEntityType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable String entityType) {
        return ResponseEntity.ok(dataQualityService.getRulesForEntityType(entityType));
    }

    /**
     * Gets rules for a specific entity.
     */
    @GetMapping("/rules/by-entity/{entityId}")
    public ResponseEntity<List<DataQualityRuleDto>> getRulesByEntity(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(dataQualityService.getRulesForEntity(entityId));
    }

    /**
     * Gets global rules (no entity type or ID).
     */
    @GetMapping("/rules/global")
    public ResponseEntity<List<DataQualityRuleDto>> getGlobalRules(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(dataQualityService.getGlobalRules());
    }

    /**
     * Deletes a data quality rule.
     */
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID ruleId) {
        log.warn("Deleting data quality rule: {}", ruleId);
        dataQualityService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates default ESP rules.
     */
    @PostMapping("/rules/defaults/esp")
    public ResponseEntity<Void> createDefaultEspRules(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Creating default ESP data quality rules");
        dataQualityService.createDefaultEspRules();
        return ResponseEntity.ok().build();
    }

    /**
     * Creates default PCP rules.
     */
    @PostMapping("/rules/defaults/pcp")
    public ResponseEntity<Void> createDefaultPcpRules(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Creating default PCP data quality rules");
        dataQualityService.createDefaultPcpRules();
        return ResponseEntity.ok().build();
    }

    /**
     * Creates default Gas Lift rules.
     */
    @PostMapping("/rules/defaults/gas-lift")
    public ResponseEntity<Void> createDefaultGasLiftRules(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Creating default Gas Lift data quality rules");
        dataQualityService.createDefaultGasLiftRules();
        return ResponseEntity.ok().build();
    }

    /**
     * Creates default Rod Pump rules.
     */
    @PostMapping("/rules/defaults/rod-pump")
    public ResponseEntity<Void> createDefaultRodPumpRules(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Creating default Rod Pump data quality rules");
        dataQualityService.createDefaultRodPumpRules();
        return ResponseEntity.ok().build();
    }

    // ==================== Validation ====================

    /**
     * Validates telemetry data and returns quality assessment.
     */
    @PostMapping("/validate")
    public ResponseEntity<DataQualityResultDto> validateTelemetry(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody TelemetryDataDto data) {
        DataQualityResultDto result = dataQualityService.validateTelemetry(data);
        return ResponseEntity.ok(result);
    }

    /**
     * Validates multiple telemetry data entries.
     */
    @PostMapping("/validate/batch")
    public ResponseEntity<List<DataQualityResultDto>> validateTelemetryBatch(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody List<TelemetryDataDto> dataList) {
        List<DataQualityResultDto> results = dataList.stream()
                .map(dataQualityService::validateTelemetry)
                .toList();
        return ResponseEntity.ok(results);
    }

    // ==================== Statistics ====================

    /**
     * Gets current data quality statistics summary.
     */
    @GetMapping("/statistics")
    public ResponseEntity<DataQualityStatistics> getStatistics(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(dataQualityService.getStatistics());
    }

    /**
     * Clears the data quality caches (useful for testing or after rule changes).
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearCache(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Clearing data quality caches");
        dataQualityService.clearCaches();
        return ResponseEntity.ok().build();
    }

}
