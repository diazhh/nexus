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
package org.thingsboard.nexus.ct.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.nexus.ct.exception.CTEntityNotFoundException;
import org.thingsboard.server.common.data.template.*;
import org.thingsboard.server.dao.template.TemplateDefinitionDaoService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateDefinitionService {

    private final TemplateDefinitionDaoService daoService;
    private final ObjectMapper objectMapper;

    // --- CRUD Operations ---

    @Transactional
    public TemplateDefinitionDto createTemplate(CreateTemplateRequest request, UUID tenantId, UUID createdBy) {
        log.info("Creating template definition: {} for tenant {}", request.getTemplateName(), tenantId);

        String templateCode = generateTemplateCode(request.getModuleCode(), request.getEntityType(), request.getTemplateName());

        daoService.findByTemplateCode(templateCode).ifPresent(existing -> {
            throw new CTBusinessException("Template with code already exists: " + templateCode);
        });

        TemplateDefinitionDto dto = TemplateDefinitionDto.builder()
                .id(UUID.randomUUID())
                .templateCode(templateCode)
                .templateName(request.getTemplateName())
                .description(request.getDescription())
                .moduleCode(request.getModuleCode())
                .entityType(request.getEntityType())
                .category(request.getCategory())
                .version("1.0.0")
                .isActive(true)
                .templateStructureJson(serializeToJson(request.getTemplateStructure()))
                .requiredVariablesJson(serializeToJson(request.getRequiredVariables()))
                .createdBy(createdBy)
                .tenantId(tenantId)
                .createdTime(System.currentTimeMillis())
                .build();

        TemplateDefinitionDto saved = daoService.saveTemplateDefinition(dto);
        log.info("Template definition created: {} ({})", saved.getTemplateName(), saved.getId());

        return enrichDto(saved);
    }

    @Transactional
    public TemplateDefinitionDto updateTemplate(UUID templateId, UpdateTemplateRequest request, UUID updatedBy) {
        log.info("Updating template definition: {}", templateId);

        TemplateDefinitionDto existing = daoService.findById(templateId)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateId.toString()));

        String previousVersion = existing.getVersion();

        if (request.getTemplateName() != null) {
            existing.setTemplateName(request.getTemplateName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getTemplateStructure() != null) {
            existing.setTemplateStructureJson(serializeToJson(request.getTemplateStructure()));
        }
        if (request.getRequiredVariables() != null) {
            existing.setRequiredVariablesJson(serializeToJson(request.getRequiredVariables()));
        }

        String newVersion = incrementVersion(previousVersion);
        existing.setVersion(newVersion);
        existing.setUpdatedBy(updatedBy);
        existing.setUpdatedTime(System.currentTimeMillis());

        TemplateDefinitionDto saved = daoService.saveTemplateDefinition(existing);

        saveVersionHistory(saved.getId(), previousVersion, existing.getTemplateStructureJson(),
                request.getChangeDescription(), updatedBy);

        log.info("Template definition updated: {} -> version {}", saved.getTemplateName(), newVersion);
        return enrichDto(saved);
    }

    @Transactional(readOnly = true)
    public TemplateDefinitionDto getById(UUID templateId) {
        log.debug("Getting template definition by id: {}", templateId);

        TemplateDefinitionDto dto = daoService.findById(templateId)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateId.toString()));

        return enrichDto(dto);
    }

    @Transactional(readOnly = true)
    public TemplateDefinitionDto getByCode(String templateCode) {
        log.debug("Getting template definition by code: {}", templateCode);

        TemplateDefinitionDto dto = daoService.findByTemplateCode(templateCode)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateCode));

        return enrichDto(dto);
    }

    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getByModule(String moduleCode, UUID tenantId) {
        log.debug("Getting template definitions for module: {} and tenant: {}", moduleCode, tenantId);

        return daoService.findByModuleAndTenant(moduleCode, tenantId, true)
                .stream().map(this::enrichDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getByModuleAndEntityType(String moduleCode, String entityType, UUID tenantId) {
        log.debug("Getting template definitions for module: {}, entityType: {}, tenant: {}",
                moduleCode, entityType, tenantId);

        return daoService.findByModuleAndEntityTypeAndTenant(moduleCode, entityType, tenantId, true)
                .stream().map(this::enrichDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getByTenant(UUID tenantId) {
        log.debug("Getting all template definitions for tenant: {}", tenantId);

        return daoService.findByTenantAndActive(tenantId, true)
                .stream().map(this::enrichDto).collect(Collectors.toList());
    }

    @Transactional
    public TemplateDefinitionDto deactivateTemplate(UUID templateId, UUID updatedBy) {
        log.info("Deactivating template definition: {}", templateId);

        TemplateDefinitionDto dto = daoService.findById(templateId)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateId.toString()));

        dto.setIsActive(false);
        dto.setUpdatedBy(updatedBy);
        dto.setUpdatedTime(System.currentTimeMillis());

        TemplateDefinitionDto saved = daoService.saveTemplateDefinition(dto);
        log.info("Template definition deactivated: {}", saved.getTemplateName());

        return enrichDto(saved);
    }

    @Transactional
    public TemplateDefinitionDto activateTemplate(UUID templateId, UUID updatedBy) {
        log.info("Activating template definition: {}", templateId);

        TemplateDefinitionDto dto = daoService.findById(templateId)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateId.toString()));

        dto.setIsActive(true);
        dto.setUpdatedBy(updatedBy);
        dto.setUpdatedTime(System.currentTimeMillis());

        TemplateDefinitionDto saved = daoService.saveTemplateDefinition(dto);
        log.info("Template definition activated: {}", saved.getTemplateName());

        return enrichDto(saved);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        log.info("Deleting template definition: {}", templateId);

        TemplateDefinitionDto dto = daoService.findById(templateId)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateId.toString()));

        long instanceCount = daoService.countInstancesByTemplateId(templateId);
        if (instanceCount > 0) {
            throw new CTBusinessException("Cannot delete template with existing instances. Deactivate instead.");
        }

        daoService.deleteVersionsByTemplateId(templateId);
        daoService.deleteById(templateId);
        log.info("Template definition deleted: {}", dto.getTemplateName());
    }

    // --- Version Operations ---

    @Transactional(readOnly = true)
    public List<TemplateVersionDto> getVersionHistory(UUID templateId) {
        log.debug("Getting version history for template: {}", templateId);

        daoService.findById(templateId)
                .orElseThrow(() -> new CTEntityNotFoundException("Template", templateId.toString()));

        return daoService.findVersionsByTemplateId(templateId);
    }

    // --- Instance Operations ---

    @Transactional
    public TemplateInstanceDto recordInstance(UUID templateId, String templateVersion,
                                               UUID rootAssetId, String instanceVariablesJson,
                                               UUID createdBy, UUID tenantId) {
        log.info("Recording template instance for template: {}, rootAsset: {}", templateId, rootAssetId);

        TemplateInstanceDto instance = TemplateInstanceDto.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .templateVersion(templateVersion)
                .rootAssetId(rootAssetId)
                .instanceVariables(instanceVariablesJson)
                .createdBy(createdBy)
                .tenantId(tenantId)
                .createdTime(System.currentTimeMillis())
                .build();

        TemplateInstanceDto saved = daoService.saveInstance(instance);
        log.info("Template instance recorded: {}", saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<TemplateInstanceDto> getInstancesByTemplate(UUID templateId) {
        log.debug("Getting instances for template: {}", templateId);
        return daoService.findInstancesByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public List<TemplateInstanceDto> getInstancesByTenant(UUID tenantId) {
        log.debug("Getting instances for tenant: {}", tenantId);
        return daoService.findInstancesByTenant(tenantId);
    }

    @Transactional(readOnly = true)
    public long countTemplates(UUID tenantId) {
        return daoService.countByTenant(tenantId);
    }

    @Transactional(readOnly = true)
    public long countInstances(UUID templateId) {
        return daoService.countInstancesByTemplateId(templateId);
    }

    // --- Helper Methods ---

    private void saveVersionHistory(UUID templateId, String version, String templateStructure,
                                     String changeDescription, UUID createdBy) {
        TemplateVersionDto versionDto = TemplateVersionDto.builder()
                .id(UUID.randomUUID())
                .templateId(templateId)
                .version(version)
                .templateStructure(templateStructure)
                .changeDescription(changeDescription != null ? changeDescription : "Version update")
                .createdBy(createdBy)
                .createdTime(System.currentTimeMillis())
                .build();

        daoService.saveVersion(versionDto);
        log.debug("Version history saved: {} v{}", templateId, version);
    }

    private String generateTemplateCode(String moduleCode, String entityType, String templateName) {
        String sanitized = templateName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return String.format("%s-%s-%s", moduleCode.toLowerCase(), entityType.toLowerCase(), sanitized);
    }

    private String incrementVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int patch = Integer.parseInt(parts[2]) + 1;
            return String.format("%s.%s.%d", parts[0], parts[1], patch);
        } catch (Exception e) {
            log.warn("Could not increment version '{}', defaulting to next", version);
            return version + ".1";
        }
    }

    private TemplateDefinitionDto enrichDto(TemplateDefinitionDto dto) {
        if (dto.getTemplateStructure() == null && dto.getTemplateStructureJson() != null) {
            dto.setTemplateStructure(deserializeTemplateStructure(dto.getTemplateStructureJson()));
        }
        if (dto.getRequiredVariables() == null && dto.getRequiredVariablesJson() != null) {
            dto.setRequiredVariables(deserializeVariables(dto.getRequiredVariablesJson()));
        }
        return dto;
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new CTBusinessException("Failed to serialize to JSON: " + e.getMessage());
        }
    }

    private TemplateStructure deserializeTemplateStructure(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, TemplateStructure.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize template structure: {}", e.getMessage());
            return null;
        }
    }

    private List<TemplateVariable> deserializeVariables(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<TemplateVariable>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize variables: {}", e.getMessage());
            return List.of();
        }
    }
}
