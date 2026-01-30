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
package org.thingsboard.nexus.dr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.dr.dto.DrDirectionalSurveyDto;
import org.thingsboard.nexus.dr.model.DrDirectionalSurvey;
import org.thingsboard.nexus.dr.model.enums.SurveyType;
import org.thingsboard.nexus.dr.service.DrDirectionalSurveyService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Directional Survey operations
 */
@RestController
@RequestMapping("/api/nexus/dr/surveys")
@RequiredArgsConstructor
@Slf4j
public class DrDirectionalSurveyController {

    private final DrDirectionalSurveyService surveyService;

    @GetMapping("/{id}")
    public ResponseEntity<DrDirectionalSurveyDto> getSurveyById(@PathVariable UUID id) {
        log.debug("REST request to get Directional Survey: {}", id);
        DrDirectionalSurveyDto survey = surveyService.getById(id);
        return ResponseEntity.ok(survey);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<DrDirectionalSurveyDto>> getSurveysByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "mdFt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Surveys for tenant: {}", tenantId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrDirectionalSurveyDto> surveys = surveyService.getByTenant(tenantId, pageable);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/run/{runId}")
    public ResponseEntity<Page<DrDirectionalSurveyDto>> getSurveysByRun(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "mdFt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Surveys for run: {}", runId);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrDirectionalSurveyDto> surveys = surveyService.getByRun(runId, pageable);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/run/{runId}/ordered")
    public ResponseEntity<List<DrDirectionalSurveyDto>> getSurveysByRunOrdered(@PathVariable UUID runId) {
        log.debug("REST request to get ordered Surveys for run: {}", runId);
        List<DrDirectionalSurveyDto> surveys = surveyService.getSurveysByRunOrdered(runId);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/run/{runId}/definitive")
    public ResponseEntity<List<DrDirectionalSurveyDto>> getDefinitiveSurveys(@PathVariable UUID runId) {
        log.debug("REST request to get definitive Surveys for run: {}", runId);
        List<DrDirectionalSurveyDto> surveys = surveyService.getDefinitiveSurveysByRun(runId);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/tenant/{tenantId}/filter")
    public ResponseEntity<Page<DrDirectionalSurveyDto>> getSurveysByFilters(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) UUID runId,
            @RequestParam(required = false) UUID wellId,
            @RequestParam(required = false) SurveyType surveyType,
            @RequestParam(required = false) Boolean isDefinitive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "mdFt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("REST request to get Surveys with filters");

        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DrDirectionalSurveyDto> surveys = surveyService.getByFilters(
                tenantId, runId, wellId, surveyType, isDefinitive, pageable);
        return ResponseEntity.ok(surveys);
    }

    @PostMapping
    public ResponseEntity<DrDirectionalSurveyDto> createSurvey(@Valid @RequestBody DrDirectionalSurvey survey) {
        log.info("REST request to create Directional Survey at MD {} ft", survey.getMdFt());
        DrDirectionalSurveyDto createdSurvey = surveyService.create(survey);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSurvey);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DrDirectionalSurveyDto> updateSurvey(
            @PathVariable UUID id,
            @Valid @RequestBody DrDirectionalSurvey survey) {

        log.info("REST request to update Directional Survey: {}", id);
        DrDirectionalSurveyDto updatedSurvey = surveyService.update(id, survey);
        return ResponseEntity.ok(updatedSurvey);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable UUID id) {
        log.info("REST request to delete Directional Survey: {}", id);
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-definitive")
    public ResponseEntity<DrDirectionalSurveyDto> markAsDefinitive(@PathVariable UUID id) {
        log.info("REST request to mark Survey as definitive: {}", id);
        DrDirectionalSurveyDto updatedSurvey = surveyService.markAsDefinitive(id);
        return ResponseEntity.ok(updatedSurvey);
    }

    // --- Trajectory Calculation Endpoints ---

    @PostMapping("/run/{runId}/recalculate")
    public ResponseEntity<Void> recalculateTrajectory(@PathVariable UUID runId) {
        log.info("REST request to recalculate trajectory for run: {}", runId);
        surveyService.recalculateEntireTrajectory(runId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/run/{runId}/recalculate-from")
    public ResponseEntity<Void> recalculateTrajectoryFromDepth(
            @PathVariable UUID runId,
            @RequestParam BigDecimal fromDepth) {

        log.info("REST request to recalculate trajectory for run {} from depth {} ft", runId, fromDepth);
        surveyService.recalculateTrajectoryFromDepth(runId, fromDepth);
        return ResponseEntity.ok().build();
    }

    // --- Analysis Endpoints ---

    @GetMapping("/run/{runId}/high-dls")
    public ResponseEntity<List<DrDirectionalSurveyDto>> getHighDlsSurveys(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "3.0") BigDecimal threshold) {

        log.debug("REST request to get high DLS surveys for run: {}", runId);
        List<DrDirectionalSurveyDto> surveys = surveyService.getHighDlsSurveys(runId, threshold);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/run/{runId}/max-dls")
    public ResponseEntity<BigDecimal> getMaxDls(@PathVariable UUID runId) {
        log.debug("REST request to get max DLS for run: {}", runId);
        BigDecimal maxDls = surveyService.getMaxDls(runId);
        return ResponseEntity.ok(maxDls);
    }

    @GetMapping("/run/{runId}/horizontal")
    public ResponseEntity<List<DrDirectionalSurveyDto>> getHorizontalSections(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "85.0") BigDecimal inclinationThreshold) {

        log.debug("REST request to get horizontal sections for run: {}", runId);
        List<DrDirectionalSurveyDto> surveys = surveyService.getHorizontalSections(runId, inclinationThreshold);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/run/{runId}/deepest")
    public ResponseEntity<DrDirectionalSurveyDto> getDeepestSurvey(@PathVariable UUID runId) {
        log.debug("REST request to get deepest survey for run: {}", runId);
        DrDirectionalSurveyDto survey = surveyService.getDeepestSurvey(runId);
        if (survey == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(survey);
    }

    @GetMapping("/run/{runId}/latest")
    public ResponseEntity<DrDirectionalSurveyDto> getLatestSurvey(@PathVariable UUID runId) {
        log.debug("REST request to get latest survey for run: {}", runId);
        DrDirectionalSurveyDto survey = surveyService.getLatestSurvey(runId);
        if (survey == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(survey);
    }

    @GetMapping("/run/{runId}/interpolate")
    public ResponseEntity<DrDirectionalSurveyDto> interpolateAtDepth(
            @PathVariable UUID runId,
            @RequestParam BigDecimal depth) {

        log.debug("REST request to interpolate at depth {} ft for run: {}", depth, runId);
        DrDirectionalSurveyDto survey = surveyService.interpolateAtDepth(runId, depth);
        if (survey == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(survey);
    }

    // --- Statistics ---

    @GetMapping("/run/{runId}/count")
    public ResponseEntity<Long> countByRun(@PathVariable UUID runId) {
        long count = surveyService.countByRun(runId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/run/{runId}/count/definitive")
    public ResponseEntity<Long> countDefinitiveByRun(@PathVariable UUID runId) {
        long count = surveyService.countDefinitiveByRun(runId);
        return ResponseEntity.ok(count);
    }
}
