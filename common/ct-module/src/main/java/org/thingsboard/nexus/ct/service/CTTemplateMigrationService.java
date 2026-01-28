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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.template.*;
import org.thingsboard.server.dao.template.TemplateDefinitionDaoService;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTTemplateMigrationService {

    private final TemplateDefinitionDaoService daoService;
    private final ObjectMapper objectMapper;

    private static final String MODULE_CODE = "CT";
    private static final UUID SYSTEM_USER_ID = UUID.fromString("13814000-1dd2-11b2-8080-808080808080");

    @Transactional
    public int migrateJsonTemplatesToDb(UUID tenantId) {
        log.info("Starting migration of JSON templates to database for tenant: {}", tenantId);

        int migratedCount = 0;

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:templates/*.json");

            for (Resource resource : resources) {
                try {
                    String migrated = migrateTemplate(resource, tenantId);
                    if (migrated != null) {
                        migratedCount++;
                        log.info("Migrated template: {} from file: {}", migrated, resource.getFilename());
                    }
                } catch (Exception e) {
                    log.error("Error migrating template from {}: {}", resource.getFilename(), e.getMessage(), e);
                }
            }

            log.info("Migration completed. {} templates migrated for tenant: {}", migratedCount, tenantId);
        } catch (IOException e) {
            log.error("Error accessing template resources: {}", e.getMessage(), e);
        }

        return migratedCount;
    }

    private String migrateTemplate(Resource resource, UUID tenantId) throws IOException {
        JsonNode root = objectMapper.readTree(resource.getInputStream());

        String name = root.path("name").asText();
        String description = root.path("description").asText();
        String version = root.path("version").asText("1.0.0");
        String category = root.path("category").asText();

        String entityType = category.contains("REEL") ? "CT_REEL" : "CT_UNIT";
        String templateCode = MODULE_CODE.toLowerCase() + "-" + entityType.toLowerCase() + "-"
                + name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        Optional<TemplateDefinitionDto> existing = daoService.findByTemplateCode(templateCode);
        if (existing.isPresent()) {
            log.info("Template already exists in DB, skipping: {}", templateCode);
            return null;
        }

        TemplateStructure structure = convertAssetHierarchyToStructure(root.path("assetHierarchy"));
        List<TemplateVariable> variables = extractVariablesFromHierarchy(root.path("assetHierarchy"));

        TemplateDefinitionDto dto = TemplateDefinitionDto.builder()
                .id(UUID.randomUUID())
                .templateCode(templateCode)
                .templateName(name)
                .description(description)
                .moduleCode(MODULE_CODE)
                .entityType(entityType)
                .category(category)
                .version(version)
                .isActive(true)
                .templateStructureJson(serializeToJson(structure))
                .requiredVariablesJson(serializeToJson(variables))
                .createdBy(SYSTEM_USER_ID)
                .tenantId(tenantId)
                .createdTime(System.currentTimeMillis())
                .build();

        daoService.saveTemplateDefinition(dto);
        return templateCode;
    }

    private TemplateStructure convertAssetHierarchyToStructure(JsonNode hierarchyNode) {
        List<TemplateNode> nodes = new ArrayList<>();
        List<TemplateRelation> relations = new ArrayList<>();

        JsonNode rootNode = hierarchyNode.path("root");
        if (rootNode.isMissingNode()) {
            return new TemplateStructure(nodes, relations);
        }

        flattenNode(rootNode, null, nodes, relations, "root");

        return new TemplateStructure(nodes, relations);
    }

    private void flattenNode(JsonNode node, String parentKey,
                              List<TemplateNode> nodes, List<TemplateRelation> relations,
                              String nodeKey) {
        String type = node.path("type").asText();
        String name = node.path("name").asText();
        String label = node.path("label").asText(name);
        String relationType = node.path("relation").asText(null);
        boolean isRoot = parentKey == null;

        List<TemplateAttribute> attributes = new ArrayList<>();
        JsonNode attrsNode = node.path("attributes");
        if (attrsNode.isObject()) {
            attrsNode.fields().forEachRemaining(entry -> {
                TemplateAttribute attr = new TemplateAttribute();
                attr.setKey(entry.getKey());
                attr.setLabel(entry.getKey());
                attr.setDataType(inferDataType(entry.getValue()));
                attr.setValue(extractJsonValue(entry.getValue()));
                attr.setIsRequired(false);
                attr.setIsServerAttribute(true);
                attributes.add(attr);
            });
        }

        List<TemplateTelemetry> telemetries = new ArrayList<>();
        JsonNode telemetryKeys = node.path("telemetryKeys");
        if (telemetryKeys.isArray()) {
            for (JsonNode keyNode : telemetryKeys) {
                TemplateTelemetry telemetry = new TemplateTelemetry();
                telemetry.setKey(keyNode.asText());
                telemetry.setLabel(keyNode.asText());
                telemetry.setDataType("DOUBLE");
                telemetries.add(telemetry);
            }
        }

        TemplateNode templateNode = new TemplateNode();
        templateNode.setNodeKey(nodeKey);
        templateNode.setNodeName(name);
        templateNode.setNodeType(type);
        templateNode.setAssetType(type);
        templateNode.setIsRoot(isRoot);
        templateNode.setParentNodeKey(parentKey);
        templateNode.setRelationType(relationType);
        templateNode.setAttributes(attributes);
        templateNode.setTelemetries(telemetries);
        nodes.add(templateNode);

        if (parentKey != null && relationType != null) {
            TemplateRelation relation = new TemplateRelation();
            relation.setFromNodeKey(parentKey);
            relation.setToNodeKey(nodeKey);
            relation.setRelationType(relationType);
            relation.setRelationTypeGroup("COMMON");
            relations.add(relation);
        }

        JsonNode children = node.path("children");
        if (children.isArray()) {
            int childIndex = 0;
            for (JsonNode child : children) {
                String childKey = nodeKey + "_child_" + childIndex;
                String childType = child.path("type").asText("unknown");
                childKey = childType.toLowerCase().replaceAll("[^a-z0-9]+", "_");
                flattenNode(child, nodeKey, nodes, relations, childKey);
                childIndex++;
            }
        }
    }

    private List<TemplateVariable> extractVariablesFromHierarchy(JsonNode hierarchyNode) {
        Set<String> variableNames = new LinkedHashSet<>();
        extractVariableNamesFromNode(hierarchyNode, variableNames);

        List<TemplateVariable> variables = new ArrayList<>();
        for (String varName : variableNames) {
            TemplateVariable variable = new TemplateVariable();
            variable.setName(varName);
            variable.setLabel(camelCaseToLabel(varName));
            variable.setDataType("STRING");
            variable.setDescription("Variable: " + varName);
            variable.setIsRequired(true);
            variable.setMaxLength(255);
            variables.add(variable);
        }

        return variables;
    }

    private void extractVariableNamesFromNode(JsonNode node, Set<String> variableNames) {
        if (node.isTextual()) {
            String text = node.asText();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}").matcher(text);
            while (matcher.find()) {
                variableNames.add(matcher.group(1));
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> extractVariableNamesFromNode(entry.getValue(), variableNames));
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                extractVariableNamesFromNode(element, variableNames);
            }
        }
    }

    private String inferDataType(JsonNode value) {
        if (value.isTextual()) {
            return "STRING";
        } else if (value.isInt() || value.isLong()) {
            return "INTEGER";
        } else if (value.isDouble() || value.isFloat()) {
            return "DOUBLE";
        } else if (value.isBoolean()) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    private Object extractJsonValue(JsonNode value) {
        if (value.isTextual()) {
            return value.asText();
        } else if (value.isInt()) {
            return value.asInt();
        } else if (value.isLong()) {
            return value.asLong();
        } else if (value.isDouble() || value.isFloat()) {
            return value.asDouble();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        }
        return value.asText();
    }

    private String camelCaseToLabel(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                result.append(' ');
            }
            if (i == 0) {
                result.append(Character.toUpperCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON: " + e.getMessage(), e);
        }
    }
}
