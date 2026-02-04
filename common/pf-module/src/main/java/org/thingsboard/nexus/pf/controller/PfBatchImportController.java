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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.nexus.pf.service.PfBatchImportService;
import org.thingsboard.nexus.pf.service.PfBatchImportService.ImportResult;
import org.thingsboard.nexus.pf.service.PfBatchImportService.ValidationResult;

import java.util.UUID;

/**
 * REST Controller for batch import operations.
 */
@RestController
@RequestMapping("/api/nexus/pf/import")
@RequiredArgsConstructor
@Slf4j
public class PfBatchImportController {

    private final PfBatchImportService batchImportService;

    /**
     * Imports wells from CSV file.
     */
    @PostMapping(value = "/wells", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importWells(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam("file") MultipartFile file) {

        log.info("Importing wells from CSV: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        ImportResult result = batchImportService.importWells(tenantId, file);

        log.info("Import completed: total={}, success={}, failed={}, duration={}ms",
                result.getTotalRecords(), result.getSuccessCount(),
                result.getFailureCount(), result.getDurationMs());

        return ResponseEntity.ok(result);
    }

    /**
     * Imports ESP systems from CSV file.
     */
    @PostMapping(value = "/esp-systems", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importEspSystems(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam("file") MultipartFile file) {

        log.info("Importing ESP systems from CSV: {}", file.getOriginalFilename());

        ImportResult result = batchImportService.importEspSystems(tenantId, file);

        return ResponseEntity.ok(result);
    }

    /**
     * Validates CSV file before import.
     */
    @PostMapping(value = "/validate/{entityType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidationResult> validateFile(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable String entityType,
            @RequestParam("file") MultipartFile file) {

        log.info("Validating CSV file for {}: {}", entityType, file.getOriginalFilename());

        ValidationResult result = batchImportService.validateCsvFile(file, entityType);

        return ResponseEntity.ok(result);
    }

    /**
     * Downloads CSV template for the specified entity type.
     */
    @GetMapping(value = "/template/{entityType}", produces = "text/csv")
    public ResponseEntity<String> downloadTemplate(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable String entityType) {

        String template = batchImportService.generateCsvTemplate(entityType);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + entityType + "_template.csv\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(template);
    }
}
