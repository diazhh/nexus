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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.dr.service.DrTemplateService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.template.TemplateDefinitionDto;
import org.thingsboard.server.common.data.template.TemplateInstanceResult;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Drilling Module Template operations
 */
@RestController
@RequestMapping("/api/nexus/dr/templates")
@RequiredArgsConstructor
@Slf4j
public class DrTemplateController {

    private final DrTemplateService templateService;

    /**
     * Get all available drilling templates
     */
    @GetMapping
    public ResponseEntity<List<TemplateDefinitionDto>> getAvailableTemplates(@RequestParam UUID tenantId) {
        log.debug("REST request to get available drilling templates for tenant: {}", tenantId);
        List<TemplateDefinitionDto> templates = templateService.getAvailableTemplates(new TenantId(tenantId));
        return ResponseEntity.ok(templates);
    }

    /**
     * Get drilling templates by entity type (e.g., DR_RIG, DR_BHA)
     */
    @GetMapping("/entity-type/{entityType}")
    public ResponseEntity<List<TemplateDefinitionDto>> getTemplatesByEntityType(
            @RequestParam UUID tenantId,
            @PathVariable String entityType) {
        log.debug("REST request to get drilling templates for entity type: {}", entityType);
        List<TemplateDefinitionDto> templates = templateService.getTemplatesByEntityType(new TenantId(tenantId), entityType);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get a specific template by ID
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateDefinitionDto> getTemplateById(
            @RequestParam UUID tenantId,
            @PathVariable UUID templateId) {
        log.debug("REST request to get drilling template: {}", templateId);
        TemplateDefinitionDto template = templateService.getTemplateById(new TenantId(tenantId), templateId);
        return ResponseEntity.ok(template);
    }

    /**
     * Get template by code (e.g., DR-RIG-LAND-STD)
     */
    @GetMapping("/code/{templateCode}")
    public ResponseEntity<TemplateDefinitionDto> getTemplateByCode(
            @RequestParam UUID tenantId,
            @PathVariable String templateCode) {
        log.debug("REST request to get drilling template by code: {}", templateCode);
        TemplateDefinitionDto template = templateService.getTemplateByCode(new TenantId(tenantId), templateCode);
        return ResponseEntity.ok(template);
    }

    /**
     * Instantiate a template to create a new digital twin
     * This creates the complete asset hierarchy with all child assets and relations
     */
    @PostMapping("/{templateId}/instantiate")
    public ResponseEntity<TemplateInstanceResult> instantiateTemplate(
            @RequestParam UUID tenantId,
            @PathVariable UUID templateId,
            @Valid @RequestBody Map<String, Object> variables) {

        log.info("REST request to instantiate drilling template: {} with variables: {}", templateId, variables.keySet());

        TemplateInstanceResult result = templateService.instantiateTemplate(
            new TenantId(tenantId),
            templateId,
            variables,
            tenantId
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Validate template variables before instantiation
     */
    @PostMapping("/{templateId}/validate")
    public ResponseEntity<Map<String, String>> validateTemplateVariables(
            @RequestParam UUID tenantId,
            @PathVariable UUID templateId,
            @Valid @RequestBody Map<String, Object> variables) {

        log.debug("REST request to validate template variables for: {}", templateId);

        Map<String, String> validationErrors = templateService.validateTemplateVariables(
            new TenantId(tenantId),
            templateId,
            variables
        );

        return ResponseEntity.ok(validationErrors);
    }

    /**
     * Get required variables for a template
     */
    @GetMapping("/{templateId}/variables")
    public ResponseEntity<List<String>> getRequiredVariables(
            @RequestParam UUID tenantId,
            @PathVariable UUID templateId) {

        log.debug("REST request to get required variables for template: {}", templateId);
        List<String> requiredVariables = templateService.getRequiredVariables(new TenantId(tenantId), templateId);
        return ResponseEntity.ok(requiredVariables);
    }
}
