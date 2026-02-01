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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.id.MappingTemplateRuleId;
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.nexus.MappingTemplate;
import org.thingsboard.server.common.data.nexus.MappingTemplateRule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.nexus.DataDistributionService;
import org.thingsboard.server.dao.nexus.MappingTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.*;

/**
 * REST Controller for NEXUS Data Mapping Templates.
 * Manages reusable mapping templates and their rules for configuring data distribution.
 */
@Tag(name = "NEXUS Data Mapping", description = "APIs for managing data mapping templates and rules")
@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/nexus/dataMapping")
public class NexusDataMappingController extends BaseController {

    private final MappingTemplateService mappingTemplateService;
    private final DataDistributionService dataDistributionService;

    // ========================
    // Mapping Templates
    // ========================

    @Operation(summary = "Get all mapping templates", description = "Returns a page of all mapping templates for the current tenant. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/templates")
    public PageData<MappingTemplate> getMappingTemplates(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Text search term") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(mappingTemplateService.findMappingTemplatesByTenantId(currentUser.getTenantId(), pageLink));
    }

    @Operation(summary = "Get mapping templates by module", description = "Returns mapping templates filtered by module key. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/templates/module/{moduleKey}")
    public PageData<MappingTemplate> getMappingTemplatesByModule(
            @Parameter(description = "Module key (CT, DR, RV)", required = true) @PathVariable String moduleKey,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Text search term") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter("moduleKey", moduleKey);
        SecurityUser currentUser = getCurrentUser();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(mappingTemplateService.findMappingTemplatesByModuleKey(currentUser.getTenantId(), moduleKey, pageLink));
    }

    @Operation(summary = "Get active mapping templates by module", description = "Returns active mapping templates for a module. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/templates/module/{moduleKey}/active")
    public List<MappingTemplate> getActiveMappingTemplatesByModule(
            @Parameter(description = "Module key (CT, DR, RV)", required = true) @PathVariable String moduleKey
    ) throws ThingsboardException {
        checkParameter("moduleKey", moduleKey);
        SecurityUser currentUser = getCurrentUser();
        return mappingTemplateService.findActiveMappingTemplatesByModuleKey(currentUser.getTenantId(), moduleKey);
    }

    @Operation(summary = "Get mapping template by ID", description = "Returns a single mapping template by its ID. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template found"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/template/{templateId}")
    public MappingTemplate getMappingTemplateById(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        MappingTemplateId id = new MappingTemplateId(toUUID(templateId));
        return checkNotNull(mappingTemplateService.findMappingTemplateById(id));
    }

    @Operation(summary = "Create or update mapping template", description = "Creates a new mapping template or updates an existing one. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/template")
    public MappingTemplate saveMappingTemplate(
            @Parameter(description = "Mapping template object", required = true) @RequestBody MappingTemplate template
    ) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        return checkNotNull(mappingTemplateService.saveMappingTemplate(currentUser.getTenantId(), template));
    }

    @Operation(summary = "Delete mapping template", description = "Deletes a mapping template and all its rules. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/template/{templateId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMappingTemplate(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        MappingTemplateId id = new MappingTemplateId(toUUID(templateId));
        mappingTemplateService.deleteMappingTemplate(id);
    }

    @Operation(summary = "Duplicate mapping template", description = "Creates a copy of a mapping template with a new name. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template duplicated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or name already exists"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/template/{templateId}/duplicate")
    public MappingTemplate duplicateMappingTemplate(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId,
            @Parameter(description = "New name for the duplicated template", required = true) @RequestParam String newName
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        checkParameter("newName", newName);
        SecurityUser currentUser = getCurrentUser();
        MappingTemplateId id = new MappingTemplateId(toUUID(templateId));
        return checkNotNull(mappingTemplateService.duplicateMappingTemplate(currentUser.getTenantId(), id, newName));
    }

    // ========================
    // Mapping Template Rules
    // ========================

    @Operation(summary = "Get template rules", description = "Returns all rules for a mapping template. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/template/{templateId}/rules")
    public List<MappingTemplateRule> getMappingTemplateRules(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        MappingTemplateId id = new MappingTemplateId(toUUID(templateId));
        return mappingTemplateService.findMappingTemplateRulesByTemplateId(id);
    }

    @Operation(summary = "Get template rule by ID", description = "Returns a single mapping template rule. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule found"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/rule/{ruleId}")
    public MappingTemplateRule getMappingTemplateRuleById(
            @Parameter(description = "Rule ID", required = true) @PathVariable String ruleId
    ) throws ThingsboardException {
        checkParameter("ruleId", ruleId);
        MappingTemplateRuleId id = new MappingTemplateRuleId(toUUID(ruleId));
        return checkNotNull(mappingTemplateService.findMappingTemplateRuleById(id));
    }

    @Operation(summary = "Create or update template rule", description = "Creates a new rule or updates an existing one for a mapping template. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/rule")
    public MappingTemplateRule saveMappingTemplateRule(
            @Parameter(description = "Mapping template rule object", required = true) @RequestBody MappingTemplateRule rule
    ) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        return checkNotNull(mappingTemplateService.saveMappingTemplateRule(currentUser.getTenantId(), rule));
    }

    @Operation(summary = "Bulk create template rules", description = "Creates multiple rules for a mapping template at once. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rules saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/template/{templateId}/rules/bulk")
    public List<MappingTemplateRule> saveMappingTemplateRules(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId,
            @Parameter(description = "List of mapping template rules", required = true) @RequestBody List<MappingTemplateRule> rules
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        SecurityUser currentUser = getCurrentUser();
        MappingTemplateId id = new MappingTemplateId(toUUID(templateId));
        return mappingTemplateService.saveMappingTemplateRules(currentUser.getTenantId(), id, rules);
    }

    @Operation(summary = "Delete template rule", description = "Deletes a mapping template rule. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/rule/{ruleId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMappingTemplateRule(
            @Parameter(description = "Rule ID", required = true) @PathVariable String ruleId
    ) throws ThingsboardException {
        checkParameter("ruleId", ruleId);
        MappingTemplateRuleId id = new MappingTemplateRuleId(toUUID(ruleId));
        mappingTemplateService.deleteMappingTemplateRule(id);
    }

    @Operation(summary = "Count template rules", description = "Returns the count of rules in a mapping template. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/template/{templateId}/rules/count")
    public long countMappingTemplateRules(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        MappingTemplateId id = new MappingTemplateId(toUUID(templateId));
        return mappingTemplateService.countMappingTemplateRulesByTemplateId(id);
    }

    // ========================
    // Template Application
    // ========================

    @Operation(summary = "Apply mapping template", description = "Applies a mapping template to create a DataSourceConfig linking a Device to an Asset. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template applied successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or device already configured"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/template/{templateId}/apply")
    public DataSourceConfig applyMappingTemplate(
            @Parameter(description = "Template ID", required = true) @PathVariable String templateId,
            @Parameter(description = "Device ID", required = true) @RequestParam String deviceId,
            @Parameter(description = "Target Asset ID", required = true) @RequestParam String targetAssetId
    ) throws ThingsboardException {
        checkParameter("templateId", templateId);
        checkParameter("deviceId", deviceId);
        checkParameter("targetAssetId", targetAssetId);

        SecurityUser currentUser = getCurrentUser();
        MappingTemplateId tid = new MappingTemplateId(toUUID(templateId));
        DeviceId did = new DeviceId(toUUID(deviceId));
        AssetId aid = new AssetId(toUUID(targetAssetId));

        return checkNotNull(mappingTemplateService.applyMappingTemplate(currentUser.getTenantId(), tid, did, aid));
    }

    // ========================
    // Data Source Configurations
    // ========================

    @Operation(summary = "Get all data source configurations", description = "Returns a page of all data source configurations for the current tenant. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/dataSources")
    public PageData<DataSourceConfig> getDataSources(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Module key filter (CT, DR, RV)") @RequestParam(required = false) String moduleKey,
            @Parameter(description = "Text search term") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

        if (moduleKey != null && !moduleKey.isEmpty()) {
            return checkNotNull(dataDistributionService.findDataSourceConfigsByModuleKey(currentUser.getTenantId(), moduleKey, pageLink));
        } else {
            return checkNotNull(dataDistributionService.findDataSourceConfigsByTenantId(currentUser.getTenantId(), pageLink));
        }
    }

    @Operation(summary = "Get data source configuration by ID", description = "Returns a single data source configuration by its ID. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data source found"),
        @ApiResponse(responseCode = "404", description = "Data source not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/dataSource/{id}")
    public DataSourceConfig getDataSourceById(
            @Parameter(description = "Data Source Config ID", required = true) @PathVariable String id
    ) throws ThingsboardException {
        checkParameter("id", id);
        DataSourceConfigId dataSourceId = new DataSourceConfigId(toUUID(id));
        return checkNotNull(dataDistributionService.findDataSourceConfigById(dataSourceId));
    }

    @Operation(summary = "Delete data source configuration", description = "Deletes a data source configuration and all its mapping rules. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data source deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Data source not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/dataSource/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteDataSource(
            @Parameter(description = "Data Source Config ID", required = true) @PathVariable String id
    ) throws ThingsboardException {
        checkParameter("id", id);
        DataSourceConfigId dataSourceId = new DataSourceConfigId(toUUID(id));
        dataDistributionService.deleteDataSourceConfig(dataSourceId);
    }
}
