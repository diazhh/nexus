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
package org.thingsboard.nexus.dr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.exception.DrBusinessException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.template.*;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.template.TemplateDefinitionDaoService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing Drilling Module templates and instantiation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrTemplateService {

    private final AssetService assetService;
    private final RelationService relationService;
    private final DrAttributeService attributeService;
    private final TemplateDefinitionDaoService templateDaoService;
    private final ObjectMapper objectMapper;

    private static final String MODULE_CODE = "DR";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Get all available templates for the Drilling module
     */
    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getAvailableTemplates(UUID tenantId) {
        log.debug("Getting available templates for drilling module and tenant: {}", tenantId);
        return templateDaoService.findByModuleAndTenant(MODULE_CODE, tenantId, true)
                .stream()
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all available templates for the Drilling module (TenantId version)
     */
    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getAvailableTemplates(TenantId tenantId) {
        return getAvailableTemplates(tenantId.getId());
    }

    /**
     * Get templates by entity type (TenantId version)
     */
    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getTemplatesByEntityType(TenantId tenantId, String entityType) {
        return getAvailableTemplatesByType(entityType, tenantId.getId());
    }

    /**
     * Get template by ID (TenantId version)
     */
    @Transactional(readOnly = true)
    public TemplateDefinitionDto getTemplateById(TenantId tenantId, UUID templateId) {
        return getTemplateById(templateId);
    }

    /**
     * Get template by code (TenantId version)
     */
    @Transactional(readOnly = true)
    public TemplateDefinitionDto getTemplateByCode(TenantId tenantId, String templateCode) {
        return getTemplateByCode(templateCode);
    }

    /**
     * Validate template variables
     */
    @Transactional(readOnly = true)
    public Map<String, String> validateTemplateVariables(TenantId tenantId, UUID templateId, Map<String, Object> variables) {
        log.debug("Validating template variables for template: {}", templateId);

        Map<String, String> errors = new HashMap<>();
        TemplateDefinitionDto template = getTemplateById(templateId);

        if (template.getTemplateStructure() == null) {
            errors.put("template", "Template has no structure defined");
            return errors;
        }

        // Find required variables from template structure
        List<String> requiredVariables = getRequiredVariablesInternal(template);

        for (String required : requiredVariables) {
            if (!variables.containsKey(required) || variables.get(required) == null ||
                (variables.get(required) instanceof String && ((String) variables.get(required)).isEmpty())) {
                errors.put(required, "Required variable '" + required + "' is missing or empty");
            }
        }

        return errors;
    }

    /**
     * Get required variables for a template
     */
    @Transactional(readOnly = true)
    public List<String> getRequiredVariables(TenantId tenantId, UUID templateId) {
        TemplateDefinitionDto template = getTemplateById(templateId);
        return getRequiredVariablesInternal(template);
    }

    private List<String> getRequiredVariablesInternal(TemplateDefinitionDto template) {
        Set<String> variables = new HashSet<>();

        if (template.getTemplateStructure() != null && template.getTemplateStructure().getNodes() != null) {
            for (TemplateNode node : template.getTemplateStructure().getNodes()) {
                // Extract variables from node name
                extractVariables(node.getNodeName(), variables);

                // Extract variables from attributes
                if (node.getAttributes() != null) {
                    for (TemplateAttribute attr : node.getAttributes()) {
                        if (attr.getValue() instanceof String) {
                            extractVariables((String) attr.getValue(), variables);
                        }
                        if (Boolean.TRUE.equals(attr.getIsRequired())) {
                            variables.add(attr.getKey());
                        }
                    }
                }
            }
        }

        return new ArrayList<>(variables);
    }

    private void extractVariables(String text, Set<String> variables) {
        if (text == null) return;

        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
    }

    /**
     * Get templates by entity type (e.g., DR_RIG, DR_BHA)
     */
    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getAvailableTemplatesByType(String entityType, UUID tenantId) {
        log.debug("Getting available templates for entityType: {}, tenant: {}", entityType, tenantId);
        return templateDaoService.findByModuleAndEntityTypeAndTenant(MODULE_CODE, entityType, tenantId, true)
                .stream()
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    /**
     * Get template by ID
     */
    @Transactional(readOnly = true)
    public TemplateDefinitionDto getTemplateById(UUID templateId) {
        log.debug("Getting template by id: {}", templateId);
        return templateDaoService.findById(templateId)
                .map(this::enrichDto)
                .orElseThrow(() -> new DrBusinessException("Template not found: " + templateId));
    }

    /**
     * Get template by code
     */
    @Transactional(readOnly = true)
    public TemplateDefinitionDto getTemplateByCode(String templateCode) {
        log.debug("Getting template by code: {}", templateCode);
        return templateDaoService.findByTemplateCode(templateCode)
                .map(this::enrichDto)
                .orElseThrow(() -> new DrBusinessException("Template not found: " + templateCode));
    }

    /**
     * Instantiate a template to create digital twin assets
     */
    @Transactional
    public TemplateInstanceResult instantiateTemplate(
            TenantId tenantId,
            UUID templateId,
            Map<String, Object> variables,
            UUID createdBy) {

        log.info("Instantiating drilling template {} for tenant {} with variables: {}",
                templateId, tenantId, variables);

        TemplateDefinitionDto template = getTemplateById(templateId);

        TemplateStructure structure = template.getTemplateStructure();
        if (structure == null || structure.getNodes() == null || structure.getNodes().isEmpty()) {
            throw new DrBusinessException("Template has no structure defined: " + template.getTemplateName());
        }

        Map<String, UUID> nodeKeyToAssetIdMap = new LinkedHashMap<>();
        List<UUID> createdAssetIds = new ArrayList<>();

        // Find and create the root node first
        TemplateNode rootNode = structure.getNodes().stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsRoot()))
                .findFirst()
                .orElseThrow(() -> new DrBusinessException("Template has no root node: " + template.getTemplateName()));

        Asset rootAsset = createAssetFromTemplateNode(tenantId, rootNode, variables);
        nodeKeyToAssetIdMap.put(rootNode.getNodeKey(), rootAsset.getId().getId());
        createdAssetIds.add(rootAsset.getId().getId());

        // Create all child nodes
        List<TemplateNode> childNodes = structure.getNodes().stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRoot()))
                .collect(Collectors.toList());

        for (TemplateNode childNode : childNodes) {
            Asset childAsset = createAssetFromTemplateNode(tenantId, childNode, variables);
            nodeKeyToAssetIdMap.put(childNode.getNodeKey(), childAsset.getId().getId());
            createdAssetIds.add(childAsset.getId().getId());
        }

        // Create relations between assets
        if (structure.getRelations() != null) {
            for (TemplateRelation rel : structure.getRelations()) {
                UUID fromAssetId = nodeKeyToAssetIdMap.get(rel.getFromNodeKey());
                UUID toAssetId = nodeKeyToAssetIdMap.get(rel.getToNodeKey());

                if (fromAssetId != null && toAssetId != null) {
                    createRelation(new AssetId(fromAssetId), new AssetId(toAssetId), rel.getRelationType());
                } else {
                    log.warn("Could not create relation {} -> {}: asset IDs not found",
                            rel.getFromNodeKey(), rel.getToNodeKey());
                }
            }
        }

        // Record the instance
        UUID instanceId = null;
        try {
            String variablesJson = objectMapper.writeValueAsString(variables);
            TemplateInstanceDto instance = TemplateInstanceDto.builder()
                    .id(UUID.randomUUID())
                    .templateId(templateId)
                    .templateVersion(template.getVersion())
                    .rootAssetId(rootAsset.getId().getId())
                    .instanceVariables(variablesJson)
                    .createdBy(createdBy)
                    .tenantId(tenantId.getId())
                    .createdTime(System.currentTimeMillis())
                    .build();

            TemplateInstanceDto saved = templateDaoService.saveInstance(instance);
            instanceId = saved.getId();
        } catch (Exception e) {
            log.error("Failed to record template instance: {}", e.getMessage(), e);
        }

        log.info("Drilling template instantiated successfully: {} with {} assets created",
                template.getTemplateName(), createdAssetIds.size());

        return TemplateInstanceResult.builder()
                .instanceId(instanceId)
                .rootAssetId(rootAsset.getId().getId())
                .createdAssetIds(createdAssetIds)
                .nodeKeyToAssetIdMap(nodeKeyToAssetIdMap)
                .build();
    }

    /**
     * Create an asset from a template node
     */
    private Asset createAssetFromTemplateNode(
            TenantId tenantId,
            TemplateNode node,
            Map<String, Object> variables) {

        String assetName = replaceVariables(node.getNodeName(), variables);
        String assetType = node.getAssetType() != null ? node.getAssetType() : node.getNodeType();

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetName);
        asset.setType(assetType);
        asset.setLabel(assetName);

        asset = assetService.saveAsset(asset);
        log.debug("Created drilling asset: {} ({}) of type {}", asset.getName(), asset.getId(), assetType);

        // Save attributes from template
        if (node.getAttributes() != null && !node.getAttributes().isEmpty()) {
            Map<String, Object> processedAttributes = new HashMap<>();
            for (TemplateAttribute attr : node.getAttributes()) {
                Object value = attr.getValue();
                if (value instanceof String) {
                    value = replaceVariables((String) value, variables);
                }
                processedAttributes.put(attr.getKey(), value);
            }
            attributeService.saveServerAttributes(asset.getId().getId(), processedAttributes);
        }

        return asset;
    }

    /**
     * Create a relation between two assets
     */
    private void createRelation(AssetId fromAssetId, AssetId toAssetId, String relationType) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(fromAssetId);
        relation.setTo(toAssetId);
        relation.setType(relationType);
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelation(null, relation);
        log.debug("Created relation: {} -[{}]-> {}", fromAssetId, relationType, toAssetId);
    }

    /**
     * Replace template variables in a string
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Enrich DTO with deserialized structures
     */
    private TemplateDefinitionDto enrichDto(TemplateDefinitionDto dto) {
        if (dto.getTemplateStructure() == null && dto.getTemplateStructureJson() != null) {
            try {
                dto.setTemplateStructure(
                        objectMapper.readValue(dto.getTemplateStructureJson(), TemplateStructure.class));
            } catch (Exception e) {
                log.warn("Failed to deserialize template structure: {}", e.getMessage());
            }
        }
        return dto;
    }
}
