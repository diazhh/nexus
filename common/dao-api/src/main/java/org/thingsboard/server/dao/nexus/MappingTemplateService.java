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
package org.thingsboard.server.dao.nexus;

import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.id.MappingTemplateRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.nexus.MappingTemplate;
import org.thingsboard.server.common.data.nexus.MappingTemplateRule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Mapping Template operations.
 * Manages reusable mapping templates and their rules.
 */
public interface MappingTemplateService {

    // ========================
    // Mapping Templates
    // ========================

    /**
     * Save or update a mapping template
     */
    MappingTemplate saveMappingTemplate(TenantId tenantId, MappingTemplate template);

    /**
     * Find mapping template by ID
     */
    MappingTemplate findMappingTemplateById(MappingTemplateId templateId);

    /**
     * Find all mapping templates for a tenant with pagination
     */
    PageData<MappingTemplate> findMappingTemplatesByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Find mapping templates by module key
     */
    List<MappingTemplate> findMappingTemplatesByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Find mapping templates by module key with pagination
     */
    PageData<MappingTemplate> findMappingTemplatesByModuleKey(TenantId tenantId, String moduleKey, PageLink pageLink);

    /**
     * Find the default template for a module
     */
    Optional<MappingTemplate> findDefaultMappingTemplateByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Find active mapping templates for a tenant
     */
    List<MappingTemplate> findActiveMappingTemplates(TenantId tenantId);

    /**
     * Find active mapping templates by module key
     */
    List<MappingTemplate> findActiveMappingTemplatesByModuleKey(TenantId tenantId, String moduleKey);

    /**
     * Delete a mapping template (and all its rules)
     */
    void deleteMappingTemplate(MappingTemplateId templateId);

    /**
     * Check if a template name already exists within a module
     */
    boolean existsMappingTemplateByName(TenantId tenantId, String moduleKey, String name);

    /**
     * Duplicate a mapping template with a new name
     */
    MappingTemplate duplicateMappingTemplate(TenantId tenantId, MappingTemplateId templateId, String newName);

    // ========================
    // Mapping Template Rules
    // ========================

    /**
     * Save or update a mapping template rule
     */
    MappingTemplateRule saveMappingTemplateRule(TenantId tenantId, MappingTemplateRule rule);

    /**
     * Save multiple mapping template rules (bulk insert)
     */
    List<MappingTemplateRule> saveMappingTemplateRules(TenantId tenantId, MappingTemplateId templateId, List<MappingTemplateRule> rules);

    /**
     * Find mapping template rule by ID
     */
    MappingTemplateRule findMappingTemplateRuleById(MappingTemplateRuleId ruleId);

    /**
     * Find all rules for a template
     */
    List<MappingTemplateRule> findMappingTemplateRulesByTemplateId(MappingTemplateId templateId);

    /**
     * Find active rules for a template (ordered by priority)
     */
    List<MappingTemplateRule> findActiveMappingTemplateRulesByTemplateId(MappingTemplateId templateId);

    /**
     * Delete a mapping template rule
     */
    void deleteMappingTemplateRule(MappingTemplateRuleId ruleId);

    /**
     * Delete all rules for a template
     */
    void deleteMappingTemplateRulesByTemplateId(MappingTemplateId templateId);

    /**
     * Count rules for a template
     */
    long countMappingTemplateRulesByTemplateId(MappingTemplateId templateId);

    // ========================
    // Template Application
    // ========================

    /**
     * Apply a mapping template to create a DataSourceConfig and DataMappingRules.
     * This creates a new DataSourceConfig linking Device to Asset, and copies
     * all template rules as DataMappingRules.
     *
     * @param tenantId the tenant
     * @param templateId the mapping template to apply
     * @param deviceId the source device
     * @param targetAssetId the target asset
     * @return the created DataSourceConfig
     */
    DataSourceConfig applyMappingTemplate(TenantId tenantId, MappingTemplateId templateId, DeviceId deviceId, AssetId targetAssetId);

    // ========================
    // Cleanup
    // ========================

    /**
     * Delete all mapping templates and rules for a tenant
     */
    void deleteByTenantId(TenantId tenantId);
}
