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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DataMappingRuleId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.id.MappingTemplateRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.DataMappingRule;
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.nexus.DistributionMode;
import org.thingsboard.server.common.data.nexus.MappingTemplate;
import org.thingsboard.server.common.data.nexus.MappingTemplateRule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * Implementation of MappingTemplateService.
 */
@Service("MappingTemplateService")
@Slf4j
public class BaseMappingTemplateService implements MappingTemplateService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_TEMPLATE_ID = "Incorrect mappingTemplateId ";
    public static final String INCORRECT_RULE_ID = "Incorrect mappingTemplateRuleId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String INCORRECT_ASSET_ID = "Incorrect assetId ";
    public static final String TEMPLATE_NOT_FOUND = "Mapping template not found: ";
    public static final String RULE_NOT_FOUND = "Mapping template rule not found: ";
    public static final String MODULE_KEY_REQUIRED = "Module key is required";
    public static final String NAME_REQUIRED = "Name is required";
    public static final String TEMPLATE_NAME_EXISTS = "Mapping template with name '%s' already exists in module '%s'";
    public static final String SOURCE_KEY_REQUIRED = "Source key is required";
    public static final String TARGET_KEY_REQUIRED = "Target key is required";
    public static final String SOURCE_KEY_EXISTS = "Source key '%s' already exists in this template";

    @Autowired
    private MappingTemplateDao mappingTemplateDao;

    @Autowired
    private MappingTemplateRuleDao mappingTemplateRuleDao;

    @Autowired
    private DataSourceConfigDao dataSourceConfigDao;

    @Autowired
    private DataMappingRuleDao dataMappingRuleDao;

    // ========================
    // Mapping Templates
    // ========================

    @Override
    @Transactional
    public MappingTemplate saveMappingTemplate(TenantId tenantId, MappingTemplate template) {
        log.trace("Executing saveMappingTemplate [{}]", template);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateMappingTemplate(template);

        if (template.getId() == null) {
            // Creating new template
            if (mappingTemplateDao.existsByName(tenantId.getId(), template.getModuleKey(), template.getName())) {
                throw new IncorrectParameterException(String.format(TEMPLATE_NAME_EXISTS, template.getName(), template.getModuleKey()));
            }
            template.setTenantId(tenantId);
        } else {
            // Updating existing template
            MappingTemplate existing = mappingTemplateDao.findById(template.getId().getId());
            if (existing == null) {
                throw new IncorrectParameterException(TEMPLATE_NOT_FOUND + template.getId());
            }
            // Check if name is being changed to one that already exists
            if (!existing.getName().equals(template.getName()) &&
                    mappingTemplateDao.existsByName(tenantId.getId(), template.getModuleKey(), template.getName())) {
                throw new IncorrectParameterException(String.format(TEMPLATE_NAME_EXISTS, template.getName(), template.getModuleKey()));
            }
        }

        return mappingTemplateDao.save(tenantId, template);
    }

    @Override
    public MappingTemplate findMappingTemplateById(MappingTemplateId templateId) {
        log.trace("Executing findMappingTemplateById [{}]", templateId);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        return mappingTemplateDao.findById(templateId.getId());
    }

    @Override
    public PageData<MappingTemplate> findMappingTemplatesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMappingTemplatesByTenantId [{}] [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return mappingTemplateDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<MappingTemplate> findMappingTemplatesByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findMappingTemplatesByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return mappingTemplateDao.findByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    public PageData<MappingTemplate> findMappingTemplatesByModuleKey(TenantId tenantId, String moduleKey, PageLink pageLink) {
        log.trace("Executing findMappingTemplatesByModuleKey [{}] [{}] [{}]", tenantId, moduleKey, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        validatePageLink(pageLink);
        return mappingTemplateDao.findByModuleKey(tenantId.getId(), moduleKey, pageLink);
    }

    @Override
    public Optional<MappingTemplate> findDefaultMappingTemplateByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findDefaultMappingTemplateByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return mappingTemplateDao.findDefaultByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    public List<MappingTemplate> findActiveMappingTemplates(TenantId tenantId) {
        log.trace("Executing findActiveMappingTemplates [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return mappingTemplateDao.findActiveByTenantId(tenantId.getId());
    }

    @Override
    public List<MappingTemplate> findActiveMappingTemplatesByModuleKey(TenantId tenantId, String moduleKey) {
        log.trace("Executing findActiveMappingTemplatesByModuleKey [{}] [{}]", tenantId, moduleKey);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        return mappingTemplateDao.findActiveByModuleKey(tenantId.getId(), moduleKey);
    }

    @Override
    @Transactional
    public void deleteMappingTemplate(MappingTemplateId templateId) {
        log.trace("Executing deleteMappingTemplate [{}]", templateId);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);

        // First delete all rules for this template
        mappingTemplateRuleDao.deleteByTemplateId(templateId.getId());

        // Then delete the template
        if (!mappingTemplateDao.removeById(templateId.getId())) {
            throw new IncorrectParameterException(TEMPLATE_NOT_FOUND + templateId);
        }
    }

    @Override
    public boolean existsMappingTemplateByName(TenantId tenantId, String moduleKey, String name) {
        log.trace("Executing existsMappingTemplateByName [{}] [{}] [{}]", tenantId, moduleKey, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(moduleKey, k -> MODULE_KEY_REQUIRED);
        validateString(name, n -> NAME_REQUIRED);
        return mappingTemplateDao.existsByName(tenantId.getId(), moduleKey, name);
    }

    @Override
    @Transactional
    public MappingTemplate duplicateMappingTemplate(TenantId tenantId, MappingTemplateId templateId, String newName) {
        log.trace("Executing duplicateMappingTemplate [{}] [{}] [{}]", tenantId, templateId, newName);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        validateString(newName, n -> NAME_REQUIRED);

        MappingTemplate original = mappingTemplateDao.findById(templateId.getId());
        if (original == null) {
            throw new IncorrectParameterException(TEMPLATE_NOT_FOUND + templateId);
        }

        // Check if new name already exists
        if (mappingTemplateDao.existsByName(tenantId.getId(), original.getModuleKey(), newName)) {
            throw new IncorrectParameterException(String.format(TEMPLATE_NAME_EXISTS, newName, original.getModuleKey()));
        }

        // Create new template
        MappingTemplate newTemplate = new MappingTemplate();
        newTemplate.setTenantId(tenantId);
        newTemplate.setModuleKey(original.getModuleKey());
        newTemplate.setName(newName);
        newTemplate.setDescription(original.getDescription());
        newTemplate.setTargetAssetType(original.getTargetAssetType());
        newTemplate.setDistributionMode(original.getDistributionMode());
        newTemplate.setDefault(false); // Duplicate is never default
        newTemplate.setActive(original.isActive());
        newTemplate.setAdditionalInfo(original.getAdditionalInfo());

        MappingTemplate savedTemplate = mappingTemplateDao.save(tenantId, newTemplate);

        // Copy all rules
        List<MappingTemplateRule> originalRules = mappingTemplateRuleDao.findByTemplateId(templateId.getId());
        for (MappingTemplateRule originalRule : originalRules) {
            MappingTemplateRule newRule = new MappingTemplateRule();
            newRule.setTemplateId(savedTemplate.getId());
            newRule.setSourceKey(originalRule.getSourceKey());
            newRule.setTargetKey(originalRule.getTargetKey());
            newRule.setTransformationType(originalRule.getTransformationType());
            newRule.setTransformationConfig(originalRule.getTransformationConfig());
            newRule.setUnitSource(originalRule.getUnitSource());
            newRule.setUnitTarget(originalRule.getUnitTarget());
            newRule.setDescription(originalRule.getDescription());
            newRule.setPriority(originalRule.getPriority());
            newRule.setActive(originalRule.isActive());
            mappingTemplateRuleDao.save(tenantId, newRule);
        }

        return savedTemplate;
    }

    // ========================
    // Mapping Template Rules
    // ========================

    @Override
    @Transactional
    public MappingTemplateRule saveMappingTemplateRule(TenantId tenantId, MappingTemplateRule rule) {
        log.trace("Executing saveMappingTemplateRule [{}]", rule);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateMappingTemplateRule(rule);

        // Verify the template exists
        MappingTemplate template = mappingTemplateDao.findById(rule.getTemplateId().getId());
        if (template == null) {
            throw new IncorrectParameterException(TEMPLATE_NOT_FOUND + rule.getTemplateId());
        }

        // Check for duplicate source key (only for new rules)
        if (rule.getId() == null) {
            if (mappingTemplateRuleDao.existsBySourceKey(rule.getTemplateId().getId(), rule.getSourceKey())) {
                throw new IncorrectParameterException(String.format(SOURCE_KEY_EXISTS, rule.getSourceKey()));
            }
        }

        return mappingTemplateRuleDao.save(tenantId, rule);
    }

    @Override
    @Transactional
    public List<MappingTemplateRule> saveMappingTemplateRules(TenantId tenantId, MappingTemplateId templateId, List<MappingTemplateRule> rules) {
        log.trace("Executing saveMappingTemplateRules [{}] [{}]", templateId, rules.size());
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);

        // Verify the template exists
        MappingTemplate template = mappingTemplateDao.findById(templateId.getId());
        if (template == null) {
            throw new IncorrectParameterException(TEMPLATE_NOT_FOUND + templateId);
        }

        List<MappingTemplateRule> savedRules = new ArrayList<>();
        for (MappingTemplateRule rule : rules) {
            rule.setTemplateId(templateId);
            validateMappingTemplateRule(rule);
            savedRules.add(mappingTemplateRuleDao.save(tenantId, rule));
        }
        return savedRules;
    }

    @Override
    public MappingTemplateRule findMappingTemplateRuleById(MappingTemplateRuleId ruleId) {
        log.trace("Executing findMappingTemplateRuleById [{}]", ruleId);
        validateId(ruleId, id -> INCORRECT_RULE_ID + id);
        return mappingTemplateRuleDao.findById(ruleId.getId());
    }

    @Override
    public List<MappingTemplateRule> findMappingTemplateRulesByTemplateId(MappingTemplateId templateId) {
        log.trace("Executing findMappingTemplateRulesByTemplateId [{}]", templateId);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        return mappingTemplateRuleDao.findByTemplateId(templateId.getId());
    }

    @Override
    public List<MappingTemplateRule> findActiveMappingTemplateRulesByTemplateId(MappingTemplateId templateId) {
        log.trace("Executing findActiveMappingTemplateRulesByTemplateId [{}]", templateId);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        return mappingTemplateRuleDao.findActiveByTemplateId(templateId.getId());
    }

    @Override
    @Transactional
    public void deleteMappingTemplateRule(MappingTemplateRuleId ruleId) {
        log.trace("Executing deleteMappingTemplateRule [{}]", ruleId);
        validateId(ruleId, id -> INCORRECT_RULE_ID + id);
        if (!mappingTemplateRuleDao.removeById(ruleId.getId())) {
            throw new IncorrectParameterException(RULE_NOT_FOUND + ruleId);
        }
    }

    @Override
    @Transactional
    public void deleteMappingTemplateRulesByTemplateId(MappingTemplateId templateId) {
        log.trace("Executing deleteMappingTemplateRulesByTemplateId [{}]", templateId);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        mappingTemplateRuleDao.deleteByTemplateId(templateId.getId());
    }

    @Override
    public long countMappingTemplateRulesByTemplateId(MappingTemplateId templateId) {
        log.trace("Executing countMappingTemplateRulesByTemplateId [{}]", templateId);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        return mappingTemplateRuleDao.countByTemplateId(templateId.getId());
    }

    // ========================
    // Template Application
    // ========================

    @Override
    @Transactional
    public DataSourceConfig applyMappingTemplate(TenantId tenantId, MappingTemplateId templateId, DeviceId deviceId, AssetId targetAssetId) {
        log.trace("Executing applyMappingTemplate [{}] [{}] [{}] [{}]", tenantId, templateId, deviceId, targetAssetId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(templateId, id -> INCORRECT_TEMPLATE_ID + id);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        validateId(targetAssetId, id -> INCORRECT_ASSET_ID + id);

        // Get the template
        MappingTemplate template = mappingTemplateDao.findById(templateId.getId());
        if (template == null) {
            throw new IncorrectParameterException(TEMPLATE_NOT_FOUND + templateId);
        }

        // Get the template rules
        List<MappingTemplateRule> templateRules = mappingTemplateRuleDao.findActiveByTemplateId(templateId.getId());

        // Check if device already has a config
        Optional<DataSourceConfig> existingConfig = dataSourceConfigDao.findByDeviceId(tenantId.getId(), deviceId.getId());
        if (existingConfig.isPresent()) {
            throw new IncorrectParameterException("Device already has a data source configuration. Please delete it first.");
        }

        // Create the DataSourceConfig
        DataSourceConfig config = new DataSourceConfig();
        config.setTenantId(tenantId);
        config.setDeviceId(deviceId);
        config.setTargetAssetId(targetAssetId);
        config.setModuleKey(template.getModuleKey());
        config.setTargetAssetType(template.getTargetAssetType());
        config.setDistributionMode(template.getDistributionMode() != null ? template.getDistributionMode() : DistributionMode.MAPPED);
        config.setActive(true);
        // Store reference to the template used
        config.setAdditionalInfo(template.getAdditionalInfo());

        DataSourceConfig savedConfig = dataSourceConfigDao.save(tenantId, config);

        // Create DataMappingRules from template rules
        for (MappingTemplateRule templateRule : templateRules) {
            DataMappingRule rule = new DataMappingRule();
            rule.setDataSourceConfigId(savedConfig.getId());
            rule.setSourceKey(templateRule.getSourceKey());
            rule.setTargetKey(templateRule.getTargetKey());
            rule.setTransformationType(templateRule.getTransformationType());
            rule.setTransformationConfig(templateRule.getTransformationConfig());
            rule.setPriority(templateRule.getPriority());
            rule.setActive(templateRule.isActive());
            dataMappingRuleDao.save(tenantId, rule);
        }

        log.info("Applied mapping template '{}' to device {} -> asset {}. Created {} rules.",
                template.getName(), deviceId, targetAssetId, templateRules.size());

        return savedConfig;
    }

    // ========================
    // Cleanup
    // ========================

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);

        // Get all templates to delete their rules
        List<MappingTemplate> templates = mappingTemplateDao.findActiveByTenantId(tenantId.getId());
        for (MappingTemplate template : templates) {
            mappingTemplateRuleDao.deleteByTemplateId(template.getId().getId());
        }

        // Delete templates
        mappingTemplateDao.deleteByTenantId(tenantId.getId());
    }

    // ========================
    // Private Methods
    // ========================

    private void validateMappingTemplate(MappingTemplate template) {
        if (template == null) {
            throw new IncorrectParameterException("Mapping template cannot be null");
        }
        validateString(template.getModuleKey(), k -> MODULE_KEY_REQUIRED);
        validateString(template.getName(), n -> NAME_REQUIRED);
    }

    private void validateMappingTemplateRule(MappingTemplateRule rule) {
        if (rule == null) {
            throw new IncorrectParameterException("Mapping template rule cannot be null");
        }
        validateId(rule.getTemplateId(), id -> INCORRECT_TEMPLATE_ID + id);
        if (rule.getSourceKey() == null || rule.getSourceKey().isEmpty()) {
            throw new IncorrectParameterException(SOURCE_KEY_REQUIRED);
        }
        if (rule.getTargetKey() == null || rule.getTargetKey().isEmpty()) {
            throw new IncorrectParameterException(TARGET_KEY_REQUIRED);
        }
    }
}
