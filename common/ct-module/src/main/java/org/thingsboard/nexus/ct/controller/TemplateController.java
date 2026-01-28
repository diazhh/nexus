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
package org.thingsboard.nexus.ct.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.ct.service.TemplateDefinitionService;
import org.thingsboard.server.common.data.template.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {

    private final TemplateDefinitionService templateDefinitionService;

    @PostMapping
    public ResponseEntity<TemplateDefinitionDto> createTemplate(
            @RequestParam UUID tenantId,
            @RequestParam UUID userId,
            @RequestBody CreateTemplateRequest request) {

        log.info("REST request to create template: {}", request.getTemplateName());
        TemplateDefinitionDto created = templateDefinitionService.createTemplate(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TemplateDefinitionDto> updateTemplate(
            @PathVariable UUID templateId,
            @RequestParam UUID userId,
            @RequestBody UpdateTemplateRequest request) {

        log.info("REST request to update template: {}", templateId);
        TemplateDefinitionDto updated = templateDefinitionService.updateTemplate(templateId, request, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDefinitionDto> getTemplateById(@PathVariable UUID templateId) {
        log.debug("REST request to get template: {}", templateId);
        TemplateDefinitionDto template = templateDefinitionService.getById(templateId);
        return ResponseEntity.ok(template);
    }

    @GetMapping("/code/{templateCode}")
    public ResponseEntity<TemplateDefinitionDto> getTemplateByCode(@PathVariable String templateCode) {
        log.debug("REST request to get template by code: {}", templateCode);
        TemplateDefinitionDto template = templateDefinitionService.getByCode(templateCode);
        return ResponseEntity.ok(template);
    }

    @GetMapping("/module/{moduleCode}")
    public ResponseEntity<List<TemplateDefinitionDto>> getTemplatesByModule(
            @PathVariable String moduleCode,
            @RequestParam UUID tenantId) {

        log.debug("REST request to get templates for module: {}", moduleCode);
        List<TemplateDefinitionDto> templates = templateDefinitionService.getByModule(moduleCode, tenantId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/module/{moduleCode}/type/{entityType}")
    public ResponseEntity<List<TemplateDefinitionDto>> getTemplatesByModuleAndType(
            @PathVariable String moduleCode,
            @PathVariable String entityType,
            @RequestParam UUID tenantId) {

        log.debug("REST request to get templates for module: {} and type: {}", moduleCode, entityType);
        List<TemplateDefinitionDto> templates = templateDefinitionService
                .getByModuleAndEntityType(moduleCode, entityType, tenantId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<TemplateDefinitionDto>> getTemplatesByTenant(@PathVariable UUID tenantId) {
        log.debug("REST request to get templates for tenant: {}", tenantId);
        List<TemplateDefinitionDto> templates = templateDefinitionService.getByTenant(tenantId);
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{templateId}/deactivate")
    public ResponseEntity<TemplateDefinitionDto> deactivateTemplate(
            @PathVariable UUID templateId,
            @RequestParam UUID userId) {

        log.info("REST request to deactivate template: {}", templateId);
        TemplateDefinitionDto deactivated = templateDefinitionService.deactivateTemplate(templateId, userId);
        return ResponseEntity.ok(deactivated);
    }

    @PutMapping("/{templateId}/activate")
    public ResponseEntity<TemplateDefinitionDto> activateTemplate(
            @PathVariable UUID templateId,
            @RequestParam UUID userId) {

        log.info("REST request to activate template: {}", templateId);
        TemplateDefinitionDto activated = templateDefinitionService.activateTemplate(templateId, userId);
        return ResponseEntity.ok(activated);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        log.info("REST request to delete template: {}", templateId);
        templateDefinitionService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{templateId}/versions")
    public ResponseEntity<List<TemplateVersionDto>> getTemplateVersions(@PathVariable UUID templateId) {
        log.debug("REST request to get versions for template: {}", templateId);
        List<TemplateVersionDto> versions = templateDefinitionService.getVersionHistory(templateId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{templateId}/instances")
    public ResponseEntity<List<TemplateInstanceDto>> getTemplateInstances(@PathVariable UUID templateId) {
        log.debug("REST request to get instances for template: {}", templateId);
        List<TemplateInstanceDto> instances = templateDefinitionService.getInstancesByTemplate(templateId);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/tenant/{tenantId}/instances")
    public ResponseEntity<List<TemplateInstanceDto>> getInstancesByTenant(@PathVariable UUID tenantId) {
        log.debug("REST request to get instances for tenant: {}", tenantId);
        List<TemplateInstanceDto> instances = templateDefinitionService.getInstancesByTenant(tenantId);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<Long> countTemplates(@PathVariable UUID tenantId) {
        long count = templateDefinitionService.countTemplates(tenantId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{templateId}/instances/count")
    public ResponseEntity<Long> countInstances(@PathVariable UUID templateId) {
        long count = templateDefinitionService.countInstances(templateId);
        return ResponseEntity.ok(count);
    }
}
