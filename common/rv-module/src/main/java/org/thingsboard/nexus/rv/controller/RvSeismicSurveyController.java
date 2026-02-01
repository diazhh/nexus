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
package org.thingsboard.nexus.rv.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.rv.dto.RvSeismicSurveyDto;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.nexus.rv.service.RvSeismicSurveyService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Seismic Survey assets.
 */
@RestController
@RequestMapping("/api/nexus/rv/seismic-surveys")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RV Seismic Surveys", description = "Seismic Survey Management")
public class RvSeismicSurveyController {

    private final RvSeismicSurveyService seismicSurveyService;

    @GetMapping
    @Operation(summary = "Get all seismic surveys")
    public ResponseEntity<PageData<RvSeismicSurveyDto>> getAllSeismicSurveys(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /seismic-surveys - tenantId={}, page={}, size={}", tenantId, page, size);
        return ResponseEntity.ok(toPageData(seismicSurveyService.getAllSeismicSurveys(tenantId, page, size)));
    }

    private <T> PageData<T> toPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get seismic survey by ID")
    public ResponseEntity<RvSeismicSurveyDto> getSeismicSurveyById(@PathVariable UUID id) {
        log.debug("GET /seismic-surveys/{}", id);
        return seismicSurveyService.getSeismicSurveyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-field/{fieldId}")
    @Operation(summary = "Get seismic surveys by field")
    public ResponseEntity<List<RvSeismicSurveyDto>> getSeismicSurveysByField(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID fieldId) {
        log.debug("GET /seismic-surveys/by-field/{}", fieldId);
        return ResponseEntity.ok(seismicSurveyService.getSeismicSurveysByField(tenantId, fieldId));
    }

    @GetMapping("/by-type/{surveyType}")
    @Operation(summary = "Get seismic surveys by type (2D, 3D, 4D, VSP)")
    public ResponseEntity<List<RvSeismicSurveyDto>> getSeismicSurveysByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable String surveyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /seismic-surveys/by-type/{}", surveyType);
        return ResponseEntity.ok(seismicSurveyService.getSeismicSurveysByType(tenantId, surveyType, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a new seismic survey")
    public ResponseEntity<RvSeismicSurveyDto> createSeismicSurvey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody RvSeismicSurveyDto dto) {
        log.info("POST /seismic-surveys - name={}, type={}", dto.getName(), dto.getSurveyType());
        dto.setTenantId(tenantId);
        RvSeismicSurveyDto created = seismicSurveyService.createSeismicSurvey(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a seismic survey")
    public ResponseEntity<RvSeismicSurveyDto> updateSeismicSurvey(
            @PathVariable UUID id,
            @RequestBody RvSeismicSurveyDto dto) {
        log.info("PUT /seismic-surveys/{}", id);
        dto.setId(id);
        return ResponseEntity.ok(seismicSurveyService.updateSeismicSurvey(dto));
    }

    @PostMapping("/{id}/mark-interpreted")
    @Operation(summary = "Mark survey as interpreted")
    public ResponseEntity<Void> markAsInterpreted(
            @PathVariable UUID id,
            @RequestParam String interpreter,
            @RequestParam int horizons,
            @RequestParam int faults) {
        log.info("POST /seismic-surveys/{}/mark-interpreted", id);
        seismicSurveyService.markAsInterpreted(id, interpreter, horizons, faults);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a seismic survey")
    public ResponseEntity<Void> deleteSeismicSurvey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        log.warn("DELETE /seismic-surveys/{}", id);
        seismicSurveyService.deleteSeismicSurvey(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
