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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.template.Template;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.Validator;

import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * Validator for Template entity.
 */
@Component
public class TemplateValidator extends DataValidator<Template> {

    @Autowired
    private TemplateDao templateDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, Template template) {
        validateString(template.getName(), "Template name should be specified!");
        
        if (template.getTenantId() == null) {
            throw new DataValidationException("Template should be assigned to tenant!");
        }
        
        if (template.getConfiguration() == null) {
            throw new DataValidationException("Template configuration should be specified!");
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, Template template) {
        Template existingTemplate = templateDao.findByTenantIdAndName(tenantId.getId(), template.getName());
        if (existingTemplate != null) {
            throw new DataValidationException("Template with name '" + template.getName() + "' already exists!");
        }
    }

    @Override
    protected Template validateUpdate(TenantId tenantId, Template template) {
        Template existingTemplate = templateDao.findByTenantIdAndName(tenantId.getId(), template.getName());
        if (existingTemplate != null && !existingTemplate.getId().equals(template.getId())) {
            throw new DataValidationException("Template with name '" + template.getName() + "' already exists!");
        }
        return null;
    }
}
