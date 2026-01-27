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
package org.thingsboard.server.dao.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TemplateId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.template.Template;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.template.TemplateDao;
import org.thingsboard.server.dao.template.TemplateValidator;

import java.util.Optional;

/**
 * Base implementation of TemplateService.
 */
@Service
@Slf4j
public class BaseTemplateService extends AbstractEntityService implements TemplateService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_TEMPLATE_ID = "Incorrect templateId ";

    @Autowired
    private TemplateDao templateDao;

    @Autowired
    private TemplateValidator templateValidator;

    @Override
    public Template findTemplateById(TenantId tenantId, TemplateId templateId) {
        log.trace("Executing findTemplateById [{}]", templateId);
        Validator.validateId(templateId, "Incorrect templateId " + templateId);
        return templateDao.findById(tenantId, templateId.getId());
    }

    @Override
    @Transactional
    public Template saveTemplate(Template template) {
        log.trace("Executing saveTemplate [{}]", template);
        templateValidator.validate(template, Template::getTenantId);
        return templateDao.save(template.getTenantId(), template);
    }

    @Override
    @Transactional
    public void deleteTemplate(TenantId tenantId, TemplateId templateId) {
        log.trace("Executing deleteTemplate [{}]", templateId);
        Validator.validateId(templateId, "Incorrect templateId " + templateId);
        Template template = findTemplateById(tenantId, templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template with id [" + templateId + "] not found");
        }
        templateDao.removeById(tenantId, templateId.getId());
    }

    @Override
    public PageData<Template> findTemplatesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTemplatesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink);
        return templateDao.findTemplatesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public Template findTemplateByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findTemplateByTenantIdAndName [{}][{}]", tenantId, name);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        return templateDao.findByTenantIdAndName(tenantId.getId(), name);
    }
}
