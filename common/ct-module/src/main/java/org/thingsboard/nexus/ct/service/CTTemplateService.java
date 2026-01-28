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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.template.*;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTTemplateService {

    private final AssetService assetService;
    private final RelationService relationService;
    private final CTAttributeService attributeService;
    private final TemplateDefinitionService templateDefinitionService;
    private final ObjectMapper objectMapper;

    private static final String MODULE_CODE = "CT";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getAvailableTemplates(String moduleCode, UUID tenantId) {
        log.debug("Getting available templates for module: {} and tenant: {}", moduleCode, tenantId);
        return templateDefinitionService.getByModule(moduleCode, tenantId);
    }

    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> getAvailableTemplatesByType(String moduleCode, String entityType, UUID tenantId) {
        log.debug("Getting available templates for module: {}, entityType: {}, tenant: {}", moduleCode, entityType, tenantId);
        return templateDefinitionService.getByModuleAndEntityType(moduleCode, entityType, tenantId);
    }

    @Transactional(readOnly = true)
    public TemplateDefinitionDto getTemplateById(UUID templateId) {
        log.debug("Getting template by id: {}", templateId);
        return templateDefinitionService.getById(templateId);
    }

    @Transactional(readOnly = true)
    public TemplateDefinitionDto getTemplateByCode(String templateCode) {
        log.debug("Getting template by code: {}", templateCode);
        return templateDefinitionService.getByCode(templateCode);
    }

    @Transactional
    public TemplateInstanceResult instantiateTemplate(
            TenantId tenantId,
            UUID templateId,
            Map<String, Object> variables,
            UUID createdBy) {

        log.info("Instantiating template {} for tenant {} with variables: {}",
                templateId, tenantId, variables);

        TemplateDefinitionDto template = templateDefinitionService.getById(templateId);

        TemplateStructure structure = template.getTemplateStructure();
        if (structure == null || structure.getNodes() == null || structure.getNodes().isEmpty()) {
            throw new CTBusinessException("Template has no structure defined: " + template.getTemplateName());
        }

        Map<String, UUID> nodeKeyToAssetIdMap = new LinkedHashMap<>();
        List<UUID> createdAssetIds = new ArrayList<>();

        TemplateNode rootNode = structure.getNodes().stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsRoot()))
                .findFirst()
                .orElseThrow(() -> new CTBusinessException("Template has no root node: " + template.getTemplateName()));

        Asset rootAsset = createAssetFromTemplateNode(tenantId, rootNode, variables);
        nodeKeyToAssetIdMap.put(rootNode.getNodeKey(), rootAsset.getId().getId());
        createdAssetIds.add(rootAsset.getId().getId());

        List<TemplateNode> childNodes = structure.getNodes().stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRoot()))
                .collect(Collectors.toList());

        for (TemplateNode childNode : childNodes) {
            Asset childAsset = createAssetFromTemplateNode(tenantId, childNode, variables);
            nodeKeyToAssetIdMap.put(childNode.getNodeKey(), childAsset.getId().getId());
            createdAssetIds.add(childAsset.getId().getId());
        }

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

        UUID instanceId = null;
        try {
            String variablesJson = objectMapper.writeValueAsString(variables);
            var instance = templateDefinitionService.recordInstance(
                    templateId, template.getVersion(), rootAsset.getId().getId(),
                    variablesJson, createdBy, tenantId.getId());
            instanceId = instance.getId();
        } catch (Exception e) {
            log.error("Failed to record template instance: {}", e.getMessage(), e);
        }

        log.info("Template instantiated successfully: {} with {} assets created",
                template.getTemplateName(), createdAssetIds.size());

        return TemplateInstanceResult.builder()
                .instanceId(instanceId)
                .rootAssetId(rootAsset.getId().getId())
                .createdAssetIds(createdAssetIds)
                .nodeKeyToAssetIdMap(nodeKeyToAssetIdMap)
                .build();
    }

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
        log.debug("Created asset: {} ({}) of type {}", asset.getName(), asset.getId(), assetType);

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

    private void createRelation(AssetId fromAssetId, AssetId toAssetId, String relationType) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(fromAssetId);
        relation.setTo(toAssetId);
        relation.setType(relationType);
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelation(null, relation);
        log.debug("Created relation: {} -[{}]-> {}", fromAssetId, relationType, toAssetId);
    }

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
}
