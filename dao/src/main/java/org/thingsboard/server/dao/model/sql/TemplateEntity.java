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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.template.Template;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TEMPLATE_COLUMN_FAMILY_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = TEMPLATE_COLUMN_FAMILY_NAME)
public class TemplateEntity extends BaseSqlEntity<Template> {

    @Column(name = ModelConstants.TEMPLATE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.TEMPLATE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.TEMPLATE_CONFIGURATION_PROPERTY, columnDefinition = "jsonb")
    @Convert(converter = JsonConverter.class)
    private JsonNode configuration;

    public TemplateEntity() {
        super();
    }

    public TemplateEntity(Template template) {
        if (template.getId() != null) {
            this.setUuid(template.getId().getId());
        }
        this.setCreatedTime(template.getCreatedTime());
        if (template.getTenantId() != null) {
            this.tenantId = template.getTenantId().getId();
        }
        this.name = template.getName();
        this.configuration = template.getConfiguration();
    }

    @Override
    public Template toData() {
        Template template = new Template(new TemplateId(this.getUuid()));
        template.setCreatedTime(createdTime);
        if (tenantId != null) {
            template.setTenantId(TenantId.fromUUID(tenantId));
        }
        template.setName(name);
        template.setConfiguration(configuration);
        return template;
    }
}
