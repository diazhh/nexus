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
package org.thingsboard.nexus.po.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.po.dto.ml.MlModelType;
import org.thingsboard.nexus.po.dto.ml.PoMlModelDto;
import org.thingsboard.nexus.po.service.ml.PoMlModelService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for ML model management.
 */
@RestController
@RequestMapping("/api/nexus/po/ml/models")
@RequiredArgsConstructor
@Slf4j
public class PoMlModelController {

    private final PoMlModelService modelService;

    /**
     * Get all models for tenant with pagination.
     */
    @GetMapping
    public ResponseEntity<Page<PoMlModelDto>> getModels(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTime"));
        return ResponseEntity.ok(modelService.getModels(tenantId, pageRequest));
    }

    /**
     * Get all models for tenant (no pagination).
     */
    @GetMapping("/all")
    public ResponseEntity<List<PoMlModelDto>> getAllModels(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(modelService.getAllModels(tenantId));
    }

    /**
     * Get model by ID.
     */
    @GetMapping("/{modelId}")
    public ResponseEntity<PoMlModelDto> getModel(
            @PathVariable UUID modelId) {
        return ResponseEntity.ok(modelService.getModel(modelId));
    }

    /**
     * Get active models for tenant.
     */
    @GetMapping("/active")
    public ResponseEntity<List<PoMlModelDto>> getActiveModels(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(modelService.getActiveModels(tenantId));
    }

    /**
     * Get models by type.
     */
    @GetMapping("/type/{modelType}")
    public ResponseEntity<List<PoMlModelDto>> getModelsByType(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable MlModelType modelType) {
        return ResponseEntity.ok(modelService.getModelsByType(tenantId, modelType));
    }

    /**
     * Deploy a model (set as active).
     */
    @PostMapping("/{modelId}/deploy")
    public ResponseEntity<PoMlModelDto> deployModel(
            @PathVariable UUID modelId) {
        log.info("Deploying model: {}", modelId);
        return ResponseEntity.ok(modelService.deployModel(modelId));
    }

    /**
     * Archive a model.
     */
    @PostMapping("/{modelId}/archive")
    public ResponseEntity<PoMlModelDto> archiveModel(
            @PathVariable UUID modelId) {
        log.info("Archiving model: {}", modelId);
        return ResponseEntity.ok(modelService.archiveModel(modelId));
    }

    /**
     * Delete a model.
     */
    @DeleteMapping("/{modelId}")
    public ResponseEntity<Void> deleteModel(
            @PathVariable UUID modelId) {
        log.info("Deleting model: {}", modelId);
        modelService.deleteModel(modelId);
        return ResponseEntity.noContent().build();
    }
}
