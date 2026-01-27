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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.dto.template.*;
import org.thingsboard.nexus.ct.exception.CTBusinessException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.relation.RelationService;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTTemplateService {

    private final AssetService assetService;
    private final RelationService relationService;
    private final CTAttributeService attributeService;
    private final ObjectMapper objectMapper;
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private final Map<String, CTTemplateDto> templateCache = new HashMap<>();

    @Transactional(readOnly = true)
    public List<CTTemplateDto> getAvailableTemplates(String category) {
        log.debug("Getting available templates for category: {}", category);
        
        try {
            if (templateCache.isEmpty()) {
                loadTemplates();
            }
            
            if (category == null || category.isEmpty()) {
                return new ArrayList<>(templateCache.values());
            }
            
            return templateCache.values().stream()
                    .filter(t -> category.equals(t.getCategory()))
                    .toList();
        } catch (Exception e) {
            log.error("Error getting templates for category: {}", category, e);
            throw new CTBusinessException("Failed to load templates: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public CTTemplateDto getTemplateById(String templateId) {
        log.debug("Getting template by id: {}", templateId);
        
        if (templateCache.isEmpty()) {
            loadTemplates();
        }
        
        CTTemplateDto template = templateCache.get(templateId);
        if (template == null) {
            throw new CTBusinessException("Template not found: " + templateId);
        }
        
        return template;
    }

    @Transactional
    public TemplateInstanceResult instantiateTemplate(
            TenantId tenantId,
            String templateId,
            Map<String, Object> variables) {
        
        log.info("Instantiating template {} for tenant {} with variables: {}", 
                templateId, tenantId, variables);
        
        try {
            CTTemplateDto template = getTemplateById(templateId);
            
            AssetNodeDto rootNode = template.getAssetHierarchy().getRoot();
            
            Asset rootAsset = createAssetFromNode(tenantId, rootNode, variables, null);
            
            Map<String, UUID> assetIdsByType = new HashMap<>();
            List<UUID> createdAssetIds = new ArrayList<>();
            List<String> createdRelations = new ArrayList<>();
            
            assetIdsByType.put(rootNode.getType(), rootAsset.getId().getId());
            createdAssetIds.add(rootAsset.getId().getId());
            
            if (rootNode.getChildren() != null && !rootNode.getChildren().isEmpty()) {
                processChildAssets(tenantId, rootAsset, rootNode.getChildren(), 
                        variables, assetIdsByType, createdAssetIds, createdRelations);
            }
            
            return TemplateInstanceResult.builder()
                    .rootAssetId(rootAsset.getId().getId())
                    .assetIdsByType(assetIdsByType)
                    .createdAssetIds(createdAssetIds)
                    .createdRelations(createdRelations)
                    .success(true)
                    .build();
            
        } catch (Exception e) {
            log.error("Error instantiating template {}: {}", templateId, e.getMessage(), e);
            return TemplateInstanceResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Asset createAssetFromNode(
            TenantId tenantId,
            AssetNodeDto node,
            Map<String, Object> variables,
            Asset parentAsset) {
        
        String assetName = replaceVariables(node.getName(), variables);
        String assetLabel = node.getLabel() != null ? 
                replaceVariables(node.getLabel(), variables) : assetName;
        
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetName);
        asset.setType(node.getType());
        asset.setLabel(assetLabel);
        
        asset = assetService.saveAsset(asset);
        log.debug("Created asset: {} ({})", asset.getName(), asset.getId());
        
        if (node.getAttributes() != null && !node.getAttributes().isEmpty()) {
            Map<String, Object> processedAttributes = new HashMap<>();
            node.getAttributes().forEach((key, value) -> {
                Object processedValue = value instanceof String ? 
                        replaceVariables((String) value, variables) : value;
                processedAttributes.put(key, processedValue);
            });
            
            attributeService.saveServerAttributes(asset.getId().getId(), processedAttributes);
        }
        
        if (parentAsset != null && node.getRelation() != null) {
            createRelation(parentAsset.getId(), asset.getId(), node.getRelation());
        }
        
        return asset;
    }

    private void processChildAssets(
            TenantId tenantId,
            Asset parentAsset,
            List<AssetNodeDto> childNodes,
            Map<String, Object> variables,
            Map<String, UUID> assetIdsByType,
            List<UUID> createdAssetIds,
            List<String> createdRelations) {
        
        for (AssetNodeDto childNode : childNodes) {
            Asset childAsset = createAssetFromNode(tenantId, childNode, variables, parentAsset);
            
            assetIdsByType.put(childNode.getType(), childAsset.getId().getId());
            createdAssetIds.add(childAsset.getId().getId());
            
            if (childNode.getRelation() != null) {
                createdRelations.add(String.format("%s -[%s]-> %s", 
                        parentAsset.getName(), childNode.getRelation(), childAsset.getName()));
            }
            
            if (childNode.getChildren() != null && !childNode.getChildren().isEmpty()) {
                processChildAssets(tenantId, childAsset, childNode.getChildren(), 
                        variables, assetIdsByType, createdAssetIds, createdRelations);
            }
        }
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

    private void loadTemplates() {
        log.info("Loading CT templates from classpath");
        
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:templates/*.json");
            
            for (Resource resource : resources) {
                try {
                    CTTemplateDto template = objectMapper.readValue(
                            resource.getInputStream(), 
                            CTTemplateDto.class);
                    templateCache.put(template.getTemplateId(), template);
                    log.debug("Loaded template: {} ({})", template.getName(), template.getTemplateId());
                } catch (Exception e) {
                    log.error("Error loading template from {}: {}", 
                            resource.getFilename(), e.getMessage());
                }
            }
            
            log.info("Loaded {} templates", templateCache.size());
        } catch (IOException e) {
            log.error("Error loading templates: {}", e.getMessage(), e);
        }
    }
}
